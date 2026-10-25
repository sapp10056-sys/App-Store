package com.example

import com.example.model.ApplicationItem
import com.example.util.AppShareHelper
import org.junit.Assert.assertTrue
import org.junit.Test

class AppShareHelperTest {

    @Test
    fun testShareMessageContainsNovaStoreApkDownloadLink() {
        val app = ApplicationItem(
            appId = "com.sample.testapp",
            appName = "SampleTestApp",
            developerName = "TestDev",
            category = "Tools",
            versionName = "2.1.0",
            versionCode = 5,
            downloadUrl = "https://example.com/download.apk",
            shortDescription = "Test app short description"
        )

        val message = AppShareHelper.createShareMessage(app)

        // Verifies the official GitHub NovaStore APK download URL is prominently featured so the recipient can download NovaStore
        assertTrue("Message should contain GitHub NovaStore APK download URL", message.contains(AppShareHelper.GITHUB_APK_DOWNLOAD_URL))

        // Verifies essential app details
        assertTrue("Message should contain app name", message.contains("SampleTestApp"))
        assertTrue("Message should contain version", message.contains("v2.1.0"))
    }

    @Test
    fun testNovaStoreDirectApkShareMessage() {
        val message = AppShareHelper.createNovaStoreShareMessage()

        assertTrue("NovaStore share message must contain download link", message.contains(AppShareHelper.GITHUB_APK_DOWNLOAD_URL))
        assertTrue("NovaStore share message must mention NovaStore", message.contains("NovaStore"))
    }
}
