package kr.paranos.stolenlife.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/*
 * 게임 데이터 모델.
 *
 * 웹 프로토타입은 문서 본문을 `body: () => html` 함수로 들고 있었고, 그 안에서 S(상태)를 직접 읽어
 * 칩 획득 여부 · 마스킹 · 잠금 상태를 문자열로 구워 냈다. 앱에서는 이를 둘로 나눈다.
 *
 *   - 본문은 상태를 모르는 불변 트리(Block / Inline)로 둔다.
 *   - 상태에 따라 달라지는 부분(독백 분기 · 검색 결과 분기 · 힌트)은 Cond 로 선언하고,
 *     GameState.meets(cond) 하나로만 판정한다.
 *
 * 그래서 데이터는 테스트로 전수 검사할 수 있고, 화면은 상태만 바꿔 끼우면 된다.
 */

// ═════════════════════════════════════════════════════════════════════════════
//  식별자
// ═════════════════════════════════════════════════════════════════════════════

/** 단서함 분류. [code] 가 칩 앞 말머리(`[PER]`)와 서랍 탭에 그대로 찍힌다. */
enum class ChipType(val code: String) {
    PERSON("PER"),
    PLACE("LOC"),
    NUMBER("NUM"),
    ORG("ORG"),
    ACCOUNT("ACC"),
}

/**
 * 단서 칩. 웹판 CHIPTYPE · CHIPLABEL 두 테이블을 하나로 합쳤다.
 * [key] 는 검색 질의어 겸 저장 키, [label] 은 화면에 찍히는 글자다.
 */
enum class Chip(val key: String, val type: ChipType, label: String? = null) {
    HAESONG_0419("해송 0419", ChipType.NUMBER),
    DATE_2006_11_03("2006-11-03", ChipType.NUMBER, "2006.11.03"),
    DATE_1998_05_11("1998-05-11", ChipType.NUMBER, "1998.05.11"),
    OH_BYEONGCHEOL("오병철", ChipType.PERSON),
    KANG_MINWOO("강민우", ChipType.PERSON),
    SEO_HARIN("서하린", ChipType.PERSON),
    HAN_SOYUN("한소윤", ChipType.PERSON),
    PARK_TAESEONG("박태성", ChipType.PERSON),
    LIGHTHOUSE_KEEPER("등대지기", ChipType.PERSON),
    MW419("MW419", ChipType.PERSON),
    WOLPO_EUP("월포읍", ChipType.PLACE),
    WOLPO_SUSAN("월포수산", ChipType.PLACE),
    BAEKRODO("백로도", ChipType.PLACE),
    PARANNURI("파란누리", ChipType.PLACE),
    MUJEOK_FOUNDATION("무적문화재단", ChipType.ORG),
    MUJEOK_NOVEL("《무적》", ChipType.ORG),
    VAULT95("금고95", ChipType.ORG),
    GAPYEONG_CRASH("가평 국도 사고", ChipType.ORG),
    LIGHTHOUSE_1998("@lighthouse_1998", ChipType.ACCOUNT),
    ;

    val label: String = label ?: key
}

/** 단서함 상단 분류 탭 `[ALL] [PER] [LOC] [NUM] [ORG] [ACC]` */
enum class ChipFilter(private val type: ChipType?) {
    ALL(null),
    PER(ChipType.PERSON),
    LOC(ChipType.PLACE),
    NUM(ChipType.NUMBER),
    ORG(ChipType.ORG),
    ACC(ChipType.ACCOUNT),
    ;

    fun accepts(chip: Chip): Boolean = type == null || chip.type == type
}

/**
 * ParanOS 98 의 앱. 선언 순서가 곧 홈 화면 아이콘 순서다.
 * [alertChrome] 이 참이면 타이틀바를 Blood 로 칠한다(금고 · 설정).
 */
enum class AppId(
    val title: String,
    val homeLabel: String,
    val code: String,
    val alertChrome: Boolean = false,
) {
    MESSENGER("메신저", "메신저", "MSG"),
    WEB("ParanNet 검색", "검색", "NET"),
    INSTA("InstaPixel", "InstaPixel", "IPX"),
    FACEBOOK("FaceBook 95", "FaceBook95", "FB95"),
    PARAN("파란누리", "파란누리", "CUG"),
    ROOM("파란여관 203호", "203호", "R203"),
    VAULT("금고95", "금고95", "V95", alertChrome = true),
    SETTINGS("설정", "설정", "SYS", alertChrome = true),
    ;

    /** 단서함에서 칩을 눌러 바로 검색할 수 있는 앱 */
    val searchable: Boolean get() = this == WEB || this == INSTA || this == FACEBOOK
}

