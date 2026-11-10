package kr.paranos.stolenlife.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import kr.paranos.stolenlife.R
import kr.paranos.stolenlife.data.GameRepository
import kr.paranos.stolenlife.data.GameState
import kr.paranos.stolenlife.data.LockId
import kr.paranos.stolenlife.data.RoomSpot
import kr.paranos.stolenlife.ui.components.withPressSound
import kr.paranos.stolenlife.ui.components.PixelArt
import kr.paranos.stolenlife.ui.components.PixelImage
import kr.paranos.stolenlife.ui.components.RetroButton
import kr.paranos.stolenlife.ui.components.Side
import kr.paranos.stolenlife.ui.components.blinkAlpha
import kr.paranos.stolenlife.ui.components.edge
import kr.paranos.stolenlife.ui.components.emDp
import kr.paranos.stolenlife.ui.components.ems
import kr.paranos.stolenlife.ui.components.pressSurface
import kr.paranos.stolenlife.ui.theme.Gold
import kr.paranos.stolenlife.ui.theme.Mute
import kr.paranos.stolenlife.ui.theme.RoomTone
import kr.paranos.stolenlife.ui.theme.StolenTheme
import kr.paranos.stolenlife.viewmodel.GameIntents

private enum class SpotState { IDLE, HOT, DONE }

private fun GameState.spotState(spot: RoomSpot): SpotState {
    val done = when (spot) {
        RoomSpot.COFFEE -> coffeeAt != null
        RoomSpot.VENT -> binderFound
        RoomSpot.MIRROR -> mirrorAt != null
        RoomSpot.OTP -> otpPressed
        RoomSpot.DOOR, RoomSpot.WINDOW, RoomSpot.BED -> spot in glancedSpots
    }
    val hot = when (spot) {
        RoomSpot.OTP -> LockId.VAULT in unlocked && !otpPressed
        RoomSpot.MIRROR -> chapter >= 3 && mirrorAt == null
        RoomSpot.COFFEE -> coffeeAt == null
        RoomSpot.VENT -> coffeeAt != null && !binderFound
        else -> false
    }
    return when {
        done -> SpotState.DONE
        hot -> SpotState.HOT
        else -> SpotState.IDLE
    }
}

/** 파란여관 203호: 방 그림 위 `[ + ]` 조사 지점 + 아래 조사 목록 + 읽은 기록 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RoomContent(state: GameState, intents: GameIntents) {
    Column(Modifier.fillMaxWidth()) {
        BoxWithConstraints(
            Modifier
                .fillMaxWidth()
                .aspectRatio(3f / 4f)
                .edge(ROOM_RULE, 1.dp, Side.BOTTOM),
        ) {
            PixelImage(PixelArt.room(), Modifier.fillMaxSize())
            if (state.chapter >= 3) Siren(maxWidth, maxHeight)
            RoomSpot.entries.forEach { spot ->
                Hotspot(spot, state.spotState(spot), maxWidth, maxHeight) { intents.inspect(spot) }
            }
        }
        Column(Modifier.padding(start = emDp(.9f), end = emDp(.9f), top = emDp(.7f), bottom = emDp(1.2f))) {
            Heading(stringResource(R.string.room_inspect))
            RoomSpot.entries.forEach { spot -> InspectRow(spot, state.spotState(spot)) { intents.inspect(spot) } }
            val records = GameRepository.room.records.filter { (doc, _) -> state.hasRead(doc) }
            if (records.isNotEmpty()) {
                Spacer(Modifier.size(emDp(1.2f)))
                Heading(stringResource(R.string.room_records))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(emDp(.45f)), verticalArrangement = Arrangement.spacedBy(emDp(.45f))) {
                    records.forEach { (doc, label) -> RetroButton(label, { intents.openDocument(doc) }) }
                }
            }
        }
    }
}

@Composable
private fun Heading(text: String) {
    BasicText(
        text = text,
        style = StolenTheme.type.ui.copy(fontSize = ems(.62f), letterSpacing = 0.16.em, color = Mute),
        modifier = Modifier.padding(top = emDp(.2f), bottom = emDp(.4f)),
    )
}

/** 방 그림 위 조사 지점. 맥동(.35↔1, 2.6초) · 조사 완료는 흐리게 고정 · 단서 있음은 금색. 누르는 동안 이름표. */
@Composable
private fun Hotspot(spot: RoomSpot, spotState: SpotState, width: androidx.compose.ui.unit.Dp, height: androidx.compose.ui.unit.Dp, onClick: () -> Unit) {
    val type = StolenTheme.type
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val pulse = rememberInfiniteTransition(label = "spot")
    val phase by pulse.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1300, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulse",
    )
    val alpha = if (spotState == SpotState.DONE) 0.28f else 0.35f + 0.65f * phase
    val scale = if (spotState == SpotState.DONE) 1f else 0.9f + 0.1f * phase
    Box(
        Modifier
            .offset(x = width * (spot.leftPct / 100f), y = height * (spot.topPct / 100f))
            .size(width * (spot.widthPct / 100f), height * (spot.heightPct / 100f))
            .semantics { contentDescription = spot.label }
            .clickable(source, indication = null, onClick = withPressSound(onClick)),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            text = stringResource(R.string.room_marker),
            style = type.ui.copy(fontSize = ems(.7f), letterSpacing = (-0.04).em, color = if (spotState == SpotState.HOT) Gold else RoomTone.Marker),
            modifier = Modifier
                .alpha(alpha)
                .scale(scale)
                .background(MARKER_WELL)
                .padding(horizontal = 2.dp),
        )
        if (pressed) {
            BasicText(
                text = spot.label,
                style = type.ui.copy(fontSize = ems(.56f), letterSpacing = 0.1.em, color = RoomTone.MarkerName),
                modifier = Modifier
                    .offset(y = emDp(1.1f))
                    .background(Color.Black.copy(alpha = 0.7f))
                    .padding(horizontal = emDp(.35f)),
            )
        }
    }
}

