package kr.paranos.stolenlife.feedback

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kr.paranos.stolenlife.viewmodel.Bgm
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tanh
import kotlin.random.Random

/**
 * 배경음 합성기. 소리 파일 없이 챕터 분위기를 실시간으로 만든다.
 *
 * 한 장면은 다섯 겹으로 이뤄진다.
 *  - 패드: 낮은 화음. 음마다 아주 조금 어긋난 쌍을 겹쳐 느리게 맥놀이(울렁임)가 생긴다.
 *  - 잡음: 저역만 남긴 백색 잡음을 느리게 부풀렸다 줄여 파도 · 바람 · 냉각기 소리를 낸다.
 *  - 점음: 몇 초에 한 번 떨어지는 오르골 · 물방울 소리. 음은 매번 무작위.
 *  - 박: 벽시계 째깍 · 심장 박동.
 *  - 사이렌: 3장에서만, 아주 멀리.
 *
 * 장면이 바뀌면 1초쯤 걸려 사라졌다가 새 장면이 천천히 올라온다.
 * [Bgm.NONE] 이면 소리가 다 빠진 뒤 트랙을 멈추고 스레드도 잠든다.
 */
class AmbientSynth {

    private class Mood(
        val pad: FloatArray,
        val padLevel: Float,
        /** 쌍으로 겹친 음 사이의 차이(Hz). 클수록 빨리 울렁인다. */
        val beat: Float,
        val noiseLevel: Float,
        val noiseCutoff: Float,
        /** 잡음이 한 번 부풀었다 가라앉는 데 걸리는 초 */
        val swell: Float,
        val hum: Float = 0f,
        val humLevel: Float = 0f,
        val notes: FloatArray = FloatArray(0),
        val noteGap: ClosedFloatingPointRange<Float> = 3f..6f,
        val noteLevel: Float = 0f,
        val noteDecay: Float = 1f,
        /** 늘어진 테이프처럼 음 높이가 흔들리는 정도 */
        val wow: Float = 0f,
        val tickEvery: Float = 0f,
        val tickLevel: Float = 0f,
        val heartEvery: Float = 0f,
        val heartLevel: Float = 0f,
        val siren: Float = 0f,
    )

    private class Note(val frequency: Float, val start: Long, val level: Float, val decay: Float) {
        var phase = 0.0
    }

    private val lock = Object()
    private var requested = Bgm.NONE
    private var running = true

    private var current: Mood? = null
    private var currentId = Bgm.NONE
    private var gain = 0.0
    private var target = 0.0

    private var clock = 0L
    private val padPhases = DoubleArray(16)
    private var lowpass = 0.0
    private var sirenPhase = 0.0
    private var humPhase = 0.0
    private val notes = ArrayList<Note>()
    private var nextNote = 0L

