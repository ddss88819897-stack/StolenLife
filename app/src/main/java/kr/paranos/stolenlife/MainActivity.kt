package kr.paranos.stolenlife

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import androidx.compose.runtime.CompositionLocalProvider
import kr.paranos.stolenlife.feedback.AmbientSynth
import kr.paranos.stolenlife.feedback.Haptics
import kr.paranos.stolenlife.feedback.ToneSynth
import kr.paranos.stolenlife.ui.StolenLifeApp
import kr.paranos.stolenlife.ui.components.LocalPressSound
import kr.paranos.stolenlife.ui.theme.StolenTheme
import kr.paranos.stolenlife.viewmodel.Bgm
import kr.paranos.stolenlife.viewmodel.Feedback
import kr.paranos.stolenlife.viewmodel.GameViewModel

/**
 * 단일 액티비티. 시스템 상태바 · 내비게이션 바를 숨기는 몰입 모드로 띄우고,
 * ParanOS 98 의 상태바와 하단 바를 화면 전체에 직접 그린다. 시스템 바는 가장자리를 쓸어 올리면 잠깐 나타난다.
 *
 * ViewModel 이 내보내는 소리 · 진동 신호는 화면이 보이는 동안에만 재생한다.
 */
class MainActivity : ComponentActivity() {

    private val game: GameViewModel by viewModels()
    private lateinit var synth: ToneSynth
    private lateinit var ambient: AmbientSynth
    private lateinit var haptics: Haptics

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        enterImmersiveMode()

        synth = ToneSynth()
        ambient = AmbientSynth()
        haptics = Haptics(this)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                game.feedback.collect { signal ->
                    when (signal) {
                        is Feedback.Tone -> synth.play(signal.frequency, signal.seconds, signal.wave, signal.volume, signal.delayMs)
                        Feedback.Modem -> synth.modem()
                        is Feedback.Haptic -> haptics.perform(signal.kind)
                    }
                }
            }
        }
        // 배경음: 화면이 보이는 동안만 깔고, 홈으로 나가면 서서히 끊는다.
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                try {
                    game.bgm.collect(ambient::set)
                } finally {
                    ambient.set(Bgm.NONE)
                }
            }
        }
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) = game.onForegroundChanged(true)
            override fun onStop(owner: LifecycleOwner) = game.onForegroundChanged(false)
        })

        setContent {
            StolenTheme {
                CompositionLocalProvider(LocalPressSound provides game::pressSound) {
                    StolenLifeApp(game)
                }
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        // 알림창 · 다이얼로그에서 돌아오면 시스템 바가 다시 보이므로 매번 숨긴다.
        if (hasFocus) enterImmersiveMode()
    }

    override fun onDestroy() {
        synth.release()
        ambient.release()
        super.onDestroy()
    }

    private fun enterImmersiveMode() {
        WindowCompat.getInsetsController(window, window.decorView).apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.systemBars())
        }
    }
}
