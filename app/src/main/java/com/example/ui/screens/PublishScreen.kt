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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Launch
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Upgrade
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.model.ApplicationItem
import com.example.model.DeveloperAccount
import com.example.model.StoreCategories
import com.example.ui.theme.CyanPrimaryLight
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.VioletSecondaryDark
import com.example.ui.theme.WarningAmber
import com.example.viewmodel.PublishFormState
import com.example.viewmodel.Screen
import com.example.viewmodel.StoreViewModel
import kotlinx.coroutines.launch

/**
 * 3-Page APK Upload Flow:
 * Page 1: Developer Login / Create ID & Password Page
 * Page 2: APK Upload Details Page (App details, version, download link, changelog)
 * Page 3: Congratulations Page (Displays Developer ID, Live status & Instant user access)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PublishScreen(
    viewModel: StoreViewModel,
    onNavigate: (Screen) -> Unit
) {
    val context = LocalContext.current
    val form by viewModel.publishForm.collectAsState()
    val telegramBotUrl by viewModel.telegramBotUrl.collectAsState()
    val currentDeveloper by viewModel.currentDeveloper.collectAsState()
    val allApps by viewModel.repository.applications.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // 3-Step Wizard state: 1 = Login/Auth, 2 = Details Form, 3 = Congratulations
    var currentStep by remember(form.submissionSuccess) {
        mutableIntStateOf(
            if (form.submissionSuccess) 3
            else if (currentDeveloper != null) 2
            else 1
        )
    }

    // Handle Hardware Back Button
    BackHandler {
        when (currentStep) {
            3 -> {
                viewModel.resetPublishSuccess()
                onNavigate(Screen.Home)
            }
            2 -> {
                if (currentDeveloper == null) {
                    currentStep = 1
                } else {
                    onNavigate(Screen.Home)
                }
            }
            else -> onNavigate(Screen.Home)
        }
    }

    val pageTitle = when (currentStep) {
        1 -> "Developer Account (1/3)"
        2 -> "APK Upload Details (2/3)"
        3 -> "Congratulations (3/3)"
        else -> "Publish APK"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(pageTitle, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            when (currentStep) {
                                3 -> {
                                    viewModel.resetPublishSuccess()
                                    onNavigate(Screen.Home)
                                }
                                2 -> currentStep = 1
                                else -> onNavigate(Screen.Home)
                            }
                        },
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
        ) {
            // STEP PROGRESS INDICATOR
            PublishStepProgressIndicator(
                currentStep = currentStep,
                developerId = currentDeveloper?.developerId,
                onStepSelected = { step ->
                    if (step == 1) {
                        currentStep = 1
                    } else if (step == 2 && currentDeveloper != null) {
                        currentStep = 2
                    }
                }
            )

            // PAGE ROUTER
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                when (currentStep) {
                    1 -> {
                        PublishPage1Login(
                            currentDeveloper = currentDeveloper,
                            allApps = allApps,
                            onLogin = { id, pass, onResult ->
                                viewModel.loginDeveloper(id, pass) { success, err ->
                                    if (success) {
                                        currentStep = 2
                                    }
                                    onResult(success, err)
                                }
                            },
                            onRegister = { id, name, pass, contact, onResult ->
                                viewModel.registerDeveloper(id, name, pass, contact) { success, err ->
                                    if (success) {
                                        currentStep = 2
                                    }
                                    onResult(success, err)
                                }
                            },
                            onLogout = { viewModel.logoutDeveloper() },
                            onProceedToStep2 = { currentStep = 2 },
                            onSelectAppForUpdate = { app ->
                                viewModel.prefillForAppUpdate(app)
                                currentStep = 2
                            }
                        )
                    }
                    2 -> {
                        PublishPage2Details(
                            form = form,
                            currentDeveloper = currentDeveloper,
                            telegramBotUrl = telegramBotUrl,
                            viewModel = viewModel,
                            onChangeDeveloper = { currentStep = 1 },
                            onPublishSuccess = {
                                scope.launch {
                                    snackbarHostState.showSnackbar("Listing published successfully!")
                                }
                                currentStep = 3
                            }
                        )
                    }
                    3 -> {
                        PublishPage3Congratulations(
                            lastPublishedApp = form.lastPublishedApp,
                            currentDeveloper = currentDeveloper,
                            onViewInStore = {
                                viewModel.resetPublishSuccess()
                                onNavigate(Screen.Home)
                            },
                            onOpenAppDetails = { appId ->
                                viewModel.resetPublishSuccess()
                                viewModel.openAppDetails(appId)
                            },
                            onUploadAnother = {
                                viewModel.resetPublishSuccess()
                                currentStep = 2
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * 3-Step Wizard Navigation Bar
 */
