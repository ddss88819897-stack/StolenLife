package kr.paranos.stolenlife.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import kotlinx.coroutines.delay
import kr.paranos.stolenlife.R
import kr.paranos.stolenlife.data.GameTuning
import kr.paranos.stolenlife.ui.theme.AlertTone
import kr.paranos.stolenlife.ui.theme.Gold
import kr.paranos.stolenlife.ui.theme.LineGreen
import kr.paranos.stolenlife.ui.theme.Mute
import kr.paranos.stolenlife.ui.theme.Paper
import kr.paranos.stolenlife.ui.theme.Phosphor
import kr.paranos.stolenlife.ui.theme.Rust
import kr.paranos.stolenlife.ui.theme.ShellTone
import kr.paranos.stolenlife.ui.theme.StolenShapes
import kr.paranos.stolenlife.ui.theme.StolenTheme
import kr.paranos.stolenlife.ui.theme.Void
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.FilterQuality

val StatusBarHeight: Dp = 22.dp
val NavBarHeight: Dp = 36.dp

// ═════════════════════════════════════════════════════════════════════════════
//  상태바: 시계 · 등대 · ParanOS 98 · P98 · 신호 · 배터리
// ═════════════════════════════════════════════════════════════════════════════

@Composable
fun StatusBar(
    clock: String,
    battery: Int,
    proxyOff: Boolean,
    beacon: Int,
    beaconRed: Boolean,
    modifier: Modifier = Modifier,
) {
    val type = StolenTheme.type
    val label = type.ui.copy(fontSize = ems(.68f), letterSpacing = 0.06.em, color = Mute)
    Row(
        modifier
            .fillMaxWidth()
            .height(StatusBarHeight)
            .background(Void)
            .edge(ShellTone.StatusRule, 1.dp, Side.BOTTOM)
            .padding(horizontal = emDp(.9f)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicText(clock, style = label)
        Spacer(Modifier.weight(1f))
        Beacon(beacon, beaconRed)
        Spacer(Modifier.width(emDp(.35f)))
        BasicText(stringResource(R.string.os_name).uppercase(), style = label)
        Spacer(Modifier.weight(1f))
        ProxyBadge(proxyOff, label)
        Spacer(Modifier.width(emDp(.5f)))
        SignalBars()
        Spacer(Modifier.width(emDp(.5f)))
        BatteryGauge(battery, label)
    }
}

/**
 * 등대 스프라이트. 신호가 나갈 때마다 0.9초 주기로 세 번 번쩍인다.
 * 붉은 신호(경찰청으로 기록 전송)는 다음 신호 전까지 붉게 남는다.
 */
@Composable
private fun Beacon(seq: Int, red: Boolean) {
    var bright by remember { mutableStateOf(false) }
    LaunchedEffect(seq) {
        if (seq == 0) return@LaunchedEffect
        repeat(3) {
            bright = true
            delay(450)
            bright = false
            delay(450)
        }
    }
    val gain = if (bright) 2.4f else 1f
    val (r, g, b) = if (red) Triple(1.15f, 0.42f, 0.36f) else Triple(1f, 1f, 1f)
    val filter = ColorFilter.colorMatrix(
        ColorMatrix(
            floatArrayOf(
                r * gain, 0f, 0f, 0f, 0f,
                0f, g * gain, 0f, 0f, 0f,
                0f, 0f, b * gain, 0f, 0f,
                0f, 0f, 0f, 1f, 0f,
            ),
        ),
    )
    Image(
        bitmap = PixelArt.beacon(),
        contentDescription = null,
        filterQuality = FilterQuality.None,
        colorFilter = filter,
        modifier = Modifier
            .size(width = 11.dp, height = 16.dp)
            .alpha(if (bright) 1f else 0.85f),
    )
}

@Composable
private fun ProxyBadge(off: Boolean, label: androidx.compose.ui.text.TextStyle) {
    BasicText(
        text = stringResource(if (off) R.string.proxy_off else R.string.proxy_on),
        style = label.copy(color = if (off) AlertTone.Text else Phosphor, fontSize = label.fontSize * 0.92f),
        modifier = Modifier
            .alpha(if (off) blinkAlpha(1200) else 1f)
            .border(1.dp, if (off) Rust else LineGreen)
            .padding(horizontal = emDp(.3f)),
    )
}

@Composable
private fun SignalBars() {
    Row(Modifier.height(9.dp), horizontalArrangement = Arrangement.spacedBy(1.dp), verticalAlignment = Alignment.Bottom) {
        listOf(.3f, .55f, .8f, 1f).forEachIndexed { i, h ->
            Swatch(Mute.copy(alpha = if (i == 3) 0.3f else 1f), Modifier.width(2.dp).fillMaxHeight(h))
        }
    }
}

@Composable
private fun BatteryGauge(level: Int, label: androidx.compose.ui.text.TextStyle) {
    val low = level <= GameTuning.LOW_BATTERY
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(width = 22.dp, height = 10.dp)
                .border(1.dp, Mute)
                .padding(1.dp),
        ) {
            Swatch(
                if (low) AlertTone.Edge else Phosphor,
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(maxOf(4, level) / 100f),
            )
        }
        Swatch(Mute, Modifier.size(width = 2.dp, height = 5.dp))
        Spacer(Modifier.width(emDp(.35f)))
        BasicText(stringResource(R.string.battery_level, level), style = label.copy(color = if (low) AlertTone.Text else Mute))
    }
}

