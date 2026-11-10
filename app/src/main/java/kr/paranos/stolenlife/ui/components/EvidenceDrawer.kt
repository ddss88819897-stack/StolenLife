package kr.paranos.stolenlife.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import kr.paranos.stolenlife.R
import kr.paranos.stolenlife.data.AppId
import kr.paranos.stolenlife.data.Chip
import kr.paranos.stolenlife.data.ChipFilter
import kr.paranos.stolenlife.ui.theme.Gold
import kr.paranos.stolenlife.ui.theme.Ink
import kr.paranos.stolenlife.ui.theme.LineInk
import kr.paranos.stolenlife.ui.theme.Mute
import kr.paranos.stolenlife.ui.theme.Paper
import kr.paranos.stolenlife.ui.theme.Paper2
import kr.paranos.stolenlife.ui.theme.Rust
import kr.paranos.stolenlife.ui.theme.ShellTone
import kr.paranos.stolenlife.ui.theme.StolenTheme
import kr.paranos.stolenlife.viewmodel.DrawerUi

/*
 * 단서함 바텀시트 `EVIDENCE FILE // 단서함`.
 *
 * ModalBottomSheet 는 창 전체를 덮어 하단 시스템 바까지 가리므로 쓰지 않는다.
 * 하단 바 위쪽 영역에만 직접 만든 시트를 띄운다(웹판 #drawer, bottom: 2.75em).
 */

private val SheetEasing = CubicBezierEasing(.2f, .8f, .2f, 1f)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EvidenceDrawer(
    drawer: DrawerUi,
    chips: List<Chip>,
    fresh: Set<Chip>,
    onClose: () -> Unit,
    onFilter: (ChipFilter) -> Unit,
    onChip: (Chip) -> Unit,
    modifier: Modifier = Modifier,
) {
    val type = StolenTheme.type
    BoxWithConstraints(modifier.fillMaxSize()) {
        val sheetMax = maxHeight * 0.64f
        AnimatedVisibility(drawer.open, enter = fadeIn(tween(250)), exit = fadeOut(tween(250))) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(ShellTone.DrawerScrim)
                    .clickable(remember { MutableInteractionSource() }, indication = null, onClick = onClose),
            )
        }
        AnimatedVisibility(
            visible = drawer.open,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically(tween(320, easing = SheetEasing)) { it },
            exit = slideOutVertically(tween(320, easing = SheetEasing)) { it },
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = sheetMax)
                    .wrapContentHeight()
                    // box-shadow 대신 위쪽 2dp 검은 띠(블러 없음)
                    .drawBehind { drawRect(Color.Black, Offset(0f, -2.dp.toPx()), size.copy(height = 2.dp.toPx())) }
                    .background(ShellTone.DrawerSheet)
                    .edge(Gold, 1.dp, Side.TOP)
                    .clickable(remember { MutableInteractionSource() }, indication = null) { },
            ) {
                // 손잡이: 누르면 닫힌다
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(emDp(1.5f))
                        .tap(onClick = onClose),
                    contentAlignment = Alignment.Center,
                ) {
                    Swatch(ShellTone.DrawerHandle, Modifier.size(width = emDp(3.4f), height = 3.dp))
                }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .edge(ShellTone.DrawerRule, 1.dp, Side.BOTTOM)
                        .padding(start = emDp(1f), end = emDp(1f), bottom = emDp(.45f)),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    BasicText(stringResource(R.string.drawer_title), style = type.ui.copy(fontSize = ems(.7f), letterSpacing = 0.16.em, color = Gold))
                    Spacer(Modifier.weight(1f))
                    BasicText(
                        stringResource(R.string.drawer_count, chips.size),
                        style = type.keypad.copy(fontSize = ems(.82f), letterSpacing = 0.06.em, color = Paper),
                    )
                }
                DrawerSubline(drawer.target)
                // 분류 탭 [ALL] [PER] [LOC] [NUM] [ORG] [ACC]
                Row(Modifier.padding(start = emDp(1f), end = emDp(1f), top = emDp(.6f))) {
                    ChipFilter.entries.forEach { filter ->
                        val on = drawer.filter == filter
                        BasicText(
                            text = stringResource(R.string.chip_type_tag, filter.name),
                            style = type.ui.copy(fontSize = ems(.62f), letterSpacing = 0.1.em, color = if (on) Ink else Mute),
                            modifier = Modifier
                                .border(1.dp, if (on) Paper2 else ShellTone.DrawerTab)
                                .pressSurface(if (on) Paper2 else Color.Transparent, if (on) Paper2 else ShellTone.NavPressed) { onFilter(filter) }
                                .padding(horizontal = emDp(.5f), vertical = emDp(.3f)),
                        )
                    }
                }
                val list = chips.asReversed().filter(drawer.filter::accepts)
                FlowRow(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = emDp(6f))
                        .verticalScroll(rememberScrollState())
                        .padding(start = emDp(1f), end = emDp(1f), top = emDp(.8f), bottom = emDp(1.1f)),
                    horizontalArrangement = Arrangement.spacedBy(emDp(.55f)),
                    verticalArrangement = Arrangement.spacedBy(emDp(.6f)),
                ) {
                    if (list.isEmpty()) {
                        BasicText(
                            text = stringResource(if (chips.isEmpty()) R.string.drawer_empty_none else R.string.drawer_empty_filter),
                            style = type.ui.copy(fontSize = ems(.74f), color = Mute),
                            modifier = Modifier.padding(vertical = emDp(.6f)),
                        )
                    }
                    list.forEach { chip -> EvidenceChip(chip, chip in fresh) { onChip(chip) } }
                }
            }
        }
    }
}

