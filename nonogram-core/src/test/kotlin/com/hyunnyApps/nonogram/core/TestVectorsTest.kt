package com.hyunnyApps.nonogram.core

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** testvectors/ 의 공용 벡터를 검증한다. iOS도 같은 파일로 같은 검사를 한다 */
class TestVectorsTest {
    private val dir = File(System.getProperty("testvectors.dir"))
    private fun load(name: String) = Json.parseToJsonElement(File(dir, name).readText()).jsonArray

    @Test
    fun lineSolver() {
        for (case in load("line_solver.json")) {
            val o = case.jsonObject
            val clue = o.getValue("clue").jsonArray.map { b -> b.jsonArray.let { Block(it[0].jsonPrimitive.int, it[1].jsonPrimitive.int) } }
            val input = o.getValue("in").jsonArray.map { it.jsonPrimitive.int }.toIntArray()
            val actual = LineSolver.solve(input, clue)
            val expected = o.getValue("out")
            if (expected is JsonNull) assertNull(actual, o.toString())
            else assertContentEquals((expected as JsonArray).map { it.jsonPrimitive.int }.toIntArray(), actual, o.toString())
        }
    }

    @Test
    fun boardSolver() {
        for (case in load("board_solver.json")) {
            val o = case.jsonObject
            val grid = Grid.fromRows(o.getValue("rows").jsonArray.map { it.jsonPrimitive.content })
            val result = BoardSolver(grid).solve()
            assertEquals(o.getValue("outcome").jsonPrimitive.content, result.outcome.name, o.toString())
            o["lineRounds"]?.let { assertEquals(it.jsonPrimitive.int, result.lineRounds) }
            o["probeEliminations"]?.let { assertEquals(it.jsonPrimitive.int, result.probeEliminations) }
            o["level"]?.let { assertEquals(it.jsonPrimitive.int, DifficultyRater.rate(grid, result).level) }
        }
    }
}
