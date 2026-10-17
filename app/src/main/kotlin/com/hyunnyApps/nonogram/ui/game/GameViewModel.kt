package com.hyunnyApps.nonogram.ui.game

import android.os.SystemClock
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import com.hyunnyApps.nonogram.AppContainer
import com.hyunnyApps.nonogram.NonogramApplication
import com.hyunnyApps.nonogram.ads.RewardedState
import com.hyunnyApps.nonogram.data.DAILY
import com.hyunnyApps.nonogram.data.PlayerRepository
import com.hyunnyApps.nonogram.data.PuzzleProgress
import com.hyunnyApps.nonogram.data.PuzzleRef
import com.hyunnyApps.nonogram.core.Clue
import com.hyunnyApps.nonogram.core.Grid
import com.hyunnyApps.nonogram.core.daily.DailyCalendar
import com.hyunnyApps.nonogram.core.format.PuzzleOut
import com.hyunnyApps.nonogram.core.game.GameSession
import com.hyunnyApps.nonogram.core.game.Hint
import com.hyunnyApps.nonogram.core.game.Mark
import com.hyunnyApps.nonogram.core.game.MarkCodec
import java.time.LocalDate

/** 보드 그리기에 필요한 스냅샷. version 이 같으면 내용도 같다 */
class BoardUi(
    val marks: IntArray,
    val version: Long,
    val rowDone: BooleanArray,
    val colDone: BooleanArray,
) {
    override fun equals(other: Any?) = other is BoardUi && version == other.version
    override fun hashCode() = version.hashCode()
}

data class GameUiState(
    val loading: Boolean = true,
    val error: Boolean = false,
    val puzzle: PuzzleOut? = null,
    val source: String = "",
    /** 팩 안 번호(0부터) */
    val position: Int = 0,
    val dailyDate: LocalDate? = null,
    val rowClues: List<Clue> = emptyList(),
    val colClues: List<Clue> = emptyList(),
    val board: BoardUi? = null,
    /** 현재 도구: 색 번호(1..C) 또는 Mark.CROSS */
    val tool: Int = 1,
    val colorCount: Int = 1,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val hints: Int = 0,
    val hintsUsed: Int = 0,
    val lastHint: Hint? = null,
    /** 드래그 중인 칸 (해당 행·열 강조용). 없으면 -1 */
    val activeCell: Int = -1,
    /** 아직 한 퍼즐도 완성하지 않은 사용자 → 안내 문구 표시 */
    val firstTimer: Boolean = false,
    val rewardOfferVisible: Boolean = false,
    val rewardedReady: Boolean = false,
    val elapsedMs: Long = 0,
    val completed: Boolean = false,
    /** 이번에 막 완성했는지 (완성 연출은 이때만 재생, 다시 열면 바로 결과 화면) */
    val justCompleted: Boolean = false,
    val nextId: String? = null,
)

sealed interface GameMessage {
    data class HintsGranted(val count: Int) : GameMessage
    data object AdNotReady : GameMessage
}

/**
 * 게임 화면 ViewModel.
 * 규칙은 전부 core 의 GameSession 이 담당하고, 여기서는 상태 노출·저장·타이머·힌트/광고 흐름만 조율한다.
 */
