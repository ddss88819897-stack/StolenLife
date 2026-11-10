package kr.paranos.stolenlife.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import kr.paranos.stolenlife.R
import kr.paranos.stolenlife.data.GameRepository
import kr.paranos.stolenlife.ui.theme.AlertTone
import kr.paranos.stolenlife.ui.theme.Blood
import kr.paranos.stolenlife.ui.theme.Gold
import kr.paranos.stolenlife.ui.theme.Ink
import kr.paranos.stolenlife.ui.theme.Ink2
import kr.paranos.stolenlife.ui.theme.Mute
import kr.paranos.stolenlife.ui.theme.Paper
import kr.paranos.stolenlife.ui.theme.Phosphor
import kr.paranos.stolenlife.ui.theme.Rust
import kr.paranos.stolenlife.ui.theme.ShellTone
import kr.paranos.stolenlife.ui.theme.Slate
import kr.paranos.stolenlife.ui.theme.StolenTheme
import kr.paranos.stolenlife.ui.theme.TextMain
import kr.paranos.stolenlife.ui.theme.TitleGreen
import kr.paranos.stolenlife.ui.theme.VaultTone
import kr.paranos.stolenlife.ui.theme.WindowTone
import kr.paranos.stolenlife.viewmodel.GameIntents
import kr.paranos.stolenlife.viewmodel.KeypadKey
import kr.paranos.stolenlife.viewmodel.Overlay

/*
 * 화면 전체를 덮는 창(웹판 #ov). 한 번에 하나만 뜬다.
 * 바깥(어두운 막)을 눌러 닫을 수 있는 것은 키패드와 '처음부터 다시' 확인창뿐이다.
 */

@Composable
fun OverlayHost(overlay: Overlay, intents: GameIntents, modifier: Modifier = Modifier) {
    val scrim = if (overlay is Overlay.Chapter) ShellTone.CardScrim else ShellTone.OverlayScrim
    Box(
        modifier
            .fillMaxSize()
            .background(scrim)
            .clickable(remember { MutableInteractionSource() }, indication = null) {
                when (overlay) {
                    is Overlay.Chapter -> intents.continueChapter()
                    is Overlay.Keypad, Overlay.ResetConfirm -> intents.closeOverlay()
                    else -> Unit
                }
            }
            .padding(emDp(1.1f)),
        contentAlignment = Alignment.Center,
    ) {
        when (overlay) {
            is Overlay.Chapter -> ChapterCardView(overlay)
            is Overlay.Keypad -> KeypadWindow(overlay, intents)
            is Overlay.CrossCheck -> CrossCheckWindow(overlay, intents)
            Overlay.Otp -> OtpWindow(intents)
            Overlay.ResetConfirm -> ResetWindow(intents)
            is Overlay.Fin -> FinWindow(overlay, intents)
        }
    }
}

/** 윈도우95 창: 타이틀바 + 본문. 창 안을 눌러도 바깥 막으로 클릭이 새지 않는다. */
@Composable
fun RetroWindow(
    title: String,
    modifier: Modifier = Modifier,
    titleBar: Color = TitleGreen,
    trailing: @Composable () -> Unit = {},
    content: @Composable () -> Unit,
) {
    val type = StolenTheme.type
    Column(
        modifier
            .widthIn(max = emDp(23f))
            .fillMaxWidth()
            .hardShadow(2.dp)
            .bevel()
            .background(Slate)
            .clickable(remember { MutableInteractionSource() }, indication = null) { },
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .background(titleBar)
                .edge(Color.Black, 1.dp, Side.BOTTOM)
                .padding(horizontal = emDp(.75f), vertical = emDp(.45f)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicText(title, style = type.ui.copy(fontSize = ems(.72f), letterSpacing = 0.1.em, color = Paper), modifier = Modifier.weight(1f))
            trailing()
        }
        Box(Modifier.padding(emDp(.9f))) { content() }
    }
}

@Composable
private fun CloseGlyph(onClick: () -> Unit) {
    val cd = stringResource(R.string.close_cd)
    BasicText(
        text = stringResource(R.string.close_glyph),
        style = StolenTheme.type.ui.copy(fontSize = ems(1.1f), lineHeight = ems(1.1f), color = Mute),
        modifier = Modifier
            .semantics { contentDescription = cd }
            .tap(onClick = onClick)
            .padding(horizontal = emDp(.2f)),
    )
}

