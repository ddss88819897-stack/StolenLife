package kr.paranos.stolenlife.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import kr.paranos.stolenlife.ui.theme.LineGreen
import kr.paranos.stolenlife.ui.theme.Paper
import kr.paranos.stolenlife.ui.theme.StolenTheme
import kr.paranos.stolenlife.ui.theme.WindowTone

/*
 * 화면 전체가 공유하는 레트로 부품.
 * 규칙: 직각 모서리 · 1px 베벨 · 블러 없는 2dp 검은 그림자 · 리플 대신 평면 눌림 색.
 */

/** 웹판 1em(폰 폭 기준 약 14px)을 sp / dp 로. CSS 의 em 값을 그대로 옮겨 적기 위한 단위. */
private const val EM = 14f

fun ems(value: Float): TextUnit = (EM * value).sp

fun emDp(value: Float): Dp = (EM * value).dp

/** border: 1px [line] + outline: 1px #000. 윈도우95 창 테두리. */
fun Modifier.bevel(line: Color = LineGreen, outline: Color = Color.Black): Modifier =
    this.border(1.dp, outline).padding(1.dp).border(1.dp, line)

/** box-shadow: 2px 2px 0 #000 (블러 없음). 부모가 자르지 않으면 영역 밖으로 그린다. */
fun Modifier.hardShadow(offset: Dp = 2.dp, color: Color = Color.Black): Modifier = this.drawBehind {
    val o = offset.toPx()
    drawRect(color, topLeft = Offset(o, o), size = size)
}

/** 점선 테두리. [sides] 로 위 · 아래만 긋는 것도 된다. */
fun Modifier.dashedBorder(
    color: Color,
    width: Dp = 1.dp,
    dash: Dp = 3.dp,
    gap: Dp = 2.dp,
    top: Boolean = true,
    bottom: Boolean = true,
    start: Boolean = true,
    end: Boolean = true,
): Modifier = this.drawBehind {
    val stroke = width.toPx()
    val effect = PathEffect.dashPathEffect(floatArrayOf(dash.toPx(), gap.toPx()))
    val h = stroke / 2
    if (top) drawLine(color, Offset(0f, h), Offset(size.width, h), stroke, pathEffect = effect)
    if (bottom) drawLine(color, Offset(0f, size.height - h), Offset(size.width, size.height - h), stroke, pathEffect = effect)
    if (start) drawLine(color, Offset(h, 0f), Offset(h, size.height), stroke, pathEffect = effect)
    if (end) drawLine(color, Offset(size.width - h, 0f), Offset(size.width - h, size.height), stroke, pathEffect = effect)
}

/** 한쪽 변에만 실선 (border-left: 3px solid …) */
fun Modifier.edge(color: Color, width: Dp, side: Side): Modifier = this.drawBehind {
    val w = width.toPx()
    when (side) {
        Side.START -> drawRect(color, size = size.copy(width = w))
        Side.END -> drawRect(color, topLeft = Offset(size.width - w, 0f), size = size.copy(width = w))
        Side.TOP -> drawRect(color, size = size.copy(height = w))
        Side.BOTTOM -> drawRect(color, topLeft = Offset(0f, size.height - w), size = size.copy(height = w))
    }
}

enum class Side { START, END, TOP, BOTTOM }

/** 점선 사각 외곽선 (핀 고정 문장 outline: 1px dashed) */
fun Modifier.dashedOutline(color: Color, width: Dp = 1.dp): Modifier = this.drawBehind {
    drawRect(
        color,
        style = Stroke(width.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 2.dp.toPx()))),
    )
}

/**
 * 누르는 동안 바탕색만 바뀌는 클릭 영역. 리플(원형 번짐)을 쓰지 않는다.
 * @Composable 수식어라서 리스트 안에서도 각자 눌림 상태를 따로 기억한다.
 */
@Composable
fun Modifier.pressSurface(
    normal: Color,
    pressed: Color,
    enabled: Boolean = true,
    role: Role = Role.Button,
    onClick: () -> Unit,
): Modifier {
    val source = remember { MutableInteractionSource() }
    val isPressed by source.collectIsPressedAsState()
    val click = withPressSound(onClick)
    return this
        .background(if (isPressed && enabled) pressed else normal)
        .clickable(source, indication = null, enabled = enabled, role = role, onClick = click)
}