/** 문서 지질. [stamp] 가 있으면 우상단 도장과 바코드 관리번호 헤더가 붙는다. */
enum class DocStyle(val stamp: String? = null) {
    GOV("대외비 // CONFIDENTIAL"),
    LOG("검시필 // EXAMINED"),
    BINDER("대외비 // 특별감사"),
    VAULT("RESTRICTED // 열람 제한"),
    NEWS,
    WIKI,
    BLOG,
    PARAN,
    CY,
    DIARY,
    SPAM,
    ;

    /** 어두운 바탕 위에 찍히는 지질. 칩 '+' 표시 · 핀 밑줄 색을 밝은 쪽으로 바꾼다. */
    val darkGround: Boolean get() = this == VAULT || this == PARAN || this == SPAM
}

/**
 * 문서 식별자. [key] 는 웹판 DOCS 의 키와 같아서, 같은 해시로 같은 관리번호(SL-12345-A)와
 * 바코드가 나온다.
 */
enum class DocId {
    // Chapter 1
    NOTICE, POWER_CONTRACT, SETTLEMENT,

    // Chapter 2
    WIKI_KANG, FOUNDATION, GAPYEONG, RADIO_2005, SOYUN_NEWS, CY_SOYUN, DIARY, BODYLOG, BINDER, VAULT_SUMMARY,

    // Chapter 3
    NEWS_PARK, BURN_BLOG, MIRRORLOG,

    // 파란누리 (1998)
    PARAN_LH, PARAN_MW, PARAN_JOKE, PARAN_IMF, PARAN_NOTICE,

    // 검색 더미
    D_HOE, D_419, D_SPAM_SILVER, D_OH_NEWS, D_WOLPO_TOUR, D_WOLPOSUSAN, D_FOUND_JOB, D_DDUK, D_WRECK,
    D_BOOK_REVIEW, D_PARAN_INFO, D_HARIN_GALLERY, D_LIGHTHOUSE_JOB, D_BAEKRO_FISH, D_SCHOLAR, D_PARK_AWARD,
    D_LH_WEB,
    ;

    val key: String get() = name.lowercase()
}

enum class ProfileId {
    FB_OH, FB_KANG, FB_YOUTH, FB_HARIN, FB_HAESONG,
    IP_HARIN, IP_KANG, IP_DDUK, IP_OH, IP_BAEKRO, IP_LH,
    ;

    val key: String get() = name.lowercase()
}

/** 대조 가능한 붉은 밑줄 문장. 문서 하나에 정확히 한 번씩만 나온다(테스트로 보장). */
enum class PinId {
    CORP_NOTICE, CORP_POWER,
    DIARY_BEFORE, BODYLOG_COFFEE,
    BURN_RECORD, MIRROR_LOG,
    KANG_BIRTH, SUBJECT_NO,
}

/**
 * 교차검증으로 얻는 결론. 괄호 안은 웹판 플래그 이름.
 * - ASYLUM_RENAMED (x1)  요양원 = 냉동창고
 * - BORROWED_HABITS (x2) 내 몸에 원수의 식습관
 * - NOT_DOYUN (x3)       나는 한도윤이 아니다
 * - BIRTHDAY_CELL (x0)   수감번호 = 강민우 생일
 */
enum class Deduction(val webFlag: String) {
    ASYLUM_RENAMED("x1"),
    BORROWED_HABITS("x2"),
    NOT_DOYUN("x3"),
    BIRTHDAY_CELL("x0"),
}

enum class LockId { WEBHARD, CLOUD, VAULT, LIGHTHOUSE }

/** 게시물을 눌렀을 때 터지는 독백 키 (웹판 SAYS) */
enum class Reaction {
    FB_FISH, FB_MEMORY, FB_KANG_MEMORY, SLANG, POLICE, HARIN_2008, HAESONG_GROUP,
    IP_HARIN_SERIES, IP_HARIN_SUGAR, IP_KANG_BLACK, DDUK, MASK,
}

