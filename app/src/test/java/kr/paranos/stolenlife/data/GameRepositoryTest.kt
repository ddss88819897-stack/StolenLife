package kr.paranos.stolenlife.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 데이터 무결성 검사. 웹판에서 옮기다 문서 하나가 빠지거나, 핀 문장이 두 군데 박히거나,
 * 검색 결과가 없는 문서를 가리키면 여기서 바로 깨진다.
 */
class GameRepositoryTest {

    private val repo = GameRepository

    @Test
    fun everyDocumentIdHasExactlyOneDocument() {
        assertEquals(DocId.entries.toSet(), repo.documents.keys)
        assertEquals(38, repo.documents.size)
        repo.documents.forEach { (id, doc) -> assertEquals(id, doc.id) }
    }

    @Test
    fun everyProfileIdHasExactlyOneProfile() {
        assertEquals(ProfileId.entries.toSet(), repo.profiles.keys)
        assertEquals(11, repo.profiles.size)
        repo.profiles.values.forEach { assertTrue(it.app == AppId.FACEBOOK || it.app == AppId.INSTA) }
    }

    @Test
    fun everyPinAppearsInExactlyOneDocument() {
        val owners = mutableMapOf<PinId, MutableList<DocId>>()
        repo.documents.values.forEach { doc ->
            doc.body.allInlines().filterIsInstance<Inline.PinRef>().forEach { owners.getOrPut(it.pin) { mutableListOf() } += doc.id }
        }
        assertEquals(PinId.entries.toSet(), owners.keys)
        owners.forEach { (pin, docs) -> assertEquals("$pin 이 여러 문서에 있음: $docs", 1, docs.size) }
    }

    @Test
    fun everyCrossPairJoinsTwoDifferentDocumentsAndCoversEveryDeduction() {
        val pinHome = repo.documents.values.flatMap { doc ->
            doc.body.allInlines().filterIsInstance<Inline.PinRef>().map { it.pin to doc.id }
        }.toMap()
        repo.crossPairs.forEach { pair ->
            assertTrue("${pair.deduction} 의 두 문장이 같은 문서에 있음", pinHome[pair.a] != pinHome[pair.b])
            assertNotNull(repo.pairFor(pair.b, pair.a))
        }
        assertEquals(Deduction.entries.toSet(), repo.crossPairs.map { it.deduction }.toSet())
        assertEquals(PinId.entries.toSet(), repo.crossPairs.flatMap { listOf(it.a, it.b) }.toSet())
    }

    @Test
    fun everySearchHitPointsAtExistingContent() {
        repo.search.forEach { (app, table) ->
            assertTrue("$app 은 검색 앱이 아님", app.searchable)
            table.values.flatMap { it.outcomes }.flatten().forEach { hit ->
                when (hit) {
                    is SearchHit.Doc -> assertTrue(hit.id in repo.documents)
                    is SearchHit.Person -> {
                        val profile = repo.profiles.getValue(hit.id)
                        assertEquals("${hit.id} 가 다른 앱 검색에 걸림", app, profile.app)
                    }
                }
            }
        }
    }

    @Test
    fun everyChipCanBeObtained() {
        val referenced = buildSet {
            repo.documents.values.forEach { d -> d.body.allInlines().filterIsInstance<Inline.ChipRef>().forEach { add(it.chip) } }
            repo.profiles.values.forEach { p -> p.body.allInlines().filterIsInstance<Inline.ChipRef>().forEach { add(it.chip) } }
            repo.beats.values.flatMap { it.watson }.mapNotNull { it.rich }.flatten()
                .filterIsInstance<Inline.ChipRef>().forEach { add(it.chip) }
            repo.proxyCollapse.watson.mapNotNull { it.rich }.flatten()
                .filterIsInstance<Inline.ChipRef>().forEach { add(it.chip) }
            repo.beats.values.mapNotNull { it.grantChip }.forEach(::add)
            repo.chapters.values.mapNotNull { it.grantChip }.forEach(::add)
        }
        assertEquals(Chip.entries.toSet(), referenced)
    }

    @Test
    fun everyLockGateInContentHasASpecAndEveryLockIsReachable() {
        assertEquals(LockId.entries.toSet(), repo.locks.keys)
        val gates = (repo.documents.values.flatMap { it.body } + repo.profiles.values.flatMap { it.body })
            .filterIsInstance<Block.LockGate>()
            .map { it.lock }
            .toSet()
        // 금고95 는 본문이 아니라 금고 앱 화면에서 직접 연다.
        assertEquals(LockId.entries.toSet() - LockId.VAULT, gates)
    }

    @Test
    fun docLinksPointAtExistingDocuments() {
        val links = (repo.documents.values.flatMap { it.body } + repo.profiles.values.flatMap { it.body })
            .flatMap { block ->
                when (block) {
                    is Block.DocLink -> listOf(block.doc)
                    is Block.Post -> listOfNotNull(block.link?.doc)
                    else -> emptyList()
                }
            }
        assertTrue(links.isNotEmpty())
        links.forEach { assertTrue(it in repo.documents) }
    }

