package com.hyunnyApps.nonogram

import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import android.util.DisplayMetrics
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
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
    /**
     * 알림으로 들어와 아직 오늘의 퍼즐로 이동하지 않았으면 true.
     * 저장 상태에 남겨 둔다 → 세로 고정 전환 등으로 이동 전에 화면이 다시 만들어져도 이동을 잃지 않고,
     * 이동한 뒤의 재생성(회전 등)에서는 다시 이동하지 않는다.
     */
    private var openDailyPending = false

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        applyOrientationPolicy()
        val container = (application as NonogramApplication).container
        openDailyPending = savedInstanceState?.getBoolean(KEY_OPEN_DAILY_PENDING)
            ?: intent.getBooleanExtra(DailyReminder.EXTRA_OPEN_DAILY, false)
        val openDaily = openDailyPending

        // 1) 이전 실행에서 동의를 받았다면 바로 광고 초기화 (첫 화면 지연 없음)
        container.ads.initializeIfAllowed()
        // 2) 매 실행마다 동의 정보를 갱신하고 필요 시 양식 표시 → 끝나면 다시 초기화 시도
        container.consent.gatherConsent(this) { container.ads.initializeIfAllowed() }

        setContent {
            val ink by container.player.inkIndex.collectAsStateWithLifecycle(initialValue = 0)
            NonogramTheme(inkIndex = ink) {
                CompositionLocalProvider(LocalContentColor provides LocalNoteColors.current.ink) {
                    PaperBackground(Modifier.fillMaxSize()) {
                        // 가로 화면의 카메라 홀·옆 내비게이션 바를 피한다 (배경 모눈은 화면 끝까지)
                        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))) {
                            Box(Modifier.weight(1f)) {
                                NonogramNavHost(container, Modifier.fillMaxSize(), openDaily, onDailyOpened = { openDailyPending = false })
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

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(KEY_OPEN_DAILY_PENDING, openDailyPending)
    }

    /**
     * 폰(폴드 커버 화면 포함)은 세로 고정, 태블릿·폴드 펼친 화면은 자유 회전.
     *
     * - 기준: 화면의 짧은 변 600dp. Android 16(API 36 타깃)부터 이 이상인 화면에서는
     *   시스템이 방향 고정을 어차피 무시하므로 같은 기준을 쓴다.
     * - 폴드를 접고 펼치면 화면 크기 변경으로 액티비티가 다시 만들어지며 여기서 다시 판단한다.
     * - 창 크기가 아니라 "디스플레이" 크기로 판단한다. 태블릿에서 작은 창(분할·자유 창)으로 열렸다고 폰으로 오인하지 않게.
     * - 분할 화면에서는 시스템이 요청을 무시한다. 그래서 모든 화면은 가로/세로 어떤 비율에서도 배치가 성립해야 한다.
     */
    private fun applyOrientationPolicy() {
        val compact = displaySmallestWidthDp() < LARGE_SCREEN_MIN_DP
        val wanted = if (compact) ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT else ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        if (requestedOrientation != wanted) requestedOrientation = wanted
    }

    /** 지금 앱이 떠 있는 디스플레이(폴드는 커버/메인 중 현재 것)의 짧은 변 (dp) */
    private fun displaySmallestWidthDp(): Float {
        val (w, h) = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            windowManager.maximumWindowMetrics.bounds.let { it.width() to it.height() }
        } else {
            val metrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.getRealMetrics(metrics)
            metrics.widthPixels to metrics.heightPixels
        }
        return minOf(w, h) / resources.displayMetrics.density
    }

    private companion object {
        const val LARGE_SCREEN_MIN_DP = 600
        const val KEY_OPEN_DAILY_PENDING = "open_daily_pending"
    }
}
