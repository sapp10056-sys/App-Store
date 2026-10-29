package com.example.data

import android.content.Context
import android.util.Log
import com.example.model.AppUpdateInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class UpdateChecker(
    private val context: Context? = null,
    private val preferencesManager: PreferencesManager
) {
    constructor(preferencesManager: PreferencesManager) : this(null, preferencesManager)

    private val tag = "UpdateChecker"
    var lastSyncError: String? = null
        private set
    var isRemoteLive: Boolean = false
        private set

    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .cache(null)
        .build()

    suspend fun checkForUpdate(currentVersionCode: Int): Result<AppUpdateInfo?> {
        return withContext(Dispatchers.IO) {
            val configuredUrl = preferencesManager.getUpdateConfigUrl().trim()

            // Candidate URLs to eliminate CDN caching and branch discrepancies
            val candidateUrls = mutableListOf<String>()
            candidateUrls.add(appendCacheBuster(configuredUrl))

            if (configuredUrl.contains("/main/")) {
                candidateUrls.add(appendCacheBuster(configuredUrl.replace("/main/", "/master/")))
            } else if (configuredUrl.contains("/master/")) {
                candidateUrls.add(appendCacheBuster(configuredUrl.replace("/master/", "/main/")))
            }

            // GitHub API contents direct endpoint (returns fresh raw content bypassing Fastly CDN)
            val rawRegex = Regex("""^https://raw\.githubusercontent\.com/([^/]+)/([^/]+)/([^/]+)/(.+)$""")
            val match = rawRegex.matchEntire(configuredUrl)
            if (match != null) {
                val (owner, repo, branch, path) = match.destructured
                candidateUrls.add("https://api.github.com/repos/$owner/$repo/contents/$path?ref=$branch")
            }

            // Firebase Realtime Database live cloud endpoint (instant real-time updates without caching)
            candidateUrls.add("https://app-store-a9559-default-rtdb.asia-southeast1.firebasedatabase.app/appUpdate.json")

            var fetchedJsonString: String? = null
            var had404 = false

            for (url in candidateUrls) {
                try {
                    val requestBuilder = Request.Builder()
                        .url(url)
                        .header("Cache-Control", "no-cache, no-store, max-age=0, must-revalidate")
                        .header("Pragma", "no-cache")
                        .header("Expires", "0")

                    if (url.contains("api.github.com")) {
                        requestBuilder.header("Accept", "application/vnd.github.raw")
                    }

                    val response = client.newCall(requestBuilder.build()).execute()
                    if (response.isSuccessful) {
                        val body = response.body?.string()
                        if (!body.isNullOrBlank()) {
                            fetchedJsonString = body
                            isRemoteLive = true
                            lastSyncError = null
                            Log.d(tag, "Successfully fetched live update config from: $url")
                            break
                        }
                    } else if (response.code == 404) {
                        had404 = true
                    }
                } catch (e: Exception) {
                    Log.w(tag, "Candidate url attempt failed ($url): ${e.message}")
                }
            }

            if (fetchedJsonString != null) {
                isRemoteLive = true
                lastSyncError = null
            } else {
                isRemoteLive = false
                if (had404) {
                    lastSyncError = "GitHub 404: Repository is PRIVATE! Make repo PUBLIC on GitHub to enable live sync."
                } else {
                    lastSyncError = "Network error: Unable to connect to remote GitHub endpoint."
                }
            }

            // Fallback to local asset if remote GitHub repo is not yet public, 404s, or device is offline
            if (fetchedJsonString == null && context != null) {
                try {
                    context.assets.open("app-update.json").use { stream ->
                        fetchedJsonString = stream.bufferedReader().use { it.readText() }
                        Log.d(tag, "Loaded local assets/app-update.json configuration")
                    }
                } catch (e: Exception) {
                    Log.w(tag, "Local assets/app-update.json fallback handled: ${e.message}")
                }
            }

            if (fetchedJsonString.isNullOrBlank()) {
                val error = lastSyncError ?: "Unable to fetch remote or local update configuration"
                return@withContext Result.failure(Exception(error))
            }

            try {
                val json = JSONObject(fetchedJsonString)

                val latestVersionCode = json.optInt("latestVersionCode", currentVersionCode)
                val latestVersionName = json.optString("latestVersionName", "1.0.0")
                val minimumSupportedVersionCode = json.optInt("minimumSupportedVersionCode", 1)
                val forceUpdate = json.optBoolean("forceUpdate", false)
                val downloadUrl = json.optString("downloadUrl", "https://github.com/sapp10056-sys/App-Store/releases/latest")
                    .ifBlank { "https://github.com/sapp10056-sys/App-Store/releases/latest" }
                val releaseNotes = json.optString("releaseNotes", "A mandatory remote update has been triggered by administrators on GitHub. Update to the latest version to maintain service access.")
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

                // If forceUpdate is true, or current version is below minimumSupportedVersionCode,
                // enforce update blocking screen immediately
                val isEnforced = forceUpdate || (currentVersionCode < minimumSupportedVersionCode)

                val updateInfo = AppUpdateInfo(
                    latestVersionCode = latestVersionCode,
                    latestVersionName = latestVersionName,
                    minimumSupportedVersionCode = minimumSupportedVersionCode,
                    forceUpdate = isEnforced,
                    downloadUrl = downloadUrl,
                    releaseNotes = releaseNotes,
                    telegramBotUrl = remoteBotUrl,
                    adminUsername = adminUser,
                    adminPassword = adminPass
                )

                if (latestVersionCode > currentVersionCode || isEnforced) {
                    Result.success(updateInfo)
                } else {
                    Result.success(null) // Up to date, unlocks app without reinstalling
                }
            } catch (e: Exception) {
                Log.e(tag, "Failed to parse update config: ${e.message}")
                Result.failure(e)
            }
        }
    }

    private fun appendCacheBuster(url: String): String {
        return if (url.contains("?")) {
            "$url&_cb=${System.currentTimeMillis()}"
        } else {
            "$url?_cb=${System.currentTimeMillis()}"
        }
    }
}
