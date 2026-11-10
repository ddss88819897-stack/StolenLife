package kr.paranos.stolenlife.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import kr.paranos.stolenlife.R
import kr.paranos.stolenlife.data.PinnedEvidence
import kr.paranos.stolenlife.ui.theme.Gold
import kr.paranos.stolenlife.ui.theme.Paper
import kr.paranos.stolenlife.ui.theme.ShellTone
import kr.paranos.stolenlife.ui.theme.StolenTheme
import kr.paranos.stolenlife.viewmodel.PinMenu
import kr.paranos.stolenlife.viewmodel.PinOption

/**
 * [PIN] 대조 버퍼. 앱 타이틀바 바로 아래에 떠서, 다른 문서를 읽는 동안에도 고정한 문장이 계속 보인다.
 * 깜빡임은 넣지 않는다(상시 깜빡임 금지).
 */
@Composable
fun PinBuffer(pin: PinnedEvidence, onClear: () -> Unit, modifier: Modifier = Modifier) {
    val type = StolenTheme.type
    Row(
        modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .hardShadow(2.dp)
            .background(ShellTone.PinSurface)
            .border(1.dp, Gold)
            .clickable(remember { MutableInteractionSource() }, indication = null) { }
            .padding(horizontal = emDp(.55f), vertical = emDp(.35f)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicText(
            text = stringResource(R.string.pin_label),
            style = type.ui.copy(fontSize = ems(.53f), lineHeight = ems(.7f), letterSpacing = 0.1.em, color = Gold),
            modifier = Modifier
                .edge(ShellTone.PinRule, 1.dp, Side.END)
                .padding(end = emDp(.6f)),
        )
        Spacer(Modifier.width(emDp(.6f)))
        Column(Modifier.weight(1f)) {
            BasicText(
                text = pin.text,
                style = type.ui.copy(fontSize = ems(.76f), lineHeight = ems(1.03f), color = Paper),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            BasicText(
                text = stringResource(R.string.pin_source, pin.source),
                style = type.ui.copy(fontSize = ems(.6f), lineHeight = ems(.85f), color = ShellTone.PinSub),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(emDp(.6f)))
        val clearLabel = stringResource(R.string.pin_clear_cd)
        BasicText(
            text = stringResource(R.string.pin_clear),
            style = type.ui.copy(fontSize = ems(.72f), color = Gold),
            modifier = Modifier
                .border(1.dp, ShellTone.PinRule)
                .pressSurface(ShellTone.PinSurface, ShellTone.PinRule, onClick = onClear)
                .semantics { contentDescription = clearLabel }
                .padding(horizontal = emDp(.5f), vertical = emDp(.05f)),
        )
    }
}

/**
 * 붉은 밑줄 문장을 눌렀을 때 그 자리 근처에 뜨는 작은 메뉴(웹판 #pop).
 * [anchor] 는 마지막으로 손가락이 닿은 화면 좌표. 아래 공간이 모자라면 위로 뒤집는다.
 * 메뉴 밖 어디를 눌러도 닫힌다.
 */
@Composable
fun PinMenuPopup(menu: PinMenu, anchor: Offset, onChoose: (PinOption) -> Unit, onDismiss: () -> Unit) {
    val type = StolenTheme.type
    val labels = mapOf(
        PinOption.SET to stringResource(R.string.pin_menu_set),
        PinOption.CLEAR to stringResource(R.string.pin_menu_clear),
        PinOption.MATCH to stringResource(R.string.pin_menu_match),
        PinOption.REPLACE to stringResource(R.string.pin_menu_replace),
    )
    Box(
        Modifier
            .fillMaxSize()
            .clickable(remember { MutableInteractionSource() }, indication = null, onClick = onDismiss),
    ) {
        Layout(
            content = {
                Column(
                    Modifier
                        .width(IntrinsicSize.Max)
                        .widthIn(min = emDp(13f))
                        .hardShadow(2.dp)
                        .background(ShellTone.Popup)
                        .border(1.dp, Gold),
                ) {
                    menu.options.forEachIndexed { index, option ->
                        BasicText(
                            text = labels.getValue(option),
                            style = type.ui.copy(fontSize = ems(.78f), letterSpacing = 0.02.em, color = Paper),
                            modifier = Modifier
                                .fillMaxWidth()
                                .pressSurface(ShellTone.Popup, ShellTone.PopupRule) { onChoose(option) }
                                .then(if (index < menu.options.lastIndex) Modifier.edge(ShellTone.PopupRule, 1.dp, Side.BOTTOM) else Modifier)
                                .padding(horizontal = emDp(.85f), vertical = emDp(.62f)),
                        )
                    }
                }
            },
            modifier = Modifier.fillMaxSize(),
        ) { measurables, constraints ->
            val popup = measurables.first().measure(Constraints(maxWidth = (constraints.maxWidth - 8.dp.roundToPx()).coerceAtLeast(0)))
            val margin = 4.dp.roundToPx()
            val gap = 14.dp.roundToPx()
            val x = (anchor.x.toInt() - popup.width / 4).coerceIn(margin, (constraints.maxWidth - popup.width - margin).coerceAtLeast(margin))
            val below = anchor.y.toInt() + gap
            val y = if (below + popup.height > constraints.maxHeight - 8.dp.roundToPx()) {
                (anchor.y.toInt() - popup.height - gap).coerceAtLeast(margin)
            } else {
                below
            }
            layout(constraints.maxWidth, constraints.maxHeight) { popup.place(x, y) }
        }
    }
}
