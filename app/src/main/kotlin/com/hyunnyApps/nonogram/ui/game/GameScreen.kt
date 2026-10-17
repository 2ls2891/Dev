package com.hyunnyApps.nonogram.ui.game

import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hyunnyApps.nonogram.R
import com.hyunnyApps.nonogram.ads.RewardedAdManager
import com.hyunnyApps.nonogram.core.game.Mark
import com.hyunnyApps.nonogram.data.DAILY
import com.hyunnyApps.nonogram.data.PlayerRepository
import com.hyunnyApps.nonogram.feedback.Sfx
import com.hyunnyApps.nonogram.feedback.SoundManager
import com.hyunnyApps.nonogram.ui.components.LevelTag
import com.hyunnyApps.nonogram.ui.components.PaperCard
import com.hyunnyApps.nonogram.ui.components.PixelArt
import com.hyunnyApps.nonogram.ui.components.PixelIcon
import com.hyunnyApps.nonogram.ui.components.PixelIcons
import com.hyunnyApps.nonogram.ui.components.RulesContent
import com.hyunnyApps.nonogram.ui.components.Stamp
import com.hyunnyApps.nonogram.ui.components.StickerButton
import com.hyunnyApps.nonogram.ui.components.StickerStyle
import com.hyunnyApps.nonogram.ui.components.formatElapsed
import com.hyunnyApps.nonogram.ui.components.parseHexColor
import com.hyunnyApps.nonogram.ui.components.text
import com.hyunnyApps.nonogram.ui.theme.HandFont
import com.hyunnyApps.nonogram.ui.theme.LocalBoardColors
import com.hyunnyApps.nonogram.ui.theme.LocalNoteColors

