package com.example.security

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Debug
import android.os.Process
import android.util.Log
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.security.MessageDigest
import java.util.zip.ZipFile
import kotlin.system.exitProcess

/**
 * SecurityIntegrityChecker
 *
 * Implements Google Play App Defense / Play Integrity style anti-tamper security:
 * 1. Package Name Integrity: Ensures package name exactly matches official ID.
 * 2. Signature Verification: Checks SHA-256 certificate fingerprint against official key.
 * 3. Anti-Sign-Kill Protection: Detects MT Manager / NP Manager signature-kill hooks,
 *    fake PackageManager proxies, and injected signature-bypass DEX/assets.
 * 4. Anti-Hooking & Anti-Debugging: Detects Frida, Xposed, Substrate, and debuggers.
 *
 * IF ANY SIGNATURE MISMATCH, PACKAGE TAMPERING, OR KILL-SIGN ATTEMPT IS DETECTED:
 * THE APPLICATION IMMEDIATELY CRASHES AND TERMINATES PROCESS TO PREVENT EXECUTION.
 */
object SecurityIntegrityChecker {
    private const val TAG = "NovaSecurity"

    /** Official Package Name - Modded/cloned package names will crash immediately */
    const val EXPECTED_PACKAGE_NAME = "com.novastore.app"

    /** Official SHA-256 Certificate Fingerprints */
    private val VALID_SIGNATURE_SHA256_HASHES = setOf(
        // Official build debug key SHA-256 (no colons, uppercase)
        "F4B6CFD322164BF6037A973F1B376B38357BCD66B7B7AA8AA0326AAF77DA544F"
    )

    /**
     * Verifies APK signature, package name, and anti-tamper integrity.
     * If signature mismatch, sign-kill hook, or modding is detected, the app crashes immediately.
     */
    fun checkIntegrityOrCrash(context: Context) {
        if (isTestEnvironment()) {
            Log.d(TAG, "Test environment detected; skipping hard crash for local test runner.")
            return
        }

        val violations = mutableListOf<String>()

        // 1. Package Name Integrity Check
        val currentPackage = context.packageName
        if (currentPackage != EXPECTED_PACKAGE_NAME) {
            violations.add("Package name mismatch: expected '$EXPECTED_PACKAGE_NAME' but found '$currentPackage'")
        }

        // 2. Anti-Sign-Kill Hook & Proxy Detection
        if (isPackageManagerHooked(context)) {
            violations.add("Kill-Signature dynamic proxy or PackageManager hook detected")
        }

        // 3. Injected Modding Artifacts (MT Manager, NP Manager, CorePatch)
        if (isSignKillArtifactPresent(context)) {
            violations.add("Kill-Signature injection artifact or signature bypass module detected")
        }

        // 4. Official Signature Fingerprint Check
        val signatures = getInstalledSignatures(context)
        if (signatures.isEmpty()) {
            violations.add("No valid APK signing certificates found (Signature stripped or killed)")
        } else {
            var signatureMatches = false
            for (sig in signatures) {
                val sha256 = computeSha256(sig)
                if (VALID_SIGNATURE_SHA256_HASHES.contains(sha256)) {
                    signatureMatches = true
                    break
                }
            }
            if (!signatureMatches) {
                violations.add("Signature mismatch: APK re-signed with an unauthorized modder certificate")
            }
        }

        // 5. Active Debugger Check
        if (isDebuggerAttached()) {
            violations.add("Active debugger attached to process")
        }

        // 6. Memory Hooking Frameworks (Frida, Xposed, Substrate)
        if (isHookingFrameworkLoaded()) {
            violations.add("Hooking framework loaded in process memory (/proc/self/maps)")
        }

        // 7. Modding Classes in Runtime ClassLoader
        if (isModdingClassPresent()) {
            violations.add("Reverse-engineering / modding classes present in classloader")
        }

        // 8. Modding Binaries or Debug Pipes
        if (isSuspiciousBinaryPresent()) {
            violations.add("Modding binaries or debug pipes detected")
        }

        // IF ANY VIOLATION DETECTED: FATAL CRASH & TERMINATE PROCESS IMMEDIATELY!
        if (violations.isNotEmpty()) {
            Log.e(TAG, "CRITICAL: APK Signature Mismatch or Kill-Sign Tampering Detected! Violations: $violations")
            triggerFatalTamperCrash("CRITICAL SECURITY ERROR: APK Signature Mismatch or Package Tampering Detected! Process Terminated.")
        }
    }

