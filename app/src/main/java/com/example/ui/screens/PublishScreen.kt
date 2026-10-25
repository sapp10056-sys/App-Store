package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.model.StoreCategories
import com.example.ui.theme.CyanPrimaryLight
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.VioletSecondaryDark
import com.example.ui.theme.WarningAmber
import com.example.viewmodel.Screen
import com.example.viewmodel.StoreViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PublishScreen(
    viewModel: StoreViewModel,
    onNavigate: (Screen) -> Unit
) {
    BackHandler {
        onNavigate(Screen.Home)
    }

    val context = LocalContext.current
    val form by viewModel.publishForm.collectAsState()
    val telegramBotUrl by viewModel.telegramBotUrl.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var newScreenshotUrl by remember { mutableStateOf("") }
    var categoryExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Publish Application", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(
                        onClick = { onNavigate(Screen.Home) },
                        modifier = Modifier.testTag("publish_back_button")
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // STEP 1: Telegram Submission Workflow Banner
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(CyanPrimaryLight),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "How APK Submission Works",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "2-Step submission via Telegram & DevUploads",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "1. Send your APK file to the official Telegram Bot.\n" +
                                "2. Platform operator uploads it to high-speed DevUploads.\n" +
                                "3. Paste the generated DevUploads link and details below to publish instantly.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(telegramBotUrl))
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                scope.launch {
                                    snackbarHostState.showSnackbar("Unable to open Telegram link: $telegramBotUrl")
                                }
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CyanPrimaryLight),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("open_telegram_bot_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Open Telegram Bot (${telegramBotUrl.substringAfterLast("/")})", fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Submission Success Banner
            if (form.submissionSuccess) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = SuccessGreen.copy(alpha = 0.15f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SuccessGreen)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Application Published Successfully!", fontWeight = FontWeight.Bold, color = SuccessGreen)
                            Text("Your application listing is now live on NovaStore.", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            Text(
                text = "Listing Information",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Complete all required fields to submit your listing to Firestore.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 1. Application Name
            OutlinedTextField(
                value = form.appName,
                onValueChange = { viewModel.updatePublishAppName(it) },
                label = { Text("Application Name *") },
                isError = form.appName.isNotEmpty() && !form.isAppNameValid,
                supportingText = {
                    if (form.appName.isNotEmpty() && !form.isAppNameValid) {
                        Text("Name must be at least 2 characters", color = MaterialTheme.colorScheme.error)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_app_name"),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 2. Developer Name
            OutlinedTextField(
                value = form.developerName,
                onValueChange = { viewModel.updatePublishDevName(it) },
                label = { Text("Developer / Studio Name *") },
                isError = form.developerName.isNotEmpty() && !form.isDevNameValid,
                supportingText = {
                    if (form.developerName.isNotEmpty() && !form.isDevNameValid) {
                        Text("Developer name required", color = MaterialTheme.colorScheme.error)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_dev_name"),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            // Irreversible submission warning notice
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = WarningAmber.copy(alpha = 0.12f)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = WarningAmber, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Note: Once submitted, listings cannot be edited or deleted by standard users. Only platform administrators can modify or remove published apps.",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 4. Logo URL + Live Preview
            OutlinedTextField(
                value = form.logoUrl,
                onValueChange = { viewModel.updatePublishLogoUrl(it) },
                label = { Text("Application Logo Image URL (HTTPS) *") },
                placeholder = { Text("https://example.com/logo.png") },
                isError = form.logoUrl.isNotEmpty() && !form.isLogoUrlValid,
                supportingText = {
                    if (form.logoUrl.isNotEmpty() && !form.isLogoUrlValid) {
                        Text("Must be a valid HTTPS image URL", color = MaterialTheme.colorScheme.error)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_logo_url"),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            // Logo Live Preview Card
            if (form.logoUrl.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surface),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(form.logoUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Logo Preview",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(54.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Live Logo Preview", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                        Text(
                            if (form.isLogoUrlValid) "Valid HTTPS image link detected" else "Checking image URL format...",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (form.isLogoUrlValid) SuccessGreen else MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 5. Short Description
            OutlinedTextField(
                value = form.shortDescription,
                onValueChange = { viewModel.updatePublishShortDesc(it) },
                label = { Text("Short Description (10-150 chars) *") },
                isError = form.shortDescription.isNotEmpty() && !form.isShortDescValid,
                supportingText = {
                    Text("${form.shortDescription.length}/150 characters")
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_short_desc"),
                shape = RoundedCornerShape(12.dp),
                maxLines = 2
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 6. Full Description
            OutlinedTextField(
                value = form.fullDescription,
                onValueChange = { viewModel.updatePublishFullDesc(it) },
                label = { Text("Full Description (at least 20 chars) *") },
                isError = form.fullDescription.isNotEmpty() && !form.isFullDescValid,
                supportingText = {
                    if (form.fullDescription.isNotEmpty() && !form.isFullDescValid) {
                        Text("Full description must be at least 20 characters", color = MaterialTheme.colorScheme.error)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_full_desc"),
                shape = RoundedCornerShape(12.dp),
                minLines = 3,
                maxLines = 6
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 7. Category Dropdown
            ExposedDropdownMenuBox(
                expanded = categoryExpanded,
                onExpandedChange = { categoryExpanded = it },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = form.category,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Category *") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                        .testTag("input_category"),
                    shape = RoundedCornerShape(12.dp)
                )

                ExposedDropdownMenu(
                    expanded = categoryExpanded,
                    onDismissRequest = { categoryExpanded = false }
                ) {
                    StoreCategories.submissionCategories.forEach { cat ->
                        DropdownMenuItem(
                            text = { Text(cat) },
                            onClick = {
                                viewModel.updatePublishCategory(cat)
                                categoryExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 8 & 9. Version Name & Version Code
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = form.versionName,
                    onValueChange = { viewModel.updatePublishVersionName(it) },
                    label = { Text("Version Name *") },
                    placeholder = { Text("1.0.0") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("input_version_name"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                OutlinedTextField(
                    value = form.versionCode,
                    onValueChange = { viewModel.updatePublishVersionCode(it) },
                    label = { Text("Version Code *") },
                    placeholder = { Text("1") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("input_version_code"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 10. Minimum Android Version
            OutlinedTextField(
                value = form.minAndroidVersion,
                onValueChange = { viewModel.updatePublishMinAndroid(it) },
                label = { Text("Minimum Android OS *") },
                placeholder = { Text("Android 8.0 (API 26)") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_min_os"),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 11. APK Download URL (DevUploads or verified HTTPS host)
            OutlinedTextField(
                value = form.downloadUrl,
                onValueChange = { viewModel.updatePublishDownloadUrl(it) },
                label = { Text("APK Download URL (DevUploads / HTTPS) *") },
                placeholder = { Text("https://devuploads.com/...") },
                isError = form.downloadUrl.isNotEmpty() && !form.isDownloadUrlValid,
                supportingText = {
                    if (form.downloadUrl.isNotEmpty() && !form.isDownloadUrlValid) {
                        Text("Must be a secure HTTPS link from DevUploads or supported host", color = MaterialTheme.colorScheme.error)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_download_url"),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Support Contact or URL

            // 13. Support Contact / URL
            OutlinedTextField(
                value = form.supportUrl,
                onValueChange = { viewModel.updatePublishSupport(it) },
                label = { Text("Support Contact or URL *") },
                placeholder = { Text("https://github.com/myrepo or email") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_support_url"),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 14. Permissions and Important Disclosures
            OutlinedTextField(
                value = form.permissionsDisclosure,
                onValueChange = { viewModel.updatePublishPermissions(it) },
                label = { Text("Permissions & Disclosures *") },
                placeholder = { Text("e.g. INTERNET, CAMERA (for scanning QR codes)") },
                isError = form.permissionsDisclosure.isNotEmpty() && !form.isPermissionsValid,
                supportingText = {
                    if (form.permissionsDisclosure.isNotEmpty() && !form.isPermissionsValid) {
                        Text("Please disclose required permissions for transparency", color = MaterialTheme.colorScheme.error)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_permissions"),
                shape = RoundedCornerShape(12.dp),
                minLines = 2
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Optional Screenshot URLs
            Text(
                text = "Screenshot URLs (Optional)",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = newScreenshotUrl,
                    onValueChange = { newScreenshotUrl = it },
                    placeholder = { Text("https://example.com/screenshot.png") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        if (newScreenshotUrl.isNotBlank()) {
                            viewModel.addScreenshotUrl(newScreenshotUrl)
                            newScreenshotUrl = ""
                        }
                    },
                    modifier = Modifier.background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(10.dp))
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Screenshot", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }

            form.screenshotUrls.forEachIndexed { index, url ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${index + 1}. $url",
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { viewModel.removeScreenshotUrl(index) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Mandatory Checkboxes (15, 16, 17)
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Mandatory Authorizations",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = form.hasDistributionRights,
                            onCheckedChange = { viewModel.toggleDistributionRights(it) },
                            modifier = Modifier.testTag("checkbox_rights"),
                            colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "I confirm that I have legal permission to distribute this application.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = form.isInfoAccurate,
                            onCheckedChange = { viewModel.toggleInfoAccurate(it) },
                            modifier = Modifier.testTag("checkbox_accurate"),
                            colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "I confirm that all entered information, links, and permissions are accurate.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = form.agreesToRules,
                            onCheckedChange = { viewModel.toggleAgreesToRules(it) },
                            modifier = Modifier.testTag("checkbox_rules"),
                            colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "I agree to NovaStore platform submission guidelines and abuse policy.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            if (form.submissionError != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = form.submissionError ?: "",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Publish Button
            Button(
                onClick = {
                    viewModel.submitPublishForm {
                        scope.launch {
                            snackbarHostState.showSnackbar("Listing published successfully!")
                        }
                    }
                },
                enabled = form.isFormValid && !form.isSubmitting,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("submit_publish_button")
            ) {
                if (form.isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Publishing to Firestore...")
                } else {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (form.isFormValid) "Publish Application" else "Complete All Required Fields",
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}
