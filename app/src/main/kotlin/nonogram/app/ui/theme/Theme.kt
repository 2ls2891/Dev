package nonogram.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

private val Accent = Color(0xFF5B5BD6)

private val Light = lightColorScheme(
    primary = Accent,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE3E2FF),
    onPrimaryContainer = Color(0xFF16137A),
    secondary = Color(0xFFE8833A),
    background = Color(0xFFFAF9FD),
    surface = Color(0xFFFAF9FD),
    surfaceVariant = Color(0xFFEDEBF4),
)

private val Dark = darkColorScheme(
    primary = Color(0xFFC2C1FF),
    onPrimary = Color(0xFF221E8F),
    primaryContainer = Color(0xFF3B3AA6),
    onPrimaryContainer = Color(0xFFE3E2FF),
    secondary = Color(0xFFFFB68A),
    background = Color(0xFF131318),
    surface = Color(0xFF131318),
    surfaceVariant = Color(0xFF2A2A33),
)

/** 보드 전용 색. 흑백 퍼즐의 채움색은 테마에 맞춰 바꾸고, 컬러 퍼즐은 팔레트 색을 그대로 쓴다 */
@Immutable
data class BoardColors(
    val background: Color,
    val gridLine: Color,
    val gridLineBold: Color,
    val monoFill: Color,
    val cross: Color,
    val clueText: Color,
    val clueDone: Color,
    val highlight: Color,
    val clueBackground: Color,
)

private val LightBoard = BoardColors(
    background = Color.White,
    gridLine = Color(0xFFD5D3E0),
    gridLineBold = Color(0xFF8E8BA3),
    monoFill = Color(0xFF26243A),
    cross = Color(0xFFA09DB5),
    clueText = Color(0xFF26243A),
    clueDone = Color(0xFFBDBACB),
    highlight = Accent,
    clueBackground = Color(0xFFF3F2F9),
)

private val DarkBoard = BoardColors(
    background = Color(0xFF1C1C24),
    gridLine = Color(0xFF3A3947),
    gridLineBold = Color(0xFF6E6C82),
    monoFill = Color(0xFFE6E4F2),
    cross = Color(0xFF77758A),
    clueText = Color(0xFFE6E4F2),
    clueDone = Color(0xFF55536A),
    highlight = Color(0xFFC2C1FF),
    clueBackground = Color(0xFF24242E),
)

val LocalBoardColors = staticCompositionLocalOf { LightBoard }

@Composable
fun NonogramTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    androidx.compose.runtime.CompositionLocalProvider(LocalBoardColors provides if (dark) DarkBoard else LightBoard) {
        MaterialTheme(colorScheme = if (dark) Dark else Light, content = content)
    }
}
