package com.hyunnyApps.nonogram.core.game

import com.hyunnyApps.nonogram.core.CellMask
import com.hyunnyApps.nonogram.core.Clue
import com.hyunnyApps.nonogram.core.Grid
import com.hyunnyApps.nonogram.core.LineSolver
import com.hyunnyApps.nonogram.core.RowCodec
import com.hyunnyApps.nonogram.core.clueOf

/**
 * 사용자가 칸에 남긴 표시.
 * UNKNOWN(0) = 미정, CROSS(-1) = X(비어 있다고 표시), 1..C = 해당 색으로 칠함.
 */
object Mark {
    const val UNKNOWN = 0
    const val CROSS = -1
}

/** 진행 저장용 문자열 인코딩. '.' = 미정, '/' = X, 그 외는 RowCodec(1..9, a..z) */
object MarkCodec {
    fun encode(marks: IntArray): String = buildString(marks.size) {
        for (m in marks) append(if (m == Mark.CROSS) '/' else RowCodec.encode(m))
    }

    /** 형식이 맞지 않으면 null (저장 파일 손상·버전 차이 방어) */
    fun decode(s: String, size: Int, colorCount: Int): IntArray? {
        if (s.length != size) return null
        val out = IntArray(size)
        for (i in 0 until size) {
            val ch = s[i]
            out[i] = if (ch == '/') Mark.CROSS
            else runCatching { RowCodec.decode(ch) }.getOrNull()?.takeIf { it <= colorCount } ?: return null
        }
        return out
    }
}

/** 한 번의 드래그가 바꾼 칸들 (실행 취소 단위) */
private class Change(val index: Int, val before: Int, val after: Int)

sealed interface Hint {
    val index: Int
    /** 칸에 넣을 올바른 표시 (빈칸이면 CROSS) */
    val mark: Int

    /** 잘못 표시한 칸을 바로잡는다 */
    data class FixMistake(override val index: Int, override val mark: Int) : Hint

    /** 현재 상태에서 한 줄만 보고 추론할 수 있는 칸. lineIsRow/line = 근거가 된 줄 */
    data class Deduce(override val index: Int, override val mark: Int, val lineIsRow: Boolean, val line: Int) : Hint

    /** 줄 추론으로 찾을 수 없을 때 정답 칸을 공개한다 */
    data class Reveal(override val index: Int, override val mark: Int) : Hint
}

/**
 * 한 판의 게임 규칙 (UI·플랫폼 독립). ViewModel이 소유하고 화면은 marks를 그리기만 한다.
 *
 * 드래그 규칙 (일반적인 피크로스 방식)
 *  - 시작 칸의 표시가 현재 도구와 같으면 "지우기" 모드: 경로 위에서 같은 표시만 지운다.
 *  - 아니면 "칠하기" 모드: 시작 칸은 무조건 칠하고, 경로 위에서는 미정 칸만 칠한다(다른 표시는 보존).
 *  - 시작 칸에서 벗어나는 순간 가로/세로 중 한 축으로 고정된다.
 */
class GameSession(val solution: Grid, initialMarks: IntArray? = null) {
    val width = solution.width
    val height = solution.height
    val colorCount = maxOf(1, solution.colorCount)
    val rowClues: List<Clue> = solution.rowClues()
    val colClues: List<Clue> = solution.colClues()

    private val marks: IntArray = initialMarks?.copyOf()?.also { require(it.size == width * height) } ?: IntArray(width * height)
    private val undoStack = ArrayDeque<List<Change>>()
    private val redoStack = ArrayDeque<List<Change>>()

    /** 변경될 때마다 증가. UI 갱신·저장 트리거에 사용 */
    var version: Long = 0
        private set

    fun marksSnapshot(): IntArray = marks.copyOf()

    fun markAt(index: Int): Int = marks[index]

    val canUndo get() = undoStack.isNotEmpty()
    val canRedo get() = redoStack.isNotEmpty()

    // ---- 드래그 ----

    private var strokeStart = -1
    private var strokeTool = 0
    private var strokeErase = false
    private var strokeAxis = 0 // 0 = 미정, 1 = 가로, 2 = 세로
    private val strokeOriginal = LinkedHashMap<Int, Int>()

    val isStroking get() = strokeStart >= 0

    /** @param tool 칠할 색(1..C) 또는 Mark.CROSS */
    fun beginStroke(index: Int, tool: Int) {
        require(index in marks.indices)
        require(tool == Mark.CROSS || tool in 1..colorCount) { "invalid tool $tool" }
        if (isStroking) endStroke()
        strokeStart = index
        strokeTool = tool
        strokeErase = marks[index] == tool
        strokeAxis = 0
        strokeOriginal.clear()
        applySegment(listOf(index))
    }

    fun extendStroke(index: Int) {
        if (!isStroking || index !in marks.indices) return
        val sr = strokeStart / width
        val sc = strokeStart % width
        val r = index / width
        val c = index % width
        if (strokeAxis == 0 && index != strokeStart) {
            strokeAxis = when {
                r == sr -> 1
                c == sc -> 2
                kotlin.math.abs(c - sc) >= kotlin.math.abs(r - sr) -> 1
                else -> 2
            }
        }
        val segment = when (strokeAxis) {
            1 -> (minOf(sc, c)..maxOf(sc, c)).map { sr * width + it }
            2 -> (minOf(sr, r)..maxOf(sr, r)).map { it * width + sc }
            else -> listOf(strokeStart)
        }
        applySegment(segment)
    }

    /** @return 이번 드래그로 실제 바뀐 칸이 있으면 true */
    fun endStroke(): Boolean {
        if (!isStroking) return false
        val changes = strokeOriginal.mapNotNull { (i, before) -> if (marks[i] != before) Change(i, before, marks[i]) else null }
        strokeStart = -1
        strokeOriginal.clear()
        if (changes.isEmpty()) return false
        undoStack.addLast(changes)
        redoStack.clear()
        return true
    }

