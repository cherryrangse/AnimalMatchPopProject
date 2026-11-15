package com.pixelvector.animalmatch.monetization

import android.content.Context
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Google AdMob 최상단 적응형 배너(Adaptive Banner) 매니저
 */
object AdManager {
    const val TEST_ADAPTIVE_BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/6300978111"

    private val isInitialized = AtomicBoolean(false)

    fun initializeIfNeeded(context: Context, onInitialized: () -> Unit = {}) {
        if (isInitialized.compareAndSet(false, true)) {
            MobileAds.initialize(context.applicationContext) {
                onInitialized()
            }
        } else {
            onInitialized()
        }
    }

    fun createAdaptiveBannerAdView(
        context: Context,
        adWidthDp: Int
    ): AdView {
        val adView = AdView(context)
        adView.adUnitId = TEST_ADAPTIVE_BANNER_AD_UNIT_ID
        val adaptiveSize = AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(
            context,
            adWidthDp.coerceAtLeast(320)
        )
        adView.setAdSize(adaptiveSize)
        return adView
    }

    fun loadBannerAd(adView: AdView) {
        val adRequest = AdRequest.Builder().build()
        adView.loadAd(adRequest)
    }

    fun destroyBannerAd(adView: AdView?) {
        runCatching {
            adView?.destroy()
        }
    }
}
