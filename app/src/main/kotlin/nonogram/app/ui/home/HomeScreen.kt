package nonogram.app.ui.home

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import nonogram.app.R
import nonogram.app.data.DAILY
import nonogram.app.ui.components.DifficultyStars
import nonogram.app.ui.components.PixelPicture
import nonogram.app.ui.components.text
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenPuzzle: (source: String, puzzleId: String) -> Unit,
    onOpenPack: (packId: String) -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name), fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.settings_title))
                    }
                },
            )
        },
    ) { padding ->
        if (state.loading) {
            Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }
        LazyColumn(
            Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AssistChip(onClick = {}, label = { Text("🔥 " + stringResource(R.string.home_streak, state.streak)) })
                    AssistChip(onClick = {}, label = { Text("💡 " + pluralStringResource(R.plurals.home_hints, state.hints, state.hints)) })
                }
            }
            state.daily?.let { daily ->
                item { DailyCard(daily) { onOpenPuzzle(DAILY, daily.puzzle.id) } }
            }
            item {
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    listOf(KIND_MONO to R.string.home_tab_mono, KIND_COLOR to R.string.home_tab_color).forEachIndexed { i, (k, label) ->
                        SegmentedButton(
                            selected = state.kind == k,
                            onClick = { viewModel.selectKind(k) },
                            shape = SegmentedButtonDefaults.itemShape(i, 2),
                        ) { Text(stringResource(label)) }
                    }
                }
            }
            items(state.sections, key = { "${state.kind}-${it.theme}" }) { section ->
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(section.title.text(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(section.packs, key = { it.summary.id }) { pack -> PackCard(pack) { onOpenPack(pack.summary.id) } }
                    }
                }
            }
        }
    }
}

@Composable
private fun DailyCard(daily: DailyItem, onClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.home_daily_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(daily.date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)), style = MaterialTheme.typography.bodyMedium)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    DifficultyStars(daily.puzzle.difficulty.level)
                    Text("  ${daily.puzzle.w}×${daily.puzzle.h}", style = MaterialTheme.typography.labelMedium)
                }
            }
            if (daily.completed) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    PixelPicture(daily.puzzle.reveal, Modifier.size(64.dp))
                    OutlinedButton(onClick = onClick) { Text(stringResource(R.string.home_daily_done)) }
                }
            } else {
                Button(onClick = onClick) {
                    Text(stringResource(if (daily.inProgress) R.string.home_daily_continue else R.string.home_daily_start))
                }
            }
        }
    }
}

@Composable
private fun PackCard(pack: PackItem, onClick: () -> Unit) {
    val s = pack.summary
    Card(Modifier.width(156.dp).clickable(onClick = onClick)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(s.title.text(), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 1)
            Text(stringResource(R.string.pack_progress, pack.completed, s.count), style = MaterialTheme.typography.bodySmall)
            LinearProgressIndicator(
                progress = { if (s.count == 0) 0f else pack.completed.toFloat() / s.count },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(2.dp))
            val maxLevel = s.levels.indexOfLast { it > 0 } + 1
            val minLevel = s.levels.indexOfFirst { it > 0 } + 1
            Text("★$minLevel – ★$maxLevel", style = MaterialTheme.typography.labelSmall)
        }
    }
}
