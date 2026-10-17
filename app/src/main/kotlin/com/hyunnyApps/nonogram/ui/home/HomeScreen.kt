package com.hyunnyApps.nonogram.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hyunnyApps.nonogram.R
import com.hyunnyApps.nonogram.data.DAILY
import com.hyunnyApps.nonogram.ui.components.AppIcons
import com.hyunnyApps.nonogram.ui.components.LevelChip
import com.hyunnyApps.nonogram.ui.components.PixelPicture
import com.hyunnyApps.nonogram.ui.components.levelName
import com.hyunnyApps.nonogram.ui.components.text
import com.hyunnyApps.nonogram.core.format.Picture
import java.time.format.DateTimeFormatter

@Composable
fun HomeScreen(
    onOpenPuzzle: (source: String, puzzleId: String) -> Unit,
    onOpenPack: (packId: String) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenTutorial: () -> Unit,
    viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    // 첫 실행이면 규칙 튜토리얼을 한 번 자동으로 보여준다
    LaunchedEffect(state.loading, state.tutorialSeen) {
        if (!state.loading && !state.tutorialSeen) onOpenTutorial()
    }
    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        if (state.loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@Surface
        }
        LazyColumn(
            Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.statusBars),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            item { Header(onOpenTutorial, onOpenSettings) }
            item { StatsRow(state) }
            if (!state.tutorialSeen) item { TutorialCard(onOpenTutorial) }
            state.daily?.let { daily -> item { DailyHero(daily) { onOpenPuzzle(DAILY, daily.puzzle.id) } } }
            item { ModeSelector(state.kind, viewModel::selectKind) }
            items(state.sections, key = { "${state.kind}-${it.theme}" }) { section ->
                ThemeSectionView(section) { onOpenPack(it) }
            }
        }
    }
}

@Composable
private fun Header(onHelp: () -> Unit, onSettings: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineLarge)
            Text(
                stringResource(R.string.app_tagline),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onHelp) { Icon(AppIcons.Help, stringResource(R.string.help)) }
        IconButton(onClick = onSettings) { Icon(Icons.Filled.Settings, stringResource(R.string.settings_title)) }
    }
}

@Composable
private fun StatsRow(state: HomeUiState) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        StatTile("🔥", stringResource(R.string.stat_streak), stringResource(R.string.stat_days, state.streak), Modifier.weight(1f))
        StatTile("💡", stringResource(R.string.stat_hints), stringResource(R.string.stat_count, state.hints), Modifier.weight(1f))
        StatTile("🖼️", stringResource(R.string.stat_completed), stringResource(R.string.stat_count, state.completedTotal), Modifier.weight(1f))
    }
}

@Composable
private fun StatTile(emoji: String, label: String, value: String, modifier: Modifier) {
    Surface(modifier, shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surface) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(emoji, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.width(8.dp))
            Column {
                Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun TutorialCard(onClick: () -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.secondaryContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("👋", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.tutorial_card_title), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
                Text(stringResource(R.string.tutorial_card_text), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
            }
            Button(onClick = onClick, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)) {
                Text(stringResource(R.string.tutorial_card_button))
            }
        }
    }
}

/** 오늘의 퍼즐 미완성일 때 보여주는 "?" 픽셀 그림 */
private val MysteryPicture = Picture(
    palette = listOf("#FFFFFF"),
    rows = listOf(
        "..111..",
        ".11.11.",
        ".....11",
        "....11.",
        "...11..",
        "...11..",
        ".......",
        "...11..",
    ),
)

