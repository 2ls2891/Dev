package com.hyunnyApps.nonogram.ui.game

import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hyunnyApps.nonogram.R
import com.hyunnyApps.nonogram.ads.RewardedAdManager
import com.hyunnyApps.nonogram.data.DAILY
import com.hyunnyApps.nonogram.data.PlayerRepository
import com.hyunnyApps.nonogram.ui.components.AppIcons
import com.hyunnyApps.nonogram.ui.components.LevelChip
import com.hyunnyApps.nonogram.ui.components.PixelPicture
import com.hyunnyApps.nonogram.ui.components.RulesContent
import com.hyunnyApps.nonogram.ui.components.formatElapsed
import com.hyunnyApps.nonogram.ui.components.parseHexColor
import com.hyunnyApps.nonogram.ui.components.text
import com.hyunnyApps.nonogram.ui.theme.LocalBoardColors
import com.hyunnyApps.nonogram.core.game.Mark

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameScreen(
    rewarded: RewardedAdManager,
    onBack: () -> Unit,
    onOpenPuzzle: (source: String, puzzleId: String) -> Unit,
    viewModel: GameViewModel = viewModel(factory = GameViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val activity = LocalActivity.current
    var showRules by rememberSaveable { mutableStateOf(false) }

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

    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.statusBars)) {
            Column(Modifier.fillMaxSize()) {
                GameTopBar(state, onBack) { showRules = true }
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    when {
                        state.loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                        state.error -> Text(stringResource(R.string.error_load), Modifier.align(Alignment.Center))
                        state.completed -> CompletedContent(state, viewModel::restart, onBack) { next -> onOpenPuzzle(state.source, next) }
                        else -> PlayContent(state, viewModel)
                    }
                }
            }
            SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
        }
    }

    if (showRules) {
        ModalBottomSheet(onDismissRequest = { showRules = false }) {
            Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).padding(bottom = 32.dp)) {
                Text(stringResource(R.string.rules_title), style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(16.dp))
                RulesContent(includeColor = state.puzzle?.kind == "color")
            }
        }
    }

    if (state.rewardOfferVisible) {
        AlertDialog(
            onDismissRequest = viewModel::dismissRewardOffer,
            icon = { Icon(AppIcons.Lightbulb, null) },
            title = { Text(stringResource(R.string.reward_title)) },
            text = { Text(pluralStringResource(R.plurals.reward_text, PlayerRepository.REWARDED_HINTS, PlayerRepository.REWARDED_HINTS)) },
            confirmButton = {
                Button(onClick = {
                    val shown = activity != null && rewarded.show(activity, onRewarded = viewModel::onRewardEarned)
                    viewModel.dismissRewardOffer()
                    if (!shown) viewModel.onAdNotReady()
                }) {
                    Text(stringResource(if (state.rewardedReady) R.string.reward_watch else R.string.reward_loading))
                }
            },
            dismissButton = { TextButton(onClick = viewModel::dismissRewardOffer) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

@Composable
private fun GameTopBar(state: GameUiState, onBack: () -> Unit, onHelp: () -> Unit) {
    val p = state.puzzle
    Row(Modifier.fillMaxWidth().padding(start = 4.dp, end = 8.dp, top = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back)) }
        Column(Modifier.weight(1f)) {
            if (p != null) {
                Text(
                    when {
                        state.completed -> p.title.text()
                        state.source == DAILY -> stringResource(R.string.home_daily_title)
                        else -> stringResource(R.string.game_puzzle_number, state.position + 1)
                    },
                    style = MaterialTheme.typography.titleMedium,
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    LevelChip(p.difficulty.level)
                    Text(stringResource(R.string.size_format, p.w, p.h), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Row(
            Modifier.clip(RoundedCornerShape(50)).background(MaterialTheme.colorScheme.surface).padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(AppIcons.Timer, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(4.dp))
            Text(formatElapsed(state.elapsedMs), style = MaterialTheme.typography.labelLarge)
        }
        IconButton(onClick = onHelp) { Icon(AppIcons.Help, stringResource(R.string.help)) }
    }
}

@Composable
private fun PlayContent(state: GameUiState, viewModel: GameViewModel) {
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
    val total = board.rowDone.size + board.colDone.size
    val done = board.rowDone.count { it } + board.colDone.count { it }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.game_lines_done, done, total), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(12.dp))
            LinearProgressIndicator(
                progress = { if (total == 0) 0f else done.toFloat() / total },
                modifier = Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(3.dp)),
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                drawStopIndicator = {},
                gapSize = 0.dp,
            )
        }
        if (state.firstTimer && !board.marks.any { it != 0 }) {
            Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.primaryContainer) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        AnnotatedString.fromHtml(stringResource(R.string.game_coach)),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    val p = state.puzzle
                    if (p != null && maxOf(p.w, p.h) >= 20) {
                        Text(stringResource(R.string.game_coach_zoom), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
            }
        }
    }
}

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
    val cs = MaterialTheme.colorScheme
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (isColor) {
            // 컬러 퍼즐: 색 선택 + 빈칸 표시
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                colors.forEachIndexed { i, color ->
                    val selected = tool == i + 1
                    Box(
                        Modifier.size(48.dp).clip(CircleShape)
                            .border(if (selected) BorderStroke(3.dp, cs.primary) else BorderStroke(1.dp, cs.outline), CircleShape)
                            .clickable(onClickLabel = stringResource(R.string.game_tool_fill)) { onTool(i + 1) }
                            .padding(6.dp).clip(CircleShape).background(color),
                    )
                }
                ToolSegment(tool == Mark.CROSS, AppIcons.Cross, stringResource(R.string.game_tool_cross), Modifier) { onTool(Mark.CROSS) }
            }
        } else {
            // 흑백 퍼즐: 칠하기 / 빈칸 표시 두 가지 도구
            Row(
                Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).background(cs.surfaceVariant).padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                ToolSegment(tool == 1, AppIcons.Square, stringResource(R.string.game_tool_fill), Modifier.weight(1f), iconTint = colors.first()) { onTool(1) }
                ToolSegment(tool == Mark.CROSS, AppIcons.Cross, stringResource(R.string.game_tool_cross), Modifier.weight(1f)) { onTool(Mark.CROSS) }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ActionButton(AppIcons.Undo, stringResource(R.string.game_undo), canUndo, Modifier.weight(1f), onUndo)
            ActionButton(AppIcons.Redo, stringResource(R.string.game_redo), canRedo, Modifier.weight(1f), onRedo)
            ActionButton(AppIcons.Lightbulb, stringResource(R.string.game_hint) + " $hints", true, Modifier.weight(1f), onHint, highlight = true)
        }
    }
}

