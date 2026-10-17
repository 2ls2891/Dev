package com.hyunnyApps.nonogram

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hyunnyApps.nonogram.ads.AnchoredBannerAd
import com.hyunnyApps.nonogram.reminder.DailyReminder
import com.hyunnyApps.nonogram.ui.NonogramNavHost
import com.hyunnyApps.nonogram.ui.components.PaperBackground
import com.hyunnyApps.nonogram.ui.theme.LocalNoteColors
import com.hyunnyApps.nonogram.ui.theme.NonogramTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val container = (application as NonogramApplication).container
        // 알림을 눌러 들어온 경우 (화면 회전 등 재생성 시에는 다시 이동하지 않음)
        val openDaily = savedInstanceState == null && intent.getBooleanExtra(DailyReminder.EXTRA_OPEN_DAILY, false)

        // 1) 이전 실행에서 동의를 받았다면 바로 광고 초기화 (첫 화면 지연 없음)
        container.ads.initializeIfAllowed()
        // 2) 매 실행마다 동의 정보를 갱신하고 필요 시 양식 표시 → 끝나면 다시 초기화 시도
        container.consent.gatherConsent(this) { container.ads.initializeIfAllowed() }

        setContent {
            val ink by container.player.inkIndex.collectAsStateWithLifecycle(initialValue = 0)
            NonogramTheme(inkIndex = ink) {
                CompositionLocalProvider(LocalContentColor provides LocalNoteColors.current.ink) {
                    PaperBackground(Modifier.fillMaxSize()) {
                        Column(Modifier.fillMaxSize()) {
                            Box(Modifier.weight(1f)) {
                                NonogramNavHost(container, Modifier.fillMaxSize(), openDaily)
                            }
                            // 앱 전체에서 하나만 두는 하단 고정 배너 (화면 전환 시 재요청하지 않음)
                            HorizontalDivider(color = LocalNoteColors.current.ink.copy(alpha = 0.15f))
                            AnchoredBannerAd(container.ads, Modifier.navigationBarsPadding())
                        }
                    }
                }
            }
        }
    }
}
