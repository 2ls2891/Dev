package com.hyunnyApps.nonogram.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hyunnyApps.nonogram.R

/*
 * 컨셉: "모눈종이 노트"
 *  - 크림색 모눈종이 위에 만년필 잉크(네이비)로 칠하고, 빨간 색연필로 강조, 형광펜으로 표시
 *  - 제목·숫자 강조는 손글씨체(Gaegu), 본문은 시스템 폰트로 가독성 유지
 *  - 밤에는 "남색 밤 노트" (분필 같은 흰 잉크)
 */

/** 노트 고유 색. MaterialTheme 색과 별도로 컴포넌트가 직접 쓴다 */
@Immutable
data class NoteColors(
    val paper: Color,
    val paperLine: Color,
    val paperLineStrong: Color,
    val card: Color,
    val ink: Color,
    val inkSoft: Color,
    val redPencil: Color,
    val highlighter: Color,
    val tape: Color,
    val shadow: Color,
    val success: Color,
)

private val LightNote = NoteColors(
    paper = Color(0xFFF7F2E6),
    paperLine = Color(0xFFE1E7E8),
    paperLineStrong = Color(0xFFCFDADD),
    card = Color(0xFFFFFDF8),
    ink = Color(0xFF26304F),
    inkSoft = Color(0xFF6A7088),
    redPencil = Color(0xFFE0533D),
    highlighter = Color(0xFFFFE066),
    tape = Color(0xCCEAD7A4),
    shadow = Color(0xFF26304F),
    success = Color(0xFF3BA776),
)

private val DarkNote = NoteColors(
    paper = Color(0xFF1C2030),
    paperLine = Color(0xFF262B3E),
    paperLineStrong = Color(0xFF30364C),
    card = Color(0xFF242A3D),
    ink = Color(0xFFEDEAF7),
    inkSoft = Color(0xFFA9AFC4),
    redPencil = Color(0xFFFF7A63),
    highlighter = Color(0xFFFFE066),
    tape = Color(0x99B8A673),
    shadow = Color(0xFF0E111B),
    success = Color(0xFF5BD39A),
)

val LocalNoteColors = staticCompositionLocalOf { LightNote }

/** 보드 전용 색 */
@Immutable
data class BoardColors(
    val background: Color,
    val gridLine: Color,
    val gridLineBold: Color,
    val monoFill: Color,
    val cross: Color,
    val clueText: Color,
    val clueDone: Color,
    val clueBackground: Color,
    val activeLine: Color,
    val activeClue: Color,
    val highlight: Color,
)

private val LightBoard = BoardColors(
    background = Color(0xFFFFFDF8),
    gridLine = Color(0xFFD9E1E4),
    gridLineBold = Color(0xFF8E99AE),
    monoFill = LightNote.ink,
    cross = Color(0xFFB0A898),
    clueText = LightNote.ink,
    clueDone = Color(0xFFB9B5AA),
    clueBackground = Color.Transparent,
    activeLine = Color(0x33FFE066),
    activeClue = Color(0x99FFE066),
    highlight = LightNote.redPencil,
)

private val DarkBoard = BoardColors(
    background = DarkNote.card,
    gridLine = Color(0xFF353C54),
    gridLineBold = Color(0xFF6C7593),
    monoFill = DarkNote.ink,
    cross = Color(0xFF6C7593),
    clueText = DarkNote.ink,
    clueDone = Color(0xFF586079),
    clueBackground = Color.Transparent,
    activeLine = Color(0x26FFE066),
    activeClue = Color(0x55FFE066),
    highlight = DarkNote.redPencil,
)

/** 난이도 1~5 색 (입문 → 전문가), 색연필 톤 */
val LevelColors = listOf(
    Color(0xFF3BA776),
    Color(0xFF3B82C4),
    Color(0xFF7B61C9),
    Color(0xFFE38B2C),
    Color(0xFFE0533D),
)

val LocalBoardColors = staticCompositionLocalOf { LightBoard }

