package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.model.ApplicationItem
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.WarningAmber
import com.example.viewmodel.Screen
import com.example.viewmodel.StoreViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(
    viewModel: StoreViewModel,
    onNavigate: (Screen) -> Unit
) {
    BackHandler {
        onNavigate(Screen.Settings)
    }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val allApps by viewModel.repository.applications.collectAsState()
    val isAdminLoggedIn by viewModel.isAdminLoggedIn.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    var editingApp by remember { mutableStateOf<ApplicationItem?>(null) }
    var deletingAppId by remember { mutableStateOf<String?>(null) }

    if (!isAdminLoggedIn) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Admin login required to access moderation.")
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = { onNavigate(Screen.Settings) }) {
                    Text("Go to Login")
                }
            }
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Admin Moderation Console", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(
                        onClick = { onNavigate(Screen.Settings) },
                        modifier = Modifier.testTag("admin_back_button")
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            viewModel.logoutAdmin()
                            onNavigate(Screen.Settings)
                        }
                    ) {
                        Icon(Icons.Default.Logout, contentDescription = "Logout", tint = ErrorRed)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Stats Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AdminStatCard(
                    title = "Total Apps",
                    value = "${allApps.size}",
                    modifier = Modifier.weight(1f)
                )
                AdminStatCard(
                    title = "Flagged Apps",
                    value = "${allApps.count { it.reportCount > 0 }}",
                    modifier = Modifier.weight(1f)
                )
                AdminStatCard(
                    title = "Admin User",
                    value = viewModel.preferencesManager.getAdminUsername(),
                    modifier = Modifier.weight(1f)
                )
            }

    var customConfigUrl by remember { mutableStateOf(viewModel.preferencesManager.getUpdateConfigUrl()) }

            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Catalog (${allApps.size})") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Flagged (${allApps.count { it.reportCount > 0 }})") }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Update Config") }
                )
            }

            if (selectedTab == 2) {
                val updateInfo by viewModel.updateInfo.collectAsState()
                val isChecking by viewModel.isCheckingUpdate.collectAsState()

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("GitHub app-update.json Endpoint", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "Configure your remote JSON endpoint. For real-time updates without downloading new APKs, this repository must be PUBLIC on GitHub.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = customConfigUrl,
                                onValueChange = { customConfigUrl = it },
                                label = { Text("Raw GitHub URL") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp)
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        viewModel.preferencesManager.setUpdateConfigUrl(customConfigUrl.trim())
                                        viewModel.checkAppUpdates()
                                        Toast.makeText(context, "URL saved & synchronized!", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Save URL")
                                }

                                OutlinedButton(
                                    onClick = { viewModel.checkAppUpdates(silent = false) },
                                    enabled = !isChecking,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(if (isChecking) "Checking..." else "Test Sync")
                                }
                            }
                        }
                    }

                    // Active Live Status Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (updateInfo?.forceUpdate == true) ErrorRed.copy(alpha = 0.12f) else SuccessGreen.copy(alpha = 0.12f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (updateInfo?.forceUpdate == true) Icons.Default.Info else Icons.Default.Check,
                                    contentDescription = null,
                                    tint = if (updateInfo?.forceUpdate == true) ErrorRed else SuccessGreen
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (updateInfo?.forceUpdate == true) "FORCE UPDATE ACTIVE (App Locked)" else "APP UNLOCKED (Normal Mode)",
                                    fontWeight = FontWeight.Bold,
                                    color = if (updateInfo?.forceUpdate == true) ErrorRed else SuccessGreen
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Latest Remote Version: v${updateInfo?.latestVersionName ?: "1.0.0"} (Build ${updateInfo?.latestVersionCode ?: 1})\nForce Update Status: ${updateInfo?.forceUpdate == true}\nPolling Interval: Auto-sync every 8 seconds",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // Instructions Card for User
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Troubleshooting & GitHub Guide", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "1. GitHub Visibility: Go to github.com -> Repository Settings -> Change to PUBLIC. If private, GitHub blocks raw access with 404.\n\n" +
                                "2. Real-time Force Update: Edit app-update.json directly on GitHub. Setting 'forceUpdate': true will instantly lock user APKs within 8 seconds without requiring any APK re-download. Setting 'forceUpdate': false instantly unlocks it.\n\n" +
                                "3. Package Conflict Fix: If 'Package conflicts with an existing package' appears on your phone, uninstall the previously installed NovaStore app once from your device. Future updates will then install smoothly.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            } else {
                val displayList = if (selectedTab == 0) allApps else allApps.filter { it.reportCount > 0 }

                if (displayList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (selectedTab == 0) "No applications listed." else "No flagged applications reported.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(displayList) { app ->
                            AdminAppItem(
                                app = app,
                                onEdit = { editingApp = app },
                                onStatusChange = { newStatus ->
                                    scope.launch {
                                        viewModel.repository.updateAppStatus(app.appId, newStatus)
                                        Toast.makeText(context, "Status updated to $newStatus", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                onDelete = { deletingAppId = app.appId }
                            )
                        }
                    }
                }
            }
        }
    }

    // Edit Application Dialog
    editingApp?.let { targetApp ->
        EditAppDialog(
            app = targetApp,
            onSave = { updated ->
                viewModel.updateAppAsAdmin(updated) {
                    Toast.makeText(context, "Application updated successfully", Toast.LENGTH_SHORT).show()
                    editingApp = null
                }
            },
            onDismiss = { editingApp = null }
        )
    }

    // Delete Confirmation Dialog
    deletingAppId?.let { targetId ->
        AlertDialog(
            onDismissRequest = { deletingAppId = null },
            title = { Text("Confirm Deletion", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to permanently delete this application listing from NovaStore?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteAppAsAdmin(targetId) {
                            Toast.makeText(context, "Application listing deleted", Toast.LENGTH_SHORT).show()
                            deletingAppId = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { deletingAppId = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun EditAppDialog(
    app: ApplicationItem,
    onSave: (ApplicationItem) -> Unit,
    onDismiss: () -> Unit
) {
    var editName by remember { mutableStateOf(app.appName) }
    var editDev by remember { mutableStateOf(app.developerName) }
    var editVersion by remember { mutableStateOf(app.versionName) }
    var editDownloadUrl by remember { mutableStateOf(app.downloadUrl) }
    var editCategory by remember { mutableStateOf(app.category) }
    var editShortDesc by remember { mutableStateOf(app.shortDescription) }
    var editFullDesc by remember { mutableStateOf(app.fullDescription) }
    var editStatus by remember { mutableStateOf(app.publicationStatus) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Application Details", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .height(380.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = editName,
                    onValueChange = { editName = it },
                    label = { Text("App Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = editDev,
                    onValueChange = { editDev = it },
                    label = { Text("Developer") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = editVersion,
                    onValueChange = { editVersion = it },
                    label = { Text("Version Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = editDownloadUrl,
                    onValueChange = { editDownloadUrl = it },
                    label = { Text("Download URL (HTTPS)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = editCategory,
                    onValueChange = { editCategory = it },
                    label = { Text("Category") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = editShortDesc,
                    onValueChange = { editShortDesc = it },
                    label = { Text("Short Description") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = editFullDesc,
                    onValueChange = { editFullDesc = it },
                    label = { Text("Full Description") },
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = editStatus,
                    onValueChange = { editStatus = it },
                    label = { Text("Status (published/pending/rejected)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        app.copy(
                            appName = editName.trim(),
                            developerName = editDev.trim(),
                            versionName = editVersion.trim(),
                            downloadUrl = editDownloadUrl.trim(),
                            category = editCategory.trim(),
                            shortDescription = editShortDesc.trim(),
                            fullDescription = editFullDesc.trim(),
                            publicationStatus = editStatus.trim().lowercase()
                        )
                    )
                }
            ) {
                Text("Save Changes")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun AdminAppItem(
    app: ApplicationItem,
    onEdit: () -> Unit,
    onStatusChange: (String) -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(app.logoUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        modifier = Modifier.size(46.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = app.appName,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Dev: ${app.developerName} • v${app.versionName}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }

                if (app.reportCount > 0) {
                    Surface(
                        color = ErrorRed.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Flag, contentDescription = null, tint = ErrorRed, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = "${app.reportCount} reports",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, color = ErrorRed)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Download URL: ${app.downloadUrl}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = when (app.publicationStatus) {
                        "published" -> SuccessGreen.copy(alpha = 0.15f)
                        "rejected" -> ErrorRed.copy(alpha = 0.15f)
                        else -> WarningAmber.copy(alpha = 0.15f)
                    },
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = app.publicationStatus.uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = when (app.publicationStatus) {
                                "published" -> SuccessGreen
                                "rejected" -> ErrorRed
                                else -> WarningAmber
                            }
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary)
                }

                if (app.publicationStatus != "published") {
                    IconButton(onClick = { onStatusChange("published") }) {
                        Icon(Icons.Default.Check, contentDescription = "Approve", tint = SuccessGreen)
                    }
                }

                if (app.publicationStatus != "rejected") {
                    IconButton(onClick = { onStatusChange("rejected") }) {
                        Icon(Icons.Default.Close, contentDescription = "Reject", tint = WarningAmber)
                    }
                }

                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = ErrorRed)
                }
            }
        }
    }
}

@Composable
fun AdminStatCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
        }
    }
}
