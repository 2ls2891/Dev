package com.hyunnyApps.nonogram.ui.gallery

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.hyunnyApps.nonogram.AppContainer
import com.hyunnyApps.nonogram.NonogramApplication
import com.hyunnyApps.nonogram.core.format.Localized
import com.hyunnyApps.nonogram.core.format.PuzzleOut
import com.hyunnyApps.nonogram.data.DAILY
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn

data class GalleryItem(val puzzle: PuzzleOut, val source: String)

/** title 이 null 이면 데일리 묶음 (화면에서 문자열 리소스로 표시) */
data class GallerySection(val source: String, val title: Localized?, val items: List<GalleryItem>)

data class GalleryUiState(
    val loading: Boolean = true,
    val sections: List<GallerySection> = emptyList(),
    val collected: Int = 0,
    val total: Int = 0,
)

/** 완성한 퍼즐의 컬러 그림 모음. 진행 요약(statuses)이 바뀌면 자동으로 다시 만든다 */
@OptIn(ExperimentalCoroutinesApi::class)
class GalleryViewModel(private val container: AppContainer) : ViewModel() {
    val state: StateFlow<GalleryUiState> = container.progress.statuses.mapLatest { statuses ->
        try {
            val index = container.content.index()
            val completed = statuses.filterValues { it.completed }
            val bySource = completed.entries.groupBy({ it.value.source }, { it.key })
            val sections = mutableListOf<GallerySection>()
            bySource[DAILY]?.let { ids ->
                val daily = container.content.daily()
                val set = ids.toSet()
                val items = daily.puzzles.filter { it.id in set }.map { GalleryItem(it, DAILY) }
                if (items.isNotEmpty()) sections += GallerySection(DAILY, null, items)
            }
            // 홈과 같은 팩 순서
            for (summary in index.packs) {
                val ids = bySource[summary.id]?.toSet() ?: continue
                val pack = container.content.pack(summary.id)
                val items = pack.puzzles.filter { it.id in ids }.map { GalleryItem(it, summary.id) }
                if (items.isNotEmpty()) sections += GallerySection(summary.id, summary.title, items)
            }
            GalleryUiState(
                loading = false,
                sections = sections,
                collected = sections.sumOf { it.items.size },
                total = index.packs.sumOf { it.count } + index.daily.days,
            )
        } catch (e: Exception) {
            Log.e("GalleryViewModel", "gallery load failed", e)
            GalleryUiState(loading = false)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GalleryUiState())

    companion object {
        val Factory = viewModelFactory {
            initializer { GalleryViewModel((this[APPLICATION_KEY] as NonogramApplication).container) }
        }
    }
}
