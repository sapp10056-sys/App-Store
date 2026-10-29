package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppRepository
import com.example.data.PreferencesManager
import com.example.data.UpdateChecker
import com.example.model.AdMobConfig
import com.example.model.AppReport
import com.example.model.AppUpdateInfo
import com.example.model.ApplicationItem
import com.example.model.StoreCategories
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

sealed class Screen {
    data object Disclaimer : Screen()
    data object Home : Screen()
    data class Details(val appId: String) : Screen()
    data object Publish : Screen()
    data object Settings : Screen()
    data object Admin : Screen()
}

data class PublishFormState(
    val targetAppId: String? = null,
    val appName: String = "",
    val developerName: String = "",
    val logoUrl: String = "",
    val shortDescription: String = "",
    val fullDescription: String = "",
    val category: String = "Tools",
    val versionName: String = "1.0.0",
    val versionCode: String = "1",
    val minAndroidVersion: String = "Android 8.0 (API 26)",
    val downloadUrl: String = "",
    val changelog: String = "",
    val supportUrl: String = "",
    val permissionsDisclosure: String = "",
    val screenshotUrls: List<String> = emptyList(),
    val hasDistributionRights: Boolean = false,
    val isInfoAccurate: Boolean = false,
    val agreesToRules: Boolean = false,
    val isSubmitting: Boolean = false,
    val submissionError: String? = null,
    val submissionSuccess: Boolean = false,
    val lastPublishedApp: ApplicationItem? = null
) {
    val isAppNameValid: Boolean get() = appName.trim().length >= 2
    val isDevNameValid: Boolean get() = developerName.trim().length >= 2
    val isLogoUrlValid: Boolean get() = logoUrl.isBlank() || logoUrl.trim().length >= 6
    val isShortDescValid: Boolean get() = shortDescription.trim().length in 3..150
    val isFullDescValid: Boolean get() = fullDescription.trim().length >= 5
    val isVersionNameValid: Boolean get() = versionName.trim().isNotEmpty()
    val isVersionCodeValid: Boolean get() = (versionCode.toLongOrNull() ?: 0L) > 0L
    val isDownloadUrlValid: Boolean get() = (downloadUrl.startsWith("https://", ignoreCase = true) || downloadUrl.startsWith("t.me/", ignoreCase = true)) && downloadUrl.trim().length >= 8
    val isSupportValid: Boolean get() = supportUrl.trim().length >= 3
    val isPermissionsValid: Boolean get() = permissionsDisclosure.trim().length >= 3
    val areCheckboxesChecked: Boolean get() = hasDistributionRights && isInfoAccurate && agreesToRules

    val isFormValid: Boolean get() = isAppNameValid &&
            isDevNameValid &&
            isLogoUrlValid &&
            isShortDescValid &&
            isFullDescValid &&
            isVersionNameValid &&
            isVersionCodeValid &&
            isDownloadUrlValid &&
            isSupportValid &&
            isPermissionsValid &&
            areCheckboxesChecked
}

class StoreViewModel(application: Application) : AndroidViewModel(application) {
    val preferencesManager = PreferencesManager(application)
    val repository = AppRepository(application)
    private val updateChecker = UpdateChecker(application, preferencesManager)

    private val _currentScreen = MutableStateFlow<Screen>(
        if (preferencesManager.isDisclaimerAccepted()) Screen.Home else Screen.Disclaimer
    )
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow(StoreCategories.ALL)
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _selectedAppId = MutableStateFlow<String?>(null)
    val selectedAppId: StateFlow<String?> = _selectedAppId.asStateFlow()

    private val _pendingDownloadApp = MutableStateFlow<ApplicationItem?>(null)
    val pendingDownloadApp: StateFlow<ApplicationItem?> = _pendingDownloadApp.asStateFlow()

    private val _pendingReportApp = MutableStateFlow<ApplicationItem?>(null)
    val pendingReportApp: StateFlow<ApplicationItem?> = _pendingReportApp.asStateFlow()

    private val _updateInfo = MutableStateFlow<AppUpdateInfo?>(null)
    val updateInfo: StateFlow<AppUpdateInfo?> = _updateInfo.asStateFlow()

