package kr.paranos.stolenlife.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.res.stringResource
import kr.paranos.stolenlife.R
import kr.paranos.stolenlife.data.Align
import kr.paranos.stolenlife.data.Block
import kr.paranos.stolenlife.data.DocStyle
import kr.paranos.stolenlife.data.Document
import kr.paranos.stolenlife.data.Inline
import kr.paranos.stolenlife.data.Profile
import kr.paranos.stolenlife.ui.theme.AppTone
import kr.paranos.stolenlife.ui.theme.Gold
import kr.paranos.stolenlife.ui.theme.Ink
import kr.paranos.stolenlife.ui.theme.Ink2
import kr.paranos.stolenlife.ui.theme.LineGreen
import kr.paranos.stolenlife.ui.theme.Mute
import kr.paranos.stolenlife.ui.theme.Paper
import kr.paranos.stolenlife.ui.theme.PaperTone
import kr.paranos.stolenlife.ui.theme.Paran98
import kr.paranos.stolenlife.ui.theme.ParanTone
import kr.paranos.stolenlife.ui.theme.Phosphor
import kr.paranos.stolenlife.ui.theme.StolenTheme
import kr.paranos.stolenlife.ui.theme.VaultTone
import kr.paranos.stolenlife.ui.theme.Amber
import kr.paranos.stolenlife.ui.theme.StolenShapes
import kr.paranos.stolenlife.viewmodel.GameIntents

/*
 * 종이 문서와 SNS 본문을 그린다. 웹판 docHTML() · profHTML() · .doc.{style} CSS.
 *
 * 문서는 Block 목록을 위에서부터 쌓는다. 종이 질감(점묘 · 괘선 · 스캔라인)은 작은 타일 비트맵을
 * 반복하는 ShaderBrush 로 깔아서, 긴 문서도 한 번의 drawRect 로 끝난다.
 */

// ═════════════════════════════════════════════════════════════════════════════
//  블록 서식
// ═════════════════════════════════════════════════════════════════════════════

/** 블록을 그릴 때 필요한 글꼴 · 색 묶음. 종이 문서와 어두운 SNS 화면이 서로 다른 값을 쓴다. */
@Immutable
class BlockStyle(
    val body: TextStyle,
    val meta: TextStyle,
    val ink: Color,
    val sub: Color,
    val rule: Color,
    val tableRule: Color,
    val tableLabel: Color,
    val palette: InkPalette,
    val link: Color,
    val photoBorder: Color,
)

private fun inkActions(intents: GameIntents, source: String) = object : InkActions {
    override fun onChip(chip: kr.paranos.stolenlife.data.Chip) = intents.tapChip(chip)
    override fun onPin(pin: Inline.PinRef) = intents.tapPin(pin, source)
    override fun onRedaction() = intents.tapRedaction()
}

/**
 * 본문 블록 목록. [source] 는 핀 문장의 출처로 쓰인다(문서 제목 · 프로필 이름).
 */
@Composable
fun BlockColumn(
    blocks: List<Block>,
    style: BlockStyle,
    ink: InkState,
    source: String,
    intents: GameIntents,
    reveal: Float = 1f,
    modifier: Modifier = Modifier,
) {
    val actions = remember(intents, source) { inkActions(intents, source) }
    Column(modifier.fillMaxWidth()) {
        blocks.forEach { block -> BlockView(block, style, ink, actions, intents, reveal) }
    }
}

