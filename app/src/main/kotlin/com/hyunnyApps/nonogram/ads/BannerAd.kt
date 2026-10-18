package com.hyunnyApps.nonogram.ads

import android.view.ViewGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView

/**
 * 화면 하단 고정 적응형 배너.
 *
 * 정책/UX 안전장치
 *  - 광고 로드 전에도 배너 높이만큼 자리를 미리 차지한다 → 광고가 늦게 떠도 보드가 밀리지 않음(오클릭 방지)
 *  - 앱 전체에서 한 번만 배치(MainActivity)되어 화면 전환마다 광고를 새로 요청하지 않는다
 *  - 화면 생명주기에 맞춰 pause/resume/destroy 를 호출한다
 *  - 동의/초기화(ready) 전에는 광고를 요청하지 않는다
 */
@Composable
fun AnchoredBannerAd(ads: AdsManager, modifier: Modifier = Modifier, unitId: String = AdConfig.BANNER_ID) {
    val ready by ads.ready.collectAsStateWithLifecycle()
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val context = LocalContext.current
        val density = LocalDensity.current
        val widthDp = maxWidth.value.toInt()
        // 크기 정책은 AdConfig.bannerSize (기본 320×50). 고정 크기 배너는 가운데 정렬
        val adSize = remember(widthDp) { AdConfig.bannerSize(context, widthDp) }
        val adWidth = with(density) { adSize.getWidthInPixels(context).toDp() }
        val height = with(density) { adSize.getHeightInPixels(context).toDp() }
        Box(Modifier.fillMaxWidth().height(height), contentAlignment = Alignment.Center) {
            if (ready) {
                val lifecycle = LocalLifecycleOwner.current.lifecycle
                val adView = remember(adSize, unitId) {
                    AdView(context).apply {
                        setAdSize(adSize)
                        adUnitId = unitId
                        loadAd(AdRequest.Builder().build())
                    }
                }
                AndroidView(
                    factory = { adView.also { (it.parent as? ViewGroup)?.removeView(it) } },
                    modifier = Modifier.width(adWidth).fillMaxHeight(),
                )
                DisposableEffect(adView, lifecycle) {
                    val observer = LifecycleEventObserver { _, event ->
                        when (event) {
                            Lifecycle.Event.ON_RESUME -> adView.resume()
                            Lifecycle.Event.ON_PAUSE -> adView.pause()
                            else -> Unit
                        }
                    }
                    lifecycle.addObserver(observer)
                    onDispose {
                        lifecycle.removeObserver(observer)
                        adView.destroy()
                    }
                }
            }
        }
    }
}
