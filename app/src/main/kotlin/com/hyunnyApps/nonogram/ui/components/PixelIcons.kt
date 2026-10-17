package com.hyunnyApps.nonogram.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.hyunnyApps.nonogram.ui.theme.LocalNoteColors

/**
 * 모눈종이에 그린 듯한 12×12 픽셀 아이콘 (직접 그림, 외부 아이콘 세트 아님).
 * '1' = 주 색(tint, 기본은 잉크색), '2'~'4' = 고정 보조색, '.' = 빈칸
 */
class PixelArt(val rows: List<String>, val extra: List<Color> = emptyList()) {
    val width = rows.first().length
    val height = rows.size
}

object PixelIcons {
    val Flame = PixelArt(
        listOf(
            "....1.......",
            "....11......",
            "...111..1...",
            "...1111.11..",
            "..11111111..",
            "..11121111..",
            ".1112221111.",
            ".1122222111.",
            ".1122222211.",
            ".1112222111.",
            "..11122111..",
            "...111111...",
        ),
        listOf(Color(0xFFFFB020)),
    )
    val Bulb = PixelArt(
        listOf(
            "....1111....",
            "...122221...",
            "..12222221..",
            "..12222221..",
            "..12222221..",
            "..12222221..",
            "...122221...",
            "....1221....",
            "....1111....",
            "....1111....",
            ".....11.....",
            "............",
        ),
        listOf(Color(0xFFFFD447)),
    )
    val Frame = PixelArt(
        listOf(
            "111111111111",
            "122222222221",
            "122224422221",
            "122224422221",
            "122222222221",
            "122222232221",
            "122223333221",
            "122233333321",
            "123333333331",
            "133333333331",
            "111111111111",
            "............",
        ),
        listOf(Color(0xFF9AD1F5), Color(0xFF6CC070), Color(0xFFFFC93C)),
    )
    val Play = PixelArt(
        listOf(
            "............",
            "...1........",
            "...11.......",
            "...111......",
            "...1111.....",
            "...11111....",
            "...11111....",
            "...1111.....",
            "...111......",
            "...11.......",
            "...1........",
            "............",
        ),
    )
    val Square = PixelArt(
        listOf(
            "............",
            ".1111111111.",
            ".1111111111.",
            ".1111111111.",
            ".1111111111.",
            ".1111111111.",
            ".1111111111.",
            ".1111111111.",
            ".1111111111.",
            ".1111111111.",
            ".1111111111.",
            "............",
        ),
    )
    val Cross = PixelArt(
        listOf(
            "11........11",
            "111......111",
            ".111....111.",
            "..111..111..",
            "...111111...",
            "....1111....",
            "....1111....",
            "...111111...",
            "..111..111..",
            ".111....111.",
            "111......111",
            "11........11",
        ),
    )
    val Undo = PixelArt(
        listOf(
            "............",
            "....1.......",
            "...11.......",
            "..111111....",
            ".11111111...",
            "..111...11..",
            "...11....11.",
            "....1.....1.",
            "..........1.",
            ".........11.",
            ".......111..",
            "............",
        ),
    )
    val Redo = PixelArt(Undo.rows.map { it.reversed() })
    val Help = PixelArt(
        listOf(
            "...111111...",
            "..11111111..",
            "..11....11..",
            ".......111..",
            "......111...",
            ".....111....",
            ".....11.....",
            ".....11.....",
            "............",
            ".....11.....",
            ".....11.....",
            "............",
        ),
    )
    val Gear = PixelArt(
        listOf(
            ".....11.....",
            "..1.1111.1..",
            ".1111111111.",
            "..111..111..",
            ".111....111.",
            "1111....1111",
            "1111....1111",
            ".111....111.",
            "..111..111..",
            ".1111111111.",
            "..1.1111.1..",
            ".....11.....",
        ),
    )
    val Back = PixelArt(
        listOf(
            "............",
            ".....1......",
            "....11......",
            "...11.......",
            "..11........",
            ".1111111111.",
            ".1111111111.",
            "..11........",
            "...11.......",
            "....11......",
            ".....1......",
            "............",
        ),
    )
    val Clock = PixelArt(
        listOf(
            "...111111...",
            "..1......1..",
            ".1...1....1.",
            "1....1.....1",
            "1....1.....1",
            "1....1111..1",
            "1..........1",
            "1..........1",
            ".1........1.",
            "..1......1..",
            "...111111...",
            "............",
        ),
    )
    val Check = PixelArt(
        listOf(
            "............",
            "..........11",
            ".........111",
            "........111.",
            ".......111..",
            "11....111...",
            "111..111....",
            ".111111.....",
            "..1111......",
            "...11.......",
            "............",
            "............",
        ),
    )
    val Bell = PixelArt(
        listOf(
            ".....11.....",
            "....1111....",
            "...111111...",
            "..11111111..",
            "..11111111..",
            "..11111111..",
            "..11111111..",
            ".1111111111.",
            "111111111111",
            "............",
            ".....11.....",
            "............",
        ),
    )
}

/** 픽셀 아이콘. tint 가 null 이면 잉크색 */
@Composable
fun PixelIcon(art: PixelArt, modifier: Modifier = Modifier, size: Dp = 22.dp, tint: Color? = null) {
    val ink = tint ?: LocalNoteColors.current.ink
    Canvas(modifier.size(size)) {
        val cell = minOf(this.size.width / art.width, this.size.height / art.height)
        val ox = (this.size.width - cell * art.width) / 2
        val oy = (this.size.height - cell * art.height) / 2
        val px = Size(cell + 0.4f, cell + 0.4f)
        art.rows.forEachIndexed { y, row ->
            row.forEachIndexed { x, ch ->
                val color = when (ch) {
                    '1' -> ink
                    in '2'..'9' -> art.extra.getOrNull(ch - '2') ?: ink
                    else -> null
                }
                if (color != null) drawRect(color, Offset(ox + x * cell, oy + y * cell), px)
            }
        }
    }
}
