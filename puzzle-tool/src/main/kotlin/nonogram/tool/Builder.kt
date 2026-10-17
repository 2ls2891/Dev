package nonogram.tool

import kotlinx.serialization.json.Json
import nonogram.core.BoardSolver
import nonogram.core.DifficultyRater
import nonogram.core.Grid
import nonogram.core.Outcome
import nonogram.core.SolveResult
import java.io.File
import java.time.DayOfWeek
import java.time.LocalDate
import kotlin.math.abs
import kotlin.math.ceil

class BuildConfig(
    val candidates: File,
    val outDir: File,
    /** true면 기존 결과를 무시하고 처음부터 만든다. 출시 후에는 절대 쓰지 말 것 */
    val fresh: Boolean,
    val epoch: LocalDate = LocalDate.of(2027, 1, 4),
    /** 흑백 퍼즐 중 데일리 전용으로 떼어둘 비율(%) */
    val dailyShare: Int = 30,
    val volumeSize: Int = 40,
    val probeBudget: Int = 4_000,
)

private data class Theme(val id: String, val title: Localized)

private val THEMES = mapOf(
    "Smileys & Emotion" to Theme("faces", Localized("표정", "Faces")),
    "People & Body" to Theme("people", Localized("사람", "People")),
    "Animals & Nature" to Theme("nature", Localized("동물과 자연", "Animals & Nature")),
    "Food & Drink" to Theme("food", Localized("음식", "Food & Drink")),
    "Travel & Places" to Theme("travel", Localized("여행과 장소", "Travel & Places")),
    "Activities" to Theme("activities", Localized("활동", "Activities")),
    "Objects" to Theme("objects", Localized("물건", "Objects")),
    "Symbols" to Theme("symbols", Localized("기호", "Symbols")),
)

private val KIND_TITLE = mapOf("mono" to Localized("", ""), "color" to Localized("컬러 ", "Color "))

/** 크기 선택 분포 (긴 변 → 가중치). 같은 그림이 여러 크기로 중복 수록되지 않도록 그림마다 한 크기만 고른다 */
private val SIZE_WEIGHTS = mapOf(
    "mono" to linkedMapOf(10 to 10, 15 to 30, 20 to 30, 25 to 20, 30 to 10),
    "color" to linkedMapOf(15 to 35, 20 to 40, 25 to 25),
)

/** 요일(월~일)별 데일리 목표 레벨 */
private val WEEKDAY_LEVELS = listOf(1, 2, 2, 3, 3, 4, 5)

private val SOURCES = listOf(
    SourceInfo(
        "fluent-emoji", "Microsoft Fluent Emoji", "MIT",
        "https://github.com/microsoft/fluentui-emoji", "1ffb34c752ecf5d402f04cfb4b392c77f57c54bc",
    ),
    SourceInfo(
        "cldr", "Unicode CLDR (한국어 이모지 이름)", "Unicode-3.0",
        "https://github.com/unicode-org/cldr-json", "48.0.0",
    ),
)

private class Accepted(val candidate: Candidate, val grid: Grid, val result: SolveResult) {
    val difficulty = DifficultyRater.rate(grid, result)
}

private sealed interface Selection {
    data class Ok(val accepted: Accepted) : Selection
    data class Rejected(val key: String, val kind: String, val reason: String) : Selection
}

val json = Json { prettyPrint = false; ignoreUnknownKeys = true; encodeDefaults = true }
private val prettyJson = Json { prettyPrint = true; encodeDefaults = true }

/** 플랫폼 독립적인 64비트 FNV-1a. 배정(데일리/크기)이 실행마다 바뀌지 않도록 사용 */
fun stableHash(s: String): Long {
    var h = -0x340d631b7bdddcdbL
    for (b in s.toByteArray(Charsets.UTF_8)) {
        h = h xor (b.toLong() and 0xff)
        h *= 0x100000001b3L
    }
    return h
}

private fun bucket(s: String, mod: Int): Int = Math.floorMod(stableHash(s), mod.toLong()).toInt()

private fun safeKey(key: String): String = key.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_')

private fun hexDistance(a: String, b: String): Double {
    val x = a.removePrefix("#").toInt(16)
    val y = b.removePrefix("#").toInt(16)
    val dr = (x shr 16 and 0xff) - (y shr 16 and 0xff)
    val dg = (x shr 8 and 0xff) - (y shr 8 and 0xff)
    val db = (x and 0xff) - (y and 0xff)
    return Math.sqrt((dr * dr + dg * dg + db * db).toDouble())
}