    private val _updateSyncError = MutableStateFlow<String?>(null)
    val updateSyncError: StateFlow<String?> = _updateSyncError.asStateFlow()

    private val _isRemoteLive = MutableStateFlow(false)
    val isRemoteLive: StateFlow<Boolean> = _isRemoteLive.asStateFlow()

    private val _isCheckingUpdate = MutableStateFlow(false)
    val isCheckingUpdate: StateFlow<Boolean> = _isCheckingUpdate.asStateFlow()

    private val _publishForm = MutableStateFlow(PublishFormState())
    val publishForm: StateFlow<PublishFormState> = _publishForm.asStateFlow()

    private val _isAdminLoggedIn = MutableStateFlow(preferencesManager.isAdminLoggedIn())
    val isAdminLoggedIn: StateFlow<Boolean> = _isAdminLoggedIn.asStateFlow()

    private val _telegramBotUrl = MutableStateFlow(preferencesManager.getTelegramBotUrl())
    val telegramBotUrl: StateFlow<String> = _telegramBotUrl.asStateFlow()

    private val _admobConfig = MutableStateFlow(
        AdMobConfig(
            enabled = preferencesManager.isAdMobEnabled(),
            bannerAdUnitId = preferencesManager.getAdMobBannerUnitId(),
            rewardedAdUnitId = preferencesManager.getAdMobRewardedUnitId()
        )
    )
    val admobConfig: StateFlow<AdMobConfig> = _admobConfig.asStateFlow()

