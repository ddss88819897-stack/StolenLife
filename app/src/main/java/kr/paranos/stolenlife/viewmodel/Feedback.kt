package kr.paranos.stolenlife.viewmodel

/**
 * ViewModel 이 화면 밖(스피커 · 진동 모터)으로 내보내는 신호.
 * 소리는 웹판 snd(주파수, 길이, 파형, 음량)를 그대로 옮긴 값이다.
 */
sealed interface Feedback {
    data class Tone(
        val frequency: Float,
        val seconds: Float = 0.05f,
        val wave: Wave = Wave.SQUARE,
        val volume: Float = 0.035f,
        val delayMs: Long = 0,
    ) : Feedback

    /** 28.8k 모뎀 다이얼 (톱니파 16번) */
    data object Modem : Feedback

    data class Haptic(val kind: HapticKind) : Feedback
}

enum class Wave { SQUARE, TRIANGLE, SAWTOOTH }

enum class HapticKind {
    /** 칩 획득: 짧은 틱 */
    TICK,

    /** 오답: 두 번 끊어 울림 */
    DOUBLE_BUZZ,

    /** 3장 엔딩 메신저 폭주: 0.3초마다 한 번씩 */
    FLOOD_PULSE,
}

/**
 * 배경음. 소리 파일 없이 [kr.paranos.stolenlife.feedback.AmbientSynth] 가 실시간으로 합성한다.
 * 엔딩부터는 일부러 끊는다(기획서: "효과음이 끊긴다").
 */
enum class Bgm {
    NONE,

    /** 부팅 화면: 먼바다 파도 + 높은 오르골 */
    TITLE,

    /** 1장 냉동창고: 냉각기 웅웅거림 + 물방울 */
    WAREHOUSE,

    /** 2장 파란여관: 늘어진 테이프 + 오르골 + 벽시계 */
    INN,

    /** 3장 포위망: 심장 박동 + 먼 사이렌 */
    SIEGE,
}
