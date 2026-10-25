package com.example.ui.components

import android.util.Log
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.model.AdMobConfig
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError

// Universal Google AdMob sample test banner ad unit ID (always fills 100%)
private const val GOOGLE_TEST_BANNER_AD_UNIT = "ca-app-pub-3940256099942544/6300978111"

@Composable
fun AdMobBannerView(
    config: AdMobConfig,
    modifier: Modifier = Modifier
) {
    if (!config.enabled || config.bannerAdUnitId.isBlank()) {
        return
    }

    var currentAdUnitId by remember(config.bannerAdUnitId) {
        mutableStateOf(config.bannerAdUnitId.trim())
    }
    var isAdLoaded by remember { mutableStateOf(false) }
    var statusText by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("admob_banner_view")
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 2.dp)
        ) {
            Text(
                text = "SPONSORED AD",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )

            statusText?.let { status ->
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "• $status",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
            contentAlignment = Alignment.Center
        ) {
            key(currentAdUnitId) {
                var adViewInstance: AdView? = remember { null }

                DisposableEffect(currentAdUnitId) {
                    onDispose {
                        try {
                            adViewInstance?.destroy()
                        } catch (_: Exception) {}
                    }
                }

                AndroidView(
                    factory = { ctx ->
                        AdView(ctx).apply {
                            setAdSize(AdSize.BANNER)
                            adUnitId = currentAdUnitId
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.WRAP_CONTENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT
                            )

                            adListener = object : AdListener() {
                                override fun onAdLoaded() {
                                    isAdLoaded = true
                                    statusText = if (currentAdUnitId == GOOGLE_TEST_BANNER_AD_UNIT) {
                                        "AdMob Test Mode"
                                    } else {
                                        "Live"
                                    }
                                    Log.i("AdMobBannerView", "Ad loaded successfully for unit $currentAdUnitId")
                                }

                                override fun onAdFailedToLoad(error: LoadAdError) {
                                    isAdLoaded = false
                                    Log.w("AdMobBannerView", "Ad failed to load ($currentAdUnitId): code=${error.code}, message=${error.message}")

                                    // If live production ad unit has no fill (new account/approval delay),
                                    // fallback to Google test unit so the banner ad appears immediately.
                                    if (currentAdUnitId != GOOGLE_TEST_BANNER_AD_UNIT) {
                                        statusText = "Loading Google Test Ad..."
                                        currentAdUnitId = GOOGLE_TEST_BANNER_AD_UNIT
                                    } else {
                                        statusText = "Ad error (code ${error.code})"
                                    }
                                }
                            }

                            try {
                                loadAd(AdRequest.Builder().build())
                            } catch (e: Exception) {
                                Log.e("AdMobBannerView", "loadAd exception: ${e.message}")
                            }

                            adViewInstance = this
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                )
            }
        }
    }
}