/**
 * 1-bit 디더링 사진. 크기와 2색 팔레트는 웹판 IMGDEF 그대로다.
 * 실제 그림은 ui 쪽 페인터가 같은 키로 그린다.
 */
enum class Picture(
    val width: Int,
    val height: Int,
    val ink: Long = PHOTO_INK,
    val ground: Long = PHOTO_GROUND,
) {
    WALL(120, 214, 0xFF1F3A2B, 0xFF070A08),
    CONTRACT(192, 144),
    FISH(192, 144),
    LIGHT(192, 192),
    COFFEE(192, 144),
    KANG(96, 96),
    KANGCUP(192, 192),
    PODIUM(192, 144),
    RADIO(144, 144, 0xFFD8BC8E, 0xFF17100A),
    CCTV(144, 144, 0xFF93B89C, 0xFF08110B),
    SCAN(192, 160),
    GROUP(192, 128),
    FOOD(192, 192),
    ISLAND(192, 192),
    HARBOR(192, 128),
}

private const val PHOTO_INK = 0xFFE3DDCF
private const val PHOTO_GROUND = 0xFF26231F

/**
 * 203호 조사 지점. 좌표는 방 그림(120×160 기준) 위의 백분율이다.
 */
enum class RoomSpot(
    val label: String,
    val leftPct: Float,
    val topPct: Float,
    val widthPct: Float,
    val heightPct: Float,
) {
    WINDOW("창문", 7.5f, 10f, 30f, 17.5f),
    VENT("환풍구", 72.5f, 5.6f, 20f, 8.8f),
    MIRROR("거울", 5.8f, 31.2f, 18.3f, 21.3f),
    DOOR("문", 75.8f, 28.8f, 21.7f, 37.5f),
    BED("침대", 1.7f, 66f, 43f, 21f),
    COFFEE("믹스커피", 49f, 56f, 14f, 10f),
    OTP("OTP 토큰", 64f, 57f, 12f, 9f),
}

/** 파란누리 게시판 */
enum class ParanBoard(val title: String) {
    WRITE("창작마당"),
    HUMOR("유머방"),
    NOTICE("공지사항"),
}

/** 설정 앱의 시연용 챕터 점프 */
enum class DemoJump { CHAPTER_2, CHAPTER_3, BEFORE_ENDING }

// ═════════════════════════════════════════════════════════════════════════════
//  본문 트리
// ═════════════════════════════════════════════════════════════════════════════

enum class Emphasis { PLAIN, BOLD, ITALIC }

enum class Align { START, CENTER, END }

/** 본문 속 시각 표기가 어느 행동 시각을 따르는지 */
enum class ClockStamp { COFFEE, MIRROR }

/**
 * 한 줄 안의 조각. 메신저 메시지에도 실려 저장되므로 직렬화 가능해야 한다.
 */
@Serializable
sealed interface Inline {

    @Serializable
    @SerialName("text")
    data class Text(val value: String, val emphasis: Emphasis = Emphasis.PLAIN) : Inline

    /** 점선 밑줄 단어. 누르면 단서함에 들어간다. 이미 쥔 칩은 실선으로 바뀐다. */
    @Serializable
    @SerialName("chip")
    data class ChipRef(val chip: Chip, val label: String? = null) : Inline {
        val shown: String get() = label ?: chip.label
    }

    /** 붉은 밑줄 문장. 누르면 [PIN] 대조 버퍼 메뉴가 뜬다. */
    @Serializable
    @SerialName("pin")
    data class PinRef(val pin: PinId, val text: String) : Inline

    /** Proxy 1998 이 지운 실명 구간. 프록시 해제 전에는 `■■■ 프록시 손상` 으로 보인다. */
    @Serializable
    @SerialName("redacted")
    data class Redacted(val content: List<Inline>) : Inline

    /** 행동 시각(커피를 탄 시각 등). 아직 행동 전이면 [fallback] 을 찍는다. */
    @Serializable
    @SerialName("stamp")
    data class TimeStamp(val source: ClockStamp, val fallback: String) : Inline

    @Serializable
    @SerialName("br")
    data object LineBreak : Inline
}

