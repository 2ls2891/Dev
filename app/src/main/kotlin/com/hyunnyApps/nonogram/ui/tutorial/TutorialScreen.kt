package com.hyunnyApps.nonogram.ui.tutorial

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.hyunnyApps.nonogram.R
import com.hyunnyApps.nonogram.core.Grid
import com.hyunnyApps.nonogram.core.format.Picture
import com.hyunnyApps.nonogram.core.game.GameSession
import com.hyunnyApps.nonogram.core.game.Mark
import com.hyunnyApps.nonogram.ui.components.PixelPicture
import com.hyunnyApps.nonogram.ui.components.RuleItem
import com.hyunnyApps.nonogram.ui.components.RuleExample
import com.hyunnyApps.nonogram.ui.components.PaperCard
import com.hyunnyApps.nonogram.ui.components.PixelIcons
import com.hyunnyApps.nonogram.ui.components.Stamp
import com.hyunnyApps.nonogram.ui.components.StickerButton
import com.hyunnyApps.nonogram.ui.components.StickerStyle
import com.hyunnyApps.nonogram.ui.theme.LocalNoteColors
import androidx.compose.foundation.layout.offset
import com.hyunnyApps.nonogram.ui.game.BoardUi
import com.hyunnyApps.nonogram.ui.game.BoardView

/** 연습용 5×5 하트 (줄 추론만으로 풀리는지 core 테스트로 검증됨) */
val TutorialHeart = listOf(
    ".1.1.",
    "11111",
    "11111",
    ".111.",
    "..1..",
)

private val HeartPicture = Picture(listOf("#E5484D"), TutorialHeart)

private const val STEPS = 5 // 소개, 규칙 1~3, 연습

@Composable
fun TutorialScreen(onMarkSeen: suspend () -> Unit, onDone: () -> Unit, initialStep: Int = 0) {
    LaunchedEffect(Unit) { onMarkSeen() }
    var step by rememberSaveable { mutableIntStateOf(initialStep) }
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.statusBars).padding(horizontal = 24.dp)) {
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.tutorial_title), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                TextButton(onClick = onDone) { Text(stringResource(R.string.tutorial_skip), color = LocalNoteColors.current.inkSoft) }
            }
            StepDots(step)
            Spacer(Modifier.height(24.dp))
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = if (step < STEPS - 1) Alignment.Center else Alignment.TopStart) {
                when (step) {
                    0 -> IntroStep()
                    1 -> RuleStep { RuleItem(1, stringResource(R.string.rules_1_title), stringResource(R.string.rules_1_text)) { Examples1() } }
                    2 -> RuleStep { RuleItem(2, stringResource(R.string.rules_2_title), stringResource(R.string.rules_2_text)) { Examples2() } }
                    3 -> RuleStep { RuleItem(3, stringResource(R.string.rules_3_title), stringResource(R.string.rules_3_text)) { RuleExample(listOf(4), "x####") } }
                    else -> PracticeStep(onDone)
                }
            }
            if (step < STEPS - 1) {
                StickerButton(stringResource(R.string.tutorial_next), { step++ }, modifier = Modifier.fillMaxWidth())
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun StepDots(step: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(STEPS) { i ->
            Box(
                Modifier.height(6.dp).width(if (i == step) 24.dp else 6.dp).clip(CircleShape)
                    .background(if (i <= step) LocalNoteColors.current.redPencil else LocalNoteColors.current.paperLineStrong),
            )
        }
    }
}

@Composable
private fun IntroStep() {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(stringResource(R.string.tutorial_intro_title), style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        Text(stringResource(R.string.tutorial_intro_text), style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                RuleExample(listOf(1, 1), ".#.#.", cell = 22.dp)
                RuleExample(listOf(5), "#####", cell = 22.dp)
                RuleExample(listOf(5), "#####", cell = 22.dp)
                RuleExample(listOf(3), ".###.", cell = 22.dp)
                RuleExample(listOf(1), "..#..", cell = 22.dp)
            }
            Text("→", style = MaterialTheme.typography.headlineMedium)
            PaperCard(Modifier.size(104.dp), rotation = 2f, tape = true) {
                PixelPicture(HeartPicture, Modifier.size(94.dp).padding(14.dp))
            }
        }
    }
}

@Composable
private fun RuleStep(content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth()) { content() }
}

@Composable
private fun Examples1() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        RuleExample(listOf(3), ".###.")
        RuleExample(listOf(5), "#####")
        RuleExample(listOf(1), "...#.")
    }
}

@Composable
private fun Examples2() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        RuleExample(listOf(2, 1), "##r#.")
        RuleExample(listOf(1, 1, 1), "#r#r#")
    }
}

@Composable
private fun PracticeStep(onDone: () -> Unit) {
    val session = remember { GameSession(Grid.fromRows(TutorialHeart)) }
    var version by remember { mutableLongStateOf(0L) }
    var tool by rememberSaveable { mutableIntStateOf(1) }
    var active by remember { mutableIntStateOf(-1) }
    val board = remember(version) {
        BoardUi(
            session.marksSnapshot(), session.version,
            BooleanArray(session.height) { session.isRowSatisfied(it) },
            BooleanArray(session.width) { session.isColSatisfied(it) },
        )
    }
    val solved = remember(version) { session.isSolved() }
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        if (solved) {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(stringResource(R.string.tutorial_done_title), style = MaterialTheme.typography.headlineSmall)
                Box {
                    PaperCard(Modifier.size(170.dp), rotation = -2f, tape = true) {
                        PixelPicture(HeartPicture, Modifier.size(160.dp).padding(24.dp))
                    }
                    Stamp(stringResource(R.string.stamp_text), Modifier.align(Alignment.BottomEnd).offset(x = 40.dp, y = 20.dp), size = 84.dp)
                }
                Spacer(Modifier.height(12.dp))
                StickerButton(stringResource(R.string.tutorial_done_button), onDone, icon = PixelIcons.Play, modifier = Modifier.fillMaxWidth())
            }
            return@Column
        }
        Text(stringResource(R.string.tutorial_practice_title), style = MaterialTheme.typography.headlineSmall)
        Text(stringResource(R.string.tutorial_practice_text), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        BoardView(
            puzzleKey = "tutorial",
            columns = 5,
            rows = 5,
            rowClues = session.rowClues,
            colClues = session.colClues,
            board = board,
            palette = emptyList(),
            isColor = false,
            highlight = null,
            activeCell = active,
            enabled = true,
            onStrokeStart = { session.beginStroke(it, tool); active = it; version++ },
            onStrokeMove = { session.extendStroke(it); active = it; version++ },
            onStrokeEnd = { session.endStroke(); active = -1; version++ },
            onStrokeCancel = { session.cancelStroke(); active = -1; version++ },
            modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StickerButton(
                stringResource(R.string.game_tool_fill), { tool = 1 },
                style = if (tool == 1) StickerStyle.Highlight else StickerStyle.Plain, selected = tool == 1,
                icon = PixelIcons.Square, height = 48.dp, modifier = Modifier.weight(1f),
            )
            StickerButton(
                stringResource(R.string.game_tool_cross), { tool = Mark.CROSS },
                style = if (tool == Mark.CROSS) StickerStyle.Highlight else StickerStyle.Plain, selected = tool == Mark.CROSS,
                icon = PixelIcons.Cross, height = 48.dp, modifier = Modifier.weight(1f),
            )
        }
    }
}


