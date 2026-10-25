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

    fun isAdMobEnabled(): Boolean {
        return prefs.getBoolean(KEY_ADMOB_ENABLED, DEFAULT_ADMOB_ENABLED)
    }

    fun setAdMobEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_ADMOB_ENABLED, enabled).apply()
    }

    fun getAdMobBannerUnitId(): String {
        return prefs.getString(KEY_ADMOB_BANNER_UNIT_ID, DEFAULT_ADMOB_BANNER_UNIT_ID) ?: DEFAULT_ADMOB_BANNER_UNIT_ID
    }

    fun setAdMobBannerUnitId(unitId: String) {
        prefs.edit().putString(KEY_ADMOB_BANNER_UNIT_ID, unitId).apply()
    }

    fun getAdMobRewardedUnitId(): String {
        return prefs.getString(KEY_ADMOB_REWARDED_UNIT_ID, DEFAULT_ADMOB_REWARDED_UNIT_ID) ?: DEFAULT_ADMOB_REWARDED_UNIT_ID
    }

    fun setAdMobRewardedUnitId(unitId: String) {
        prefs.edit().putString(KEY_ADMOB_REWARDED_UNIT_ID, unitId).apply()
    }

    companion object {
        private const val PREFS_NAME = "novastore_preferences"
        private const val KEY_DISCLAIMER_ACCEPTED = "disclaimer_accepted"
        private const val KEY_TELEGRAM_BOT_URL = "telegram_bot_url"
        private const val KEY_UPDATE_CONFIG_URL = "update_config_url"
        private const val KEY_ADMIN_USERNAME = "admin_username"
        private const val KEY_ADMIN_PASSWORD = "admin_password"
        private const val KEY_ADMIN_LOGGED_IN = "admin_logged_in"
        private const val KEY_ADMOB_ENABLED = "admob_enabled"
        private const val KEY_ADMOB_BANNER_UNIT_ID = "admob_banner_unit_id"
        private const val KEY_ADMOB_REWARDED_UNIT_ID = "admob_rewarded_unit_id"

        const val DEFAULT_TELEGRAM_BOT_URL = "https://t.me/novastoreapk"
        const val DEFAULT_UPDATE_CONFIG_URL = "https://raw.githubusercontent.com/sapp10056-sys/App-Store/main/app-update.json"
        const val DEFAULT_ADMIN_USERNAME = "admin"
        const val DEFAULT_ADMIN_PASSWORD = "admin1234"
        const val DEFAULT_ADMOB_ENABLED = true
        const val DEFAULT_ADMOB_BANNER_UNIT_ID = "ca-app-pub-5220492832518267/6377280972"
        const val DEFAULT_ADMOB_REWARDED_UNIT_ID = "ca-app-pub-5220492832518267/1149712866"
    }
}