/** 핀 문장 원문이나 교차검증 카드에 넣을 평문. 마스킹 구간은 원문 그대로 꺼낸다. */
fun List<Inline>.plainText(): String = buildString {
    for (part in this@plainText) when (part) {
        is Inline.Text -> append(part.value)
        is Inline.ChipRef -> append(part.shown)
        is Inline.PinRef -> append(part.text)
        is Inline.Redacted -> append(part.content.plainText())
        is Inline.TimeStamp -> append(part.fallback)
        Inline.LineBreak -> append('\n')
    }
}

data class TableRow(val label: String, val value: List<Inline>)

data class Comment(val author: String, val text: String)

/** 문서 · 프로필 본문의 블록 단위 */
sealed interface Block {
    /** [ruledAbove]: 일기 끝 '[관찰 기록]'처럼 위에 붉은 점선을 긋는 문단 */
    data class Paragraph(
        val content: List<Inline>,
        val align: Align = Align.START,
        val ruledAbove: Boolean = false,
    ) : Block

    data class Table(val rows: List<TableRow>) : Block

    data class Bullets(val items: List<List<Inline>>) : Block

    data class Figure(val picture: Picture, val caption: String? = null) : Block

    /** 본문 속 작은 서식 줄(싸이 방문자 수, 게시판 글쓴이 줄, '┌ 댓글 2') */
    data class Meta(val content: List<Inline>, val spacedAbove: Boolean = false) : Block

    /** 싸이월드 다이어리 · 방명록 상자 */
    data class Guestbook(val heading: String, val content: List<Inline>) : Block

    /** 다른 문서로 가는 밑줄 링크 `[DOC] …` */
    data class DocLink(val doc: DocId, val label: String) : Block

    /** `[LOCKED //]` / `[OPEN //]` 잠금 버튼 */
    data class LockGate(val lock: LockId, val label: String) : Block

    /** 회색 안내 상자 */
    data class Note(val text: String) : Block

    /** InstaPixel 3열 썸네일 */
    data class Gallery(val pictures: List<Picture>) : Block

    /** SNS 게시물. [memory] 가 있으면 '[MEMORY] 10년 전 오늘' 띠를 두른다. */
    data class Post(
        val stamp: String,
        val content: List<Inline>,
        val pictures: List<Picture> = emptyList(),
        val link: DocLink? = null,
        val comments: List<Comment> = emptyList(),
        val memory: String? = null,
        val reaction: Reaction? = null,
    ) : Block
}

// ═════════════════════════════════════════════════════════════════════════════
//  상태 조건과 분기
// ═════════════════════════════════════════════════════════════════════════════

/** 데이터가 상태를 물어볼 때 쓰는 유일한 어휘. 판정은 [GameState.meets] 한 곳에서만 한다. */
sealed interface Cond {
    data object ProxyOff : Cond
    data object CoffeeDrunk : Cond
    data object BinderFound : Cond
    data object OtpPressed : Cond
    data object MirrorChecked : Cond
    data class ChapterAtLeast(val chapter: Int) : Cond
    data class Deduced(val deduction: Deduction) : Cond
    data class Read(val doc: DocId) : Cond
    data class Visited(val profile: ProfileId) : Cond
    data class Unlocked(val lock: LockId) : Cond
    data class Holding(val chip: Chip) : Cond
}

/** 상태에 따라 값이 갈리는 데이터(독백 · 검색 결과). */
sealed interface Branch<out T> {
    data class Fixed<out T>(val value: T) : Branch<T>
    data class If<out T>(val cond: Cond, val then: T, val otherwise: T) : Branch<T>

    /** 테스트용: 어느 쪽으로 갈 수 있는 값 전부 */
    val outcomes: List<T>
        get() = when (this) {
            is Fixed -> listOf(value)
            is If -> listOf(then, otherwise)
        }
}

typealias Lines = Branch<List<String>>

// ═════════════════════════════════════════════════════════════════════════════
//  콘텐츠
// ═════════════════════════════════════════════════════════════════════════════

data class Document(
    val id: DocId,
    val title: String,
    val source: String,
    val style: DocStyle,
    val body: List<Block>,
    val monologue: Lines,
    /** 검색 결과 목록에 찍히는 미리보기 */
    val snippet: String? = null,
    /** 처음 열람할 때 경찰청 전송 로그에 남는 문장. 있으면 비콘이 붉게 깜빡인다. */
    val logLine: String? = null,
)