    private val track: AudioTrack = AudioTrack.Builder()
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build(),
        )
        .setAudioFormat(
            AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(RATE)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build(),
        )
        .setBufferSizeInBytes(
            maxOf(AudioTrack.getMinBufferSize(RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT), CHUNK * 8),
        )
        .setTransferMode(AudioTrack.MODE_STREAM)
        .build()

    private val worker = Thread(::loop, "stolenlife-ambient").apply {
        isDaemon = true
        start()
    }

    fun set(bgm: Bgm) {
        synchronized(lock) {
            requested = bgm
            lock.notifyAll()
        }
    }

    fun release() {
        synchronized(lock) {
            running = false
            lock.notifyAll()
        }
        worker.join(200)
        runCatching { track.stop() }
        track.release()
    }

    private fun loop() {
        val buffer = ShortArray(CHUNK)
        var playing = false
        while (true) {
            synchronized(lock) {
                while (running && requested == Bgm.NONE && current == null) {
                    if (playing) {
                        runCatching { track.pause(); track.flush() }
                        playing = false
                    }
                    lock.wait()
                }
                if (!running) return
                steer()
            }
            render(buffer)
            if (!playing) {
                runCatching { track.play() }
                playing = true
            }
            track.write(buffer, 0, buffer.size)
        }
    }

    /** 요청과 지금 장면이 다르면 먼저 소리를 내리고, 다 내려간 뒤에 갈아 끼운다. lock 안에서 부른다. */
    private fun steer() {
        if (requested == currentId) {
            target = if (current != null) 1.0 else 0.0
            return
        }
        target = 0.0
        if (gain < 0.002 || current == null) {
            currentId = requested
            current = moodOf(requested)
            notes.clear()
            nextNote = clock + RATE * 2L
            gain = 0.0
            target = if (current != null) 1.0 else 0.0
        }
    }

    private fun render(buffer: ShortArray) {
        val mood = current
        if (mood == null) {
            buffer.fill(0)
            clock += buffer.size
            return
        }
        val noiseCoef = 1.0 - exp(-2.0 * PI * mood.noiseCutoff / RATE)
        val noiseNorm = 1.0 / sqrt(noiseCoef)
        // 올라올 때는 천천히(약 2.5초), 사라질 때는 빨리(약 0.6초)
        val fade = if (target > gain) 1.0 / (RATE * 2.5) else 1.0 / (RATE * 0.6)

        for (i in buffer.indices) {
            val n = clock + i
            val sec = n.toDouble() / RATE
            gain += (target - gain) * fade * 4.0
            val wow = 1.0 + mood.wow * sin(TAU * 0.31 * sec)

            // ── 패드
            var pad = 0.0
            for (k in mood.pad.indices) {
                val swellK = 0.6 + 0.4 * sin(TAU * (0.045 + 0.017 * k) * sec + k)
                for (side in 0..1) {
                    val index = k * 2 + side
                    val f = mood.pad[k] * wow + if (side == 0) -mood.beat / 2 else mood.beat / 2
                    padPhases[index] = (padPhases[index] + f / RATE) % 1.0
                    pad += sin(TAU * padPhases[index]) * swellK
                }
            }
            var mix = pad * mood.padLevel / (mood.pad.size * 2)

            // ── 웅웅거림(냉각기)
            if (mood.humLevel > 0f) {
                humPhase = (humPhase + mood.hum / RATE) % 1.0
                mix += (sin(TAU * humPhase) + 0.35 * sin(TAU * 2 * humPhase) + 0.15 * sin(TAU * 3 * humPhase)) * mood.humLevel
            }

            // ── 잡음(파도 · 바람 · 테이프)
            val white = Random.nextDouble() * 2 - 1
            lowpass += noiseCoef * (white - lowpass)
            val swell = 0.5 + 0.5 * sin(TAU * sec / mood.swell)
            mix += lowpass * noiseNorm * mood.noiseLevel * (0.25 + 0.75 * swell * swell)

            // ── 점음(오르골 · 물방울)
            if (mood.notes.isNotEmpty() && n >= nextNote) {
                notes += Note(
                    frequency = mood.notes[Random.nextInt(mood.notes.size)],
                    start = n,
                    level = mood.noteLevel * (0.55f + 0.45f * Random.nextFloat()),
                    decay = mood.noteDecay,
                )
                val gap = mood.noteGap.start + Random.nextFloat() * (mood.noteGap.endInclusive - mood.noteGap.start)
                nextNote = n + (gap * RATE).toLong()
            }
            if (notes.isNotEmpty()) {
                val it = notes.iterator()
                while (it.hasNext()) {
                    val note = it.next()
                    val age = (n - note.start).toDouble()
                    val env = exp(-age / (note.decay * RATE)) * minOf(1.0, age / (0.004 * RATE))
                    if (age > note.decay * RATE * 7) {
                        it.remove()
                        continue
                    }
                    note.phase = (note.phase + note.frequency / RATE) % 1.0
                    // 기본음 + 옅은 배음 = 오르골 같은 맑은 소리
                    mix += (sin(TAU * note.phase) + 0.25 * sin(TAU * 3 * note.phase)) * env * note.level
                }
            }

            // ── 벽시계
            if (mood.tickEvery > 0f) {
                val period = (mood.tickEvery * RATE).toLong()
                val age = n % period
                if (age < RATE / 50) mix += white * exp(-age / (0.0015 * RATE)) * mood.tickLevel * if ((n / period) % 2 == 0L) 1.0 else 0.7
            }

            // ── 심장 박동: 쿵-쿵, 쉼
            if (mood.heartEvery > 0f) {
                val period = (mood.heartEvery * RATE).toLong()
                val age = (n % period).toDouble()
                val second = age - 0.27 * RATE
                var beat = sin(TAU * 52 * age / RATE) * exp(-age / (0.08 * RATE))
                if (second > 0) beat += 0.7 * sin(TAU * 48 * second / RATE) * exp(-second / (0.09 * RATE))
                mix += beat * mood.heartLevel
            }

            // ── 먼 사이렌
            if (mood.siren > 0f) {
                val f = 690.0 + 170.0 * sin(TAU * sec / 3.4)
                sirenPhase = (sirenPhase + f / RATE) % 1.0
                val ebb = 0.5 + 0.5 * sin(TAU * sec / 23.0)
                mix += sin(TAU * sirenPhase) * mood.siren * ebb * ebb
            }

            buffer[i] = (tanh(mix * gain * MASTER) * Short.MAX_VALUE).toInt().toShort()
        }
        clock += buffer.size
    }

    private fun moodOf(bgm: Bgm): Mood? = when (bgm) {
        Bgm.NONE -> null

        // A단조. 먼바다 파도 위로 높은 오르골이 드문드문.
        Bgm.TITLE -> Mood(
            pad = floatArrayOf(110.00f, 164.81f, 261.63f),
            padLevel = 0.11f,
            beat = 0.35f,
            noiseLevel = 0.05f,
            noiseCutoff = 520f,
            swell = 8.5f,
            notes = floatArrayOf(440.00f, 523.25f, 587.33f, 659.25f, 783.99f, 880.00f),
            noteGap = 2.5f..6f,
            noteLevel = 0.045f,
            noteDecay = 0.9f,
            wow = 0.002f,
        )

        // D 공허 5도. 냉각기 60Hz 웅웅거림, 천장에서 떨어지는 짧은 물방울.
        Bgm.WAREHOUSE -> Mood(
            pad = floatArrayOf(73.42f, 110.00f, 146.83f),
            padLevel = 0.09f,
            beat = 0.25f,
            noiseLevel = 0.035f,
            noiseCutoff = 280f,
            swell = 12f,
            hum = 60f,
            humLevel = 0.012f,
            notes = floatArrayOf(1174.66f, 1318.51f, 1567.98f, 1760.00f),
            noteGap = 3f..8f,
            noteLevel = 0.03f,
            noteDecay = 0.16f,
        )

        // F단조. 늘어진 카세트 테이프, 낡은 오르골, 벽시계.
        Bgm.INN -> Mood(
            pad = floatArrayOf(87.31f, 130.81f, 207.65f, 261.63f),
            padLevel = 0.09f,
            beat = 0.5f,
            noiseLevel = 0.012f,
            noiseCutoff = 2600f,
            swell = 20f,
            notes = floatArrayOf(698.46f, 830.61f, 932.33f, 1046.50f, 1244.51f, 1396.91f),
            noteGap = 1.6f..4f,
            noteLevel = 0.035f,
            noteDecay = 1.2f,
            wow = 0.006f,
            tickEvery = 1f,
            tickLevel = 0.03f,
        )

        // C 와 F#(삼전음). 심장 박동이 깔리고, 멀리서 사이렌이 밀려왔다 빠진다.
        Bgm.SIEGE -> Mood(
            pad = floatArrayOf(65.41f, 92.50f, 130.81f),
            padLevel = 0.10f,
            beat = 0.9f,
            noiseLevel = 0.045f,
            noiseCutoff = 240f,
            swell = 5f,
            notes = floatArrayOf(261.63f, 277.18f, 369.99f),
            noteGap = 5f..10f,
            noteLevel = 0.025f,
            noteDecay = 1.5f,
            heartEvery = 0.95f,
            heartLevel = 0.16f,
            siren = 0.012f,
        )
    }

    private companion object {
        const val RATE = 22_050
        const val CHUNK = 512
        const val TAU = 2.0 * PI

        /** 효과음보다 한 발 뒤에 깔리도록 */
        const val MASTER = 1.6
    }
}