private val hand18 = TextStyle(fontFamily = HandFont, fontSize = 18.sp, lineHeight = 20.sp)
private val hand22 = TextStyle(fontFamily = HandFont, fontSize = 22.sp, lineHeight = 24.sp)
private val toolText = TextStyle(fontFamily = HandFont, fontSize = 20.sp)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameScreen(
    rewarded: RewardedAdManager,
    sound: SoundManager,
    hapticEnabled: Boolean,
    onBack: () -> Unit,
    onOpenPuzzle: (source: String, puzzleId: String) -> Unit,
    viewModel: GameViewModel = viewModel(factory = GameViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val activity = LocalActivity.current
    val haptic = LocalHapticFeedback.current
    val hapticOn by rememberUpdatedState(hapticEnabled)
    var showRules by rememberSaveable { mutableStateOf(false) }

    // 줄 완성 형광펜 효과
    var flashRows by remember { mutableStateOf(emptySet<Int>()) }
    var flashCols by remember { mutableStateOf(emptySet<Int>()) }
    val flashAnim = remember { Animatable(1f) }
    var flashKey by remember { mutableStateOf(0) }
    LaunchedEffect(flashKey) {
        if (flashKey > 0) {
            flashAnim.snapTo(0f)
            flashAnim.animateTo(1f, tween(700, easing = LinearEasing))
        }
    }

    // 소리·진동
    LaunchedEffect(viewModel) {
        viewModel.feedback.collect { f ->
            when (f) {
                is GameFeedback.Painted -> {
                    sound.play(if (f.cross) Sfx.CROSS else Sfx.PENCIL)
                    if (hapticOn) haptic.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
                }
                is GameFeedback.LinesDone -> {
                    sound.play(Sfx.LINE)
                    if (hapticOn) haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                    flashRows = f.rows.toSet()
                    flashCols = f.cols.toSet()
                    flashKey++
                }
                GameFeedback.Completed -> {
                    sound.play(Sfx.COMPLETE)
                    if (hapticOn) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                }
            }
        }
    }

    // 화면이 보일 때만 타이머가 흐르고, 가려질 때 저장한다
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> viewModel.onResume()
                Lifecycle.Event.ON_PAUSE -> viewModel.onPause()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    val grantedText = pluralStringResource(R.plurals.reward_granted, PlayerRepository.REWARDED_HINTS, PlayerRepository.REWARDED_HINTS)
    val notReadyText = stringResource(R.string.reward_not_ready)
    LaunchedEffect(message) {
        val m = message ?: return@LaunchedEffect
        viewModel.consumeMessage()
        snackbar.showSnackbar(
            when (m) {
                is GameMessage.HintsGranted -> grantedText
                GameMessage.AdNotReady -> notReadyText
            }
        )
    }

    Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.statusBars)) {
        Column(Modifier.fillMaxSize()) {
            GameTopBar(state, onBack) { showRules = true }
            Box(Modifier.weight(1f).fillMaxWidth()) {
                when {
                    state.loading -> CircularProgressIndicator(Modifier.align(Alignment.Center), color = LocalNoteColors.current.ink)
                    state.error -> Text(stringResource(R.string.error_load), Modifier.align(Alignment.Center))
                    state.completed -> CompletedContent(state, sound, viewModel::restart, onBack) { next -> onOpenPuzzle(state.source, next) }
                    else -> PlayContent(
                        state, viewModel,
                        flash = if (flashAnim.value < 1f) LineFlash(flashRows, flashCols, flashAnim.value) else null,
                    )
                }
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }

    if (showRules) {
        ModalBottomSheet(onDismissRequest = { showRules = false }, containerColor = LocalNoteColors.current.card) {
            Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).padding(bottom = 32.dp)) {
                Text(stringResource(R.string.rules_title), style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(16.dp))
                RulesContent(includeColor = state.puzzle?.kind == "color")
            }
        }
    }

    if (state.rewardOfferVisible) {
        AlertDialog(
            onDismissRequest = viewModel::dismissRewardOffer,
            containerColor = LocalNoteColors.current.card,
            icon = { PixelIcon(PixelIcons.Bulb, size = 36.dp) },
            title = { Text(stringResource(R.string.reward_title), style = MaterialTheme.typography.headlineSmall) },
            text = { Text(pluralStringResource(R.plurals.reward_text, PlayerRepository.REWARDED_HINTS, PlayerRepository.REWARDED_HINTS)) },
            confirmButton = {
                StickerButton(
                    stringResource(if (state.rewardedReady) R.string.reward_watch else R.string.reward_loading),
                    onClick = {
                        val shown = activity != null && rewarded.show(activity, onRewarded = viewModel::onRewardEarned)
                        viewModel.dismissRewardOffer()
                        if (!shown) viewModel.onAdNotReady()
                    },
                    height = 46.dp,
                    modifier = Modifier.width(170.dp),
                )
            },
            dismissButton = { TextButton(onClick = viewModel::dismissRewardOffer) { Text(stringResource(R.string.cancel), color = LocalNoteColors.current.inkSoft) } },
        )
    }
}

@Composable
private fun IconSpot(art: PixelArt, label: String, onClick: () -> Unit) {
    Box(Modifier.size(44.dp).clip(CircleShape).clickable(onClickLabel = label, onClick = onClick), contentAlignment = Alignment.Center) {
        PixelIcon(art, size = 22.dp)
    }
}

