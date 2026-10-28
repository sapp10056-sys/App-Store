package com.example

import android.app.Application
import android.util.Log
import com.example.security.SecurityIntegrityChecker

/**
 * NovaApplication
 * Executes strict signature integrity, package name verification, and anti-modding / anti-kill-sign
 * checks at the very earliest entry point before any Activity or Service starts.
 */
class NovaApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            // Verify signature, package name, and anti-kill-sign integrity on launch
            SecurityIntegrityChecker.checkIntegrityOrCrash(this)
        } catch (e: SecurityException) {
            throw e
        } catch (e: Exception) {
            Log.w("NovaApplication", "Startup security note: ${e.message}")
        }
    }
}
