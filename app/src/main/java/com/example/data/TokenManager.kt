package com.example.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import com.example.model.ApplicationItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * Nova Token Management System.
 * - Handles User Unique ID generation & persistence.
 * - Manages user token balance and offline/online cloud sync via Firebase RTDB.
 * - Enforces token pricing:
 *     - ₹10  -> 50 tokens
 *     - ₹50  -> 400 tokens
 *     - ₹100 -> 800 tokens
 * - Enforces Premium APK upload fee: 20 tokens per upload.
 * - Enforces Premium APK download fee: 10 tokens per download:
 *     - 7 tokens credited to Developer ID.
 *     - 3 tokens credited to Admin account.
 * - Connects users to @nova_token_bot for token purchases.
 */
class TokenManager(private val context: Context) {
    private val tag = "TokenManager"
    private val prefs = context.getSharedPreferences("novastore_token_prefs", Context.MODE_PRIVATE)
    private val rtdbBaseUrl = "https://app-store-a9559-default-rtdb.asia-southeast1.firebasedatabase.app"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()

    private val scope = CoroutineScope(Dispatchers.IO)

    // Current user unique identifier
    val userId: String = getOrCreateUserId()

    // Current token balance for this device
    private val _userTokens = MutableStateFlow(prefs.getInt("cached_user_tokens", 0))
    val userTokens: StateFlow<Int> = _userTokens.asStateFlow()

    // Admin collected tokens balance
    private val _adminTokens = MutableStateFlow(prefs.getInt("cached_admin_tokens", 0))
    val adminTokens: StateFlow<Int> = _adminTokens.asStateFlow()

    // Apps unlocked by this user
    private val _unlockedAppIds = MutableStateFlow<Set<String>>(getSavedUnlockedApps())
    val unlockedAppIds: StateFlow<Set<String>> = _unlockedAppIds.asStateFlow()

    init {
        syncFromCloud()
    }

    private fun getOrCreateUserId(): String {
        var id = prefs.getString("unique_user_id", null)
        if (id.isNullOrBlank()) {
            val randomSuffix = UUID.randomUUID().toString().replace("-", "").take(8).lowercase()
            id = "user_$randomSuffix"
            prefs.edit().putString("unique_user_id", id).apply()
        }
        return id
    }

    private fun getSavedUnlockedApps(): Set<String> {
        val raw = prefs.getStringSet("unlocked_app_ids", emptySet()) ?: emptySet()
        return raw.toSet()
    }

    private fun saveUnlockedAppLocally(appId: String) {
        val updated = _unlockedAppIds.value.toMutableSet().apply { add(appId) }
        _unlockedAppIds.value = updated
        prefs.edit().putStringSet("unlocked_app_ids", updated).apply()
    }

    fun isAppUnlocked(appId: String): Boolean {
        return _unlockedAppIds.value.contains(appId)
    }

    /**
     * Refreshes user tokens and admin wallet balance from Firebase Realtime Database.
     */
    fun syncFromCloud() {
        scope.launch {
            try {
                // 1. Fetch user token balance
                val userRequest = Request.Builder()
                    .url("$rtdbBaseUrl/userTokens/$userId.json")
                    .get()
                    .build()
                val userResp = httpClient.newCall(userRequest).execute()
                if (userResp.isSuccessful) {
                    val body = userResp.body?.string()?.trim()
                    if (!body.isNullOrBlank() && body != "null") {
                        val tokens = body.toIntOrNull() ?: 0
                        _userTokens.value = tokens
                        prefs.edit().putInt("cached_user_tokens", tokens).apply()
                    } else {
                        // Initialize user record in cloud if null
                        updateCloudUserTokens(userId, _userTokens.value)
                    }
                }

                // 2. Fetch admin wallet balance
                val adminRequest = Request.Builder()
                    .url("$rtdbBaseUrl/adminWallet/balance.json")
                    .get()
                    .build()
                val adminResp = httpClient.newCall(adminRequest).execute()
                if (adminResp.isSuccessful) {
                    val body = adminResp.body?.string()?.trim()
                    if (!body.isNullOrBlank() && body != "null") {
                        val adminBal = body.toIntOrNull() ?: 0
                        _adminTokens.value = adminBal
                        prefs.edit().putInt("cached_admin_tokens", adminBal).apply()
                    }
                }

                // 3. Fetch unlocked apps for this user
                val unlockedRequest = Request.Builder()
                    .url("$rtdbBaseUrl/unlockedApps/$userId.json")
                    .get()
                    .build()
                val unlockedResp = httpClient.newCall(unlockedRequest).execute()
                if (unlockedResp.isSuccessful) {
                    val body = unlockedResp.body?.string()?.trim()
                    if (!body.isNullOrBlank() && body != "null" && body.startsWith("{")) {
                        val json = JSONObject(body)
                        val cloudUnlocked = mutableSetOf<String>()
                        val keys = json.keys()
                        while (keys.hasNext()) {
                            val k = keys.next()
                            if (json.optBoolean(k, false)) {
                                cloudUnlocked.add(k)
                            }
                        }
                        cloudUnlocked.addAll(_unlockedAppIds.value)
                        _unlockedAppIds.value = cloudUnlocked
                        prefs.edit().putStringSet("unlocked_app_ids", cloudUnlocked).apply()
                    }
                }
            } catch (e: Exception) {
                Log.w(tag, "Cloud sync handled: ${e.message}")
            }
        }
    }

