package com.hyunnyApps.nonogram.ui.game

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.hyunnyApps.nonogram.ui.theme.BoardColors
import com.hyunnyApps.nonogram.ui.theme.LocalBoardColors
import com.hyunnyApps.nonogram.core.Clue
import com.hyunnyApps.nonogram.core.game.Hint
import com.hyunnyApps.nonogram.core.game.Mark
import kotlin.math.ceil
import kotlin.math.floor

/**
 * 노노그램 보드.
 * - 한 손가락: 칠하기 드래그 (시작은 격자 안에서만, 진행 중에는 가장자리로 고정)
 * - 두 손가락: 확대/이동. 드래그 중에 두 번째 손가락이 닿으면 그 드래그는 취소된다.
 */
@Composable
fun BoardView(
    puzzleKey: String,
    columns: Int,
    rows: Int,
    rowClues: List<Clue>,
    colClues: List<Clue>,
    board: BoardUi,
    palette: List<Color>,
    isColor: Boolean,
    highlight: Hint?,
    enabled: Boolean,
    onStrokeStart: (Int) -> Unit,
    onStrokeMove: (Int) -> Unit,
    onStrokeEnd: () -> Unit,
    onStrokeCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalBoardColors.current
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()
    val texts = remember(textMeasurer) { TextCache(textMeasurer) }
    var scale by rememberSaveable(puzzleKey) { mutableFloatStateOf(1f) }
    var offsetX by rememberSaveable(puzzleKey) { mutableFloatStateOf(0f) }
    var offsetY by rememberSaveable(puzzleKey) { mutableFloatStateOf(0f) }
    val leftSlots = remember(rowClues) { rowClues.maxOf { it.size }.coerceAtLeast(1) }
    val topSlots = remember(colClues) { colClues.maxOf { it.size }.coerceAtLeast(1) }

    BoxWithConstraints(modifier) {
        val minSlot = with(density) { 15.dp.toPx() }
        val maxSlot = with(density) { 34.dp.toPx() }
        val minTouch = with(density) { 40.dp.toPx() }
        val widthPx = constraints.maxWidth.toFloat()
        val heightPx = constraints.maxHeight.toFloat()
        val base = remember(widthPx, heightPx, columns, rows, leftSlots, topSlots) {
            BoardGeometry(widthPx, heightPx, columns, rows, leftSlots, topSlots, minSlot, maxSlot)
        }
        // 손가락으로 정확히 누를 수 있는 크기까지 확대 가능
        val maxScale = (minTouch * 1.5f / base.baseCell).coerceAtLeast(1f)
        val geometry = BoardGeometry(
            widthPx, heightPx, columns, rows, leftSlots, topSlots, minSlot, maxSlot,
            scale, offsetX, offsetY, maxScale,
        )
        val currentGeometry by rememberUpdatedState(geometry)
        val currentEnabled by rememberUpdatedState(enabled)
        val start by rememberUpdatedState(onStrokeStart)
        val move by rememberUpdatedState(onStrokeMove)
        val end by rememberUpdatedState(onStrokeEnd)
        val cancel by rememberUpdatedState(onStrokeCancel)

        Canvas(
            Modifier
                .matchParentSize()
                .pointerInput(puzzleKey) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        var painting = false
                        if (currentEnabled) {
                            val index = currentGeometry.hit(down.position.x, down.position.y)
                            if (index >= 0) {
                                start(index)
                                painting = true
                                down.consume()
                            }
                        }
                        while (true) {
                            val event = awaitPointerEvent()
                            val pressed = event.changes.filter { it.pressed }
                            if (pressed.isEmpty()) break
                            if (pressed.size >= 2) {
                                if (painting) {
                                    cancel()
                                    painting = false
                                }
                                // 리컴포지션 전에 이벤트가 연달아 와도 누락되지 않도록 최신 변환값으로 계산
                                val g = currentGeometry.withTransform(scale, offsetX, offsetY)
                                val centroid = event.calculateCentroid()
                                val pan = event.calculatePan()
                                val (s, x, y) = g.transformed(centroid.x, centroid.y, event.calculateZoom(), pan.x, pan.y)
                                scale = s
                                offsetX = x
                                offsetY = y
                                event.changes.forEach { it.consume() }
                            } else if (painting) {
                                val p = pressed[0].position
                                move(currentGeometry.hitClamped(p.x, p.y))
                                pressed[0].consume()
                            }
                        }
                        if (painting) end()
                    }
                }
        ) {
            drawBoard(geometry, rowClues, colClues, board, palette, isColor, highlight, colors, texts)
        }
    }
}

/** 단서 숫자 레이아웃 캐시 (같은 숫자·크기는 한 번만 측정) */
private class TextCache(val measurer: TextMeasurer) {
    private val cache = HashMap<Long, TextLayoutResult>()

    fun layout(text: String, value: Int, fontPx: Float, scope: DrawScope): TextLayoutResult {
        val key = (value.toLong() shl 32) or fontPx.toRawBits().toLong().and(0xffffffffL)
        return cache.getOrPut(key) {
            measurer.measure(
                text,
                TextStyle(fontSize = with(scope) { fontPx.toSp() }, fontWeight = FontWeight.SemiBold),
            )
        }
    }
}

