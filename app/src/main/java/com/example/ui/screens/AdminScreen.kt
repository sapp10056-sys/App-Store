package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.ui.graphics.Color
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
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.LaunchedEffect
import com.example.data.TokenEconomySummary
import com.example.data.DeveloperEarning
import com.example.data.TokenTransaction
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
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
import com.example.ui.components.AppLogoImage
import com.example.ui.theme.BadgeBlue
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

    var customConfigUrl by remember { mutableStateOf(viewModel.preferencesManager.getUpdateConfigUrl()) }
    var cloudDownloadUrl by remember { mutableStateOf("https://github.com/sapp10056-sys/App-Store/releases/latest") }
    var cloudReleaseNotes by remember { mutableStateOf("NovaStore v2.0.0 is required. Please download and install the new APK version to continue using NovaStore.") }
    var cloudMinVersion by remember { mutableStateOf("2") }
    var cloudLatestVersionName by remember { mutableStateOf("2.0.0") }
    var cloudLatestVersionCode by remember { mutableStateOf("2") }
    var isPublishingCloud by remember { mutableStateOf(false) }

    var targetUserTokenId by remember { mutableStateOf("") }
    var tokenAmountInput by remember { mutableStateOf("50") }
    var inspectedUserBalance by remember { mutableStateOf<String?>(null) }
    var isCreditingTokens by remember { mutableStateOf(false) }

    val adminTokens by viewModel.adminTokens.collectAsState()
    val totalTokensCirculation by viewModel.totalTokensInCirculation.collectAsState()
    var economySummary by remember { mutableStateOf<TokenEconomySummary?>(null) }
    var isLoadingEconomy by remember { mutableStateOf(false) }

    fun refreshEconomyData() {
        isLoadingEconomy = true
        scope.launch {
            val summary = viewModel.tokenManager.fetchTokenEconomyData(allApps)
            economySummary = summary
            isLoadingEconomy = false
            val total = summary.totalTokensInCirculation.toLong()
            if (total > 0L) {
                viewModel.syncTokenStatsToFirestore(total)
            }
        }
    }

    LaunchedEffect(selectedTab) {
        if (selectedTab == 2) {
            refreshEconomyData()
        }
    }

    val displayTokensCirculation = when {
        totalTokensCirculation > 0L -> totalTokensCirculation
        (economySummary?.totalTokensInCirculation ?: 0) > 0 -> economySummary!!.totalTokensInCirculation.toLong()
        else -> adminTokens.toLong()
    }

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
                        onClick = { viewModel.exportApplicationsToCsv(context) },
                        modifier = Modifier.testTag("admin_export_csv_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileDownload,
                            contentDescription = "Export to CSV",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
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
            // Dashboard Stats Grid (Displays 'Total Tokens in Circulation' fetched from Firestore 'tokenStats' document)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AdminStatCard(
                        title = "Total Apps",
                        value = "${allApps.size}",
                        modifier = Modifier.weight(1f)
                    )
                    AdminStatCard(
                        title = "Tokens in Circulation",
                        value = "🪙 $displayTokensCirculation",
                        modifier = Modifier
                            .weight(1f)
                            .testTag("admin_stat_tokens_circulation")
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
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
            }

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
                    text = { Text("Tokens (🪙)") }
                )
                Tab(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    text = { Text("Update") }
                )
            }

            if (selectedTab == 2) {
                // Nova Tokens Management Tab
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // 1. Token Economy Monitor & Circulation Card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_token_economy_card"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MonetizationOn,
                                        contentDescription = null,
                                        tint = WarningAmber,
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = "Token Economy Monitor",
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                        Text(
                                            text = "Total circulation & live platform liquidity",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = { refreshEconomyData() },
                                    modifier = Modifier.testTag("admin_refresh_economy_btn")
                                ) {
                                    if (isLoadingEconomy) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(20.dp),
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.Refresh,
                                            contentDescription = "Refresh Economy",
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // 3 Key Metrics Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text(
                                            text = "Circulation",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "🪙 $displayTokensCirculation",
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = "Firestore: stats/tokenStats",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                Surface(
                                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text(
                                            text = "Admin Wallet",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "🪙 ${economySummary?.adminWalletBalance ?: adminTokens}",
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                            color = WarningAmber
                                        )
                                        Text(
                                            text = "3 tokens/download",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                        )
                                    }
                                }

                                Surface(
                                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text(
                                            text = "Active Wallets",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "${economySummary?.activeWalletsCount ?: 1}",
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "User Accounts",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 2. Top-Earning Developers Card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_top_developers_card"),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.TrendingUp,
                                    contentDescription = null,
                                    tint = SuccessGreen,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "Top-Earning Developers",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        text = "Ranked by download reward split (7 tokens per premium download)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            val topDevs = economySummary?.topDevelopers ?: emptyList()
                            if (isLoadingEconomy && topDevs.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                                }
                            } else if (topDevs.isEmpty()) {
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "No developer earnings recorded yet. When users download a premium app (10 tokens), the developer automatically earns 7 tokens here.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(12.dp)
                                    )
                                }
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    topDevs.take(5).forEachIndexed { index, dev ->
                                        val rankColor = when (index) {
                                            0 -> Color(0xFFF59E0B) // Gold
                                            1 -> Color(0xFF94A3B8) // Silver
                                            2 -> Color(0xFFD97706) // Bronze
                                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                                        }

                                        Surface(
                                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(10.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                // Rank Badge
                                                Surface(
                                                    color = rankColor.copy(alpha = 0.2f),
                                                    shape = RoundedCornerShape(6.dp),
                                                    modifier = Modifier.size(28.dp)
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Text(
                                                            text = "#${index + 1}",
                                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                                                            color = rankColor
                                                        )
                                                    }
                                                }

                                                Spacer(modifier = Modifier.width(10.dp))

                                                // Dev info
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = dev.developerName.ifBlank { "Developer" },
                                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Text(
                                                        text = "${dev.developerId} • ${dev.premiumAppsCount} premium app(s)",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }

                                                Spacer(modifier = Modifier.width(8.dp))

                                                // Earnings
                                                Text(
                                                    text = "🪙 ${dev.totalTokensEarned}",
                                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                                                    color = SuccessGreen
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 3. Recent Token Transactions Card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_recent_transactions_card"),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.History,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "Recent Token Transactions",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        text = "Live audit log of credits, premium uploads & downloads",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            val txList = economySummary?.recentTransactions ?: emptyList()
                            if (isLoadingEconomy && txList.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                                }
                            } else if (txList.isEmpty()) {
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "No transactions logged yet. When users purchase tokens, upload premium APKs, or download premium apps, all transactions are recorded in real-time.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(12.dp)
                                    )
                                }
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    txList.take(8).forEach { tx ->
                                        val (badgeText, badgeBg, badgeTextColor) = when (tx.type) {
                                            "ADMIN_CREDIT" -> Triple("+ CREDIT", SuccessGreen.copy(alpha = 0.15f), SuccessGreen)
                                            "PREMIUM_UPLOAD" -> Triple("UPLOAD FEE", WarningAmber.copy(alpha = 0.15f), WarningAmber)
                                            "PREMIUM_DOWNLOAD" -> Triple("DOWNLOAD", BadgeBlue.copy(alpha = 0.15f), BadgeBlue)
                                            else -> Triple(tx.type.ifBlank { "TX" }, MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant)
                                        }

                                        val formattedDate = try {
                                            val sdf = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())
                                            sdf.format(Date(tx.timestamp))
                                        } catch (e: Exception) {
                                            ""
                                        }

                                        Surface(
                                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(10.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                // Type Tag
                                                Surface(
                                                    color = badgeBg,
                                                    shape = RoundedCornerShape(4.dp)
                                                ) {
                                                    Text(
                                                        text = badgeText,
                                                        style = MaterialTheme.typography.labelSmall.copy(
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 10.sp
                                                        ),
                                                        color = badgeTextColor,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }

                                                Spacer(modifier = Modifier.width(8.dp))

                                                // Description
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = when {
                                                            tx.appName?.isNotBlank() == true -> tx.appName
                                                            tx.note.isNotBlank() -> tx.note
                                                            else -> "User: ${tx.userId}"
                                                        },
                                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Text(
                                                        text = listOfNotNull(
                                                            tx.userId.takeIf { it.isNotBlank() },
                                                            formattedDate.takeIf { it.isNotBlank() }
                                                        ).joinToString(" • "),
                                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }

                                                Spacer(modifier = Modifier.width(6.dp))

                                                // Amount
                                                Text(
                                                    text = when (tx.type) {
                                                        "ADMIN_CREDIT" -> "+${tx.amount}"
                                                        "PREMIUM_UPLOAD" -> "-${tx.amount}"
                                                        "PREMIUM_DOWNLOAD" -> "${tx.amount}"
                                                        else -> "${tx.amount}"
                                                    } + " 🪙",
                                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                                                    color = when (tx.type) {
                                                        "ADMIN_CREDIT" -> SuccessGreen
                                                        "PREMIUM_UPLOAD" -> WarningAmber
                                                        else -> MaterialTheme.colorScheme.primary
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Credit Tokens to User Account Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Credit Tokens to User",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "When a user pays on Telegram (@nova_token_bot), enter their User ID below to add tokens instantly to their cloud wallet.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            OutlinedTextField(
                                value = targetUserTokenId,
                                onValueChange = { targetUserTokenId = it },
                                label = { Text("User ID (e.g. user_8e3c7a1b)") },
                                placeholder = { Text("user_xxxx") },
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("admin_target_userid_input")
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = "Quick Select Package:",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { tokenAmountInput = "50" },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("+50 (₹10)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                OutlinedButton(
                                    onClick = { tokenAmountInput = "400" },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("+400 (₹50)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                OutlinedButton(
                                    onClick = { tokenAmountInput = "800" },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("+800 (₹100)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedTextField(
                                value = tokenAmountInput,
                                onValueChange = { tokenAmountInput = it },
                                label = { Text("Token Amount to Add") },
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("admin_token_amount_input")
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        val target = targetUserTokenId.trim()
                                        val amt = tokenAmountInput.trim().toIntOrNull() ?: 0
                                        if (target.isBlank()) {
                                            Toast.makeText(context, "Please enter a valid User ID!", Toast.LENGTH_SHORT).show()
                                            return@Button
                                        }
                                        if (amt <= 0) {
                                            Toast.makeText(context, "Please enter an amount > 0!", Toast.LENGTH_SHORT).show()
                                            return@Button
                                        }

                                        isCreditingTokens = true
                                        viewModel.tokenManager.adminCreditTokens(target, amt) { success, newTotal ->
                                            isCreditingTokens = false
                                            if (success) {
                                                Toast.makeText(context, "Success! Credited $amt tokens. User now has $newTotal tokens.", Toast.LENGTH_LONG).show()
                                                inspectedUserBalance = "User $target has $newTotal tokens"
                                            } else {
                                                Toast.makeText(context, "Failed to credit tokens. Check internet connection.", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    },
                                    enabled = !isCreditingTokens,
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                                    modifier = Modifier
                                        .weight(1.3f)
                                        .testTag("admin_credit_tokens_btn")
                                ) {
                                    Icon(Icons.Default.MonetizationOn, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(if (isCreditingTokens) "Crediting..." else "Credit Tokens")
                                }

                                OutlinedButton(
                                    onClick = {
                                        val target = targetUserTokenId.trim()
                                        if (target.isBlank()) {
                                            Toast.makeText(context, "Enter a User ID first!", Toast.LENGTH_SHORT).show()
                                            return@OutlinedButton
                                        }
                                        viewModel.tokenManager.adminCreditTokens(target, 0) { _, total ->
                                            inspectedUserBalance = "User $target has $total tokens"
                                            Toast.makeText(context, "Balance: $total tokens", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(0.9f)
                                ) {
                                    Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Check")
                                }
                            }

                            if (inspectedUserBalance != null) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = inspectedUserBalance!!,
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(10.dp)
                                    )
                                }
                            }
                        }
                    }

                    // CSV Administrative Export Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.FileDownload, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Administrative CSV Export",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Export complete store catalog data (all ${allApps.size} apps with IDs, developers, versions, downloads, and premium token status) into a formatted CSV report file.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { viewModel.exportApplicationsToCsv(context) },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("admin_export_csv_action_btn")
                            ) {
                                Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Download / Export Applications CSV Report", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else if (selectedTab == 3) {
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

                    // Live Cloud Force Update & Download Link Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Sync, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Live Cloud Update & Download Link", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "Publish download links and control Force Update live for all users instantly without needing to edit GitHub.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = cloudDownloadUrl,
                                onValueChange = { cloudDownloadUrl = it },
                                label = { Text("New APK Download Link (GitHub, Drive, Mediafire)") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp)
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedTextField(
                                value = cloudReleaseNotes,
                                onValueChange = { cloudReleaseNotes = it },
                                label = { Text("Release Notes / User Notice") },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 2,
                                shape = RoundedCornerShape(10.dp)
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = cloudLatestVersionName,
                                    onValueChange = { cloudLatestVersionName = it },
                                    label = { Text("Version Name") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    shape = RoundedCornerShape(10.dp)
                                )

                                OutlinedTextField(
                                    value = cloudMinVersion,
                                    onValueChange = { cloudMinVersion = it },
                                    label = { Text("Min Build (Force)") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    shape = RoundedCornerShape(10.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Button(
                                onClick = {
                                    isPublishingCloud = true
                                    viewModel.publishCloudUpdateConfig(
                                        downloadUrl = cloudDownloadUrl,
                                        latestVersionCode = cloudLatestVersionCode.toIntOrNull() ?: 2,
                                        latestVersionName = cloudLatestVersionName.ifBlank { "2.0.0" },
                                        minimumSupportedVersionCode = cloudMinVersion.toIntOrNull() ?: 2,
                                        releaseNotes = cloudReleaseNotes,
                                        onComplete = { success, errorMsg ->
                                            isPublishingCloud = false
                                            if (success) {
                                                Toast.makeText(context, "Cloud update published! Older APKs locked.", Toast.LENGTH_LONG).show()
                                            } else {
                                                Toast.makeText(context, "Error: $errorMsg", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    )
                                },
                                enabled = !isPublishingCloud,
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                if (isPublishingCloud) {
                                    androidx.compose.material3.CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Publishing...")
                                } else {
                                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Publish Update to Cloud Now")
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
                AppLogoImage(
                    logoUrl = app.logoUrl,
                    appName = app.appName,
                    iconSize = 46.dp,
                    shape = RoundedCornerShape(10.dp)
                )

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