/** 눌림 색 없이 그냥 누를 수 있는 영역 */
@Composable
fun Modifier.tap(enabled: Boolean = true, onClick: () -> Unit): Modifier {
    val source = remember { MutableInteractionSource() }
    return this.clickable(source, indication = null, enabled = enabled, onClick = withPressSound(onClick))
}

/**
 * 버튼 반응음. MainActivity 가 ViewModel 의 클릭음을 넣어 준다(소리 끄기 설정을 따른다).
 * 미리보기 · 테스트에서는 아무 소리도 내지 않는다.
 */
val LocalPressSound = staticCompositionLocalOf<() -> Unit> { {} }

/** [onClick] 앞에 반응음을 붙인다. 바깥을 눌러 닫는 막(scrim)에는 쓰지 않는다. */
@Composable
fun withPressSound(onClick: () -> Unit): () -> Unit {
    val sound = LocalPressSound.current
    val latest by rememberUpdatedState(onClick)
    return remember(sound) { { sound(); latest() } }
}

/** 웹판 .btn95 */
@Composable
fun RetroButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    line: Color = LineGreen,
    color: Color = Paper,
    surface: Color = WindowTone.Button,
    pressed: Color = WindowTone.ButtonPressed,
    textAlign: TextAlign = TextAlign.Center,
) {
    val type = StolenTheme.type
    BasicText(
        text = text,
        style = type.ui.copy(
            fontSize = ems(.8f),
            lineHeight = ems(1.2f),
            letterSpacing = 0.04.em,
            color = if (enabled) color else WindowTone.ButtonDisabledText,
            textAlign = textAlign,
        ),
        modifier = modifier
            .border(1.dp, if (enabled) line else WindowTone.ButtonDisabledBorder)
            .pressSurface(surface, pressed, enabled, onClick = onClick)
            .padding(horizontal = emDp(.95f), vertical = emDp(.45f)),
    )
}

/** 1-bit 도트 그림. 확대해도 번지지 않게 FilterQuality.None. */
@Composable
fun PixelImage(
    bitmap: ImageBitmap,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.FillBounds,
    alpha: Float = 1f,
) {
    Image(
        bitmap = bitmap,
        contentDescription = null,
        modifier = modifier,
        contentScale = contentScale,
        filterQuality = FilterQuality.None,
        alpha = alpha,
    )
}

/** 문서 · 게시물 속 사진: 폭 100%, 최대 높이 15em, object-fit: cover, 1px 테두리 */
@Composable
fun FramedPicture(bitmap: ImageBitmap, border: Color, modifier: Modifier = Modifier, maxHeight: Dp = emDp(15f)) {
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val natural = maxWidth * (bitmap.height.toFloat() / bitmap.width)
        PixelImage(
            bitmap = bitmap,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(minOf(natural, maxHeight))
                .border(1.dp, border),
        )
    }
}

/**
 * 상태가 바뀌었음을 알리는 깜빡임(steps(2) — 켜짐/꺼짐 두 단계만).
 * 장식용으로는 쓰지 않는다: 새 메시지 · 프록시 해제 배지 · 입력 대기 커서 · 엔딩.
 */
@Composable
fun blinkAlpha(periodMs: Int, low: Float = 0.25f): Float {
    val transition = rememberInfiniteTransition(label = "blink")
    val value by transition.animateFloat(
        initialValue = 1f,
        targetValue = low,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = periodMs
                1f at 0
                1f at periodMs / 2 - 1
                low at periodMs / 2
                low at periodMs
            },
            repeatMode = RepeatMode.Restart,
            initialStartOffset = StartOffset(0),
        ),
        label = "blinkAlpha",
    )
    return value
}

/** 깜빡이는 한 줄 텍스트 */
@Composable
fun BlinkText(text: String, style: TextStyle, periodMs: Int, modifier: Modifier = Modifier) {
    BasicText(text, style = style, modifier = modifier.alpha(blinkAlpha(periodMs)))
}

/** 텍스트 없는 사각 박스(점 · 커서 · 막대) */
@Composable
fun Swatch(color: Color, modifier: Modifier) {
    Box(modifier.background(color))
}

