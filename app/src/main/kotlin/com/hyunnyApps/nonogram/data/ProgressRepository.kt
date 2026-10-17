package com.hyunnyApps.nonogram.data

import android.util.AtomicFile
import android.util.Log
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import java.io.File

/** 퍼즐 한 판의 저장 데이터 */
@Serializable
data class PuzzleProgress(
    val w: Int,
    val h: Int,
    /** MarkCodec 형식 */
    val marks: String,
    val elapsedMs: Long = 0,
    val hintsUsed: Int = 0,
    val completed: Boolean = false,
    val completedAtMs: Long? = null,
)

@Serializable
data class PuzzleStatus(
    /** 팩 id 또는 "daily" (팩별 진행률 집계용) */
    val source: String,
    val completed: Boolean,
)

/**
 * 퍼즐 진행 저장소. 퍼즐마다 파일 하나(progress/<id>.json), 목록 화면용 요약(progress/summary.json).
 * 모든 쓰기는 AtomicFile 로 한다 → 쓰는 중 앱이 죽어도 이전 내용이 남는다.
 */
class ProgressRepository(rootDir: File, scope: CoroutineScope) {
    private val dir = File(rootDir, "progress").apply { mkdirs() }
    private val summaryFile = AtomicFile(File(dir, "summary.json"))
    private val mutex = Mutex()
    private val json = Json { ignoreUnknownKeys = true }
    private val summarySerializer = MapSerializer(String.serializer(), PuzzleStatus.serializer())

    private val _statuses = MutableStateFlow<Map<String, PuzzleStatus>>(emptyMap())
    /** puzzleId → 상태 (시작한 적 있는 퍼즐만) */
    val statuses: StateFlow<Map<String, PuzzleStatus>> = _statuses.asStateFlow()

    /** 요약 파일 로드 완료 신호. 로드 전에 요약을 덮어쓰면 기록이 사라지므로 쓰기는 반드시 이걸 기다린다 */
    private val summaryLoaded = CompletableDeferred<Unit>()

    init {
        scope.launch(Dispatchers.IO) {
            try {
                val loaded = mutex.withLock { readOrNull(summaryFile) { json.decodeFromString(summarySerializer, it) } }
                if (loaded != null) _statuses.value = loaded
            } finally {
                summaryLoaded.complete(Unit)
            }
        }
    }

    private fun fileOf(id: String) = AtomicFile(File(dir, "${id.replace(Regex("[^A-Za-z0-9_.-]"), "_")}.json"))

    suspend fun load(id: String): PuzzleProgress? = withContext(Dispatchers.IO) {
        mutex.withLock { readOrNull(fileOf(id)) { json.decodeFromString(PuzzleProgress.serializer(), it) } }
    }

    suspend fun save(id: String, source: String, progress: PuzzleProgress) = withContext(Dispatchers.IO) {
        summaryLoaded.await()
        mutex.withLock {
            write(fileOf(id), json.encodeToString(PuzzleProgress.serializer(), progress))
            val status = PuzzleStatus(source, progress.completed)
            if (_statuses.value[id] != status) {
                _statuses.update { it + (id to status) }
                write(summaryFile, json.encodeToString(summarySerializer, _statuses.value))
            }
        }
    }

    suspend fun delete(id: String) = withContext(Dispatchers.IO) {
        summaryLoaded.await()
        mutex.withLock {
            fileOf(id).delete()
            if (id in _statuses.value) {
                _statuses.update { it - id }
                write(summaryFile, json.encodeToString(summarySerializer, _statuses.value))
            }
        }
    }

    private fun <T> readOrNull(file: AtomicFile, parse: (String) -> T): T? =
        try {
            if (!file.baseFile.exists()) null else parse(file.readFully().toString(Charsets.UTF_8))
        } catch (e: Exception) {
            // 손상된 파일은 무시하고 새로 시작 (앱이 죽는 것보다 낫다)
            Log.w("Progress", "corrupted ${file.baseFile.name}", e)
            null
        }

    private fun write(file: AtomicFile, text: String) {
        val out = file.startWrite()
        try {
            out.write(text.toByteArray(Charsets.UTF_8))
            file.finishWrite(out)
        } catch (e: Exception) {
            file.failWrite(out)
            throw e
        }
    }
}
