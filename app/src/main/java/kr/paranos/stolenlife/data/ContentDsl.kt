package kr.paranos.stolenlife.data

/*
 * 문서 본문을 쓰기 위한 작은 빌더.
 *
 *     body {
 *         p { +"소재지 "; chip(Chip.WOLPO_EUP); +" 해안로 77" }
 *         table { row("대표자") { +"원장 "; chip(Chip.OH_BYEONGCHEOL) } }
 *     }
 *
 * 웹판의 C() · P() · M() · L() · K() · I() 헬퍼가 각각 chip · pin · redacted · link · lock · figure 에
 * 대응한다. @DslMarker 덕분에 table { } 안에서 바깥 p() 를 실수로 부르면 컴파일 오류가 난다.
 */

@DslMarker
annotation class StoryDsl

@StoryDsl
open class InlineScope {
    private val parts = mutableListOf<Inline>()

    operator fun String.unaryPlus() {
        parts += Inline.Text(this)
    }

    fun bold(text: String) {
        parts += Inline.Text(text, Emphasis.BOLD)
    }

    fun italic(text: String) {
        parts += Inline.Text(text, Emphasis.ITALIC)
    }

    fun chip(chip: Chip, label: String? = null) {
        parts += Inline.ChipRef(chip, label)
    }

    fun pin(pin: PinId, text: String) {
        parts += Inline.PinRef(pin, text)
    }

    fun redacted(text: String) {
        parts += Inline.Redacted(listOf(Inline.Text(text)))
    }

    fun redacted(block: InlineScope.() -> Unit) {
        parts += Inline.Redacted(spans(block))
    }

    fun stamp(source: ClockStamp, fallback: String) {
        parts += Inline.TimeStamp(source, fallback)
    }

    fun br() {
        parts += Inline.LineBreak
    }

    internal fun inlines(): List<Inline> = parts.toList()
}

fun spans(block: InlineScope.() -> Unit): List<Inline> = InlineScope().apply(block).inlines()

@StoryDsl
class TableScope {
    private val rows = mutableListOf<TableRow>()

    fun row(label: String, value: String) {
        rows += TableRow(label, listOf(Inline.Text(value)))
    }

    fun row(label: String, value: InlineScope.() -> Unit) {
        rows += TableRow(label, spans(value))
    }

    internal fun rows(): List<TableRow> = rows.toList()
}

@StoryDsl
class BulletScope {
    private val items = mutableListOf<List<Inline>>()

    fun item(text: String) {
        items += listOf(Inline.Text(text))
    }

    fun item(block: InlineScope.() -> Unit) {
        items += spans(block)
    }

    internal fun items(): List<List<Inline>> = items.toList()
}

/** 게시물: 본문 글줄에 사진 · 링크 · 댓글이 붙는다. */
@StoryDsl
class PostScope : InlineScope() {
    private val pictures = mutableListOf<Picture>()
    private val comments = mutableListOf<Comment>()
    private var link: Block.DocLink? = null

    fun picture(picture: Picture) {
        pictures += picture
    }

    fun link(doc: DocId, label: String) {
        link = Block.DocLink(doc, label)
    }

    fun comment(author: String, text: String) {
        comments += Comment(author, text)
    }

    internal fun toPost(stamp: String, memory: String?, reaction: Reaction?) = Block.Post(
        stamp = stamp,
        content = inlines(),
        pictures = pictures.toList(),
        link = link,
        comments = comments.toList(),
        memory = memory,
        reaction = reaction,
    )
}

@StoryDsl
class BodyScope {
    private val blocks = mutableListOf<Block>()

    fun p(text: String) {
        blocks += Block.Paragraph(listOf(Inline.Text(text)))
    }

    fun p(align: Align = Align.START, ruledAbove: Boolean = false, block: InlineScope.() -> Unit) {
        blocks += Block.Paragraph(spans(block), align, ruledAbove)
    }

    fun table(block: TableScope.() -> Unit) {
        blocks += Block.Table(TableScope().apply(block).rows())
    }

    fun bullets(block: BulletScope.() -> Unit) {
        blocks += Block.Bullets(BulletScope().apply(block).items())
    }

    fun figure(picture: Picture, caption: String? = null) {
        blocks += Block.Figure(picture, caption)
    }

    fun meta(text: String, spacedAbove: Boolean = false) {
        blocks += Block.Meta(listOf(Inline.Text(text)), spacedAbove)
    }

    fun meta(spacedAbove: Boolean = false, block: InlineScope.() -> Unit) {
        blocks += Block.Meta(spans(block), spacedAbove)
    }

    fun guestbook(heading: String, block: InlineScope.() -> Unit) {
        blocks += Block.Guestbook(heading, spans(block))
    }

    fun link(doc: DocId, label: String) {
        blocks += Block.DocLink(doc, label)
    }

    fun lock(lock: LockId, label: String) {
        blocks += Block.LockGate(lock, label)
    }

    fun note(text: String) {
        blocks += Block.Note(text)
    }

    fun gallery(vararg pictures: Picture) {
        blocks += Block.Gallery(pictures.toList())
    }

    fun post(
        stamp: String,
        reaction: Reaction? = null,
        memory: String? = null,
        block: PostScope.() -> Unit,
    ) {
        blocks += PostScope().apply(block).toPost(stamp, memory, reaction)
    }

    internal fun blocks(): List<Block> = blocks.toList()
}

fun body(block: BodyScope.() -> Unit): List<Block> = BodyScope().apply(block).blocks()

/** 고정 독백 */
fun lines(vararg text: String): Lines = Branch.Fixed(text.toList())

/** 상태에 따라 갈리는 독백 */
fun linesWhen(cond: Cond, then: List<String>, otherwise: List<String>): Lines = Branch.If(cond, then, otherwise)

/** WATSON 평문 메시지 */
fun watson(text: String) = MessageSpec(text)

/** 칩이 박힌 WATSON 메시지. [text] 는 토스트 미리보기에 쓰이는 평문이다. */
fun watson(text: String, rich: InlineScope.() -> Unit) = MessageSpec(text, rich = spans(rich))
