package com.hyunnyApps.nonogram.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hyunnyApps.nonogram.ui.theme.HandFont
import com.hyunnyApps.nonogram.ui.theme.LocalNoteColors
import kotlin.math.sin

/** 모눈종이 배경 (가는 선 + 5칸마다 조금 진한 선) */
fun Modifier.graphPaper(paper: Color, line: Color, strong: Color, step: Dp = 22.dp): Modifier = drawBehind {
    drawRect(paper)
    val s = step.toPx()
    var i = 0
    var x = 0f
    while (x < size.width) {
        drawLine(if (i % 5 == 0) strong else line, Offset(x, 0f), Offset(x, size.height), 1f)
        x += s; i++
    }
    i = 0
    var y = 0f
    while (y < size.height) {
        drawLine(if (i % 5 == 0) strong else line, Offset(0f, y), Offset(size.width, y), 1f)
        y += s; i++
    }
}

/** 앱 전체 배경 */
@Composable
fun PaperBackground(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val note = LocalNoteColors.current
    Box(modifier.graphPaper(note.paper, note.paperLine, note.paperLineStrong), content = content)
}

/**
 * 종이 카드. tape = true 면 위에 마스킹테이프가 붙어 있다.
 * rotation 으로 살짝 기울여 손으로 붙인 느낌을 낸다 (과하면 산만하니 ±2도 이내).
 */
@Composable
fun PaperCard(
    modifier: Modifier = Modifier,
    rotation: Float = 0f,
    tape: Boolean = false,
    color: Color = LocalNoteColors.current.card,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val note = LocalNoteColors.current
    Box(modifier.rotate(rotation)) {
        Box(
            Modifier
                .padding(top = if (tape) 10.dp else 0.dp)
                .fillMaxWidth()
                .shadow(3.dp, RoundedCornerShape(6.dp), ambientColor = note.shadow, spotColor = note.shadow)
                .background(color, RoundedCornerShape(6.dp))
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
            content = content,
        )
        if (tape) {
            Box(
                Modifier
                    .align(Alignment.TopCenter)
                    .rotate(-3f)
                    .size(86.dp, 22.dp)
                    .background(note.tape),
            )
        }
    }
}

enum class StickerStyle { Primary, Plain, Highlight }

/**
 * 스티커 버튼: 잉크 테두리 + 오른쪽 아래로 딱 떨어지는 그림자. 누르면 그림자 쪽으로 눌린다.
 * Material 기본 버튼 대신 앱 전체에서 이걸 쓴다.
 */
