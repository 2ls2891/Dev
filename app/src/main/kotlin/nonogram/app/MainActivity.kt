package nonogram.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import nonogram.app.ads.AnchoredBannerAd
import nonogram.app.ui.NonogramNavHost
import nonogram.app.ui.theme.NonogramTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val container = (application as NonogramApplication).container

        // 1) 이전 실행에서 동의를 받았다면 바로 광고 초기화 (첫 화면 지연 없음)
        container.ads.initializeIfAllowed()
        // 2) 매 실행마다 동의 정보를 갱신하고 필요 시 양식 표시 → 끝나면 다시 초기화 시도
        container.consent.gatherConsent(this) { container.ads.initializeIfAllowed() }

        setContent {
            NonogramTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    Column(Modifier.fillMaxSize()) {
                        Box(Modifier.weight(1f)) {
                            NonogramNavHost(container, Modifier.fillMaxSize())
                        }
                        // 앱 전체에서 하나만 두는 하단 고정 배너 (화면 전환 시 재요청하지 않음)
                        HorizontalDivider()
                        AnchoredBannerAd(container.ads, Modifier.navigationBarsPadding())
                    }
                }
            }
        }
    }
}