// ── 키패드 ───────────────────────────────────────────────────────────────────

@Composable
private fun KeypadWindow(pad: Overlay.Keypad, intents: GameIntents) {
    val type = StolenTheme.type
    val spec = GameRepository.locks.getValue(pad.lock)
    val shake = remember { Animatable(0f) }
    LaunchedEffect(pad.shake) {
        if (pad.shake == 0) return@LaunchedEffect
        shake.animateTo(
            0f,
            keyframes {
                durationMillis = 350
                -5f at 87
                5f at 262
                0f at 350
            },
        )
    }
    val bad = pad.shake > 0 && pad.entry.isEmpty()
    RetroWindow(
        title = stringResource(R.string.keypad_title, spec.title),
        trailing = { CloseGlyph(intents::closeOverlay) },
    ) {
        Column {
            BasicText(
                text = stringResource(R.string.keypad_hint, spec.hint),
                style = type.ui.copy(fontSize = ems(.74f), lineHeight = ems(1.1f), color = WindowTone.HintText),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(WindowTone.HintSurface)
                    .edge(Gold, 2.dp, Side.START)
                    .padding(start = emDp(.65f) + 2.dp, end = emDp(.65f), top = emDp(.35f), bottom = emDp(.35f)),
            )
            Box(
                Modifier
                    .padding(vertical = emDp(.65f))
                    .offset(x = shake.value.dp)
                    .fillMaxWidth()
                    .heightIn(min = emDp(3f))
                    .background(WindowTone.Display)
                    .border(1.dp, WindowTone.DisplayRule)
                    .padding(emDp(.4f)),
                contentAlignment = Alignment.Center,
            ) {
                BasicText(
                    text = pad.entry.ifEmpty { stringResource(R.string.keypad_placeholder) },
                    style = type.keypadDisplay.copy(
                        fontSize = ems(1.5f),
                        lineHeight = ems(1.9f),
                        letterSpacing = 0.24.em,
                        textAlign = TextAlign.Center,
                        color = when {
                            pad.entry.isEmpty() && !bad -> WindowTone.DisplayPlaceholder
                            bad -> AlertTone.Text
                            else -> Phosphor
                        },
                    ),
                )
            }
            // 1px 검은 간격 3×4 격자
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(Color.Black)
                    .border(1.dp, Color.Black)
                    .padding(1.dp),
                verticalArrangement = Arrangement.spacedBy(1.dp),
            ) {
                val rows = listOf(
                    listOf(KeypadKey.Digit(1), KeypadKey.Digit(2), KeypadKey.Digit(3)),
                    listOf(KeypadKey.Digit(4), KeypadKey.Digit(5), KeypadKey.Digit(6)),
                    listOf(KeypadKey.Digit(7), KeypadKey.Digit(8), KeypadKey.Digit(9)),
                    listOf(KeypadKey.Delete, KeypadKey.Digit(0), KeypadKey.Enter),
                )
                rows.forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(1.dp)) {
                        row.forEach { key -> KeypadButton(key, type.keypad, Modifier.weight(1f)) { intents.pressKey(key) } }
                    }
                }
            }
        }
    }
}

@Composable
private fun KeypadButton(key: KeypadKey, style: TextStyle, modifier: Modifier, onClick: () -> Unit) {
    val enter = key == KeypadKey.Enter
    val label = when (key) {
        is KeypadKey.Digit -> key.value.toString()
        KeypadKey.Delete -> stringResource(R.string.keypad_delete)
        KeypadKey.Enter -> stringResource(R.string.keypad_enter)
    }
    val cd = when (key) {
        is KeypadKey.Digit -> label
        KeypadKey.Delete -> stringResource(R.string.keypad_delete_cd)
        KeypadKey.Enter -> stringResource(R.string.keypad_enter_cd)
    }
    BasicText(
        text = label,
        style = style.copy(fontSize = ems(1.15f), lineHeight = ems(1.4f), textAlign = TextAlign.Center, color = if (enter) Gold else Paper),
        modifier = modifier
            .pressSurface(if (enter) TitleGreen else WindowTone.Key, WindowTone.KeyPressed, onClick = onClick)
            .semantics { contentDescription = cd }
            .padding(vertical = emDp(.72f)),
    )
}

