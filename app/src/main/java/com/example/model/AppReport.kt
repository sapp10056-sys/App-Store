package com.example.model

data class AppReport(
    val reportId: String = "",
    val appId: String = "",
    val appName: String = "",
    val reporterUid: String = "",
    val reason: String = "",
    val details: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val status: String = "open" // "open", "resolved", "dismissed"
) {
    fun toMap(): Map<String, Any> {
        return mapOf(
            "reportId" to reportId,
            "appId" to appId,
            "appName" to appName,
            "reporterUid" to reporterUid,
            "reason" to reason,
            "details" to details,
            "createdAt" to createdAt,
            "status" to status
        )
    }

    companion object {
        fun fromMap(id: String, map: Map<String, Any?>): AppReport {
            return AppReport(
                reportId = (map["reportId"] as? String) ?: id,
                appId = (map["appId"] as? String) ?: "",
                appName = (map["appName"] as? String) ?: "",
                reporterUid = (map["reporterUid"] as? String) ?: "",
                reason = (map["reason"] as? String) ?: "",
                details = (map["details"] as? String) ?: "",
                createdAt = (map["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                status = (map["status"] as? String) ?: "open"
            )
        }
    }
}
