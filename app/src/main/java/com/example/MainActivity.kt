package com.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.example.security.SecurityIntegrityChecker
import com.example.ui.components.AdMobBannerView
import com.example.ui.components.BottomNavBar
import com.example.ui.components.RewardedAdManager
import com.example.ui.dialogs.DownloadConfirmDialog
import com.example.ui.dialogs.ReportAppDialog
import com.example.ui.dialogs.UpdateDialog
import com.example.ui.screens.AdminScreen
import com.example.ui.screens.DetailScreen
import com.example.ui.screens.DisclaimerScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.PublishScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.Screen
import com.example.viewmodel.StoreViewModel
import com.google.android.gms.ads.MobileAds

class MainActivity : ComponentActivity() {
    private val viewModel: StoreViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Anti-tamper & anti-modding integrity verification: terminates process if modded
        SecurityIntegrityChecker.checkIntegrityOrCrash(this)
        try {
            MobileAds.initialize(this) {}
        } catch (_: Exception) {}
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                val currentScreen by viewModel.currentScreen.collectAsState()
                val pendingDownload by viewModel.pendingDownloadApp.collectAsState()
                val pendingReport by viewModel.pendingReportApp.collectAsState()
                val updateInfo by viewModel.updateInfo.collectAsState()
                val apps by viewModel.repository.applications.collectAsState()
                val admobConfig by viewModel.admobConfig.collectAsState()
                val rewardedAdManager = remember { RewardedAdManager(this@MainActivity) }

                LaunchedEffect(admobConfig) {
                    rewardedAdManager.loadAd(admobConfig)
                }

                if (currentScreen is Screen.Disclaimer) {
                    DisclaimerScreen(
                        onAccept = { viewModel.acceptDisclaimer() }
                    )
                } else {
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        bottomBar = {
                            Column {
                                AdMobBannerView(config = admobConfig)
                                if (currentScreen !is Screen.Details) {
                                    BottomNavBar(
                                        currentScreen = currentScreen,
                                        onNavigate = { screen -> viewModel.navigateTo(screen) }
                                    )
                                }
                            }
                        }
                    ) { innerPadding ->
                        when (val screen = currentScreen) {
                            is Screen.Home -> {
                                HomeScreen(
                                    viewModel = viewModel,
                                    onNavigate = { dest -> viewModel.navigateTo(dest) }
                                )
                            }
                            is Screen.Details -> {
                                val app = apps.find { it.appId == screen.appId }
                                DetailScreen(
                                    app = app,
                                    onBack = { viewModel.navigateTo(Screen.Home) },
                                    onDownloadClick = { targetApp -> viewModel.requestDownload(targetApp) },
                                    onReportClick = { targetApp -> viewModel.requestReport(targetApp) }
                                )
                            }
                            is Screen.Publish -> {
                                PublishScreen(
                                    viewModel = viewModel,
                                    onNavigate = { dest -> viewModel.navigateTo(dest) }
                                )
                            }
                            is Screen.Settings -> {
                                SettingsScreen(
                                    viewModel = viewModel,
                                    onNavigate = { dest -> viewModel.navigateTo(dest) }
                                )
                            }
                            is Screen.Admin -> {
                                AdminScreen(
                                    viewModel = viewModel,
                                    onNavigate = { dest -> viewModel.navigateTo(dest) }
                                )
                            }
                            is Screen.Disclaimer -> {
                                // Handled above
                            }
                        }
                    }
                }

                // External Download Safety Dialog
                pendingDownload?.let { app ->
                    DownloadConfirmDialog(
                        app = app,
                        onConfirm = { url ->
                            viewModel.dismissDownloadDialog()
                            rewardedAdManager.showRewardedAd(
                                activity = this@MainActivity,
                                config = admobConfig,
                                onRewardEarned = {
                                    Toast.makeText(this@MainActivity, "Sponsored ad completed. Starting download...", Toast.LENGTH_SHORT).show()
                                },
                                onAdClosed = {
                                    openExternalUrl(url)
                                }
                            )
                        },
                        onDismiss = { viewModel.dismissDownloadDialog() }
                    )
                }

                // Report Abuse Dialog
                pendingReport?.let { app ->
                    ReportAppDialog(
                        app = app,
                        onSubmit = { reason, details ->
                            viewModel.submitReport(reason, details)
                            Toast.makeText(this@MainActivity, "Report submitted to moderators", Toast.LENGTH_SHORT).show()
                        },
                        onDismiss = { viewModel.dismissReportDialog() }
                    )
                }

                // Remote App Update Dialog
                updateInfo?.let { info ->
                    UpdateDialog(
                        updateInfo = info,
                        onUpdateClick = { url ->
                            viewModel.dismissUpdateDialog()
                            if (url.isNotBlank()) {
                                openExternalUrl(url)
                            }
                        },
                        onDismiss = { viewModel.dismissUpdateDialog() }
                    )
                }
            }
        }
    }

    private fun openExternalUrl(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Unable to open link: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