    @Test
    fun everyReactionHasLinesAndEveryParanPostIsOnABoard() {
        assertEquals(Reaction.entries.toSet(), repo.reactions.keys)
        val boarded = repo.paranBoards.values.flatten().toSet()
        val paranStyled = repo.documents.values.filter { it.style == DocStyle.PARAN }.map { it.id }.toSet()
        assertEquals(paranStyled, boarded)
    }

    @Test
    fun hintsAlwaysAnswerAndEndWithAnUnconditionalLine() {
        (1..3).forEach { chapter ->
            val last = repo.hints.last { it.chapter == chapter }
            assertEquals("챕터 $chapter 마지막 힌트에는 조건이 없어야 함", null, last.doneWhen)
        }
        assertEquals(
            "목덜미 라벨, '해송 0419'. 웹을 열고 아래 칩을 눌러 검색하자.",
            repo.hintFor(GameState(started = true)),
        )
        assertEquals(
            "@lighthouse_1998. InstaPixel에서 검색. 마지막 문.",
            repo.hintFor(repo.demoState(DemoJump.BEFORE_ENDING)),
        )
    }

    @Test
    fun conditionalContentFollowsState() {
        val fresh = GameState()
        val exposed = fresh.copy(proxyOff = true, chapter = 3)
        val settlement = repo.document(DocId.SETTLEMENT).monologue
        assertEquals("28억 8천만 원. 날 가두는 데 쓴 돈.", fresh.resolve(settlement).first())
        assertEquals("실명: 강민우 본인.", exposed.resolve(settlement).first())

        assertEquals(1, repo.results(AppId.WEB, Chip.PARK_TAESEONG, fresh).size)
        assertEquals(2, repo.results(AppId.WEB, Chip.PARK_TAESEONG, exposed).size)
        assertTrue(repo.results(AppId.INSTA, Chip.LIGHTHOUSE_KEEPER, fresh).isEmpty())
        assertEquals(
            listOf(SearchHit.Person(ProfileId.IP_LH)),
            repo.results(AppId.INSTA, Chip.LIGHTHOUSE_KEEPER, exposed),
        )
        assertTrue(repo.results(AppId.FACEBOOK, Chip.MW419, fresh).isEmpty())
    }

    @Test
    fun clockStartsAt2312AndWrapsPastMidnight() {
        assertEquals("23:12", GameState().clock)
        assertEquals("23:59", GameState(minutes = 47).clock)
        assertEquals("00:30", GameState(minutes = 78).clock)
    }

    @Test
    fun demoJumpsLandWhereTheWebBuildDid() {
        val ch2 = repo.demoState(DemoJump.CHAPTER_2)
        assertEquals(1, ch2.chapter)
        assertEquals(6, ch2.chips.size)

        val ch3 = repo.demoState(DemoJump.CHAPTER_3)
        assertEquals(2, ch3.chapter)
        assertEquals(16, ch3.chips.size)
        assertTrue(ch3.chapter3Scheduled)

        val end = repo.demoState(DemoJump.BEFORE_ENDING)
        assertEquals(3, end.chapter)
        assertTrue(end.proxyOff && end.holds(Chip.LIGHTHOUSE_1998))
        assertEquals(4, end.log.size)
        assertEquals("00:30", end.log.first().time)
    }

    @Test
    fun policeDumpShowsTotalAndOnlyLastNine() {
        val log = (1..12).map { LogEntry("00:0${it % 10}", LogKind.SEARCH, "q$it") }
        val dump = repo.policeLogDump(log)
        assertTrue(dump.startsWith("[Proxy 1998 → 경찰청 전송 로그 12건]"))
        assertEquals(10, dump.lines().size)
        assertTrue(dump.endsWith("검색: q12"))
    }

    @Test
    fun endingFloodCarriesExactlyOnePoliceLogSlot() {
        assertEquals(1, repo.ending.flood.count { it.sender == Sender.POLICE_LOG })
        assertEquals(2, repo.ending.flood.count { it.sender == Sender.ALERT })
        assertEquals(17, repo.ending.flood.size)
    }
}

/** 본문 트리의 인라인 조각을 마스킹 · 게시물 안쪽까지 전부 펼친다. */
internal fun List<Block>.allInlines(): List<Inline> = flatMap { block ->
    when (block) {
        is Block.Paragraph -> block.content
        is Block.Table -> block.rows.flatMap { it.value }
        is Block.Bullets -> block.items.flatten()
        is Block.Meta -> block.content
        is Block.Guestbook -> block.content
        is Block.Post -> block.content
        is Block.Figure, is Block.DocLink, is Block.LockGate, is Block.Note, is Block.Gallery -> emptyList()
    }
}.flatMap { it.flattenRedactions() }

private fun Inline.flattenRedactions(): List<Inline> =
    if (this is Inline.Redacted) listOf(this) + content.flatMap { it.flattenRedactions() } else listOf(this)
