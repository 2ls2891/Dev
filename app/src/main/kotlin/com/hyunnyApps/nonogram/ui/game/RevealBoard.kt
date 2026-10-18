package com.hyunnyApps.nonogram.ui.game

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import com.hyunnyApps.nonogram.core.Grid
import com.hyunnyApps.nonogram.core.format.PuzzleOut
import com.hyunnyApps.nonogram.ui.components.parseHexColor
import com.hyunnyApps.nonogram.ui.theme.LocalBoardColors

/** 0..1 구간 [from, to] 안에서의 진행률 */
internal fun segment(p: Float, from: Float, to: Float): Float = ((p - from) / (to - from)).coerceIn(0f, 1f)

private fun easeInOut(t: Float): Float = if (t < 0.5f) 2 * t * t else 1 - (-2 * t + 2).let { it * it } / 2

/**
 * 완성 연출. progress 0 → 1 동안
 *  - 0.00~0.25 : (아래 깔린 BoardView 가 흐려지며) 단서·격자·✕가 사라짐
 *  - 0.10~0.30 : 그림 뒤에 흰 카드가 나타남
 *  - 0.15~0.50 : 그림이 격자 위치에서 화면 가운데로 이동·확대
 *  - 0.40~1.00 : 왼쪽 위에서 오른쪽 아래로 물결치듯 칸마다 컬러 그림 색으로 바뀜
 * progress = 0 일 때 칠해진 칸은 BoardView 와 같은 위치·색이라 화면이 튀지 않는다.
 */
@Composable
fun RevealBoard(puzzle: PuzzleOut, leftSlots: Int, topSlots: Int, progress: Float, modifier: Modifier = Modifier) {
    val board = LocalBoardColors.current
    val solution = remember(puzzle.id) { Grid.fromRows(puzzle.rows) }
    val reveal = remember(puzzle.id) { Grid.fromRows(puzzle.reveal.rows) }
    val solutionColors = remember(puzzle.id, board.monoFill) {
        if (puzzle.kind == "color") puzzle.palette.map(::parseHexColor) else listOf(board.monoFill)
    }
    val revealColors = remember(puzzle.id) { puzzle.reveal.palette.map(::parseHexColor) }

    Canvas(modifier) {
        val w = solution.width
        val h = solution.height
        // 시작: BoardView 와 같은 배치 (같은 최소/최대 단서 칸 크기)
        val g = BoardGeometry(size.width, size.height, w, h, leftSlots, topSlots, BoardMetrics.MinSlot.toPx(), BoardMetrics.MaxSlot.toPx(), maxCell = BoardMetrics.MaxCell.toPx())
        val start = Rect(g.gridLeft, g.gridTop, g.gridLeft + g.gridWidth, g.gridTop + g.gridHeight)
        // 끝: 여백을 두고 가운데 정렬
        val pad = 28.dp.toPx()
        val endCell = minOf((size.width - pad * 2) / w, (size.height - pad * 2) / h, BoardMetrics.MaxCell.toPx())
        val endW = endCell * w
        val endH = endCell * h
        val end = Rect(Offset((size.width - endW) / 2, (size.height - endH) / 2), Size(endW, endH))

        val move = easeInOut(segment(progress, 0.15f, 0.5f))
        val rect = lerp(start, end, move)
        val cardAlpha = segment(progress, 0.1f, 0.3f)
        if (cardAlpha > 0f) {
            val cardPad = 18.dp.toPx() * move
            val radius = 22.dp.toPx() * move
            drawRoundRect(
                board.background.copy(alpha = cardAlpha),
                Offset(rect.left - cardPad, rect.top - cardPad),
                Size(rect.width + cardPad * 2, rect.height + cardPad * 2),
                CornerRadius(radius, radius),
            )
        }

        val cell = rect.width / w
        val px = Size(cell + 0.6f, cell + 0.6f)
        val span = (w + h - 2).coerceAtLeast(1)
        for (r in 0 until h) {
            for (c in 0 until w) {
                val sv = solution[r, c]
                val rv = reveal[r, c]
                if (sv == 0 && rv == 0) continue
                val delay = (r + c).toFloat() / span
                val t = easeInOut(segment(progress, 0.4f + 0.35f * delay, 0.65f + 0.35f * delay))
                val from = if (sv > 0) solutionColors.getOrElse(sv - 1) { board.monoFill } else null
                val to = if (rv > 0) revealColors.getOrElse(rv - 1) { Color.Gray } else null
                val color = lerp(from ?: to!!.copy(alpha = 0f), to ?: from!!.copy(alpha = 0f), t)
                if (color.alpha > 0.01f) drawRect(color, Offset(rect.left + c * cell, rect.top + r * cell), px)
            }
        }
    }
}
