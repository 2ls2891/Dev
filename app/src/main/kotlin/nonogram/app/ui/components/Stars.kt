package nonogram.app.ui.components

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle

/** 난이도 1~5 별 표시 */
@Composable
fun DifficultyStars(level: Int, modifier: Modifier = Modifier) {
    val on = MaterialTheme.colorScheme.secondary
    val off = LocalContentColor.current.copy(alpha = 0.25f)
    Text(
        buildAnnotatedString {
            withStyle(SpanStyle(color = on)) { append("★".repeat(level.coerceIn(0, 5))) }
            withStyle(SpanStyle(color = off)) { append("★".repeat(5 - level.coerceIn(0, 5))) }
        },
        modifier = modifier,
        style = MaterialTheme.typography.labelMedium,
    )
}

fun formatElapsed(ms: Long): String {
    val total = ms / 1000
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}
