package com.hyunnyApps.nonogram.ui.game

import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** 보드 영역 크기(상단바·배너를 뺀 값)로 조작부 위치를 고르는지 */
class GameLayoutTest {
    @Test
    fun portraitScreensKeepControlsBelow() {
        assertFalse(GameLayout.useSidePanel(344.dp, 650.dp), "폴드 커버")
        assertFalse(GameLayout.useSidePanel(393.dp, 620.dp), "일반 폰")
        assertFalse(GameLayout.useSidePanel(690.dp, 600.dp), "폴드 펼침(세로에 가까움)")
        assertFalse(GameLayout.useSidePanel(800.dp, 1000.dp), "태블릿 세로")
    }

    @Test
    fun wideScreensPutControlsBeside() {
        assertTrue(GameLayout.useSidePanel(1280.dp, 570.dp), "태블릿 가로")
        assertTrue(GameLayout.useSidePanel(852.dp, 230.dp), "폰 가로 창")
        assertTrue(GameLayout.useSidePanel(829.dp, 470.dp), "폴드 펼침 가로")
    }

    @Test
    fun sidePanelWidthIsBounded() {
        assertTrue(GameLayout.sidePanelWidth(600.dp) >= 280.dp)
        assertTrue(GameLayout.sidePanelWidth(2000.dp) <= 400.dp)
    }
}
