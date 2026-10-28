package com.example.util

import android.content.Context
import com.example.model.ApplicationItem
import com.example.model.DeveloperAccount
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest

/**
 * Handles Developer Authentication, Account Registration, and Access Control.
 * Ensures Developer A cannot edit or overwrite Developer B's uploaded APKs.
 */
class DeveloperSessionManager(private val context: Context) {
    private val prefs = context.getSharedPreferences("novastore_developer_session", Context.MODE_PRIVATE)
    private val accountsFile = File(context.filesDir, "developer_accounts.json")

    private val _currentDeveloper = MutableStateFlow<DeveloperAccount?>(null)
    val currentDeveloper: StateFlow<DeveloperAccount?> = _currentDeveloper.asStateFlow()

    init {
        restoreSession()
    }

    private fun hashPassword(password: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(password.trim().toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun restoreSession() {
        val devId = prefs.getString("active_developer_id", null)
        val devName = prefs.getString("active_developer_name", null)
        val passHash = prefs.getString("active_password_hash", null)
        val contact = prefs.getString("active_contact", null)

        if (!devId.isNullOrBlank() && !passHash.isNullOrBlank()) {
            _currentDeveloper.value = DeveloperAccount(
                developerId = devId,
                developerName = devName ?: devId,
                passwordHash = passHash,
                contactUrl = contact ?: ""
            )
        }
    }

    fun getAllLocalAccounts(): List<DeveloperAccount> {
        return try {
            if (accountsFile.exists()) {
                val text = accountsFile.readText()
                if (text.isNotBlank()) {
                    val arr = JSONArray(text)
                    val list = mutableListOf<DeveloperAccount>()
                    for (i in 0 until arr.length()) {
                        val obj = arr.optJSONObject(i)
                        if (obj != null) {
                            list.add(DeveloperAccount.fromJson(obj))
                        }
                    }
                    list
                } else emptyList()
            } else emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun saveLocalAccounts(accounts: List<DeveloperAccount>) {
        try {
            val arr = JSONArray()
            accounts.forEach { arr.put(it.toJson()) }
            accountsFile.writeText(arr.toString())
        } catch (_: Exception) {}
    }

    /**
     * Registers a new developer with their chosen Developer ID, Name, and Password.
     */
    fun registerDeveloper(
        developerId: String,
        developerName: String,
        password: String,
        contactUrl: String = ""
    ): Result<DeveloperAccount> {
        val cleanId = developerId.trim().lowercase().replace(" ", "_")
        if (cleanId.length < 3) {
            return Result.failure(IllegalArgumentException("Developer ID must be at least 3 characters"))
        }
        if (password.trim().length < 4) {
            return Result.failure(IllegalArgumentException("Password must be at least 4 characters"))
        }

        val all = getAllLocalAccounts().toMutableList()
        if (all.any { it.developerId.equals(cleanId, ignoreCase = true) }) {
            return Result.failure(IllegalArgumentException("Developer ID '@$cleanId' is already registered. Please login."))
        }

        val newAccount = DeveloperAccount(
            developerId = cleanId,
            developerName = developerName.trim().ifBlank { cleanId },
            passwordHash = hashPassword(password),
            contactUrl = contactUrl.trim(),
            registeredAt = System.currentTimeMillis()
        )

        all.add(newAccount)
        saveLocalAccounts(all)

        // Set as active session
        setActiveSession(newAccount)
        return Result.success(newAccount)
    }

    /**
     * Logs in an existing developer using their Developer ID and Password.
     */
    fun loginDeveloper(developerId: String, password: String): Result<DeveloperAccount> {
        val cleanId = developerId.trim().lowercase().replace(" ", "_")
        val expectedHash = hashPassword(password)

        val account = getAllLocalAccounts().find {
            it.developerId.equals(cleanId, ignoreCase = true) && it.passwordHash == expectedHash
        }

        return if (account != null) {
            setActiveSession(account)
            Result.success(account)
        } else {
            Result.failure(IllegalArgumentException("Invalid Developer ID or Password."))
        }
    }

    private fun setActiveSession(account: DeveloperAccount) {
        prefs.edit()
            .putString("active_developer_id", account.developerId)
            .putString("active_developer_name", account.developerName)
            .putString("active_password_hash", account.passwordHash)
            .putString("active_contact", account.contactUrl)
            .apply()
        _currentDeveloper.value = account
    }

    fun logout() {
        prefs.edit().clear().apply()
        _currentDeveloper.value = null
    }

    fun isLoggedIn(): Boolean = _currentDeveloper.value != null

    /**
     * Verifies if the currently logged-in developer is authorized to edit or update the target app.
     * Developer A CANNOT edit Developer B's app!
     */
    fun canEditApp(app: ApplicationItem): Boolean {
        val current = _currentDeveloper.value ?: return false
        // If app has no owner yet, current registered developer can claim it
        if (app.developerId.isBlank()) return true
        // Otherwise must match developerId exactly
        return app.developerId.equals(current.developerId, ignoreCase = true)
    }

    /**
     * Import cloud accounts (so developer can login from any device).
     */
    fun mergeCloudAccounts(cloudAccounts: List<DeveloperAccount>) {
        val local = getAllLocalAccounts().toMutableMap { it.developerId }
        cloudAccounts.forEach { cloud ->
            if (!local.containsKey(cloud.developerId)) {
                local[cloud.developerId] = cloud
            }
        }
        saveLocalAccounts(local.values.toList())
    }

    private inline fun <T, K> Iterable<T>.toMutableMap(keySelector: (T) -> K): MutableMap<K, T> {
        val destination = LinkedHashMap<K, T>()
        for (element in this) {
            destination[keySelector(element)] = element
        }
        return destination
    }
}
