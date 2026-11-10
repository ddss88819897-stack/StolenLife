package kr.paranos.stolenlife.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.isActive
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kr.paranos.stolenlife.data.AppId
import kr.paranos.stolenlife.data.ChapterScript
import kr.paranos.stolenlife.data.Chip
import kr.paranos.stolenlife.data.ChipFilter
import kr.paranos.stolenlife.data.Command
import kr.paranos.stolenlife.data.Deduction
import kr.paranos.stolenlife.data.DemoJump
import kr.paranos.stolenlife.data.DocId
import kr.paranos.stolenlife.data.GameRepository
import kr.paranos.stolenlife.data.GameRepository.LogLines
import kr.paranos.stolenlife.data.GameRepository.Narration
import kr.paranos.stolenlife.data.GameState
import kr.paranos.stolenlife.data.GameTuning
import kr.paranos.stolenlife.data.Inline
import kr.paranos.stolenlife.data.Landing
import kr.paranos.stolenlife.data.LockId
import kr.paranos.stolenlife.data.LockReward
import kr.paranos.stolenlife.data.LogEntry
import kr.paranos.stolenlife.data.LogKind
import kr.paranos.stolenlife.data.MessageSpec
import kr.paranos.stolenlife.data.ParanBoard
import kr.paranos.stolenlife.data.PinnedEvidence
import kr.paranos.stolenlife.data.ProfileId
import kr.paranos.stolenlife.data.Reaction
import kr.paranos.stolenlife.data.RoomSpot
import kr.paranos.stolenlife.data.SaveStore
import kr.paranos.stolenlife.data.Sender
import kr.paranos.stolenlife.data.StoryEvent
import kr.paranos.stolenlife.data.ChatMessage
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

/**
 * 게임 엔진. 웹판 <script> 의 상태 변경 로직 전부를 옮겼다.
 *
 *  - [state]    저장되는 진행 상황(GameState). 바뀔 때마다 DataStore 에 JSON 으로 쓴다.
 *  - [ui]       화면 전용 상태(앱 스택 · 단서함 · 창 · 토스트 · 효과).
 *  - [monologue] 독백 바(26ms 마다 바뀌어서 따로 흘린다).
 *  - [feedback] 소리와 진동 신호.
 *
 * "한 판"에 묶인 비동기 연출(WATSON 입력 중 · 챕터 전환 · 엔딩)은 전부 [run] 스코프에서 돈다.
 * 새로 시작하거나 시연용 점프를 하면 [resetRun] 이 그 스코프를 통째로 취소한다(웹판 GEN 증가).
 */
class GameViewModel(application: Application) : AndroidViewModel(application), GameIntents {

    private val repo = GameRepository
    private val store = SaveStore(application)

    private val _state = MutableStateFlow(GameState())
    val state: StateFlow<GameState> = _state.asStateFlow()

    private val _ui = MutableStateFlow(PhoneUi())
    val ui: StateFlow<PhoneUi> = _ui.asStateFlow()

    private val _boot = MutableStateFlow(BootInfo())
    val boot: StateFlow<BootInfo> = _boot.asStateFlow()

    private val _feedback = MutableSharedFlow<Feedback>(extraBufferCapacity = 64)
    val feedback: SharedFlow<Feedback> = _feedback.asSharedFlow()

