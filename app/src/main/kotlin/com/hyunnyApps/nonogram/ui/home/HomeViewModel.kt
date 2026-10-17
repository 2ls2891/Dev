package com.hyunnyApps.nonogram.ui.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.hyunnyApps.nonogram.AppContainer
import com.hyunnyApps.nonogram.NonogramApplication
import com.hyunnyApps.nonogram.core.Grid
import com.hyunnyApps.nonogram.core.daily.DailyCalendar
import com.hyunnyApps.nonogram.core.format.DailyOut
import com.hyunnyApps.nonogram.core.format.IndexOut
import com.hyunnyApps.nonogram.core.format.Localized
import com.hyunnyApps.nonogram.core.format.PackSummary
import com.hyunnyApps.nonogram.core.format.PuzzleOut
import com.hyunnyApps.nonogram.core.game.MarkCodec
import com.hyunnyApps.nonogram.data.DAILY
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class DailyItem(
    val puzzle: PuzzleOut,
    val date: LocalDate,
    val completed: Boolean,
    val inProgress: Boolean,
)

/** 홈의 "이어서 그리기" */
data class ContinueItem(
    val puzzle: PuzzleOut,
    val source: String,
    /** 팩 안 번호(1부터). 데일리면 0 */
    val number: Int,
    val packTitle: Localized?,
    /** 칠한 칸 / 정답 칸 (0~1) */
    val fraction: Float,
)

data class PackItem(val summary: PackSummary, val completed: Int)

data class ThemeSection(val theme: String, val title: Localized, val packs: List<PackItem>, val puzzleCount: Int)

data class HomeUiState(
    val loading: Boolean = true,
    val daily: DailyItem? = null,
    val continueItem: ContinueItem? = null,
    val streak: Int = 0,
    /** 어제 하루 놓쳐서 끊긴(지킬 수 있는) 연속 기록. 없으면 null */
    val streakLost: Int? = null,
    val hints: Int = 0,
    val completedTotal: Int = 0,
    val totalPuzzles: Int = 0,
    val tutorialSeen: Boolean = true,
    /** 첫 완성 이후 한 번 "매일 알림 받을까요?" 카드 */
    val showReminderCard: Boolean = false,
    val kind: String = KIND_MONO,
    val sections: List<ThemeSection> = emptyList(),
)

const val KIND_MONO = "mono"
const val KIND_COLOR = "color"

/** 테마 표시 순서 (첫 화면에서 친숙한 것부터) */
private val THEME_ORDER = listOf("nature", "food", "faces", "objects", "activities", "travel", "people", "symbols")

private data class PlayerSnapshot(
    val hints: Int,
    val streak: com.hyunnyApps.nonogram.core.daily.Streak,
    val tutorialSeen: Boolean,
    val reminderAsked: Boolean,
    val reminderOn: Boolean,
)

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(private val container: AppContainer) : ViewModel() {
    private val kind = MutableStateFlow(KIND_MONO)
    private val content = MutableStateFlow<Pair<IndexOut, DailyOut>?>(null)

    private val player = combine(
        container.player.hints,
        container.player.streak,
        container.player.tutorialSeen,
        container.player.reminderAsked,
        container.player.reminderHour,
    ) { h, s, t, asked, hour -> PlayerSnapshot(h, s, t, asked, hour != null) }

    /** 마지막으로 연 퍼즐 → 아직 안 끝났으면 이어하기 카드 */
    private val continueItem = combine(container.player.lastPlayed, container.progress.statuses) { last, statuses -> last to statuses }
        .mapLatest { (last, statuses) ->
            val (source, id) = last ?: return@mapLatest null
            val status = statuses[id] ?: return@mapLatest null
            if (status.completed) return@mapLatest null
            runCatching {
                val ref = container.content.puzzle(source, id)
                val grid = Grid.fromRows(ref.puzzle.rows)
                val saved = container.progress.load(id)
                val marks = saved?.let { MarkCodec.decode(it.marks, grid.width * grid.height, maxOf(1, grid.colorCount)) }
                val target = grid.filledCount().coerceAtLeast(1)
                val filled = marks?.indices?.count { marks[it] > 0 && marks[it] == grid.cells[it] } ?: 0
                val packTitle = if (source == DAILY) null else container.content.index().packs.firstOrNull { it.id == source }?.title
                ContinueItem(ref.puzzle, source, if (source == DAILY) 0 else ref.position + 1, packTitle, filled.toFloat() / target)
            }.getOrNull()
        }

    val state: StateFlow<HomeUiState> = combine(content, kind, container.progress.statuses, player, continueItem) { loaded, kind, statuses, p, cont ->
        if (loaded == null) return@combine HomeUiState(loading = true, kind = kind)
        val (index, daily) = loaded
        val today = container.today()
        val todayIndex = DailyCalendar.dayIndex(LocalDate.parse(daily.epoch), today)
        val dailyPuzzle = daily.puzzles[DailyCalendar.puzzlePosition(todayIndex, daily.puzzles.size)]
        val dailyStatus = statuses[dailyPuzzle.id]
        val completedByPack = statuses.values.filter { it.completed }.groupingBy { it.source }.eachCount()
        val completedTotal = statuses.values.count { it.completed }
        val sections = index.packs
            .filter { it.kind == kind }
            .groupBy { it.theme }
            .toList()
            .sortedBy { (theme, _) -> THEME_ORDER.indexOf(theme).let { i -> if (i < 0) Int.MAX_VALUE else i } }
            .map { (theme, packs) ->
                ThemeSection(
                    theme = theme,
                    title = packs.first().themeTitle,
                    packs = packs.sortedBy { it.volume }.map { PackItem(it, completedByPack[it.id] ?: 0) },
                    puzzleCount = packs.sumOf { it.count },
                )
            }
        HomeUiState(
            loading = false,
            daily = DailyItem(dailyPuzzle, today, dailyStatus?.completed == true, dailyStatus != null && !dailyStatus.completed),
            // 데일리는 위 카드에서 이어할 수 있으므로 이어하기 카드에서 뺀다
            continueItem = cont?.takeIf { it.puzzle.id != dailyPuzzle.id },
            streak = p.streak.displayed(todayIndex),
            streakLost = if (p.streak.canRestore(todayIndex)) p.streak.current else null,
            hints = p.hints,
            completedTotal = completedTotal,
            totalPuzzles = index.packs.sumOf { it.count } + index.daily.days,
            tutorialSeen = p.tutorialSeen,
            showReminderCard = completedTotal >= 1 && !p.reminderAsked && !p.reminderOn,
            kind = kind,
            sections = sections,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    init {
        viewModelScope.launch {
            try {
                content.value = container.content.index() to container.content.daily()
            } catch (e: Exception) {
                Log.e("HomeViewModel", "content load failed", e)
            }
        }
        viewModelScope.launch { container.player.refillHints(container.today().toEpochDay()) }
    }

    fun selectKind(k: String) {
        kind.value = k
    }

    /** 보상형 광고 시청 완료 후 호출 */
    fun restoreStreak() {
        val daily = content.value?.second ?: return
        val todayIndex = DailyCalendar.dayIndex(LocalDate.parse(daily.epoch), container.today())
        container.appScope.launch { container.player.restoreStreak(todayIndex) }
    }

    fun dismissReminderCard() {
        viewModelScope.launch { container.player.markReminderAsked() }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { HomeViewModel((this[APPLICATION_KEY] as NonogramApplication).container) }
        }
    }
}
