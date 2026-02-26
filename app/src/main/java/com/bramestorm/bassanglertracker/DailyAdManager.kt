package com.bramestorm.bassanglertracker

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback


object DailyAdManager {

    private var interstitial: InterstitialAd? = null
    private var isLoading = false
    private val onLoadedCallbacks = mutableListOf<() -> Unit>()

    fun preload(context: Context, onLoaded: (() -> Unit)? = null) {
        onLoaded?.let { onLoadedCallbacks += it }

        if (!BuildConfig.FEATURE_DAILY_AD) return
        if (interstitial != null) {
            flushLoadedCallbacks()
            return
        }
        if (isLoading) return

        MobileAds.initialize(context) {}
        isLoading = true

        val unitId = "ca-app-pub-3940256099942544/1033173712" // test interstitial
        val request = AdRequest.Builder().build()

        InterstitialAd.load(
            context,
            unitId,
            request,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitial = ad
                    isLoading = false
                    flushLoadedCallbacks()
                }

                override fun onAdFailedToLoad(error: com.google.android.gms.ads.LoadAdError) {
                    interstitial = null
                    isLoading = false
                    onLoadedCallbacks.clear()
                }
            }
        )
    }

    private fun flushLoadedCallbacks() {
        val callbacks = onLoadedCallbacks.toList()
        onLoadedCallbacks.clear()
        callbacks.forEach { it.invoke() }
    }

    fun showIfReady(activity: Activity): Boolean {
        val ad = interstitial ?: return false
        interstitial = null
        ad.show(activity)
        return true
    }
}