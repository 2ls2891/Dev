package com.hyunnyApps.nonogram.ui.components

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
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

fun formatElapsed(ms: Long): String {
    val total = ms / 1000
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}
