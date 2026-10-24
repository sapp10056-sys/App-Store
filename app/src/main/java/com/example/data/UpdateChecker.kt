package com.example.data

import android.util.Log
import com.example.model.AppUpdateInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class UpdateChecker(private val preferencesManager: PreferencesManager) {
    private val tag = "UpdateChecker"

    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    suspend fun checkForUpdate(currentVersionCode: Int): Result<AppUpdateInfo?> {
        return withContext(Dispatchers.IO) {
            try {
                val url = preferencesManager.getUpdateConfigUrl()
                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()

                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("HTTP error: ${response.code}"))
                }

                val body = response.body?.string() ?: return@withContext Result.failure(Exception("Empty response"))
                val json = JSONObject(body)

                val latestVersionCode = json.optInt("latestVersionCode", currentVersionCode)
                val latestVersionName = json.optString("latestVersionName", "1.0.0")
                val minimumSupportedVersionCode = json.optInt("minimumSupportedVersionCode", 1)
                val forceUpdate = json.optBoolean("forceUpdate", false)
                val downloadUrl = json.optString("downloadUrl", "")
                val releaseNotes = json.optString("releaseNotes", "Bug fixes and performance improvements.")
                val remoteBotUrl = json.optString("telegramBotUrl", preferencesManager.getTelegramBotUrl())

                if (remoteBotUrl.isNotBlank() && remoteBotUrl.startsWith("https://t.me/")) {
                    preferencesManager.setTelegramBotUrl(remoteBotUrl)
                }

                val adminObj = json.optJSONObject("admin")
                val adminUser = adminObj?.optString("username", preferencesManager.getAdminUsername())
                    ?: preferencesManager.getAdminUsername()
                val adminPass = adminObj?.optString("password", preferencesManager.getAdminPassword())
                    ?: preferencesManager.getAdminPassword()

                if (adminUser.isNotBlank() && adminPass.isNotBlank()) {
                    preferencesManager.setAdminUsername(adminUser)
                    preferencesManager.setAdminPassword(adminPass)
                }

                val updateInfo = AppUpdateInfo(
                    latestVersionCode = latestVersionCode,
                    latestVersionName = latestVersionName,
                    minimumSupportedVersionCode = minimumSupportedVersionCode,
                    forceUpdate = forceUpdate,
                    downloadUrl = downloadUrl,
                    releaseNotes = releaseNotes,
                    telegramBotUrl = remoteBotUrl,
                    adminUsername = adminUser,
                    adminPassword = adminPass
                )

                if (latestVersionCode > currentVersionCode) {
                    Result.success(updateInfo)
                } else {
                    Result.success(null) // Up to date
                }
            } catch (e: Exception) {
                Log.w(tag, "Remote config check handled: ${e.message}")
                Result.failure(e)
            }
        }
    }
}
