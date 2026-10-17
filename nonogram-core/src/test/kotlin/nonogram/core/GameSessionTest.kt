package nonogram.core

import nonogram.core.daily.DailyCalendar
import nonogram.core.daily.Streak
import nonogram.core.game.GameSession
import nonogram.core.game.Hint
import nonogram.core.game.Mark
import nonogram.core.game.MarkCodec
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GameSessionTest {
    private val heart = Grid.fromRows(
        listOf(
            ".11.11.",
            "1111111",
            "1111111",
            ".11111.",
            "..111..",
            "...1...",
        )
    )

    private fun idx(r: Int, c: Int) = r * heart.width + c

    @Test
    fun strokeLocksToRowAndOnlyFillsUnknown() {
        val g = GameSession(heart)
        g.beginStroke(idx(1, 2), Mark.CROSS)
        g.endStroke()
        g.beginStroke(idx(1, 0), 1)
        g.extendStroke(idx(2, 5)) // 대각선 방향이지만 가로 이동이 더 크므로 가로 고정
        g.endStroke()
        val row1 = (0 until 7).map { g.markAt(idx(1, it)) }
        assertEquals(listOf(1, 1, Mark.CROSS, 1, 1, 1, 0), row1)
        assertEquals(0, g.markAt(idx(2, 5)))
    }

    @Test
    fun strokeShrinkRestoresCells() {
        val g = GameSession(heart)
        g.beginStroke(idx(0, 0), 1)
        g.extendStroke(idx(0, 6))
        g.extendStroke(idx(0, 2))
        g.endStroke()
        assertEquals(listOf(1, 1, 1, 0, 0, 0, 0), (0 until 7).map { g.markAt(idx(0, it)) })
    }

    @Test
    fun startingOnSameMarkErases() {
        val g = GameSession(heart)
        g.beginStroke(idx(1, 0), 1); g.extendStroke(idx(1, 6)); g.endStroke()
        g.beginStroke(idx(1, 3), Mark.CROSS); g.endStroke()
        g.beginStroke(idx(1, 0), 1); g.extendStroke(idx(1, 6)); g.endStroke()
        assertEquals(listOf(0, 0, 0, Mark.CROSS, 0, 0, 0), (0 until 7).map { g.markAt(idx(1, it)) })
    }

    @Test
    fun cancelStrokeRestoresWithoutHistory() {
        val g = GameSession(heart)
        g.beginStroke(idx(1, 0), 1); g.extendStroke(idx(1, 4))
        g.cancelStroke()
        assertTrue(g.marksSnapshot().all { it == 0 })
        assertFalse(g.canUndo)
    }

    @Test
    fun undoRedo() {
        val g = GameSession(heart)
        g.beginStroke(idx(1, 0), 1); g.extendStroke(idx(1, 6)); g.endStroke()
        assertTrue(g.undo())
        assertTrue(g.marksSnapshot().all { it == 0 })
        assertTrue(g.redo())
        assertEquals(7, (0 until 7).count { g.markAt(idx(1, it)) == 1 })
        assertFalse(g.redo())
    }

    @Test
    fun solvingByCluesAndHintsReachesCompletion() {
        val g = GameSession(heart)
        var guard = 0
        while (!g.isSolved()) {
            val hint = g.findHint()!!
            g.setMark(hint.index, hint.mark)
            assertTrue(guard++ < heart.width * heart.height)
        }
        assertNull(g.findHint())
    }

    @Test
    fun hintFixesMistakeFirst() {
        val g = GameSession(heart)
        g.setMark(idx(0, 0), 1) // 정답은 빈칸
        val hint = g.findHint()
        assertIs<Hint.FixMistake>(hint)
        assertEquals(idx(0, 0), hint.index)
        assertEquals(Mark.CROSS, hint.mark)
    }

    @Test
    fun markCodecRoundTripAndRejectsCorruption() {
        val marks = intArrayOf(0, 1, Mark.CROSS, 2, 0)
        val s = MarkCodec.encode(marks)
        assertEquals(".1/2.", s)
        assertContentEquals(marks, MarkCodec.decode(s, 5, 2))
        assertNull(MarkCodec.decode(s, 5, 1)) // 색 수 초과
        assertNull(MarkCodec.decode(s, 6, 2)) // 크기 불일치
        assertNull(MarkCodec.decode(".1#2.", 5, 2))
    }

    @Test
    fun dailyAndStreak() {
        val epoch = LocalDate.of(2027, 1, 4)
        assertEquals(69, DailyCalendar.dayIndex(epoch, LocalDate.of(2027, 3, 14)))
        assertEquals(0, DailyCalendar.puzzlePosition(-3, 10))
        assertEquals(2, DailyCalendar.puzzlePosition(12, 10))

        var s = Streak()
        s = s.onDailyCleared(5)
        s = s.onDailyCleared(6)
        s = s.onDailyCleared(6)
        assertEquals(Streak(2, 2, 6), s)
        assertEquals(2, s.displayed(7))
        assertEquals(0, s.displayed(8))
        s = s.onDailyCleared(9)
        assertEquals(Streak(1, 2, 9), s)
    }
}
