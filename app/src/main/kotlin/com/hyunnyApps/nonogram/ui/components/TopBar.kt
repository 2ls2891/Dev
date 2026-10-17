package com.hyunnyApps.nonogram.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.hyunnyApps.nonogram.R
import com.hyunnyApps.nonogram.ui.theme.LocalNoteColors

/** 노트 스타일 상단 바: 픽셀 뒤로가기 + 손글씨 제목 (+ 선택적 부제) */
@Composable
fun NoteTopBar(title: String, onBack: () -> Unit, subtitle: (@Composable () -> Unit)? = null) {
    val note = LocalNoteColors.current
    Row(Modifier.fillMaxWidth().padding(start = 6.dp, end = 16.dp, top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(44.dp).clip(CircleShape).clickable(onClickLabel = stringResource(R.string.back), onClick = onBack),
            contentAlignment = Alignment.Center,
        ) { PixelIcon(PixelIcons.Back, size = 22.dp) }
        Column(Modifier.padding(start = 4.dp)) {
            Text(title, style = MaterialTheme.typography.headlineSmall, color = note.ink)
            subtitle?.invoke()
        }
    }
}