@Composable
private fun BlockView(
    block: Block,
    style: BlockStyle,
    ink: InkState,
    actions: InkActions,
    intents: GameIntents,
    reveal: Float,
) {
    val type = StolenTheme.type
    when (block) {
        is Block.Paragraph -> {
            val textAlign = when (block.align) {
                Align.START -> TextAlign.Start
                Align.CENTER -> TextAlign.Center
                Align.END -> TextAlign.End
            }
            RichText(
                parts = block.content,
                style = style.body.copy(textAlign = textAlign),
                ink = ink,
                palette = style.palette,
                actions = actions,
                reveal = reveal,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = emDp(.43f))
                    .then(
                        if (block.ruledAbove) {
                            Modifier
                                .dashedBorder(RULED_ABOVE, bottom = false, start = false, end = false)
                                .padding(top = emDp(.35f))
                        } else {
                            Modifier
                        },
                    ),
            )
        }

        is Block.Table -> BoxWithConstraints(
            Modifier
                .fillMaxWidth()
                .padding(vertical = emDp(.47f)),
        ) {
            // 왼쪽 칸 31%. 칸 바탕과 괘선은 행 전체 높이에 직접 그린다(IntrinsicSize 측정 없이).
            val labelWidth = maxWidth * 0.31f
            Column(Modifier.fillMaxWidth()) {
                block.rows.forEachIndexed { index, row ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .drawBehind {
                                val w = 1.dp.toPx()
                                val split = labelWidth.toPx()
                                drawRect(style.tableLabel, size = Size(split, size.height))
                                drawRect(style.tableRule, size = Size(size.width, w))
                                if (index == block.rows.lastIndex) {
                                    drawRect(style.tableRule, topLeft = Offset(0f, size.height - w), size = Size(size.width, w))
                                }
                                drawRect(style.tableRule, size = Size(w, size.height))
                                drawRect(style.tableRule, topLeft = Offset(split - w, 0f), size = Size(w, size.height))
                                drawRect(style.tableRule, topLeft = Offset(size.width - w, 0f), size = Size(w, size.height))
                            },
                    ) {
                        BasicText(
                            text = row.label,
                            style = style.meta.copy(color = style.sub, fontSize = style.body.fontSize * 0.78f, lineHeight = style.body.lineHeight * 0.92f),
                            modifier = Modifier
                                .width(labelWidth)
                                .padding(horizontal = emDp(.43f), vertical = emDp(.28f)),
                        )
                        RichText(
                            parts = row.value,
                            style = style.body.copy(fontSize = style.body.fontSize * 0.92f, lineHeight = style.body.lineHeight * 0.92f),
                            ink = ink,
                            palette = style.palette,
                            actions = actions,
                            reveal = reveal,
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = emDp(.43f), vertical = emDp(.28f)),
                        )
                    }
                }
            }
        }

        is Block.Bullets -> Column(Modifier.padding(vertical = emDp(.3f))) {
            block.items.forEach { item ->
                Row(Modifier.padding(vertical = emDp(.17f))) {
                    BasicText(BULLET, style = style.body, modifier = Modifier.width(emDp(1f)))
                    RichText(item, style.body, ink, style.palette, actions, Modifier.weight(1f), reveal)
                }
            }
        }

        is Block.Figure -> Column(Modifier.padding(vertical = emDp(.47f))) {
            FramedPicture(PixelArt.picture(block.picture), style.photoBorder)
            block.caption?.let {
                BasicText(it, style = style.meta.copy(color = style.sub), modifier = Modifier.padding(top = emDp(.2f)))
            }
        }

        is Block.Meta -> RichText(
            parts = block.content,
            style = style.meta.copy(color = style.sub),
            ink = ink,
            palette = style.palette,
            actions = actions,
            reveal = reveal,
            modifier = Modifier.padding(top = if (block.spacedAbove) emDp(.8f) else emDp(.2f), bottom = emDp(.2f)),
        )

        is Block.Guestbook -> Column(
            Modifier
                .fillMaxWidth()
                .padding(vertical = emDp(.35f))
                .dashedBorder(PaperTone.CyFrame, dash = 1.dp, gap = 2.dp)
                .padding(horizontal = emDp(.5f), vertical = emDp(.4f)),
        ) {
            BasicText(block.heading, style = style.body.copy(fontWeight = FontWeight.Bold))
            RichText(block.content, style.body, ink, style.palette, actions, reveal = reveal)
        }

        is Block.DocLink -> BasicText(
            text = block.label,
            style = type.ui.copy(
                fontSize = style.body.fontSize * 0.9f,
                color = style.link,
                textDecoration = TextDecoration.Underline,
            ),
            modifier = Modifier
                .padding(vertical = emDp(.35f))
                .tap { intents.openDocument(block.doc) },
        )

        is Block.LockGate -> LockButton(block, block.lock in ink.unlockedLocks, intents)

        is Block.Note -> TipBox(block.text, Modifier.padding(vertical = emDp(.4f)))

        is Block.Gallery -> Row(
            Modifier
                .fillMaxWidth()
                .padding(top = emDp(.2f), bottom = emDp(.75f)),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            block.pictures.forEach { picture ->
                PixelImage(
                    PixelArt.picture(picture),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f),
                )
            }
        }

        is Block.Post -> PostCard(block, style, ink, actions, intents, reveal)
    }
}