    private fun updateCloudUserTokens(targetId: String, newBalance: Int) {
        val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
        val body = "$newBalance".toRequestBody(mediaType)
        val request = Request.Builder()
            .url("$rtdbBaseUrl/userTokens/$targetId.json")
            .put(body)
            .build()
        try {
            httpClient.newCall(request).execute().close()
        } catch (e: Exception) {
            Log.e(tag, "Failed to update cloud user tokens for $targetId: ${e.message}")
        }
    }

    private fun updateCloudAdminWallet(newBalance: Int) {
        val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
        val body = "$newBalance".toRequestBody(mediaType)
        val request = Request.Builder()
            .url("$rtdbBaseUrl/adminWallet/balance.json")
            .put(body)
            .build()
        try {
            httpClient.newCall(request).execute().close()
        } catch (e: Exception) {
            Log.e(tag, "Failed to update cloud admin wallet: ${e.message}")
        }
    }

    /**
     * Admin credits or sets tokens for a user ID.
     */
    fun adminCreditTokens(targetUserId: String, amountToAdd: Int, onComplete: (Boolean, Int) -> Unit) {
        scope.launch {
            try {
                // Fetch target's current balance
                val getReq = Request.Builder()
                    .url("$rtdbBaseUrl/userTokens/$targetUserId.json")
                    .get()
                    .build()
                val resp = httpClient.newCall(getReq).execute()
                val current = if (resp.isSuccessful) {
                    val text = resp.body?.string()?.trim()
                    if (!text.isNullOrBlank() && text != "null") text.toIntOrNull() ?: 0 else 0
                } else 0

                val newTotal = (current + amountToAdd).coerceAtLeast(0)
                updateCloudUserTokens(targetUserId, newTotal)

                if (targetUserId == userId) {
                    _userTokens.value = newTotal
                    prefs.edit().putInt("cached_user_tokens", newTotal).apply()
                }

                withContext(Dispatchers.Main) {
                    onComplete(true, newTotal)
                }
            } catch (e: Exception) {
                Log.e(tag, "Admin credit error: ${e.message}")
                withContext(Dispatchers.Main) {
                    onComplete(false, 0)
                }
            }
        }
    }

    /**
     * Deducts 20 tokens when a developer submits a Premium APK.
     */
    fun deductForPremiumUpload(devId: String, onResult: (Boolean, String) -> Unit) {
        val cost = PREMIUM_UPLOAD_COST
        val targetId = if (devId.isNotBlank()) devId else userId

        scope.launch {
            try {
                // Check balance
                val getReq = Request.Builder()
                    .url("$rtdbBaseUrl/userTokens/$targetId.json")
                    .get()
                    .build()
                val resp = httpClient.newCall(getReq).execute()
                var current = if (resp.isSuccessful) {
                    val text = resp.body?.string()?.trim()
                    if (!text.isNullOrBlank() && text != "null") text.toIntOrNull() ?: 0 else 0
                } else _userTokens.value

                if (current < cost) {
                    withContext(Dispatchers.Main) {
                        onResult(false, "Insufficient tokens! You need $cost Nova Tokens to list a Premium APK. Current balance: $current tokens.")
                    }
                    return@launch
                }

                val newBal = current - cost
                updateCloudUserTokens(targetId, newBal)

                if (targetId == userId) {
                    _userTokens.value = newBal
                    prefs.edit().putInt("cached_user_tokens", newBal).apply()
                }

                withContext(Dispatchers.Main) {
                    onResult(true, "Successfully deducted $cost tokens for premium listing.")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onResult(false, "Network error validating token balance: ${e.message}")
                }
            }
        }
    }

