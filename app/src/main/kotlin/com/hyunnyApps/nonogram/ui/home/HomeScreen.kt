package com.hyunnyApps.nonogram.ui.home

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hyunnyApps.nonogram.R
import com.hyunnyApps.nonogram.ads.RewardedAdManager
import com.hyunnyApps.nonogram.data.DAILY
import com.hyunnyApps.nonogram.data.PlayerRepository
import com.hyunnyApps.nonogram.ui.components.LevelTag
import com.hyunnyApps.nonogram.ui.components.PaperCard
import com.hyunnyApps.nonogram.ui.components.PencilUnderline
import com.hyunnyApps.nonogram.ui.components.PixelIcon
import com.hyunnyApps.nonogram.ui.components.PixelIcons
import com.hyunnyApps.nonogram.ui.components.PixelPicture
import com.hyunnyApps.nonogram.ui.components.StickerButton
import com.hyunnyApps.nonogram.ui.components.StickerStyle
import com.hyunnyApps.nonogram.ui.components.levelName
import com.hyunnyApps.nonogram.ui.components.rememberReminderSetter
import com.hyunnyApps.nonogram.ui.components.text
import com.hyunnyApps.nonogram.ui.theme.HandFont
import com.hyunnyApps.nonogram.ui.theme.LocalNoteColors
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter

private val hand16 = TextStyle(fontFamily = HandFont, fontSize = 18.sp, lineHeight = 20.sp)
private val hand20 = TextStyle(fontFamily = HandFont, fontSize = 22.sp, lineHeight = 24.sp)

@Composable
fun HomeScreen(
    rewarded: RewardedAdManager,
    player: PlayerRepository,
    onOpenPuzzle: (source: String, puzzleId: String) -> Unit,
    onOpenPack: (packId: String) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenTutorial: () -> Unit,
    onOpenGallery: () -> Unit,
    viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val activity = LocalActivity.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val deniedText = stringResource(R.string.reminder_denied)
    val keptText = stringResource(R.string.streak_kept)
    val notReadyText = stringResource(R.string.reward_not_ready)
    val setReminder = rememberReminderSetter(player) { scope.launch { snackbar.showSnackbar(deniedText) } }

    // 첫 실행이면 규칙 튜토리얼을 한 번 자동으로 보여준다
    LaunchedEffect(state.loading, state.tutorialSeen) {
        if (!state.loading && !state.tutorialSeen) onOpenTutorial()
    }
    Box(Modifier.fillMaxSize()) {
        if (state.loading) {
            CircularProgressIndicator(Modifier.align(Alignment.Center), color = LocalNoteColors.current.ink)
            return@Box
        }
        LazyColumn(
            Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.statusBars),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            item { Header(state, onOpenTutorial, onOpenSettings) }
            state.streakLost?.let { lost ->
                item {
                    StreakLostCard(lost) {
                        val shown = activity != null && rewarded.show(activity, onRewarded = {
                            viewModel.restoreStreak()
                            scope.launch { snackbar.showSnackbar(keptText) }
                        })
                        if (!shown) scope.launch { snackbar.showSnackbar(notReadyText) }
                    }
                }
            }
            state.daily?.let { daily -> item { DailyCard(daily) { onOpenPuzzle(DAILY, daily.puzzle.id) } } }
            state.continueItem?.let { c -> item { ContinueCard(c) { onOpenPuzzle(c.source, c.puzzle.id) } } }
            if (!state.tutorialSeen) item { TutorialCard(onOpenTutorial) }
            if (state.showReminderCard) {
                item { ReminderCard(onYes = { setReminder(19) }, onNo = viewModel::dismissReminderCard) }
            }
            item { CollectionRow(state.completedTotal, state.totalPuzzles, onOpenGallery) }
            item { NoteTabs(state.kind, viewModel::selectKind) }
            items(state.sections, key = { "${state.kind}-${it.theme}" }) { section ->
                ThemeSectionView(section) { onOpenPack(it) }
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun Header(state: HomeUiState, onHelp: () -> Unit, onSettings: () -> Unit) {
    val note = LocalNoteColors.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineLarge, color = note.ink)
            PencilUnderline(width = 104.dp)
        }
        HudItem(PixelIcons.Flame, state.streak.toString())
        Spacer(Modifier.width(10.dp))
        HudItem(PixelIcons.Bulb, state.hints.toString())
        Spacer(Modifier.width(6.dp))
        Box(Modifier.size(40.dp).clip(CircleShape).clickable(onClickLabel = stringResource(R.string.help), onClick = onHelp), contentAlignment = Alignment.Center) {
            PixelIcon(PixelIcons.Help, size = 20.dp)
        }
        Box(Modifier.size(40.dp).clip(CircleShape).clickable(onClickLabel = stringResource(R.string.settings_title), onClick = onSettings), contentAlignment = Alignment.Center) {
            PixelIcon(PixelIcons.Gear, size = 20.dp)
        }
    }
}

