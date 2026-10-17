package com.hyunnyApps.nonogram.ui.pack

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.hyunnyApps.nonogram.AppContainer
import com.hyunnyApps.nonogram.NonogramApplication
import com.hyunnyApps.nonogram.core.format.PackOut
import com.hyunnyApps.nonogram.core.format.PuzzleOut

data class PuzzleTile(val puzzle: PuzzleOut, val number: Int, val completed: Boolean, val started: Boolean)

data class PackUiState(
    val loading: Boolean = true,
    val error: Boolean = false,
    val pack: PackOut? = null,
    val tiles: List<PuzzleTile> = emptyList(),
    val completed: Int = 0,
)

class PackViewModel(savedState: SavedStateHandle, container: AppContainer) : ViewModel() {
    val packId: String = checkNotNull(savedState[ARG_PACK_ID])
    private val pack = MutableStateFlow<Result<PackOut>?>(null)

    val state: StateFlow<PackUiState> = combine(pack, container.progress.statuses) { result, statuses ->
        when {
            result == null -> PackUiState()
            result.isFailure -> PackUiState(loading = false, error = true)
            else -> {
                val p = result.getOrThrow()
                val tiles = p.puzzles.mapIndexed { i, puzzle ->
                    val st = statuses[puzzle.id]
                    PuzzleTile(puzzle, i + 1, st?.completed == true, st != null)
                }
                PackUiState(false, false, p, tiles, tiles.count { it.completed })
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PackUiState())

    init {
        viewModelScope.launch {
            pack.value = runCatching { container.content.pack(packId) }
                .onFailure { Log.e("PackViewModel", "pack load failed: $packId", it) }
        }
    }

    companion object {
        const val ARG_PACK_ID = "packId"

        val Factory = viewModelFactory {
            initializer {
                PackViewModel(createSavedStateHandle(), (this[APPLICATION_KEY] as NonogramApplication).container)
            }
        }
    }
}
