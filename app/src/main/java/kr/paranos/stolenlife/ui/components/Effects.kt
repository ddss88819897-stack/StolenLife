package kr.paranos.stolenlife.ui.components

import android.graphics.ColorMatrixColorFilter
import android.graphics.RenderEffect
import android.os.Build
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/*
 * 화면 효과. 웹판 #crt · .g1~.g3 · .burst · .flash · .shake.
 * 원칙: 평소에는 조용하다. 강한 효과(반전 · 번쩍임 · 흔들림)는 교차검증 성공과 엔딩에만.
 */

/** CRT 주사선: 3px 마다 1px. 글리치 단계가 오르면 진해진다. 터치를 가로채지 않는다. */
@Composable
fun CrtScanlines(glitch: Int, modifier: Modifier = Modifier) {
    val alpha = when (glitch) {
        0, 1 -> 0.13f
        2 -> 0.18f
        else -> 0.22f
    }
    Canvas(modifier.fillMaxSize()) {
        val pitch = 3f
        var y = 0f
        while (y < size.height) {
            drawRect(Color.Black.copy(alpha = alpha), Offset(0f, y), Size(size.width, 1f))
            y += pitch
        }
    }
}

/**
 * 약한 색수차(text-shadow 의 붉은/청록 그림자). 글리치 1~3 단계에서 0.4 / 0.7 / 1.0px 씩 어긋난다.
 * RenderEffect 가 있는 Android 12 이상에서만 켠다. 그 아래에서는 효과 없이 그린다.
 */
fun Modifier.chromaticAberration(level: Int): Modifier {
    if (level <= 0 || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return this
    return this.graphicsLayer {
        val shift = when (level) {
            1 -> 0.4f
            2 -> 0.7f
            else -> 1f
        } * density
        val strength = when (level) {
            1 -> 0.22f
            2 -> 0.32f
            else -> 0.42f
        }
        renderEffect = chroma(shift, strength).asComposeRenderEffect()
    }
}

private fun chroma(shift: Float, strength: Float): RenderEffect {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) error("API 31+")
    fun only(r: Float, g: Float, b: Float) = ColorMatrixColorFilter(
        floatArrayOf(
            r * strength, 0f, 0f, 0f, 0f,
            0f, g * strength, 0f, 0f, 0f,
            0f, 0f, b * strength, 0f, 0f,
            0f, 0f, 0f, 1f, 0f,
        ),
    )
    val red = RenderEffect.createColorFilterEffect(only(1f, 0f, 0f), RenderEffect.createOffsetEffect(shift, 0f))
    val teal = RenderEffect.createColorFilterEffect(only(0f, 1f, 1f), RenderEffect.createOffsetEffect(-shift, 0f))
    val ghosts = RenderEffect.createBlendModeEffect(red, teal, android.graphics.BlendMode.PLUS)
    return RenderEffect.createBlendModeEffect(
        RenderEffect.createOffsetEffect(0f, 0f),
        ghosts,
        android.graphics.BlendMode.SCREEN,
    )
}

/**
 * 화면 찢김(burst): 0.3초 3단계. 가운데 띠가 옆으로 밀리고, 다음 단계에 아래 띠가 반대로 밀리며 밝아진다.
 * [seq] 가 바뀔 때마다 한 번 재생한다.
 */
@Composable
fun Modifier.screenBurst(seq: Int): Modifier {
    var step by remember { mutableIntStateOf(0) }
    LaunchedEffect(seq) {
        if (seq == 0) return@LaunchedEffect
        step = 1
        delay(100)
        step = 2
        delay(100)
        step = 0
    }
    val shift = with(LocalDensity.current) { 3.dp.toPx() }
    return this.drawWithContent {
        drawContent()
        when (step) {
            1 -> clipRect(top = size.height * .12f, bottom = size.height * .62f) {
                translate(left = -shift) { this@drawWithContent.drawContent() }
                drawRect(Color.White.copy(alpha = 0.06f))
            }

            2 -> clipRect(top = size.height * .52f, bottom = size.height * .94f) {
                translate(left = shift) { this@drawWithContent.drawContent() }
                drawRect(Color.White.copy(alpha = 0.14f))
            }
        }
    }
}

/** 교차검증 성립 순간의 번쩍임 (0.45초, 두 단계 중 뒤쪽 절반만 밝다) */
@Composable
fun Modifier.screenFlash(seq: Int): Modifier {
    var on by remember { mutableStateOf(false) }
    LaunchedEffect(seq) {
        if (seq == 0) return@LaunchedEffect
        delay(225)
        on = true
        delay(225)
        on = false
    }
    return this.drawWithContent {
        drawContent()
        if (on) drawRect(Color.White.copy(alpha = 0.22f))
    }
}

/** 경찰 진입: 폰 전체가 좌우 5dp 로 12번 흔들린다(0.12초 주기). */
@Composable
fun Modifier.screenShake(seq: Int): Modifier {
    var dx by remember { mutableStateOf(0f) }
    LaunchedEffect(seq) {
        if (seq == 0) return@LaunchedEffect
        repeat(12) {
            dx = -5f
            delay(30)
            dx = 5f
            delay(60)
            dx = 0f
            delay(30)
        }
    }
    return this.graphicsLayer { translationX = dx * density }
}
