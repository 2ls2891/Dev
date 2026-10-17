package com.hyunnyApps.nonogram.core

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertNull

class LineSolverTest {
    private val unknown = CellMask.all(1)
    private val filled = CellMask.of(1)
    private val empty = CellMask.EMPTY

    private fun mono(vararg lengths: Int): Clue = lengths.map { Block(1, it) }

    @Test
    fun overlapOfLongBlock() {
        // 10칸에 8짜리 블록 → 가운데 6칸 확정
        val out = LineSolver.solve(IntArray(10) { unknown }, mono(8))!!
        val expected = IntArray(10) { if (it in 2..7) filled else unknown }
        assertContentEquals(expected, out)
    }

    @Test
    fun emptyClueMakesAllEmpty() {
        val out = LineSolver.solve(IntArray(5) { unknown }, emptyList())!!
        assertContentEquals(IntArray(5) { empty }, out)
    }

    @Test
    fun fullLine() {
        val out = LineSolver.solve(IntArray(4) { unknown }, mono(4))!!
        assertContentEquals(IntArray(4) { filled }, out)
    }

    @Test
    fun exactFitWithGaps() {
        // 1 1 1 in 5 → #.#.#
        val out = LineSolver.solve(IntArray(5) { unknown }, mono(1, 1, 1))!!
        assertContentEquals(intArrayOf(filled, empty, filled, empty, filled), out)
    }

    @Test
    fun differentColorsNeedNoGap() {
        // 색1 2칸 + 색2 1칸 in 3 → 112
        val out = LineSolver.solve(IntArray(3) { CellMask.all(2) }, listOf(Block(1, 2), Block(2, 1)))!!
        assertContentEquals(intArrayOf(CellMask.of(1), CellMask.of(1), CellMask.of(2)), out)
    }

    @Test
    fun contradiction() {
        assertNull(LineSolver.solve(IntArray(3) { unknown }, mono(2, 2)))
        assertNull(LineSolver.solve(intArrayOf(filled, unknown, unknown), emptyList()))
    }

    @Test
    fun matchesBruteForceOnRandomLines() {
        val rnd = Random(20261017)
        repeat(5000) {
            val n = rnd.nextInt(1, 10)
            val colors = rnd.nextInt(1, 4)
            // 실제 해가 있는 단서를 만들기 위해 무작위 줄에서 단서를 뽑는다
            val solution = IntArray(n) { if (rnd.nextDouble() < 0.45) 0 else rnd.nextInt(1, colors + 1) }
            val clue = clueOf(solution)
            // 일부 칸은 마스크를 좁혀서 (정답 포함/불포함 섞어서) 다양한 상태를 만든다
            val masks = IntArray(n) { i ->
                when (rnd.nextInt(6)) {
                    0 -> CellMask.of(solution[i])
                    1 -> CellMask.of(rnd.nextInt(0, colors + 1))
                    2 -> CellMask.all(colors) and CellMask.of(rnd.nextInt(0, colors + 1)).inv()
                    else -> CellMask.all(colors)
                }.let { if (it == 0) CellMask.all(colors) else it }
            }
            val expected = LineBruteForce.solve(masks, clue)
            val actual = LineSolver.solve(masks, clue)
            if (expected == null) assertNull(actual, "n=$n clue=$clue masks=${masks.toList()}")
            else assertContentEquals(expected, actual, "n=$n clue=$clue masks=${masks.toList()}")
        }
    }
}
