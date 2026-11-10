package kr.paranos.stolenlife.viewmodel

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kr.paranos.stolenlife.data.GameTuning

/**
 * 독백 바 타자기. 웹판 say() · nextMono() · finishLine() · monoTap() 을 코루틴으로 옮겼다.
 *
 * - [say] 는 기본적으로 진행 중인 독백을 끊고 새로 말한다. queued = true 면 뒤에 잇는다.
 * - 반환값은 마지막 줄이 "끝났을 때" 완료된다. 엔딩처럼 독백이 다 흐른 뒤에 다음 연출을
 *   이어야 하는 곳에서 await 한다(웹판 Promise).
 * - auto 줄은 사용자가 누르지 않아도 넘어간다. 일반 줄은 마지막 줄이면 다 찍히는 즉시 완료된다.
 *
 * 메인 스레드에서만 부른다(viewModelScope = Dispatchers.Main.immediate).
 */
class MonologueEngine(
    private val scope: CoroutineScope,
    /** 세 글자마다 나는 타자 소리 */
    private val onKeystroke: () -> Unit,
) {
    private class Line(val text: String, val auto: Boolean, var done: CompletableDeferred<Unit>?)

    private val queue = ArrayDeque<Line>()
    private var current: Line? = null
    private var full = false
    private var job: Job? = null

    private val _ui = MutableStateFlow(MonologueUi())
    val ui: StateFlow<MonologueUi> = _ui.asStateFlow()

    fun say(lines: List<String>, queued: Boolean = false, auto: Boolean = false): Deferred<Unit> {
        val finished = CompletableDeferred<Unit>()
        if (lines.isEmpty()) {
            finished.complete(Unit)
            return finished
        }
        if (!queued) {
            queue.forEach { it.done?.complete(Unit) }
            queue.clear()
            current?.let { line ->
                job?.cancel()
                current = null
                line.done?.complete(Unit)
            }
        }
        lines.forEachIndexed { index, text ->
            queue.addLast(Line(text, auto, if (index == lines.lastIndex) finished else null))
        }
        val playing = current
        if (playing == null) {
            next()
        } else if (full) {
            // 다 찍힌 줄이 손님을 기다리는 중이면 잠깐 뒤에 넘긴다.
            job?.cancel()
            job = scope.launch {
                delay(GameTuning.MONO_HANDOFF_MS)
                if (current === playing) {
                    playing.done.also { playing.done = null }?.complete(Unit)
                    next()
                }
            }
        }
        _ui.value = _ui.value.copy(more = queue.isNotEmpty())
        return finished
    }

    /** 독백 바를 눌렀을 때: 찍는 중이면 한 줄을 다 보여 주고, 다 찍혔으면 다음 줄로. */
    fun tap() {
        val line = current ?: return
        job?.cancel()
        if (!full) {
            finish(line)
            return
        }
        if (queue.isNotEmpty()) {
            line.done.also { line.done = null }?.complete(Unit)
            next()
        } else {
            line.done.also { line.done = null }?.complete(Unit)
        }
    }

    fun reset(text: String = "") {
        job?.cancel()
        queue.clear()
        current = null
        full = false
        _ui.value = MonologueUi(text = text)
    }

    private fun next() {
        val line = queue.removeFirstOrNull()
        if (line == null) {
            current = null
            _ui.value = _ui.value.copy(talking = false, more = false)
            return
        }
        current = line
        full = false
        _ui.value = MonologueUi(text = "", talking = true, more = queue.isNotEmpty())
        job = scope.launch {
            var shown = 0
            while (shown < line.text.length) {
                shown++
                _ui.value = _ui.value.copy(text = line.text.substring(0, shown))
                if (shown % 3 == 0) onKeystroke()
                if (shown < line.text.length) delay(GameTuning.MONO_CHAR_MS)
            }
            finish(line)
        }
    }

    private fun finish(line: Line) {
        if (current !== line) return
        full = true
        _ui.value = MonologueUi(text = line.text, talking = false, more = queue.isNotEmpty())
        val hold = (if (line.auto) GameTuning.MONO_HOLD_AUTO_MS else GameTuning.MONO_HOLD_MS) +
            line.text.length * GameTuning.MONO_HOLD_PER_CHAR_MS
        when {
            queue.isNotEmpty() -> job = scope.launch {
                delay(hold)
                if (current === line) {
                    line.done?.complete(Unit)
                    next()
                }
            }

            line.done != null && line.auto -> job = scope.launch {
                delay(hold)
                line.done.also { line.done = null }?.complete(Unit)
            }

            line.done != null -> line.done.also { line.done = null }?.complete(Unit)
        }
    }
}
