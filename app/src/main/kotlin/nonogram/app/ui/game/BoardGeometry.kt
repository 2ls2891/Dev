package nonogram.app.ui.game

import kotlin.math.floor

/**
 * 보드 배치 계산 (순수 계산, 단위 테스트 가능).
 *
 *  ┌──────┬───────────────┐
 *  │      │  열 단서(위)   │  topArea  : 세로로 고정, 가로는 칸과 함께 확대/이동
 *  ├──────┼───────────────┤
 *  │ 행   │     격자       │  격자 영역 : 확대/이동(두 손가락)
 *  │ 단서 │               │
 *  └──────┴───────────────┘
 *  leftArea : 가로로 고정, 세로는 칸과 함께 확대/이동
 *
 * 단서 칸(slot)은 최소 크기를 보장해 큰 판에서도 숫자가 읽히게 하고, 남는 공간으로 격자 칸 크기를 정한다.
 */
class BoardGeometry(
    private val viewportWidth: Float,
    private val viewportHeight: Float,
    val columns: Int,
    val rows: Int,
    private val leftSlots: Int,
    private val topSlots: Int,
    private val minSlot: Float,
    private val maxSlot: Float,
    scale: Float = 1f,
    offsetX: Float = 0f,
    offsetY: Float = 0f,
    val maxScale: Float = 4f,
) {
    val baseCell: Float
    val slot: Float

    init {
        require(columns > 0 && rows > 0)
        val ls = leftSlots.coerceAtLeast(1)
        val ts = topSlots.coerceAtLeast(1)
        var cell = minOf(viewportWidth / (ls + columns), viewportHeight / (ts + rows))
        var s = cell.coerceIn(minSlot, maxSlot)
        if (s != cell) cell = minOf((viewportWidth - ls * s) / columns, (viewportHeight - ts * s) / rows)
        baseCell = cell.coerceAtLeast(1f)
        slot = s
    }

    val leftArea = leftSlots.coerceAtLeast(1) * slot
    val topArea = topSlots.coerceAtLeast(1) * slot

    /** 확대 1배일 때 보드 전체 크기, 화면 가운데 정렬 */
    val boardWidth = leftArea + columns * baseCell
    val boardHeight = topArea + rows * baseCell
    val originX = ((viewportWidth - boardWidth) / 2).coerceAtLeast(0f)
    val originY = ((viewportHeight - boardHeight) / 2).coerceAtLeast(0f)

    /** 격자가 보이는 영역 (고정) */
    val gridLeft = originX + leftArea
    val gridTop = originY + topArea
    val gridWidth = columns * baseCell
    val gridHeight = rows * baseCell

    val scale = scale.coerceIn(1f, maxScale)
    val cell = baseCell * this.scale
    val offsetX = offsetX.coerceIn(gridWidth - columns * cell, 0f)
    val offsetY = offsetY.coerceIn(gridHeight - rows * cell, 0f)

    fun cellLeft(col: Int) = gridLeft + offsetX + col * cell
    fun cellTop(row: Int) = gridTop + offsetY + row * cell

    fun inGrid(x: Float, y: Float) = x >= gridLeft && x < gridLeft + gridWidth && y >= gridTop && y < gridTop + gridHeight

    /** 격자 안이면 칸 index, 아니면 -1 (드래그 시작용) */
    fun hit(x: Float, y: Float): Int = if (!inGrid(x, y)) -1 else hitClamped(x, y)

    /** 격자 밖으로 나가도 가장 가까운 칸 (드래그 중 가장자리 처리용) */
    fun hitClamped(x: Float, y: Float): Int {
        val c = floor((x - gridLeft - offsetX) / cell).toInt().coerceIn(0, columns - 1)
        val r = floor((y - gridTop - offsetY) / cell).toInt().coerceIn(0, rows - 1)
        return r * columns + c
    }

    /** 같은 보드에 다른 확대/이동 값을 적용한 배치 */
    fun withTransform(scale: Float, offsetX: Float, offsetY: Float) = BoardGeometry(
        viewportWidth, viewportHeight, columns, rows, leftSlots, topSlots, minSlot, maxSlot,
        scale, offsetX, offsetY, maxScale,
    )

    /** 두 손가락 제스처: (cx, cy)를 중심으로 zoom 배 확대하고 (dx, dy)만큼 이동한 새 변환 */
    fun transformed(cx: Float, cy: Float, zoom: Float, dx: Float, dy: Float): Triple<Float, Float, Float> {
        val newScale = (scale * zoom).coerceIn(1f, maxScale)
        val k = newScale / scale
        val px = cx - gridLeft
        val py = cy - gridTop
        val nx = px - (px - offsetX) * k + dx
        val ny = py - (py - offsetY) * k + dy
        return Triple(newScale, nx, ny)
    }
}