    // Filtered applications stream based on search query and category
    val filteredApplications: StateFlow<List<ApplicationItem>> = combine(
        repository.applications,
        _searchQuery,
        _selectedCategory
    ) { apps, query, category ->
        val trimmed = query.trim().lowercase()
        apps.filter { app ->
            val matchesCategory = category == StoreCategories.ALL || app.category.equals(category, ignoreCase = true)
            val matchesQuery = trimmed.isEmpty() ||
                    app.appName.lowercase().contains(trimmed) ||
                    app.developerName.lowercase().contains(trimmed) ||
                    app.shortDescription.lowercase().contains(trimmed) ||
                    app.fullDescription.lowercase().contains(trimmed)
            matchesCategory && matchesQuery
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val featuredApplications: StateFlow<List<ApplicationItem>> = repository.applications.combine(_searchQuery) { apps, query ->
        if (query.isNotBlank()) emptyList() else apps.filter { it.isFeatured || it.downloadCount > 30000L }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val trendingApplications: StateFlow<List<ApplicationItem>> = repository.applications.combine(_searchQuery) { apps, query ->
        if (query.isNotBlank()) emptyList() else apps.filter { it.isTrending || it.rating >= 4.7f }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        checkAppUpdates()
        startPeriodicUpdatePolling()
    }

    private fun startPeriodicUpdatePolling() {
        viewModelScope.launch {
            while (true) {
                delay(8000L) // Poll remote GitHub JSON every 8 seconds to reflect changes live
                checkAppUpdates(silent = true)
            }
        }
    }

    fun navigateTo(screen: Screen) {
        _currentScreen.value = screen
        if (screen is Screen.Settings || screen is Screen.Publish || screen is Screen.Admin) {
            checkAppUpdates()
        }
    }

    fun openAppDetails(appId: String) {
        _selectedAppId.value = appId
        _currentScreen.value = Screen.Details(appId)
    }

    private var pendingDeepLinkScreen: Screen? = null

    fun handleDeepLink(appId: String) {
        if (!preferencesManager.isDisclaimerAccepted()) {
            pendingDeepLinkScreen = Screen.Details(appId)
        } else {
            openAppDetails(appId)
        }
    }

    fun acceptDisclaimer() {
        preferencesManager.setDisclaimerAccepted(true)
        val target = pendingDeepLinkScreen ?: Screen.Home
        pendingDeepLinkScreen = null
        _currentScreen.value = target
    }

    fun refreshCatalog() {
        viewModelScope.launch {
            repository.refreshCatalog()
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectCategory(category: String) {
        _selectedCategory.value = category
    }

    fun requestDownload(app: ApplicationItem) {
        _pendingDownloadApp.value = app
    }

    fun dismissDownloadDialog() {
        _pendingDownloadApp.value = null
    }

    fun requestReport(app: ApplicationItem) {
        _pendingReportApp.value = app
    }

    fun dismissReportDialog() {
        _pendingReportApp.value = null
    }

    fun submitReport(reason: String, details: String) {
        val app = _pendingReportApp.value ?: return
        viewModelScope.launch {
            val report = AppReport(
                appId = app.appId,
                appName = app.appName,
                reason = reason,
                details = details
            )
            repository.submitReport(report)
            _pendingReportApp.value = null
        }
    }

    fun checkAppUpdates(silent: Boolean = false) {
        viewModelScope.launch {
            if (!silent) _isCheckingUpdate.value = true
            try {
                val result = updateChecker.checkForUpdate(currentVersionCode = com.example.BuildConfig.VERSION_CODE)
                _isRemoteLive.value = updateChecker.isRemoteLive
                _updateSyncError.value = updateChecker.lastSyncError

                _telegramBotUrl.value = preferencesManager.getTelegramBotUrl()
                _admobConfig.value = AdMobConfig(
                    enabled = preferencesManager.isAdMobEnabled(),
                    bannerAdUnitId = preferencesManager.getAdMobBannerUnitId(),
                    rewardedAdUnitId = preferencesManager.getAdMobRewardedUnitId()
                )
                result.onSuccess { info ->
                    _updateInfo.value = info
                    _telegramBotUrl.value = preferencesManager.getTelegramBotUrl()
                    _admobConfig.value = AdMobConfig(
                        enabled = preferencesManager.isAdMobEnabled(),
                        bannerAdUnitId = preferencesManager.getAdMobBannerUnitId(),
                        rewardedAdUnitId = preferencesManager.getAdMobRewardedUnitId()
                    )
                }.onFailure { err ->
                    _updateSyncError.value = err.message
                }
            } finally {
                if (!silent) _isCheckingUpdate.value = false
            }
        }
    }

    fun dismissUpdateDialog() {
        if (_updateInfo.value?.forceUpdate == true) {
            // Cannot dismiss mandatory force update dialog
            return
        }
        _updateInfo.value = null
    }

    fun publishCloudUpdateConfig(
        downloadUrl: String,
        latestVersionCode: Int = 2,
        latestVersionName: String = "2.0.0",
        minimumSupportedVersionCode: Int = 2,
        releaseNotes: String = "NovaStore v2.0.0 is required. Please download and install the new APK version to continue using NovaStore.",
        onComplete: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val json = JSONObject().apply {
                    put("latestVersionCode", latestVersionCode)
                    put("latestVersionName", latestVersionName)
                    put("minimumSupportedVersionCode", minimumSupportedVersionCode)
                    put("forceUpdate", false)
                    put("downloadUrl", downloadUrl.trim())
                    put("releaseNotes", releaseNotes.trim())
                    put("telegramBotUrl", preferencesManager.getTelegramBotUrl())
                    val adminObj = JSONObject().apply {
                        put("username", preferencesManager.getAdminUsername())
                        put("password", preferencesManager.getAdminPassword())
                    }
                    put("admin", adminObj)
                    val admobObj = JSONObject().apply {
                        put("enabled", preferencesManager.isAdMobEnabled())
                        put("bannerAdUnitId", preferencesManager.getAdMobBannerUnitId())
                        put("rewardedAdUnitId", preferencesManager.getAdMobRewardedUnitId())
                    }
                    put("admob", admobObj)
                }

                val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
                val body = json.toString().toRequestBody(mediaType)
                val request = Request.Builder()
                    .url("https://app-store-a9559-default-rtdb.asia-southeast1.firebasedatabase.app/appUpdate.json")
                    .put(body)
                    .build()

                val client = OkHttpClient()
                val response = client.newCall(request).execute()
                val success = response.isSuccessful
                if (success) {
                    checkAppUpdates(silent = true)
                }
                withContext(Dispatchers.Main) {
                    onComplete(success, if (success) null else "Server returned HTTP ${response.code}")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onComplete(false, e.message)
                }
            }
        }
    }

    // Admin Authentication against remote GitHub JSON config
    fun loginAdmin(user: String, pass: String): Boolean {
        val expectedUser = preferencesManager.getAdminUsername().trim()
        val expectedPass = preferencesManager.getAdminPassword().trim()

        return if (user.trim() == expectedUser && pass.trim() == expectedPass) {
            preferencesManager.setAdminLoggedIn(true)
            _isAdminLoggedIn.value = true
            true
        } else {
            false
        }
    }

    fun logoutAdmin() {
        preferencesManager.setAdminLoggedIn(false)
        _isAdminLoggedIn.value = false
        if (_currentScreen.value is Screen.Admin) {
            _currentScreen.value = Screen.Settings
        }
    }

    fun updateAppAsAdmin(updatedApp: ApplicationItem, onComplete: () -> Unit) {
        if (!_isAdminLoggedIn.value) return
        viewModelScope.launch {
            repository.updateApp(updatedApp)
            onComplete()
        }
    }

    fun deleteAppAsAdmin(appId: String, onComplete: () -> Unit) {
        if (!_isAdminLoggedIn.value) return
        viewModelScope.launch {
            repository.deleteApp(appId)
            onComplete()
        }
    }

    // Developer Authentication & Authorization
    val currentDeveloper = repository.developerSessionManager.currentDeveloper

    fun registerDeveloper(devId: String, devName: String, pass: String, contact: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val res = repository.registerDeveloper(devId, devName, pass, contact)
            res.onSuccess {
                _publishForm.value = _publishForm.value.copy(developerName = it.developerName)
                onResult(true, null)
            }.onFailure {
                onResult(false, it.message ?: "Registration failed")
            }
        }
    }

    fun loginDeveloper(devId: String, pass: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val res = repository.loginDeveloper(devId, pass)
            res.onSuccess {
                _publishForm.value = _publishForm.value.copy(developerName = it.developerName)
                onResult(true, null)
            }.onFailure {
                onResult(false, it.message ?: "Invalid Developer credentials")
            }
        }
    }

    fun logoutDeveloper() {
        repository.developerSessionManager.logout()
    }

    fun prefillForAppUpdate(app: ApplicationItem) {
        val dev = repository.developerSessionManager.currentDeveloper.value
        val nextVersionCode = (app.versionCode + 1).toString()
        _publishForm.value = PublishFormState(
            targetAppId = app.appId,
            appName = app.appName,
            developerName = if (dev != null) dev.developerName else app.developerName,
            logoUrl = app.logoUrl,
            shortDescription = app.shortDescription,
            fullDescription = app.fullDescription,
            category = app.category,
            versionName = "", // Developer enters new version string (e.g. 2.0.0)
            versionCode = nextVersionCode,
            minAndroidVersion = app.minAndroidVersion,
            downloadUrl = "", // Developer enters new download link
            changelog = "Version update",
            supportUrl = app.supportUrl,
            permissionsDisclosure = app.permissionsDisclosure,
            screenshotUrls = app.screenshotUrls,
            hasDistributionRights = true,
            isInfoAccurate = true,
            agreesToRules = true
        )
        _currentScreen.value = Screen.Publish
    }

    // Publish Form Mutators
    fun updatePublishAppName(value: String) { _publishForm.value = _publishForm.value.copy(appName = value) }
    fun updatePublishDevName(value: String) { _publishForm.value = _publishForm.value.copy(developerName = value) }
    fun updatePublishLogoUrl(value: String) { _publishForm.value = _publishForm.value.copy(logoUrl = value) }
    fun updatePublishShortDesc(value: String) { _publishForm.value = _publishForm.value.copy(shortDescription = value) }
    fun updatePublishFullDesc(value: String) { _publishForm.value = _publishForm.value.copy(fullDescription = value) }
    fun updatePublishCategory(value: String) { _publishForm.value = _publishForm.value.copy(category = value) }
    fun updatePublishVersionName(value: String) { _publishForm.value = _publishForm.value.copy(versionName = value) }
    fun updatePublishVersionCode(value: String) { _publishForm.value = _publishForm.value.copy(versionCode = value) }
    fun updatePublishMinAndroid(value: String) { _publishForm.value = _publishForm.value.copy(minAndroidVersion = value) }
    fun updatePublishDownloadUrl(value: String) { _publishForm.value = _publishForm.value.copy(downloadUrl = value) }
    fun updatePublishChangelog(value: String) { _publishForm.value = _publishForm.value.copy(changelog = value) }
    fun updatePublishSupport(value: String) { _publishForm.value = _publishForm.value.copy(supportUrl = value) }
    fun updatePublishPermissions(value: String) { _publishForm.value = _publishForm.value.copy(permissionsDisclosure = value) }
    fun addScreenshotUrl(url: String) {
        if (url.startsWith("https://", ignoreCase = true)) {
            _publishForm.value = _publishForm.value.copy(screenshotUrls = _publishForm.value.screenshotUrls + url.trim())
        }
    }
    fun removeScreenshotUrl(index: Int) {
        val list = _publishForm.value.screenshotUrls.toMutableList()
        if (index in list.indices) {
            list.removeAt(index)
            _publishForm.value = _publishForm.value.copy(screenshotUrls = list)
        }
    }
    fun toggleDistributionRights(checked: Boolean) { _publishForm.value = _publishForm.value.copy(hasDistributionRights = checked) }
    fun toggleInfoAccurate(checked: Boolean) { _publishForm.value = _publishForm.value.copy(isInfoAccurate = checked) }
    fun toggleAgreesToRules(checked: Boolean) { _publishForm.value = _publishForm.value.copy(agreesToRules = checked) }

    fun submitPublishForm(onSuccess: () -> Unit) {
        val form = _publishForm.value
        if (!form.isFormValid) return

        val activeDev = repository.developerSessionManager.currentDeveloper.value

        _publishForm.value = form.copy(isSubmitting = true, submissionError = null)
        viewModelScope.launch {
            val normalizedDownload = when {
                form.downloadUrl.trim().startsWith("http://", ignoreCase = true) ||
                form.downloadUrl.trim().startsWith("https://", ignoreCase = true) -> form.downloadUrl.trim()
                form.downloadUrl.trim().isNotEmpty() -> "https://" + form.downloadUrl.trim()
                else -> "https://t.me/novastoreapk"
            }

            val normalizedLogo = when {
                form.logoUrl.trim().startsWith("http://", ignoreCase = true) ||
                form.logoUrl.trim().startsWith("https://", ignoreCase = true) -> form.logoUrl.trim()
                form.logoUrl.trim().isNotEmpty() -> "https://" + form.logoUrl.trim()
                else -> "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=256&auto=format&fit=crop&q=80"
            }

            val app = ApplicationItem(
                appId = form.targetAppId ?: "",
                appName = form.appName.trim(),
                developerId = activeDev?.developerId ?: "",
                developerName = activeDev?.developerName ?: form.developerName.trim(),
                developerEmail = "",
                logoUrl = normalizedLogo,
                shortDescription = form.shortDescription.trim(),
                fullDescription = form.fullDescription.trim(),
                category = form.category,
                versionName = form.versionName.trim(),
                versionCode = form.versionCode.toLongOrNull() ?: 1L,
                minAndroidVersion = form.minAndroidVersion,
                downloadUrl = normalizedDownload,
                privacyPolicyUrl = "",
                supportUrl = form.supportUrl.trim(),
                permissionsDisclosure = form.permissionsDisclosure.trim(),
                screenshotUrls = form.screenshotUrls,
                publicationStatus = "published"
            )

            val result = repository.publishApplication(app, activeDev)
            result.onSuccess { publishedId ->
                val publishedApp = app.copy(
                    appId = publishedId,
                    developerId = activeDev?.developerId ?: app.developerId,
                    developerName = activeDev?.developerName ?: app.developerName
                )
                _publishForm.value = PublishFormState(
                    submissionSuccess = true,
                    lastPublishedApp = publishedApp
                )
                onSuccess()
            }.onFailure { err ->
                _publishForm.value = _publishForm.value.copy(
                    isSubmitting = false,
                    submissionError = err.message ?: "Failed to publish listing"
                )
            }
        }
    }

    fun resetPublishSuccess() {
        _publishForm.value = PublishFormState()
    }
}