    /** 지금 깔려야 할 배경음. 소리를 끄거나 엔딩에 들어서면 [Bgm.NONE]. */
    val bgm: StateFlow<Bgm> = _state
        .map { st ->
            when {
                !st.sound || st.endingReached -> Bgm.NONE
                !st.started -> Bgm.TITLE
                st.chapter >= 3 -> Bgm.SIEGE
                st.chapter == 2 -> Bgm.INN
                else -> Bgm.WAREHOUSE
            }
        }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.Eagerly, Bgm.TITLE)

    private val mono = MonologueEngine(viewModelScope) { tone(1300f, 0.012f, Wave.SQUARE, 0.008f) }
    val monologue: StateFlow<MonologueUi> = mono.ui

    private var run: CoroutineScope = newRunScope()
    private var watsonTail: Job? = null
    private var watsonPending = 0
    private var chapterGate: CompletableDeferred<Unit>? = null
    private var toastSeq = 0L
    private var foreground = true
    private var savedGame: GameState? = null
    private val pendingSave = MutableStateFlow<GameState?>(null)

    private val s: GameState get() = _state.value

    init {
        viewModelScope.launch {
            savedGame = store.load()
            _boot.value = BootInfo(loaded = true, canContinue = savedGame?.let { it.started && !it.endingReached } == true)
        }
        viewModelScope.launch {
            pendingSave.filterNotNull().conflate().collect { store.save(it) }
        }
        startTimers()
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  작은 도구들
    // ═════════════════════════════════════════════════════════════════════════

    private fun newRunScope() = CoroutineScope(viewModelScope.coroutineContext + SupervisorJob(viewModelScope.coroutineContext.job))

    private inline fun edit(block: GameState.() -> GameState) {
        _state.value = _state.value.block()
    }

    private inline fun ui(block: PhoneUi.() -> PhoneUi) {
        _ui.value = _ui.value.block()
    }

    private fun save() {
        if (s.started) pendingSave.value = s
    }

    private fun tick(minutes: Int = 1) = edit { copy(minutes = this.minutes + minutes) }

    private fun logit(kind: LogKind, text: String) = edit { copy(log = log + LogEntry(clock, kind, text)) }

    private fun tone(frequency: Float, seconds: Float = 0.05f, wave: Wave = Wave.SQUARE, volume: Float = 0.035f, delayMs: Long = 0) {
        if (s.sound) _feedback.tryEmit(Feedback.Tone(frequency, seconds, wave, volume, delayMs))
    }

    /** 버튼 반응음: 1998년 키보드 같은 짧은 딸깍. 높은 클릭 + 낮은 받침. */
    fun pressSound() {
        tone(1900f, 0.012f, Wave.SQUARE, 0.012f)
        tone(240f, 0.025f, Wave.TRIANGLE, 0.03f)
    }

    private fun haptic(kind: HapticKind) {
        _feedback.tryEmit(Feedback.Haptic(kind))
    }

    /** 상태바 등대가 세 번 깜빡인다. 붉으면 경찰청으로 뭔가 새어 나갔다는 뜻. */
    private fun beacon(red: Boolean = false) {
        ui { copy(fx = fx.copy(beacon = fx.beacon + 1, beaconRed = red)) }
        if (red) tone(196f, 0.18f, Wave.SQUARE, 0.04f) else tone(1760f, 0.04f, Wave.SQUARE, 0.015f)
    }

    private fun burst() = ui { copy(fx = fx.copy(burst = fx.burst + 1)) }

    private fun say(lines: List<String>, queued: Boolean = false, auto: Boolean = false) =
        mono.say(lines, queued, auto)

    private fun say(line: String) = mono.say(listOf(line))

    private fun toast(content: ToastContent, action: ToastAction? = null) {
        val toast = Toast(++toastSeq, content, action)
        ui { copy(toasts = (toasts + toast).takeLast(GameTuning.TOAST_MAX)) }
        viewModelScope.launch {
            delay(GameTuning.TOAST_MS)
            ui { copy(toasts = toasts.filterNot { it.id == toast.id }) }
        }
    }

    private val blocked: Boolean get() = _ui.value.locked

    // ═════════════════════════════════════════════════════════════════════════
    //  타이머 (게임 시계 · 스팸 · 글리치)
    // ═════════════════════════════════════════════════════════════════════════

    private fun startTimers() {
        viewModelScope.launch {
            while (isActive) {
                delay(GameTuning.CLOCK_TICK_MS)
                if (foreground && s.started && !s.endingReached) {
                    tick()
                    save()
                }
            }
        }
        viewModelScope.launch {
            while (isActive) {
                delay(GameTuning.SPAM_INTERVAL_MS)
                if (!foreground || !s.started || s.endingReached || _ui.value.overlay != null) continue
                val text = repo.spamFor(s.chapter).random()
                val reply = if (s.chapter == 3) Narration.spamSiege else Narration.spamCalm
                toast(ToastContent.Spam(text), ToastAction.Say(reply))
            }
        }
        viewModelScope.launch {
            while (isActive) {
                delay(GameTuning.GLITCH_POLL_MS)
                if (foreground && s.glitch > 0 && Random.nextFloat() < s.glitch * GameTuning.GLITCH_CHANCE_PER_LEVEL) burst()
            }
        }
    }

    /** 앱이 화면에서 사라지면 시계 · 스팸을 멈추고 즉시 저장한다. */
    fun onForegroundChanged(visible: Boolean) {
        foreground = visible
        if (!visible) save()
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  한 판 시작 · 초기화
    // ═════════════════════════════════════════════════════════════════════════

    private fun resetRun() {
        run.cancel()
        run = newRunScope()
        watsonTail = null
        watsonPending = 0
        chapterGate = null
        mono.reset()
        ui { PhoneUi(fx = fx) }
    }

    override fun newGame() {
        resetRun()
        run.launch { startGame() }
    }

    override fun continueGame() {
        val saved = savedGame ?: return newGame()
        resetRun()
        _state.value = saved
        home()
        say(repo.hintFor(saved))
        // 3장 진입 2.6초 대기 중에 앱이 꺼졌던 경우 이어서 진입시킨다.
        if (saved.chapter == 2 && saved.chapter3Scheduled) run.launch { delay(GameTuning.CHAPTER3_DELAY_MS); enterChapterThree() }
    }

    private suspend fun startGame() {
        _state.value = GameState(started = true)
        save()
        enterChapter(repo.chapters.getValue(1))
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  WATSON 메신저 큐 (웹판 W)
    // ═════════════════════════════════════════════════════════════════════════

    private fun watson(specs: List<MessageSpec>, fast: Boolean = false): Job {
        val previous = watsonTail
        watsonPending += specs.size
        val job = run.launch {
            previous?.join()
            for (spec in specs) {
                watsonPending = max(0, watsonPending - 1)
                if (spec.sender == Sender.WATSON) {
                    ui { copy(typing = true) }
                    val wait = if (fast || watsonPending > GameTuning.WATSON_BACKLOG_FAST) {
                        GameTuning.WATSON_FAST_MS
                    } else {
                        GameTuning.WATSON_BASE_MS + min(GameTuning.WATSON_MAX_EXTRA_MS, spec.text.length * GameTuning.WATSON_PER_CHAR_MS)
                    }
                    delay(wait)
                    ui { copy(typing = false) }
                }
                appendMessage(spec)
                if (_ui.value.nav.app != AppId.MESSENGER && spec.sender == Sender.WATSON) {
                    edit { copy(unread = unread + 1) }
                    toast(ToastContent.Message(repo.contactName(s), spec.text), ToastAction.OpenMessenger)
                }
                if (spec.sender == Sender.WATSON) tone(990f, 0.06f, Wave.TRIANGLE, 0.03f) else tone(660f, 0.06f, Wave.TRIANGLE, 0.03f)
                save()
            }
        }
        watsonTail = job
        return job
    }

    private fun appendMessage(spec: MessageSpec, text: String = spec.text) =
        edit { copy(messages = messages + ChatMessage(spec.sender, text, clock, spec.rich, spec.action)) }

    // ═════════════════════════════════════════════════════════════════════════
    //  칩 · 이야기 트리거
    // ═════════════════════════════════════════════════════════════════════════

    private fun addChip(chip: Chip, quiet: Boolean = false): Boolean {
        if (s.holds(chip)) return false
        edit { copy(chips = chips + chip) }
        tick()
        ui { copy(freshChips = freshChips + chip, drawer = if (drawer.open) drawer else drawer.copy(dot = true)) }
        if (!quiet) {
            toast(ToastContent.ChipAcquired(chip))
            tone(1180f, 0.06f, Wave.TRIANGLE, 0.03f)
            haptic(HapticKind.TICK)
        }
        viewModelScope.launch {
            delay(GameTuning.FRESH_CHIP_MS)
            ui { copy(freshChips = freshChips - chip) }
        }
        save()
        return true
    }

    /** 웹판 story(ev). 모든 비트는 한 번만 발동한다. */
    private fun story(event: StoryEvent) {
        val beat = repo.beats[event] ?: return
        if (event.key in s.firedBeats) return
        edit { copy(firedBeats = firedBeats + event.key) }
        beat.grantChip?.let { addChip(it) }
        beat.glitchAtLeast?.let { level -> edit { copy(glitch = max(glitch, level)) } }
        if (beat.watson.isNotEmpty()) watson(beat.watson)
        if (beat.monologue.isNotEmpty()) say(beat.monologue)
        if (beat.checksChapter3) checkChapterThree()
        save()
    }

    private fun checkChapterThree() {
        val st = s
        if (st.chapter == 2 && Deduction.BORROWED_HABITS in st.deductions && st.hasRead(DocId.VAULT_SUMMARY) && !st.chapter3Scheduled) {
            edit { copy(chapter3Scheduled = true) }
            save()
            run.launch {
                delay(GameTuning.CHAPTER3_DELAY_MS)
                enterChapterThree()
            }
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  앱 탐색 (웹판 NAV)
    // ═════════════════════════════════════════════════════════════════════════

    private fun openAppTo(app: AppId, view: AppView? = null) {
        ui {
            copy(
                pinMenu = null,
                drawer = drawer.copy(open = false),
                nav = PhoneNav(app, listOfNotNull(AppView.Root, view)),
            )
        }
        if (app == AppId.MESSENGER) edit { copy(unread = 0) }
        if (app !in s.seenApps) edit { copy(seenApps = seenApps + app) }
        save()
    }

    private fun push(view: AppView) = ui { copy(pinMenu = null, nav = nav.copy(stack = nav.stack + view)) }

    override fun openApp(app: AppId) {
        if (blocked) return
        openAppTo(app)
    }

    override fun back() {
        if (blocked) return
        val current = _ui.value
        ui { copy(pinMenu = null) }
        if (current.drawer.open) return closeDrawer()
        if (current.nav.atHome) return
        if (current.nav.stack.size > 1) ui { copy(nav = nav.copy(stack = nav.stack.dropLast(1))) } else home()
    }

    override fun home() {
        if (blocked) return
        ui { copy(pinMenu = null, drawer = drawer.copy(open = false), nav = PhoneNav()) }
    }

    /** 시스템 뒤로 가기: 처리했으면 true. 홈에서 아무것도 안 열렸으면 false(앱 종료로 넘김). */
    fun handleSystemBack(): Boolean {
        val current = _ui.value
        if (current.locked) return true
        when (current.overlay) {
            is Overlay.Keypad, Overlay.ResetConfirm -> {
                closeOverlay()
                return true
            }

            null -> Unit
            else -> return true
        }
        if (current.pinMenu != null) {
            dismissPinMenu()
            return true
        }
        if (current.drawer.open || !current.nav.atHome) {
            back()
            return true
        }
        return false
    }

    override fun openDocument(doc: DocId) {
        if (blocked) return
        push(AppView.Doc(doc))
        val document = repo.document(doc)
        if (!s.hasRead(doc)) {
            edit { copy(readDocs = readDocs + doc) }
            tick()
            document.logLine?.let {
                logit(LogKind.READ, it)
                beacon(red = true)
            }
            say(s.resolve(document.monologue))
            story(StoryEvent.DocumentOpened(doc))
            save()
        } else {
            say(s.resolve(document.monologue))
        }
    }

    override fun openProfile(profile: ProfileId) {
        if (blocked) return
        push(AppView.Prof(profile))
        say(s.resolve(repo.profile(profile).monologue))
        if (profile !in s.visitedProfiles) {
            edit { copy(visitedProfiles = visitedProfiles + profile) }
            story(StoryEvent.ProfileVisited(profile))
            save()
        }
    }

    private fun search(app: AppId, chip: Chip) {
        ui { copy(overlay = null) }
        logit(LogKind.SEARCH, LogLines.search(app, chip))
        beacon()
        tick()
        ui { copy(nav = PhoneNav(app, listOf(AppView.Root, AppView.Results(chip)))) }
        if (repo.results(app, chip, s).isEmpty()) say(Narration.noResults.random())
        save()
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  껍데기 입력
    // ═════════════════════════════════════════════════════════════════════════

    override fun tapMonologue() = mono.tap()

    override fun toggleDrawer() {
        if (blocked) return
        if (_ui.value.drawer.open) closeDrawer() else openDrawer(null)
    }

    private fun openDrawer(target: AppId?) {
        val app = _ui.value.nav.app
        ui {
            copy(
                pinMenu = null,
                drawer = drawer.copy(open = true, dot = false, target = target ?: app?.takeIf { it.searchable }),
            )
        }
        tone(520f, 0.04f, Wave.TRIANGLE, 0.02f)
    }

    override fun closeDrawer() = ui { copy(drawer = drawer.copy(open = false)) }

    override fun setDrawerFilter(filter: ChipFilter) = ui { copy(drawer = drawer.copy(filter = filter)) }

    override fun tapTrayChip(chip: Chip) {
        if (blocked) return
        val current = _ui.value
        val target = current.drawer.target ?: current.nav.app?.takeIf { it.searchable }
        if (target != null) {
            closeDrawer()
            search(target, chip)
        } else {
            say(Narration.searchElsewhere(chip.label))
        }
    }

    override fun hint() {
        if (blocked) return
        say(repo.hintFor(s))
    }

    override fun openChipPicker() {
        if (blocked) return
        if (s.chips.isEmpty()) say(Narration.PICKER_EMPTY) else openDrawer(_ui.value.nav.app)
    }

    override fun tapToast(toast: Toast) {
        ui { copy(toasts = toasts.filterNot { it.id == toast.id }) }
        if (blocked) return
        when (val action = toast.action) {
            ToastAction.OpenMessenger -> openAppTo(AppId.MESSENGER)
            ToastAction.OpenSettings -> openAppTo(AppId.SETTINGS)
            is ToastAction.Say -> say(action.lines)
            null -> Unit
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  문서 속 토큰
    // ═════════════════════════════════════════════════════════════════════════

    override fun tapChip(chip: Chip) {
        if (blocked) return
        if (!addChip(chip)) say(Narration.chipAlreadyHeld(chip.label))
    }

    override fun tapRedaction() {
        if (blocked) return
        say(s.resolve(repo.reactions.getValue(Reaction.MASK)))
    }

    override fun tapReaction(reaction: Reaction) {
        if (blocked) return
        say(s.resolve(repo.reactions.getValue(reaction)))
    }

    override fun tapPin(pin: Inline.PinRef, source: String) {
        if (blocked) return
        val held = s.pin
        val options = when {
            held == null -> listOf(PinOption.SET)
            held.pin == pin.pin -> listOf(PinOption.CLEAR)
            else -> listOf(PinOption.MATCH, PinOption.REPLACE)
        }
        ui { copy(pinMenu = PinMenu(pin, source, options)) }
    }

    override fun dismissPinMenu() = ui { copy(pinMenu = null) }

    override fun choosePinOption(option: PinOption) {
        val menu = _ui.value.pinMenu ?: return
        dismissPinMenu()
        when (option) {
            PinOption.SET, PinOption.REPLACE -> {
                edit { copy(pin = PinnedEvidence(menu.pin.pin, menu.pin.text, menu.source)) }
                tone(880f, 0.05f)
                save()
                say(Narration.PIN_FIXED)
            }

            PinOption.CLEAR -> {
                edit { copy(pin = null) }
                save()
            }

            PinOption.MATCH -> crossCheck(menu.pin, menu.source)
        }
    }

    override fun unpin() {
        if (blocked) return
        edit { copy(pin = null) }
        save()
    }

    private fun crossCheck(tapped: Inline.PinRef, source: String) {
        val held = s.pin ?: return
        val pair = repo.pairFor(held.pin, tapped.pin)
        if (pair == null) {
            tone(160f, 0.2f, Wave.SAWTOOTH, 0.03f)
            say(Narration.crossMismatch.random())
            return
        }
        if (pair.deduction in s.deductions) {
            say(Narration.CROSS_REPEAT)
            return
        }
        edit { copy(deductions = deductions + pair.deduction, pin = null) }
        tick(2)
        logit(LogKind.DEDUCTION, pair.conclusion)
        beacon(red = true)
        save()
        ui { copy(fx = fx.copy(flash = fx.flash + 1)) }
        tone(220f, 0.1f)
        tone(440f, 0.1f, delayMs = 100)
        tone(880f, 0.2f, delayMs = 200)
        ui { copy(overlay = Overlay.CrossCheck(held, tapped.text, source, pair.conclusion, s.clock, pair.deduction)) }
    }

    override fun confirmCrossCheck() {
        val overlay = _ui.value.overlay as? Overlay.CrossCheck ?: return
        ui { copy(overlay = null) }
        story(StoryEvent.Deduced(overlay.deduction))
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  잠금 · 키패드
    // ═════════════════════════════════════════════════════════════════════════

    override fun openLock(lock: LockId) {
        if (blocked) return
        val spec = repo.locks.getValue(lock)
        if (lock in s.unlocked) {
            // 웹판 그대로: 이미 연 웹하드 · 클라우드를 다시 누르면 해제 기록이 한 줄 더 남는다.
            when (lock) {
                LockId.VAULT -> openAppTo(AppId.VAULT)
                LockId.LIGHTHOUSE -> Unit
                else -> grant(lock)
            }
            return
        }
        val gate = spec.requires
        if (gate != null && !s.meets(gate)) {
            say(spec.gateLines)
            return
        }
        ui { copy(overlay = Overlay.Keypad(lock)) }
    }

    override fun pressKey(key: KeypadKey) {
        val pad = _ui.value.overlay as? Overlay.Keypad ?: return
        val spec = repo.locks.getValue(pad.lock)
        when (key) {
            KeypadKey.Delete -> {
                tone(500f, 0.03f)
                ui { copy(overlay = pad.copy(entry = pad.entry.dropLast(1))) }
            }

            is KeypadKey.Digit -> {
                if (pad.entry.length < GameTuning.KEYPAD_MAX_DIGITS) {
                    tone(800f + key.value * 40f, 0.03f)
                    ui { copy(overlay = pad.copy(entry = pad.entry + key.value)) }
                }
            }

            KeypadKey.Enter -> {
                if (pad.entry == spec.code) {
                    tone(1046f, 0.08f)
                    tone(1568f, 0.12f, delayMs = 90)
                    ui { copy(overlay = null) }
                    edit { copy(unlocked = unlocked + pad.lock) }
                    tick()
                    save()
                    grant(pad.lock)
                    return
                }
                val fails = pad.fails + 1
                tone(140f, 0.25f, Wave.SAWTOOTH, 0.04f)
                haptic(HapticKind.DOUBLE_BUZZ)
                say(if (fails >= GameTuning.KEYPAD_HINT_AFTER_FAILS) Narration.keypadWrongWithHint(spec.hint) else Narration.keypadWrong)
                ui { copy(overlay = pad.copy(entry = "", fails = fails, shake = pad.shake + 1)) }
            }
        }
    }

    /** 잠금이 풀렸을 때 일어나는 일 (웹판 LOCKS[id].ok) */
    private fun grant(lock: LockId) {
        val spec = repo.locks.getValue(lock)
        logit(LogKind.UNLOCK, spec.logLine)
        when (val reward = spec.reward) {
            is LockReward.OpenDocument -> openDocument(reward.doc)
            LockReward.VaultMasterKey -> story(StoryEvent.VaultKeyAccepted)
            LockReward.FinalTruth -> run.launch { ending() }
        }
        save()
    }

    override fun closeOverlay() {
        when (_ui.value.overlay) {
            is Overlay.Keypad, Overlay.ResetConfirm -> ui { copy(overlay = null) }
            else -> Unit
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  파란여관 203호
    // ═════════════════════════════════════════════════════════════════════════

    override fun inspect(spot: RoomSpot) {
        if (blocked) return
        val room = repo.room
        val chapter = s.chapter
        when (spot) {
            RoomSpot.COFFEE -> if (s.coffeeAt == null) {
                edit { copy(coffeeAt = clock) }
                logit(LogKind.CONTACT, LogLines.COFFEE_CONTACT)
                beacon(red = true)
                tick()
                save()
                openDocument(DocId.BODYLOG)
                story(StoryEvent.CoffeeDrunk)
            } else {
                openDocument(DocId.BODYLOG)
                say(room.coffeeAgain)
            }

            RoomSpot.VENT -> if (!s.binderFound) {
                edit { copy(binderFound = true) }
                save()
                openDocument(DocId.BINDER)
                story(StoryEvent.BinderFound)
            } else {
                openDocument(DocId.BINDER)
            }

            RoomSpot.OTP -> when {
                s.otpPressed -> say(room.otpAlreadyPressed)
                LockId.VAULT in s.unlocked -> {
                    edit { copy(otpPressed = true) }
                    logit(LogKind.AUTH, LogLines.OTP_AUTH)
                    beacon(red = true)
                    tick()
                    save()
                    ui { copy(overlay = Overlay.Otp) }
                }

                else -> say(room.otpIdle)
            }

            RoomSpot.MIRROR -> if (chapter < 3) {
                say(room.mirrorNotYet)
            } else {
                if (s.mirrorAt == null) {
                    edit { copy(mirrorAt = clock) }
                    save()
                }
                openDocument(DocId.MIRRORLOG)
            }

            RoomSpot.DOOR -> say(if (chapter >= 3) room.doorSiege else room.doorCalm)
            RoomSpot.WINDOW -> say(if (chapter >= 3) room.windowSiege else room.windowCalm)
            RoomSpot.BED -> say(room.bed)
        }
        if (spot == RoomSpot.DOOR || spot == RoomSpot.WINDOW || spot == RoomSpot.BED) {
            edit { copy(glancedSpots = glancedSpots + spot) }
            save()
        }
    }

    override fun confirmOtp() {
        if (_ui.value.overlay != Overlay.Otp) return
        ui { copy(overlay = null) }
        openAppTo(AppId.VAULT)
        if (!s.hasRead(DocId.VAULT_SUMMARY)) {
            val summary = repo.document(DocId.VAULT_SUMMARY)
            edit { copy(readDocs = readDocs + DocId.VAULT_SUMMARY) }
            summary.logLine?.let { logit(LogKind.READ, it) }
            say(s.resolve(summary.monologue))
            story(StoryEvent.DocumentOpened(DocId.VAULT_SUMMARY))
            save()
        }
    }

    override fun goToRoom() {
        if (blocked) return
        openAppTo(AppId.ROOM)
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  파란누리 1998 터미널
    // ═════════════════════════════════════════════════════════════════════════

    override fun dialModem() {
        if (blocked) return
        if (s.sound) _feedback.tryEmit(Feedback.Modem)
        say(Narration.MODEM_DIAL)
        run.launch {
            delay(MODEM_CONNECT_MS)
            push(AppView.ParanConnected)
        }
    }

    override fun enterParan() {
        if (blocked) return
        tone(660f, 0.05f)
        push(AppView.ParanMenu)
        say(Narration.PARAN_ENTER)
    }

    override fun openBoard(board: ParanBoard) {
        if (blocked) return
        push(AppView.Board(board))
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  메신저 버튼 · 챕터
    // ═════════════════════════════════════════════════════════════════════════

    override fun messageAction(command: Command) {
        if (blocked || command in s.completedCommands) return
        when (command) {
            Command.GO_CHAPTER_2 -> run.launch { enterChapterTwo() }
        }
    }

    override fun continueChapter() {
        if (_ui.value.overlay !is Overlay.Chapter) return
        ui { copy(overlay = null) }
        chapterGate?.complete(Unit)
        chapterGate = null
    }

    private suspend fun chapterCard(script: ChapterScript) {
        tone(110f, 0.5f, Wave.SAWTOOTH, 0.05f)
        val gate = CompletableDeferred<Unit>()
        chapterGate = gate
        ui { copy(overlay = Overlay.Chapter(script.card, s.clock), drawer = drawer.copy(open = false), pinMenu = null) }
        gate.await()
    }

    private suspend fun enterChapterTwo() {
        if (s.chapter >= 2) return
        val script = repo.chapters.getValue(2)
        edit {
            copy(
                completedCommands = completedCommands + Command.GO_CHAPTER_2,
                chapter = 2,
                battery = script.battery,
            )
        }
        tick(script.minutesAdvance)
        save()
        enterChapter(script)
    }

    private suspend fun enterChapterThree() {
        if (s.chapter >= 3) return
        val script = repo.chapters.getValue(3)
        edit { copy(chapter = 3, battery = script.battery) }
        tick(script.minutesAdvance)
        save()
        enterChapter(script)
    }

    /** 카드 → 읽음 처리 → 칩 → 수신 로그 → 화면 이동 → 독백 → WATSON (웹판 startGame / goCh2 / goCh3) */
    private suspend fun enterChapter(script: ChapterScript) {
        chapterCard(script)
        script.markRead?.let { doc -> edit { copy(readDocs = readDocs + doc) } }
        script.grantChip?.let { addChip(it, quiet = true) }
        script.receiveLog?.let { logit(LogKind.RECEIVE, it) }
        when (val landing = script.landing) {
            Landing.Home -> home()
            is Landing.App -> openAppTo(landing.app)
            is Landing.Doc -> {
                openAppTo(landing.app, AppView.Doc(landing.doc))
                tone(988f, 0.1f)
                tone(988f, 0.1f, delayMs = 160)
            }
        }
        say(script.monologue, queued = script.monologueQueued)
        watson(script.watson)
        save()
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  Proxy 1998 해제
    // ═════════════════════════════════════════════════════════════════════════

    override fun proxyHoldStarted() {
        if (blocked || s.proxyOff) return
        tone(200f, 1.4f, Wave.SAWTOOTH, 0.02f)
    }

    override fun killProxy() {
        if (blocked || s.proxyOff || Deduction.NOT_DOYUN !in s.deductions) return
        run.launch { proxyCollapse() }
    }

    private suspend fun proxyCollapse() {
        edit { copy(proxyOff = true, glitch = 3) }
        tick(2)
        logit(LogKind.SYSTEM, LogLines.PROXY_KILLED)
        beacon(red = true)
        save()
        repeat(4) { i ->
            burst()
            tone(80f + i * 60f, 0.2f, Wave.SAWTOOTH, 0.05f)
            delay(PROXY_BURST_MS)
            delay(PROXY_BURST_GAP_MS)
        }
        val script = repo.proxyCollapse
        val restored = script.restoredDocs.filter(s::hasRead).map { repo.document(it).title.take(RESTORED_TITLE_CHARS) }
        toast(ToastContent.Restored(restored), ToastAction.OpenSettings)
        say(script.monologue)
        watson(script.watson)
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  엔딩
    // ═════════════════════════════════════════════════════════════════════════

    private suspend fun ending() {
        val script = repo.ending
        edit { copy(endingReached = true, battery = GameTuning.ENDING_BATTERY) }
        ui {
            copy(
                locked = true,
                drawer = drawer.copy(open = false),
                overlay = null,
                pinMenu = null,
                nav = PhoneNav(AppId.INSTA, listOf(AppView.Root, AppView.LighthouseOpen)),
            )
        }
        edit { copy(glitch = 3) }
        save()

        val heartbeat = run.launch {
            while (isActive) {
                beacon(red = true)
                delay(ENDING_BEACON_MS)
            }
        }
        delay(ENDING_REVEAL_DELAY_MS)
        say(script.revelation, auto = true).await()

        edit { copy(contactRevealed = true) }
        ui { copy(nav = PhoneNav(AppId.MESSENGER, listOf(AppView.Root))) }
        edit { copy(unread = 0) }

        val policeDump = repo.policeLogDump(s.log)
        val pulse = run.launch {
            while (isActive) {
                haptic(HapticKind.FLOOD_PULSE)
                delay(GameTuning.FLOOD_HAPTIC_INTERVAL_MS)
            }
        }
        for (spec in script.flood) {
            delay(
                if (spec.sender == Sender.WATSON) {
                    GameTuning.FLOOD_BASE_MS + min(GameTuning.FLOOD_MAX_EXTRA_MS, spec.text.length * GameTuning.FLOOD_PER_CHAR_MS)
                } else {
                    GameTuning.FLOOD_ALERT_MS
                },
            )
            appendMessage(spec, if (spec.sender == Sender.POLICE_LOG) policeDump else spec.text)
            if (spec.sender == Sender.ALERT) {
                tone(330f, 0.07f, Wave.SAWTOOTH, 0.04f)
                burst()
            } else {
                tone(990f, 0.07f, Wave.TRIANGLE, 0.04f)
            }
        }
        pulse.cancel()
        heartbeat.cancel()
        save()
        delay(ENDING_RAID_DELAY_MS)
        raid()
    }

    private suspend fun raid() {
        val script = repo.ending
        ui { copy(raid = RaidStage.BANG, fx = fx.copy(shake = fx.shake + 1)) }
        tone(70f, 0.5f, Wave.SAWTOOTH, 0.2f)
        tone(55f, 0.7f, Wave.SQUARE, 0.12f)
        haptic(HapticKind.DOUBLE_BUZZ)
        delay(RAID_SHOUT_MS)
        ui { copy(raid = RaidStage.SHOUT) }
        delay(RAID_AFTERMATH_MS)
        ui { copy(raid = RaidStage.AFTERMATH) }
        say(script.lastWords, auto = true).await()
        delay(RAID_FIN_MS)

        val log = s.log
        fun count(kind: LogKind) = log.count { it.kind == kind }
        ui {
            copy(
                raid = null,
                locked = false,
                overlay = Overlay.Fin(
                    searches = count(LogKind.SEARCH),
                    deductions = count(LogKind.DEDUCTION),
                    contacts = count(LogKind.CONTACT) + count(LogKind.AUTH),
                    logs = log.size,
                ),
            )
        }
    }

    override fun restartFromFin() {
        if (_ui.value.overlay !is Overlay.Fin) return
        newGame()
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  설정
    // ═════════════════════════════════════════════════════════════════════════

    override fun toggleSound() {
        if (blocked) return
        edit { copy(sound = !sound) }
        save()
    }

    override fun askReset() {
        if (blocked) return
        ui { copy(overlay = Overlay.ResetConfirm) }
    }

    override fun answerReset(yes: Boolean) {
        if (_ui.value.overlay != Overlay.ResetConfirm) return
        ui { copy(overlay = null) }
        if (!yes) return
        viewModelScope.launch {
            store.clear()
            newGame()
        }
    }

    override fun jump(to: DemoJump) {
        if (blocked) return
        resetRun()
        _state.value = repo.demoState(to)
        save()
        when (to) {
            DemoJump.CHAPTER_2 -> run.launch { enterChapterTwo() }
            DemoJump.CHAPTER_3 -> run.launch { enterChapterThree() }
            DemoJump.BEFORE_ENDING -> {
                openAppTo(AppId.INSTA, AppView.Results(Chip.LIGHTHOUSE_1998))
                say(Narration.DEMO_BEFORE_ENDING)
            }
        }
    }

    override fun onCleared() {
        pendingSave.value?.let { last -> CoroutineScope(SupervisorJob()).launch { store.save(last) } }
        super.onCleared()
    }

    private companion object {
        const val MODEM_CONNECT_MS = 1_300L
        const val PROXY_BURST_MS = 320L
        const val PROXY_BURST_GAP_MS = 80L
        const val RESTORED_TITLE_CHARS = 14
        const val ENDING_BEACON_MS = 800L
        const val ENDING_REVEAL_DELAY_MS = 900L
        const val ENDING_RAID_DELAY_MS = 1_800L
        const val RAID_SHOUT_MS = 1_000L
        const val RAID_AFTERMATH_MS = 2_200L
        const val RAID_FIN_MS = 1_200L
    }
}
