package kr.paranos.stolenlife.viewmodel

import kr.paranos.stolenlife.data.AppId
import kr.paranos.stolenlife.data.Chip
import kr.paranos.stolenlife.data.ChipFilter
import kr.paranos.stolenlife.data.Command
import kr.paranos.stolenlife.data.DemoJump
import kr.paranos.stolenlife.data.DocId
import kr.paranos.stolenlife.data.Inline
import kr.paranos.stolenlife.data.LockId
import kr.paranos.stolenlife.data.ParanBoard
import kr.paranos.stolenlife.data.ProfileId
import kr.paranos.stolenlife.data.Reaction
import kr.paranos.stolenlife.data.RoomSpot

/**
 * 화면이 ViewModel 에 보낼 수 있는 모든 입력. 웹판 click 핸들러의 data-* 분기와 act() 를 한데 모았다.
 * 화면 코드는 이 인터페이스만 알면 되므로 미리보기에서는 빈 구현을 넣을 수 있다.
 */
interface GameIntents {
    // 부팅
    fun newGame()
    fun continueGame()

    // 껍데기(독백 바 · 하단 바 · 단서함)
    fun tapMonologue()
    fun back()
    fun home()
    fun toggleDrawer()
    fun closeDrawer()
    fun setDrawerFilter(filter: ChipFilter)
    fun tapTrayChip(chip: Chip)
    fun hint()
    fun openChipPicker()
    fun tapToast(toast: Toast)

    // 문서 · 프로필 속 토큰
    fun openApp(app: AppId)
    fun tapChip(chip: Chip)
    fun tapPin(pin: Inline.PinRef, source: String)
    fun tapRedaction()
    fun tapReaction(reaction: Reaction)
    fun openDocument(doc: DocId)
    fun openProfile(profile: ProfileId)
    fun openLock(lock: LockId)
    fun choosePinOption(option: PinOption)
    fun dismissPinMenu()
    fun unpin()

    // 창
    fun pressKey(key: KeypadKey)
    fun closeOverlay()
    fun continueChapter()
    fun confirmCrossCheck()
    fun confirmOtp()
    fun answerReset(yes: Boolean)
    fun restartFromFin()

    // 앱별
    fun messageAction(command: Command)
    fun inspect(spot: RoomSpot)
    fun dialModem()
    fun enterParan()
    fun openBoard(board: ParanBoard)
    fun goToRoom()
    fun proxyHoldStarted()
    fun killProxy()
    fun toggleSound()
    fun askReset()
    fun jump(to: DemoJump)
}
