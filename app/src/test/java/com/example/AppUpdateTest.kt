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

    @Test
    fun testDeprecatedV1ApkIsForcedToUpdateToV2() {
        val oldApkVersionCode = 1
        val v2UpdateInfo = AppUpdateInfo(
            latestVersionCode = 2,
            latestVersionName = "2.0.0",
            minimumSupportedVersionCode = 2,
            forceUpdate = false,
            downloadUrl = "https://github.com/sapp10056-sys/App-Store/releases/latest"
        )

        val isEnforced = v2UpdateInfo.forceUpdate || (oldApkVersionCode < v2UpdateInfo.minimumSupportedVersionCode)
        val shouldTrigger = v2UpdateInfo.latestVersionCode > oldApkVersionCode || isEnforced

        assertTrue("Old v1 APK must be locked with force update", isEnforced)
        assertTrue("Old v1 APK must trigger update prompt", shouldTrigger)
    }

    @Test
    fun testNewV2ApkRunsNormallyWithoutUpdatePrompt() {
        val newApkVersionCode = 2
        val v2UpdateInfo = AppUpdateInfo(
            latestVersionCode = 2,
            latestVersionName = "2.0.0",
            minimumSupportedVersionCode = 2,
            forceUpdate = false,
            downloadUrl = "https://github.com/sapp10056-sys/App-Store/releases/latest"
        )

        val isEnforced = v2UpdateInfo.forceUpdate || (newApkVersionCode < v2UpdateInfo.minimumSupportedVersionCode)
        val shouldTrigger = v2UpdateInfo.latestVersionCode > newApkVersionCode || isEnforced

        assertFalse("New v2 APK must NOT be locked", isEnforced)
        assertFalse("New v2 APK must NOT trigger update prompt", shouldTrigger)
    }
}
