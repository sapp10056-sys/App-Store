package com.example.model

import org.json.JSONObject

/**
 * Represents a specific release version of an application (e.g. Uptodown / APKPure version history).
 */
data class AppVersionItem(
    val versionName: String = "1.0.0",
    val versionCode: Long = 1L,
    val downloadUrl: String = "",
    val releaseDate: Long = System.currentTimeMillis(),
    val changelog: String = "Performance improvements and bug fixes.",
    val apkSize: String = "Auto"
) {
    fun toMap(): Map<String, Any> {
        return mapOf(
            "versionName" to versionName,
            "versionCode" to versionCode,
            "downloadUrl" to downloadUrl,
            "releaseDate" to releaseDate,
            "changelog" to changelog,
            "apkSize" to apkSize
        )
    }

    fun toJson(): JSONObject {
        val json = JSONObject()
        json.put("versionName", versionName)
        json.put("versionCode", versionCode)
        json.put("downloadUrl", downloadUrl)
        json.put("releaseDate", releaseDate)
        json.put("changelog", changelog)
        json.put("apkSize", apkSize)
        return json
    }

    companion object {
        fun fromMap(map: Map<String, Any?>): AppVersionItem {
            return AppVersionItem(
                versionName = (map["versionName"] as? String) ?: "1.0.0",
                versionCode = (map["versionCode"] as? Number)?.toLong() ?: 1L,
                downloadUrl = (map["downloadUrl"] as? String) ?: "",
                releaseDate = (map["releaseDate"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                changelog = (map["changelog"] as? String) ?: "Release update",
                apkSize = (map["apkSize"] as? String) ?: "Auto"
            )
        }

        fun fromJson(json: JSONObject): AppVersionItem {
            return AppVersionItem(
                versionName = json.optString("versionName", "1.0.0"),
                versionCode = json.optLong("versionCode", 1L),
                downloadUrl = json.optString("downloadUrl"),
                releaseDate = json.optLong("releaseDate", System.currentTimeMillis()),
                changelog = json.optString("changelog", "Release update"),
                apkSize = json.optString("apkSize", "Auto")
            )
        }
    }
}
