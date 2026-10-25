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
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(6, TimeUnit.SECONDS)
        .cache(null)
        .build()

    suspend fun checkForUpdate(currentVersionCode: Int): Result<AppUpdateInfo?> {
        return withContext(Dispatchers.IO) {
            try {
                val baseUrl = preferencesManager.getUpdateConfigUrl().trim()
                // Append timestamp cache-buster to prevent GitHub CDN and proxies from caching stale JSON
                val cacheBustUrl = if (baseUrl.contains("?")) {
                    "$baseUrl&_cb=${System.currentTimeMillis()}"
                } else {
                    "$baseUrl?_cb=${System.currentTimeMillis()}"
                }

                val request = Request.Builder()
                    .url(cacheBustUrl)
                    .header("Cache-Control", "no-cache, no-store, must-revalidate")
                    .header("Pragma", "no-cache")
                    .header("Expires", "0")
                    .build()

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
                val downloadUrl = json.optString("downloadUrl", "https://github.com/sapp10056-sys/App-Store/releases/latest")
                    .ifBlank { "https://github.com/sapp10056-sys/App-Store/releases/latest" }
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

                val admobObj = json.optJSONObject("admob")
                if (admobObj != null) {
                    val adEnabled = admobObj.optBoolean("enabled", preferencesManager.isAdMobEnabled())
                    val bannerId = admobObj.optString("bannerAdUnitId", preferencesManager.getAdMobBannerUnitId())
                    val rewardedId = admobObj.optString("rewardedAdUnitId", preferencesManager.getAdMobRewardedUnitId())
                    preferencesManager.setAdMobEnabled(adEnabled)
                    if (bannerId.isNotBlank()) preferencesManager.setAdMobBannerUnitId(bannerId)
                    if (rewardedId.isNotBlank()) preferencesManager.setAdMobRewardedUnitId(rewardedId)
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

                // Trigger update if there is a newer version, OR if remote admin flagged forceUpdate = true
                if (latestVersionCode > currentVersionCode || forceUpdate || currentVersionCode < minimumSupportedVersionCode) {
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