@Composable
private fun HudItem(icon: com.hyunnyApps.nonogram.ui.components.PixelArt, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        PixelIcon(icon, size = 22.dp)
        Spacer(Modifier.width(4.dp))
        Text(value, style = hand20, color = LocalNoteColors.current.ink)
    }
}

/** "?" 가 적힌 작은 모눈 (오늘의 퍼즐 미완성일 때) */
@Composable
private fun MysteryGrid(modifier: Modifier) {
    val note = LocalNoteColors.current
    Box(modifier.background(note.card).border(1.5.dp, note.ink.copy(alpha = 0.6f)), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val n = 6
            val s = size.width / n
            for (i in 1 until n) {
                drawLine(note.paperLineStrong, Offset(i * s, 0f), Offset(i * s, size.height), 1.5f)
                drawLine(note.paperLineStrong, Offset(0f, i * s), Offset(size.width, i * s), 1.5f)
            }
        }
        Text("?", style = TextStyle(fontFamily = HandFont, fontSize = 52.sp), color = note.redPencil)
    }
}

@Composable
private fun DailyCard(daily: DailyItem, onClick: () -> Unit) {
    val note = LocalNoteColors.current
    val locale = LocalConfiguration.current.locales[0]
    val date = daily.date.format(DateTimeFormatter.ofPattern(stringResource(R.string.date_pattern), locale))
    PaperCard(tape = true, rotation = -1.2f, onClick = onClick) {
        Column(Modifier.padding(start = 18.dp, end = 18.dp, top = 22.dp, bottom = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(92.dp)) {
                    if (daily.completed) {
                        PixelPicture(daily.puzzle.reveal, Modifier.fillMaxSize().padding(6.dp))
                    } else {
                        MysteryGrid(Modifier.fillMaxSize())
                    }
                }
                Spacer(Modifier.width(16.dp))
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(date, style = hand16, color = note.inkSoft)
                    Text(stringResource(R.string.home_daily_title), style = MaterialTheme.typography.headlineMedium, color = note.ink)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        LevelTag(daily.puzzle.difficulty.level)
                        Text(stringResource(R.string.size_format, daily.puzzle.w, daily.puzzle.h), style = hand16, color = note.inkSoft)
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            StickerButton(
                text = stringResource(
                    when {
                        daily.completed -> R.string.home_daily_done
                        daily.inProgress -> R.string.home_daily_continue
                        else -> R.string.home_daily_start
                    }
                ),
                onClick = onClick,
                icon = if (daily.completed) PixelIcons.Check else PixelIcons.Play,
                style = if (daily.completed) StickerStyle.Plain else StickerStyle.Primary,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun ContinueCard(item: ContinueItem, onClick: () -> Unit) {
    val note = LocalNoteColors.current
    PaperCard(rotation = 0.8f, onClick = onClick) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).background(note.highlighter, RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                PixelIcon(PixelIcons.Play, size = 24.dp)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.continue_title), style = MaterialTheme.typography.titleLarge, color = note.ink)
                Text(
                    listOfNotNull(item.packTitle?.text(), if (item.number > 0) stringResource(R.string.game_puzzle_number, item.number) else null)
                        .joinToString(" · ") + "  ·  " + stringResource(R.string.continue_progress, (item.fraction * 100).toInt()),
                    style = MaterialTheme.typography.bodySmall,
                    color = note.inkSoft,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                PencilProgress(item.fraction)
            }
        }
    }
}

