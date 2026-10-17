package nonogram.core

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class BoardSolverTest {
    private fun solvedGrid(result: SolveResult, w: Int, h: Int) =
        Grid(w, h, IntArray(w * h) { CellMask.valueOf(result.masks[it]) })

    @Test
    fun heartIsLineSolvable() {
        val grid = Grid.fromRows(
            listOf(
                ".11.11.",
                "1111111",
                "1111111",
                ".11111.",
                "..111..",
                "...1...",
            )
        )
        val result = BoardSolver(grid).solve()
        assertEquals(Outcome.SOLVED, result.outcome)
        assertEquals(0, result.probeEliminations)
        assertEquals(grid, solvedGrid(result, grid.width, grid.height))
    }

    @Test
    fun ambiguousDiagonalIsStuck() {
        // 2x2 대각선: 두 가지 해가 있으므로 확정 불가
        val grid = Grid.fromRows(listOf("1.", ".1"))
        assertEquals(Outcome.STUCK, BoardSolver(grid).solve().outcome)
    }

    @Test
    fun inconsistentCluesAreContradiction() {
        val solver = BoardSolver(listOf(listOf(Block(1, 2))), listOf(emptyList(), emptyList()))
        assertEquals(Outcome.CONTRADICTION, solver.solve().outcome)
    }

    @Test
    fun solvedResultAlwaysEqualsSourceGrid() {
        // 건전성: SOLVED면 반드시 원래 그림과 같아야 한다 (유일해 보장)
        val rnd = Random(7)
        var solved = 0
        var probing = 0
        repeat(400) {
            val w = rnd.nextInt(3, 12)
            val h = rnd.nextInt(3, 12)
            val colors = rnd.nextInt(1, 3)
            val grid = Grid(w, h, IntArray(w * h) { if (rnd.nextDouble() < 0.4) 0 else rnd.nextInt(1, colors + 1) })
            val result = BoardSolver(grid).solve()
            assertNotEquals(Outcome.CONTRADICTION, result.outcome)
            if (result.outcome == Outcome.SOLVED) {
                solved++
                if (result.probeEliminations > 0) probing++
                assertEquals(grid, solvedGrid(result, w, h))
            } else {
                // STUCK이어도 확정된 칸은 원래 그림과 일치해야 한다
                result.masks.forEachIndexed { i, m -> assertTrue(CellMask.allows(m, grid.cells[i])) }
            }
        }
        assertTrue(solved > 50, "too few solved samples: $solved")
        println("random boards: solved=$solved, needed probing=$probing")
    }

    @Test
    fun lineOnlyModeNeverUsesProbing() {
        val rnd = Random(11)
        repeat(100) {
            val grid = Grid(10, 10, IntArray(100) { if (rnd.nextBoolean()) 1 else 0 })
            val result = BoardSolver(grid).solve(useProbing = false)
            assertEquals(0, result.probeEliminations)
        }
    }
}
