package com.hyunnyApps.nonogram.ui.components

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 화면 크기 대응 기준.
 *
 * 폰 · 폴드 커버(약 340~410dp 폭) · 폴드 펼침(약 690~750dp) · 태블릿(800dp~) · 분할 화면까지
 * 같은 코드로 대응한다. 기기 종류가 아니라 "지금 창의 크기"만 보고 배치를 고른다.
 */
object Adaptive {
    /** 글 위주 화면(설정·규칙 등)의 최대 내용 폭. 넓은 화면에서 한 줄이 너무 길어지지 않게 */
    val ContentMaxWidth = 640.dp

    /** 이 폭 이상이면 홈을 두 단으로 나눈다 (태블릿 가로, 큰 태블릿 세로) */
    val TwoPaneMinWidth = 840.dp

    /** 화면 좌우 기본 여백 */
    val SideMargin = 20.dp

    /** 퍼즐 칸·컬렉션 격자의 최소 타일 크기. 큰 화면에서는 타일을 키워 너무 잘게 쪼개지지 않게 */
    fun tileMinSize(width: Dp): Dp = if (width >= 600.dp) 128.dp else 100.dp

    /** 내용 폭을 maxContent 로 제한하고 가운데 정렬할 때의 좌우 여백 */
    fun sidePadding(available: Dp, maxContent: Dp = ContentMaxWidth, min: Dp = SideMargin): Dp =
        maxOf(min, (available - maxContent) / 2)
}

/**
 * 각 화면 위쪽에 둘 안전 영역(상태바 + 위쪽 카메라 홀).
 * 좌우 안전 영역(가로 화면의 카메라 홀·내비게이션 바)은 MainActivity 에서 앱 전체에 한 번 적용한다.
 */
val TopSafeInsets: WindowInsets
    @Composable get() = WindowInsets.safeDrawing.only(WindowInsetsSides.Top)
