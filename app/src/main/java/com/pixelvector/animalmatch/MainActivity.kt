package com.pixelvector.animalmatch

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pixelvector.animalmatch.data.local.GamePreferencesRepository
import com.pixelvector.animalmatch.monetization.AdManager
import com.pixelvector.animalmatch.monetization.BillingManager
import com.pixelvector.animalmatch.monetization.ConsentManager
import com.pixelvector.animalmatch.ui.components.SoundEffectPlayer
import com.pixelvector.animalmatch.ui.screen.AnimalMatchScreen
import com.pixelvector.animalmatch.ui.theme.AnimalMatchPopTheme
import com.pixelvector.animalmatch.ui.viewmodel.GameViewModel

class MainActivity : ComponentActivity() {

    private lateinit var consentManager: ConsentManager
    private lateinit var billingManager: BillingManager
    private lateinit var gameViewModel: GameViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        // Android 12+(API 31+) 기본 단색 스플래시 오버레이를 즉시 해제하여 숲속 배경 스플래시 화면이 바로 노출되도록 처리
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            splashScreen.setOnExitAnimationListener { splashView ->
                splashView.remove()
            }
        }

        super.onCreate(savedInstanceState)
        enableImmersiveFullscreen()

        SoundEffectPlayer.init(applicationContext)

        val repository = GamePreferencesRepository(applicationContext)
        gameViewModel = ViewModelProvider(
            this,
            GameViewModel.provideFactory(repository)
        )[GameViewModel::class.java]

        consentManager = ConsentManager(this)
        consentManager.gatherConsent(this) { canRequest ->
            if (canRequest) {
                AdManager.initializeIfNeeded(applicationContext)
            }
        }

        billingManager = BillingManager(
            context = applicationContext,
            onAdsRemovedEntitled = {
                gameViewModel.markAdsRemoved()
            }
        )
        billingManager.startConnection()

        setContent {
            AnimalMatchPopTheme {
                val canRequestAds by consentManager.canRequestAds.collectAsStateWithLifecycle()
                AnimalMatchScreen(
                    viewModel = gameViewModel,
                    canRequestAds = canRequestAds,
                    onRemoveAdsClick = {
                        billingManager.launchRemoveAdsPurchaseFlow(this@MainActivity)
                    },
                    onRestorePurchasesClick = {
                        billingManager.restorePurchases()
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        enableImmersiveFullscreen()
        if (::gameViewModel.isInitialized && gameViewModel.uiState.value.isSoundEnabled) {
            SoundEffectPlayer.startBgm(true)
        }
    }

    override fun onPause() {
        super.onPause()
        SoundEffectPlayer.stopBgm()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            enableImmersiveFullscreen()
        }
    }

    private fun enableImmersiveFullscreen() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes = window.attributes.apply {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())
    }

    override fun onDestroy() {
        SoundEffectPlayer.release()
        if (::billingManager.isInitialized) {
            billingManager.endConnection()
        }
        super.onDestroy()
    }
}