/** 색연필로 그은 진행 막대 */
@Composable
private fun PencilProgress(fraction: Float, modifier: Modifier = Modifier) {
    val note = LocalNoteColors.current
    Canvas(modifier.fillMaxWidth().height(8.dp)) {
        val y = size.height / 2
        drawLine(note.paperLineStrong, Offset(0f, y), Offset(size.width, y), 4.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
        if (fraction > 0f) {
            drawLine(note.redPencil, Offset(0f, y), Offset(size.width * fraction.coerceIn(0f, 1f), y), 4.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
        }
    }
}

@Composable
private fun StreakLostCard(lost: Int, onKeep: () -> Unit) {
    val note = LocalNoteColors.current
    PaperCard(rotation = -0.6f) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            PixelIcon(PixelIcons.Flame, size = 34.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.streak_lost_title, lost), style = MaterialTheme.typography.titleLarge, color = note.ink)
                Text(stringResource(R.string.streak_lost_text), style = MaterialTheme.typography.bodySmall, color = note.inkSoft)
            }
            StickerButton(stringResource(R.string.streak_keep), onKeep, style = StickerStyle.Highlight, height = 44.dp, modifier = Modifier.width(120.dp))
        }
    }
}

@Composable
private fun TutorialCard(onClick: () -> Unit) {
    val note = LocalNoteColors.current
    PaperCard(rotation = 0.5f, onClick = onClick) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            PixelIcon(PixelIcons.Help, size = 30.dp, tint = note.redPencil)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.tutorial_card_title), style = MaterialTheme.typography.titleLarge, color = note.ink)
                Text(stringResource(R.string.tutorial_card_text), style = MaterialTheme.typography.bodySmall, color = note.inkSoft)
            }
            StickerButton(stringResource(R.string.tutorial_card_button), onClick, style = StickerStyle.Plain, height = 44.dp, modifier = Modifier.width(96.dp))
        }
    }
}

@Composable
private fun ReminderCard(onYes: () -> Unit, onNo: () -> Unit) {
    val note = LocalNoteColors.current
    PaperCard(rotation = -0.4f, tape = true) {
        Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PixelIcon(PixelIcons.Bell, size = 28.dp, tint = note.redPencil)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.reminder_card_title), style = MaterialTheme.typography.titleLarge, color = note.ink)
                    Text(stringResource(R.string.reminder_card_text), style = MaterialTheme.typography.bodySmall, color = note.inkSoft)
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                StickerButton(stringResource(R.string.reminder_card_yes), onYes, height = 44.dp, modifier = Modifier.weight(1f))
                Spacer(Modifier.width(8.dp))
                TextButton(onClick = onNo) { Text(stringResource(R.string.reminder_card_no), color = note.inkSoft) }
            }
        }
    }
}

@Composable
private fun CollectionRow(collected: Int, total: Int, onClick: () -> Unit) {
    val note = LocalNoteColors.current
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable(onClick = onClick).padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PixelIcon(PixelIcons.Frame, size = 28.dp)
        Spacer(Modifier.width(10.dp))
        Text(stringResource(R.string.gallery_title), style = MaterialTheme.typography.titleLarge, color = note.ink, modifier = Modifier.weight(1f))
        Text(stringResource(R.string.gallery_progress, collected, total), style = hand16, color = note.inkSoft)
        Text("  ›", style = hand20, color = note.inkSoft)
    }
}