/** 칠하기 색 선택지. 다크 모드에서는 밝은 쪽 색을 써서 대비를 유지한다 */
data class InkOption(val light: Color, val dark: Color)

val InkOptions = listOf(
    InkOption(LightNote.ink, DarkNote.ink), // 만년필 (기본)
    InkOption(Color(0xFF3F4FD8), Color(0xFFA5A1FF)), // 인디고
    InkOption(Color(0xFF2E7D4F), Color(0xFF5BD39A)), // 숲
    InkOption(Color(0xFFA0283E), Color(0xFFFB7185)), // 와인
    InkOption(Color(0xFF1F6FA3), Color(0xFF5BC0F0)), // 바다
    InkOption(Color(0xFF7C4A1E), Color(0xFFD6A274)), // 코코아
)

/** 손글씨 제목체 (Gaegu, SIL OFL 1.1) */
val HandFont = FontFamily(Font(R.font.gaegu_bold, FontWeight.Bold))

private fun hand(size: Int, line: Int = size + 4) =
    TextStyle(fontFamily = HandFont, fontWeight = FontWeight.Bold, fontSize = size.sp, lineHeight = line.sp)

private val AppTypography = Typography().let { t ->
    t.copy(
        displayMedium = hand(46, 50),
        headlineLarge = hand(36, 40),
        headlineMedium = hand(30, 34),
        headlineSmall = hand(26, 30),
        titleLarge = hand(24, 28),
        titleMedium = t.titleMedium.copy(fontWeight = FontWeight.Bold),
        labelLarge = t.labelLarge.copy(fontWeight = FontWeight.Bold),
    )
}

private val AppShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
)

@Composable
fun NonogramTheme(dark: Boolean = isSystemInDarkTheme(), inkIndex: Int = 0, content: @Composable () -> Unit) {
    val note = if (dark) DarkNote else LightNote
    val ink = InkOptions.getOrElse(inkIndex) { InkOptions[0] }
    val board = (if (dark) DarkBoard else LightBoard).copy(monoFill = if (dark) ink.dark else ink.light)
    val scheme = if (dark) {
        darkColorScheme(
            primary = note.redPencil, onPrimary = Color.White,
            primaryContainer = Color(0xFF3A2A2E), onPrimaryContainer = note.ink,
            secondary = note.ink, onSecondary = note.paper,
            secondaryContainer = note.card, onSecondaryContainer = note.ink,
            tertiary = note.success,
            background = note.paper, onBackground = note.ink,
            surface = note.card, onSurface = note.ink,
            surfaceVariant = note.paperLineStrong, onSurfaceVariant = note.inkSoft,
            surfaceContainerLow = note.card, surfaceContainer = note.card, surfaceContainerHigh = note.card,
            surfaceContainerHighest = note.card,
            outline = note.inkSoft, outlineVariant = note.paperLineStrong,
        )
    } else {
        lightColorScheme(
            primary = note.redPencil, onPrimary = Color.White,
            primaryContainer = Color(0xFFFCE3DD), onPrimaryContainer = Color(0xFF6E1E12),
            secondary = note.ink, onSecondary = Color.White,
            secondaryContainer = Color(0xFFFFF4C2), onSecondaryContainer = note.ink,
            tertiary = note.success,
            background = note.paper, onBackground = note.ink,
            surface = note.card, onSurface = note.ink,
            surfaceVariant = Color(0xFFEDE6D4), onSurfaceVariant = note.inkSoft,
            surfaceContainerLow = note.card, surfaceContainer = note.card, surfaceContainerHigh = note.card,
            surfaceContainerHighest = note.card,
            outline = note.inkSoft, outlineVariant = Color(0xFFE2DBC8),
        )
    }
    CompositionLocalProvider(LocalBoardColors provides board, LocalNoteColors provides note) {
        MaterialTheme(colorScheme = scheme, typography = AppTypography, shapes = AppShapes, content = content)
    }
}
