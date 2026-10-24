package com.example.model

data class AppUpdateInfo(
    val latestVersionCode: Int = 1,
    val latestVersionName: String = "1.0.0",
    val minimumSupportedVersionCode: Int = 1,
    val forceUpdate: Boolean = false,
    val downloadUrl: String = "",
    val releaseNotes: String = "",
    val telegramBotUrl: String = "https://t.me/YOUR_BOT_USERNAME",
    val adminUsername: String = "admin",
    val adminPassword: String = "admin1234"
)