@Composable
private fun GameTopBar(state: GameUiState, onBack: () -> Unit, onHelp: () -> Unit) {
    val note = LocalNoteColors.current
    val p = state.puzzle
    Row(Modifier.fillMaxWidth().padding(start = 6.dp, end = 6.dp, top = 4.dp, bottom = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        IconSpot(PixelIcons.Back, stringResource(R.string.back), onBack)
        Column(Modifier.weight(1f).padding(start = 4.dp)) {
            if (p != null) {
                Text(
                    when {
                        state.completed -> p.title.text()
                        state.source == DAILY -> stringResource(R.string.home_daily_title)
                        else -> stringResource(R.string.game_puzzle_number, state.position + 1)
                    },
                    style = MaterialTheme.typography.titleLarge,
                    color = note.ink,
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    LevelTag(p.difficulty.level)
                    Text(stringResource(R.string.size_format, p.w, p.h), style = hand18, color = note.inkSoft)
                }
            }
        }
        PixelIcon(PixelIcons.Clock, size = 18.dp, tint = note.inkSoft)
        Spacer(Modifier.width(4.dp))
        Text(formatElapsed(state.elapsedMs), style = hand22, color = note.ink)
        Spacer(Modifier.width(2.dp))
        IconSpot(PixelIcons.Help, stringResource(R.string.help), onHelp)
    }
}

@Composable
private fun PlayContent(state: GameUiState, viewModel: GameViewModel, flash: LineFlash?) {
    val puzzle = state.puzzle ?: return
    val board = state.board ?: return
    val boardColors = LocalBoardColors.current
    val isColor = puzzle.kind == "color"
    val palette = remember(puzzle.id) { puzzle.palette.map(::parseHexColor) }
    Column(Modifier.fillMaxSize()) {
        // 보드 + 진행 상황을 남는 공간의 가운데에 둔다
        Column(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.Center) {
            BoardView(
                puzzleKey = puzzle.id,
                columns = puzzle.w,
                rows = puzzle.h,
                rowClues = state.rowClues,
                colClues = state.colClues,
                board = board,
                palette = palette,
                isColor = isColor,
                highlight = state.lastHint,
                activeCell = state.activeCell,
                enabled = !state.completed,
                onStrokeStart = viewModel::onStrokeStart,
                onStrokeMove = viewModel::onStrokeMove,
                onStrokeEnd = viewModel::onStrokeEnd,
                onStrokeCancel = viewModel::onStrokeCancel,
                modifier = Modifier.weight(1f, fill = false).fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                flash = flash,
            )
            StatusStrip(state, board, Modifier.padding(horizontal = 20.dp))
        }
        GameToolbar(
            tool = state.tool,
            colors = if (isColor) palette else listOf(boardColors.monoFill),
            isColor = isColor,
            canUndo = state.canUndo,
            canRedo = state.canRedo,
            hints = state.hints,
            onTool = viewModel::selectTool,
            onUndo = viewModel::undo,
            onRedo = viewModel::redo,
            onHint = viewModel::requestHint,
        )
        // 하단 배너와 조작 버튼 사이 여백 (오클릭 방지)
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun StatusStrip(state: GameUiState, board: BoardUi, modifier: Modifier) {
    val note = LocalNoteColors.current
    val total = board.rowDone.size + board.colDone.size
    val done = board.rowDone.count { it } + board.colDone.count { it }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            stringResource(R.string.game_lines_done, done, total),
            style = hand18,
            color = note.inkSoft,
            modifier = Modifier.align(Alignment.End),
        )
        if (state.firstTimer && !board.marks.any { it != 0 }) {
            PaperCard(tape = true, rotation = -0.8f, color = Color(0xFFFFF7CC).takeIf { note.ink.luminanceIsDark() } ?: note.card) {
                Column(Modifier.padding(start = 14.dp, end = 14.dp, top = 18.dp, bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        AnnotatedString.fromHtml(stringResource(R.string.game_coach)),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (note.ink.luminanceIsDark()) Color(0xFF26304F) else note.ink,
                    )
                    val p = state.puzzle
                    if (p != null && maxOf(p.w, p.h) >= 20) {
                        Text(stringResource(R.string.game_coach_zoom), style = MaterialTheme.typography.bodySmall, color = note.inkSoft)
                    }
                }
            }
        }
    }
}

/** 잉크색이 어두우면(라이트 모드) true */
private fun Color.luminanceIsDark(): Boolean = (0.299f * red + 0.587f * green + 0.114f * blue) < 0.5f

@Composable
private fun GameToolbar(
    tool: Int,
    colors: List<Color>,
    isColor: Boolean,
    canUndo: Boolean,
    canRedo: Boolean,
    hints: Int,
    onTool: (Int) -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onHint: () -> Unit,
) {
    val note = LocalNoteColors.current
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (isColor) {
            // 컬러 퍼즐: 색연필 고르기 + 빈칸 표시
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                colors.forEachIndexed { i, color ->
                    val selected = tool == i + 1
                    Box(
                        Modifier.size(48.dp)
                            .offset(y = if (selected) (-4).dp else 0.dp)
                            .clip(CircleShape)
                            .border(if (selected) 3.dp else 2.dp, note.ink, CircleShape)
                            .clickable(onClickLabel = stringResource(R.string.game_tool_fill)) { onTool(i + 1) }
                            .padding(5.dp).clip(CircleShape).background(color),
                    )
                }
                StickerButton(
                    stringResource(R.string.game_tool_cross), { onTool(Mark.CROSS) },
                    style = if (tool == Mark.CROSS) StickerStyle.Highlight else StickerStyle.Plain,
                    selected = tool == Mark.CROSS, icon = PixelIcons.Cross, height = 46.dp, textStyle = toolText,
                    modifier = Modifier.width(140.dp),
                )
            }
        } else {
            // 흑백 퍼즐: 칠하기 / 빈칸 표시 (고른 쪽은 형광펜 + 눌린 상태)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StickerButton(
                    stringResource(R.string.game_tool_fill), { onTool(1) },
                    style = if (tool == 1) StickerStyle.Highlight else StickerStyle.Plain,
                    selected = tool == 1, icon = PixelIcons.Square, height = 50.dp, textStyle = toolText,
                    modifier = Modifier.weight(1f),
                )
                StickerButton(
                    stringResource(R.string.game_tool_cross), { onTool(Mark.CROSS) },
                    style = if (tool == Mark.CROSS) StickerStyle.Highlight else StickerStyle.Plain,
                    selected = tool == Mark.CROSS, icon = PixelIcons.Cross, height = 50.dp, textStyle = toolText,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StickerButton(stringResource(R.string.game_undo), onUndo, style = StickerStyle.Plain, icon = PixelIcons.Undo, enabled = canUndo, height = 44.dp, textStyle = toolText, modifier = Modifier.weight(1f))
            StickerButton(stringResource(R.string.game_redo), onRedo, style = StickerStyle.Plain, icon = PixelIcons.Redo, enabled = canRedo, height = 44.dp, textStyle = toolText, modifier = Modifier.weight(1f))
            StickerButton(stringResource(R.string.game_hint) + " $hints", onHint, style = StickerStyle.Plain, icon = PixelIcons.Bulb, height = 44.dp, textStyle = toolText, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun CompletedContent(state: GameUiState, sound: SoundManager, onRestart: () -> Unit, onBack: () -> Unit, onNext: (String) -> Unit) {
    val puzzle = state.puzzle ?: return
    val board = state.board
    val note = LocalNoteColors.current
    val haptic = LocalHapticFeedback.current
    var confirmRestart by rememberSaveable { mutableStateOf(false) }
    // 막 완성했으면 색 입히기 연출 → 도장 쾅 → 결과 패널. 다시 열었으면 바로 결과
    val animate = state.justCompleted
    val progress = remember(puzzle.id) { Animatable(if (animate) 0f else 1f) }
    val stampScale = remember(puzzle.id) { Animatable(if (animate) 0f else 1f) }
    var panelVisible by rememberSaveable(puzzle.id) { mutableStateOf(!animate) }
    LaunchedEffect(puzzle.id, animate) {
        if (animate && progress.value < 1f) {
            progress.animateTo(1f, tween(durationMillis = 2000, easing = LinearEasing))
            sound.play(Sfx.STAMP)
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            stampScale.snapTo(1.8f)
            stampScale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium))
        }
        stampScale.snapTo(1f)
        panelVisible = true
    }
    val leftSlots = remember(state.rowClues) { state.rowClues.maxOfOrNull { it.size }?.coerceAtLeast(1) ?: 1 }
    val topSlots = remember(state.colClues) { state.colClues.maxOfOrNull { it.size }?.coerceAtLeast(1) ?: 1 }

    Column(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
            // 연출 초반: 풀던 보드(단서·격자·✕)를 그대로 깔고 흐리게 사라지게 한다
            val boardAlpha = 1f - segment(progress.value, 0f, 0.25f)
            if (board != null && boardAlpha > 0f) {
                BoardView(
                    puzzleKey = puzzle.id + "-reveal",
                    columns = puzzle.w,
                    rows = puzzle.h,
                    rowClues = state.rowClues,
                    colClues = state.colClues,
                    board = board,
                    palette = puzzle.palette.map(::parseHexColor),
                    isColor = puzzle.kind == "color",
                    highlight = null,
                    activeCell = -1,
                    enabled = false,
                    onStrokeStart = {}, onStrokeMove = {}, onStrokeEnd = {}, onStrokeCancel = {},
                    modifier = Modifier.fillMaxWidth().alpha(boardAlpha),
                )
            }
            RevealBoard(puzzle, leftSlots, topSlots, progress.value, Modifier.fillMaxSize())
            if (stampScale.value > 0f) {
                Stamp(
                    stringResource(R.string.stamp_text),
                    Modifier.align(Alignment.BottomEnd).padding(end = 8.dp, bottom = 4.dp).scale(stampScale.value),
                    size = 104.dp,
                )
            }
        }
        AnimatedVisibility(panelVisible, enter = expandVertically(tween(450)) + fadeIn(tween(450))) {
            // 버튼은 패널 위쪽에 두고 아래 여백을 넉넉히 → 하단 배너와 거리 확보
            Column(
                Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(puzzle.title.text(), style = MaterialTheme.typography.headlineMedium, color = note.ink, textAlign = TextAlign.Center)
                Row(horizontalArrangement = Arrangement.spacedBy(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    ResultItem(PixelIcons.Clock, stringResource(R.string.game_time), formatElapsed(state.elapsedMs))
                    ResultItem(PixelIcons.Bulb, stringResource(R.string.game_hints_label), state.hintsUsed.toString())
                }
                Spacer(Modifier.height(2.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StickerButton(stringResource(R.string.game_back_to_list), onBack, style = StickerStyle.Plain, modifier = Modifier.weight(1f))
                    state.nextId?.let { next ->
                        StickerButton(stringResource(R.string.game_next), { onNext(next) }, icon = PixelIcons.Play, modifier = Modifier.weight(1f))
                    }
                }
                TextButton(onClick = { confirmRestart = true }) { Text(stringResource(R.string.game_restart), color = note.inkSoft) }
            }
        }
    }
    if (confirmRestart) {
        AlertDialog(
            onDismissRequest = { confirmRestart = false },
            containerColor = note.card,
            text = { Text(stringResource(R.string.game_restart_confirm)) },
            confirmButton = { TextButton(onClick = { confirmRestart = false; onRestart() }) { Text(stringResource(R.string.ok), color = note.redPencil) } },
            dismissButton = { TextButton(onClick = { confirmRestart = false }) { Text(stringResource(R.string.cancel), color = note.inkSoft) } },
        )
    }
}

@Composable
private fun ResultItem(icon: PixelArt, label: String, value: String) {
    val note = LocalNoteColors.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        PixelIcon(icon, size = 20.dp, tint = note.inkSoft)
        Spacer(Modifier.width(6.dp))
        Text("$label ", style = hand18, color = note.inkSoft)
        Text(value, style = hand22, color = note.ink)
    }
}

