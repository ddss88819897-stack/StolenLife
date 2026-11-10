package kr.paranos.stolenlife.ui.components

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import kr.paranos.stolenlife.data.Chip
import kr.paranos.stolenlife.data.ClockStamp
import kr.paranos.stolenlife.data.Emphasis
import kr.paranos.stolenlife.data.GameRepository.Narration
import kr.paranos.stolenlife.data.GameState
import kr.paranos.stolenlife.data.Inline
import kr.paranos.stolenlife.data.LockId
import kr.paranos.stolenlife.data.PinId
import kr.paranos.stolenlife.ui.theme.InkTone
import kr.paranos.stolenlife.ui.theme.StolenTheme

/*
 * 문서 · 게시물 · 메신저 속 한 줄을 그린다. 웹판 C() · P() · M() 가 만들던 세 가지 토큰:
 *
 *   칩    점선 밑줄 + 위첨자 '+'. 이미 쥔 칩은 실선 밑줄 + 옅은 바탕.
 *   핀    붉은 2dp 밑줄 + 옅은 붉은 바탕. 버퍼에 고정된 문장은 점선 외곽선.
 *   마스킹 프록시 활성 중: 검은 막대 + '프록시 손상'. 해제 후: 붉은 굵은 글씨로 번져 나온다.
 *
 * 밑줄은 TextDecoration 으로는 점선을 못 그리므로, 줄 배치 결과(TextLayoutResult)에서
 * 토큰 범위의 줄별 경계를 구해 drawBehind 로 직접 긋는다.
 */

/** 토큰 판정에 필요한 진행 상황만 뽑은 것 */
@Immutable
data class InkState(
    val held: Set<Chip>,
    val pinned: PinId?,
    val proxyOff: Boolean,
    val coffeeAt: String?,
    val mirrorAt: String?,
    val unlockedLocks: Set<LockId>,
) {
    companion object {
        fun of(state: GameState) = InkState(
            held = state.chips.toSet(),
            pinned = state.pin?.pin,
            proxyOff = state.proxyOff,
            coffeeAt = state.coffeeAt,
            mirrorAt = state.mirrorAt,
            unlockedLocks = state.unlocked,
        )
    }
}

/** 종이 색에 따라 바뀌는 토큰 색 */
@Immutable
data class InkPalette(
    val chipPlus: Color,
    val chipHeldWash: Color,
    val pinRule: Color,
    val pinWash: Color,
    val pinWashPinned: Color,
    val redaction: Color,
    val revealed: Color,
    /** 칩 글자 색을 바꾸는 곳(메신저 말풍선은 금색) */
    val chipText: Color = Color.Unspecified,
) {
    companion object {
        val Light = InkPalette(
            chipPlus = InkTone.ChipPlus,
            chipHeldWash = InkTone.ChipHeldWash,
            pinRule = InkTone.ChipPlus,
            pinWash = InkTone.PinWash,
            pinWashPinned = InkTone.PinWashPinned,
            redaction = InkTone.Redaction,
            revealed = InkTone.Revealed,
        )
        val Dark = InkPalette(
            chipPlus = InkTone.ChipPlusOnDark,
            chipHeldWash = InkTone.ChipHeldWashOnDark,
            pinRule = InkTone.PinRuleOnDark,
            pinWash = InkTone.PinWashOnDark,
            pinWashPinned = InkTone.PinWashOnDark.copy(alpha = 0.3f),
            redaction = InkTone.RedactionOnDark,
            revealed = InkTone.RevealedOnDark,
        )
    }
}

/** 토큰을 눌렀을 때 */
interface InkActions {
    fun onChip(chip: Chip)
    fun onPin(pin: Inline.PinRef)
    fun onRedaction()
}

private enum class MarkKind { CHIP_OPEN, CHIP_HELD, PIN, PINNED }

private data class Mark(val kind: MarkKind, val start: Int, val end: Int)

private class Rich(val text: AnnotatedString, val marks: List<Mark>)

/**
 * @param reveal 0→1. 프록시가 꺼진 직후 실명이 번져 나오는 정도(웹판 @keyframes bleed).
 */
@Composable
fun RichText(
    parts: List<Inline>,
    style: TextStyle,
    ink: InkState,
    palette: InkPalette,
    actions: InkActions,
    modifier: Modifier = Modifier,
    reveal: Float = 1f,
) {
    val latest = rememberUpdatedState(actions)
    val uiFamily = StolenTheme.type.ui.fontFamily
    val rich = remember(parts, ink, palette, reveal, uiFamily) { build(parts, ink, palette, reveal, uiFamily, latest) }
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    BasicText(
        text = rich.text,
        style = style,
        onTextLayout = { layout = it },
        modifier = modifier.drawBehind { layout?.let { drawMarks(it, rich.marks, palette) } },
    )
}

/** 평문 한 줄(칩 · 핀 없는 문장)도 같은 서식으로 */
fun plainInline(text: String): List<Inline> = listOf(Inline.Text(text))

