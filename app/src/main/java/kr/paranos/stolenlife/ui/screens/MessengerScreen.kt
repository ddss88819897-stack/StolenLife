package kr.paranos.stolenlife.ui.screens

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import kr.paranos.stolenlife.R
import kr.paranos.stolenlife.data.ChatMessage
import kr.paranos.stolenlife.data.Chip
import kr.paranos.stolenlife.data.GameRepository
import kr.paranos.stolenlife.data.GameState
import kr.paranos.stolenlife.data.Inline
import kr.paranos.stolenlife.data.Sender
import kr.paranos.stolenlife.ui.components.BlinkText
import kr.paranos.stolenlife.ui.components.InkActions
import kr.paranos.stolenlife.ui.components.InkPalette
import kr.paranos.stolenlife.ui.components.InkState
import kr.paranos.stolenlife.ui.components.RetroButton
import kr.paranos.stolenlife.ui.components.RichText
import kr.paranos.stolenlife.ui.components.Side
import kr.paranos.stolenlife.ui.components.dashedBorder
import kr.paranos.stolenlife.ui.components.edge
import kr.paranos.stolenlife.ui.components.emDp
import kr.paranos.stolenlife.ui.components.ems
import kr.paranos.stolenlife.ui.components.plainInline
import kr.paranos.stolenlife.ui.theme.AppTone
import kr.paranos.stolenlife.ui.theme.ChatTone
import kr.paranos.stolenlife.ui.theme.Gold
import kr.paranos.stolenlife.ui.theme.Phosphor
import kr.paranos.stolenlife.ui.theme.Rust
import kr.paranos.stolenlife.ui.theme.StolenTheme
import kr.paranos.stolenlife.ui.theme.ToastTone
import kr.paranos.stolenlife.ui.theme.WindowTone
import kr.paranos.stolenlife.viewmodel.GameIntents

/**
 * WATSON 보안 메신저. 답장 없음 · 단방향 수신.
 * 새 메시지가 오거나 '입력 중…'이 뜨면 맨 아래로 내려간다.
 */
@Composable
fun MessengerContent(
    state: GameState,
    typing: Boolean,
    ink: InkState,
    width: Dp,
    scroll: ScrollState,
    intents: GameIntents,
) {
    val type = StolenTheme.type
    val contact = GameRepository.contactName(state)
    LaunchedEffect(state.messages.size, typing) {
        withFrameNanos { }
        scroll.animateScrollTo(scroll.maxValue)
    }
    val actions = object : InkActions {
        override fun onChip(chip: Chip) = intents.tapChip(chip)
        override fun onPin(pin: Inline.PinRef) = Unit
        override fun onRedaction() = intents.tapRedaction()
    }
    Column(Modifier.fillMaxWidth()) {
        state.messages.forEach { message -> Bubble(message, contact, ink, width, actions, state, intents) }
        if (typing) {
            BlinkText(
                text = stringResource(R.string.msg_typing, contact),
                style = type.ui.copy(fontSize = ems(.68f), letterSpacing = 0.08.em, color = AppTone.SubGreen),
                periodMs = 1100,
                modifier = Modifier.padding(emDp(.35f)),
            )
        }
        BasicText(
            text = stringResource(R.string.msg_footer),
            style = type.ui.copy(fontSize = ems(.6f), letterSpacing = 0.1.em, color = ChatTone.Footer, textAlign = TextAlign.Center),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = emDp(1.1f), bottom = emDp(.2f)),
        )
    }
}

@Composable
private fun Bubble(
    message: ChatMessage,
    contact: String,
    ink: InkState,
    width: Dp,
    actions: InkActions,
    state: GameState,
    intents: GameIntents,
) {
    val type = StolenTheme.type
    val small = type.ui.copy(fontSize = ems(.82f), lineHeight = ems(.82f * 1.6f), color = ChatTone.BubbleText)
    when (message.sender) {
        Sender.SYSTEM -> BasicText(
            text = message.text,
            style = type.ui.copy(fontSize = ems(.66f), letterSpacing = 0.04.em, color = AppTone.SubGreen, textAlign = TextAlign.Center),
            modifier = Modifier
                .padding(vertical = emDp(.9f))
                .fillMaxWidth()
                .dashedBorder(ChatTone.BubbleRule, start = false, end = false)
                .padding(emDp(.4f)),
        )

        Sender.ALERT -> BasicText(
            text = message.text,
            style = type.ui.copy(fontSize = ems(.76f), lineHeight = ems(.76f * 1.6f), color = ChatTone.AlertText),
            modifier = Modifier
                .padding(vertical = emDp(.45f))
                .fillMaxWidth()
                .background(ChatTone.AlertBubble)
                .border(1.dp, Rust)
                .edge(ChatTone.AlertEdge, 3.dp, Side.START)
                .padding(start = emDp(.75f) + 3.dp, end = emDp(.75f), top = emDp(.5f), bottom = emDp(.5f)),
        )

        Sender.POLICE_LOG -> BasicText(
            text = message.text,
            style = type.ui.copy(fontSize = ems(.66f), lineHeight = ems(.66f * 1.6f), color = Phosphor),
            modifier = Modifier
                .padding(vertical = emDp(.45f))
                .fillMaxWidth()
                .background(ChatTone.LogBubble)
                .border(1.dp, WindowTone.DisplayRule)
                .edge(Phosphor, 2.dp, Side.START)
                .padding(start = emDp(.75f) + 2.dp, end = emDp(.75f), top = emDp(.5f), bottom = emDp(.5f)),
        )

        Sender.WATSON -> Column(
            Modifier
                .padding(vertical = emDp(.45f))
                .widthIn(max = width * 0.86f)
                .background(ChatTone.Bubble)
                .border(1.dp, ChatTone.BubbleRule)
                .edge(ToastTone.Message, 2.dp, Side.START)
                .padding(start = emDp(.75f) + 2.dp, end = emDp(.75f), top = emDp(.5f), bottom = emDp(.5f)),
        ) {
            val head = buildAnnotatedString {
                withStyle(SpanStyle(color = Gold, fontWeight = FontWeight.Medium, fontSize = 0.8.em, letterSpacing = 0.1.em)) { append(contact) }
                withStyle(SpanStyle(color = ChatTone.Time, fontSize = 0.68.em)) { append("  " + message.time) }
            }
            BasicText(head, style = small)
            RichText(
                parts = message.rich ?: plainInline(message.text),
                style = small,
                ink = ink,
                palette = InkPalette.Light.copy(chipText = Gold, chipHeldWash = HELD_ON_DARK),
                actions = actions,
            )
            message.action?.let { action ->
                RetroButton(
                    text = action.label,
                    onClick = { intents.messageAction(action.command) },
                    enabled = action.command !in state.completedCommands,
                    line = Gold,
                    color = Gold,
                    modifier = Modifier
                        .padding(top = emDp(.65f))
                        .fillMaxWidth(),
                )
            }
        }
    }
}

private val HELD_ON_DARK = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.07f)