// ═════════════════════════════════════════════════════════════════════════════
//  독백 바: [0419 // 내면 기록] + 오실로스코프 + 타자기 독백
// ═════════════════════════════════════════════════════════════════════════════

@Composable
fun MonologueBar(
    text: String,
    talking: Boolean,
    more: Boolean,
    glitch: Int,
    exposed: Boolean,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val type = StolenTheme.type
    val scroll = rememberScrollState()
    LaunchedEffect(text) { scroll.scrollTo(scroll.maxValue) }
    Box(
        modifier
            .fillMaxWidth()
            .heightIn(min = emDp(4.2f), max = emDp(6.6f))
            .background(ShellTone.MonoBar)
            .edge(ShellTone.MonoRule, 1.dp, Side.TOP)
            .tap(onClick = onTap),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = emDp(.7f), end = emDp(.85f), top = emDp(.5f), bottom = emDp(.5f)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                Modifier
                    .width(emDp(5.4f))
                    .clip(StolenShapes.TagCut)
                    .background(ShellTone.MonoTag)
                    .border(1.dp, LineGreen, StolenShapes.TagCut)
                    .padding(horizontal = emDp(.4f), vertical = emDp(.28f)),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                BasicText(stringResource(R.string.mono_tag), style = type.keypad.copy(fontSize = ems(.92f), lineHeight = ems(.92f), letterSpacing = 0.1.em, color = Gold))
                BasicText(stringResource(R.string.mono_caption), style = type.ui.copy(fontSize = ems(.56f), lineHeight = ems(.66f), letterSpacing = 0.04.em, color = Mute), maxLines = 1)
                Oscilloscope(talking, glitch, exposed, Modifier.fillMaxWidth().height(emDp(1.05f)))
            }
            Spacer(Modifier.width(emDp(.75f)))
            BasicText(
                text = text,
                style = type.monologue.copy(fontSize = ems(.86f), lineHeight = ems(.86f * 1.55f), color = ShellTone.MonoText),
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scroll),
            )
        }
        if (more) {
            BlinkText(
                text = stringResource(R.string.mono_more),
                style = type.ui.copy(fontSize = ems(.58f), color = Gold),
                periodMs = 1000,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = emDp(.75f), bottom = emDp(.2f)),
            )
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
//  하단 시스템 바: 뒤로 · 홈 · [≡ 단서함] · 사고정리
// ═════════════════════════════════════════════════════════════════════════════

@Composable
fun SystemNavBar(
    chipCount: Int,
    drawerOpen: Boolean,
    dot: Boolean,
    enabled: Boolean,
    onBack: () -> Unit,
    onHome: () -> Unit,
    onDrawer: () -> Unit,
    onHint: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .fillMaxWidth()
            .height(NavBarHeight)
            .background(Void)
            .edge(ShellTone.NavRule, 1.dp, Side.TOP),
    ) {
        NavButton(stringResource(R.string.nav_back_glyph), stringResource(R.string.nav_back), enabled, false, onBack)
        NavButton(stringResource(R.string.nav_home_glyph), stringResource(R.string.nav_home), enabled, false, onHome)
        NavButton(
            glyph = stringResource(R.string.nav_drawer_glyph),
            label = stringResource(R.string.nav_drawer),
            enabled = enabled,
            on = drawerOpen,
            onClick = onDrawer,
            count = stringResource(R.string.chip_count, chipCount),
            dot = dot,
        )
        NavButton(stringResource(R.string.nav_hint_glyph), stringResource(R.string.nav_hint), enabled, false, onHint)
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.NavButton(
    glyph: String,
    label: String,
    enabled: Boolean,
    on: Boolean,
    onClick: () -> Unit,
    count: String? = null,
    dot: Boolean = false,
) {
    val type = StolenTheme.type
    Box(
        Modifier
            .weight(1f)
            .fillMaxHeight()
            .pressSurface(Void, ShellTone.NavPressed, enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BasicText(glyph, style = type.keypad.copy(fontSize = ems(.92f), color = if (on) Gold else Mute))
            Spacer(Modifier.width(emDp(.3f)))
            BasicText(label, style = type.ui.copy(fontSize = ems(.74f), letterSpacing = 0.03.em, color = if (on) Paper else ShellTone.NavIdle))
            if (count != null) {
                Spacer(Modifier.width(emDp(.3f)))
                BasicText(count, style = type.keypad.copy(fontSize = ems(.74f), color = Gold))
            }
        }
        if (dot) {
            // 둥근 점 대신 6dp 정사각 표식 + 2dp 바탕색 테두리
            Swatch(
                ShellTone.BadgeDot,
                Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-14).dp, y = 7.dp)
                    .border(2.dp, Void)
                    .padding(2.dp)
                    .size(6.dp),
            )
        }
    }
}