@Composable
private fun DrawerSubline(target: AppId?) {
    val type = StolenTheme.type
    val text = if (target == null) {
        buildAnnotatedString { append(stringResource(R.string.drawer_sub_idle)) }
    } else {
        val prefix = stringResource(R.string.drawer_sub_target_prefix)
        val suffix = stringResource(R.string.drawer_sub_target_suffix)
        buildAnnotatedString {
            append(prefix)
            withStyle(SpanStyle(color = Paper, fontWeight = FontWeight.Medium)) { append(target.title) }
            append(suffix)
        }
    }
    BasicText(
        text = text,
        style = type.ui.copy(fontSize = ems(.68f), lineHeight = ems(1.05f), color = Mute),
        modifier = Modifier
            .fillMaxWidth()
            .dashedBorder(ShellTone.DrawerRule, top = false, start = false, end = false)
            .padding(horizontal = emDp(1f), vertical = emDp(.5f)),
    )
}

/**
 * 단서 칩 = 타자기로 찍은 종이 라벨. Paper2 바탕 · 1px 잉크 테두리 · 2dp 검은 그림자.
 * 왼쪽 가장자리에 철끈 구멍(정사각). 누르면 1dp 내려앉는다. 막 얻은 칩은 1.8초 동안 밝게 떠오른다.
 */
@Composable
fun EvidenceChip(chip: Chip, fresh: Boolean, onClick: () -> Unit) {
    val type = StolenTheme.type
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val glow = remember { Animatable(if (fresh) 1f else 0f) }
    LaunchedEffect(fresh) { if (fresh) glow.animateTo(0f, tween(1800)) }
    val sink = if (pressed) 1.dp else 0.dp
    val lift = (-3 * glow.value).dp
    Box(
        Modifier
            .offset { IntOffset(sink.roundToPx(), (sink + lift).roundToPx()) }
            .hardShadow(if (pressed) 1.dp else 2.dp)
            .background(lerp(Paper2, FRESH_PAPER, glow.value))
            .border(1.dp, LineInk)
            .clickable(source, indication = null, onClick = withPressSound(onClick))
            .padding(start = emDp(.65f), end = emDp(.65f), top = emDp(.3f), bottom = emDp(.32f)),
    ) {
        Swatch(
            ShellTone.DrawerSheet,
            Modifier
                .align(Alignment.CenterStart)
                .offset(x = -(emDp(.65f) + 1.dp))
                .size(4.dp),
        )
        val label = buildAnnotatedString {
            withStyle(SpanStyle(color = Rust, fontSize = 0.76.em, letterSpacing = 0.06.em, fontWeight = FontWeight.SemiBold)) {
                append(stringResource(R.string.chip_type_tag, chip.type.code))
            }
            append("  ")
            append(chip.label)
        }
        BasicText(label, style = type.ui.copy(fontSize = ems(.8f), lineHeight = ems(1.04f), color = Ink))
    }
}

private val FRESH_PAPER = Color(0xFFF1EAD8)