private fun build(
    parts: List<Inline>,
    ink: InkState,
    palette: InkPalette,
    reveal: Float,
    uiFamily: FontFamily?,
    actions: State<InkActions>,
): Rich {
    val marks = mutableListOf<Mark>()
    val text = buildAnnotatedString {
        fun AnnotatedString.Builder.emit(list: List<Inline>, revealed: Boolean) {
            for (part in list) when (part) {
                is Inline.Text -> withStyle(
                    when (part.emphasis) {
                        Emphasis.PLAIN -> SpanStyle()
                        Emphasis.BOLD -> SpanStyle(fontWeight = FontWeight.Bold)
                        Emphasis.ITALIC -> SpanStyle(fontStyle = FontStyle.Italic)
                    },
                ) { append(part.value) }

                is Inline.ChipRef -> {
                    val held = part.chip in ink.held
                    val start = length
                    withLink(LinkAnnotation.Clickable("chip:${part.chip.name}") { actions.value.onChip(part.chip) }) {
                        withStyle(
                            SpanStyle(
                                fontWeight = FontWeight.Bold,
                                color = palette.chipText,
                                background = if (held) palette.chipHeldWash else Color.Unspecified,
                            ),
                        ) { append(part.shown) }
                        val end = length
                        marks += Mark(if (held) MarkKind.CHIP_HELD else MarkKind.CHIP_OPEN, start, end)
                        if (!held) {
                            withStyle(
                                SpanStyle(
                                    fontSize = 0.62.em,
                                    baselineShift = BaselineShift.Superscript,
                                    color = palette.chipPlus,
                                    fontFamily = uiFamily,
                                    fontWeight = FontWeight.Bold,
                                ),
                            ) { append("+") }
                        }
                    }
                }

                is Inline.PinRef -> {
                    val pinned = ink.pinned == part.pin
                    val start = length
                    withLink(LinkAnnotation.Clickable("pin:${part.pin.name}") { actions.value.onPin(part) }) {
                        withStyle(SpanStyle(background = if (pinned) palette.pinWashPinned else palette.pinWash)) {
                            append(part.text)
                        }
                    }
                    marks += Mark(MarkKind.PIN, start, length)
                    if (pinned) marks += Mark(MarkKind.PINNED, start, length)
                }

                is Inline.Redacted -> if (ink.proxyOff || revealed) {
                    withStyle(
                        SpanStyle(
                            color = palette.revealed.copy(alpha = reveal),
                            fontWeight = FontWeight.Bold,
                            background = palette.revealed.copy(alpha = 0.08f * reveal),
                        ),
                    ) { emit(part.content, revealed = true) }
                } else {
                    withLink(LinkAnnotation.Clickable("mask") { actions.value.onRedaction() }) {
                        withStyle(SpanStyle(color = palette.redaction, background = palette.redaction)) {
                            append(" ${Narration.REDACTION_BLOCKS} ")
                        }
                        withStyle(
                            SpanStyle(
                                color = InkTone.RedactionLabel,
                                background = palette.redaction,
                                fontSize = 0.55.em,
                                fontFamily = uiFamily,
                                fontWeight = FontWeight.Normal,
                                letterSpacing = 0.06.em,
                            ),
                        ) { append("${Narration.REDACTION_LABEL} ") }
                    }
                }

                is Inline.TimeStamp -> append(
                    when (part.source) {
                        ClockStamp.COFFEE -> ink.coffeeAt
                        ClockStamp.MIRROR -> ink.mirrorAt
                    } ?: part.fallback,
                )

                Inline.LineBreak -> append('\n')
            }
        }
        emit(parts, revealed = false)
    }
    return Rich(text, marks)
}

/** 토큰 범위가 걸친 줄마다 [left, right] × [top, baseline] 사각형 */
private fun TextLayoutResult.segments(start: Int, end: Int): List<Pair<Rect, Float>> {
    if (end <= start) return emptyList()
    val first = getLineForOffset(start)
    val last = getLineForOffset(end - 1)
    return (first..last).mapNotNull { line ->
        val s = maxOf(start, getLineStart(line))
        val e = minOf(end, getLineEnd(line, visibleEnd = true))
        if (s >= e) return@mapNotNull null
        val left = getBoundingBox(s).left
        val right = getBoundingBox(e - 1).right
        Rect(minOf(left, right), getLineTop(line), maxOf(left, right), getLineBottom(line)) to getLineBaseline(line)
    }
}

private fun DrawScope.drawMarks(layout: TextLayoutResult, marks: List<Mark>, palette: InkPalette) {
    val dash = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 2.dp.toPx()))
    for (mark in marks) {
        for ((box, baseline) in layout.segments(mark.start, mark.end)) {
            val underline = baseline + 2.dp.toPx()
            when (mark.kind) {
                MarkKind.CHIP_OPEN -> drawLine(
                    color = inkColor(layout, mark.start),
                    start = Offset(box.left, underline),
                    end = Offset(box.right, underline),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = dash,
                )

                MarkKind.CHIP_HELD -> drawLine(
                    color = inkColor(layout, mark.start),
                    start = Offset(box.left, underline),
                    end = Offset(box.right, underline),
                    strokeWidth = 1.dp.toPx(),
                )

                MarkKind.PIN -> drawLine(
                    color = palette.pinRule,
                    start = Offset(box.left, underline + 0.5.dp.toPx()),
                    end = Offset(box.right, underline + 0.5.dp.toPx()),
                    strokeWidth = 2.dp.toPx(),
                )

                MarkKind.PINNED -> {
                    val fontPx = layout.layoutInput.style.fontSize.toPx()
                    val top = baseline - fontPx * 1.05f
                    drawRect(
                        color = palette.pinRule,
                        topLeft = Offset(box.left - 2.dp.toPx(), top),
                        size = Size(box.width + 4.dp.toPx(), underline + 3.dp.toPx() - top),
                        style = Stroke(1.dp.toPx(), pathEffect = dash),
                    )
                }
            }
        }
    }
}

/** 칩 밑줄은 글자색(currentColor)을 따른다. */
private fun inkColor(layout: TextLayoutResult, offset: Int): Color {
    val spanColor = layout.layoutInput.text.spanStyles
        .lastOrNull { it.start <= offset && offset < it.end && it.item.color != Color.Unspecified }
        ?.item?.color
    return spanColor ?: layout.layoutInput.style.color.takeIf { it != Color.Unspecified } ?: Color.Black
}
