package kr.paranos.stolenlife.viewmodel

import kr.paranos.stolenlife.data.AppId
import kr.paranos.stolenlife.data.ChapterCard
import kr.paranos.stolenlife.data.Chip
import kr.paranos.stolenlife.data.ChipFilter
import kr.paranos.stolenlife.data.Deduction
import kr.paranos.stolenlife.data.DocId
import kr.paranos.stolenlife.data.Inline
import kr.paranos.stolenlife.data.LockId
import kr.paranos.stolenlife.data.ParanBoard
import kr.paranos.stolenlife.data.PinnedEvidence
import kr.paranos.stolenlife.data.ProfileId

/*
 * 화면 전용 상태. 저장하지 않는다(웹판의 NAV · DR · fresh · 오버레이 · 토스트에 해당).
 * 저장되는 진행 상황은 data.GameState 쪽에 있다.
 */

/** 앱 안에서 쌓이는 화면 하나. 웹판 NAV.stack 의 { v: ... } 객체. */
sealed interface AppView {
    /** 앱 첫 화면(메신저 대화 · 검색창 · 터미널 · 금고 · 203호 · 설정) */
    data object Root : AppView
    data class Results(val chip: Chip) : AppView
    data class Doc(val id: DocId) : AppView
    data class Prof(val id: ProfileId) : AppView

    /** 엔딩: @lighthouse_1998 비공개 계정이 열린 화면 */
    data object LighthouseOpen : AppView

    /** 파란누리 터미널: 접속 직후 / 동호회 메뉴 / 게시판 */
    data object ParanConnected : AppView
    data object ParanMenu : AppView
    data class Board(val board: ParanBoard) : AppView
}

/** [app] 이 null 이면 홈 런처. */
data class PhoneNav(val app: AppId? = null, val stack: List<AppView> = emptyList()) {
    val atHome: Boolean get() = app == null
    val top: AppView? get() = stack.lastOrNull()
}

/** 단서함 바텀시트. [target] 은 칩을 눌렀을 때 바로 검색할 앱. [dot] 은 내비게이션 바의 붉은 점. */
data class DrawerUi(
    val open: Boolean = false,
    val target: AppId? = null,
    val filter: ChipFilter = ChipFilter.ALL,
    val dot: Boolean = false,
)

sealed interface ToastContent {
    /** `[MSG] WATSON 문은 내가 열었다.` */
    data class Message(val contact: String, val text: String) : ToastContent

    /** `[FILE] 단서 획득 오병철` */
    data class ChipAcquired(val chip: Chip) : ToastContent

    /** `[SPAM] …` */
    data class Spam(val text: String) : ToastContent

    /** `[WARN] 복원된 문서 3건 — …` */
    data class Restored(val titles: List<String>) : ToastContent
}

sealed interface ToastAction {
    data object OpenMessenger : ToastAction
    data object OpenSettings : ToastAction
    data class Say(val lines: List<String>) : ToastAction
}

data class Toast(val id: Long, val content: ToastContent, val action: ToastAction? = null)

/** 화면 전체를 덮는 창. 하나만 뜬다(웹판 #ov). */
sealed interface Overlay {
    data class Chapter(val card: ChapterCard, val clock: String) : Overlay

    /** [shake] 는 오답마다 1씩 오른다. 화면은 값이 바뀔 때 좌우로 흔든다. */
    data class Keypad(
        val lock: LockId,
        val entry: String = "",
        val fails: Int = 0,
        val shake: Int = 0,
    ) : Overlay

    data class CrossCheck(
        val a: PinnedEvidence,
        val bText: String,
        val bSource: String,
        val conclusion: String,
        val clock: String,
        val deduction: Deduction,
    ) : Overlay

    data object Otp : Overlay
    data object ResetConfirm : Overlay

    data class Fin(val searches: Int, val deductions: Int, val contacts: Int, val logs: Int) : Overlay
}

sealed interface KeypadKey {
    data class Digit(val value: Int) : KeypadKey
    data object Delete : KeypadKey
    data object Enter : KeypadKey
}

/** 붉은 밑줄 문장을 눌렀을 때 뜨는 작은 메뉴의 항목 */
enum class PinOption { SET, CLEAR, MATCH, REPLACE }

data class PinMenu(val pin: Inline.PinRef, val source: String, val options: List<PinOption>)

/**
 * 일회성 시각 효과. 값이 바뀔 때마다 화면이 한 번 재생한다.
 * [beaconRed] 는 다음 비콘 신호 전까지 붉은 색을 유지한다(웹판 #beacon.red).
 */
data class Fx(
    val burst: Int = 0,
    val flash: Int = 0,
    val beacon: Int = 0,
    val beaconRed: Boolean = false,
    val shake: Int = 0,
)

enum class RaidStage { BANG, SHOUT, AFTERMATH }

data class PhoneUi(
    val nav: PhoneNav = PhoneNav(),
    val drawer: DrawerUi = DrawerUi(),
    /** 방금 얻은 칩. 단서함에서 4초 동안 반짝인다. */
    val freshChips: Set<Chip> = emptySet(),
    val toasts: List<Toast> = emptyList(),
    val overlay: Overlay? = null,
    val pinMenu: PinMenu? = null,
    /** 메신저 '입력 중…' */
    val typing: Boolean = false,
    /** 엔딩 연출 중 입력 차단(웹판 LOCK) */
    val locked: Boolean = false,
    val fx: Fx = Fx(),
    val raid: RaidStage? = null,
)

/** 독백 바. 26ms 마다 바뀌므로 PhoneUi 와 따로 흘린다. */
data class MonologueUi(
    val text: String = "",
    /** 한 글자씩 찍는 중 → 오실로스코프 진폭이 커진다 */
    val talking: Boolean = false,
    /** 뒤에 줄이 더 남았다 → ▼ */
    val more: Boolean = false,
)

/** 부팅 화면이 알아야 할 것: 이어하기 버튼을 보일지 */
data class BootInfo(val loaded: Boolean = false, val canContinue: Boolean = false)
