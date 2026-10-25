package com.example.ui.components

import android.app.Activity
import android.content.Context
import android.util.Log
import com.example.model.AdMobConfig
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

class RewardedAdManager(private val context: Context) {
    private var rewardedAd: RewardedAd? = null
    private var isLoading = false
    private var triedFallback = false

    // Official Google AdMob sample rewarded ad unit ID (always fills 100% in test/emulator)
    private val GOOGLE_TEST_REWARDED_AD_UNIT = "ca-app-pub-3940256099942544/5224354917"

    fun loadAd(config: AdMobConfig, onLoaded: (() -> Unit)? = null) {
        if (!config.enabled || config.rewardedAdUnitId.isBlank()) return
        if (rewardedAd != null || isLoading) return

        isLoading = true
        val targetAdUnitId = if (triedFallback) GOOGLE_TEST_REWARDED_AD_UNIT else config.rewardedAdUnitId.trim()
        val adRequest = AdRequest.Builder().build()

        RewardedAd.load(
            context,
            targetAdUnitId,
            adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    isLoading = false
                    rewardedAd = ad
                    Log.i("RewardedAdManager", "Rewarded ad loaded successfully (unit: $targetAdUnitId)")
                    onLoaded?.invoke()
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    isLoading = false
                    rewardedAd = null
                    Log.w("RewardedAdManager", "Rewarded ad failed to load ($targetAdUnitId): code=${loadAdError.code}, msg=${loadAdError.message}")

                    // If live ad has no fill (new AdMob ID review period), switch to Google test rewarded ad
                    if (!triedFallback && targetAdUnitId != GOOGLE_TEST_REWARDED_AD_UNIT) {
                        triedFallback = true
                        loadAd(config, onLoaded)
                    }
                }
            }
        )
    }

    fun isAdReady(): Boolean = rewardedAd != null

    fun showRewardedAd(
        activity: Activity,
        config: AdMobConfig,
        onRewardEarned: () -> Unit = {},
        onAdClosed: () -> Unit
    ) {
        val currentAd = rewardedAd
        if (currentAd == null) {
            // If ad is not ready, do not block the user; proceed directly
            Log.i("RewardedAdManager", "Ad not ready, proceeding directly")
            onAdClosed()
            loadAd(config)
            return
        }

        var rewardGiven = false

        currentAd.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                Log.i("RewardedAdManager", "Rewarded ad dismissed")
                rewardedAd = null
                loadAd(config) // Preload next rewarded ad
                onAdClosed()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                Log.w("RewardedAdManager", "Failed to show rewarded ad: ${adError.message}")
                rewardedAd = null
                loadAd(config)
                onAdClosed()
            }

            override fun onAdShowedFullScreenContent() {
                Log.i("RewardedAdManager", "Rewarded ad showed full screen")
            }
        }

        currentAd.show(activity) { rewardItem ->
            rewardGiven = true
            Log.i("RewardedAdManager", "User earned reward: ${rewardItem.amount} ${rewardItem.type}")
            onRewardEarned()
        }
    }
}
