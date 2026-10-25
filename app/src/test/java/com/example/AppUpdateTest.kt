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
    fun testNoUpdateWhenVersionsMatchAndForceUpdateFalse() {
        val currentVersionCode = 1
        val updateInfo = AppUpdateInfo(
            latestVersionCode = 1,
            latestVersionName = "1.0.0",
            minimumSupportedVersionCode = 1,
            forceUpdate = false
        )

        val shouldTrigger = updateInfo.latestVersionCode > currentVersionCode ||
                updateInfo.forceUpdate ||
                currentVersionCode < updateInfo.minimumSupportedVersionCode

        assertFalse(shouldTrigger)
    }

    @Test
    fun testForceUpdateImmediatelyTriggersEvenWhenVersionsMatch() {
        val currentVersionCode = 1
        val updateInfo = AppUpdateInfo(
            latestVersionCode = 1,
            latestVersionName = "1.0.0",
            minimumSupportedVersionCode = 1,
            forceUpdate = true
        )

        // Remote admin toggled forceUpdate: true in GitHub JSON
        val shouldTrigger = updateInfo.latestVersionCode > currentVersionCode ||
                updateInfo.forceUpdate ||
                currentVersionCode < updateInfo.minimumSupportedVersionCode

        assertTrue("When forceUpdate is true on GitHub, update must immediately trigger", shouldTrigger)
        assertTrue(updateInfo.forceUpdate)
    }
}
