package com.example

import com.example.viewmodel.PublishFormState
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StoreValidationTest {

    @Test
    fun testEmptyFormIsInvalid() {
        val form = PublishFormState()
        assertFalse(form.isFormValid)
    }

    @Test
    fun testValidFormPassesAllValidationsWithoutEmailOrPrivacyPolicy() {
        val validForm = PublishFormState(
            appName = "NovaBrowser",
            developerName = "Nova Team",
            logoUrl = "https://example.com/logo.png",
            shortDescription = "A fast and private open source web browser.",
            fullDescription = "NovaBrowser is designed for speed, privacy, and full control over your internet browsing experience.",
            category = "Tools",
            versionName = "1.0.0",
            versionCode = "1",
            minAndroidVersion = "Android 8.0 (API 26)",
            downloadUrl = "https://devuploads.com/example-browser-apk",
            supportUrl = "https://example.com/support",
            permissionsDisclosure = "INTERNET, ACCESS_NETWORK_STATE",
            hasDistributionRights = true,
            isInfoAccurate = true,
            agreesToRules = true
        )

        assertTrue(validForm.isAppNameValid)
        assertTrue(validForm.isDevNameValid)
        assertTrue(validForm.isLogoUrlValid)
        assertTrue(validForm.isShortDescValid)
        assertTrue(validForm.isFullDescValid)
        assertTrue(validForm.isVersionNameValid)
        assertTrue(validForm.isVersionCodeValid)
        assertTrue(validForm.isDownloadUrlValid)
        assertTrue(validForm.isSupportValid)
        assertTrue(validForm.isPermissionsValid)
        assertTrue(validForm.areCheckboxesChecked)
        assertTrue(validForm.isFormValid)
    }

    @Test
    fun testInvalidDownloadUrlFailsValidation() {
        val formWithHttp = PublishFormState(
            appName = "NovaBrowser",
            developerName = "Nova Team",
            logoUrl = "https://example.com/logo.png",
            shortDescription = "A fast and private open source web browser.",
            fullDescription = "NovaBrowser is designed for speed, privacy, and full control over your internet browsing experience.",
            category = "Tools",
            versionName = "1.0.0",
            versionCode = "1",
            minAndroidVersion = "Android 8.0 (API 26)",
            downloadUrl = "http://insecure-site.com/apk", // Insecure HTTP
            supportUrl = "https://example.com/support",
            permissionsDisclosure = "INTERNET",
            hasDistributionRights = true,
            isInfoAccurate = true,
            agreesToRules = true
        )

        assertFalse(formWithHttp.isDownloadUrlValid)
        assertFalse(formWithHttp.isFormValid)
    }

    @Test
    fun testUncheckedMandatoryCheckboxesBlockPublish() {
        val formWithUnchecked = PublishFormState(
            appName = "NovaBrowser",
            developerName = "Nova Team",
            logoUrl = "https://example.com/logo.png",
            shortDescription = "A fast and private open source web browser.",
            fullDescription = "NovaBrowser is designed for speed, privacy, and full control over your internet browsing experience.",
            category = "Tools",
            versionName = "1.0.0",
            versionCode = "1",
            minAndroidVersion = "Android 8.0 (API 26)",
            downloadUrl = "https://devuploads.com/example-browser-apk",
            supportUrl = "https://example.com/support",
            permissionsDisclosure = "INTERNET",
            hasDistributionRights = true,
            isInfoAccurate = false, // Not checked
            agreesToRules = true
        )

        assertFalse(formWithUnchecked.areCheckboxesChecked)
        assertFalse(formWithUnchecked.isFormValid)
    }
}
