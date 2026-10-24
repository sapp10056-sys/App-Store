package com.example.security

import android.content.Context
import android.os.Debug
import android.os.Process
import android.util.Log
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.net.InetSocketAddress
import java.net.Socket
import kotlin.system.exitProcess

/**
 * SecurityIntegrityChecker
 * Detects unauthorized modding, tampering, hooking (Frida, Xposed),
 * memory injection, and reverse-engineering attempts.
 * If tampering is detected, immediately terminates the application process.
 */
object SecurityIntegrityChecker {
    private const val TAG = "NovaSecurity"

    fun checkIntegrityOrCrash(context: Context) {
        val violations = mutableListOf<String>()

        // 1. Check for Active Debugger Attachments
        if (isDebuggerAttached()) {
            violations.add("Active debugger attached")
        }

        // 2. Check for Hooking Frameworks in Memory (/proc/self/maps)
        if (isHookingFrameworkLoaded()) {
            violations.add("Hooking framework detected in process memory")
        }

        // 3. Check for Xposed / Frida Classes in Runtime ClassLoader
        if (isModdingClassPresent()) {
            violations.add("Reverse engineering / modding classes detected")
        }

        // 4. Check for Suspicious Modding Binaries / Pipes
        if (isSuspiciousBinaryPresent()) {
            violations.add("Modding binaries or debug pipes detected")
        }

        // If any violation is detected: Fatal Crash!
        if (violations.isNotEmpty()) {
            Log.e(TAG, "CRITICAL: Security integrity violation detected! Terminating process: $violations")
            triggerFatalTamperCrash("Security Integrity Violation: Unauthorized APK tampering detected.")
        }
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
                // Class not found, good
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

    fun triggerFatalTamperCrash(reason: String): Nothing {
        try {
            // Immediate terminate process
            Process.killProcess(Process.myPid())
            exitProcess(1)
        } finally {
            throw SecurityException(reason)
        }
    }
}