/** 잠금 버튼 `[LOCKED //] …` / `[OPEN //] …` */
@Composable
fun LockButton(block: Block.LockGate, open: Boolean, intents: GameIntents) {
    val type = StolenTheme.type
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = emDp(.55f))
            .border(1.dp, if (open) LOCK_OPEN_RULE else LOCK_RULE)
            .pressSurface(LOCK_SURFACE, LOCK_PRESSED) { intents.openLock(block.lock) }
            .padding(horizontal = emDp(.75f), vertical = emDp(.65f)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicText(
            text = stringResource(if (open) R.string.lock_open else R.string.lock_closed),
            style = type.ui.copy(fontSize = ems(.64f), letterSpacing = 0.08.em, color = if (open) LOCK_OPEN_TAG_COLOR else LOCK_TAG_COLOR),
            maxLines = 1,
        )
        Spacer(Modifier.width(emDp(.65f)))
        BasicText(block.label, style = type.ui.copy(fontSize = ems(.8f), color = if (open) Phosphor else Gold))
    }
}

/** 회색 안내 상자 (.tip) */
@Composable
fun TipBox(text: String, modifier: Modifier = Modifier) {
    BasicText(
        text = text,
        style = StolenTheme.type.ui.copy(fontSize = ems(.72f), lineHeight = ems(1.15f), color = AppTone.TipText),
        modifier = modifier
            .fillMaxWidth()
            .background(AppTone.TipSurface)
            .edge(LineGreen, 2.dp, Side.START)
            .padding(horizontal = emDp(.75f), vertical = emDp(.5f)),
    )
}

