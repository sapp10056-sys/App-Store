package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Upgrade
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.TextButton
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

    val currentDeveloper by viewModel.currentDeveloper.collectAsState()
    val allApps by viewModel.repository.applications.collectAsState()
    val myApps = remember(allApps, currentDeveloper) {
        val devId = currentDeveloper?.developerId
        if (devId.isNullOrBlank()) emptyList()
        else allApps.filter { it.developerId.equals(devId, ignoreCase = true) }
    }

    var devAuthMode by remember { mutableStateOf(0) } // 0 = Login, 1 = Register
    var devInputId by remember { mutableStateOf("") }
    var devInputPass by remember { mutableStateOf("") }
    var devInputName by remember { mutableStateOf("") }
    var devPassVisible by remember { mutableStateOf(false) }
    var devAuthMsg by remember { mutableStateOf<String?>(null) }

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
                                text = "Upload APK via Telegram Group",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Join group, upload APK, and generate link",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "1. Tap 'Upload APK in Telegram Group' below to open the group.\n" +
                                "2. Upload your APK file in the group to generate your download link.\n" +
                                "3. Paste the generated download link below to publish your app.",
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
                            .testTag("upload_apk_telegram_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Upload APK in Telegram Group (${telegramBotUrl.substringAfterLast("/")})", fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // DEVELOPER AUTHENTICATION & ACCESS CONTROL CARD
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("developer_account_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (currentDeveloper != null) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                    else MaterialTheme.colorScheme.surfaceVariant
                ),
                border = if (currentDeveloper != null) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)) else null
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    val dev = currentDeveloper
                    if (dev == null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Lock, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Developer Identity & Security",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "Create ID & Password to upload & protect your APKs",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Har developer ka apna alag ID & Password hota hai. Aapke ID se upload kiya gaya APK sirf aap hi update ya edit kar payenge, koi dusra developer nahi.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Toggle Tab: Sign In vs Create Account
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.background)
                                .padding(4.dp)
                        ) {
                            Button(
                                onClick = { devAuthMode = 0; devAuthMsg = null },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (devAuthMode == 0) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    contentColor = if (devAuthMode == 0) Color.White else MaterialTheme.colorScheme.onSurface
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(vertical = 6.dp)
                            ) {
                                Text("Sign In", fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = { devAuthMode = 1; devAuthMsg = null },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (devAuthMode == 1) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    contentColor = if (devAuthMode == 1) Color.White else MaterialTheme.colorScheme.onSurface
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(vertical = 6.dp)
                            ) {
                                Text("Create ID & Pass", fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Developer ID Input
                        OutlinedTextField(
                            value = devInputId,
                            onValueChange = { devInputId = it.trim().lowercase().replace(" ", "_") },
                            label = { Text("Developer ID / Handle (e.g. rohit_dev) *") },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(18.dp)) },
                            modifier = Modifier.fillMaxWidth().testTag("dev_auth_id_input"),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true
                        )

                        if (devAuthMode == 1) {
                            Spacer(modifier = Modifier.height(10.dp))
                            // Developer Studio / Display Name
                            OutlinedTextField(
                                value = devInputName,
                                onValueChange = { devInputName = it },
                                label = { Text("Developer / Studio Display Name *") },
                                placeholder = { Text("e.g. Rohit Studios") },
                                modifier = Modifier.fillMaxWidth().testTag("dev_auth_name_input"),
                                shape = RoundedCornerShape(10.dp),
                                singleLine = true
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Developer Password Input
                        OutlinedTextField(
                            value = devInputPass,
                            onValueChange = { devInputPass = it },
                            label = { Text("Password *") },
                            visualTransformation = if (devPassVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { devPassVisible = !devPassVisible }) {
                                    Icon(
                                        imageVector = if (devPassVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = null
                                    )
                                }
                            },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(18.dp)) },
                            modifier = Modifier.fillMaxWidth().testTag("dev_auth_pass_input"),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true
                        )

                        if (devAuthMsg != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = devAuthMsg!!,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (devAuthMsg!!.contains("success", ignoreCase = true) || devAuthMsg!!.contains("logged", ignoreCase = true)) SuccessGreen else MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = {
                                if (devAuthMode == 0) {
                                    viewModel.loginDeveloper(devInputId, devInputPass) { success, err ->
                                        devAuthMsg = if (success) "Successfully logged in as @$devInputId!" else (err ?: "Login failed")
                                    }
                                } else {
                                    viewModel.registerDeveloper(devInputId, devInputName, devInputPass, "") { success, err ->
                                        devAuthMsg = if (success) "Account @$devInputId created & logged in!" else (err ?: "Registration failed")
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().testTag("dev_auth_submit_btn"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = if (devAuthMode == 0) "Sign In as Developer" else "Create Developer ID & Sign In",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        // LOGGED IN DEVELOPER BADGE
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(SuccessGreen),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "@${dev.developerId}",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${dev.developerName} (Authorized Developer)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = SuccessGreen,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            OutlinedButton(
                                onClick = { viewModel.logoutDeveloper() },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Text("Log Out", style = MaterialTheme.typography.labelSmall)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Aapke is ID ke sath upload kiya gaya koi bhi APK dusra developer edit nahi kar payega.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // If this developer has existing apps in catalog, show quick Uptodown style Update buttons
                        if (myApps.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "Your Existing Apps (Click to Add New Version):",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                myApps.forEach { myApp ->
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = MaterialTheme.colorScheme.background,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { viewModel.prefillForAppUpdate(myApp) }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                AsyncImage(
                                                    model = ImageRequest.Builder(context).data(myApp.logoUrl).crossfade(true).build(),
                                                    contentDescription = null,
                                                    modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)),
                                                    contentScale = ContentScale.Crop
                                                )
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Column {
                                                    Text(myApp.appName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                                    Text("Current: v${myApp.versionName} • ${myApp.getAllVersions().size} versions", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                            }

                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = MaterialTheme.colorScheme.primaryContainer
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(Icons.Default.Upgrade, contentDescription = null, modifier = Modifier.size(12.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Add Version", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // TARGET APP UPDATE BANNER (If updating an existing app)
            if (form.targetAppId != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = CyanPrimaryLight.copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Upgrade, contentDescription = null, tint = CyanPrimaryLight, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Adding New Version to '${form.appName}'", fontWeight = FontWeight.Bold)
                            }
                            Text(
                                "This update will be appended to the version history of '${form.appName}' (Uptodown style).",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        TextButton(onClick = { viewModel.resetPublishSuccess() }) {
                            Text("Cancel", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }

            // SUBMISSION ERROR BANNER (e.g. Access Denied / Developer mismatch)
            if (form.submissionError != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = form.submissionError!!,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

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

            // 11. APK Download URL (Telegram link / DevUploads / HTTPS)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "APK Download Link *",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )
                TextButton(
                    onClick = {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(telegramBotUrl))
                            context.startActivity(intent)
                        } catch (_: Exception) {}
                    }
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Upload on Telegram", style = MaterialTheme.typography.labelSmall)
                }
            }

            OutlinedTextField(
                value = form.downloadUrl,
                onValueChange = { viewModel.updatePublishDownloadUrl(it) },
                label = { Text("Paste Download Link (Telegram / DevUploads / HTTPS)") },
                placeholder = { Text("https://t.me/... or https://devuploads.com/...") },
                isError = form.downloadUrl.isNotEmpty() && !form.isDownloadUrlValid,
                supportingText = {
                    if (form.downloadUrl.isNotEmpty() && !form.isDownloadUrlValid) {
                        Text("Must be a secure HTTPS link from Telegram, DevUploads, or direct host", color = MaterialTheme.colorScheme.error)
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

    if (form.submissionSuccess) {
        AlertDialog(
            onDismissRequest = { viewModel.resetPublishSuccess() },
            icon = {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = SuccessGreen,
                    modifier = Modifier.size(40.dp)
                )
            },
            title = { Text("Application Published Live!", fontWeight = FontWeight.Bold) },
            text = {
                Text("Your listing has been successfully saved to NovaStore and is now live in the store catalog.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetPublishSuccess()
                        onNavigate(Screen.Home)
                    }
                ) {
                    Text("View in Store")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.resetPublishSuccess() }) {
                    Text("Publish Another")
                }
            }
        )
    }
}
