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
 * Implements strict military-grade anti-tamper and anti-sign-kill protection:
 * 1. Package Name Integrity: Blocks modified package names.
 * 2. Official Signature Integrity: Only the legitimate author signature is permitted.
 *    Any re-signed APK (custom cert, testkey, modder cert) crashes immediately.
 * 3. Anti-Sign-Kill Protection: Specifically catches MT Manager, NP Manager, and CorePatch
 *    dynamic proxies (hooks on ActivityThread.sPackageManager or ApplicationPackageManager.mPM)
 *    and injected signature-killer DEX/assets.
 * 4. Anti-Hooking & Memory Scanning: Scans /proc/self/maps for Frida, Xposed, SandHook, Substrate.
 * 5. Anti-Debugging: Detects JDWP debugger connection.
 * 6. Fatal Crash: Uses Linux kernel SIGKILL and process termination if any violation is detected.
 */
object SecurityIntegrityChecker {
    private const val TAG = "NovaSecurity"

    /** Official Package Names allowed for NovaStore */
    val ALLOWED_PACKAGE_NAMES = setOf(
        "com.novastore.app",
        "com.aistudio.novastore.app"
    )

    /** Known Official Build Signatures (SHA-256 in uppercase without colons) */
    val KNOWN_OFFICIAL_SIGNATURES = setOf(
        // Authoritative official build keystore
        "F4B6CFD322164BF6037A973F1B376B38357BCD66B7B7AA8AA0326AAF77DA544F"
    )

    /** Known Public Modder Testkeys */
    private val KNOWN_MALICIOUS_MODDER_SIGNATURES = setOf(
        // Standard Android Open Source Project (AOSP) testkey
        "27184BE3D332B5887864B62D56B61E872F544FF2CA99CE2B1A0E4D0EDF3014F3",
        "A40DA80A59D170CAA950CF15C18C454D47A39B26989D8B640ECD745BA71BF5DC"
    )

