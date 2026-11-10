package kr.paranos.stolenlife.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import kr.paranos.stolenlife.R
import kr.paranos.stolenlife.data.GameRepository
import kr.paranos.stolenlife.ui.components.ChromaText
import kr.paranos.stolenlife.ui.components.RetroButton
import kr.paranos.stolenlife.ui.components.emDp
import kr.paranos.stolenlife.ui.components.ems
import kr.paranos.stolenlife.ui.theme.AlertTone
import kr.paranos.stolenlife.ui.theme.BootTone
import kr.paranos.stolenlife.ui.theme.Gold
import kr.paranos.stolenlife.ui.theme.Paper
import kr.paranos.stolenlife.ui.theme.StolenTheme
import kr.paranos.stolenlife.viewmodel.BootInfo
import kr.paranos.stolenlife.viewmodel.RaidStage

/** 부팅: BIOS 로그 → 타이틀 → 새로 시작 / 이어하기 */
@Composable
fun BootScreen(boot: BootInfo, onNew: () -> Unit, onContinue: () -> Unit, modifier: Modifier = Modifier) {
    val type = StolenTheme.type
    Column(
        modifier
            .fillMaxSize()
            .background(BootTone.Surface)
            .padding(horizontal = emDp(1.3f), vertical = emDp(1.5f)),
    ) {
        BasicText(
            text = stringResource(R.string.boot_bios),
            style = type.terminal.copy(fontSize = ems(.76f), lineHeight = ems(.76f * 1.75f), color = BootTone.Text),
            modifier = Modifier.weight(1f),
        )
        BasicText(
            text = stringResource(R.string.boot_title),
            style = type.chapterTitle.copy(fontSize = ems(.76f * 2.35f), lineHeight = ems(2.3f), color = Paper, textAlign = TextAlign.Center),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = emDp(.8f), bottom = emDp(.2f)),
        )
        BasicText(
            text = stringResource(R.string.boot_subtitle),
            style = type.ui.copy(fontSize = ems(.61f), letterSpacing = 0.5.em, color = Gold, textAlign = TextAlign.Center),
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = emDp(.3f), bottom = emDp(1.7f)),
        )
        Column(
            Modifier
                .fillMaxWidth()
                .padding(bottom = emDp(1.4f)),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(emDp(.5f)),
        ) {
            RetroButton(stringResource(R.string.boot_new), onNew, Modifier.widthIn(min = emDp(10f)))
            if (boot.canContinue) RetroButton(stringResource(R.string.boot_continue), onContinue, Modifier.widthIn(min = emDp(10f)))
        }
        BasicText(
            text = stringResource(R.string.boot_foot),
            style = type.ui.copy(fontSize = ems(.61f), lineHeight = ems(1.1f), color = BootTone.Foot, textAlign = TextAlign.Center),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/**
 * 경찰 진입. 상태바 + 화면 영역을 검게 덮고(독백 바와 하단 바는 남긴다), 손전등 빛이 방을 훑는다.
 *  BANG      쾅—!
 *  SHOUT     "경찰이다! 강민우, 움직이지 마!"
 *  AFTERMATH 손전등이 잦아들고, 떨어진 화면 위로 수갑 채워지는 손 그림자.
 * 손전등은 그라데이션 대신 세 겹의 계단식 원으로 그린다.
 */
@Composable
fun RaidOverlay(stage: RaidStage, modifier: Modifier = Modifier) {
    val type = StolenTheme.type
    val ending = GameRepository.ending
    val sweep = rememberInfiniteTransition(label = "torch")
    val t by sweep.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "torchSweep",
    )
    val dim = remember { Animatable(1f) }
    LaunchedEffect(stage) { if (stage == RaidStage.AFTERMATH) dim.animateTo(0.22f, tween(400)) }

    BoxWithConstraints(
        modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(remember { MutableInteractionSource() }, indication = null) { },
    ) {
        val w = maxWidth
        val h = maxHeight
        Canvas(Modifier.fillMaxSize().alpha(dim.value)) {
            // inset:-30% 상자 안에서 translate(-25%,-10%) ↔ (20%,15%)
            val box = size.width * 1.6f
            val cx = size.width / 2 + box * (-0.25f + 0.45f * t)
            val cy = size.height / 2 + box * (-0.10f + 0.25f * t)
            val center = Offset(cx, cy)
            drawCircle(AlertTone.Torch.copy(alpha = 0.05f), box * 0.30f, center)
            drawCircle(AlertTone.Torch.copy(alpha = 0.12f), box * 0.20f, center)
            drawCircle(AlertTone.Torch.copy(alpha = 0.32f), box * 0.12f, center)
            drawCircle(AlertTone.Torch.copy(alpha = 0.7f), box * 0.05f, center)
        }
        if (stage == RaidStage.AFTERMATH) {
            Box(
                Modifier
                    .offset(x = w * 0.22f, y = h * 0.18f + h * 0.64f * 0.3f)
                    .size(w * 0.56f, h * 0.64f)
                    .graphicsLayer { rotationZ = -24f; alpha = 0.8f }
                    .background(AlertTone.RaidDrop)
                    .border(1.dp, AlertTone.RaidDropRule),
            )
        }
        if (stage != RaidStage.AFTERMATH) {
            ChromaText(
                text = ending.raidBang,
                style = type.chapterTitle.copy(fontSize = ems(3f), lineHeight = ems(3.6f), color = Paper, textAlign = TextAlign.Center),
                spread = 2f,
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = h * 0.28f),
            )
        }
        if (stage != RaidStage.BANG) {
            BasicText(
                text = if (stage == RaidStage.SHOUT) ending.raidShout else ending.raidAftermath,
                style = type.ui.copy(fontSize = ems(1f), lineHeight = ems(1.7f), color = AlertTone.RaidShout, textAlign = TextAlign.Center),
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = h * 0.52f)
                    .padding(horizontal = emDp(1.2f)),
            )
        }
    }
}
