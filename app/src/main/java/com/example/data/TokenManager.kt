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
 * TokenTransaction record for audit logging and admin economy monitoring.
 */
data class TokenTransaction(
    val txId: String = "",
    val type: String = "", // "ADMIN_CREDIT", "PREMIUM_UPLOAD", "PREMIUM_DOWNLOAD"
    val timestamp: Long = System.currentTimeMillis(),
    val userId: String = "",
    val developerId: String? = null,
    val appId: String? = null,
    val appName: String? = null,
    val amount: Int = 0,
    val note: String = ""
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "txId" to txId,
        "type" to type,
        "timestamp" to timestamp,
        "userId" to userId,
        "developerId" to developerId,
        "appId" to appId,
        "appName" to appName,
        "amount" to amount,
        "note" to note
    )

    companion object {
        fun fromJson(json: JSONObject): TokenTransaction {
            return TokenTransaction(
                txId = json.optString("txId", ""),
                type = json.optString("type", ""),
                timestamp = json.optLong("timestamp", System.currentTimeMillis()),
                userId = json.optString("userId", ""),
                developerId = json.optString("developerId", null),
                appId = json.optString("appId", null),
                appName = json.optString("appName", null),
                amount = json.optInt("amount", 0),
                note = json.optString("note", "")
            )
        }
    }
}

/**
 * Top earning developer representation in the token economy.
 */
data class DeveloperEarning(
    val developerId: String,
    val developerName: String,
    val totalTokensEarned: Int,
    val premiumAppsCount: Int
)

/**
 * Referral token grant with 30-day validity.
 * Purchased tokens have lifetime validity; referral tokens expire after 30 days.
 */
data class ReferralTokenGrant(
    val grantId: String = "",
    val amount: Int = 0,
    val grantedAt: Long = System.currentTimeMillis(),
    val expiresAt: Long = System.currentTimeMillis() + (30L * 24 * 60 * 60 * 1000L),
    val note: String = ""
) {
    val isExpired: Boolean get() = System.currentTimeMillis() > expiresAt
    val daysRemaining: Int get() = ((expiresAt - System.currentTimeMillis()) / (24L * 60 * 60 * 1000L)).coerceAtLeast(0L).toInt()

    fun toMap(): Map<String, Any?> = mapOf(
        "grantId" to grantId,
        "amount" to amount,
        "grantedAt" to grantedAt,
        "expiresAt" to expiresAt,
        "note" to note
    )

    companion object {
        fun fromJson(json: JSONObject): ReferralTokenGrant {
            return ReferralTokenGrant(
                grantId = json.optString("grantId", ""),
                amount = json.optInt("amount", 0),
                grantedAt = json.optLong("grantedAt", System.currentTimeMillis()),
                expiresAt = json.optLong("expiresAt", System.currentTimeMillis() + (30L * 24 * 60 * 60 * 1000L)),
                note = json.optString("note", "")
            )
        }
    }
}

/**
 * Audit record of a referral code usage between two users.
 */
data class ReferralLog(
    val logId: String = "",
    val referrerUserId: String = "",
    val referrerCode: String = "",
    val refereeUserId: String = "",
    val tokensToReferee: Int = 20,
    val tokensToReferrer: Int = 10,
    val timestamp: Long = System.currentTimeMillis(),
    val expiresAt: Long = System.currentTimeMillis() + (30L * 24 * 60 * 60 * 1000L)
) {
    val isExpired: Boolean get() = System.currentTimeMillis() > expiresAt
    val daysRemaining: Int get() = ((expiresAt - System.currentTimeMillis()) / (24L * 60 * 60 * 1000L)).coerceAtLeast(0L).toInt()

    fun toMap(): Map<String, Any?> = mapOf(
        "logId" to logId,
        "referrerUserId" to referrerUserId,
        "referrerCode" to referrerCode,
        "refereeUserId" to refereeUserId,
        "tokensToReferee" to tokensToReferee,
        "tokensToReferrer" to tokensToReferrer,
        "timestamp" to timestamp,
        "expiresAt" to expiresAt
    )

    companion object {
        fun fromJson(json: JSONObject): ReferralLog {
            return ReferralLog(
                logId = json.optString("logId", ""),
                referrerUserId = json.optString("referrerUserId", ""),
                referrerCode = json.optString("referrerCode", ""),
                refereeUserId = json.optString("refereeUserId", ""),
                tokensToReferee = json.optInt("tokensToReferee", 20),
                tokensToReferrer = json.optInt("tokensToReferrer", 10),
                timestamp = json.optLong("timestamp", System.currentTimeMillis()),
                expiresAt = json.optLong("expiresAt", System.currentTimeMillis() + (30L * 24 * 60 * 60 * 1000L))
            )
        }
    }
}