/** 노트 인덱스 탭 (기본 / 컬러) */
@Composable
private fun NoteTabs(kind: String, onSelect: (String) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        NoteTab(kind == KIND_MONO, R.string.mode_mono_title, R.string.mode_mono_desc, Modifier.weight(1f)) { onSelect(KIND_MONO) }
        NoteTab(kind == KIND_COLOR, R.string.mode_color_title, R.string.mode_color_desc, Modifier.weight(1f)) { onSelect(KIND_COLOR) }
    }
}

@Composable
private fun NoteTab(selected: Boolean, title: Int, desc: Int, modifier: Modifier, onClick: () -> Unit) {
    val note = LocalNoteColors.current
    val shape = RoundedCornerShape(10.dp)
    Column(
        modifier
            .clip(shape)
            .background(if (selected) note.card else Color.Transparent)
            .border(if (selected) 2.dp else 1.dp, if (selected) note.ink else note.ink.copy(alpha = 0.25f), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (selected) {
                Box(Modifier.size(10.dp).background(note.redPencil, CircleShape))
                Spacer(Modifier.width(6.dp))
            }
            Text(stringResource(title), style = MaterialTheme.typography.titleLarge, color = if (selected) note.ink else note.inkSoft)
        }
        Text(stringResource(desc), style = MaterialTheme.typography.bodySmall, color = note.inkSoft)
    }
}

@Composable
private fun ThemeSectionView(section: ThemeSection, onOpenPack: (String) -> Unit) {
    val note = LocalNoteColors.current
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(section.title.text(), style = MaterialTheme.typography.headlineSmall, color = note.ink, modifier = Modifier.weight(1f))
            Text(stringResource(R.string.section_count, section.puzzleCount), style = hand16, color = note.inkSoft)
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp), contentPadding = PaddingValues(end = 8.dp, bottom = 6.dp)) {
            items(section.packs, key = { it.summary.id }) { pack -> NotebookCard(pack) { onOpenPack(pack.summary.id) } }
        }
    }
}

/** 팩 = 스프링 노트 한 권 */
@Composable
private fun NotebookCard(pack: PackItem, onClick: () -> Unit) {
    val s = pack.summary
    val note = LocalNoteColors.current
    val minLevel = s.levels.indexOfFirst { it > 0 } + 1
    val maxLevel = s.levels.indexOfLast { it > 0 } + 1
    PaperCard(modifier = Modifier.width(150.dp), onClick = onClick) {
        Column {
            Box(Modifier.fillMaxWidth().height(128.dp)) {
                // 스프링 구멍
                Column(Modifier.padding(start = 6.dp, top = 10.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    repeat(9) { Box(Modifier.size(7.dp).background(note.paper, CircleShape).border(1.dp, note.inkSoft.copy(alpha = 0.6f), CircleShape)) }
                }
                Box(Modifier.fillMaxSize().padding(start = 24.dp, end = 12.dp, top = 12.dp, bottom = 8.dp), contentAlignment = Alignment.Center) {
                    s.cover?.let { PixelPicture(it, Modifier.fillMaxSize()) }
                }
                // 권 번호 라벨 (테이프)
                Text(
                    stringResource(R.string.pack_volume, s.volume),
                    style = hand20,
                    color = note.ink,
                    modifier = Modifier.align(Alignment.BottomEnd).padding(end = 8.dp, bottom = 4.dp)
                        .background(note.tape).padding(horizontal = 8.dp),
                )
            }
            Column(Modifier.padding(start = 24.dp, end = 12.dp, bottom = 12.dp, top = 4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (minLevel > 0) {
                    Text(
                        stringResource(R.string.pack_range, stringResource(levelName(minLevel)), stringResource(levelName(maxLevel))),
                        style = hand16,
                        color = note.inkSoft,
                        maxLines = 1,
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PencilProgress(if (s.count == 0) 0f else pack.completed.toFloat() / s.count, Modifier.weight(1f))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.pack_progress, pack.completed, s.count), style = hand16, color = note.inkSoft)
                }
            }
        }
    }
}

