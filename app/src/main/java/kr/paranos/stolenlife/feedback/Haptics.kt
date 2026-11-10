package kr.paranos.stolenlife.feedback

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kr.paranos.stolenlife.viewmodel.HapticKind

/**
 * 진동 3종.
 *  - TICK         칩 획득. 짧고 마른 틱.
 *  - DOUBLE_BUZZ  비밀번호 오답 · 경찰 진입. 두 번 끊어서.
 *  - FLOOD_PULSE  엔딩 메신저 폭주 동안 0.3초마다 한 번(ViewModel 이 박자를 센다).
 */
class Haptics(context: Context) {

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        context.getSystemService(VibratorManager::class.java)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    fun perform(kind: HapticKind) {
        val motor = vibrator?.takeIf { it.hasVibrator() } ?: return
        val effect = when (kind) {
            HapticKind.TICK ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)
                } else {
                    VibrationEffect.createOneShot(12, amplitude(motor, 120))
                }

            HapticKind.DOUBLE_BUZZ -> VibrationEffect.createWaveform(
                longArrayOf(0, 70, 80, 70),
                intArrayOf(0, amplitude(motor, 255), 0, amplitude(motor, 255)),
                -1,
            )

            HapticKind.FLOOD_PULSE -> VibrationEffect.createOneShot(45, amplitude(motor, 160))
        }
        motor.vibrate(effect)
    }

    private fun amplitude(motor: Vibrator, value: Int): Int =
        if (motor.hasAmplitudeControl()) value else VibrationEffect.DEFAULT_AMPLITUDE
}
