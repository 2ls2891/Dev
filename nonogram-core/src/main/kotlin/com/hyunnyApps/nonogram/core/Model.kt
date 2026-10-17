package com.hyunnyApps.nonogram.core

/**
 * 줄 단서의 한 블록.
 * color는 1부터 시작하는 팔레트 번호다. 흑백 퍼즐은 모든 블록이 color = 1이다.
 */
data class Block(val color: Int, val length: Int) {
    init {
        require(color >= 1) { "color must be >= 1" }
        require(length >= 1) { "length must be >= 1" }
    }
}

typealias Clue = List<Block>

/**
 * 칸의 "가능한 값" 비트마스크.
 * bit 0 = 빈칸, bit c = 색 c. 값이 하나로 확정되면 비트가 하나만 남는다.
 */
object CellMask {
    const val EMPTY = 1

    fun of(value: Int): Int = 1 shl value

    /** 빈칸 + 1..colorCount 모든 색 */
    fun all(colorCount: Int): Int = (1 shl (colorCount + 1)) - 1

    fun isSolved(mask: Int): Boolean = mask != 0 && (mask and (mask - 1)) == 0

    /** 확정된 칸의 값(0 = 빈칸). 확정되지 않았으면 -1 */
    fun valueOf(mask: Int): Int = if (isSolved(mask)) Integer.numberOfTrailingZeros(mask) else -1

    fun allows(mask: Int, value: Int): Boolean = mask and (1 shl value) != 0
}

/**
 * 완성된 그림(정답). cells[r * width + c]는 0(빈칸) 또는 1..colorCount.
 */
class Grid(val width: Int, val height: Int, val cells: IntArray) {
    init {
        require(width >= 1 && height >= 1) { "size must be positive" }
        require(cells.size == width * height) { "cells size mismatch" }
        require(cells.all { it >= 0 }) { "cell values must be >= 0" }
    }

    val colorCount: Int get() = cells.maxOrNull() ?: 0

    operator fun get(row: Int, col: Int): Int = cells[row * width + col]

    fun row(r: Int): IntArray = IntArray(width) { cells[r * width + it] }

    fun col(c: Int): IntArray = IntArray(height) { cells[it * width + c] }

    fun rowClues(): List<Clue> = List(height) { clueOf(row(it)) }

    fun colClues(): List<Clue> = List(width) { clueOf(col(it)) }

    fun filledCount(): Int = cells.count { it != 0 }

    override fun equals(other: Any?): Boolean =
        other is Grid && width == other.width && height == other.height && cells.contentEquals(other.cells)

    override fun hashCode(): Int = (width * 31 + height) * 31 + cells.contentHashCode()

    companion object {
        /** RowCodec 형식의 문자열 줄 목록으로 생성 */
        fun fromRows(rows: List<String>): Grid {
            require(rows.isNotEmpty()) { "rows must not be empty" }
            val w = rows[0].length
            require(rows.all { it.length == w }) { "all rows must have same length" }
            val cells = IntArray(w * rows.size)
            rows.forEachIndexed { r, s -> s.forEachIndexed { c, ch -> cells[r * w + c] = RowCodec.decode(ch) } }
            return Grid(w, rows.size, cells)
        }
    }
}

/** 값 배열(0 = 빈칸)에서 단서를 계산한다. 같은 색이 이어지면 한 블록이다. */
fun clueOf(values: IntArray): Clue {
    val out = ArrayList<Block>()
    var i = 0
    while (i < values.size) {
        val v = values[i]
        if (v == 0) { i++; continue }
        var j = i
        while (j < values.size && values[j] == v) j++
        out += Block(v, j - i)
        i = j
    }
    return out
}

/**
 * 줄 문자열 인코딩. '.' = 빈칸, '1'..'9' = 1..9, 'a'..'z' = 10..35.
 * 퍼즐 JSON과 테스트 벡터가 이 형식을 공유한다(iOS도 동일하게 구현할 것).
 */
object RowCodec {
    const val MAX_VALUE = 35

    fun decode(ch: Char): Int = when (ch) {
        '.' -> 0
        in '1'..'9' -> ch - '0'
        in 'a'..'z' -> ch - 'a' + 10
        else -> throw IllegalArgumentException("invalid cell char '$ch'")
    }

    fun encode(value: Int): Char = when (value) {
        0 -> '.'
        in 1..9 -> '0' + value
        in 10..MAX_VALUE -> 'a' + (value - 10)
        else -> throw IllegalArgumentException("value out of range: $value")
    }

    fun encodeRows(grid: Grid): List<String> =
        List(grid.height) { r -> String(CharArray(grid.width) { c -> encode(grid[r, c]) }) }
}
