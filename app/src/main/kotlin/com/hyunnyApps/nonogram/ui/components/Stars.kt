package com.hyunnyApps.nonogram.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hyunnyApps.nonogram.R
import com.hyunnyApps.nonogram.ui.theme.LevelColors

@StringRes
fun levelName(level: Int): Int = when (level.coerceIn(1, 5)) {
    1 -> R.string.level_1
    2 -> R.string.level_2
    3 -> R.string.level_3
    4 -> R.string.level_4
    else -> R.string.level_5
}

fun levelColor(level: Int): Color = LevelColors[level.coerceIn(1, 5) - 1]

/** 난이도를 말로 보여주는 작은 칩 (예: "보통") */
@Composable
fun LevelChip(level: Int, modifier: Modifier = Modifier, onDark: Boolean = false) {
    val color = levelColor(level)
    Text(
        stringResource(levelName(level)),
        modifier = modifier
            .background(if (onDark) Color.White.copy(alpha = 0.2f) else color.copy(alpha = 0.14f), RoundedCornerShape(50))
            .padding(horizontal = 8.dp, vertical = 2.dp),
        color = if (onDark) Color.White else color,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
    )
}

fun formatElapsed(ms: Long): String {
    val total = ms / 1000
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}
