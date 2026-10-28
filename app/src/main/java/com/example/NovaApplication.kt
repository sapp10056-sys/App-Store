package com.example

import android.app.Application
import com.example.security.SecurityIntegrityChecker

/**
 * NovaApplication
 * Executes strict signature integrity, package name verification, and anti-modding / anti-kill-sign
 * checks at the very earliest entry point before any Activity or Service starts.
 */
class NovaApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Verify signature, package name, and anti-kill-sign integrity immediately on launch
        SecurityIntegrityChecker.checkIntegrityOrCrash(this)
    }
}
