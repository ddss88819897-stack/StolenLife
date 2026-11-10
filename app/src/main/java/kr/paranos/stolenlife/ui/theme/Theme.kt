package kr.paranos.stolenlife.ui.theme

import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.invalidateDraw
import kotlinx.coroutines.launch
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

/**
 * Material3 ColorScheme 에는 Paper · Rust · Phosphor 같은 슬롯이 없다.
 * 기본 슬롯은 최소한으로만 채우고, 나머지는 이 확장 토큰으로 꺼내 쓴다.
 */
@Immutable
data class StolenColors(
    val void: Color = Void,
    val slate: Color = Slate,
    val khaki: Color = Khaki,
    val paper: Color = Paper,
    val paper2: Color = Paper2,
    val ink: Color = Ink,
    val ink2: Color = Ink2,
    val rust: Color = Rust,
    val gold: Color = Gold,
    val text: Color = TextMain,
    val mute: Color = Mute,
    val phosphor: Color = Phosphor,
    val amber: Color = Amber,
    val line: Color = LineGreen,
    val lineInk: Color = LineInk,
    val titleBar: Color = TitleGreen,
    val alertBar: Color = Blood,
)

/**
 * 모서리 규칙: 직각이 기본이다. 각진 귀퉁이가 필요한 곳(독백 태그 · 바인더 탭)만 컷 코너를 쓴다.
 * Material Shapes 슬롯은 CornerBasedShape 를 요구하므로 0dp 컷 코너 = 직사각형으로 채운다.
 */
object StolenShapes {
    val Square = RectangleShape
    private val zero = CutCornerShape(0.dp)

    /** 독백 바 왼쪽 `0419 // 내면 기록` 태그: 오른쪽 위 7dp 절단 */
    val TagCut = CutCornerShape(topEnd = 7.dp)

    /** 감사 바인더 상단 인덱스 탭: 위가 좁은 사다리꼴 (웹 clip-path 8% / 92%) */
    val FolderTab = GenericShape { size, _ ->
        moveTo(size.width * 0.08f, 0f)
        lineTo(size.width * 0.92f, 0f)
        lineTo(size.width, size.height)
        lineTo(0f, size.height)
        close()
    }

    val material = Shapes(
        extraSmall = zero,
        small = zero,
        medium = zero,
        large = zero,
        extraLarge = zero,
    )
}

val LocalStolenColors = staticCompositionLocalOf { StolenColors() }
val LocalStolenType = staticCompositionLocalOf<StolenTypography> {
    error("StolenTheme { } 바깥에서 StolenTheme.type 을 읽었다.")
}

@Composable
fun StolenTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val fonts = remember(context) { loadStolenFonts(context.assets) }
    val type = remember(fonts) { stolenTypography(fonts) }
    val colors = remember { StolenColors() }

    val scheme = darkColorScheme(
        primary = Gold,
        onPrimary = Ink,
        secondary = Phosphor,
        onSecondary = Void,
        background = Slate,
        onBackground = TextMain,
        surface = ShellTone.DrawerSheet,
        onSurface = TextMain,
        surfaceVariant = Khaki,
        onSurfaceVariant = Mute,
        error = Rust,
        onError = Paper,
        outline = LineGreen,
        outlineVariant = LineInk,
        scrim = ShellTone.DrawerScrim,
    )

    CompositionLocalProvider(
        LocalStolenColors provides colors,
        LocalStolenType provides type,
    ) {
        MaterialTheme(
            colorScheme = scheme,
            typography = type.asMaterial(),
            shapes = StolenShapes.material,
        ) {
            // MaterialTheme 이 깔아 둔 리플(원형 번짐)을 평면 눌림으로 덮어쓴다.
            CompositionLocalProvider(LocalIndication provides FlatPress, content = content)
        }
    }
}

/** `StolenTheme.colors.gold` 처럼 꺼내 쓰는 진입점. MaterialTheme 과 같은 패턴. */
object StolenTheme {
    val colors: StolenColors
        @Composable @ReadOnlyComposable get() = LocalStolenColors.current

    val type: StolenTypography
        @Composable @ReadOnlyComposable get() = LocalStolenType.current
}

/**
 * 리플 대신 쓰는 눌림 표시: 누르는 동안 6% 밝은 사각 덮개 한 장. 번짐 · 원형 · 그라데이션 없음.
 */
object FlatPress : IndicationNodeFactory {
    override fun create(interactionSource: InteractionSource): DelegatableNode = FlatPressNode(interactionSource)
    override fun equals(other: Any?): Boolean = other === this
    override fun hashCode(): Int = 0x0419
}

private class FlatPressNode(private val source: InteractionSource) :
    androidx.compose.ui.Modifier.Node(), DrawModifierNode {
    private var presses = 0

    override fun onAttach() {
        coroutineScope.launch {
            source.interactions.collect { interaction ->
                when (interaction) {
                    is PressInteraction.Press -> presses++
                    is PressInteraction.Release, is PressInteraction.Cancel -> presses = (presses - 1).coerceAtLeast(0)
                }
                invalidateDraw()
            }
        }
    }

    override fun ContentDrawScope.draw() {
        drawContent()
        if (presses > 0) drawRect(Color.White.copy(alpha = 0.06f))
    }
}
