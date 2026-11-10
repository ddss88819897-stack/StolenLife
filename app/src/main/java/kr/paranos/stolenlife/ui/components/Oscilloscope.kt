package kr.paranos.stolenlife.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kr.paranos.stolenlife.ui.theme.ScopeTone
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.sin
import kotlin.random.Random

/**
 * 독백 태그 `0419 // 내면 기록` 옆의 녹색 오실로스코프. 웹판 scopeLoop() 와 같은 식이다.
 *
 *   y = sin(p·26 + A)·0.6 + sin(p·61 − B)·0.4,  포락선 sin(p·π)
 *
 * 말하는 중(글자가 찍히는 중)에는 진폭 0.85 에 잡음이 섞이고, 조용할 때는 0.07 로 숨만 쉰다.
 * 글리치 단계마다 0.05 씩 더 떨린다. Proxy 1998 이 꺼지면 선이 붉게 바뀐다.
 */
@Composable
fun Oscilloscope(talking: Boolean, glitch: Int, exposed: Boolean, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "scope")
    // 웹판 t·0.011 과 t·0.019 (t 는 ms) 를 각각 한 바퀴 주기로 돌린다.
    val phaseA by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(PERIOD_A_MS, easing = LinearEasing)),
        label = "phaseA",
    )
    val phaseB by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(PERIOD_B_MS, easing = LinearEasing)),
        label = "phaseB",
    )
    val amplitude = (if (talking) 0.85f else 0.07f) + glitch * 0.05f
    val line = if (exposed) ScopeTone.Exposed else ScopeTone.Calm

    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val grid = 1.dp.toPx()
        for (i in 1 until 4) {
            val x = w * i / 4
            drawLine(ScopeTone.Grid, Offset(x, 0f), Offset(x, h), grid)
        }
        drawLine(ScopeTone.Grid, Offset(0f, h / 2), Offset(w, h / 2), grid)

        val path = Path()
        val step = max(1f, 1.dp.toPx())
        var x = 0f
        while (x <= w) {
            val p = x / w
            val envelope = sin(p * PI).toFloat()
            val wave = sin(p * 26 + phaseA) * 0.6f + sin(p * 61 - phaseB) * 0.4f
            val jitter = if (talking) (Random.nextFloat() - 0.5f) * h * 0.2f * envelope else 0f
            val y = h / 2 + wave * amplitude * h * 0.45f * envelope + jitter
            if (x == 0f) path.moveTo(x, y) else path.lineTo(x, y)
            x += step
        }
        // 형광 잔광: 굵고 옅은 선 위에 가는 선 한 번 더 (블러 없이)
        drawPath(path, line.copy(alpha = 0.22f), style = Stroke(width = 3.dp.toPx()))
        drawPath(path, line, style = Stroke(width = 0.9.dp.toPx()))
    }
}

/** 2π / 0.011 ≈ 571ms, 2π / 0.019 ≈ 331ms */
private const val PERIOD_A_MS = 571
private const val PERIOD_B_MS = 331
