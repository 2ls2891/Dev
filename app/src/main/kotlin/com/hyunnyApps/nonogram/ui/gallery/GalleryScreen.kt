package com.hyunnyApps.nonogram.ui.gallery

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hyunnyApps.nonogram.R
import com.hyunnyApps.nonogram.ui.components.Adaptive
import com.hyunnyApps.nonogram.ui.components.TopSafeInsets
import com.hyunnyApps.nonogram.ui.components.NoteTopBar
import com.hyunnyApps.nonogram.ui.components.PaperCard
import com.hyunnyApps.nonogram.ui.components.PixelIcon
import com.hyunnyApps.nonogram.ui.components.PixelIcons
import com.hyunnyApps.nonogram.ui.components.PixelPicture
import com.hyunnyApps.nonogram.ui.components.StickerButton
import com.hyunnyApps.nonogram.ui.components.text
import com.hyunnyApps.nonogram.ui.theme.HandFont
import com.hyunnyApps.nonogram.ui.theme.LocalNoteColors

private val hand18 = TextStyle(fontFamily = HandFont, fontSize = 18.sp, lineHeight = 20.sp)

@Composable
fun GalleryScreen(
    onBack: () -> Unit,
    onOpenPuzzle: (source: String, puzzleId: String) -> Unit,
    viewModel: GalleryViewModel = viewModel(factory = GalleryViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val note = LocalNoteColors.current
    Column(Modifier.fillMaxSize().windowInsetsPadding(TopSafeInsets)) {
        NoteTopBar(stringResource(R.string.gallery_title), onBack) {
            if (!state.loading) Text(stringResource(R.string.gallery_progress, state.collected, state.total), style = hand18, color = note.inkSoft)
        }
        when {
            state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = note.ink) }
            state.collected == 0 -> EmptyGallery(onBack)
            else -> LazyVerticalGrid(
                columns = GridCells.Adaptive(Adaptive.tileMinSize(LocalConfiguration.current.screenWidthDp.dp)),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                state.sections.forEach { section ->
                    item(span = { GridItemSpan(maxLineSpan) }, key = "h-${section.source}") {
                        Text(
                            section.title?.text() ?: stringResource(R.string.gallery_daily),
                            style = MaterialTheme.typography.titleLarge,
                            color = note.ink,
                            modifier = Modifier.padding(top = 12.dp),
                        )
                    }
                    items(section.items, key = { "${section.source}-${it.puzzle.id}" }) { item ->
                        // 폴라로이드: 아래 여백이 넓은 사진 + 손글씨 제목
                        val tilt = ((item.puzzle.id.hashCode() % 5) - 2) * 0.6f
                        PaperCard(rotation = tilt, onClick = { onOpenPuzzle(item.source, item.puzzle.id) }) {
                            Column(Modifier.padding(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                PixelPicture(item.puzzle.reveal, Modifier.fillMaxWidth().aspectRatio(1f).padding(4.dp))
                                Text(
                                    item.puzzle.title.text(),
                                    style = hand18,
                                    color = note.ink,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(top = 4.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyGallery(onBack: () -> Unit) {
    val note = LocalNoteColors.current
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        PixelIcon(PixelIcons.Frame, size = 72.dp)
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.gallery_empty_title), style = MaterialTheme.typography.headlineSmall, color = note.ink)
        Spacer(Modifier.height(6.dp))
        Text(stringResource(R.string.gallery_empty_text), style = MaterialTheme.typography.bodyMedium, color = note.inkSoft, textAlign = TextAlign.Center)
        Spacer(Modifier.height(20.dp))
        StickerButton(stringResource(R.string.gallery_empty_button), onBack, icon = PixelIcons.Play, modifier = Modifier.width(220.dp))
    }
}
