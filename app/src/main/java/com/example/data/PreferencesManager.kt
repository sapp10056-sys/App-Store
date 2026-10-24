package com.example.data

import android.content.Context
import android.content.SharedPreferences

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isDisclaimerAccepted(): Boolean {
        return prefs.getBoolean(KEY_DISCLAIMER_ACCEPTED, false)
    }

    fun setDisclaimerAccepted(accepted: Boolean) {
        prefs.edit().putBoolean(KEY_DISCLAIMER_ACCEPTED, accepted).apply()
    }

    fun getTelegramBotUrl(): String {
        return prefs.getString(KEY_TELEGRAM_BOT_URL, DEFAULT_TELEGRAM_BOT_URL) ?: DEFAULT_TELEGRAM_BOT_URL
    }

    fun setTelegramBotUrl(url: String) {
        prefs.edit().putString(KEY_TELEGRAM_BOT_URL, url).apply()
    }

    fun getUpdateConfigUrl(): String {
        return prefs.getString(KEY_UPDATE_CONFIG_URL, DEFAULT_UPDATE_CONFIG_URL) ?: DEFAULT_UPDATE_CONFIG_URL
    }

    fun setUpdateConfigUrl(url: String) {
        prefs.edit().putString(KEY_UPDATE_CONFIG_URL, url).apply()
    }

    // Remote Admin Credentials fetched from GitHub JSON
    fun getAdminUsername(): String {
        return prefs.getString(KEY_ADMIN_USERNAME, DEFAULT_ADMIN_USERNAME) ?: DEFAULT_ADMIN_USERNAME
    }

    fun setAdminUsername(username: String) {
        prefs.edit().putString(KEY_ADMIN_USERNAME, username).apply()
    }

    fun getAdminPassword(): String {
        return prefs.getString(KEY_ADMIN_PASSWORD, DEFAULT_ADMIN_PASSWORD) ?: DEFAULT_ADMIN_PASSWORD
    }

    fun setAdminPassword(password: String) {
        prefs.edit().putString(KEY_ADMIN_PASSWORD, password).apply()
    }

    fun isAdminLoggedIn(): Boolean {
        return prefs.getBoolean(KEY_ADMIN_LOGGED_IN, false)
    }

    fun setAdminLoggedIn(loggedIn: Boolean) {
        prefs.edit().putBoolean(KEY_ADMIN_LOGGED_IN, loggedIn).apply()
    }

    companion object {
        private const val PREFS_NAME = "novastore_preferences"
        private const val KEY_DISCLAIMER_ACCEPTED = "disclaimer_accepted"
        private const val KEY_TELEGRAM_BOT_URL = "telegram_bot_url"
        private const val KEY_UPDATE_CONFIG_URL = "update_config_url"
        private const val KEY_ADMIN_USERNAME = "admin_username"
        private const val KEY_ADMIN_PASSWORD = "admin_password"
        private const val KEY_ADMIN_LOGGED_IN = "admin_logged_in"

        const val DEFAULT_TELEGRAM_BOT_URL = "https://t.me/YOUR_BOT_USERNAME"
        const val DEFAULT_UPDATE_CONFIG_URL = "https://raw.githubusercontent.com/username/NovaStore/main/app-update.json"
        const val DEFAULT_ADMIN_USERNAME = "admin"
        const val DEFAULT_ADMIN_PASSWORD = "admin1234"
    }
}
