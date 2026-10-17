package com.hyunnyApps.nonogram.ui.game

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BoardGeometryTest {
    private fun geometry(scale: Float = 1f, ox: Float = 0f, oy: Float = 0f) =
        BoardGeometry(1000f, 1200f, columns = 20, rows = 20, leftSlots = 5, topSlots = 6, minSlot = 40f, maxSlot = 90f,
            scale = scale, offsetX = ox, offsetY = oy, maxScale = 3f)

    @Test
    fun fitsInsideViewportWithMinimumClueSlot() {
        val g = geometry()
        assertTrue(g.slot >= 40f)
        assertTrue(g.boardWidth <= 1000f + 0.01f)
        assertTrue(g.boardHeight <= 1200f + 0.01f)
        assertEquals(g.gridLeft + g.gridWidth, g.originX + g.boardWidth, 0.01f)
    }

    @Test
    fun hitTestMapsCells() {
        val g = geometry()
        val x = g.cellLeft(3) + g.cell / 2
        val y = g.cellTop(7) + g.cell / 2
        assertEquals(7 * 20 + 3, g.hit(x, y))
        assertEquals(-1, g.hit(g.gridLeft - 1, y)) // 단서 영역
        assertEquals(7 * 20 + 0, g.hitClamped(g.gridLeft - 50, y))
    }

    @Test
    fun zoomKeepsPointUnderFingerAndClampsOffsets() {
        val g = geometry()
        val cx = g.gridLeft + g.gridWidth / 2
        val cy = g.gridTop + g.gridHeight / 2
        val before = g.hit(cx, cy)
        val (s, x, y) = g.transformed(cx, cy, 2f, 0f, 0f)
        val zoomed = geometry(s, x, y)
        assertEquals(2f, zoomed.scale, 0.001f)
        assertEquals(before, zoomed.hit(cx, cy))
        // 과도한 이동은 격자 범위 안으로 제한
        val panned = geometry(2f, 99999f, -99999f)
        assertEquals(0f, panned.offsetX)
        assertEquals(panned.gridHeight - 20 * panned.cell, panned.offsetY, 0.01f)
    }

    @Test
    fun scaleIsClamped() {
        assertEquals(3f, geometry(scale = 10f).scale)
        assertEquals(1f, geometry(scale = 0.2f).scale)
    }
}