data class Profile(
    val id: ProfileId,
    val app: AppId,
    val name: String,
    val headline: String,
    val avatar: Picture,
    val body: List<Block>,
    val monologue: Lines,
) {
    /** InstaPixel 계정은 @ 를 붙인다. 해시태그 페이지(#백로도)는 그대로 둔다. */
    val displayName: String
        get() = if (app == AppId.INSTA && !name.startsWith("#")) "@$name" else name
}

sealed interface SearchHit {
    data class Doc(val id: DocId) : SearchHit
    data class Person(val id: ProfileId) : SearchHit
}

/** 잠금을 풀었을 때 일어나는 일 */
sealed interface LockReward {
    data class OpenDocument(val doc: DocId) : LockReward
    data object VaultMasterKey : LockReward
    data object FinalTruth : LockReward
}

data class LockSpec(
    val id: LockId,
    val title: String,
    val hint: String,
    val code: String,
    /** 해제 로그 문장 */
    val logLine: String,
    val reward: LockReward,
    /** 이 조건을 만족하기 전에는 키패드 대신 [gateLines] 독백만 나온다. */
    val requires: Cond? = null,
    val gateLines: List<String> = emptyList(),
)

data class CrossPair(
    val a: PinId,
    val b: PinId,
    val deduction: Deduction,
    val conclusion: String,
) {
    fun matches(x: PinId, y: PinId): Boolean = (a == x && b == y) || (a == y && b == x)
}

// ═════════════════════════════════════════════════════════════════════════════
//  이야기 진행
// ═════════════════════════════════════════════════════════════════════════════

/** 이야기 트리거. [key] 는 웹판 story(ev) 문자열과 같다(한 번만 발동 기록에 쓰임). */
sealed interface StoryEvent {
    val key: String

    data class DocumentOpened(val doc: DocId) : StoryEvent {
        override val key: String get() = "open:${doc.key}"
    }

    data class ProfileVisited(val profile: ProfileId) : StoryEvent {
        override val key: String get() = "prof:${profile.key}"
    }

    data class Deduced(val deduction: Deduction) : StoryEvent {
        override val key: String get() = "cross:${deduction.webFlag}"
    }

    data object CoffeeDrunk : StoryEvent {
        override val key: String get() = "coffee"
    }

    data object BinderFound : StoryEvent {
        override val key: String get() = "binder"
    }

    data object VaultKeyAccepted : StoryEvent {
        override val key: String get() = "vaultkey"
    }
}

/**
 * 트리거 하나가 일으키는 일. 적용 순서는 웹판과 같다:
 * 칩 지급 → 글리치 상승 → WATSON 메시지 → 독백 → 3장 진입 검사.
 * 모든 비트는 한 번만 발동한다.
 */
data class StoryBeat(
    val watson: List<MessageSpec> = emptyList(),
    val monologue: List<String> = emptyList(),
    val grantChip: Chip? = null,
    val glitchAtLeast: Int? = null,
    val checksChapter3: Boolean = false,
)

enum class Sender { WATSON, SYSTEM, ALERT, POLICE_LOG }

/** 메시지 버튼이 내리는 명령 */
@Serializable
enum class Command { GO_CHAPTER_2 }

@Serializable
data class MessageAction(val command: Command, val label: String)

/** 대본 속 메시지 한 통. [rich] 가 있으면 [text] 대신 칩이 박힌 문장으로 그린다. */
data class MessageSpec(
    val text: String,
    val sender: Sender = Sender.WATSON,
    val rich: List<Inline>? = null,
    val action: MessageAction? = null,
)

data class ChapterCard(val number: Int, val title: String, val place: String)

/** 챕터에 들어설 때 화면이 어디로 가는지 */
sealed interface Landing {
    data object Home : Landing
    data class App(val app: AppId) : Landing
    data class Doc(val app: AppId, val doc: DocId) : Landing
}

data class ChapterScript(
    val card: ChapterCard,
    val battery: Int,
    val minutesAdvance: Int,
    val landing: Landing,
    val monologue: List<String>,
    /** true 면 진행 중인 독백 뒤에 잇는다(1·2장). false 면 끊고 바로 말한다(3장). */
    val monologueQueued: Boolean,
    val watson: List<MessageSpec>,
    /** 조용히 쥐여 주는 칩(토스트 없음) */
    val grantChip: Chip? = null,
    /** 진입하면서 '읽음' 처리할 문서 */
    val markRead: DocId? = null,
    /** 진입 시 경찰청 로그 */
    val receiveLog: String? = null,
)

