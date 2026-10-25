package com.example

import com.example.model.ApplicationItem
import com.example.util.AppShareHelper
import org.junit.Assert.assertTrue
import org.junit.Test

class AppShareHelperTest {

    @Test
    fun testShareMessageContainsDeepLinkAndGitHubFallback() {
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

        // Verifies the custom scheme deep link is present
        assertTrue("Message should contain deep link", message.contains("novastore://app/com.sample.testapp"))

        // Verifies the official GitHub APK fallback link is present for users without NovaStore
        assertTrue("Message should contain GitHub NovaStore APK download URL", message.contains(AppShareHelper.GITHUB_APK_DOWNLOAD_URL))

        // Verifies essential app details
        assertTrue("Message should contain app name", message.contains("SampleTestApp"))
        assertTrue("Message should contain version", message.contains("v2.1.0"))
    }
}
