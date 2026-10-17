package nonogram.app.ui.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import nonogram.app.AppContainer
import nonogram.app.NonogramApplication
import nonogram.core.daily.DailyCalendar
import nonogram.core.format.DailyOut
import nonogram.core.format.IndexOut
import nonogram.core.format.Localized
import nonogram.core.format.PackSummary
import nonogram.core.format.PuzzleOut
import java.time.LocalDate

data class DailyItem(
    val puzzle: PuzzleOut,
    val date: LocalDate,
    val completed: Boolean,
    val inProgress: Boolean,
)

data class PackItem(val summary: PackSummary, val completed: Int)

data class ThemeSection(val theme: String, val title: Localized, val packs: List<PackItem>)

data class HomeUiState(
    val loading: Boolean = true,
    val daily: DailyItem? = null,
    val streak: Int = 0,
    val bestStreak: Int = 0,
    val hints: Int = 0,
    val kind: String = KIND_MONO,
    val sections: List<ThemeSection> = emptyList(),
)

const val KIND_MONO = "mono"
const val KIND_COLOR = "color"

/** 테마 표시 순서 (첫 화면에서 친숙한 것부터) */
private val THEME_ORDER = listOf("nature", "food", "faces", "objects", "activities", "travel", "people", "symbols")

class HomeViewModel(private val container: AppContainer) : ViewModel() {
    private val kind = MutableStateFlow(KIND_MONO)
    private val content = MutableStateFlow<Pair<IndexOut, DailyOut>?>(null)

    val state: StateFlow<HomeUiState> = combine(
        content,
        kind,
        container.progress.statuses,
        container.player.hints,
        container.player.streak,
    ) { loaded, kind, statuses, hints, streak ->
        if (loaded == null) return@combine HomeUiState(loading = true, kind = kind)
        val (index, daily) = loaded
        val today = container.today()
        val todayIndex = DailyCalendar.dayIndex(LocalDate.parse(daily.epoch), today)
        val dailyPuzzle = daily.puzzles[DailyCalendar.puzzlePosition(todayIndex, daily.puzzles.size)]
        val dailyStatus = statuses[dailyPuzzle.id]
        val completedByPack = statuses.values.filter { it.completed }.groupingBy { it.source }.eachCount()
        val sections = index.packs
            .filter { it.kind == kind }
            .groupBy { it.theme }
            .toList()
            .sortedBy { (theme, _) -> THEME_ORDER.indexOf(theme).let { i -> if (i < 0) Int.MAX_VALUE else i } }
            .map { (theme, packs) ->
                ThemeSection(
                    theme,
                    packs.first().themeTitle,
                    packs.sortedBy { it.volume }.map { PackItem(it, completedByPack[it.id] ?: 0) },
                )
            }
        HomeUiState(
            loading = false,
            daily = DailyItem(dailyPuzzle, today, dailyStatus?.completed == true, dailyStatus != null && !dailyStatus.completed),
            streak = streak.displayed(todayIndex),
            bestStreak = streak.best,
            hints = hints,
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

    companion object {
        val Factory = viewModelFactory {
            initializer { HomeViewModel((this[APPLICATION_KEY] as NonogramApplication).container) }
        }
    }
}
