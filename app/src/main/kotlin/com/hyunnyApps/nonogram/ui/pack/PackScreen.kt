package com.hyunnyApps.nonogram.ui.pack

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hyunnyApps.nonogram.R
import com.hyunnyApps.nonogram.ui.components.Adaptive
import com.hyunnyApps.nonogram.ui.components.TopSafeInsets
import com.hyunnyApps.nonogram.ui.components.LevelTag
import com.hyunnyApps.nonogram.ui.components.NoteTopBar
import com.hyunnyApps.nonogram.ui.components.PaperCard
import com.hyunnyApps.nonogram.ui.components.PixelPicture
import com.hyunnyApps.nonogram.ui.components.levelColor
import com.hyunnyApps.nonogram.ui.components.text
import com.hyunnyApps.nonogram.ui.theme.HandFont
import com.hyunnyApps.nonogram.ui.theme.LocalNoteColors

private val hand18 = TextStyle(fontFamily = HandFont, fontSize = 18.sp, lineHeight = 20.sp)

@Composable
fun PackScreen(
    onBack: () -> Unit,
    onOpenPuzzle: (source: String, puzzleId: String) -> Unit,
    viewModel: PackViewModel = viewModel(factory = PackViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val note = LocalNoteColors.current
    Column(Modifier.fillMaxSize().windowInsetsPadding(TopSafeInsets)) {
        NoteTopBar(state.pack?.title?.text() ?: "", onBack) {
            state.pack?.let { p ->
                Text(stringResource(R.string.pack_header_progress, state.completed, p.puzzles.size), style = hand18, color = note.inkSoft)
            }
        }
        when {
            state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = note.ink) }
            state.error -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(stringResource(R.string.error_load)) }
            else -> LazyVerticalGrid(
                columns = GridCells.Adaptive(Adaptive.tileMinSize(LocalConfiguration.current.screenWidthDp.dp)),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                items(state.tiles, key = { it.puzzle.id }) { tile ->
                    PuzzleTileView(tile) { onOpenPuzzle(viewModel.packId, tile.puzzle.id) }
                }
            }
        }
    }
}

@Composable
private fun PuzzleTileView(tile: PuzzleTile, onClick: () -> Unit) {
    val p = tile.puzzle
    val note = LocalNoteColors.current
    val accent = levelColor(p.difficulty.level)
    // 번호마다 살짝 다르게 기울여 손으로 붙인 메모처럼
    val tilt = ((tile.number * 37) % 5 - 2) * 0.5f
    PaperCard(rotation = tilt, onClick = onClick) {
        Column(Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(4.dp))
                    .then(if (tile.started && !tile.completed) Modifier.border(2.dp, note.redPencil, RoundedCornerShape(4.dp)) else Modifier),
                contentAlignment = Alignment.Center,
            ) {
                if (tile.completed) {
                    PixelPicture(p.reveal, Modifier.fillMaxSize().padding(6.dp))
                } else {
                    // 빈 모눈 + 손글씨 번호
                    Canvas(Modifier.fillMaxSize()) {
                        val n = 5
                        val s = size.width / n
                        for (i in 1 until n) {
                            drawLine(note.paperLine, Offset(i * s, 0f), Offset(i * s, size.height), 1.5f)
                            drawLine(note.paperLine, Offset(0f, i * s), Offset(size.width, i * s), 1.5f)
                        }
                    }
                    Text("${tile.number}", style = TextStyle(fontFamily = HandFont, fontSize = 40.sp), color = accent)
                }
            }
            Text(
                if (tile.completed) p.title.text() else stringResource(R.string.size_format, p.w, p.h),
                style = hand18,
                color = if (tile.completed) note.ink else note.inkSoft,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp),
            )
            if (!tile.completed) {
                if (tile.started) Text(stringResource(R.string.tile_in_progress), style = hand18, color = note.redPencil)
                else LevelTag(p.difficulty.level)
            }
        }
    }
}

