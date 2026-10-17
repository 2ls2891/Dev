package nonogram.app.ui.game

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import nonogram.app.R
import nonogram.app.ads.RewardedAdManager
import nonogram.app.data.DAILY
import nonogram.app.data.PlayerRepository
import nonogram.app.ui.components.DifficultyStars
import nonogram.app.ui.components.PixelPicture
import nonogram.app.ui.components.formatElapsed
import nonogram.app.ui.components.parseHexColor
import nonogram.app.ui.components.text
import nonogram.app.ui.theme.LocalBoardColors
import nonogram.core.game.Mark

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

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                title = {
                    val p = state.puzzle
                    if (p != null) {
                        Column {
                            Text(
                                when {
                                    state.completed -> p.title.text()
                                    state.source == DAILY -> stringResource(R.string.home_daily_title)
                                    else -> stringResource(R.string.game_puzzle_number, state.position + 1)
                                },
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                DifficultyStars(p.difficulty.level)
                                Text("  ${p.w}×${p.h}", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                },
                actions = {
                    Text(
                        formatElapsed(state.elapsedMs),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(end = 16.dp),
                    )
                },
            )
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when {
                state.loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                state.error -> Text(stringResource(R.string.error_load), Modifier.align(Alignment.Center))
                state.completed -> CompletedContent(state, viewModel::restart, onBack) { next -> onOpenPuzzle(state.source, next) }
                else -> PlayContent(state, viewModel)
            }
        }
    }

    if (state.rewardOfferVisible) {
        AlertDialog(
            onDismissRequest = viewModel::dismissRewardOffer,
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
private fun PlayContent(state: GameUiState, viewModel: GameViewModel) {
    val puzzle = state.puzzle ?: return
    val board = state.board ?: return
    val boardColors = LocalBoardColors.current
    val isColor = puzzle.kind == "color"
    val palette = remember(puzzle.id) { puzzle.palette.map(::parseHexColor) }
    Column(Modifier.fillMaxSize()) {
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
            enabled = !state.completed,
            onStrokeStart = viewModel::onStrokeStart,
            onStrokeMove = viewModel::onStrokeMove,
            onStrokeEnd = viewModel::onStrokeEnd,
            onStrokeCancel = viewModel::onStrokeCancel,
            modifier = Modifier.weight(1f).fillMaxWidth().padding(8.dp),
        )
        GameToolbar(
            tool = state.tool,
            colors = if (isColor) palette else listOf(boardColors.monoFill),
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
private fun GameToolbar(
    tool: Int,
    colors: List<Color>,
    canUndo: Boolean,
    canRedo: Boolean,
    hints: Int,
    onTool: (Int) -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onHint: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            Modifier.weight(1f).horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            colors.forEachIndexed { i, color ->
                ToolSwatch(selected = tool == i + 1, onClick = { onTool(i + 1) }, label = stringResource(R.string.game_tool_fill)) {
                    Box(Modifier.size(24.dp).clip(RoundedCornerShape(4.dp)).background(color))
                }
            }
            ToolSwatch(selected = tool == Mark.CROSS, onClick = { onTool(Mark.CROSS) }, label = stringResource(R.string.game_tool_cross)) {
                Icon(Icons.Filled.Close, contentDescription = null, tint = LocalBoardColors.current.cross)
            }
        }
        IconButton(onClick = onUndo, enabled = canUndo) { Text("↶", style = MaterialTheme.typography.titleLarge) }
        IconButton(onClick = onRedo, enabled = canRedo) { Text("↷", style = MaterialTheme.typography.titleLarge) }
        IconButton(onClick = onHint) {
            BadgedBox(badge = { Badge { Text(hints.toString()) } }) {
                Text("💡", style = MaterialTheme.typography.titleLarge)
            }
        }
    }
}

@Composable
private fun ToolSwatch(selected: Boolean, onClick: () -> Unit, label: String, content: @Composable () -> Unit) {
    val border = if (selected) BorderStroke(3.dp, MaterialTheme.colorScheme.primary) else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    Box(
        Modifier
            .size(44.dp)
            .clip(CircleShape)
            .border(border, CircleShape)
            .clickable(onClickLabel = label, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable
private fun CompletedContent(state: GameUiState, onRestart: () -> Unit, onBack: () -> Unit, onNext: (String) -> Unit) {
    val puzzle = state.puzzle ?: return
    var confirmRestart by rememberSaveable { mutableStateOf(false) }
    // 버튼은 화면 위쪽~가운데에 두어 하단 배너와 멀리 떨어뜨린다
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(R.string.game_completed), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        PixelPicture(
            puzzle.reveal,
            Modifier.widthIn(max = 280.dp).fillMaxWidth().aspectRatio(puzzle.w.toFloat() / puzzle.h),
        )
        Text(puzzle.title.text(), style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Text(
            stringResource(R.string.game_time, formatElapsed(state.elapsedMs)) + "   " +
                pluralStringResource(R.plurals.game_hints_used, state.hintsUsed, state.hintsUsed),
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(4.dp))
        state.nextId?.let { next ->
            Button(onClick = { onNext(next) }, modifier = Modifier.fillMaxWidth(0.7f)) { Text(stringResource(R.string.game_next)) }
        }
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth(0.7f)) { Text(stringResource(R.string.game_back_to_list)) }
        TextButton(onClick = { confirmRestart = true }) { Text(stringResource(R.string.game_restart)) }
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

