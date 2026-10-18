package com.hyunnyApps.nonogram.ui.game

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 게임 화면 배치 결정 (순수 계산, 단위 테스트 가능).
 *
 * 기기 종류를 보지 않고, 보드에 쓸 수 있는 영역의 크기만으로 고른다.
 *  - 아래 배치: 보드 아래에 조작부 (폰, 폴드 커버·펼침, 세로 태블릿)
 *  - 옆 배치  : 보드 오른쪽에 조작부 (가로 태블릿, 가로로 긴 분할 창)
 * 두 배치에서 보드가 차지할 수 있는 정사각형 크기를 비교해 더 큰 쪽을 쓴다.
 * 비슷하면 익숙한 아래 배치를 유지한다(창 크기가 조금 바뀔 때마다 배치가 뒤집히지 않게).
 */
object GameLayout {
    /** 아래 배치일 때 조작부 + 진행 줄이 차지하는 대략적인 높이 */
    val StackedControlsHeight = 190.dp

    /** 조작부가 넓은 화면에서 끝없이 늘어나지 않게 */
    val ControlsMaxWidth = 560.dp

    private val SidePanelMin = 280.dp
    private val SidePanelMax = 400.dp

    fun sidePanelWidth(width: Dp): Dp = (width * 0.36f).coerceIn(SidePanelMin, SidePanelMax)

    fun useSidePanel(width: Dp, height: Dp): Boolean {
        val stacked = minOf(width, height - StackedControlsHeight)
        val side = minOf(width - sidePanelWidth(width), height)
        return side > stacked * 1.15f
    }
}