private fun DrawScope.drawBoard(
    g: BoardGeometry,
    rowClues: List<Clue>,
    colClues: List<Clue>,
    board: BoardUi,
    palette: List<Color>,
    isColor: Boolean,
    highlight: Hint?,
    colors: BoardColors,
    texts: TextCache,
) {
    fun fillColor(value: Int) = if (isColor) palette.getOrElse(value - 1) { colors.monoFill } else colors.monoFill

    val cell = g.cell
    val c0 = floor(-g.offsetX / cell).toInt().coerceIn(0, g.columns - 1)
    val c1 = (ceil((g.gridWidth - g.offsetX) / cell).toInt() - 1).coerceIn(0, g.columns - 1)
    val r0 = floor(-g.offsetY / cell).toInt().coerceIn(0, g.rows - 1)
    val r1 = (ceil((g.gridHeight - g.offsetY) / cell).toInt() - 1).coerceIn(0, g.rows - 1)

    // ---- 격자 ----
    clipRect(g.gridLeft, g.gridTop, g.gridLeft + g.gridWidth, g.gridTop + g.gridHeight) {
        drawRect(colors.background, Offset(g.gridLeft, g.gridTop), Size(g.gridWidth, g.gridHeight))

        if (highlight is Hint.Deduce) {
            val band = colors.highlight.copy(alpha = 0.12f)
            if (highlight.lineIsRow) drawRect(band, Offset(g.gridLeft, g.cellTop(highlight.line)), Size(g.gridWidth, cell))
            else drawRect(band, Offset(g.cellLeft(highlight.line), g.gridTop), Size(cell, g.gridHeight))
        }

        val crossInset = cell * 0.28f
        val crossWidth = (cell * 0.07f).coerceAtLeast(1.5f)
        for (r in r0..r1) {
            for (c in c0..c1) {
                val m = board.marks[r * g.columns + c]
                if (m == Mark.UNKNOWN) continue
                val x = g.cellLeft(c)
                val y = g.cellTop(r)
                if (m > 0) {
                    drawRect(fillColor(m), Offset(x, y), Size(cell, cell))
                } else {
                    drawLine(colors.cross, Offset(x + crossInset, y + crossInset), Offset(x + cell - crossInset, y + cell - crossInset), crossWidth)
                    drawLine(colors.cross, Offset(x + cell - crossInset, y + crossInset), Offset(x + crossInset, y + cell - crossInset), crossWidth)
                }
            }
        }

        val thin = 1f
        val bold = (cell * 0.06f).coerceIn(2f, 4f)
        for (c in c0..(c1 + 1)) {
            val x = g.cellLeft(c)
            val isBold = c % 5 == 0 || c == g.columns
            drawLine(if (isBold) colors.gridLineBold else colors.gridLine, Offset(x, g.gridTop), Offset(x, g.gridTop + g.gridHeight), if (isBold) bold else thin)
        }
        for (r in r0..(r1 + 1)) {
            val y = g.cellTop(r)
            val isBold = r % 5 == 0 || r == g.rows
            drawLine(if (isBold) colors.gridLineBold else colors.gridLine, Offset(g.gridLeft, y), Offset(g.gridLeft + g.gridWidth, y), if (isBold) bold else thin)
        }

        if (highlight != null) {
            val r = highlight.index / g.columns
            val c = highlight.index % g.columns
            drawRect(colors.highlight, Offset(g.cellLeft(c), g.cellTop(r)), Size(cell, cell), style = Stroke(width = bold * 1.4f))
        }
    }

    // ---- 단서 ----
    /** length = 0 이면 빈 줄을 뜻하는 "0" (항상 흐리게) */
    fun drawClueNumber(colorIndex: Int, length: Int, done: Boolean, x: Float, y: Float, w: Float, h: Float, fontPx: Float) {
        val color: Color
        if (isColor && length > 0) {
            val bg = fillColor(colorIndex)
            val inset = minOf(w, h) * 0.08f
            drawRect(bg.copy(alpha = if (done) 0.35f else 1f), Offset(x + inset, y + inset), Size(w - inset * 2, h - inset * 2))
            color = (if (bg.luminance() > 0.5f) Color.Black else Color.White).copy(alpha = if (done) 0.5f else 1f)
        } else {
            color = if (done || length == 0) colors.clueDone else colors.clueText
        }
        val layout = texts.layout(length.toString(), length, fontPx, this)
        drawText(layout, color, Offset(x + (w - layout.size.width) / 2, y + (h - layout.size.height) / 2))
    }

    fun Clue.items(): List<Pair<Int, Int>> = if (isEmpty()) listOf(0 to 0) else map { it.color to it.length }

    val slot = g.slot
    // 왼쪽(행) 단서: 오른쪽 정렬
    clipRect(g.originX, g.gridTop, g.gridLeft, g.gridTop + g.gridHeight) {
        drawRect(colors.clueBackground, Offset(g.originX, g.gridTop), Size(g.leftArea, g.gridHeight))
        val fontPx = minOf(slot, cell) * 0.6f
        for (r in r0..r1) {
            val items = rowClues[r].items()
            val y = g.cellTop(r)
            items.forEachIndexed { i, (color, length) ->
                drawClueNumber(color, length, board.rowDone[r], g.gridLeft - (items.size - i) * slot, y, slot, cell, fontPx)
            }
        }
    }
    // 위쪽(열) 단서: 아래 정렬
    clipRect(g.gridLeft, g.originY, g.gridLeft + g.gridWidth, g.gridTop) {
        drawRect(colors.clueBackground, Offset(g.gridLeft, g.originY), Size(g.gridWidth, g.topArea))
        val fontPx = minOf(slot, cell) * 0.6f
        for (c in c0..c1) {
            val items = colClues[c].items()
            val x = g.cellLeft(c)
            items.forEachIndexed { i, (color, length) ->
                drawClueNumber(color, length, board.colDone[c], x, g.gridTop - (items.size - i) * slot, cell, slot, fontPx)
            }
        }
    }
}
