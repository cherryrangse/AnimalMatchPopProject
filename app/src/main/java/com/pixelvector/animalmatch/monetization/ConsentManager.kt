package com.pixelvector.animalmatch.monetization

import android.app.Activity
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * EU GDPR 대응 Google UMP (User Messaging Platform) 동의 관리자
 */
class ConsentManager(activity: Activity) {

    private val consentInformation: ConsentInformation =
        UserMessagingPlatform.getConsentInformation(activity)

    private val _canRequestAds = MutableStateFlow(consentInformation.canRequestAds())
    val canRequestAds: StateFlow<Boolean> = _canRequestAds.asStateFlow()

    fun gatherConsent(
        activity: Activity,
        onComplete: (canRequestAds: Boolean) -> Unit
    ) {
        val params = ConsentRequestParameters.Builder()
            .setTagForUnderAgeOfConsent(false) // COPPA: 만 13세 이상 타깃
            .build()

        consentInformation.requestConsentInfoUpdate(
            activity,
            params,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) {
                    val allowed = consentInformation.canRequestAds()
                    _canRequestAds.value = allowed
                    onComplete(allowed)
                }
            },
            {
                val allowed = consentInformation.canRequestAds()
                _canRequestAds.value = allowed
                onComplete(allowed)
            }
        )

        if (consentInformation.canRequestAds()) {
            _canRequestAds.value = true
        }
    }
}
