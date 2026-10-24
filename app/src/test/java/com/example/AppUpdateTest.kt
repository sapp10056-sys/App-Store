package com.example

import com.example.model.AppUpdateInfo
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppUpdateTest {

    @Test
    fun testUpdateAvailableWhenLatestVersionHigher() {
        val currentVersionCode = 1
        val updateInfo = AppUpdateInfo(
            latestVersionCode = 2,
            latestVersionName = "1.1.0",
            minimumSupportedVersionCode = 1,
            forceUpdate = false,
            downloadUrl = "https://github.com/example/releases/latest"
        )

        assertTrue(updateInfo.latestVersionCode > currentVersionCode)
    }

    @Test
    fun testNoUpdateWhenVersionsMatch() {
        val currentVersionCode = 2
        val updateInfo = AppUpdateInfo(
            latestVersionCode = 2,
            latestVersionName = "1.1.0",
            minimumSupportedVersionCode = 1,
            forceUpdate = false
        )

        assertFalse(updateInfo.latestVersionCode > currentVersionCode)
    }

    @Test
    fun testForceUpdateConditionTriggered() {
        val currentVersionCode = 1
        val updateInfo = AppUpdateInfo(
            latestVersionCode = 3,
            latestVersionName = "2.0.0",
            minimumSupportedVersionCode = 2,
            forceUpdate = true
        )

        val isMandatory = updateInfo.forceUpdate && currentVersionCode < updateInfo.minimumSupportedVersionCode
        assertTrue(isMandatory)
    }
}
