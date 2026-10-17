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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// 디자인 원칙: 종이처럼 따뜻한 배경 + 잉크색 칸 + 인디고 포인트 하나. 장식보다 여백과 대비.
private val Indigo = Color(0xFF4F46E5)
private val Ink = Color(0xFF1E1B2E)
private val Paper = Color(0xFFF5F2EB)

private val Light = lightColorScheme(
    primary = Indigo,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE4E2FF),
    onPrimaryContainer = Color(0xFF221C8F),
    secondary = Color(0xFFFF7A45),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFE6DA),
    onSecondaryContainer = Color(0xFF7A2E0E),
    tertiary = Color(0xFF16A36A),
    background = Paper,
    onBackground = Ink,
    surface = Color.White,
    onSurface = Ink,
    surfaceVariant = Color(0xFFEDE8DE),
    onSurfaceVariant = Color(0xFF5E5869),
    surfaceContainerLow = Color(0xFFFAF8F3),
    surfaceContainer = Color.White,
    surfaceContainerHigh = Color.White,
    outline = Color(0xFFCFC8BA),
    outlineVariant = Color(0xFFE3DDD1),
)

private val Dark = darkColorScheme(
    primary = Color(0xFFA5A1FF),
    onPrimary = Color(0xFF1C168A),
    primaryContainer = Color(0xFF3730A3),
    onPrimaryContainer = Color(0xFFE4E2FF),
    secondary = Color(0xFFFF9A6B),
    onSecondary = Color(0xFF4A1A05),
    secondaryContainer = Color(0xFF5C2A12),
    onSecondaryContainer = Color(0xFFFFDCCB),
    tertiary = Color(0xFF4ADE9B),
    background = Color(0xFF131218),
    onBackground = Color(0xFFECE9F5),
    surface = Color(0xFF1D1C24),
    onSurface = Color(0xFFECE9F5),
    surfaceVariant = Color(0xFF29272F),
    onSurfaceVariant = Color(0xFFB9B4C6),
    surfaceContainerLow = Color(0xFF18171E),
    surfaceContainer = Color(0xFF1D1C24),
    surfaceContainerHigh = Color(0xFF24232C),
    outline = Color(0xFF4A4756),
    outlineVariant = Color(0xFF34323D),
)

/** 보드 전용 색. 흑백 퍼즐 채움색은 테마에 맞추고, 컬러 퍼즐은 팔레트 색을 그대로 쓴다 */
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
    background = Color.White,
    gridLine = Color(0xFFE2DDD2),
    gridLineBold = Color(0xFF9C9587),
    monoFill = Ink,
    cross = Color(0xFFB4AEA2),
    clueText = Ink,
    clueDone = Color(0xFFC9C3B7),
    clueBackground = Color(0xFFF5F2EB),
    activeLine = Color(0x144F46E5),
    activeClue = Color(0x334F46E5),
    highlight = Color(0xFFFF7A45),
)

private val DarkBoard = BoardColors(
    background = Color(0xFF1D1C24),
    gridLine = Color(0xFF34323D),
    gridLineBold = Color(0xFF6E6A7C),
    monoFill = Color(0xFFECE9F5),
    cross = Color(0xFF6E6A7C),
    clueText = Color(0xFFECE9F5),
    clueDone = Color(0xFF4F4C5B),
    clueBackground = Color(0xFF18171E),
    activeLine = Color(0x1FA5A1FF),
    activeClue = Color(0x40A5A1FF),
    highlight = Color(0xFFFF9A6B),
)

/** 난이도 1~5 색 (입문 → 전문가) */
val LevelColors = listOf(
    Color(0xFF16A36A),
    Color(0xFF1E88E5),
    Color(0xFF6D5BD0),
    Color(0xFFF08A24),
    Color(0xFFE5484D),
)

val LocalBoardColors = staticCompositionLocalOf { LightBoard }

/** 칠하기 색 선택지. 다크 모드에서는 밝은 쪽 색을 써서 대비를 유지한다 */
data class InkOption(val light: Color, val dark: Color)

val InkOptions = listOf(
    InkOption(Ink, Color(0xFFECE9F5)), // 먹색 (기본)
    InkOption(Color(0xFF4F46E5), Color(0xFFA5A1FF)), // 인디고
    InkOption(Color(0xFF15803D), Color(0xFF4ADE80)), // 숲
    InkOption(Color(0xFF9F1239), Color(0xFFFB7185)), // 와인
    InkOption(Color(0xFF0369A1), Color(0xFF38BDF8)), // 바다
    InkOption(Color(0xFF7C4A1E), Color(0xFFD6A274)), // 코코아
)

private val AppTypography = Typography().let { t ->
    t.copy(
        headlineLarge = t.headlineLarge.copy(fontWeight = FontWeight.ExtraBold, fontSize = 30.sp, letterSpacing = (-0.5).sp),
        headlineSmall = t.headlineSmall.copy(fontWeight = FontWeight.Bold),
        titleLarge = t.titleLarge.copy(fontWeight = FontWeight.Bold),
        titleMedium = t.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        labelLarge = t.labelLarge.copy(fontWeight = FontWeight.SemiBold),
    )
}

private val AppShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
)

@Composable
fun NonogramTheme(dark: Boolean = isSystemInDarkTheme(), inkIndex: Int = 0, content: @Composable () -> Unit) {
    val ink = InkOptions.getOrElse(inkIndex) { InkOptions[0] }
    val board = (if (dark) DarkBoard else LightBoard).copy(monoFill = if (dark) ink.dark else ink.light)
    CompositionLocalProvider(LocalBoardColors provides board) {
        MaterialTheme(
            colorScheme = if (dark) Dark else Light,
            typography = AppTypography,
            shapes = AppShapes,
            content = content,
        )
    }
}
