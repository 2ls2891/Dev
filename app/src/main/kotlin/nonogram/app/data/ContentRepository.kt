package nonogram.app.data

import android.content.res.AssetManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import nonogram.core.format.ContentJson
import nonogram.core.format.DailyOut
import nonogram.core.format.FORMAT_VERSION
import nonogram.core.format.IndexOut
import nonogram.core.format.PackOut
import nonogram.core.format.PuzzleOut

/** 퍼즐이 어디서 왔는지. 팩이면 packId, 데일리면 [DAILY] */
const val DAILY = "daily"

data class PuzzleRef(
    val puzzle: PuzzleOut,
    val source: String,
    /** 팩 안 순서(0부터) 또는 데일리 dayIndex */
    val position: Int,
    /** 같은 팩의 다음 퍼즐 id (마지막이거나 데일리면 null) */
    val nextId: String?,
)

/**
 * 앱에 번들된 퍼즐 콘텐츠(assets = content/packs) 읽기. 한 번 읽은 파일은 메모리에 둔다.
 */
class ContentRepository(private val assets: AssetManager) {
    private val mutex = Mutex()
    private var index: IndexOut? = null
    private var daily: DailyOut? = null
    private val packs = HashMap<String, PackOut>()

    private suspend fun read(name: String): String = withContext(Dispatchers.IO) {
        assets.open(name).bufferedReader(Charsets.UTF_8).use { it.readText() }
    }

    suspend fun index(): IndexOut = mutex.withLock {
        index ?: ContentJson.decodeFromString(IndexOut.serializer(), read("index.json")).also {
            check(it.format <= FORMAT_VERSION) { "unsupported content format ${it.format}" }
            index = it
        }
    }

    suspend fun pack(id: String): PackOut {
        val file = index().packs.firstOrNull { it.id == id }?.file ?: error("unknown pack $id")
        return mutex.withLock {
            packs[id] ?: ContentJson.decodeFromString(PackOut.serializer(), read(file)).also { packs[id] = it }
        }
    }

    suspend fun daily(): DailyOut {
        val file = index().daily.file
        return mutex.withLock {
            daily ?: ContentJson.decodeFromString(DailyOut.serializer(), read(file)).also { daily = it }
        }
    }

    suspend fun puzzle(source: String, puzzleId: String): PuzzleRef {
        if (source == DAILY) {
            val d = daily()
            val pos = d.puzzles.indexOfFirst { it.id == puzzleId }
            require(pos >= 0) { "unknown daily puzzle $puzzleId" }
            return PuzzleRef(d.puzzles[pos], DAILY, pos, null)
        }
        val p = pack(source)
        val pos = p.puzzles.indexOfFirst { it.id == puzzleId }
        require(pos >= 0) { "unknown puzzle $puzzleId in $source" }
        return PuzzleRef(p.puzzles[pos], source, pos, p.puzzles.getOrNull(pos + 1)?.id)
    }
}
