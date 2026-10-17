package com.hyunnyApps.nonogram.ui.game

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.size
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
import com.hyunnyApps.nonogram.ui.theme.HandFont
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
    activeCell: Int,
    enabled: Boolean,
    onStrokeStart: (Int) -> Unit,
    onStrokeMove: (Int) -> Unit,
    onStrokeEnd: () -> Unit,
    onStrokeCancel: () -> Unit,
    modifier: Modifier = Modifier,
    /** 방금 맞춘 줄 형광펜 효과 (0→1 진행) */
    flash: LineFlash? = null,
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

        val boardWidthDp = with(density) { widthPx.toDp() }
        val boardHeightDp = with(density) { base.boardHeight.toDp() }
        Canvas(
            Modifier
                .size(boardWidthDp, boardHeightDp)
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
            drawBoard(geometry, rowClues, colClues, board, palette, isColor, highlight, activeCell, colors, texts, flash)
        }
    }
}

/** 방금 맞춘 줄 (형광펜이 단서 위를 쓱 지나간다) */
class LineFlash(val rows: Set<Int>, val cols: Set<Int>, val progress: Float)

/** 형광펜 효과: 앞쪽 60% 동안 칠해지고 나머지 동안 사라진다 */
private fun DrawScope.drawFlash(p: Float, x: Float, y: Float, w: Float, h: Float, horizontal: Boolean, colors: BoardColors) {
    val sweep = (p / 0.6f).coerceIn(0f, 1f)
    val fade = 1f - ((p - 0.6f) / 0.4f).coerceIn(0f, 1f)
    val color = colors.activeClue.copy(alpha = 0.9f * fade)
    if (horizontal) drawRect(color, Offset(x, y + h * 0.12f), Size(w * sweep, h * 0.76f))
    else drawRect(color, Offset(x + w * 0.12f, y), Size(w * 0.76f, h * sweep))
}

/** 단서 숫자 레이아웃 캐시 (같은 숫자·크기는 한 번만 측정) */
private class TextCache(val measurer: TextMeasurer) {
    private val cache = HashMap<Long, TextLayoutResult>()

    fun layout(text: String, value: Int, fontPx: Float, scope: DrawScope): TextLayoutResult {
        val key = (value.toLong() shl 32) or fontPx.toRawBits().toLong().and(0xffffffffL)
        return cache.getOrPut(key) {
            measurer.measure(
                text,
                TextStyle(fontSize = with(scope) { fontPx.toSp() }, fontFamily = HandFont, fontWeight = FontWeight.Bold),
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
    activeCell: Int,
    colors: BoardColors,
    texts: TextCache,
    flash: LineFlash?,
) {
    val activeRow = if (activeCell >= 0) activeCell / g.columns else -1
    val activeCol = if (activeCell >= 0) activeCell % g.columns else -1
    fun fillColor(value: Int) = if (isColor) palette.getOrElse(value - 1) { colors.monoFill } else colors.monoFill

    val cell = g.cell
    val c0 = floor(-g.offsetX / cell).toInt().coerceIn(0, g.columns - 1)
    val c1 = (ceil((g.gridWidth - g.offsetX) / cell).toInt() - 1).coerceIn(0, g.columns - 1)
    val r0 = floor(-g.offsetY / cell).toInt().coerceIn(0, g.rows - 1)
    val r1 = (ceil((g.gridHeight - g.offsetY) / cell).toInt() - 1).coerceIn(0, g.rows - 1)

    // ---- 격자 ----
    clipRect(g.gridLeft, g.gridTop, g.gridLeft + g.gridWidth, g.gridTop + g.gridHeight) {
        drawRect(colors.background, Offset(g.gridLeft, g.gridTop), Size(g.gridWidth, g.gridHeight))

        // 손가락이 있는 행·열 강조 (어느 단서를 보고 있는지 바로 보이게)
        if (activeRow >= 0) {
            drawRect(colors.activeLine, Offset(g.gridLeft, g.cellTop(activeRow)), Size(g.gridWidth, cell))
            drawRect(colors.activeLine, Offset(g.cellLeft(activeCol), g.gridTop), Size(cell, g.gridHeight))
        }

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
        if (activeRow >= 0) drawRect(colors.activeClue, Offset(g.originX, g.cellTop(activeRow)), Size(g.leftArea, cell))
        val fontPx = minOf(slot, cell) * 0.72f
        for (r in r0..r1) {
            val items = rowClues[r].items()
            val y = g.cellTop(r)
            if (flash != null && r in flash.rows) drawFlash(flash.progress, g.originX, y, g.leftArea, cell, horizontal = true, colors)
            items.forEachIndexed { i, (color, length) ->
                drawClueNumber(color, length, board.rowDone[r], g.gridLeft - (items.size - i) * slot, y, slot, cell, fontPx)
            }
            // 맞춘 줄은 연필로 쓱 그어 지운다
            if (board.rowDone[r] && rowClues[r].isNotEmpty()) {
                val x0 = g.gridLeft - items.size * slot + slot * 0.2f
                drawLine(colors.clueDone, Offset(x0, y + cell * 0.55f), Offset(g.gridLeft - slot * 0.2f, y + cell * 0.45f), (cell * 0.07f).coerceIn(1.5f, 3f))
            }
        }
    }
    // 위쪽(열) 단서: 아래 정렬
    clipRect(g.gridLeft, g.originY, g.gridLeft + g.gridWidth, g.gridTop) {
        drawRect(colors.clueBackground, Offset(g.gridLeft, g.originY), Size(g.gridWidth, g.topArea))
        if (activeCol >= 0) drawRect(colors.activeClue, Offset(g.cellLeft(activeCol), g.originY), Size(cell, g.topArea))
        val fontPx = minOf(slot, cell) * 0.72f
        for (c in c0..c1) {
            val items = colClues[c].items()
            val x = g.cellLeft(c)
            if (flash != null && c in flash.cols) drawFlash(flash.progress, x, g.originY, cell, g.topArea, horizontal = false, colors)
            items.forEachIndexed { i, (color, length) ->
                drawClueNumber(color, length, board.colDone[c], x, g.gridTop - (items.size - i) * slot, cell, slot, fontPx)
            }
            if (board.colDone[c] && colClues[c].isNotEmpty()) {
                val y0 = g.gridTop - items.size * slot + slot * 0.2f
                drawLine(colors.clueDone, Offset(x + cell * 0.45f, y0), Offset(x + cell * 0.55f, g.gridTop - slot * 0.2f), (cell * 0.07f).coerceIn(1.5f, 3f))
            }
        }
    }
}