/**
 * Overall summary of the Token Economy for administrative reporting.
 */
data class TokenEconomySummary(
    val totalTokensInCirculation: Int = 0,
    val adminWalletBalance: Int = 0,
    val activeWalletsCount: Int = 0,
    val recentTransactions: List<TokenTransaction> = emptyList(),
    val topDevelopers: List<DeveloperEarning> = emptyList(),
    val referralLogs: List<ReferralLog> = emptyList(),
    val totalReferralTokensGranted: Int = 0
)

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
 * - Records audit transactions and aggregates economy metrics for Admin Console.
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

    // Unique referral code for this device (e.g. NOVA-E31102)
    val referralCode: String = getOrCreateReferralCode()

    // Breakdown: Lifetime purchased tokens (never expire) vs referral bonus tokens (valid for 30 days)
    private val _purchasedTokens = MutableStateFlow(prefs.getInt("cached_purchased_tokens", prefs.getInt("cached_user_tokens", 0)))
    val purchasedTokens: StateFlow<Int> = _purchasedTokens.asStateFlow()

    private val _referralGrants = MutableStateFlow<List<ReferralTokenGrant>>(loadSavedReferralGrants())
    val referralGrants: StateFlow<List<ReferralTokenGrant>> = _referralGrants.asStateFlow()

    private val _activeReferralTokens = MutableStateFlow(calculateActiveReferralTokens(_referralGrants.value))
    val activeReferralTokens: StateFlow<Int> = _activeReferralTokens.asStateFlow()

    // Total available token balance for this device (purchased + unexpired referral)
    private val _userTokens = MutableStateFlow((_purchasedTokens.value + _activeReferralTokens.value).coerceAtLeast(0))
    val userTokens: StateFlow<Int> = _userTokens.asStateFlow()

    // Has this device already claimed a referral code
    private val _hasRedeemedReferral = MutableStateFlow(prefs.getBoolean("has_redeemed_referral", false))
    val hasRedeemedReferral: StateFlow<Boolean> = _hasRedeemedReferral.asStateFlow()

    // Admin collected tokens balance
    private val _adminTokens = MutableStateFlow(prefs.getInt("cached_admin_tokens", 0))
    val adminTokens: StateFlow<Int> = _adminTokens.asStateFlow()

    // Apps unlocked by this user
    private val _unlockedAppIds = MutableStateFlow<Set<String>>(getSavedUnlockedApps())
    val unlockedAppIds: StateFlow<Set<String>> = _unlockedAppIds.asStateFlow()

    init {
        recalculateBalance()
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

    private fun getOrCreateReferralCode(): String {
        var code = prefs.getString("unique_referral_code", null)
        if (code.isNullOrBlank()) {
            val suffix = userId.removePrefix("user_").take(6).uppercase()
            code = "NOVA-$suffix"
            prefs.edit().putString("unique_referral_code", code).apply()
        }
        return code
    }

    fun hasRedeemedReferral(): Boolean = _hasRedeemedReferral.value

    fun getRedeemedReferralCode(): String? = prefs.getString("redeemed_referral_code", null)

    private fun calculateActiveReferralTokens(grants: List<ReferralTokenGrant>): Int {
        val now = System.currentTimeMillis()
        return grants.filter { it.expiresAt > now }.sumOf { it.amount }
    }

    fun recalculateBalance() {
        val activeRef = calculateActiveReferralTokens(_referralGrants.value)
        _activeReferralTokens.value = activeRef
        val total = (_purchasedTokens.value + activeRef).coerceAtLeast(0)
        _userTokens.value = total
        prefs.edit()
            .putInt("cached_purchased_tokens", _purchasedTokens.value)
            .putInt("cached_user_tokens", total)
            .apply()
    }

    private fun loadSavedReferralGrants(): List<ReferralTokenGrant> {
        val raw = prefs.getString("cached_referral_grants", null) ?: return emptyList()
        return try {
            val arr = org.json.JSONArray(raw)
            val list = mutableListOf<ReferralTokenGrant>()
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i)
                if (obj != null) {
                    val grant = ReferralTokenGrant.fromJson(obj)
                    if (!grant.isExpired) list.add(grant)
                }
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun saveReferralGrantsLocally(grants: List<ReferralTokenGrant>) {
        try {
            val arr = org.json.JSONArray()
            grants.filter { !it.isExpired }.forEach { arr.put(JSONObject(it.toMap())) }
            prefs.edit().putString("cached_referral_grants", arr.toString()).apply()
        } catch (_: Exception) {}
    }

    /**
     * Deducts tokens prioritizing expiring referral grants first, then lifetime purchased tokens.
     */
    fun spendTokensFromUser(amountToSpend: Int): Boolean {
        recalculateBalance()
        if (_userTokens.value < amountToSpend) return false

        var remaining = amountToSpend
        val currentGrants = _referralGrants.value.filter { !it.isExpired }.sortedBy { it.expiresAt }
        val updatedGrants = mutableListOf<ReferralTokenGrant>()

        for (grant in currentGrants) {
            if (remaining <= 0) {
                updatedGrants.add(grant)
            } else if (grant.amount <= remaining) {
                remaining -= grant.amount
            } else {
                updatedGrants.add(grant.copy(amount = grant.amount - remaining))
                remaining = 0
            }
        }

        if (remaining > 0) {
            _purchasedTokens.value = (_purchasedTokens.value - remaining).coerceAtLeast(0)
        }

        _referralGrants.value = updatedGrants
        saveReferralGrantsLocally(updatedGrants)
        recalculateBalance()
        updateCloudUserTokens(userId, _userTokens.value)
        syncWalletDataToCloud()
        return true
    }

    /**
     * Syncs full wallet data (purchased tokens + 30-day referral grants) to RTDB /userTokensData/{userId}.json
     */
    fun syncWalletDataToCloud() {
        scope.launch {
            try {
                val grantsMap = mutableMapOf<String, Any>()
                _referralGrants.value.filter { !it.isExpired }.forEach {
                    grantsMap[it.grantId] = it.toMap()
                }
                val payload = mapOf(
                    "userId" to userId,
                    "purchasedTokens" to _purchasedTokens.value,
                    "activeReferralTokens" to _activeReferralTokens.value,
                    "totalTokens" to _userTokens.value,
                    "referralGrants" to grantsMap,
                    "updatedAt" to System.currentTimeMillis()
                )
                val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
                val req = Request.Builder()
                    .url("$rtdbBaseUrl/userTokensData/$userId.json")
                    .put(JSONObject(payload).toString().toRequestBody(mediaType))
                    .build()
                httpClient.newCall(req).execute().close()
            } catch (e: Exception) {
                Log.w(tag, "syncWalletDataToCloud note: ${e.message}")
            }
        }
    }

    /**
     * Redeems a referral code on app open or in token wallet:
     * - Referee receives 20 tokens (30 days validity).
     * - Referrer receives 10 tokens (30 days validity).
     * - Disallows self-referral and duplicate redemption.
     */
    fun redeemReferralCode(rawCode: String, onResult: (Boolean, String) -> Unit) {
        val clean = rawCode.trim().uppercase()
        if (clean.isBlank()) {
            onResult(false, "Please enter a referral code.")
            return
        }
        if (clean == referralCode) {
            onResult(false, "You cannot use your own referral code!")
            return
        }
        if (_hasRedeemedReferral.value) {
            onResult(false, "You have already claimed a referral bonus on this device!")
            return
        }

        scope.launch {
            try {
                // 1. Resolve referrer user ID from cloud RTDB /referralCodes/{CODE}.json
                val codeReq = Request.Builder()
                    .url("$rtdbBaseUrl/referralCodes/$clean.json")
                    .get()
                    .build()
                val codeResp = httpClient.newCall(codeReq).execute()
                var referrerId: String? = null
                if (codeResp.isSuccessful) {
                    val body = codeResp.body?.string()?.trim()?.replace("\"", "")
                    if (!body.isNullOrBlank() && body != "null") {
                        referrerId = body
                    }
                }

                // Fallback: If code is standard NOVA-XXXX format, derive user ID
                if (referrerId == null && clean.startsWith("NOVA-") && clean.length >= 7) {
                    val hex = clean.removePrefix("NOVA-").lowercase()
                    referrerId = "user_$hex"
                }

                if (referrerId.isNullOrBlank() || referrerId == userId) {
                    withContext(Dispatchers.Main) {
                        onResult(false, "Invalid referral code. Please check and try again.")
                    }
                    return@launch
                }

                val now = System.currentTimeMillis()
                val expiresAt = now + REFERRAL_VALIDITY_MS

                // 2. Grant 20 tokens to Referee (current user) with 30-day validity
                val refereeGrant = ReferralTokenGrant(
                    grantId = "grant_${now}_referee",
                    amount = REFEREE_BONUS_TOKENS,
                    grantedAt = now,
                    expiresAt = expiresAt,
                    note = "Referral code applied: $clean (+20 tokens, 30 days validity)"
                )
                val newGrants = _referralGrants.value + refereeGrant
                _referralGrants.value = newGrants
                saveReferralGrantsLocally(newGrants)
                recalculateBalance()
                updateCloudUserTokens(userId, _userTokens.value)
                syncWalletDataToCloud()

                _hasRedeemedReferral.value = true
                prefs.edit()
                    .putBoolean("has_redeemed_referral", true)
                    .putString("redeemed_referral_code", clean)
                    .apply()

                // 3. Credit 10 tokens to Referrer in cloud RTDB (with 30-day validity)
                val referrerGrant = ReferralTokenGrant(
                    grantId = "grant_${now}_referrer",
                    amount = REFERRER_REWARD_TOKENS,
                    grantedAt = now,
                    expiresAt = expiresAt,
                    note = "Referral reward from $userId (+10 tokens, 30 days validity)"
                )
                val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
                try {
                    val putGrantReq = Request.Builder()
                        .url("$rtdbBaseUrl/userTokensData/$referrerId/referralGrants/${referrerGrant.grantId}.json")
                        .put(JSONObject(referrerGrant.toMap()).toString().toRequestBody(mediaType))
                        .build()
                    httpClient.newCall(putGrantReq).execute().close()

                    // Also fetch and bump referrer's userTokens total
                    val refGet = Request.Builder().url("$rtdbBaseUrl/userTokens/$referrerId.json").get().build()
                    val refResp = httpClient.newCall(refGet).execute()
                    val refCur = if (refResp.isSuccessful) {
                        val text = refResp.body?.string()?.trim()
                        if (!text.isNullOrBlank() && text != "null") text.toIntOrNull() ?: 0 else 0
                    } else 0
                    val newRefBal = refCur + REFERRER_REWARD_TOKENS
                    updateCloudUserTokens(referrerId, newRefBal)
                } catch (e: Exception) {
                    Log.w(tag, "Referrer credit note: ${e.message}")
                }

                // 4. Save Referral Log to /referralLogs/{logId}.json
                val logId = "reflog_${now}_${(1000..9999).random()}"
                val log = ReferralLog(
                    logId = logId,
                    referrerUserId = referrerId,
                    referrerCode = clean,
                    refereeUserId = userId,
                    tokensToReferee = REFEREE_BONUS_TOKENS,
                    tokensToReferrer = REFERRER_REWARD_TOKENS,
                    timestamp = now,
                    expiresAt = expiresAt
                )
                try {
                    val logReq = Request.Builder()
                        .url("$rtdbBaseUrl/referralLogs/$logId.json")
                        .put(JSONObject(log.toMap()).toString().toRequestBody(mediaType))
                        .build()
                    httpClient.newCall(logReq).execute().close()
                } catch (e: Exception) {
                    Log.w(tag, "Referral log note: ${e.message}")
                }

                // 5. Record transactions for audit
                recordTransaction(
                    type = "REFERRAL_BONUS_CLAIMED",
                    targetUserId = userId,
                    amount = REFEREE_BONUS_TOKENS,
                    note = "Claimed referral code $clean (+20 tokens, valid for 30 days)"
                )
                recordTransaction(
                    type = "REFERRAL_REWARD_EARNED",
                    targetUserId = referrerId,
                    amount = REFERRER_REWARD_TOKENS,
                    note = "Earned +10 tokens from referral of $userId (valid for 30 days)"
                )

                withContext(Dispatchers.Main) {
                    onResult(true, "🎉 Referral code applied! 20 Nova Tokens added to your wallet (valid for 30 days). Referrer received 10 tokens.")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onResult(false, "Network error applying referral code: ${e.message}")
                }
            }
        }
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
     * Records transaction in Firebase Realtime Database for audit and analytics.
     */
    fun recordTransaction(
        type: String,
        targetUserId: String,
        amount: Int,
        devId: String? = null,
        appId: String? = null,
        appName: String? = null,
        note: String = ""
    ) {
        val txId = "tx_${System.currentTimeMillis()}_${(1000..9999).random()}"
        val tx = TokenTransaction(
            txId = txId,
            type = type,
            timestamp = System.currentTimeMillis(),
            userId = targetUserId,
            developerId = devId,
            appId = appId,
            appName = appName,
            amount = amount,
            note = note
        )
        scope.launch {
            try {
                val json = JSONObject(tx.toMap()).toString()
                val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
                val request = Request.Builder()
                    .url("$rtdbBaseUrl/tokenTransactions/$txId.json")
                    .put(json.toRequestBody(mediaType))
                    .build()
                httpClient.newCall(request).execute().close()
            } catch (e: Exception) {
                Log.e(tag, "Failed to record transaction $txId: ${e.message}")
            }
        }
    }

    /**
     * Refreshes user tokens and admin wallet balance from Firebase Realtime Database.
     */
    fun syncFromCloud() {
        scope.launch {
            try {
                // 0. Register my referral code in cloud
                try {
                    val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
                    val regReq = Request.Builder()
                        .url("$rtdbBaseUrl/referralCodes/$referralCode.json")
                        .put("\"$userId\"".toRequestBody(mediaType))
                        .build()
                    httpClient.newCall(regReq).execute().close()
                } catch (_: Exception) {}

                // 1. Fetch user detailed tokens data (purchased vs referral grants)
                var loadedDetailed = false
                try {
                    val dataReq = Request.Builder()
                        .url("$rtdbBaseUrl/userTokensData/$userId.json")
                        .get()
                        .build()
                    val dataResp = httpClient.newCall(dataReq).execute()
                    if (dataResp.isSuccessful) {
                        val body = dataResp.body?.string()?.trim()
                        if (!body.isNullOrBlank() && body != "null" && body.startsWith("{")) {
                            val json = JSONObject(body)
                            val purchased = json.optInt("purchasedTokens", -1)
                            if (purchased >= 0) {
                                _purchasedTokens.value = purchased
                            }
                            val grantsObj = json.optJSONObject("referralGrants")
                            if (grantsObj != null) {
                                val grantsList = mutableListOf<ReferralTokenGrant>()
                                val keys = grantsObj.keys()
                                while (keys.hasNext()) {
                                    val gKey = keys.next()
                                    val gObj = grantsObj.optJSONObject(gKey)
                                    if (gObj != null) {
                                        val grant = ReferralTokenGrant.fromJson(gObj)
                                        if (!grant.isExpired) {
                                            grantsList.add(grant)
                                        }
                                    }
                                }
                                _referralGrants.value = grantsList
                                saveReferralGrantsLocally(grantsList)
                            }
                            recalculateBalance()
                            loadedDetailed = true
                        }
                    }
                } catch (e: Exception) {
                    Log.w(tag, "userTokensData sync note: ${e.message}")
                }

                // If no detailed data yet, fallback to /userTokens/$userId.json
                if (!loadedDetailed) {
                    val userRequest = Request.Builder()
                        .url("$rtdbBaseUrl/userTokens/$userId.json")
                        .get()
                        .build()
                    val userResp = httpClient.newCall(userRequest).execute()
                    if (userResp.isSuccessful) {
                        val body = userResp.body?.string()?.trim()
                        if (!body.isNullOrBlank() && body != "null") {
                            val tokens = body.toIntOrNull() ?: 0
                            _purchasedTokens.value = tokens
                            recalculateBalance()
                        } else {
                            updateCloudUserTokens(userId, _userTokens.value)
                        }
                    }
                }
                updateCloudUserTokens(userId, _userTokens.value)

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

                // Log audit transaction
                recordTransaction(
                    type = "ADMIN_CREDIT",
                    targetUserId = targetUserId,
                    amount = amountToAdd,
                    note = "Admin added $amountToAdd tokens (Previous: $current -> New: $newTotal)"
                )

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
     * Prioritizes the active user device wallet (userId) where purchased packages reside,
     * or deducts from the developer wallet (devId) if it holds enough tokens.
     */
    fun deductForPremiumUpload(devId: String, onResult: (Boolean, String) -> Unit) {
        val cost = PREMIUM_UPLOAD_COST

        scope.launch {
            try {
                // 1. Fetch live balance from active user device wallet (userId)
                val userReq = Request.Builder()
                    .url("$rtdbBaseUrl/userTokens/$userId.json")
                    .get()
                    .build()
                val userResp = httpClient.newCall(userReq).execute()
                val userBal = if (userResp.isSuccessful) {
                    val text = userResp.body?.string()?.trim()
                    if (!text.isNullOrBlank() && text != "null") text.toIntOrNull() ?: _userTokens.value else _userTokens.value
                } else _userTokens.value

                // 2. Also check developer account wallet if distinct from userId
                var devBal = 0
                if (devId.isNotBlank() && devId != userId) {
                    try {
                        val devReq = Request.Builder()
                            .url("$rtdbBaseUrl/userTokens/$devId.json")
                            .get()
                            .build()
                        val devResp = httpClient.newCall(devReq).execute()
                        if (devResp.isSuccessful) {
                            val text = devResp.body?.string()?.trim()
                            if (!text.isNullOrBlank() && text != "null") devBal = text.toIntOrNull() ?: 0
                        }
                    } catch (_: Exception) {}
                }

                // Determine target wallet: prefer user's device wallet holding tokens
                val targetWalletId: String
                val currentBal: Int
                if (userBal >= cost) {
                    targetWalletId = userId
                    currentBal = userBal
                } else if (devBal >= cost) {
                    targetWalletId = devId
                    currentBal = devBal
                } else {
                    val available = maxOf(userBal, devBal)
                    withContext(Dispatchers.Main) {
                        onResult(
                            false,
                            "Insufficient tokens! You need $cost Nova Tokens to list a Premium APK. Current balance: $available tokens."
                        )
                    }
                    return@launch
                }

                if (targetWalletId == userId) {
                    spendTokensFromUser(cost)
                } else {
                    val newBal = currentBal - cost
                    updateCloudUserTokens(targetWalletId, newBal)
                }

                // Log audit transaction
                recordTransaction(
                    type = "PREMIUM_UPLOAD",
                    targetUserId = targetWalletId,
                    amount = cost,
                    devId = if (devId.isNotBlank()) devId else targetWalletId,
                    note = "Deducted $cost tokens for Premium APK listing"
                )

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
     * - Deducts 10 tokens from user (spending expiring referral tokens first).
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

                // 2. Deduct 10 tokens from user (consumes 30-day referral grants first, then lifetime purchased tokens)
                spendTokensFromUser(cost)

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

                // 6. Record audit transaction
                recordTransaction(
                    type = "PREMIUM_DOWNLOAD",
                    targetUserId = userId,
                    amount = cost,
                    devId = devId,
                    appId = app.appId,
                    appName = app.appName,
                    note = "Downloaded ${app.appName} ($cost tokens total: $DEV_REWARD_TOKENS to dev $devId, $ADMIN_COMMISSION_TOKENS to Admin)"
                )

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
     * Fetches complete Token Economy Summary for the Admin Console:
     * - Total tokens in circulation across all user wallets + admin platform wallet
     * - Active wallets count
     * - Recent transactions audit log
     * - Top earning developers based on token rewards and premium listings
     * - Complete referral audit logs (who referred whom, codes, tokens rewarded, expiry)
     */
    suspend fun fetchTokenEconomyData(apps: List<ApplicationItem>): TokenEconomySummary = withContext(Dispatchers.IO) {
        var totalInCirculation = _adminTokens.value
        var activeWallets = 0
        val devBalances = mutableMapOf<String, Int>()

        // 1. Fetch user tokens
        try {
            val req = Request.Builder().url("$rtdbBaseUrl/userTokens.json").get().build()
            httpClient.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) {
                    val body = resp.body?.string()?.trim()
                    if (!body.isNullOrBlank() && body.startsWith("{")) {
                        val json = JSONObject(body)
                        val keys = json.keys()
                        while (keys.hasNext()) {
                            val uid = keys.next()
                            val bal = json.optInt(uid, 0)
                            if (bal > 0) {
                                totalInCirculation += bal
                                activeWallets++
                            }
                            devBalances[uid] = bal
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(tag, "fetchTokenEconomy users error: ${e.message}")
        }

        // 2. Fetch recent transactions
        val transactionsList = mutableListOf<TokenTransaction>()
        try {
            val txReq = Request.Builder().url("$rtdbBaseUrl/tokenTransactions.json").get().build()
            httpClient.newCall(txReq).execute().use { resp ->
                if (resp.isSuccessful) {
                    val body = resp.body?.string()?.trim()
                    if (!body.isNullOrBlank() && body.startsWith("{")) {
                        val json = JSONObject(body)
                        val keys = json.keys()
                        while (keys.hasNext()) {
                            val k = keys.next()
                            val txObj = json.optJSONObject(k)
                            if (txObj != null) {
                                transactionsList.add(TokenTransaction.fromJson(txObj))
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(tag, "fetchTokenEconomy tx error: ${e.message}")
        }

        // Sort newest first
        transactionsList.sortByDescending { it.timestamp }

        // 3. Compute Top Earning Developers
        val devEarningsMap = mutableMapOf<String, Int>()
        transactionsList.filter { it.type == "PREMIUM_DOWNLOAD" && !it.developerId.isNullOrBlank() }.forEach { tx ->
            val dId = tx.developerId ?: ""
            devEarningsMap[dId] = (devEarningsMap[dId] ?: 0) + DEV_REWARD_TOKENS
        }

        val devAppsMap = apps.filter { it.isPremium }.groupBy { it.developerId }
        val allDevIds = (devEarningsMap.keys + devAppsMap.keys).filter { it.isNotBlank() }.toSet()
        val topDevelopers = mutableListOf<DeveloperEarning>()

        for (dId in allDevIds) {
            val devApps = devAppsMap[dId] ?: emptyList()
            val devName = devApps.firstOrNull()?.developerName ?: dId
            val tokensFromDownloads = devEarningsMap[dId] ?: 0
            val currentWallet = devBalances[dId] ?: 0
            val totalEarned = (tokensFromDownloads).coerceAtLeast(currentWallet)

            topDevelopers.add(
                DeveloperEarning(
                    developerId = dId,
                    developerName = devName,
                    totalTokensEarned = totalEarned,
                    premiumAppsCount = devApps.size
                )
            )
        }

        topDevelopers.sortByDescending { it.totalTokensEarned }

        // 4. Fetch referral audit logs
        val referralLogsList = mutableListOf<ReferralLog>()
        var totalReferralTokens = 0
        try {
            val refReq = Request.Builder().url("$rtdbBaseUrl/referralLogs.json").get().build()
            httpClient.newCall(refReq).execute().use { resp ->
                if (resp.isSuccessful) {
                    val body = resp.body?.string()?.trim()
                    if (!body.isNullOrBlank() && body.startsWith("{")) {
                        val json = JSONObject(body)
                        val keys = json.keys()
                        while (keys.hasNext()) {
                            val k = keys.next()
                            val refObj = json.optJSONObject(k)
                            if (refObj != null) {
                                val log = ReferralLog.fromJson(refObj)
                                referralLogsList.add(log)
                                totalReferralTokens += (log.tokensToReferee + log.tokensToReferrer)
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(tag, "fetchTokenEconomy referralLogs error: ${e.message}")
        }
        referralLogsList.sortByDescending { it.timestamp }

        TokenEconomySummary(
            totalTokensInCirculation = totalInCirculation,
            adminWalletBalance = _adminTokens.value,
            activeWalletsCount = activeWallets.coerceAtLeast(1),
            recentTransactions = transactionsList.take(30),
            topDevelopers = topDevelopers.take(10),
            referralLogs = referralLogsList,
            totalReferralTokensGranted = totalReferralTokens
        )
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

        // Referral Rules
        const val REFEREE_BONUS_TOKENS = 20
        const val REFERRER_REWARD_TOKENS = 10
        const val REFERRAL_VALIDITY_DAYS = 30
        const val REFERRAL_VALIDITY_MS = 30L * 24 * 60 * 60 * 1000L
    }
}
