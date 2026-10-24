package com.example.model

/**
 * Model representing an application listed on NovaStore.
 */
data class ApplicationItem(
    val appId: String = "",
    val appName: String = "",
    val developerName: String = "",
    val developerEmail: String = "",
    val logoUrl: String = "",
    val shortDescription: String = "",
    val fullDescription: String = "",
    val category: String = "Tools",
    val versionName: String = "1.0.0",
    val versionCode: Long = 1L,
    val minAndroidVersion: String = "Android 8.0 (API 26)",
    val downloadUrl: String = "",
    val privacyPolicyUrl: String = "",
    val supportUrl: String = "",
    val permissionsDisclosure: String = "",
    val screenshotUrls: List<String> = emptyList(),
    val submitterUid: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val publicationStatus: String = "published", // "published", "pending", "rejected"
    val reportCount: Long = 0L,
    val rating: Float = 4.8f,
    val downloadCount: Long = 1200L,
    val isFeatured: Boolean = false,
    val isTrending: Boolean = false
) {
    fun toMap(): Map<String, Any> {
        return mapOf(
            "appId" to appId,
            "appName" to appName,
            "developerName" to developerName,
            "developerEmail" to developerEmail,
            "logoUrl" to logoUrl,
            "shortDescription" to shortDescription,
            "fullDescription" to fullDescription,
            "category" to category,
            "versionName" to versionName,
            "versionCode" to versionCode,
            "minAndroidVersion" to minAndroidVersion,
            "downloadUrl" to downloadUrl,
            "privacyPolicyUrl" to privacyPolicyUrl,
            "supportUrl" to supportUrl,
            "permissionsDisclosure" to permissionsDisclosure,
            "screenshotUrls" to screenshotUrls,
            "submitterUid" to submitterUid,
            "createdAt" to createdAt,
            "updatedAt" to updatedAt,
            "publicationStatus" to publicationStatus,
            "reportCount" to reportCount,
            "rating" to rating.toDouble(),
            "downloadCount" to downloadCount,
            "isFeatured" to isFeatured,
            "isTrending" to isTrending
        )
    }

    companion object {
        fun fromMap(id: String, map: Map<String, Any?>): ApplicationItem {
            @Suppress("UNCHECKED_CAST")
            val screenshots = (map["screenshotUrls"] as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()
            return ApplicationItem(
                appId = (map["appId"] as? String) ?: id,
                appName = (map["appName"] as? String) ?: "",
                developerName = (map["developerName"] as? String) ?: "",
                developerEmail = (map["developerEmail"] as? String) ?: "",
                logoUrl = (map["logoUrl"] as? String) ?: "",
                shortDescription = (map["shortDescription"] as? String) ?: "",
                fullDescription = (map["fullDescription"] as? String) ?: "",
                category = (map["category"] as? String) ?: "Tools",
                versionName = (map["versionName"] as? String) ?: "1.0.0",
                versionCode = (map["versionCode"] as? Number)?.toLong() ?: 1L,
                minAndroidVersion = (map["minAndroidVersion"] as? String) ?: "Android 8.0 (API 26)",
                downloadUrl = (map["downloadUrl"] as? String) ?: "",
                privacyPolicyUrl = (map["privacyPolicyUrl"] as? String) ?: "",
                supportUrl = (map["supportUrl"] as? String) ?: "",
                permissionsDisclosure = (map["permissionsDisclosure"] as? String) ?: "",
                screenshotUrls = screenshots,
                submitterUid = (map["submitterUid"] as? String) ?: "",
                createdAt = (map["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                updatedAt = (map["updatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                publicationStatus = (map["publicationStatus"] as? String) ?: "published",
                reportCount = (map["reportCount"] as? Number)?.toLong() ?: 0L,
                rating = (map["rating"] as? Number)?.toFloat() ?: 4.8f,
                downloadCount = (map["downloadCount"] as? Number)?.toLong() ?: 1200L,
                isFeatured = (map["isFeatured"] as? Boolean) ?: false,
                isTrending = (map["isTrending"] as? Boolean) ?: false
            )
        }
    }
}
