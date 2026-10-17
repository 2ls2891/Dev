package com.hyunnyApps.nonogram.ui.pack

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hyunnyApps.nonogram.R
import com.hyunnyApps.nonogram.ui.components.LevelChip
import com.hyunnyApps.nonogram.ui.components.PixelPicture
import com.hyunnyApps.nonogram.ui.components.levelColor
import com.hyunnyApps.nonogram.ui.components.text

@Composable
fun PackScreen(
    onBack: () -> Unit,
    onOpenPuzzle: (source: String, puzzleId: String) -> Unit,
    viewModel: PackViewModel = viewModel(factory = PackViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.statusBars)) {
            Row(Modifier.padding(start = 4.dp, end = 16.dp, top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back)) }
                state.pack?.let { Text(it.title.text(), style = MaterialTheme.typography.titleLarge) }
            }
            when {
                state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                state.error -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(stringResource(R.string.error_load)) }
                else -> LazyVerticalGrid(
                    columns = GridCells.Adaptive(104.dp),
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item(span = { GridItemSpan(maxLineSpan) }) { ProgressHeader(state) }
                    items(state.tiles, key = { it.puzzle.id }) { tile ->
                        PuzzleTileView(tile) { onOpenPuzzle(viewModel.packId, tile.puzzle.id) }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProgressHeader(state: PackUiState) {
    val total = state.tiles.size
    Column(Modifier.padding(bottom = 4.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            stringResource(R.string.pack_header_progress, state.completed, total),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        LinearProgressIndicator(
            progress = { if (total == 0) 0f else state.completed.toFloat() / total },
            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            drawStopIndicator = {},
            gapSize = 0.dp,
        )
    }
}

@Composable
private fun PuzzleTileView(tile: PuzzleTile, onClick: () -> Unit) {
    val p = tile.puzzle
    val cs = MaterialTheme.colorScheme
    val accent = levelColor(p.difficulty.level)
    Column(
        Modifier
            .clip(MaterialTheme.shapes.medium)
            .background(cs.surface)
            .then(if (tile.started && !tile.completed) Modifier.border(2.dp, cs.primary, MaterialTheme.shapes.medium) else Modifier)
            .clickable(onClick = onClick)
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(12.dp))
                .background(if (tile.completed) cs.surfaceVariant.copy(alpha = 0.5f) else accent.copy(alpha = 0.10f))
                .padding(if (tile.completed) 8.dp else 0.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (tile.completed) {
                PixelPicture(p.reveal, Modifier.fillMaxSize())
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${tile.number}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold, color = accent)
                    Text(stringResource(R.string.size_format, p.w, p.h), style = MaterialTheme.typography.labelMedium, color = cs.onSurfaceVariant)
                }
            }
        }
        if (tile.completed) {
            Text(p.title.text(), style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
        } else if (tile.started) {
            Text(stringResource(R.string.tile_in_progress), style = MaterialTheme.typography.labelLarge, color = cs.primary)
        } else {
            LevelChip(p.difficulty.level)
        }
    }
}

