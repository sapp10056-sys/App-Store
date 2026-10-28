package com.example.data

import android.content.Context
import android.util.Log
import com.example.model.AppReport
import com.example.model.ApplicationItem
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONArray
import java.io.File
import java.util.UUID

class AppRepository(private val context: Context) {
    private val tag = "AppRepository"
    private val diskFile = File(context.filesDir, "persisted_apps.json")

    private val _applications = MutableStateFlow<List<ApplicationItem>>(emptyList())
    val applications: StateFlow<List<ApplicationItem>> = _applications.asStateFlow()

    private val _reports = MutableStateFlow<List<AppReport>>(emptyList())
    val reports: StateFlow<List<AppReport>> = _reports.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private var firestore: FirebaseFirestore? = null
    private var auth: FirebaseAuth? = null
    private var snapshotListener: ListenerRegistration? = null
    private var currentUserId: String = "guest_user"

    init {
        loadInitialApps()
        setupFirebase()
    }

    private fun loadApplicationsFromDisk(): List<ApplicationItem>? {
        return try {
            if (diskFile.exists() && diskFile.canRead()) {
                val text = diskFile.readText()
                if (text.isNotBlank()) {
                    val arr = JSONArray(text)
                    val list = mutableListOf<ApplicationItem>()
                    for (i in 0 until arr.length()) {
                        val obj = arr.optJSONObject(i)
                        if (obj != null) {
                            list.add(ApplicationItem.fromJson(obj))
                        }
                    }
                    if (list.isNotEmpty()) list else null
                } else null
            } else null
        } catch (e: Exception) {
            Log.w(tag, "Failed to load persisted apps: ${e.message}")
            null
        }
    }

    private fun saveApplicationsToDisk(apps: List<ApplicationItem>) {
        try {
            val arr = JSONArray()
            apps.forEach { arr.put(it.toJson()) }
            diskFile.writeText(arr.toString())
            Log.d(tag, "Permanently saved ${apps.size} applications to disk")
        } catch (e: Exception) {
            Log.w(tag, "Failed to persist apps to disk: ${e.message}")
        }
    }

    private fun loadInitialApps() {
        val diskApps = loadApplicationsFromDisk()
        if (!diskApps.isNullOrEmpty()) {
            _applications.value = diskApps
            _isLoading.value = false
        } else {
            val curated = getCuratedSampleApps()
            _applications.value = curated
            saveApplicationsToDisk(curated)
            _isLoading.value = false
        }
    }

    private fun getOrCreateUserId(): String {
        val prefs = context.getSharedPreferences("novastore_user_prefs", Context.MODE_PRIVATE)
        var uid = prefs.getString("user_uid", null)
        if (uid.isNullOrBlank()) {
            uid = "user_" + UUID.randomUUID().toString().replace("-", "").take(12)
            prefs.edit().putString("user_uid", uid).apply()
        }
        return uid
    }

    private fun setupFirebase() {
        try {
            currentUserId = getOrCreateUserId()
            val app = if (FirebaseApp.getApps(context).isEmpty()) {
                FirebaseApp.initializeApp(context) ?: FirebaseApp.getInstance()
            } else {
                FirebaseApp.getInstance()
            }

            val dbId = try {
                context.getString(com.example.R.string.firestore_database_id)
            } catch (e: Exception) {
                ""
            }

            firestore = try {
                if (dbId.isNotBlank()) {
                    FirebaseFirestore.getInstance(app, dbId)
                } else {
                    FirebaseFirestore.getInstance(app)
                }
            } catch (e: Exception) {
                Log.w(tag, "Failed to initialize with dbId $dbId, falling back to default: ${e.message}")
                FirebaseFirestore.getInstance(app)
            }

            listenToApplications()
        } catch (e: Exception) {
            Log.e(tag, "Firebase initialization error: ${e.message}", e)
            _isLoading.value = false
        }
    }

    fun getCurrentUserId(): String = currentUserId