/** 풀이 전에 걸러내는 품질 기준. null이면 통과, 아니면 탈락 사유 */
private fun prefilter(c: Candidate): String? = when (c.kind) {
    "mono" -> when {
        c.fill < 0.15 || c.fill > 0.75 -> "fill"
        (c.fidelity ?: 0.0) < (if (c.size <= 10) 0.72 else 0.60) -> "fidelity"
        else -> null
    }
    "color" -> when {
        c.size < 15 -> "size"
        c.palette.size < 2 -> "colors"
        c.fill < 0.25 -> "fill"
        c.palette.indices.any { i -> (i + 1 until c.palette.size).any { j -> hexDistance(c.palette[i], c.palette[j]) < 48 } } -> "similar-colors"
        else -> null
    }
    else -> "kind"
}

private fun select(group: List<Candidate>, probeBudget: Int): Selection {
    val first = group.first()
    val weights = SIZE_WEIGHTS.getValue(first.kind)
    val passed = group.filter { it.size in weights && prefilter(it) == null }
    if (passed.isEmpty()) {
        val reason = group.mapNotNull { prefilter(it) }.groupingBy { it }.eachCount().maxByOrNull { it.value }?.key ?: "size"
        return Selection.Rejected(first.key, first.kind, reason)
    }
    // 그림마다 목표 크기를 가중치에 따라 고정 배정하고, 가능한 크기 중 가장 가까운 것부터 시도
    var roll = bucket("${first.key}/${first.kind}/size", weights.values.sum())
    var target = weights.keys.last()
    for ((size, weight) in weights) {
        if (roll < weight) { target = size; break }
        roll -= weight
    }
    for (c in passed.sortedWith(compareBy({ abs(it.size - target) }, { it.size }))) {
        val grid = Grid.fromRows(c.rows)
        val result = BoardSolver(grid).solve(useProbing = true, probeBudget = probeBudget)
        if (result.outcome == Outcome.SOLVED) return Selection.Ok(Accepted(c, grid, result))
    }
    return Selection.Rejected(first.key, first.kind, "not-unique")
}

private fun Accepted.toPuzzle(): PuzzleOut {
    val c = candidate
    val k = safeKey(c.key)
    return PuzzleOut(
        id = "fe-$k-${c.kind[0]}${c.size}",
        title = Localized(c.name_ko ?: c.name, c.name.replaceFirstChar { it.uppercase() }),
        kind = c.kind,
        w = c.w, h = c.h,
        palette = c.palette,
        rows = c.rows,
        reveal = c.reveal,
        difficulty = DifficultyOut(
            difficulty.level,
            Math.round(difficulty.score * 10) / 10.0,
            difficulty.technique.name.lowercase(),
        ),
        source = c.source,
        sourceKey = k,
    )
}

class BuildReport(val lines: List<String>) {
    override fun toString() = lines.joinToString("\n")
}

