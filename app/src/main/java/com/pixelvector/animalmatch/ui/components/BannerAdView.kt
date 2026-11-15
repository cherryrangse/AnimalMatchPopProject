package com.pixelvector.animalmatch.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.pixelvector.animalmatch.monetization.AdManager

@Composable
fun BannerAdView(
    isAdsRemoved: Boolean,
    canRequestAds: Boolean = true,
    modifier: Modifier = Modifier
) {
    if (isAdsRemoved) return

    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val adWidthDp = configuration.screenWidthDp.coerceAtLeast(320)

    val adView = remember(context, adWidthDp) {
        AdManager.createAdaptiveBannerAdView(context, adWidthDp)
    }

    DisposableEffect(adView, canRequestAds) {
        if (canRequestAds) {
            AdManager.loadBannerAd(adView)
        }
        onDispose {
            AdManager.destroyBannerAd(adView)
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 50.dp)
            .background(Color(0xFFECEFF1)),
        contentAlignment = Alignment.Center
    ) {
        if (canRequestAds) {
            AndroidView(
                factory = { adView },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
