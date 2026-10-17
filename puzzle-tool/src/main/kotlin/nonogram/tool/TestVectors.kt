package nonogram.tool

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import nonogram.core.Block
import nonogram.core.BoardSolver
import nonogram.core.CellMask
import nonogram.core.Grid
import nonogram.core.LineBruteForce
import nonogram.core.clueOf
import java.io.File
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.random.Random

/**
 * iOS 포팅 검증용 테스트 벡터. 기대값은 LineSolver가 아닌 전수 탐색(LineBruteForce)으로 만든다.
 * Kotlin(core 테스트)과 Swift 테스트가 같은 파일을 읽어서 검증한다.
 */
fun writeTestVectors(outDir: File, packsDir: File) {
    outDir.mkdirs()
    val pretty = kotlinx.serialization.json.Json { prettyPrint = false }

    // 1) 줄 추론
    val rnd = Random(424242)
    val lines = buildJsonArray {
        repeat(600) {
            val n = rnd.nextInt(1, 11)
            val colors = if (it < 300) 1 else rnd.nextInt(2, 4)
            val solution = IntArray(n) { if (rnd.nextDouble() < 0.4) 0 else rnd.nextInt(1, colors + 1) }
            val clue = if (rnd.nextInt(20) == 0) listOf(Block(1, n + 1)) else clueOf(solution) // 가끔 모순 케이스
            val masks = IntArray(n) { i ->
                when (rnd.nextInt(5)) {
                    0 -> CellMask.of(solution[i])
                    1 -> CellMask.of(rnd.nextInt(0, colors + 1))
                    else -> CellMask.all(colors)
                }
            }
            val out = LineBruteForce.solve(masks, clue)
            add(buildJsonObject {
                put("clue", JsonArray(clue.map { b -> JsonArray(listOf(JsonPrimitive(b.color), JsonPrimitive(b.length))) }))
                put("in", JsonArray(masks.map { JsonPrimitive(it) }))
                put("out", out?.let { o -> JsonArray(o.map { JsonPrimitive(it) }) } ?: JsonNull)
            })
        }
    }
    File(outDir, "line_solver.json").writeText(pretty.encodeToString(JsonArray.serializer(), lines))

    // 2) 판 추론: 실제 수록 퍼즐 일부 (probing 필요한 것 포함) + 해가 여러 개인 예
    val index = json.decodeFromString<IndexOut>(File(packsDir, "index.json").readText())
    val puzzles = index.packs.flatMap { json.decodeFromString<PackOut>(File(packsDir, it.file).readText()).puzzles }
    val picked = (puzzles.filter { it.difficulty.technique == "probing" && it.kind == "mono" }.take(3) +
        puzzles.filter { it.difficulty.technique == "probing" && it.kind == "color" }.take(2) +
        puzzles.filter { it.difficulty.technique == "line" && it.kind == "mono" }.take(5) +
        puzzles.filter { it.kind == "color" }.take(5))
    val boards = buildJsonArray {
        for (p in picked) {
            val grid = Grid.fromRows(p.rows)
            val r = BoardSolver(grid).solve()
            add(buildJsonObject {
                put("id", p.id)
                put("rows", JsonArray(p.rows.map { JsonPrimitive(it) }))
                put("outcome", r.outcome.name)
                put("lineRounds", r.lineRounds)
                put("probeEliminations", r.probeEliminations)
                put("level", p.difficulty.level)
            })
        }
        val ambiguous = listOf("1.", ".1")
        add(buildJsonObject {
            put("id", "ambiguous-diagonal")
            put("rows", JsonArray(ambiguous.map { JsonPrimitive(it) }))
            put("outcome", BoardSolver(Grid.fromRows(ambiguous)).solve().outcome.name)
        })
    }
    File(outDir, "board_solver.json").writeText(pretty.encodeToString(JsonArray.serializer(), boards))

    // 3) 데일리 dayIndex (서머타임 경계 포함). 기기 현지 날짜 기준이므로 날짜만 비교한다
    val epoch = LocalDate.parse(index.daily.epoch)
    val dates = listOf("2026-12-31", "2027-01-04", "2027-01-05", "2027-03-14", "2027-03-15", "2027-03-28",
        "2027-10-31", "2027-11-07", "2028-02-29", "2028-03-01", "2029-01-04")
    val daily = buildJsonArray {
        for (d in dates) add(buildJsonObject {
            put("epoch", epoch.toString())
            put("date", d)
            put("dayIndex", ChronoUnit.DAYS.between(epoch, LocalDate.parse(d)))
        })
    }
    File(outDir, "daily_index.json").writeText(pretty.encodeToString(JsonArray.serializer(), daily))
    println("test vectors → $outDir (${lines.size} lines, ${boards.size} boards, ${daily.size} dates)")
}
