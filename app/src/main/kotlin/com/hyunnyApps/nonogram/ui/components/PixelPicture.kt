package com.hyunnyApps.nonogram.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import com.hyunnyApps.nonogram.core.RowCodec
import com.hyunnyApps.nonogram.core.format.Picture

fun parseHexColor(hex: String): Color = Color(0xFF000000 or hex.removePrefix("#").toLong(16))

/** 팔레트+행 문자열 그림을 칸 단위로 그린다 (클리어 그림, 썸네일). 비율을 유지해 가운데 정렬 */
@Composable
fun PixelPicture(picture: Picture, modifier: Modifier = Modifier) {
    val colors = remember(picture) { picture.palette.map(::parseHexColor) }
    val h = picture.rows.size
    val w = picture.rows.firstOrNull()?.length ?: 0
    Canvas(modifier) {
        if (w == 0 || h == 0) return@Canvas
        val cell = minOf(size.width / w, size.height / h)
        val ox = (size.width - cell * w) / 2
        val oy = (size.height - cell * h) / 2
        // 칸 사이 틈이 보이지 않도록 살짝 겹쳐 그린다
        val px = Size(cell + 0.5f, cell + 0.5f)
        picture.rows.forEachIndexed { y, row ->
            row.forEachIndexed { x, ch ->
                if (ch != '.') drawRect(colors[RowCodec.decode(ch) - 1], Offset(ox + x * cell, oy + y * cell), px)
            }
        }
    }
}