data class ProxyCollapseScript(
    /** 프록시가 지우던 문서. 이미 읽은 것만 '복원된 문서'로 표시한다. */
    val restoredDocs: List<DocId>,
    val monologue: List<String>,
    val watson: List<MessageSpec>,
)

data class EndingScript(
    /** @lighthouse_1998 을 연 직후 자동 재생 독백 */
    val revelation: List<String>,
    /** 등대지기 메신저 폭주. POLICE_LOG 는 실행 시점의 로그로 채운다. */
    val flood: List<MessageSpec>,
    val lighthouseHeadline: String,
    val lighthouseFigures: List<Pair<Picture, String>>,
    val sameFace: String,
    val raidBang: String,
    val raidShout: String,
    val raidAftermath: String,
    val lastWords: List<String>,
)

data class RoomScript(
    val coffeeAgain: List<String>,
    val otpAlreadyPressed: List<String>,
    val otpIdle: List<String>,
    val otpCode: String,
    val mirrorNotYet: List<String>,
    val doorCalm: List<String>,
    val doorSiege: List<String>,
    val windowCalm: List<String>,
    val windowSiege: List<String>,
    val bed: List<String>,
    /** 조사 목록 아래 '기록' 버튼: 읽은 순서와 무관하게 이 순서로 놓는다. */
    val records: List<Pair<DocId, String>>,
)

/** 사고정리(?) 버튼. 현재 챕터 규칙을 위에서부터 보고 [doneWhen] 이 아직 거짓인 첫 줄을 말한다. */
data class HintRule(val chapter: Int, val text: String, val doneWhen: Cond? = null)

// ═════════════════════════════════════════════════════════════════════════════
//  저장 상태
// ═════════════════════════════════════════════════════════════════════════════

/** 경찰청 전송 로그 분류. FIN 화면 통계가 이 분류로 센다. */
@Serializable
enum class LogKind(val label: String) {
    SEARCH("검색"),
    READ("열람"),
    UNLOCK("해제"),
    DEDUCTION("교차검증"),
    CONTACT("접촉"),
    AUTH("인증"),
    RECEIVE("수신"),
    SYSTEM("시스템"),
}

@Serializable
data class LogEntry(val time: String, val kind: LogKind, val text: String)

@Serializable
data class ChatMessage(
    val sender: Sender,
    val text: String,
    val time: String,
    val rich: List<Inline>? = null,
    val action: MessageAction? = null,
)

/** [PIN] 대조 버퍼에 고정된 문장 */
@Serializable
data class PinnedEvidence(val pin: PinId, val text: String, val source: String)

/**
 * 진행 상황 전체. 웹판 S 객체를 타입이 있는 필드로 풀었다.
 * 화면 전용 상태(열린 앱 스택 · 서랍 · 방금 얻은 칩 반짝임)는 여기 두지 않는다.
 */
