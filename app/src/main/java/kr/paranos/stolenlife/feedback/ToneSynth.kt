package kr.paranos.stolenlife.feedback

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kr.paranos.stolenlife.viewmodel.Wave
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.tanh
import kotlin.random.Random

/**
 * 웹판 snd() 의 안드로이드판. WebAudio 오실레이터 + 지수 감쇠 게인을 AudioTrack 스트림 하나로 흉내 낸다.
 *
 * 소리 파일을 하나도 넣지 않는다. 사각파 · 삼각파 · 톱니파를 직접 합성해서
 * 1998년 PC 스피커 같은 삑 소리를 낸다. 소리가 없을 때는 트랙을 멈추고 스레드도 잠든다.
 */
class ToneSynth {

    private class Voice(
        val frequency: Float,
        val wave: Wave,
        val volume: Float,
        val start: Long,
        val decaySamples: Long,
        val end: Long,
    ) {
        var phase = 0.0
    }

    private val lock = Object()
    private val voices = ArrayList<Voice>()
    private var clock = 0L
    private var running = true
    private val track: AudioTrack = AudioTrack.Builder()
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
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
            maxOf(AudioTrack.getMinBufferSize(RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT), CHUNK * 4),
        )
        .setTransferMode(AudioTrack.MODE_STREAM)
        .build()

    private val worker = Thread(::loop, "stolenlife-tone").apply {
        isDaemon = true
        start()
    }

    /** snd(f, d, type, v). [delayMs] 는 웹판 setTimeout(() => snd(...), n) 자리. */
    fun play(frequency: Float, seconds: Float, wave: Wave, volume: Float, delayMs: Long = 0) {
        synchronized(lock) {
            val start = clock + delayMs * RATE / 1000
            val decay = (seconds * RATE).toLong().coerceAtLeast(1)
            voices += Voice(frequency, wave, volume, start, decay, start + decay + RATE / 50)
            lock.notifyAll()
        }
    }

    /** 28.8k 모뎀 다이얼: 85ms 간격 톱니파 16번, 주파수는 매번 무작위 */
    fun modem() {
        repeat(16) { i -> play(500f + Random.nextFloat() * 2000f, 0.1f, Wave.SAWTOOTH, 0.025f, i * 85L) }
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
                while (running && voices.isEmpty()) {
                    if (playing) {
                        runCatching { track.pause(); track.flush() }
                        playing = false
                    }
                    lock.wait()
                }
                if (!running) return
                render(buffer)
            }
            if (!playing) {
                runCatching { track.play() }
                playing = true
            }
            track.write(buffer, 0, buffer.size)
        }
    }

    /** lock 안에서만 부른다. */
    private fun render(buffer: ShortArray) {
        for (i in buffer.indices) {
            val now = clock + i
            var mix = 0.0
            for (voice in voices) {
                if (now < voice.start || now >= voice.end) continue
                val t = (now - voice.start).toDouble()
                // gain: v → 0.0001 지수 감쇠 (exponentialRampToValueAtTime)
                val gain = voice.volume * (FLOOR / voice.volume).toDouble().pow(minOf(1.0, t / voice.decaySamples))
                voice.phase = (voice.phase + voice.frequency / RATE) % 1.0
                mix += oscillator(voice.wave, voice.phase) * gain
            }
            // 부드러운 리미터(tanh): 크게 키워도 겹친 소리가 찢어지지 않는다.
            buffer[i] = (tanh(mix * HEADROOM) * Short.MAX_VALUE).toInt().toShort()
        }
        clock += buffer.size
        voices.removeAll { it.end <= clock }
    }

    private fun oscillator(wave: Wave, phase: Double): Double = when (wave) {
        Wave.SQUARE -> if (phase < 0.5) 1.0 else -1.0
        Wave.SAWTOOTH -> 2.0 * phase - 1.0
        Wave.TRIANGLE -> 1.0 - 4.0 * abs(phase - 0.5)
    }

    private companion object {
        const val RATE = 22_050
        const val CHUNK = 256
        const val FLOOR = 0.0001f

        /** WebAudio 0.035 는 폰 스피커에서 거의 안 들린다. 5배로 키우고 tanh 로 눌러 담는다. */
        const val HEADROOM = 5.0
    }
}