@Composable
private fun ToolSegment(selected: Boolean, icon: ImageVector, label: String, modifier: Modifier, iconTint: Color? = null, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Row(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) cs.surface else Color.Transparent)
            .then(if (selected) Modifier.border(2.dp, cs.primary, RoundedCornerShape(12.dp)) else Modifier)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, Modifier.size(20.dp), tint = iconTint ?: if (selected) cs.onSurface else cs.onSurfaceVariant)
        Spacer(Modifier.width(8.dp))
        Text(label, style = MaterialTheme.typography.labelLarge, color = if (selected) cs.onSurface else cs.onSurfaceVariant)
    }
}

@Composable
private fun ActionButton(icon: ImageVector, label: String, enabled: Boolean, modifier: Modifier, onClick: () -> Unit, highlight: Boolean = false) {
    val cs = MaterialTheme.colorScheme
    FilledTonalButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(48.dp),
        contentPadding = PaddingValues(horizontal = 8.dp),
        colors = if (highlight) androidx.compose.material3.ButtonDefaults.filledTonalButtonColors(containerColor = cs.secondaryContainer, contentColor = cs.onSecondaryContainer)
        else androidx.compose.material3.ButtonDefaults.filledTonalButtonColors(containerColor = cs.surface, contentColor = cs.onSurface),
    ) {
        Icon(icon, null, Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelLarge, maxLines = 1)
    }
}

@Composable
private fun CompletedContent(state: GameUiState, onRestart: () -> Unit, onBack: () -> Unit, onNext: (String) -> Unit) {
    val puzzle = state.puzzle ?: return
    val board = state.board
    val cs = MaterialTheme.colorScheme
    val haptic = LocalHapticFeedback.current
    var confirmRestart by rememberSaveable { mutableStateOf(false) }
    // 막 완성했으면 색 입히기 연출 후 결과 패널, 다시 열었으면 바로 결과
    val animate = state.justCompleted
    val progress = remember(puzzle.id) { Animatable(if (animate) 0f else 1f) }
    var panelVisible by rememberSaveable(puzzle.id) { mutableStateOf(!animate) }
    LaunchedEffect(puzzle.id, animate) {
        if (animate && progress.value < 1f) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            progress.animateTo(1f, tween(durationMillis = 2000, easing = LinearEasing))
        }
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
        }
        AnimatedVisibility(panelVisible, enter = expandVertically(tween(450)) + fadeIn(tween(450))) {
            // 버튼은 패널 위쪽에 두고 아래 여백을 넉넉히 → 하단 배너와 거리 확보
            Column(
                Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("🎉 " + stringResource(R.string.game_completed) + "  " + puzzle.title.text(), style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ResultTile(stringResource(R.string.game_time), formatElapsed(state.elapsedMs))
                    ResultTile(stringResource(R.string.game_hints_label), state.hintsUsed.toString())
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f).height(52.dp)) { Text(stringResource(R.string.game_back_to_list)) }
                    state.nextId?.let { next ->
                        Button(onClick = { onNext(next) }, modifier = Modifier.weight(1f).height(52.dp)) {
                            Text(stringResource(R.string.game_next), style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
                TextButton(onClick = { confirmRestart = true }) { Text(stringResource(R.string.game_restart), color = cs.onSurfaceVariant) }
            }
        }
    }
    if (confirmRestart) {
        AlertDialog(
            onDismissRequest = { confirmRestart = false },
            text = { Text(stringResource(R.string.game_restart_confirm)) },
            confirmButton = { TextButton(onClick = { confirmRestart = false; onRestart() }) { Text(stringResource(R.string.ok)) } },
            dismissButton = { TextButton(onClick = { confirmRestart = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

@Composable
private fun ResultTile(label: String, value: String) {
    Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
    }
}
