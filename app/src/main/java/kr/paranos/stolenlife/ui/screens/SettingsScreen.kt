package kr.paranos.stolenlife.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import kr.paranos.stolenlife.R
import kr.paranos.stolenlife.data.Deduction
import kr.paranos.stolenlife.data.DemoJump
import kr.paranos.stolenlife.data.GameRepository
import kr.paranos.stolenlife.data.GameState
import kr.paranos.stolenlife.ui.components.RetroButton
import kr.paranos.stolenlife.ui.components.TipBox
import kr.paranos.stolenlife.ui.components.emDp
import kr.paranos.stolenlife.ui.components.ems
import kr.paranos.stolenlife.ui.components.tap
import kr.paranos.stolenlife.ui.theme.AppTone
import kr.paranos.stolenlife.ui.theme.Gold
import kr.paranos.stolenlife.ui.theme.Paper
import kr.paranos.stolenlife.ui.theme.Rust
import kr.paranos.stolenlife.ui.theme.Slate
import kr.paranos.stolenlife.ui.theme.StolenTheme
import kr.paranos.stolenlife.viewmodel.GameIntents

/**
 * 설정: Proxy 1998 로컬 필터 · 시스템 · 시연용 챕터 점프.
 * 필터 강제 종료는 '나는 한도윤이 아니다' 교차검증 이후에만 길게 눌러(1.5초) 할 수 있다.
 */
@Composable
fun SettingsContent(state: GameState, intents: GameIntents) {
    val type = StolenTheme.type
    val text = type.ui.copy(fontSize = ems(.8f), lineHeight = ems(.8f * 1.7f), color = AppTone.GroupText)
    Column(Modifier.fillMaxWidth()) {
        GroupBox(stringResource(R.string.set_proxy_legend)) {
            when {
                state.proxyOff -> {
                    BasicText(
                        text = buildAnnotatedString {
                            append(stringResource(R.string.set_proxy_killed_prefix))
                            withStyle(SpanStyle(color = KILLED, fontWeight = FontWeight.Bold)) { append(stringResource(R.string.set_proxy_killed)) }
                        },
                        style = text,
                    )
                    BasicText(stringResource(R.string.set_proxy_restored), style = text)
                    val restored = GameRepository.proxyCollapse.restoredDocs.filter(state::hasRead)
                    if (restored.isEmpty()) {
                        BasicText(stringResource(R.string.set_proxy_restored_none), style = text)
                    }
                    restored.forEach { id ->
                        BasicText(
                            text = stringResource(R.string.set_proxy_restored_item, GameRepository.document(id).title),
                            style = text.copy(color = Paper, textDecoration = TextDecoration.Underline),
                            modifier = Modifier.tap { intents.openDocument(id) },
                        )
                    }
                }

                Deduction.NOT_DOYUN in state.deductions -> {
                    BasicText(stringResource(R.string.set_proxy_active), style = text)
                    HoldToKill(intents)
                }

                else -> {
                    BasicText(stringResource(R.string.set_proxy_active), style = text)
                    RetroButton(
                        text = stringResource(R.string.set_proxy_locked),
                        onClick = {},
                        enabled = false,
                        modifier = Modifier
                            .padding(top = emDp(.4f))
                            .fillMaxWidth(),
                    )
                }
            }
        }
        GroupBox(stringResource(R.string.set_system_legend)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BasicText(stringResource(R.string.set_sound), style = text)
                RetroButton(stringResource(if (state.sound) R.string.set_sound_on else R.string.set_sound_off), intents::toggleSound)
            }
            Spacer(Modifier.height(emDp(1f)))
            RetroButton(stringResource(R.string.set_reset), intents::askReset)
        }
        GroupBox(stringResource(R.string.set_jump_legend)) {
            Row {
                RetroButton(stringResource(R.string.set_jump_ch2), { intents.jump(DemoJump.CHAPTER_2) })
                Spacer(Modifier.width(emDp(.35f)))
                RetroButton(stringResource(R.string.set_jump_ch3), { intents.jump(DemoJump.CHAPTER_3) })
                Spacer(Modifier.width(emDp(.35f)))
                RetroButton(stringResource(R.string.set_jump_end), { intents.jump(DemoJump.BEFORE_ENDING) })
            }
        }
        TipBox(stringResource(R.string.set_rooted))
    }
}

/** fieldset + legend: 테두리 위쪽 선을 범례 글자가 끊고 지나간다. */
@Composable
private fun GroupBox(legend: String, content: @Composable () -> Unit) {
    val type = StolenTheme.type
    Box(Modifier.padding(top = emDp(.45f), bottom = emDp(.85f))) {
        Column(
            Modifier
                .fillMaxWidth()
                .border(1.dp, AppTone.GroupRule)
                .padding(start = emDp(.85f), end = emDp(.85f), top = emDp(.95f), bottom = emDp(.85f)),
        ) { content() }
        BasicText(
            text = legend,
            style = type.ui.copy(fontSize = ems(.64f), letterSpacing = 0.14.em, color = Gold),
            modifier = Modifier
                .offset(x = emDp(.6f), y = -emDp(.45f))
                .background(Slate)
                .padding(horizontal = emDp(.45f)),
        )
    }
}

/** `[WARN] 필터 강제 종료 — 길게 누르기`: 1.5초 동안 붉은 막대가 왼쪽부터 찬다. 손을 떼면 처음부터. */
@Composable
private fun HoldToKill(intents: GameIntents) {
    val type = StolenTheme.type
    val latest by rememberUpdatedState(intents)
    var holding by remember { mutableStateOf(false) }
    val progress = remember { Animatable(0f) }
    LaunchedEffect(holding) {
        if (holding) {
            latest.proxyHoldStarted()
            progress.snapTo(0f)
            progress.animateTo(1f, tween(HOLD_MS, easing = LinearEasing))
            latest.killProxy()
            holding = false
        } else {
            progress.snapTo(0f)
        }
    }
    Box(
        Modifier
            .padding(top = emDp(.55f))
            .fillMaxWidth()
            .background(AppTone.HoldSurface)
            .border(1.dp, Rust)
            .pointerInput(Unit) {
                detectTapGestures(onPress = {
                    holding = true
                    tryAwaitRelease()
                    holding = false
                })
            },
    ) {
        Box(
            Modifier
                .matchParentSize()
                .fillMaxHeight(),
        ) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(progress.value)
                    .background(AppTone.HoldFill),
            )
        }
        BasicText(
            text = stringResource(R.string.set_proxy_hold),
            style = type.ui.copy(fontSize = ems(.8f), letterSpacing = 0.03.em, color = AppTone.HoldText, textAlign = TextAlign.Center),
            modifier = Modifier
                .fillMaxWidth()
                .padding(emDp(.75f)),
        )
    }
}

private const val HOLD_MS = 1500
private val KILLED = Color(0xFFAA0000)
