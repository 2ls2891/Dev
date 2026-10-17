package com.hyunnyApps.nonogram.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.hyunnyApps.nonogram.R

/** 규칙 한 항목: 제목 + 설명 + 그림 예시 */
@Composable
fun RuleItem(number: Int, title: String, text: String, example: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row {
            Text("$number. ", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Text(title, style = MaterialTheme.typography.titleMedium)
        }
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(2.dp))
        example()
    }
}

/** 게임 화면 도움말·설정의 규칙 설명 (튜토리얼과 같은 예시를 쓴다) */
@Composable
fun RulesContent(includeColor: Boolean, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(22.dp)) {
        RuleItem(1, stringResource(R.string.rules_1_title), stringResource(R.string.rules_1_text)) {
            RuleExample(listOf(3), ".###.")
        }
        RuleItem(2, stringResource(R.string.rules_2_title), stringResource(R.string.rules_2_text)) {
            RuleExample(listOf(2, 1), "##r#.")
        }
        RuleItem(3, stringResource(R.string.rules_3_title), stringResource(R.string.rules_3_text)) {
            RuleExample(listOf(4), "x####")
        }
        RuleItem(4, stringResource(R.string.rules_4_title), stringResource(R.string.rules_4_text)) {}
        if (includeColor) {
            val red = Color(0xFFE5484D)
            val blue = Color(0xFF1E88E5)
            RuleItem(5, stringResource(R.string.rules_color_title), stringResource(R.string.rules_color_text)) {
                RuleExample(listOf(2, 2), ".RRBB", clueColors = listOf(red, blue), fillColors = mapOf('R' to red, 'B' to blue))
            }
        }
    }
}