/** 3장: 창문에 붉고 푸른 경광등 (1.2초, 두 단계) */
@Composable
private fun Siren(width: androidx.compose.ui.unit.Dp, height: androidx.compose.ui.unit.Dp) {
    val red = blinkAlpha(1200, low = 0f)
    val window = RoomSpot.WINDOW
    Box(
        Modifier
            .offset(x = width * (window.leftPct / 100f), y = height * (window.topPct / 100f))
            .size(width * (window.widthPct / 100f), height * (window.heightPct / 100f))
            .background(if (red > 0.5f) RoomTone.SirenRed else RoomTone.SirenBlue),
    )
}

@Composable
private fun InspectRow(spot: RoomSpot, spotState: SpotState, onClick: () -> Unit) {
    val type = StolenTheme.type
    val (mark, label, status) = when (spotState) {
        SpotState.DONE -> Triple(RoomTone.DoneMark, RoomTone.DoneText, Mute)
        SpotState.HOT -> Triple(Gold, RoomTone.ListText, Gold)
        SpotState.IDLE -> Triple(RoomTone.ListMark, RoomTone.ListText, Mute)
    }
    Row(
        Modifier
            .fillMaxWidth()
            .edge(RoomTone.ListRule, 1.dp, Side.BOTTOM)
            .pressSurface(Color.Transparent, RoomTone.ListPressed, onClick = onClick)
            .padding(horizontal = emDp(.15f), vertical = emDp(.58f)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicText(stringResource(R.string.room_marker), style = type.ui.copy(fontSize = ems(.66f), color = mark))
        Spacer(Modifier.width(emDp(.75f)))
        BasicText(spot.label, style = type.ui.copy(fontSize = ems(.8f), color = label), modifier = Modifier.weight(1f))
        BasicText(
            text = stringResource(
                when (spotState) {
                    SpotState.DONE -> R.string.room_state_done
                    SpotState.HOT -> R.string.room_state_hot
                    SpotState.IDLE -> R.string.room_state_idle
                },
            ),
            style = type.ui.copy(fontSize = ems(.56f), letterSpacing = 0.06.em, color = status),
        )
    }
}

private val ROOM_RULE = Color(0xFF1D2220)
private val MARKER_WELL = Color(0x80080908)
