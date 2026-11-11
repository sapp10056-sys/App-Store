package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.viewmodel.Screen
import com.example.viewmodel.StoreViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: StoreViewModel,
    onNavigate: (Screen) -> Unit
) {
    BackHandler {
        onNavigate(Screen.Home)
    }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentDeveloper by viewModel.currentDeveloper.collectAsState()
    val allApps by viewModel.repository.applications.collectAsState()
    val userTokens by viewModel.userTokens.collectAsState()
    val purchasedTokens by viewModel.tokenManager.purchasedTokens.collectAsState()
    val activeReferralTokens by viewModel.tokenManager.activeReferralTokens.collectAsState()
    val referralCode = viewModel.tokenManager.referralCode
    val hasRedeemed by viewModel.tokenManager.hasRedeemedReferral.collectAsState()

    var referralInput by remember { mutableStateOf("") }
    var redeemMessage by remember { mutableStateOf<String?>(null) }
    var isRedeeming by remember { mutableStateOf(false) }

    val myUploadedApps = remember(allApps, currentDeveloper) {
        val devId = currentDeveloper?.developerId
        if (devId.isNullOrBlank()) emptyList()
        else allApps.filter { it.developerId.equals(devId, ignoreCase = true) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Profile & Wallet", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { onNavigate(Screen.Home) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Profile Header Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(36.dp))
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = if (currentDeveloper != null) "@${currentDeveloper?.developerId}" else viewModel.tokenManager.userId,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )

                    Text(
                        text = if (currentDeveloper != null) "${currentDeveloper?.developerName} (Verified Developer)" else "NovaStore Member",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        StatItem(label = "Uploaded Apps", value = "${myUploadedApps.size}")
                        StatItem(label = "Total Tokens", value = "🪙 $userTokens")
                        StatItem(label = "Referral Bonus", value = "🪙 $activeReferralTokens")
                    }
                }
            }

            // Referral Code Card (Share & Earn)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Share, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Your Unique Referral Code", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Share your referral code with friends. When they use it, they get 20 tokens and you earn 10 tokens (valid for 30 days)!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = referralCode,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                            color = MaterialTheme.colorScheme.primary
                        )

                        Button(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Nova Referral Code", referralCode)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Referral code copied to clipboard!", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Copy")
                        }
                    }
                }
            }

            // Redeem Referral Code Card (Optional)
            if (!hasRedeemed) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text("Have a Referral Code?", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Enter a friend's referral code to instantly claim 20 Free Nova Tokens! (Optional)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = referralInput,
                            onValueChange = { referralInput = it },
                            label = { Text("Enter Referral Code (e.g. NOVA-XXXXXX)") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )

                        if (redeemMessage != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = redeemMessage!!,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (redeemMessage!!.contains("success", ignoreCase = true) || redeemMessage!!.contains("🎉", ignoreCase = true)) Color(0xFF10B981) else MaterialTheme.colorScheme.error
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                if (referralInput.isBlank()) {
                                    redeemMessage = "Please enter a referral code."
                                    return@Button
                                }
                                isRedeeming = true
                                redeemMessage = null
                                viewModel.tokenManager.redeemReferralCode(referralInput) { success, msg ->
                                    isRedeeming = false
                                    redeemMessage = msg
                                    if (success) {
                                        referralInput = ""
                                    }
                                }
                            },
                            enabled = !isRedeeming,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (isRedeeming) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                            } else {
                                Text("Claim 20 Free Tokens")
                            }
                        }
                    }
                }
            }

            // Developer Account Management Button
            OutlinedButton(
                onClick = { onNavigate(Screen.Publish) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.CloudUpload, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (currentDeveloper != null) "Manage Developer Uploads" else "Developer Login / Register")
            }

            if (currentDeveloper != null) {
                Button(
                    onClick = {
                        viewModel.logoutDeveloper()
                        Toast.makeText(context, "Logged out successfully", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Logout, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Log Out Developer Session")
                }
            }
        }
    }
}

@Composable
fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