fun build(cfg: BuildConfig): BuildReport {
    require(cfg.epoch.dayOfWeek == DayOfWeek.MONDAY) { "epoch must be a Monday (weekday levels start on Monday)" }
    val started = System.currentTimeMillis()
    val candidates = json.decodeFromString<List<Candidate>>(cfg.candidates.readText())
    cfg.outDir.mkdirs()

    // 1) 기존 결과 로드: 이미 수록된 퍼즐은 그대로 두고, 새 그림만 추가한다 (사용자 진행 데이터 보호)
    val indexFile = File(cfg.outDir, "index.json")
    val existingPacks = mutableListOf<PackOut>()
    var existingDaily: DailyOut? = null
    if (!cfg.fresh && indexFile.exists()) {
        val index = json.decodeFromString<IndexOut>(indexFile.readText())
        index.packs.forEach { existingPacks += json.decodeFromString<PackOut>(File(cfg.outDir, it.file).readText()) }
        existingDaily = json.decodeFromString<DailyOut>(File(cfg.outDir, index.daily.file).readText())
        require(existingDaily.epoch == cfg.epoch.toString()) { "daily epoch cannot change after release" }
    }
    val used = (existingPacks.flatMap { it.puzzles } + (existingDaily?.puzzles ?: emptyList()))
        .map { "${it.sourceKey}/${it.kind}" }.toSet()

    // 2) 그림(key)·종류(kind)별로 묶어 검증/선택 (병렬)
    val groups = candidates.groupBy { "${safeKey(it.key)}/${it.kind}" }.filterKeys { it !in used }.values.toList()
    val selections = groups.parallelStream().map { select(it, cfg.probeBudget) }.toList()
    val accepted = selections.filterIsInstance<Selection.Ok>().map { it.accepted }
    val rejected = selections.filterIsInstance<Selection.Rejected>()

    // 3) 흑백 일부는 데일리 전용 풀로
    val (dailyPool, packPool) = accepted.partition { it.candidate.kind == "mono" && bucket("${it.candidate.key}/daily", 100) < cfg.dailyShare }

    // 4) 테마별 볼륨 구성: 점수순으로 정렬한 뒤 볼륨에 번갈아 배분 → 각 볼륨이 쉬움→어려움으로 고르게 구성됨
    val newPacks = mutableListOf<PackOut>()
    packPool.groupBy { it.candidate.kind to (THEMES[it.candidate.group] ?: error("unknown group ${it.candidate.group}")) }
        .toSortedMap(compareBy({ it.first }, { it.second.id }))
        .forEach { (kt, items) ->
            val (kind, theme) = kt
            val sorted = items.sortedWith(compareBy({ it.difficulty.score }, { it.candidate.key }))
            val volumes = ceil(sorted.size / cfg.volumeSize.toDouble()).toInt().coerceAtLeast(1)
            val firstVolume = (existingPacks.filter { it.kind == kind && it.theme == theme.id }.maxOfOrNull { it.volume } ?: 0) + 1
            for (v in 0 until volumes) {
                val puzzles = sorted.filterIndexed { i, _ -> i % volumes == v }.map { it.toPuzzle() }
                val no = firstVolume + v
                val prefix = KIND_TITLE.getValue(kind)
                newPacks += PackOut(
                    id = "$kind-${theme.id}-$no",
                    kind = kind,
                    theme = theme.id,
                    volume = no,
                    title = Localized("${prefix.ko}${theme.title.ko} $no", "${prefix.en}${theme.title.en} $no"),
                    puzzles = puzzles,
                )
            }
        }

    // 5) 데일리: 기존 일정 뒤에 새 퍼즐을 요일별 목표 난이도에 맞춰 이어 붙인다
    val schedule = existingDaily?.puzzles?.toMutableList() ?: mutableListOf()
    val pool = dailyPool.sortedBy { stableHash("${it.candidate.key}/order") }.map { it.toPuzzle() }.toMutableList()
    while (pool.isNotEmpty()) {
        val want = WEEKDAY_LEVELS[schedule.size % 7]
        val pick = pool.minWith(compareBy({ abs(it.difficulty.level - want) }, { if (it.difficulty.level <= want) 0 else 1 }))
        pool.remove(pick)
        schedule += pick
    }
    val daily = DailyOut(epoch = cfg.epoch.toString(), weekdayLevels = WEEKDAY_LEVELS, puzzles = schedule)

    // 6) 쓰기
    val allPacks = existingPacks + newPacks
    allPacks.forEach { File(cfg.outDir, "${it.id}.json").writeText(json.encodeToString(PackOut.serializer(), it)) }
    File(cfg.outDir, "daily.json").writeText(json.encodeToString(DailyOut.serializer(), daily))
    val index = IndexOut(
        packs = allPacks.map { p ->
            PackSummary(
                p.id, p.kind, p.theme, p.volume, p.title, "${p.id}.json", p.puzzles.size,
                (1..5).map { l -> p.puzzles.count { it.difficulty.level == l } },
            )
        },
        daily = DailySummary("daily.json", daily.epoch, daily.puzzles.size),
        sources = SOURCES,
    )
    indexFile.writeText(prettyJson.encodeToString(IndexOut.serializer(), index))

    // 7) 리포트
    val all = allPacks.flatMap { it.puzzles } + daily.puzzles
    val r = mutableListOf<String>()
    r += "# 콘텐츠 빌드 리포트"
    r += ""
    r += "- 후보 그룹: ${groups.size} (기존 수록 제외), 채택 ${accepted.size}, 탈락 ${rejected.size}"
    r += "- 전체 수록: 팩 ${allPacks.size}개 / 퍼즐 ${allPacks.sumOf { it.puzzles.size }}개, 데일리 ${daily.puzzles.size}일"
    r += "- 소요: ${(System.currentTimeMillis() - started) / 1000}s"
    r += ""
    r += "## 종류 × 레벨"
    r += "| 종류 | L1 | L2 | L3 | L4 | L5 | 합계 |"
    r += "|---|---|---|---|---|---|---|"
    for (kind in listOf("mono", "color")) {
        val ps = all.filter { it.kind == kind }
        r += "| $kind | ${(1..5).joinToString(" | ") { l -> ps.count { it.difficulty.level == l }.toString() }} | ${ps.size} |"
    }
    r += ""
    r += "## 크기(긴 변) 분포"
    all.groupBy { "${it.kind} ${maxOf(it.w, it.h)}" }.toSortedMap().forEach { (k, v) -> r += "- $k: ${v.size}" }
    r += ""
    r += "## probing이 필요한 퍼즐: ${all.count { it.difficulty.technique == "probing" }}"
    r += ""
    r += "## 탈락 사유"
    rejected.groupBy { "${it.kind}: ${it.reason}" }.toSortedMap().forEach { (k, v) -> r += "- $k: ${v.size}" }
    r += ""
    r += "## 팩"
    allPacks.forEach { p -> r += "- ${p.id} (${p.title.ko}): ${p.puzzles.size}" }
    return BuildReport(r)
}