class GameViewModel(
    savedState: SavedStateHandle,
    private val container: AppContainer,
    private val now: () -> Long = SystemClock::elapsedRealtime,
) : ViewModel() {
    private val source: String = checkNotNull(savedState[ARG_SOURCE])
    private val puzzleId: String = checkNotNull(savedState[ARG_PUZZLE_ID])

    private val _state = MutableStateFlow(GameUiState(source = source))
    val state: StateFlow<GameUiState> = _state.asStateFlow()

    private val _message = MutableStateFlow<GameMessage?>(null)
    /** 한 번 보여주고 consumeMessage() 로 지운다 */
    val message: StateFlow<GameMessage?> = _message.asStateFlow()

    private var session: GameSession? = null
    private var ref: PuzzleRef? = null

    // 타이머: 화면이 보이는 동안만 흐른다
    private var resumed = false
    private var accumulatedMs = 0L
    private var runningSince: Long? = null
    private var tickerJob: Job? = null

    // 저장
    private var saveJob: Job? = null
    private var savedVersion = 0L
    private var hasSavedProgress = false
    private var completedAtMs: Long? = null

    init {
        viewModelScope.launch { load() }
        viewModelScope.launch { container.player.hints.collect { n -> _state.update { it.copy(hints = n) } } }
        viewModelScope.launch {
            container.progress.statuses.collect { st -> _state.update { it.copy(firstTimer = st.values.none { s -> s.completed }) } }
        }
        viewModelScope.launch {
            container.rewarded.state.collect { s -> _state.update { it.copy(rewardedReady = s == RewardedState.READY) } }
        }
    }

    private suspend fun load() {
        try {
            val r = container.content.puzzle(source, puzzleId)
            val grid = Grid.fromRows(r.puzzle.rows)
            val saved = container.progress.load(puzzleId)?.takeIf { it.w == grid.width && it.h == grid.height }
            val marks = saved?.let { MarkCodec.decode(it.marks, grid.width * grid.height, maxOf(1, grid.colorCount)) }
            val s = GameSession(grid, marks)
            ref = r
            session = s
            accumulatedMs = saved?.elapsedMs ?: 0
            hasSavedProgress = saved != null
            completedAtMs = saved?.completedAtMs
            savedVersion = s.version
            val dailyDate = if (source == DAILY) dailyDateOf(r.position) else null
            _state.update {
                it.copy(
                    loading = false,
                    puzzle = r.puzzle,
                    position = r.position,
                    dailyDate = dailyDate,
                    rowClues = s.rowClues,
                    colClues = s.colClues,
                    colorCount = s.colorCount,
                    hintsUsed = saved?.hintsUsed ?: 0,
                    completed = saved?.completed == true,
                    elapsedMs = accumulatedMs,
                    nextId = r.nextId,
                )
            }
            publishBoard()
            if (resumed) startTimer()
        } catch (e: Exception) {
            Log.e(TAG, "load failed: $source/$puzzleId", e)
            _state.update { it.copy(loading = false, error = true) }
        }
    }

    /** 데일리 일정 위치 → 화면에 보여줄 날짜 (오늘 일정이면 오늘) */
    private suspend fun dailyDateOf(position: Int): LocalDate {
        val daily = container.content.daily()
        val today = container.today()
        val epoch = LocalDate.parse(daily.epoch)
        val todayIndex = DailyCalendar.dayIndex(epoch, today)
        return if (DailyCalendar.puzzlePosition(todayIndex, daily.puzzles.size) == position) today
        else epoch.plusDays(position.toLong())
    }

    private fun publishBoard() {
        val s = session ?: return
        _state.update {
            it.copy(
                board = BoardUi(
                    s.marksSnapshot(),
                    s.version,
                    BooleanArray(s.height) { r -> s.isRowSatisfied(r) },
                    BooleanArray(s.width) { c -> s.isColSatisfied(c) },
                ),
                canUndo = s.canUndo,
                canRedo = s.canRedo,
            )
        }
    }

    // ---- 입력 ----

    private fun editable(): GameSession? = session?.takeIf { !_state.value.completed }

    fun selectTool(tool: Int) {
        val s = session ?: return
        if (tool == Mark.CROSS || tool in 1..s.colorCount) _state.update { it.copy(tool = tool) }
    }

    fun onStrokeStart(index: Int) {
        val s = editable() ?: return
        s.beginStroke(index, _state.value.tool)
        _state.update { it.copy(activeCell = index) }
        publishBoard()
    }

    fun onStrokeMove(index: Int) {
        val s = editable() ?: return
        if (!s.isStroking) return
        val before = s.version
        s.extendStroke(index)
        if (_state.value.activeCell != index) _state.update { it.copy(activeCell = index) }
        if (s.version != before) publishBoard()
    }

    fun onStrokeEnd() {
        val s = editable() ?: return
        _state.update { it.copy(activeCell = -1) }
        if (s.endStroke()) afterChange() else publishBoard()
    }

    fun onStrokeCancel() {
        val s = session ?: return
        s.cancelStroke()
        _state.update { it.copy(activeCell = -1) }
        publishBoard()
    }

    fun undo() {
        if (editable()?.undo() == true) afterChange()
    }

    fun redo() {
        if (editable()?.redo() == true) afterChange()
    }

    private fun afterChange() {
        _state.update { it.copy(lastHint = null) }
        publishBoard()
        checkCompleted()
        scheduleSave()
    }

    // ---- 힌트 / 보상형 광고 ----

    fun requestHint() {
        val s = editable() ?: return
        viewModelScope.launch {
            if (!container.player.consumeHint()) {
                _state.update { it.copy(rewardOfferVisible = true) }
                return@launch
            }
            val hint = s.findHint() ?: return@launch
            s.setMark(hint.index, hint.mark)
            _state.update { it.copy(lastHint = hint, hintsUsed = it.hintsUsed + 1) }
            publishBoard()
            checkCompleted()
            scheduleSave()
        }
    }

    fun dismissRewardOffer() = _state.update { it.copy(rewardOfferVisible = false) }

    /** 광고 시청 완료 콜백 (화면에서 RewardedAdManager.show 의 onRewarded 로 연결) */
    fun onRewardEarned() {
        container.appScope.launch { container.player.addHints(PlayerRepository.REWARDED_HINTS) }
        _state.update { it.copy(rewardOfferVisible = false) }
        _message.value = GameMessage.HintsGranted(PlayerRepository.REWARDED_HINTS)
    }

    fun onAdNotReady() {
        _message.value = GameMessage.AdNotReady
    }

    fun consumeMessage() {
        _message.value = null
    }

    // ---- 완료 ----

    private fun checkCompleted() {
        val s = session ?: return
        if (_state.value.completed || !s.isSolved()) return
        stopTimer()
        completedAtMs = System.currentTimeMillis()
        _state.update { it.copy(completed = true, justCompleted = true, lastHint = null, activeCell = -1, elapsedMs = accumulatedMs) }
        val r = ref ?: return
        if (r.source == DAILY) {
            container.appScope.launch {
                val daily = container.content.daily()
                val todayIndex = DailyCalendar.dayIndex(LocalDate.parse(daily.epoch), container.today())
                // 오늘의 데일리를 푼 경우에만 연속 기록에 반영
                if (DailyCalendar.puzzlePosition(todayIndex, daily.puzzles.size) == r.position) {
                    container.player.onDailyCleared(todayIndex)
                }
            }
        }
        saveNow()
    }

    fun restart() {
        val r = ref ?: return
        val fresh = GameSession(Grid.fromRows(r.puzzle.rows))
        session = fresh
        accumulatedMs = 0
        savedVersion = fresh.version
        completedAtMs = null
        _state.update { it.copy(completed = false, justCompleted = false, elapsedMs = 0, hintsUsed = 0, lastHint = null) }
        publishBoard()
        if (resumed) startTimer()
        container.appScope.launch { container.progress.delete(puzzleId) }
        hasSavedProgress = false
    }

    // ---- 생명주기 / 타이머 ----

    fun onResume() {
        resumed = true
        startTimer()
    }

    fun onPause() {
        resumed = false
        stopTimer()
        saveNow()
    }

    private fun startTimer() {
        if (session == null || _state.value.completed || runningSince != null) return
        runningSince = now()
        tickerJob = viewModelScope.launch {
            while (isActive) {
                _state.update { it.copy(elapsedMs = currentElapsed()) }
                delay(1000)
            }
        }
    }

    private fun stopTimer() {
        runningSince?.let { accumulatedMs += now() - it }
        runningSince = null
        tickerJob?.cancel()
        tickerJob = null
    }

    private fun currentElapsed() = accumulatedMs + (runningSince?.let { now() - it } ?: 0)

    // ---- 저장 ----

    private fun scheduleSave() {
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            delay(400)
            saveNow()
        }
    }

    /** 앱 범위 코루틴에서 저장 → 화면이 닫혀도 끝까지 쓴다 */
    private fun saveNow() {
        val s = session ?: return
        val st = _state.value
        // 아무 것도 안 하고 나간 퍼즐은 파일을 만들지 않는다
        if (s.version == savedVersion && !hasSavedProgress) return
        savedVersion = s.version
        hasSavedProgress = true
        val progress = PuzzleProgress(
            w = s.width,
            h = s.height,
            marks = MarkCodec.encode(s.marksSnapshot()),
            elapsedMs = currentElapsed(),
            hintsUsed = st.hintsUsed,
            completed = st.completed,
            completedAtMs = completedAtMs,
        )
        container.appScope.launch { container.progress.save(puzzleId, source, progress) }
    }

    override fun onCleared() {
        stopTimer()
        saveNow()
    }

    companion object {
        const val ARG_SOURCE = "source"
        const val ARG_PUZZLE_ID = "puzzleId"
        private const val TAG = "GameViewModel"

        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as NonogramApplication
                GameViewModel(createSavedStateHandle(), app.container)
            }
        }
    }
}
