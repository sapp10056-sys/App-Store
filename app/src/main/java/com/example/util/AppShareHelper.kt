package com.example.util

import android.content.Context
import android.content.Intent
import com.example.model.ApplicationItem

object AppShareHelper {
    const val GITHUB_APK_DOWNLOAD_URL = "https://github.com/sapp10056-sys/App-Store/releases/latest"

    /**
     * Share message for an individual app.
     * Contains the direct NovaStore APK download link prominently so the recipient
     * can tap the link and immediately download NovaStore APK.
     */
    fun createShareMessage(app: ApplicationItem, storeDownloadUrl: String = GITHUB_APK_DOWNLOAD_URL): String {
        return buildString {
            append("📲 Download \"${app.appName}\" on NovaStore App!\n\n")
            append("📥 Download NovaStore APK Link (Tap to download):\n")
            append("$storeDownloadUrl\n\n")
            append("Application Details:\n")
            append("• Name: ${app.appName}\n")
            append("• Version: v${app.versionName}\n")
            append("• Developer: ${app.developerName}\n")
            append("• Category: ${app.category}\n")
            if (app.shortDescription.isNotBlank()) {
                append("• Description: ${app.shortDescription}\n")
            }
            append("\n💡 Link par touch karke NovaStore APK download karein aur install karein.")
        }
    }

    /**
     * Share message for NovaStore APK itself.
     */
    fun createNovaStoreShareMessage(storeDownloadUrl: String = GITHUB_APK_DOWNLOAD_URL): String {
        return buildString {
            append("🚀 Download NovaStore Official APK!\n\n")
            append("Get 100% free Android applications, games, and direct updates without limits.\n\n")
            append("📥 Download NovaStore APK directly:\n")
            append("$storeDownloadUrl\n\n")
            append("💡 Link par touch karke NovaStore APK download aur install karein.")
        }
    }

    fun shareApp(context: Context, app: ApplicationItem, storeDownloadUrl: String = GITHUB_APK_DOWNLOAD_URL) {
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_SUBJECT, "Download ${app.appName} on NovaStore")
            putExtra(Intent.EXTRA_TEXT, createShareMessage(app, storeDownloadUrl))
            type = "text/plain"
        }
        context.startActivity(Intent.createChooser(sendIntent, "Share ${app.appName} via"))
    }

    fun shareNovaStoreApp(context: Context, storeDownloadUrl: String = GITHUB_APK_DOWNLOAD_URL) {
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_SUBJECT, "Download NovaStore APK")
            putExtra(Intent.EXTRA_TEXT, createNovaStoreShareMessage(storeDownloadUrl))
            type = "text/plain"
        }
        context.startActivity(Intent.createChooser(sendIntent, "Share NovaStore APK via"))
    }
}
