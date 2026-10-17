package com.hyunnyApps.nonogram.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.hyunnyApps.nonogram.BuildConfig

/**
 * Google UMP(사용자 메시지 플랫폼) 동의 관리.
 * EEA·영국 등 동의가 필요한 지역에서는 동의 양식을 띄우고, 광고 요청 가능 여부(canRequestAds)를 알려준다.
 * https://developers.google.com/admob/android/privacy
 */
class ConsentManager(context: Context) {
    private val consentInformation: ConsentInformation = UserMessagingPlatform.getConsentInformation(context)

    private val _privacyOptionsRequired = MutableStateFlow(isPrivacyOptionsRequired())
    /** 설정 화면에 "개인정보 설정" 버튼을 보여야 하는지 */
    val privacyOptionsRequired: StateFlow<Boolean> = _privacyOptionsRequired.asStateFlow()

    /** 이전 실행에서 이미 동의를 받았다면 즉시 true (앱 시작 지연 없이 광고 초기화 가능) */
    fun canRequestAds(): Boolean = consentInformation.canRequestAds()

    private fun isPrivacyOptionsRequired() =
        consentInformation.privacyOptionsRequirementStatus == ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED

    /**
     * 동의 정보를 갱신하고 필요하면 양식을 띄운다. 매 실행마다 호출해야 한다.
     * 실패해도 onFinished 는 반드시 호출된다(이전 동의 상태로 광고 가능 여부를 판단).
     */
    fun gatherConsent(activity: Activity, onFinished: () -> Unit) {
        val params = ConsentRequestParameters.Builder().apply {
            if (BuildConfig.DEBUG && BuildConfig.UMP_DEBUG_EEA) {
                // 디버그 전용: EEA 사용자처럼 동의 양식을 강제로 표시 (에뮬레이터는 자동으로 테스트 기기)
                setConsentDebugSettings(
                    ConsentDebugSettings.Builder(activity)
                        .setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)
                        .apply { BuildConfig.UMP_TEST_DEVICE_ID.takeIf { it.isNotBlank() }?.let(::addTestDeviceHashedId) }
                        .build()
                )
            }
        }.build()

        consentInformation.requestConsentInfoUpdate(
            activity,
            params,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { formError ->
                    if (formError != null) Log.w(TAG, "consent form: ${formError.errorCode} ${formError.message}")
                    _privacyOptionsRequired.value = isPrivacyOptionsRequired()
                    onFinished()
                }
            },
            { requestError ->
                Log.w(TAG, "consent update: ${requestError.errorCode} ${requestError.message}")
                onFinished()
            },
        )
    }

    /** 설정 화면의 "개인정보 설정" 버튼에서 호출 */
    fun showPrivacyOptionsForm(activity: Activity, onDismissed: () -> Unit = {}) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { formError ->
            if (formError != null) Log.w(TAG, "privacy options: ${formError.errorCode} ${formError.message}")
            _privacyOptionsRequired.value = isPrivacyOptionsRequired()
            onDismissed()
        }
    }

    private companion object {
        const val TAG = "Consent"
    }
}