@Composable
fun StickerButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: StickerStyle = StickerStyle.Primary,
    icon: PixelArt? = null,
    enabled: Boolean = true,
    height: Dp = 52.dp,
    selected: Boolean = false,
    textStyle: TextStyle = TextStyle(fontFamily = HandFont, fontSize = 22.sp),
    /** true 면 아이콘만 보인다(좁은 화면). 글자는 화면 읽기용 설명으로만 쓴다 */
    iconOnly: Boolean = false,
) {
    val note = LocalNoteColors.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val fill = when (style) {
        StickerStyle.Primary -> note.redPencil
        StickerStyle.Plain -> note.card
        StickerStyle.Highlight -> note.highlighter
    }
    val content = when (style) {
        StickerStyle.Primary -> Color.White
        StickerStyle.Highlight -> Color(0xFF26304F)
        StickerStyle.Plain -> note.ink
    }.let { if (enabled) it else it.copy(alpha = 0.35f) }
    val border = if (enabled) note.ink else note.ink.copy(alpha = 0.25f)
    val shadow = 4.dp
    val down = pressed || selected
    // 자동 축소 하한은 글꼴 크기 설정과 무관하게 실제 11sp 정도로 (설정이 200%면 하한도 같이 커지는 것을 막음)
    val minFont = (11f / LocalDensity.current.fontScale).coerceAtLeast(6f).sp
    Box(modifier.height(height + shadow)) {
        // 그림자: 오른쪽 아래로 shadow 만큼 밀린 같은 크기의 판
        Box(
            Modifier.fillMaxSize().padding(start = shadow, top = shadow)
                .background(if (enabled) note.shadow else note.shadow.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
        )
        Row(
            Modifier
                .fillMaxSize()
                .padding(end = shadow, bottom = shadow)
                .offset(if (down) shadow else 0.dp, if (down) shadow else 0.dp)
                .background(if (enabled) fill else fill.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                .border(2.dp, border, RoundedCornerShape(12.dp))
                .clickable(interactionSource = interaction, indication = null, enabled = enabled, role = Role.Button, onClick = onClick)
                .then(if (iconOnly && icon != null) Modifier.semantics { contentDescription = text } else Modifier)
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                PixelIcon(icon, size = if (iconOnly) 24.dp else 20.dp, tint = content)
                if (iconOnly) return@Row
                Spacer(Modifier.width(8.dp))
            }
            // 큰 글꼴 설정이나 좁은 화면(폴드 커버)에서도 잘리지 않게 칸에 맞춰 글자를 줄인다
            Text(
                text,
                style = textStyle,
                color = content,
                maxLines = 1,
                autoSize = TextAutoSize.StepBased(minFontSize = minFont, maxFontSize = textStyle.fontSize, stepSize = 0.5.sp),
                modifier = Modifier.weight(1f, fill = false),
            )
        }
    }
}

/** 제목 아래 연필로 그은 물결선 */
@Composable
fun PencilUnderline(modifier: Modifier = Modifier, color: Color = LocalNoteColors.current.redPencil, width: Dp = 120.dp) {
    Canvas(modifier.size(width, 8.dp)) {
        val path = Path()
        val w = size.width
        val h = size.height
        path.moveTo(0f, h * 0.6f)
        var x = 0f
        while (x <= w) {
            path.lineTo(x, h * 0.5f + sin(x / w * 12f) * h * 0.25f)
            x += 4f
        }
        drawPath(path, color, style = Stroke(width = 2.5.dp.toPx()))
    }
}

/** 난이도 표시: 색연필 테두리 꼬리표 */
@Composable
fun LevelTag(level: Int, modifier: Modifier = Modifier, onDark: Boolean = false) {
    val color = levelColor(level)
    Text(
        stringResource(levelName(level)),
        modifier = modifier
            .border(1.5.dp, if (onDark) Color.White else color, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 1.dp),
        color = if (onDark) Color.White else color,
        style = TextStyle(fontFamily = HandFont, fontSize = 18.sp, lineHeight = 20.sp),
    )
}

/** "참 잘했어요" 도장 */
@Composable
fun Stamp(text: String, modifier: Modifier = Modifier, size: Dp = 112.dp) {
    val red = LocalNoteColors.current.redPencil
    Box(modifier.size(size).rotate(-14f), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val r = this.size.minDimension / 2
            drawCircle(red, r - 3.dp.toPx(), style = Stroke(4.dp.toPx()))
            drawCircle(red, r - 10.dp.toPx(), style = Stroke(1.5.dp.toPx()))
        }
        // 도장은 그림이라 글꼴 크기 설정과 무관하게 원 크기에 맞춘다
        val density = LocalDensity.current
        Text(
            text,
            color = red,
            textAlign = TextAlign.Center,
            style = with(density) { TextStyle(fontFamily = HandFont, fontSize = (size / 4.6f).toSp(), lineHeight = (size / 4.2f).toSp()) },
        )
    }
}

/** 형광펜 칠한 배경 텍스트 */
@Composable
fun Highlighted(text: String, style: TextStyle, modifier: Modifier = Modifier) {
    val note = LocalNoteColors.current
    Text(
        text,
        style = style,
        color = note.ink,
        modifier = modifier.drawBehind {
            drawRect(note.highlighter.copy(alpha = 0.7f), Offset(0f, size.height * 0.45f), androidx.compose.ui.geometry.Size(size.width, size.height * 0.45f))
        },
    )
}
