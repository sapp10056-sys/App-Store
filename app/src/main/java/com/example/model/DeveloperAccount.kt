package com.example.model

import org.json.JSONObject

/**
 * Model representing a registered Developer Account on NovaStore.
 * Developers register with their unique Developer ID and Password, and only they
 * can upload and edit their own applications.
 */
data class DeveloperAccount(
    val developerId: String = "",          // Unique username/handle, e.g. "rohit_dev"
    val developerName: String = "",        // Studio / Display Name, e.g. "Rohit Gaming"
    val passwordHash: String = "",         // Password or PIN hash
    val contactUrl: String = "",           // Optional Telegram or Email
    val registeredAt: Long = System.currentTimeMillis()
) {
    fun toMap(): Map<String, Any> {
        return mapOf(
            "developerId" to developerId,
            "developerName" to developerName,
            "passwordHash" to passwordHash,
            "contactUrl" to contactUrl,
            "registeredAt" to registeredAt
        )
    }

    fun toJson(): JSONObject {
        val json = JSONObject()
        json.put("developerId", developerId)
        json.put("developerName", developerName)
        json.put("passwordHash", passwordHash)
        json.put("contactUrl", contactUrl)
        json.put("registeredAt", registeredAt)
        return json
    }

    companion object {
        fun fromMap(id: String, map: Map<String, Any?>): DeveloperAccount {
            return DeveloperAccount(
                developerId = (map["developerId"] as? String) ?: id,
                developerName = (map["developerName"] as? String) ?: "",
                passwordHash = (map["passwordHash"] as? String) ?: "",
                contactUrl = (map["contactUrl"] as? String) ?: "",
                registeredAt = (map["registeredAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
            )
        }

        fun fromJson(json: JSONObject): DeveloperAccount {
            return DeveloperAccount(
                developerId = json.optString("developerId"),
                developerName = json.optString("developerName"),
                passwordHash = json.optString("passwordHash"),
                contactUrl = json.optString("contactUrl"),
                registeredAt = json.optLong("registeredAt", System.currentTimeMillis())
            )
        }
    }
}
