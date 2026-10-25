package com.example.model

import org.json.JSONArray
import org.json.JSONObject

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

    fun toJson(): JSONObject {
        val json = JSONObject()
        json.put("appId", appId)
        json.put("appName", appName)
        json.put("developerName", developerName)
        json.put("developerEmail", developerEmail)
        json.put("logoUrl", logoUrl)
        json.put("shortDescription", shortDescription)
        json.put("fullDescription", fullDescription)
        json.put("category", category)
        json.put("versionName", versionName)
        json.put("versionCode", versionCode)
        json.put("minAndroidVersion", minAndroidVersion)
        json.put("downloadUrl", downloadUrl)
        json.put("privacyPolicyUrl", privacyPolicyUrl)
        json.put("supportUrl", supportUrl)
        json.put("permissionsDisclosure", permissionsDisclosure)
        val arr = JSONArray()
        screenshotUrls.forEach { arr.put(it) }
        json.put("screenshotUrls", arr)
        json.put("submitterUid", submitterUid)
        json.put("createdAt", createdAt)
        json.put("updatedAt", updatedAt)
        json.put("publicationStatus", publicationStatus)
        json.put("reportCount", reportCount)
        json.put("rating", rating.toDouble())
        json.put("downloadCount", downloadCount)
        json.put("isFeatured", isFeatured)
        json.put("isTrending", isTrending)
        return json
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

        fun fromJson(json: JSONObject): ApplicationItem {
            val screenshots = mutableListOf<String>()
            val arr = json.optJSONArray("screenshotUrls")
            if (arr != null) {
                for (i in 0 until arr.length()) {
                    screenshots.add(arr.optString(i))
                }
            }
            return ApplicationItem(
                appId = json.optString("appId"),
                appName = json.optString("appName"),
                developerName = json.optString("developerName"),
                developerEmail = json.optString("developerEmail"),
                logoUrl = json.optString("logoUrl"),
                shortDescription = json.optString("shortDescription"),
                fullDescription = json.optString("fullDescription"),
                category = json.optString("category", "Tools"),
                versionName = json.optString("versionName", "1.0.0"),
                versionCode = json.optLong("versionCode", 1L),
                minAndroidVersion = json.optString("minAndroidVersion", "Android 8.0 (API 26)"),
                downloadUrl = json.optString("downloadUrl"),
                privacyPolicyUrl = json.optString("privacyPolicyUrl"),
                supportUrl = json.optString("supportUrl"),
                permissionsDisclosure = json.optString("permissionsDisclosure"),
                screenshotUrls = screenshots,
                submitterUid = json.optString("submitterUid"),
                createdAt = json.optLong("createdAt", System.currentTimeMillis()),
                updatedAt = json.optLong("updatedAt", System.currentTimeMillis()),
                publicationStatus = json.optString("publicationStatus", "published"),
                reportCount = json.optLong("reportCount", 0L),
                rating = json.optDouble("rating", 4.8).toFloat(),
                downloadCount = json.optLong("downloadCount", 1200L),
                isFeatured = json.optBoolean("isFeatured", false),
                isTrending = json.optBoolean("isTrending", false)
            )
        }
    }
}