    /**
     * Unlocks a Premium APK for download:
     * - Checks user has at least 10 tokens.
     * - Deducts 10 tokens from user.
     * - Credits 7 tokens to Developer ID.
     * - Credits 3 tokens to Admin Wallet.
     * - Saves app as unlocked.
     */
    fun unlockPremiumApp(app: ApplicationItem, onResult: (Boolean, String) -> Unit) {
        if (isAppUnlocked(app.appId)) {
            onResult(true, "App already unlocked!")
            return
        }

        val cost = app.tokenCost.coerceAtLeast(PREMIUM_DOWNLOAD_COST)

        scope.launch {
            try {
                // 1. Check user token balance
                val userReq = Request.Builder()
                    .url("$rtdbBaseUrl/userTokens/$userId.json")
                    .get()
                    .build()
                val userResp = httpClient.newCall(userReq).execute()
                val currentBal = if (userResp.isSuccessful) {
                    val text = userResp.body?.string()?.trim()
                    if (!text.isNullOrBlank() && text != "null") text.toIntOrNull() ?: 0 else _userTokens.value
                } else _userTokens.value

                if (currentBal < cost) {
                    withContext(Dispatchers.Main) {
                        onResult(false, "Insufficient tokens! Need $cost tokens to download this Premium APK. Your balance: $currentBal tokens.")
                    }
                    return@launch
                }

                // 2. Deduct 10 tokens from user
                val newUserBal = currentBal - cost
                updateCloudUserTokens(userId, newUserBal)
                _userTokens.value = newUserBal
                prefs.edit().putInt("cached_user_tokens", newUserBal).apply()

                // 3. Credit 7 tokens to Developer
                val devId = app.developerId.ifBlank { "anonymous_dev" }
                if (devId.isNotBlank()) {
                    val devReq = Request.Builder()
                        .url("$rtdbBaseUrl/userTokens/$devId.json")
                        .get()
                        .build()
                    val devResp = httpClient.newCall(devReq).execute()
                    val devCurrent = if (devResp.isSuccessful) {
                        val text = devResp.body?.string()?.trim()
                        if (!text.isNullOrBlank() && text != "null") text.toIntOrNull() ?: 0 else 0
                    } else 0
                    val newDevBal = devCurrent + DEV_REWARD_TOKENS
                    updateCloudUserTokens(devId, newDevBal)
                }

                // 4. Credit 3 tokens to Admin Wallet
                val adminReq = Request.Builder()
                    .url("$rtdbBaseUrl/adminWallet/balance.json")
                    .get()
                    .build()
                val adminResp = httpClient.newCall(adminReq).execute()
                val adminCurrent = if (adminResp.isSuccessful) {
                    val text = adminResp.body?.string()?.trim()
                    if (!text.isNullOrBlank() && text != "null") text.toIntOrNull() ?: 0 else _adminTokens.value
                } else _adminTokens.value
                val newAdminBal = adminCurrent + ADMIN_COMMISSION_TOKENS
                updateCloudAdminWallet(newAdminBal)
                _adminTokens.value = newAdminBal
                prefs.edit().putInt("cached_admin_tokens", newAdminBal).apply()

                // 5. Save unlocked state locally and in cloud
                saveUnlockedAppLocally(app.appId)

                val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
                val unlockReq = Request.Builder()
                    .url("$rtdbBaseUrl/unlockedApps/$userId/${app.appId}.json")
                    .put("true".toRequestBody(mediaType))
                    .build()
                try {
                    httpClient.newCall(unlockReq).execute().close()
                } catch (_: Exception) {}

                withContext(Dispatchers.Main) {
                    onResult(true, "Successfully unlocked ${app.appName}! (10 tokens deducted: 7 to Dev, 3 to Admin)")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onResult(false, "Transaction failed: ${e.message}")
                }
            }
        }
    }

    /**
     * Opens Telegram bot @nova_token_bot with user ID pre-filled.
     */
    fun openTelegramBot(context: Context, planLabel: String = "") {
        val planText = if (planLabel.isNotBlank()) " for $planLabel" else ""
        val message = "Hello, I want to purchase Nova Tokens$planText.\nUser ID: $userId"
        val telegramDeepLink = "tg://resolve?domain=nova_token_bot&text=${Uri.encode(message)}"
        val webFallback = "https://t.me/nova_token_bot?text=${Uri.encode(message)}"

        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(telegramDeepLink)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            try {
                val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(webFallback)).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(webIntent)
            } catch (e: Exception) {
                Toast.makeText(context, "Please open Telegram and message @nova_token_bot with User ID: $userId", Toast.LENGTH_LONG).show()
            }
        }
    }

    companion object {
        const val BOT_USERNAME = "@nova_token_bot"
        const val BOT_URL = "https://t.me/nova_token_bot"

        // Token Pricing Packages
        const val TIER_1_INR = 10
        const val TIER_1_TOKENS = 50

        const val TIER_2_INR = 50
        const val TIER_2_TOKENS = 400

        const val TIER_3_INR = 100
        const val TIER_3_TOKENS = 800

        // Fee Rules
        const val PREMIUM_UPLOAD_COST = 20
        const val PREMIUM_DOWNLOAD_COST = 10
        const val DEV_REWARD_TOKENS = 7
        const val ADMIN_COMMISSION_TOKENS = 3
    }
}
