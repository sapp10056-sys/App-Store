package com.example

import com.example.data.TokenManager
import com.example.model.ApplicationItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class NovaTokenTest {

    @Test
    fun testTokenPricingTiers() {
        assertEquals(50, TokenManager.TIER_1_TOKENS)
        assertEquals(10, TokenManager.TIER_1_INR)

        assertEquals(400, TokenManager.TIER_2_TOKENS)
        assertEquals(50, TokenManager.TIER_2_INR)

        assertEquals(800, TokenManager.TIER_3_TOKENS)
        assertEquals(100, TokenManager.TIER_3_INR)
    }

    @Test
    fun testTokenDistributionSplit() {
        assertEquals(20, TokenManager.PREMIUM_UPLOAD_COST)
        assertEquals(10, TokenManager.PREMIUM_DOWNLOAD_COST)
        assertEquals(7, TokenManager.DEV_REWARD_TOKENS)
        assertEquals(3, TokenManager.ADMIN_COMMISSION_TOKENS)

        // Ensure 7 + 3 equals total download cost (10 tokens)
        assertEquals(TokenManager.PREMIUM_DOWNLOAD_COST, TokenManager.DEV_REWARD_TOKENS + TokenManager.ADMIN_COMMISSION_TOKENS)
    }

    @Test
    fun testApplicationItemPremiumDefaults() {
        val freeApp = ApplicationItem(appName = "Free App", isPremium = false)
        assertFalse(freeApp.isPremium)

        val premiumApp = ApplicationItem(appName = "Pro Tool", isPremium = true, tokenCost = 10)
        assertTrue(premiumApp.isPremium)
        assertEquals(10, premiumApp.tokenCost)
    }

    @Test
    fun testSerializationPreservesPremiumFields() {
        val app = ApplicationItem(
            appId = "app_test_123",
            appName = "Premium Video Editor",
            developerId = "dev_alpha",
            isPremium = true,
            tokenCost = 10
        )

        val map = app.toMap()
        assertEquals(true, map["isPremium"])
        assertEquals(10, map["tokenCost"])

        val fromMapApp = ApplicationItem.fromMap(app.appId, map)
        assertTrue(fromMapApp.isPremium)
        assertEquals(10, fromMapApp.tokenCost)

        val json = app.toJson()
        assertTrue(json.getBoolean("isPremium"))
        assertEquals(10, json.getInt("tokenCost"))

        val parsed = ApplicationItem.fromJson(json)
        assertTrue(parsed.isPremium)
        assertEquals(10, parsed.tokenCost)
    }
}