@Serializable
data class GameState(
    val chapter: Int = 1,
    /** 획득 순서 유지. 단서함은 역순(최근 것이 앞)으로 보여 준다. */
    val chips: List<Chip> = emptyList(),
    val readDocs: Set<DocId> = emptySet(),
    val visitedProfiles: Set<ProfileId> = emptySet(),
    val unlocked: Set<LockId> = emptySet(),
    val deductions: Set<Deduction> = emptySet(),
    /** 한 번 발동한 StoryEvent.key */
    val firedBeats: Set<String> = emptySet(),
    val completedCommands: Set<Command> = emptySet(),
    /** 203호 믹스커피를 탄 시각. null 이면 아직 안 마심. */
    val coffeeAt: String? = null,
    val binderFound: Boolean = false,
    /** 거울을 본 시각. 3장 전에는 거울을 볼 수 없다. */
    val mirrorAt: String? = null,
    val otpPressed: Boolean = false,
    /** 단서 없이 둘러본 지점(문 · 창문 · 침대) */
    val glancedSpots: Set<RoomSpot> = emptySet(),
    val pin: PinnedEvidence? = null,
    val log: List<LogEntry> = emptyList(),
    val messages: List<ChatMessage> = emptyList(),
    val unread: Int = 0,
    /** 0~3. 색수차 · 화면 찢김 빈도 */
    val glitch: Int = 0,
    val proxyOff: Boolean = false,
    val sound: Boolean = true,
    /** 23:12 로부터 흐른 분 */
    val minutes: Int = 0,
    val battery: Int = 31,
    val started: Boolean = false,
    val seenApps: Set<AppId> = emptySet(),
    val chapter3Scheduled: Boolean = false,
    /** WATSON 의 정체가 '등대지기'로 드러났는지 */
    val contactRevealed: Boolean = false,
    val endingReached: Boolean = false,
) {
    fun holds(chip: Chip): Boolean = chip in chips

    fun hasRead(doc: DocId): Boolean = doc in readDocs

    /** 상태바 시계. 2026.11.03 23:12 에서 출발해 자정을 넘긴다. */
    val clock: String
        get() {
            val total = (GameTuning.START_MINUTE_OF_DAY + minutes).mod(24 * 60)
            return "${(total / 60).twoDigits()}:${(total % 60).twoDigits()}"
        }

    fun meets(cond: Cond): Boolean = when (cond) {
        Cond.ProxyOff -> proxyOff
        Cond.CoffeeDrunk -> coffeeAt != null
        Cond.BinderFound -> binderFound
        Cond.OtpPressed -> otpPressed
        Cond.MirrorChecked -> mirrorAt != null
        is Cond.ChapterAtLeast -> chapter >= cond.chapter
        is Cond.Deduced -> cond.deduction in deductions
        is Cond.Read -> cond.doc in readDocs
        is Cond.Visited -> cond.profile in visitedProfiles
        is Cond.Unlocked -> cond.lock in unlocked
        is Cond.Holding -> cond.chip in chips
    }

    fun <T> resolve(branch: Branch<T>): T = when (branch) {
        is Branch.Fixed -> branch.value
        is Branch.If -> if (meets(branch.cond)) branch.then else branch.otherwise
    }
}

/** 로케일과 무관하게 항상 ASCII 두 자리 (아랍어 숫자 등으로 바뀌지 않게) */
private fun Int.twoDigits(): String = toString().padStart(2, '0')

/** 연출 타이밍과 확률. 숫자는 웹판에서 실제로 손으로 맞춘 값이다. */
object GameTuning {
    const val START_MINUTE_OF_DAY = 23 * 60 + 12

    /** 실제 20초마다 게임 시계 1분 */
    const val CLOCK_TICK_MS = 20_000L
    const val SPAM_INTERVAL_MS = 52_000L
    const val GLITCH_POLL_MS = 4_000L
    const val GLITCH_CHANCE_PER_LEVEL = 0.08f
    const val GLITCH_BURST_MS = 300L

    /** 독백 타자 속도 · 줄 끝 대기 */
    const val MONO_CHAR_MS = 26L
    const val MONO_HOLD_MS = 2_300L
    const val MONO_HOLD_AUTO_MS = 1_100L
    const val MONO_HOLD_PER_CHAR_MS = 30L
    const val MONO_HANDOFF_MS = 900L

    /** WATSON 입력 중 표시. 밀린 메시지가 이 수를 넘으면 빨리 감는다. */
    const val WATSON_BACKLOG_FAST = 2
    const val WATSON_FAST_MS = 280L
    const val WATSON_BASE_MS = 650L
    const val WATSON_PER_CHAR_MS = 26L
    const val WATSON_MAX_EXTRA_MS = 1_500L

    /** 엔딩 메신저 폭주 */
    const val FLOOD_BASE_MS = 700L
    const val FLOOD_PER_CHAR_MS = 18L
    const val FLOOD_MAX_EXTRA_MS = 1_400L
    const val FLOOD_ALERT_MS = 1_100L
    const val FLOOD_HAPTIC_INTERVAL_MS = 300L

    const val TOAST_MS = 4_200L
    const val TOAST_MAX = 3
    const val FRESH_CHIP_MS = 4_000L
    const val CHAPTER3_DELAY_MS = 2_600L
    const val PROXY_HOLD_MS = 1_500L
    const val KEYPAD_MAX_DIGITS = 12
    const val KEYPAD_HINT_AFTER_FAILS = 3

    const val ENDING_BATTERY = 4
    const val LOW_BATTERY = 10
}
