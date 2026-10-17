package com.hyunnyApps.nonogram.core

import com.hyunnyApps.nonogram.core.format.ContentJson
import com.hyunnyApps.nonogram.core.format.DailyOut
import com.hyunnyApps.nonogram.core.format.IndexOut
import com.hyunnyApps.nonogram.core.format.PackOut
import com.hyunnyApps.nonogram.core.format.PuzzleOut
import com.hyunnyApps.nonogram.core.game.GameSession
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** 앱에 번들되는 content/packs 전체를 앱과 같은 파서·규칙으로 검증한다 */
class BundledContentTest {
    private val dir = File(System.getProperty("content.dir"))

    @Test
    fun allPuzzlesParseAndCompleteWithTheirSolution() {
        val index = ContentJson.decodeFromString(IndexOut.serializer(), File(dir, "index.json").readText())
        val packs = index.packs.map { ContentJson.decodeFromString(PackOut.serializer(), File(dir, it.file).readText()) }
        val daily = ContentJson.decodeFromString(DailyOut.serializer(), File(dir, index.daily.file).readText())
        val all = packs.flatMap { it.puzzles } + daily.puzzles

        assertEquals(all.size, all.map { it.id }.toSet().size, "puzzle ids must be unique")
        index.packs.zip(packs).forEach { (summary, pack) ->
            assertEquals(summary.count, pack.puzzles.size, summary.id)
            assertTrue(summary.themeTitle.ko.isNotBlank(), summary.id)
        }
        assertEquals(index.daily.days, daily.puzzles.size)
        all.forEach(::checkPuzzle)
    }

    private fun checkPuzzle(p: PuzzleOut) {
        val grid = Grid.fromRows(p.rows)
        assertEquals(p.w, grid.width, p.id)
        assertEquals(p.h, grid.height, p.id)
        assertTrue(grid.colorCount <= p.palette.size, p.id)
        assertEquals(p.h, p.reveal.rows.size, p.id)
        assertTrue(p.reveal.rows.all { it.length == p.w }, p.id)
        // 정답을 그대로 칠하면 클리어 판정이 나야 한다
        val session = GameSession(grid, grid.cells.copyOf())
        assertTrue(session.isSolved(), p.id)
    }
}
