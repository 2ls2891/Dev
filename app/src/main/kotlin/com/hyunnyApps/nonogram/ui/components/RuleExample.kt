package com.hyunnyApps.nonogram.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.hyunnyApps.nonogram.ui.theme.LocalBoardColors

/**
 * 규칙 설명용 한 줄 예시: 단서 숫자 + 칸들.
 * cells: '#' = 칠함, 'x' = ✕ 표시, '.' = 빈칸, 'r' = 강조된 빈칸(띄어야 하는 칸)
 */
@Composable
fun RuleExample(clue: List<Int>, cells: String, modifier: Modifier = Modifier, cell: Dp = 30.dp, clueColors: List<Color>? = null, fillColors: Map<Char, Color> = emptyMap()) {
    val board = LocalBoardColors.current
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Row(
            Modifier.background(board.clueBackground).width(cell * maxOf(2, clue.size)).size(cell * maxOf(2, clue.size), cell),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            clue.forEachIndexed { i, n ->
                val bg = clueColors?.getOrNull(i)
                Box(
                    Modifier.size(cell).then(if (bg != null) Modifier.background(bg) else Modifier),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(n.toString(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = if (bg != null) Color.White else board.clueText)
                }
            }
        }
        cells.forEach { ch ->
            val fill = fillColors[ch] ?: if (ch == '#') board.monoFill else null
            Box(
                Modifier
                    .size(cell)
                    .background(
                        when {
                            fill != null -> fill
                            ch == 'r' -> board.highlight.copy(alpha = 0.25f)
                            else -> board.background
                        }
                    )
                    .border(0.5.dp, board.gridLine),
                contentAlignment = Alignment.Center,
            ) {
                if (ch == 'x') Icon(AppIcons.Cross, null, tint = board.cross, modifier = Modifier.size(cell * 0.6f))
            }
        }
    }
}
