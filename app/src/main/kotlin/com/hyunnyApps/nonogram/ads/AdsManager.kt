package com.hyunnyApps.nonogram.ads

import android.content.Context
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Mobile Ads SDK 초기화. 동의(canRequestAds)가 확인된 뒤 앱 수명 동안 한 번만 초기화한다.
 * ready 가 true 가 되기 전에는 어떤 광고도 요청하지 않는다.
 */
class AdsManager(
    private val context: Context,
    private val consent: ConsentManager,
    private val scope: CoroutineScope,
) {
    private val started = AtomicBoolean(false)
    private val _ready = MutableStateFlow(false)
    val ready: StateFlow<Boolean> = _ready.asStateFlow()

    fun initializeIfAllowed() {
        if (!consent.canRequestAds()) return
        if (!started.compareAndSet(false, true)) return
        MobileAds.setRequestConfiguration(
            RequestConfiguration.Builder()
                .setMaxAdContentRating(AdConfig.MAX_CONTENT_RATING)
                .build()
        )
        // Google 권장: 초기화는 메인 스레드 밖에서
        scope.launch(Dispatchers.IO) {
            MobileAds.initialize(context) { _ready.value = true }
        }
    }
}