@Composable
private fun DailyHero(daily: DailyItem, onClick: () -> Unit) {
    val locale = LocalConfiguration.current.locales[0]
    val pattern = stringResource(R.string.date_pattern)
    val date = daily.date.format(DateTimeFormatter.ofPattern(pattern, locale))
    val gradient = Brush.linearGradient(listOf(Color(0xFF4F46E5), Color(0xFF8B5CF6)))
    Box(
        Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(gradient)
            .clickable(onClick = onClick)
            .padding(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(104.dp).clip(RoundedCornerShape(18.dp)).background(Color.White.copy(alpha = if (daily.completed) 1f else 0.16f)).padding(14.dp),
                contentAlignment = Alignment.Center,
            ) {
                PixelPicture(if (daily.completed) daily.puzzle.reveal else MysteryPicture, Modifier.fillMaxSize())
            }
            Spacer(Modifier.width(18.dp))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(stringResource(R.string.home_daily_title), style = MaterialTheme.typography.titleLarge, color = Color.White)
                Text(date, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.85f))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LevelChip(daily.puzzle.difficulty.level, onDark = true)
                    Text(stringResource(R.string.size_format, daily.puzzle.w, daily.puzzle.h), style = MaterialTheme.typography.labelLarge, color = Color.White)
                }
                Spacer(Modifier.height(2.dp))
                Button(
                    onClick = onClick,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color(0xFF4F46E5)),
                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp),
                ) {
                    if (!daily.completed) Icon(AppIcons.Play, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        stringResource(
                            when {
                                daily.completed -> R.string.home_daily_done
                                daily.inProgress -> R.string.home_daily_continue
                                else -> R.string.home_daily_start
                            }
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun ModeSelector(kind: String, onSelect: (String) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        ModeCard(kind == KIND_MONO, "⬛", R.string.mode_mono_title, R.string.mode_mono_desc, Modifier.weight(1f)) { onSelect(KIND_MONO) }
        ModeCard(kind == KIND_COLOR, "🎨", R.string.mode_color_title, R.string.mode_color_desc, Modifier.weight(1f)) { onSelect(KIND_COLOR) }
    }
}

@Composable
private fun ModeCard(selected: Boolean, emoji: String, title: Int, desc: Int, modifier: Modifier, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = if (selected) cs.primaryContainer else cs.surface),
        border = if (selected) BorderStroke(2.dp, cs.primary) else BorderStroke(1.dp, cs.outlineVariant),
    ) {
        Column(Modifier.padding(14.dp)) {
            Text("$emoji  " + stringResource(title), style = MaterialTheme.typography.titleMedium, color = if (selected) cs.onPrimaryContainer else cs.onSurface)
            Text(stringResource(desc), style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
        }
    }
}

@Composable
private fun ThemeSectionView(section: ThemeSection, onOpenPack: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(section.title.text(), style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            Text(stringResource(R.string.section_count, section.puzzleCount), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(section.packs, key = { it.summary.id }) { pack -> PackCard(pack) { onOpenPack(pack.summary.id) } }
        }
    }
}

@Composable
private fun PackCard(pack: PackItem, onClick: () -> Unit) {
    val s = pack.summary
    val cs = MaterialTheme.colorScheme
    val minLevel = s.levels.indexOfFirst { it > 0 } + 1
    val maxLevel = s.levels.indexOfLast { it > 0 } + 1
    Card(
        onClick = onClick,
        modifier = Modifier.width(160.dp),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = cs.surface),
    ) {
        Box(Modifier.fillMaxWidth().height(116.dp).background(cs.surfaceVariant.copy(alpha = 0.6f)).padding(16.dp), contentAlignment = Alignment.Center) {
            s.cover?.let { PixelPicture(it, Modifier.fillMaxSize()) }
        }
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(R.string.pack_volume, s.volume), style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (minLevel > 0) {
                Text(
                    stringResource(R.string.pack_range, stringResource(levelName(minLevel)), stringResource(levelName(maxLevel))),
                    style = MaterialTheme.typography.labelMedium,
                    color = cs.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                LinearProgressIndicator(
                    progress = { if (s.count == 0) 0f else pack.completed.toFloat() / s.count },
                    modifier = Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(3.dp)),
                    trackColor = cs.surfaceVariant,
                    drawStopIndicator = {},
                    gapSize = 0.dp,
                )
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.pack_progress, pack.completed, s.count), style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant)
            }
        }
    }
}