    /** 진행 중인 드래그를 기록 없이 되돌린다 (예: 두 손가락 확대가 시작될 때) */
    fun cancelStroke() {
        if (!isStroking) return
        for ((i, before) in strokeOriginal) marks[i] = before
        strokeStart = -1
        strokeOriginal.clear()
        version++
    }

    private fun applySegment(segment: List<Int>) {
        val inSegment = segment.toHashSet()
        // 경로에서 빠진 칸은 원래대로
        val it = strokeOriginal.entries.iterator()
        while (it.hasNext()) {
            val (i, before) = it.next()
            if (i !in inSegment) {
                marks[i] = before
                it.remove()
            }
        }
        for (i in segment) {
            val before = strokeOriginal.getOrPut(i) { marks[i] }
            marks[i] = when {
                strokeErase -> if (before == strokeTool) Mark.UNKNOWN else before
                i == strokeStart || before == Mark.UNKNOWN -> strokeTool
                else -> before
            }
        }
        version++
    }

    // ---- 실행 취소 ----

    fun undo(): Boolean {
        if (isStroking) return false
        val changes = undoStack.removeLastOrNull() ?: return false
        for (ch in changes) marks[ch.index] = ch.before
        redoStack.addLast(changes)
        version++
        return true
    }

    fun redo(): Boolean {
        if (isStroking) return false
        val changes = redoStack.removeLastOrNull() ?: return false
        for (ch in changes) marks[ch.index] = ch.after
        undoStack.addLast(changes)
        version++
        return true
    }

    /** 힌트 등 단일 칸 변경 (실행 취소 가능) */
    fun setMark(index: Int, mark: Int) {
        if (isStroking) endStroke()
        val before = marks[index]
        if (before == mark) return
        marks[index] = mark
        undoStack.addLast(listOf(Change(index, before, mark)))
        redoStack.clear()
        version++
    }

    fun clearAll() {
        if (isStroking) endStroke()
        val changes = marks.indices.filter { marks[it] != Mark.UNKNOWN }.map { Change(it, marks[it], Mark.UNKNOWN) }
        if (changes.isEmpty()) return
        for (ch in changes) marks[ch.index] = Mark.UNKNOWN
        undoStack.addLast(changes)
        redoStack.clear()
        version++
    }

    // ---- 판정 ----

    private fun lineValues(row: Boolean, line: Int): IntArray =
        if (row) IntArray(width) { c -> marks[line * width + c].coerceAtLeast(0) }
        else IntArray(height) { r -> marks[r * width + line].coerceAtLeast(0) }

    /** 칠한 칸만 보고 단서를 만족하는 줄인지 (단서 숫자를 흐리게 표시할 때 사용) */
    fun isRowSatisfied(row: Int): Boolean = clueOf(lineValues(true, row)) == rowClues[row]

    fun isColSatisfied(col: Int): Boolean = clueOf(lineValues(false, col)) == colClues[col]

    /**
     * 클리어 판정: 모든 행/열 단서를 만족하면 완료.
     * 수록 퍼즐은 해가 유일하므로 정답 배열과 같다는 뜻이다.
     */
    fun isSolved(): Boolean = (0 until height).all(::isRowSatisfied) && (0 until width).all(::isColSatisfied)

    fun filledProgress(): Float {
        val target = solution.filledCount()
        if (target == 0) return 1f
        return marks.indices.count { marks[it] > 0 && marks[it] == solution.cells[it] }.toFloat() / target
    }

    // ---- 힌트 ----

    private fun correctMark(index: Int) = solution.cells[index].let { if (it == 0) Mark.CROSS else it }

    private fun isMistake(index: Int): Boolean {
        val m = marks[index]
        val s = solution.cells[index]
        return (m > 0 && m != s) || (m == Mark.CROSS && s != 0)
    }

    /** 다음 힌트. 이미 풀렸으면 null */
    fun findHint(): Hint? {
        if (isSolved()) return null
        marks.indices.firstOrNull(::isMistake)?.let { return Hint.FixMistake(it, correctMark(it)) }

        // 실수가 없으므로 현재 표시는 모두 정답과 일치한다 → 그대로 마스크로 만들어 한 줄 추론
        val all = CellMask.all(colorCount)
        val masks = IntArray(marks.size) { i ->
            when (val m = marks[i]) {
                Mark.UNKNOWN -> all
                Mark.CROSS -> CellMask.EMPTY
                else -> CellMask.of(m)
            }
        }
        for (row in 0 until height) {
            val line = IntArray(width) { masks[row * width + it] }
            val solved = LineSolver.solve(line, rowClues[row]) ?: continue
            for (c in 0 until width) {
                val i = row * width + c
                if (marks[i] == Mark.UNKNOWN && CellMask.isSolved(solved[c])) return Hint.Deduce(i, correctMark(i), true, row)
            }
        }
        for (col in 0 until width) {
            val line = IntArray(height) { masks[it * width + col] }
            val solved = LineSolver.solve(line, colClues[col]) ?: continue
            for (r in 0 until height) {
                val i = r * width + col
                if (marks[i] == Mark.UNKNOWN && CellMask.isSolved(solved[r])) return Hint.Deduce(i, correctMark(i), false, col)
            }
        }
        val index = marks.indices.firstOrNull { marks[it] == Mark.UNKNOWN && solution.cells[it] != 0 }
            ?: marks.indices.firstOrNull { marks[it] == Mark.UNKNOWN }
            ?: return null
        return Hint.Reveal(index, correctMark(index))
    }
}
