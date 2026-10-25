package com.example.util

import android.content.Context
import android.content.Intent
import com.example.model.ApplicationItem

object AppShareHelper {
    const val GITHUB_APK_DOWNLOAD_URL = "https://github.com/sapp10056-sys/App-Store/releases/latest"

    fun createShareMessage(app: ApplicationItem): String {
        return buildString {
            append("📲 Check out \"${app.appName}\" on NovaStore!\n\n")
            append("• Developer: ${app.developerName}\n")
            append("• Version: v${app.versionName}\n")
            append("• Category: ${app.category}\n")
            if (app.shortDescription.isNotBlank()) {
                append("• Description: ${app.shortDescription}\n")
            }
            append("\n🔗 Open directly in NovaStore app:\n")
            append("novastore://app/${app.appId}\n\n")
            append("📥 Don't have NovaStore installed yet?\n")
            append("Download NovaStore APK from GitHub:\n")
            append(GITHUB_APK_DOWNLOAD_URL)
        }
    }

    fun shareApp(context: Context, app: ApplicationItem) {
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_SUBJECT, "Download ${app.appName} on NovaStore")
            putExtra(Intent.EXTRA_TEXT, createShareMessage(app))
            type = "text/plain"
        }
        context.startActivity(Intent.createChooser(sendIntent, "Share ${app.appName}"))
    }
}
