package kr.paranos.stolenlife.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import kr.paranos.stolenlife.R
import kr.paranos.stolenlife.ui.theme.Gold
import kr.paranos.stolenlife.ui.theme.Mute
import kr.paranos.stolenlife.ui.theme.Paper
import kr.paranos.stolenlife.ui.theme.StolenTheme
import kr.paranos.stolenlife.ui.theme.ToastTone
import kr.paranos.stolenlife.viewmodel.Toast
import kr.paranos.stolenlife.viewmodel.ToastContent

/**
 * 상단 알림. 최대 3장, 4.2초 뒤 사라진다(ViewModel 이 관리). 태그는 전부 텍스트: [MSG] [FILE] [SPAM] [WARN].
 */
@Composable
fun ToastStack(toasts: List<Toast>, onTap: (Toast) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(emDp(.4f))) {
        toasts.forEach { toast -> key(toast.id) { ToastCard(toast) { onTap(toast) } } }
    }
}

private val DropEasing = CubicBezierEasing(.2f, .8f, .2f, 1f)

@Composable
private fun ToastCard(toast: Toast, onTap: () -> Unit) {
    val type = StolenTheme.type
    val drop = remember { Animatable(0f) }
    LaunchedEffect(Unit) { drop.animateTo(1f, tween(280, easing = DropEasing)) }

    val spam = toast.content is ToastContent.Spam
    val accent = when (toast.content) {
        is ToastContent.Message -> ToastTone.Message
        is ToastContent.Spam -> kr.paranos.stolenlife.ui.theme.Rust
        is ToastContent.ChipAcquired, is ToastContent.Restored -> Gold
    }
    val tag = stringResource(
        when (toast.content) {
            is ToastContent.Message -> R.string.toast_tag_msg
            is ToastContent.ChipAcquired -> R.string.toast_tag_file
            is ToastContent.Spam -> R.string.toast_tag_spam
            is ToastContent.Restored -> R.string.toast_tag_warn
        },
    )
    val newMessage = stringResource(R.string.toast_new_message)
    val acquired = stringResource(R.string.toast_chip_acquired)
    val restored = (toast.content as? ToastContent.Restored)?.let {
        stringResource(R.string.toast_restored, it.titles.size, it.titles.joinToString(" / "))
    }
    val text = buildAnnotatedString {
        withStyle(SpanStyle(color = if (spam) ToastTone.SpamTag else Mute, fontSize = 0.86.em, letterSpacing = 0.08.em)) { append(tag) }
        append("  ")
        when (val content = toast.content) {
            is ToastContent.Message -> {
                withStyle(SpanStyle(color = Gold, fontWeight = FontWeight.Medium)) { append(content.contact) }
                append("  ")
                append(content.text.ifEmpty { newMessage })
            }

            is ToastContent.ChipAcquired -> {
                append(acquired)
                append("  ")
                withStyle(SpanStyle(color = Gold, fontWeight = FontWeight.Medium)) { append(content.chip.label) }
            }

            is ToastContent.Spam -> append(content.text)
            is ToastContent.Restored -> append(restored.orEmpty())
        }
    }
    BasicText(
        text = text,
        style = type.ui.copy(fontSize = ems(.73f), lineHeight = ems(1.06f), color = if (spam) ToastTone.SpamText else Paper),
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                translationY = (1f - drop.value) * -12.dp.toPx()
                alpha = drop.value
            }
            .hardShadow(2.dp)
            .background(if (spam) ToastTone.SpamSurface else ToastTone.Surface)
            .border(1.dp, ToastTone.Border)
            .edge(accent, 3.dp, Side.START)
            .then(if (toast.action != null) Modifier.tap(onClick = onTap) else Modifier)
            .padding(start = emDp(.65f) + 3.dp, end = emDp(.65f), top = emDp(.42f), bottom = emDp(.42f)),
    )
}