@Composable
private fun PublishStepProgressIndicator(
    currentStep: Int,
    developerId: String?,
    onStepSelected: (Int) -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Step 1 Chip
            StepBadge(
                step = 1,
                label = if (!developerId.isNullOrBlank()) "@$developerId" else "1. Login ID",
                isActive = currentStep == 1,
                isCompleted = currentStep > 1 || developerId != null,
                onClick = { onStepSelected(1) },
                modifier = Modifier.weight(1f)
            )

            HorizontalDivider(
                modifier = Modifier
                    .width(16.dp)
                    .padding(horizontal = 4.dp),
                color = if (currentStep > 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
            )

            // Step 2 Chip
            StepBadge(
                step = 2,
                label = "2. APK Details",
                isActive = currentStep == 2,
                isCompleted = currentStep > 2,
                onClick = { onStepSelected(2) },
                modifier = Modifier.weight(1f)
            )

            HorizontalDivider(
                modifier = Modifier
                    .width(16.dp)
                    .padding(horizontal = 4.dp),
                color = if (currentStep > 2) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
            )

            // Step 3 Chip
            StepBadge(
                step = 3,
                label = "3. Success 🎉",
                isActive = currentStep == 3,
                isCompleted = false,
                onClick = { /* Read only until completed */ },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun StepBadge(
    step: Int,
    label: String,
    isActive: Boolean,
    isCompleted: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor = when {
        isActive -> MaterialTheme.colorScheme.primary
        isCompleted -> SuccessGreen.copy(alpha = 0.2f)
        else -> MaterialTheme.colorScheme.surface
    }
    val contentColor = when {
        isActive -> Color.White
        isCompleted -> SuccessGreen
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = containerColor,
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(enabled = isCompleted || isActive, onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (isCompleted && !isActive) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = SuccessGreen,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium
                ),
                color = contentColor,
                maxLines = 1
            )
        }
    }
}

/**
 * PAGE 1: Developer Login / Create ID & Password Page
 */
@Composable
private fun PublishPage1Login(
    currentDeveloper: DeveloperAccount?,
    allApps: List<ApplicationItem>,
    onLogin: (String, String, (Boolean, String?) -> Unit) -> Unit,
    onRegister: (String, String, String, String, (Boolean, String?) -> Unit) -> Unit,
    onLogout: () -> Unit,
    onProceedToStep2: () -> Unit,
    onSelectAppForUpdate: (ApplicationItem) -> Unit
) {
    var authMode by remember { mutableIntStateOf(0) } // 0 = Sign In, 1 = Create Account
    var devId by remember { mutableStateOf("") }
    var devName by remember { mutableStateOf("") }
    var devPass by remember { mutableStateOf("") }
    var devContact by remember { mutableStateOf("") }
    var passVisible by remember { mutableStateOf(false) }
    var authMessage by remember { mutableStateOf<String?>(null) }
    var isAuthenticating by remember { mutableStateOf(false) }

    val myApps = remember(allApps, currentDeveloper) {
        val devHandle = currentDeveloper?.developerId
        if (devHandle.isNullOrBlank()) emptyList()
        else allApps.filter { it.developerId.equals(devHandle, ignoreCase = true) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        if (currentDeveloper != null) {
            // DEVELOPER ALREADY LOGGED IN CARD
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("logged_in_dev_card"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                ),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(SuccessGreen),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "@${currentDeveloper.developerId}",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${currentDeveloper.developerName} (Authorized Developer)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SuccessGreen,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = onLogout,
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Text("Log Out", style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Aapka Developer ID authenticated hai! Is ID ke sath upload kiye gaye sabhi APKs encrypted hain aur koi dusra developer inhe edit ya overwrite nahi kar sakta.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Button(
                        onClick = onProceedToStep2,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("proceed_to_step2_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Proceed to Step 2: APK Upload Details", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                }
            }

            // Quick App Selector for Updates (Uptodown Style)
            if (myApps.isNotEmpty()) {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Your Uploaded APKs (${myApps.size}):",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Naya version upload karne ke liye apna app select karein (Uptodown style multi-version):",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    myApps.forEach { app ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectAppForUpdate(app) }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(MaterialTheme.colorScheme.surface),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(app.appName.take(1), fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(app.appName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                        Text("Current: v${app.versionName} (${app.getAllVersions().size} versions)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
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
        } else {
            // HERO AUTHENTICATION HEADER
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                )
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Developer Login & Registration",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Apna Developer ID & Password banayein ya sign in karein",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "🔒 Security Protection:\nHar developer ka apna unique Developer ID aur Password hota hai. Aapke ID se upload kiya gaya APK sirf aap hi update ya edit kar payenge, koi dusra developer use replace nahi kar sakta.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // TAB SWITCHER: SIGN IN VS CREATE ACCOUNT
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(4.dp)
            ) {
                Button(
                    onClick = { authMode = 0; authMessage = null },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (authMode == 0) MaterialTheme.colorScheme.primary else Color.Transparent,
                        contentColor = if (authMode == 0) Color.White else MaterialTheme.colorScheme.onSurface
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    Text("Developer Sign In", fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = { authMode = 1; authMessage = null },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (authMode == 1) MaterialTheme.colorScheme.primary else Color.Transparent,
                        contentColor = if (authMode == 1) Color.White else MaterialTheme.colorScheme.onSurface
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    Text("Create ID & Password", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // DEVELOPER ID INPUT
            OutlinedTextField(
                value = devId,
                onValueChange = { devId = it.trim().lowercase().replace(" ", "_") },
                label = { Text("Developer ID / Handle (e.g. rohit_apps) *") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(20.dp)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("dev_auth_id_input"),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            if (authMode == 1) {
                Spacer(modifier = Modifier.height(12.dp))
                // DEVELOPER STUDIO NAME INPUT
                OutlinedTextField(
                    value = devName,
                    onValueChange = { devName = it },
                    label = { Text("Developer / Studio Display Name *") },
                    placeholder = { Text("e.g. Rohit Mobile Studios") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dev_auth_name_input"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))
                // OPTIONAL CONTACT
                OutlinedTextField(
                    value = devContact,
                    onValueChange = { devContact = it },
                    label = { Text("Contact Email / Telegram (Optional)") },
                    placeholder = { Text("contact@rohitapps.com or @username") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // PASSWORD INPUT
            OutlinedTextField(
                value = devPass,
                onValueChange = { devPass = it },
                label = { Text("Password *") },
                visualTransformation = if (passVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { passVisible = !passVisible }) {
                        Icon(
                            imageVector = if (passVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = "Toggle Password"
                        )
                    }
                },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(20.dp)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("dev_auth_pass_input"),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            // ERROR / SUCCESS MESSAGE BANNER
            if (authMessage != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (authMessage!!.contains("success", ignoreCase = true) || authMessage!!.contains("created", ignoreCase = true)) SuccessGreen.copy(alpha = 0.15f) else MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = authMessage!!,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (authMessage!!.contains("success", ignoreCase = true) || authMessage!!.contains("created", ignoreCase = true)) SuccessGreen else MaterialTheme.colorScheme.onErrorContainer,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            // PRIMARY SUBMIT BUTTON
            Button(
                onClick = {
                    if (devId.isBlank() || devPass.isBlank()) {
                        authMessage = "Please enter both Developer ID and Password."
                        return@Button
                    }
                    if (authMode == 1 && devName.isBlank()) {
                        authMessage = "Please enter your Developer or Studio Name."
                        return@Button
                    }
                    isAuthenticating = true
                    if (authMode == 0) {
                        onLogin(devId, devPass) { success, err ->
                            isAuthenticating = false
                            if (!success) {
                                authMessage = err ?: "Invalid Developer ID or Password."
                            }
                        }
                    } else {
                        onRegister(devId, devName, devPass, devContact) { success, err ->
                            isAuthenticating = false
                            if (!success) {
                                authMessage = err ?: "Registration failed. Try a different ID."
                            }
                        }
                    }
                },
                enabled = !isAuthenticating,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("dev_auth_submit_btn"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                if (isAuthenticating) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Authenticating...")
                } else {
                    Icon(
                        imageVector = if (authMode == 0) Icons.Default.Lock else Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (authMode == 0) "Sign In & Proceed to Upload" else "Create ID & Proceed to Upload",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}

/**
 * PAGE 2: APK Upload Details Page (App details, version, download link, changelog)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PublishPage2Details(
    form: PublishFormState,
    currentDeveloper: DeveloperAccount?,
    telegramBotUrl: String,
    viewModel: StoreViewModel,
    onChangeDeveloper: () -> Unit,
    onPublishSuccess: () -> Unit
) {
    val context = LocalContext.current
    var newScreenshotUrl by remember { mutableStateOf("") }
    var categoryExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // TOP DEVELOPER STRIP
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Publisher: @${currentDeveloper?.developerId ?: form.developerName.ifBlank { "anonymous" }}",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                TextButton(
                    onClick = onChangeDeveloper,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text("Change Account", style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // TELEGRAM APK UPLOAD QUICK HELPER
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = CyanPrimaryLight.copy(alpha = 0.15f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(CyanPrimaryLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text("Upload APK via Telegram", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        Text("Upload file to generate download link", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Button(
                    onClick = {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(telegramBotUrl))
                            context.startActivity(intent)
                        } catch (_: Exception) {}
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CyanPrimaryLight),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text("Open Telegram", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // TARGET APP BANNER (If updating existing app - Uptodown style)
        if (form.targetAppId != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = SuccessGreen.copy(alpha = 0.15f))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Upgrade, contentDescription = null, tint = SuccessGreen)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Adding New Version to '${form.appName}'", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                            Text("Yeh naya version purani app ke andar version list mein judega (Uptodown style)", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    TextButton(onClick = { viewModel.resetPublishSuccess() }) {
                        Text("New App Instead", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }

        // SUBMISSION ERROR (e.g., Ownership check or validation)
        if (form.submissionError != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = form.submissionError!!,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        Text("Application Details", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
        Text("Sabhi jaruri details fill karein aur Upload button dabayein.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(14.dp))

        // 1. App Name
        OutlinedTextField(
            value = form.appName,
            onValueChange = { viewModel.updatePublishAppName(it) },
            label = { Text("Application Name * (e.g. CapCut Pro)") },
            isError = form.appName.isNotEmpty() && !form.isAppNameValid,
            modifier = Modifier.fillMaxWidth().testTag("input_app_name"),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(10.dp))

        // 2. Developer Studio Display Name
        OutlinedTextField(
            value = form.developerName,
            onValueChange = { viewModel.updatePublishDevName(it) },
            label = { Text("Developer / Studio Display Name *") },
            modifier = Modifier.fillMaxWidth().testTag("input_dev_name"),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(10.dp))

        // 3. Category Dropdown
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
                modifier = Modifier.menuAnchor().fillMaxWidth().testTag("input_category"),
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

        Spacer(modifier = Modifier.height(10.dp))

        // 4 & 5. Version Name & Version Code
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(
                value = form.versionName,
                onValueChange = { viewModel.updatePublishVersionName(it) },
                label = { Text("Version Name * (e.g. 2.1.0)") },
                placeholder = { Text("1.0.0") },
                modifier = Modifier.weight(1f).testTag("input_version_name"),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            OutlinedTextField(
                value = form.versionCode,
                onValueChange = { viewModel.updatePublishVersionCode(it) },
                label = { Text("Version Code * (e.g. 2)") },
                placeholder = { Text("1") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f).testTag("input_version_code"),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 6. Direct APK Download Link
        OutlinedTextField(
            value = form.downloadUrl,
            onValueChange = { viewModel.updatePublishDownloadUrl(it) },
            label = { Text("Direct APK Download URL (HTTPS / Telegram) *") },
            placeholder = { Text("https://t.me/... or https://devuploads.com/...") },
            isError = form.downloadUrl.isNotEmpty() && !form.isDownloadUrlValid,
            supportingText = {
                if (form.downloadUrl.isNotEmpty() && !form.isDownloadUrlValid) {
                    Text("Valid HTTPS or Telegram download link required", color = MaterialTheme.colorScheme.error)
                }
            },
            modifier = Modifier.fillMaxWidth().testTag("input_download_url"),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(10.dp))

        // 7. Changelog / What's New
        OutlinedTextField(
            value = form.changelog,
            onValueChange = { viewModel.updatePublishChangelog(it) },
            label = { Text("What's New in this Version / Changelog *") },
            placeholder = { Text("e.g. Added dark mode, fixed video export bug") },
            modifier = Modifier.fillMaxWidth().testTag("input_changelog"),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(10.dp))

        // 8. Logo / Icon URL
        OutlinedTextField(
            value = form.logoUrl,
            onValueChange = { viewModel.updatePublishLogoUrl(it) },
            label = { Text("App Icon URL (HTTPS) - Optional") },
            placeholder = { Text("https://example.com/icon.png") },
            modifier = Modifier.fillMaxWidth().testTag("input_logo_url"),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )

        if (form.logoUrl.isNotBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context).data(form.logoUrl).crossfade(true).build(),
                    contentDescription = "Icon Preview",
                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text("Icon Live Preview", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 9. Short Description
        OutlinedTextField(
            value = form.shortDescription,
            onValueChange = { viewModel.updatePublishShortDesc(it) },
            label = { Text("Short Description (10-150 characters) *") },
            isError = form.shortDescription.isNotEmpty() && !form.isShortDescValid,
            modifier = Modifier.fillMaxWidth().testTag("input_short_desc"),
            shape = RoundedCornerShape(12.dp),
            maxLines = 2
        )

        Spacer(modifier = Modifier.height(10.dp))

        // 10. Full Description
        OutlinedTextField(
            value = form.fullDescription,
            onValueChange = { viewModel.updatePublishFullDesc(it) },
            label = { Text("Full Description (at least 20 characters) *") },
            modifier = Modifier.fillMaxWidth().testTag("input_full_desc"),
            shape = RoundedCornerShape(12.dp),
            minLines = 3,
            maxLines = 6
        )

        Spacer(modifier = Modifier.height(10.dp))

        // 11. Support Contact & Permissions
        OutlinedTextField(
            value = form.supportUrl,
            onValueChange = { viewModel.updatePublishSupport(it) },
            label = { Text("Support Contact / Telegram / GitHub *") },
            placeholder = { Text("https://t.me/support or contact@developer.com") },
            modifier = Modifier.fillMaxWidth().testTag("input_support_url"),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = form.permissionsDisclosure,
            onValueChange = { viewModel.updatePublishPermissions(it) },
            label = { Text("Permissions Disclosure *") },
            placeholder = { Text("INTERNET, STORAGE (standard network access)") },
            modifier = Modifier.fillMaxWidth().testTag("input_permissions"),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(14.dp))

        // SCREENSHOTS
        Text("Screenshots (Optional)", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = newScreenshotUrl,
                onValueChange = { newScreenshotUrl = it },
                placeholder = { Text("https://example.com/screen1.png") },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp),
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
                Icon(Icons.Default.Add, contentDescription = "Add Screenshot")
            }
        }

        form.screenshotUrls.forEachIndexed { idx, url ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("${idx + 1}. $url", maxLines = 1, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                IconButton(onClick = { viewModel.removeScreenshotUrl(idx) }) {
                    Icon(Icons.Default.Delete, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // MANDATORY AUTHORIZATIONS
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("Authorizations & Declarations", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(6.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = form.hasDistributionRights,
                        onCheckedChange = { viewModel.toggleDistributionRights(it) },
                        modifier = Modifier.testTag("checkbox_rights")
                    )
                    Text("I have legal permission to publish and distribute this APK.", style = MaterialTheme.typography.bodySmall)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = form.isInfoAccurate,
                        onCheckedChange = { viewModel.toggleInfoAccurate(it) },
                        modifier = Modifier.testTag("checkbox_accurate")
                    )
                    Text("All entered versions, links, and details are accurate.", style = MaterialTheme.typography.bodySmall)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = form.agreesToRules,
                        onCheckedChange = { viewModel.toggleAgreesToRules(it) },
                        modifier = Modifier.testTag("checkbox_rules")
                    )
                    Text("I agree to NovaStore developer policies.", style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // UPLOAD & PUBLISH BUTTON
        Button(
            onClick = {
                viewModel.submitPublishForm {
                    onPublishSuccess()
                }
            },
            enabled = form.isFormValid && !form.isSubmitting,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("submit_publish_button")
        ) {
            if (form.isSubmitting) {
                CircularProgressIndicator(modifier = Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(10.dp))
                Text("Uploading & Syncing to Store Catalog...", fontWeight = FontWeight.Bold)
            } else {
                Icon(Icons.Default.CloudDone, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (form.isFormValid) "Upload & Publish APK Now" else "Complete Required Fields & Checkboxes",
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}

/**
 * PAGE 3: Congratulations Page (Displays Developer ID, Live Status & Direct Actions)
 */
@Composable
private fun PublishPage3Congratulations(
    lastPublishedApp: ApplicationItem?,
    currentDeveloper: DeveloperAccount?,
    onViewInStore: () -> Unit,
    onOpenAppDetails: (String) -> Unit,
    onUploadAnother: () -> Unit
) {
    val devHandle = currentDeveloper?.developerId ?: lastPublishedApp?.developerId ?: "developer"
    val devName = currentDeveloper?.developerName ?: lastPublishedApp?.developerName ?: "Verified Developer"
    val appName = lastPublishedApp?.appName ?: "Your Application"
    val versionName = lastPublishedApp?.versionName ?: "Latest"
    val category = lastPublishedApp?.category ?: "Tools"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(10.dp))

        // BIG CELEBRATION ICON
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(SuccessGreen, CyanPrimaryLight)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Success",
                tint = Color.White,
                modifier = Modifier.size(60.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "🎉 Congratulations!",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "APK Uploaded & Published Successfully!",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = SuccessGreen,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        // DEVELOPER & APP DETAILS CARD
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("publish_congrats_card"),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
            border = androidx.compose.foundation.BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                // Developer Identity
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(SuccessGreen),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text("Uploaded by Developer:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("@$devHandle ($devName)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                // App Details Grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Application", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(appName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Version", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("v$versionName", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium, color = CyanPrimaryLight)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Category", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(category, fontWeight = FontWeight.Medium, style = MaterialTheme.typography.bodyMedium)
                    }
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = SuccessGreen.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "🟢 LIVE IN STORE",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = SuccessGreen
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // REALTIME VISIBILITY NOTICE
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = CyanPrimaryLight.copy(alpha = 0.12f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CloudDone, contentDescription = null, tint = CyanPrimaryLight, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Aapka APK turant sabhi users ko home screen aur category list mein live show ho gaya hai. Ab koi bhi user direct download kar sakta hai!",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 18.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // ACTION BUTTON 1: VIEW IN STORE
        Button(
            onClick = onViewInStore,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("congrats_view_store_btn"),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Icon(Icons.Default.Storefront, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("View in Store Catalog (Home)", fontWeight = FontWeight.Bold)
        }

        // ACTION BUTTON 2: OPEN APP DETAILS (if appId present)
        if (lastPublishedApp != null && lastPublishedApp.appId.isNotBlank()) {
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedButton(
                onClick = { onOpenAppDetails(lastPublishedApp.appId) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("congrats_open_details_btn"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Launch, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Open App Details Page", fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // ACTION BUTTON 3: UPLOAD ANOTHER APP
        TextButton(
            onClick = onUploadAnother,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("congrats_upload_another_btn")
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Upload Another APK / Add Version", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}