@Composable
private fun PostCard(
    post: Block.Post,
    style: BlockStyle,
    ink: InkState,
    actions: InkActions,
    intents: GameIntents,
    reveal: Float,
) {
    val type = StolenTheme.type
    val reaction = post.reaction
    Column(
        Modifier
            .fillMaxWidth()
            .padding(bottom = emDp(.6f))
            .border(1.dp, if (post.memory != null) AppTone.MemoryRule else AppTone.CardRule)
            .background(AppTone.Card)
            .then(if (reaction != null) Modifier.tap { intents.tapReaction(reaction) } else Modifier)
            .padding(horizontal = emDp(.75f), vertical = emDp(.7f)),
    ) {
        post.memory?.let {
            BasicText(
                text = it,
                style = type.ui.copy(fontSize = ems(.52f), letterSpacing = 0.14.em, color = Gold),
                modifier = Modifier
                    .fillMaxWidth()
                    .edge(AppTone.MemoryBannerRule, 1.dp, Side.BOTTOM)
                    .padding(bottom = emDp(.3f)),
            )
            Spacer(Modifier.height(emDp(.37f)))
        }
        BasicText(post.stamp, style = type.ui.copy(fontSize = ems(.54f), letterSpacing = 0.04.em, color = AppTone.SubGreen))
        Spacer(Modifier.height(emDp(.25f)))
        RichText(post.content, style.body, ink, style.palette, actions, reveal = reveal)
        post.pictures.forEach { picture ->
            Spacer(Modifier.height(emDp(.45f)))
            FramedPicture(PixelArt.picture(picture), Color.Black)
        }
        post.link?.let { link ->
            BasicText(
                text = link.label,
                style = type.ui.copy(fontSize = style.body.fontSize * 0.9f, color = Gold, textDecoration = TextDecoration.Underline),
                modifier = Modifier
                    .padding(top = emDp(.45f))
                    .tap { intents.openDocument(link.doc) },
            )
        }
        if (post.comments.isNotEmpty()) {
            Column(
                Modifier
                    .padding(top = emDp(.45f))
                    .fillMaxWidth()
                    .dashedBorder(AppTone.CommentRule, bottom = false, start = false, end = false)
                    .padding(top = emDp(.33f)),
            ) {
                post.comments.forEach { comment ->
                    val line = androidx.compose.ui.text.buildAnnotatedString {
                        pushStyle(androidx.compose.ui.text.SpanStyle(color = Paper, fontWeight = FontWeight.Medium))
                        append(comment.author)
                        pop()
                        append("  ")
                        append(comment.text)
                    }
                    BasicText(line, style = style.body.copy(fontSize = style.body.fontSize * 0.9f, color = AppTone.CommentText))
                }
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
//  종이 문서
// ═════════════════════════════════════════════════════════════════════════════

@Immutable
private data class PaperSpec(
    val ground: Color,
    val ink: Color,
    val meta: Color,
    val rule: Color,
    val scale: Float,
    val face: Face,
    val dark: Boolean,
)

private enum class Face { SERIF, MONO, PIXEL }

private fun paperSpec(style: DocStyle): PaperSpec = when (style) {
    DocStyle.GOV, DocStyle.NEWS, DocStyle.DIARY -> PaperSpec(Paper, Ink, Ink2, PaperTone.Rule, 1f, Face.SERIF, false)
    DocStyle.WIKI -> PaperSpec(PaperTone.Wiki, Ink, Ink2, PaperTone.Rule, 1f, Face.SERIF, false)
    DocStyle.BLOG -> PaperSpec(PaperTone.Blog, Ink, Ink2, PaperTone.Rule, 1f, Face.SERIF, false)
    DocStyle.LOG -> PaperSpec(Paper, Ink, Ink2, PaperTone.Rule, 0.93f, Face.MONO, false)
    DocStyle.BINDER -> PaperSpec(PaperTone.Binder, Ink, Ink2, PaperTone.Rule, 1f, Face.SERIF, false)
    DocStyle.VAULT -> PaperSpec(VaultTone.Document, Amber, VaultTone.Small, VaultTone.DocumentRule, 0.93f, Face.MONO, true)
    DocStyle.PARAN -> PaperSpec(Paran98, ParanTone.Text, ParanTone.Yellow, ParanTone.MetaRule, 1f, Face.PIXEL, true)
    DocStyle.CY -> PaperSpec(PaperTone.Cy, Ink, PaperTone.CyMeta, PaperTone.Rule, 0.93f, Face.MONO, false)
    DocStyle.SPAM -> PaperSpec(PaperTone.Spam, PaperTone.SpamText, PaperTone.SpamMeta, PaperTone.SpamRule, 0.95f, Face.MONO, true)
}

/** 프록시 해제 직후 문서를 열면 실명이 1.6초에 걸쳐 번져 나온다. */
@Composable
private fun rememberReveal(proxyOff: Boolean): Float {
    val reveal = remember { Animatable(if (proxyOff) 0f else 1f) }
    LaunchedEffect(proxyOff) {
        if (proxyOff) reveal.animateTo(1f, tween(REVEAL_MS)) else reveal.snapTo(1f)
    }
    return reveal.value
}

@Composable
fun DocumentView(doc: Document, ink: InkState, intents: GameIntents, modifier: Modifier = Modifier) {
    val type = StolenTheme.type
    val spec = paperSpec(doc.style)
    val reveal = rememberReveal(ink.proxyOff)
    val docSize = ems(.86f) * spec.scale
    val family = when (spec.face) {
        Face.SERIF -> type.docBody.fontFamily
        Face.MONO -> type.ui.fontFamily
        Face.PIXEL -> type.terminal.fontFamily
    }
    val body = TextStyle(fontFamily = family, fontSize = docSize, lineHeight = docSize * 1.72f, color = spec.ink)
    val metaStyle = type.ui.copy(fontSize = docSize * 0.66f, lineHeight = docSize * 1.0f, letterSpacing = 0.02.em, color = spec.meta)
    val blockStyle = BlockStyle(
        body = body,
        meta = metaStyle,
        ink = spec.ink,
        sub = spec.meta,
        rule = spec.rule,
        tableRule = if (spec.dark) spec.rule else PaperTone.TableRule,
        tableLabel = if (spec.dark) Color.White.copy(alpha = 0.04f) else PaperTone.TableLabel,
        palette = if (doc.style.darkGround) InkPalette.Dark else InkPalette.Light,
        link = spec.ink,
        photoBorder = if (spec.dark) Color.Black else Ink,
    )
    val density = LocalDensity.current
    val grain = remember(doc.style, density) { paperGrain(doc.style, density.density) }
    val sidePad = if (doc.style == DocStyle.LOG) emDp(2.1f) else if (doc.style == DocStyle.DIARY) emDp(2.2f) else emDp(1.05f)

    Column(modifier.fillMaxWidth()) {
        if (doc.style == DocStyle.BINDER) {
            BasicText(
                text = stringResource(R.string.doc_binder_tab),
                style = type.ui.copy(fontSize = ems(.52f), letterSpacing = 0.14.em, color = PaperTone.BinderTabText),
                modifier = Modifier
                    .padding(start = emDp(1.1f), top = emDp(.6f))
                    .clip(StolenShapes.FolderTab)
                    .background(PaperTone.Binder)
                    .padding(horizontal = emDp(1f), vertical = emDp(.25f)),
            )
        }
        Box(
            Modifier
                .fillMaxWidth()
                .hardShadow(2.dp)
                .background(spec.ground)
                .then(if (grain != null) Modifier.drawBehind { drawRect(grain) } else Modifier)
                .paperFrame(doc.style),
        ) {
            Column(
                Modifier.padding(start = sidePad, end = if (doc.style == DocStyle.LOG) emDp(2.1f) else emDp(1.05f), top = emDp(1f), bottom = emDp(1.15f)),
            ) {
                if (doc.style.stamp != null) DocHeader(doc, spec)
                BasicText(
                    text = doc.title,
                    style = type.docTitle.copy(
                        fontSize = docSize * if (doc.style == DocStyle.NEWS) 1.12f else 1.06f,
                        lineHeight = docSize * 1.5f,
                        color = spec.ink,
                        fontFamily = if (spec.face == Face.PIXEL) type.terminal.fontFamily else type.docTitle.fontFamily,
                        textAlign = if (doc.style == DocStyle.GOV) TextAlign.Center else TextAlign.Start,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = emDp(.13f), bottom = emDp(.4f)),
                )
                BasicText(
                    text = doc.source,
                    style = metaStyle,
                    modifier = Modifier
                        .fillMaxWidth()
                        .edge(spec.rule, 1.dp, Side.BOTTOM)
                        .padding(bottom = emDp(.43f)),
                )
                Spacer(Modifier.height(emDp(.6f)))
                BlockColumn(doc.body, blockStyle, ink, doc.title, intents, reveal)
            }
            doc.style.stamp?.let { Stamp(it, doc.style.darkGround, Modifier.align(Alignment.TopEnd)) }
        }
    }
}

/** 관리번호 헤더: 바코드 + SL-12345-A, 아래 2px 실선 */
@Composable
private fun DocHeader(doc: Document, spec: PaperSpec) {
    val type = StolenTheme.type
    val barColor = if (spec.dark) VaultTone.Small else Ink
    val bars = remember(doc.id) { barcode(doc.id.key) }
    Row(
        Modifier
            .fillMaxWidth()
            .padding(bottom = emDp(.65f))
            .edge(if (spec.dark) VaultTone.BoxRule else Ink, 2.dp, Side.BOTTOM)
            .padding(bottom = emDp(.45f), top = emDp(.1f)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Canvas(
            Modifier
                .width(emDp(5.7f))
                .height(emDp(1f)),
        ) {
            val total = bars.last().let { it.first + it.second + 1 }.toFloat()
            val unit = size.width / total
            bars.forEach { (x, w) -> drawRect(barColor, Offset(x * unit, 0f), Size(w * unit, size.height)) }
        }
        Spacer(Modifier.width(emDp(.6f)))
        BasicText(
            text = docNumber(doc.id.key),
            style = type.ui.copy(fontSize = ems(.5f), letterSpacing = 0.14.em, color = if (spec.dark) VaultTone.Small else Ink2),
        )
    }
}

/** 도장: -6° 기울이고 2px 테두리 + 2px 띄운 1px 외곽선. 사선으로 잉크가 빠진 자국. */
@Composable
private fun Stamp(text: String, onDark: Boolean, modifier: Modifier) {
    val color = if (onDark) PaperTone.StampOnDark else PaperTone.Stamp
    Box(
        modifier
            .padding(top = emDp(.3f), end = emDp(.7f))
            .graphicsLayer {
                rotationZ = -6f
                compositingStrategy = CompositingStrategy.Offscreen
            }
            .border(1.dp, color.copy(alpha = 0.45f))
            .padding(2.dp)
            .border(2.dp, color.copy(alpha = 0.8f))
            .drawWithContent {
                drawContent()
                // mask-image: repeating-linear-gradient(118deg, #000 0 3px, rgba(0,0,0,.6) 3px 4px)
                val step = 4.dp.toPx()
                val reach = size.width + size.height
                var x = -size.height
                while (x < reach) {
                    drawLine(
                        Color.Black.copy(alpha = 0.4f),
                        Offset(x, size.height),
                        Offset(x + size.height * 0.53f, 0f),
                        1.dp.toPx(),
                        blendMode = BlendMode.DstOut,
                    )
                    x += step
                }
            }
            .padding(horizontal = emDp(.5f), vertical = emDp(.23f)),
    ) {
        BasicText(
            text = text,
            style = StolenTheme.type.ui.copy(fontSize = ems(.5f), letterSpacing = 0.16.em, fontWeight = FontWeight.SemiBold, color = color),
            maxLines = 1,
            overflow = TextOverflow.Clip,
        )
    }
}

/** 스타일별 종이 장식(테두리 · 괘선 · 트랙터 구멍 · 스캔라인) */
private fun Modifier.paperFrame(style: DocStyle): Modifier = when (style) {
    DocStyle.GOV -> drawBehind {
        val inset = 6.dp.toPx()
        drawRect(PaperTone.Rule.copy(alpha = 0.35f), Offset(inset, inset), Size(size.width - inset * 2, size.height - inset * 2), style = Stroke(1.dp.toPx()))
    }

    DocStyle.NEWS -> drawBehind {
        val w = 1.dp.toPx()
        drawRect(Ink, size = Size(size.width, w))
        drawRect(Ink, topLeft = Offset(0f, 3.dp.toPx()), size = Size(size.width, w))
    }

    DocStyle.LOG -> drawBehind {
        val band = emDp(1.7f).toPx()
        var y = 0f
        while (y < size.height) {
            drawRect(PaperTone.LogStripe, Offset(0f, y), Size(size.width, minOf(band, size.height - y)))
            y += band * 2
        }
        val column = emDp(1.15f).toPx()
        val pitch = emDp(1.45f).toPx()
        val hole = emDp(.27f).toPx()
        val dash = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 2.dp.toPx()))
        val edge = emDp(.25f).toPx()
        listOf(edge + column / 2 to edge + column, size.width - edge - column / 2 to size.width - edge - column).forEach { (cx, lineX) ->
            var cy = emDp(.2f).toPx() + pitch / 2
            while (cy < size.height) {
                drawCircle(PaperTone.LogPunch, hole, Offset(cx, cy))
                cy += pitch
            }
            drawLine(PaperTone.Rule.copy(alpha = 0.28f), Offset(lineX, 0f), Offset(lineX, size.height), 1.dp.toPx(), pathEffect = dash)
        }
    }

    DocStyle.DIARY -> drawBehind {
        val pitch = emDp(.86f * 1.66f).toPx()
        var y = pitch
        while (y < size.height) {
            drawRect(PaperTone.DiaryLine, Offset(0f, y), Size(size.width, 1.dp.toPx()))
            y += pitch + 1.dp.toPx()
        }
        drawRect(PaperTone.DiaryMargin, Offset(emDp(1.45f).toPx(), 0f), Size(1.dp.toPx(), size.height))
    }

    DocStyle.VAULT -> border(1.dp, VaultTone.DocumentRule)
    DocStyle.CY -> border(5.dp, PaperTone.CyFrame)
    DocStyle.SPAM -> dashedBorder(PaperTone.SpamFrame)
    DocStyle.PARAN -> border(1.dp, Color.Black)
    else -> this
}

/** 종이 질감 타일. 점묘(3px 간격) 또는 1998 스캔라인(3px 간격 1px 줄). */
private fun paperGrain(style: DocStyle, density: Float): Brush? {
    val (dot, color) = when (style) {
        DocStyle.GOV, DocStyle.NEWS, DocStyle.WIKI, DocStyle.BLOG, DocStyle.DIARY, DocStyle.LOG -> true to PaperTone.Grain
        DocStyle.BINDER -> true to PaperTone.BinderGrain
        DocStyle.PARAN -> false to Color.Black.copy(alpha = 0.2f)
        else -> return null
    }
    val cell = (3 * density).toInt().coerceAtLeast(3)
    val tile = ImageBitmap(cell, cell)
    val canvas = androidx.compose.ui.graphics.Canvas(tile)
    val paint = Paint().apply { this.color = color }
    if (dot) {
        val r = density.coerceAtLeast(1f)
        canvas.drawRect(0f, 0f, r, r, paint)
    } else {
        canvas.drawRect(0f, 0f, cell.toFloat(), density.coerceAtLeast(1f), paint)
    }
    return ShaderBrush(ImageShader(tile, TileMode.Repeated, TileMode.Repeated))
}

/** 웹판 docNo(): h = h·31 + c (uint32) → SL-xxxxx-A */
fun docNumber(key: String): String {
    var h = 0L
    for (ch in key) h = (h * 31 + ch.code) and 0xFFFFFFFFL
    return "SL-${h % 90000 + 10000}-${'A' + (h % 26).toInt()}"
}

/** 웹판 barcode(): 44개 막대의 (x, 폭). LCG 가 같아서 웹과 같은 무늬가 나온다. */
fun barcode(key: String): List<Pair<Int, Int>> {
    var h = 7L
    for (ch in key) h = (h * 131 + ch.code) and 0xFFFFFFFFL
    var x = 0
    return List(44) {
        h = (h * 1103515245L + 12345L) and 0xFFFFFFFFL
        val width = 1 + ((h ushr 29) % 3).toInt()
        (x to width).also { x += width + 1 + ((h ushr 26) % 2).toInt() }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
//  SNS 프로필
// ═════════════════════════════════════════════════════════════════════════════

@Composable
fun socialBlockStyle(): BlockStyle {
    val type = StolenTheme.type
    val body = type.ui.copy(fontSize = ems(.82f), lineHeight = ems(.82f * 1.6f), color = AppTone.PostText)
    return BlockStyle(
        body = body,
        meta = type.ui.copy(fontSize = ems(.66f), color = Mute),
        ink = AppTone.PostText,
        sub = Mute,
        rule = AppTone.CardRule,
        tableRule = AppTone.CardRule,
        tableLabel = AppTone.Card,
        palette = InkPalette.Light.copy(chipHeldWash = InkTone_HeldOnDark),
        link = Gold,
        photoBorder = Color.Black,
    )
}

private val InkTone_HeldOnDark = Color.White.copy(alpha = 0.07f)

/** 프로필 머리 카드: 아바타 + 이름 + 한 줄 소개 */
@Composable
fun ProfileHeader(name: String, headline: String, avatar: ImageBitmap, modifier: Modifier = Modifier) {
    val type = StolenTheme.type
    Row(
        modifier
            .fillMaxWidth()
            .padding(bottom = emDp(.75f))
            .border(1.dp, AppTone.CardRule)
            .background(AppTone.Card)
            .padding(emDp(.75f)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PixelImage(
            avatar,
            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
            modifier = Modifier
                .size(emDp(3.7f))
                .bevel(line = Color.Black, outline = LineGreen),
        )
        Spacer(Modifier.width(emDp(.85f)))
        Column {
            BasicText(name, style = type.ui.copy(fontSize = ems(.95f), color = Paper))
            BasicText(headline, style = type.ui.copy(fontSize = ems(.68f), lineHeight = ems(1.02f), color = Mute))
        }
    }
}

@Composable
fun ProfileView(profile: Profile, ink: InkState, intents: GameIntents, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth()) {
        ProfileHeader(profile.displayName, profile.headline, PixelArt.picture(profile.avatar))
        BlockColumn(profile.body, socialBlockStyle(), ink, profile.name, intents, rememberReveal(ink.proxyOff))
    }
}

private const val REVEAL_MS = 1600
private const val BULLET = "•"
private val RULED_ABOVE = Color(0xFFCC6666)
private val LOCK_SURFACE = Color(0xFF161A19)
private val LOCK_PRESSED = Color(0xFF1F2523)
private val LOCK_RULE = Color(0xFF5A4A30)
private val LOCK_OPEN_RULE = Color(0xFF35503F)
private val LOCK_TAG_COLOR = Color(0xFFA8885A)
private val LOCK_OPEN_TAG_COLOR = Color(0xFF6F9A7C)