// ── 교차검증 ─────────────────────────────────────────────────────────────────

@Composable
private fun CrossCheckWindow(cross: Overlay.CrossCheck, intents: GameIntents) {
    val type = StolenTheme.type
    RetroWindow(
        title = stringResource(R.string.cross_title),
        titleBar = Blood,
        trailing = { BasicText(cross.clock, style = type.ui.copy(fontSize = ems(.72f), letterSpacing = 0.1.em, color = Paper)) },
    ) {
        Column {
            BasicText(
                text = stringResource(R.string.cross_heading),
                style = type.keypad.copy(fontSize = ems(1f), letterSpacing = 0.24.em, color = WindowTone.CrossHeading, textAlign = TextAlign.Center),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = emDp(.1f), bottom = emDp(.7f)),
            )
            EvidenceCard(stringResource(R.string.cross_side_a, cross.a.source), cross.a.text)
            BasicText(
                text = stringResource(R.string.cross_versus),
                style = type.ui.copy(fontSize = ems(.66f), letterSpacing = 0.3.em, color = Gold, textAlign = TextAlign.Center),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = emDp(.3f)),
            )
            EvidenceCard(stringResource(R.string.cross_side_b, cross.bSource), cross.bText)
            BasicText(
                text = cross.conclusion,
                style = type.ui.copy(fontSize = ems(.86f), lineHeight = ems(.86f * 1.6f), color = Gold),
                modifier = Modifier
                    .padding(top = emDp(.7f))
                    .fillMaxWidth()
                    .background(WindowTone.CrossConclusion)
                    .border(1.dp, Gold)
                    .padding(horizontal = emDp(.75f), vertical = emDp(.7f)),
            )
            Row(Modifier.fillMaxWidth().padding(top = emDp(.85f)), horizontalArrangement = Arrangement.End) {
                RetroButton(stringResource(R.string.ok), intents::confirmCrossCheck)
            }
        }
    }
}

@Composable
private fun EvidenceCard(label: String, text: String) {
    val type = StolenTheme.type
    Column(
        Modifier
            .fillMaxWidth()
            .padding(vertical = emDp(.35f))
            .background(Paper)
            .edge(Rust, 3.dp, Side.START)
            .padding(start = emDp(.75f) + 3.dp, end = emDp(.75f), top = emDp(.55f), bottom = emDp(.55f)),
    ) {
        BasicText(label, style = type.ui.copy(fontSize = ems(.6f), letterSpacing = 0.02.em, color = Ink2))
        Spacer(Modifier.height(emDp(.15f)))
        BasicText(text, style = type.docBody.copy(fontSize = ems(.84f), lineHeight = ems(.84f * 1.6f), color = Ink))
    }
}

// ── 챕터 카드 ────────────────────────────────────────────────────────────────

@Composable
private fun ChapterCardView(chapter: Overlay.Chapter) {
    val type = StolenTheme.type
    val card = chapter.card
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        BasicText(
            text = stringResource(R.string.chapter_number, card.number),
            style = type.keypad.copy(fontSize = ems(.82f), letterSpacing = 0.55.em, color = Gold),
        )
        Swatch(Gold.copy(alpha = 0.55f), Modifier.padding(vertical = emDp(1.1f)).width(emDp(3.2f)).height(1.dp))
        ChromaText(
            text = card.title,
            style = type.chapterTitle.copy(fontSize = ems(1.95f), lineHeight = ems(2.5f), color = Paper, textAlign = TextAlign.Center),
            modifier = Modifier.padding(top = emDp(.15f), bottom = emDp(.5f)),
        )
        BasicText(
            text = card.place + "\n" + stringResource(R.string.chapter_date, chapter.clock),
            style = type.ui.copy(fontSize = ems(.74f), lineHeight = ems(.74f * 1.8f), color = Mute, textAlign = TextAlign.Center),
        )
        Spacer(Modifier.height(emDp(3f)))
        BlinkText(
            text = stringResource(R.string.chapter_tap),
            style = type.ui.copy(fontSize = ems(.64f), letterSpacing = 0.32.em, color = WindowTone.ChapterHint),
            periodMs = 1600,
        )
    }
}