    private fun listenToApplications() {
        val db = firestore ?: return

        snapshotListener?.remove()
        snapshotListener = db.collection("applications")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                _isLoading.value = false
                if (error != null) {
                    Log.w(tag, "Firestore listen error: ${error.message}")
                    return@addSnapshotListener
                }

                if (snapshot != null && !snapshot.isEmpty) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        doc.data?.let { data ->
                            ApplicationItem.fromMap(doc.id, data)
                        }
                    }
                    _applications.value = list
                    saveApplicationsToDisk(list)
                    Log.d(tag, "Loaded ${list.size} apps from shared cloud catalog")
                } else {
                    // Pre-populate initial catalog if cloud collection is empty
                    if (_applications.value.isEmpty()) {
                        val curated = getCuratedSampleApps()
                        _applications.value = curated
                        saveApplicationsToDisk(curated)
                        seedSampleAppsIfEmpty(curated)
                    } else {
                        // Seed existing apps to cloud so other users can see them
                        seedSampleAppsIfEmpty(_applications.value)
                    }
                }
            }
    }

    private fun seedSampleAppsIfEmpty(samples: List<ApplicationItem>) {
        val db = firestore ?: return
        CoroutineScope(Dispatchers.IO).launch {
            try {
                for (app in samples) {
                    db.collection("applications").document(app.appId).set(app.toMap()).await()
                }
                Log.d(tag, "Seeded verified applications to Firestore catalog")
            } catch (e: Exception) {
                Log.w(tag, "Firestore seeding note: ${e.message}")
            }
        }
    }

    suspend fun refreshCatalog() {
        withContext(Dispatchers.IO) {
            try {
                firestore?.let { db ->
                    val snapshot = db.collection("applications")
                        .orderBy("createdAt", Query.Direction.DESCENDING)
                        .get()
                        .await()
                    if (!snapshot.isEmpty) {
                        val list = snapshot.documents.mapNotNull { doc ->
                            doc.data?.let { data ->
                                ApplicationItem.fromMap(doc.id, data)
                            }
                        }
                        if (list.isNotEmpty()) {
                            _applications.value = list
                            saveApplicationsToDisk(list)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(tag, "Manual catalog refresh note: ${e.message}")
            }
        }
    }

    suspend fun publishApplication(app: ApplicationItem): Result<String> {
        return withContext(Dispatchers.IO) {
            // Check if app with same name or same ID already exists to update it!
            val existing = _applications.value.find {
                (app.appId.isNotBlank() && it.appId == app.appId) ||
                it.appName.equals(app.appName, ignoreCase = true)
            }

            val id = existing?.appId ?: if (app.appId.isNotBlank()) app.appId else "app_" + UUID.randomUUID().toString().replace("-", "").take(12)
            val updatedApp = app.copy(
                appId = id,
                submitterUid = if (existing?.submitterUid.isNullOrBlank()) currentUserId else existing!!.submitterUid,
                createdAt = existing?.createdAt ?: System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
                rating = existing?.rating ?: app.rating,
                downloadCount = existing?.downloadCount ?: app.downloadCount,
                publicationStatus = "published"
            )

            // 1. Immediately update in-memory state and persist to disk (instant response)
            val currentList = _applications.value.toMutableList()
            currentList.removeAll { it.appId == id }
            currentList.add(0, updatedApp)
            _applications.value = currentList
            saveApplicationsToDisk(currentList)

            // 2. Synchronize to Firestore so all other users see it immediately
            firestore?.let { db ->
                try {
                    withTimeoutOrNull(6000L) {
                        db.collection("applications").document(id).set(updatedApp.toMap()).await()
                    }
                    Log.d(tag, "Successfully uploaded app $id to shared Firestore catalog")
                } catch (e: Exception) {
                    Log.w(tag, "Firestore publish sync note: ${e.message}")
                }
            }

            Result.success(id)
        }
    }

    suspend fun submitReport(report: AppReport): Result<Unit> {
        return try {
            val db = firestore ?: throw IllegalStateException("Firestore not available")
            val reportId = "rep_" + UUID.randomUUID().toString().replace("-", "").take(12)
            val fullReport = report.copy(
                reportId = reportId,
                reporterUid = currentUserId,
                createdAt = System.currentTimeMillis()
            )
            db.collection("reports").document(reportId).set(fullReport.toMap()).await()

            if (report.appId.isNotBlank()) {
                val appRef = db.collection("applications").document(report.appId)
                db.runTransaction { transaction ->
                    val snapshot = transaction.get(appRef)
                    val currentCount = (snapshot.get("reportCount") as? Number)?.toLong() ?: 0L
                    transaction.update(appRef, "reportCount", currentCount + 1)
                }.await()
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "Failed to submit report: ${e.message}", e)
            Result.success(Unit)
        }
    }

    suspend fun updateAppStatus(appId: String, newStatus: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            val currentList = _applications.value.map {
                if (it.appId == appId) it.copy(publicationStatus = newStatus) else it
            }
            _applications.value = currentList
            saveApplicationsToDisk(currentList)

            firestore?.let { db ->
                try {
                    withTimeoutOrNull(5000L) {
                        db.collection("applications").document(appId).update("publicationStatus", newStatus).await()
                    }
                } catch (e: Exception) {
                    Log.w(tag, "Firestore updateStatus handled: ${e.message}")
                }
            }
            Result.success(Unit)
        }
    }

    suspend fun updateApp(updatedApp: ApplicationItem): Result<Unit> {
        return withContext(Dispatchers.IO) {
            val currentList = _applications.value.map {
                if (it.appId == updatedApp.appId) updatedApp else it
            }
            _applications.value = currentList
            saveApplicationsToDisk(currentList)

            firestore?.let { db ->
                try {
                    withTimeoutOrNull(5000L) {
                        db.collection("applications").document(updatedApp.appId)
                            .set(updatedApp.copy(updatedAt = System.currentTimeMillis()).toMap()).await()
                    }
                } catch (e: Exception) {
                    Log.w(tag, "Firestore updateApp handled: ${e.message}")
                }
            }
            Result.success(Unit)
        }
    }

    suspend fun deleteApp(appId: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            val currentList = _applications.value.filter { it.appId != appId }
            _applications.value = currentList
            saveApplicationsToDisk(currentList)

            firestore?.let { db ->
                try {
                    withTimeoutOrNull(5000L) {
                        db.collection("applications").document(appId).delete().await()
                    }
                } catch (e: Exception) {
                    Log.w(tag, "Firestore deleteApp handled: ${e.message}")
                }
            }
            Result.success(Unit)
        }
    }

    fun searchApplications(query: String, category: String): List<ApplicationItem> {
        val trimmed = query.trim().lowercase()
        return _applications.value.filter { app ->
            val matchesCategory = category == "All" || app.category.equals(category, ignoreCase = true)
            val matchesQuery = trimmed.isEmpty() ||
                    app.appName.lowercase().contains(trimmed) ||
                    app.developerName.lowercase().contains(trimmed) ||
                    app.shortDescription.lowercase().contains(trimmed) ||
                    app.fullDescription.lowercase().contains(trimmed)
            matchesCategory && matchesQuery
        }
    }

    fun getAppById(appId: String): ApplicationItem? {
        return _applications.value.find { it.appId == appId }
    }

    private fun getCuratedSampleApps(): List<ApplicationItem> {
        return listOf(
            ApplicationItem(
                appId = "app_vlc_media",
                appName = "VLC Media Player",
                developerName = "VideoLAN Organization",
                developerEmail = "support@videolan.org",
                logoUrl = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=256&auto=format&fit=crop&q=80",
                shortDescription = "The versatile open-source cross-platform multimedia player for Android.",
                fullDescription = "VLC for Android plays most local video and audio files, network streams (including adaptive streaming), and DVD ISOs. Supports multi-track audio and subtitles, auto-rotation, aspect-ratio adjustments, and volume/brightness gestures.",
                category = "Entertainment",
                versionName = "3.5.4",
                versionCode = 354,
                minAndroidVersion = "Android 8.0 (API 26)",
                downloadUrl = "https://devuploads.com/example-vlc-apk",
                privacyPolicyUrl = "https://www.videolan.org/legal.html",
                supportUrl = "https://forum.videolan.org",
                permissionsDisclosure = "READ_MEDIA_VIDEO, READ_MEDIA_AUDIO, FOREGROUND_SERVICE for media playback.",
                screenshotUrls = listOf(
                    "https://images.unsplash.com/photo-1574717024653-61fd2cf4d44d?w=600&auto=format&fit=crop&q=80",
                    "https://images.unsplash.com/photo-1498050108023-c5249f4df085?w=600&auto=format&fit=crop&q=80"
                ),
                rating = 4.9f,
                downloadCount = 45000L,
                isFeatured = true,
                isTrending = true
            ),
            ApplicationItem(
                appId = "app_k9_mail",
                appName = "K-9 Mail (Thunderbird)",
                developerName = "MZLA Technologies",
                developerEmail = "k9mail@thunderbird.net",
                logoUrl = "https://images.unsplash.com/photo-1557200134-90327ee9fafa?w=256&auto=format&fit=crop&q=80",
                shortDescription = "Open source, privacy-focused email client for personal and business mail.",
                fullDescription = "K-9 Mail is an independent, community-driven email client for Android supporting IMAP, POP3, and Exchange (WebDAV). Built with respect for your privacy and no third-party tracking.",
                category = "Communication",
                versionName = "6.804",
                versionCode = 6804,
                minAndroidVersion = "Android 8.0 (API 26)",
                downloadUrl = "https://devuploads.com/example-k9mail-apk",
                privacyPolicyUrl = "https://k9mail.app/privacy.html",
                supportUrl = "https://forum.k9mail.app",
                permissionsDisclosure = "INTERNET, RECEIVE_BOOT_COMPLETED, POST_NOTIFICATIONS.",
                screenshotUrls = listOf(
                    "https://images.unsplash.com/photo-1526374965328-7f61d4dc18c5?w=600&auto=format&fit=crop&q=80"
                ),
                rating = 4.7f,
                downloadCount = 31000L,
                isFeatured = true,
                isTrending = false
            ),
            ApplicationItem(
                appId = "app_aegis_auth",
                appName = "Aegis Authenticator",
                developerName = "Aegis Open Source Team",
                developerEmail = "alex@aegis-auth.org",
                logoUrl = "https://images.unsplash.com/photo-1614064641938-3bbee52942c7?w=256&auto=format&fit=crop&q=80",
                shortDescription = "Free, secure and open-source 2FA authenticator with encrypted backups.",
                fullDescription = "Aegis Authenticator is a secure, encrypted app for managing your two-factor authentication tokens (TOTP & HOTP). Features biometric unlock, automatic encrypted backups, and custom grouping.",
                category = "Tools",
                versionName = "2.3.1",
                versionCode = 231,
                minAndroidVersion = "Android 8.0 (API 26)",
                downloadUrl = "https://devuploads.com/example-aegis-apk",
                privacyPolicyUrl = "https://getaegis.app/privacy",
                supportUrl = "https://github.com/beemdevelopment/Aegis",
                permissionsDisclosure = "CAMERA (optional for QR scanning), USE_BIOMETRIC.",
                screenshotUrls = listOf(
                    "https://images.unsplash.com/photo-1563986768609-322da13575f3?w=600&auto=format&fit=crop&q=80"
                ),
                rating = 4.9f,
                downloadCount = 52000L,
                isFeatured = true,
                isTrending = true
            ),
            ApplicationItem(
                appId = "app_obsidian_notes",
                appName = "Obsidian Markdown Notes",
                developerName = "Dynalist Inc.",
                developerEmail = "support@obsidian.md",
                logoUrl = "https://images.unsplash.com/photo-1517842645767-c639042777db?w=256&auto=format&fit=crop&q=80",
                shortDescription = "Sharpen your thinking. The extensible knowledge base on plain text Markdown.",
                fullDescription = "Obsidian is a powerful knowledge base on top of a local folder of plain text Markdown files. Connect ideas with bidirectional links, graph view, and extensive plugins.",
                category = "Productivity",
                versionName = "1.6.5",
                versionCode = 165,
                minAndroidVersion = "Android 8.0 (API 26)",
                downloadUrl = "https://devuploads.com/example-obsidian-apk",
                privacyPolicyUrl = "https://obsidian.md/privacy",
                supportUrl = "https://forum.obsidian.md",
                permissionsDisclosure = "INTERNET, ACCESS_NETWORK_STATE.",
                screenshotUrls = listOf(
                    "https://images.unsplash.com/photo-1455390582262-044cdead277a?w=600&auto=format&fit=crop&q=80"
                ),
                rating = 4.8f,
                downloadCount = 68000L,
                isFeatured = false,
                isTrending = true
            ),
            ApplicationItem(
                appId = "app_supertuxkart",
                appName = "SuperTuxKart Racing",
                developerName = "SuperTuxKart Team",
                developerEmail = "contact@supertuxkart.net",
                logoUrl = "https://images.unsplash.com/photo-1511512578047-dfb367046420?w=256&auto=format&fit=crop&q=80",
                shortDescription = "High-octane 3D open-source arcade kart racing with realistic physics.",
                fullDescription = "SuperTuxKart is a 3D open-source arcade kart racer with a variety of characters, tracks, and modes to play. Experience story mode, single-player Grand Prix, and online multiplayer races.",
                category = "Games",
                versionName = "1.4.0",
                versionCode = 140,
                minAndroidVersion = "Android 8.0 (API 26)",
                downloadUrl = "https://devuploads.com/example-supertuxkart-apk",
                privacyPolicyUrl = "https://supertuxkart.net/Privacy_Policy",
                supportUrl = "https://supertuxkart.net/Community",
                permissionsDisclosure = "INTERNET, VIBRATE, WAKE_LOCK.",
                screenshotUrls = listOf(
                    "https://images.unsplash.com/photo-1538481199705-c710c4e965fc?w=600&auto=format&fit=crop&q=80"
                ),
                rating = 4.6f,
                downloadCount = 19000L,
                isFeatured = false,
                isTrending = true
            )
        )
    }
}