    /**
     * Verifies APK signature, package name, and anti-tamper integrity.
     * If signature mismatch from modding tools, sign-kill hook, or memory injection is detected,
     * the app terminates instantly via kernel SIGKILL.
     */
    fun checkIntegrityOrCrash(context: Context) {
        if (isTestEnvironment()) {
            Log.d(TAG, "Test environment detected; skipping integrity enforcement.")
            return
        }

        try {
            val criticalViolations = mutableListOf<String>()

            // 1. Package Name Integrity Check
            val currentPackage = context.packageName
            if (!ALLOWED_PACKAGE_NAMES.contains(currentPackage)) {
                criticalViolations.add("Package name hijacking detected: '$currentPackage' is unauthorized")
            }

            // 2. Anti-Sign-Kill Hook & Proxy Detection (MT Manager / NP Manager / CorePatch)
            if (isPackageManagerHooked(context)) {
                criticalViolations.add("Kill-Signature dynamic proxy or PackageManager hook detected")
            }

            // 3. Injected Modding Artifacts (Kill-Signature modules & patched DEX)
            if (isSignKillArtifactPresent(context)) {
                criticalViolations.add("Kill-Signature injection artifact or signature bypass module detected in APK")
            }

            // 4. Memory Hooking Frameworks (Frida, Xposed, Substrate, SandHook)
            if (isHookingFrameworkLoaded()) {
                criticalViolations.add("Hooking framework loaded in process memory (/proc/self/maps)")
            }

            // 5. Modding Classes in Runtime ClassLoader
            if (isModdingClassPresent()) {
                criticalViolations.add("Reverse-engineering / modding classes present in classloader")
            }

            // 6. Anti-Debugging Check
            if (isDebuggerAttached()) {
                criticalViolations.add("Active debugger attached to application process")
            }

            // 7. Signature Verification: Must strictly match official build signature
            val signatures = getInstalledSignatures(context)
            if (signatures.isEmpty()) {
                criticalViolations.add("No valid APK signing certificates found (Signature stripped or killed)")
            } else {
                var foundOfficial = false
                for (sig in signatures) {
                    val sha256 = computeSha256(sig)
                    if (KNOWN_MALICIOUS_MODDER_SIGNATURES.contains(sha256)) {
                        criticalViolations.add("APK is signed with a known public modder testkey: $sha256")
                        break
                    }
                    if (KNOWN_OFFICIAL_SIGNATURES.contains(sha256)) {
                        foundOfficial = true
                    }
                }
                if (!foundOfficial) {
                    criticalViolations.add("Unauthorized signature detected: APK has been re-signed or modded")
                }
            }

            // IF CRITICAL MODDING / SIGN-KILL / TAMPERING DETECTED: CRASH & TERMINATE PROCESS
            if (criticalViolations.isNotEmpty()) {
                Log.e(TAG, "CRITICAL SECURITY VIOLATION: $criticalViolations")
                triggerFatalTamperCrash("Security Integrity Violation: Unauthorized APK tampering detected. ($criticalViolations)")
            } else {
                Log.d(TAG, "Security integrity verified successfully for package: $currentPackage")
            }
        } catch (e: SecurityException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Integrity check diagnostic warning: ${e.message}")
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
     * Detects if PackageManager or ActivityThread.sPackageManager has been hooked by
     * Kill-Signature tools via dynamic proxy or reflection (MT Manager, NP Manager, CorePatch).
     */
    private fun isPackageManagerHooked(context: Context): Boolean {
        try {
            val pm = context.packageManager
            if (java.lang.reflect.Proxy.isProxyClass(pm.javaClass)) {
                return true
            }
            val pmClassName = pm.javaClass.name.lowercase()
            if (pmClassName.contains("proxy") && (pmClassName.contains("hook") || pmClassName.contains("fake") || pmClassName.contains("kill"))) {
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

            // Check ApplicationPackageManager mPM internal interface proxy (MT Manager's target)
            try {
                val mPmField = pm.javaClass.getDeclaredField("mPM")
                mPmField.isAccessible = true
                val mPm = mPmField.get(pm)
                if (mPm != null && java.lang.reflect.Proxy.isProxyClass(mPm.javaClass)) {
                    return true
                }
            } catch (_: Exception) {}
        } catch (_: Exception) {}
        return false
    }

    /**
     * Detects known signature-kill modules (MT Manager, NP Manager, CorePatch, SandHook, etc.)
     */
    private fun isSignKillArtifactPresent(context: Context): Boolean {
        val signKillClasses = listOf(
            "bin.mt.signature.Killer",
            "bin.mt.apksignaturekiller",
            "np.manager.signature",
            "np.manager.ApkSignatureKiller",
            "com.google.android.gms.internal.KillSignature",
            "arm.stub.StubApp",
            "com.swift.sandhook.SandHook",
            "me.weishu.epic.art.Epic",
            "top.canyie.pine.Pine",
            "andhook.lib.AndHook"
        )
        for (cls in signKillClasses) {
            try {
                Class.forName(cls)
                return true // Sign-kill class found!
            } catch (_: ClassNotFoundException) {}
        }

        // Direct scan inside the APK zip for signature-killer injected files & libs
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
                            name.contains("np_sign") ||
                            name.contains("libmt.so") ||
                            name.contains("libkiller.so") ||
                            name.contains("bin/mt/") ||
                            name.contains("assets/np/")
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
                    "sandhook",
                    "libmt.so",
                    "libkiller.so"
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

    private fun isTestEnvironment(): Boolean {
        return try {
            Class.forName("org.robolectric.Robolectric") != null
        } catch (_: ClassNotFoundException) {
            false
        } || Build.FINGERPRINT.contains("robolectric", ignoreCase = true)
    }

    /**
     * Immediately crashes and terminates the process when genuine modding/tampering is detected.
     */
    fun triggerFatalTamperCrash(reason: String): Nothing {
        try {
            Log.e(TAG, "FATAL SECURITY TAMPER DETECTED: $reason")
            Process.sendSignal(Process.myPid(), Process.SIGNAL_KILL)
            Process.killProcess(Process.myPid())
            exitProcess(137)
        } finally {
            throw SecurityException(reason)
        }
    }
}