/** 색수차 제목: 붉은색 · 청록색 사본을 1dp 씩 어긋나게 깔고 위에 본문을 찍는다(text-shadow, 블러 없음). */
@Composable
fun ChromaText(text: String, style: TextStyle, modifier: Modifier = Modifier, spread: Float = 1f) {
    Box(modifier) {
        BasicText(text, style = style.copy(color = AlertTone.ChromaRed), modifier = Modifier.fillMaxWidth().offset(x = spread.dp))
        BasicText(text, style = style.copy(color = AlertTone.ChromaTeal), modifier = Modifier.fillMaxWidth().offset(x = (-spread).dp))
        BasicText(text, style = style, modifier = Modifier.fillMaxWidth())
    }
}

// ── 하드웨어 OTP · 처음부터 다시 · FIN ──────────────────────────────────────

@Composable
private fun OtpWindow(intents: GameIntents) {
    val type = StolenTheme.type
    RetroWindow(stringResource(R.string.otp_title)) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            BasicText(
                text = GameRepository.room.otpCode,
                style = type.keypadDisplay.copy(fontSize = ems(2f), lineHeight = ems(2.4f), letterSpacing = 0.25.em, color = VaultTone.OtpDigits, textAlign = TextAlign.Center),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(WindowTone.Display)
                    .border(1.dp, VaultTone.OtpRule)
                    .padding(emDp(.35f)),
            )
            BasicText(
                text = stringResource(R.string.otp_sent),
                style = type.ui.copy(fontSize = ems(.74f), color = Mute, textAlign = TextAlign.Center),
                modifier = Modifier.padding(vertical = emDp(.8f)),
            )
            RetroButton(stringResource(R.string.ok), intents::confirmOtp)
        }
    }
}

@Composable
private fun ResetWindow(intents: GameIntents) {
    val type = StolenTheme.type
    RetroWindow(stringResource(R.string.set_reset)) {
        Column {
            BasicText(stringResource(R.string.set_reset_confirm), style = type.ui.copy(fontSize = ems(.86f), lineHeight = ems(1.3f), color = TextMain))
            Row(Modifier.fillMaxWidth().padding(top = emDp(.8f)), horizontalArrangement = Arrangement.End) {
                RetroButton(stringResource(R.string.yes), { intents.answerReset(true) })
                Spacer(Modifier.width(emDp(.4f)))
                RetroButton(stringResource(R.string.no), { intents.answerReset(false) })
            }
        }
    }
}

@Composable
private fun FinWindow(fin: Overlay.Fin, intents: GameIntents) {
    val type = StolenTheme.type
    Column(
        Modifier
            .fillMaxWidth()
            .background(ShellTone.CardScrim)
            .border(1.dp, AlertTone.FinRule)
            .clickable(remember { MutableInteractionSource() }, indication = null) { }
            .padding(horizontal = emDp(1.2f), vertical = emDp(2.3f)),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        BasicText(
            text = stringResource(R.string.fin_title),
            style = type.fin.copy(fontSize = ems(2.8f), lineHeight = ems(3.2f), letterSpacing = 0.5.em, color = Paper, textAlign = TextAlign.Center),
            modifier = Modifier.padding(start = emDp(1.4f)),
        )
        BasicText(
            text = stringResource(R.string.fin_subtitle),
            style = type.docTitle.copy(fontSize = ems(1f), color = AlertTone.FinSub, textAlign = TextAlign.Center),
            modifier = Modifier.padding(top = emDp(.3f)),
        )
        BasicText(
            text = stringResource(R.string.fin_stats, fin.searches, fin.deductions, fin.contacts, fin.logs),
            style = type.ui.copy(fontSize = ems(.72f), lineHeight = ems(1.44f), color = Mute, textAlign = TextAlign.Center),
            modifier = Modifier.padding(vertical = emDp(1.5f)),
        )
        RetroButton(stringResource(R.string.fin_restart), intents::restartFromFin)
    }
}