    /**
     * Extracts current APK signing certificates across all Android versions.
     */
    @Suppress("DEPRECATION")
    private fun getInstalledSignatures(context: Context): List<ByteArray> {
        return try {
            val pm = context.packageManager
            val pkg = context.packageName
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val packageInfo = pm.getPackageInfo(pkg, PackageManager.GET_SIGNING_CERTIFICATES)
                val signingInfo = packageInfo.signingInfo ?: return emptyList()
                if (signingInfo.hasMultipleSigners()) {
                    signingInfo.apkContentsSigners.map { it.toByteArray() }
                } else {
                    signingInfo.signingCertificateHistory.map { it.toByteArray() }
                }
            } else {
                val packageInfo = pm.getPackageInfo(pkg, PackageManager.GET_SIGNATURES)
                packageInfo.signatures?.map { it.toByteArray() } ?: emptyList()
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    /**
     * Computes SHA-256 in uppercase hex without colons.
     */
    private fun computeSha256(bytes: ByteArray): String {
        return try {
            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(bytes)
            digest.joinToString("") { "%02X".format(it) }
        } catch (_: Exception) {
            ""
        }
    }

    /**
     * Detects if PackageManager has been hooked by Kill-Signature tools via dynamic proxy or reflection.
     */
    private fun isPackageManagerHooked(context: Context): Boolean {
        try {
            val pm = context.packageManager
            if (java.lang.reflect.Proxy.isProxyClass(pm.javaClass)) {
                return true
            }
            val pmClassName = pm.javaClass.name.lowercase()
            if (pmClassName.contains("proxy") || pmClassName.contains("hook") || pmClassName.contains("fake")) {
                return true
            }

            // Check ActivityThread sPackageManager proxy
            val activityThreadClass = Class.forName("android.app.ActivityThread")
            val getPackageManagerMethod = activityThreadClass.getDeclaredMethod("getPackageManager")
            getPackageManagerMethod.isAccessible = true
            val sPackageManager = getPackageManagerMethod.invoke(null)
            if (sPackageManager != null && java.lang.reflect.Proxy.isProxyClass(sPackageManager.javaClass)) {
                return true
            }
        } catch (_: Exception) {}
        return false
    }

    /**
     * Detects known signature-kill modules (MT Manager, NP Manager, CorePatch, etc.)
     */
    private fun isSignKillArtifactPresent(context: Context): Boolean {
        val signKillClasses = listOf(
            "bin.mt.signature.Killer",
            "bin.mt.apksignaturekiller",
            "np.manager.signature",
            "com.google.android.gms.internal.KillSignature",
            "arm.stub.StubApp",
            "com.swift.sandhook.SandHook"
        )
        for (cls in signKillClasses) {
            try {
                Class.forName(cls)
                return true
            } catch (_: ClassNotFoundException) {}
        }

        // Direct scan inside the APK zip for signature-killer injected files
        try {
            val apkPath = context.applicationInfo.sourceDir
            if (!apkPath.isNullOrBlank()) {
                val apkFile = File(apkPath)
                if (apkFile.exists()) {
                    val zip = ZipFile(apkFile)
                    val entries = zip.entries()
                    while (entries.hasMoreElements()) {
                        val name = entries.nextElement().name.lowercase()
                        if (name.contains("kill_signature") ||
                            name.contains("mt_sign") ||
                            name.contains("np_sign")
                        ) {
                            zip.close()
                            return true
                        }
                    }
                    zip.close()
                }
            }
        } catch (_: Exception) {}

        return false
    }

    private fun isDebuggerAttached(): Boolean {
        return try {
            Debug.isDebuggerConnected() || Debug.waitingForDebugger()
        } catch (_: Exception) {
            false
        }
    }

    private fun isHookingFrameworkLoaded(): Boolean {
        return try {
            val mapsFile = File("/proc/self/maps")
            if (mapsFile.exists() && mapsFile.canRead()) {
                val reader = BufferedReader(FileReader(mapsFile))
                var line: String?
                val suspiciousKeywords = listOf(
                    "frida",
                    "gadget",
                    "xposed",
                    "substrate",
                    "cydia",
                    "dexposed",
                    "edxposed",
                    "sandhook"
                )
                while (reader.readLine().also { line = it } != null) {
                    val lower = line?.lowercase() ?: ""
                    for (keyword in suspiciousKeywords) {
                        if (lower.contains(keyword)) {
                            reader.close()
                            return true
                        }
                    }
                }
                reader.close()
            }
            false
        } catch (_: Exception) {
            false
        }
    }

    private fun isModdingClassPresent(): Boolean {
        val suspiciousClasses = listOf(
            "de.robv.android.xposed.XposedBridge",
            "de.robv.android.xposed.XC_MethodHook",
            "com.saurik.substrate.MS\$MethodHook",
            "org.lsposed.lspd.core.LSPCore",
            "com.topjohnwu.magisk.DelegateApplication"
        )
        for (className in suspiciousClasses) {
            try {
                Class.forName(className)
                return true
            } catch (_: ClassNotFoundException) {
            } catch (_: Exception) {
                return true
            }
        }
        return false
    }

    private fun isSuspiciousBinaryPresent(): Boolean {
        val paths = listOf(
            "/data/local/tmp/frida-server",
            "/data/local/tmp/re.frida.server",
            "/system/bin/frida-server"
        )
        for (p in paths) {
            try {
                if (File(p).exists()) return true
            } catch (_: Exception) {}
        }
        return false
    }

    private fun isTestEnvironment(): Boolean {
        return try {
            Class.forName("org.robolectric.Robolectric") != null
        } catch (_: ClassNotFoundException) {
            false
        } || Build.FINGERPRINT.contains("robolectric", ignoreCase = true)
    }

    /**
     * Immediately crashes and terminates the process so modders cannot run the app.
     */
    fun triggerFatalTamperCrash(reason: String): Nothing {
        try {
            Log.e(TAG, "SECURITY CRASH: $reason")
            Process.killProcess(Process.myPid())
            exitProcess(137)
        } finally {
            throw SecurityException(reason)
        }
    }
}
