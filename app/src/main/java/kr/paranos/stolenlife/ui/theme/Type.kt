package kr.paranos.stolenlife.ui.theme

import android.content.res.AssetManager
import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import kr.paranos.stolenlife.R

/*
 * ─────────────────────────────────────────────────────────────────────────────
 *  서체 적용 가이드
 * ─────────────────────────────────────────────────────────────────────────────
 *  역할        | 웹 프로토타입                     | 앱
 *  ------------+-----------------------------------+--------------------------------------
 *  문서 명조   | Nanum Myeongjo                    | res/font/nanum_myeongjo_*.otf (동봉)
 *  UI 모노     | IBM Plex Mono + Nanum Gothic Coding| assets/fonts/NanumGothicCoding-*.ttf
 *  픽셀 포인트 | DungGeunMo                        | assets/fonts/DungGeunMo.ttf
 *
 *  1) 나눔명조(Regular · ExtraBold)는 프로젝트에 이미 들어 있다. 공문서 · 기사 · 일기 · 챕터 제목.
 *
 *  2) 둥근모꼴(DungGeunMo)은 TTF 를 받아 아래 경로에 그대로 넣으면 다음 실행부터 적용된다.
 *        app/src/main/assets/fonts/DungGeunMo.ttf
 *     시계 · 키패드 · 1998 터미널 · FIN · 내비게이션 글리프가 이 서체를 쓴다.
 *     배포 전에 라이선스 문서(OFL)를 함께 넣을 것.
 *
 *  3) 나눔고딕코딩은 Google Fonts 에서 받아 아래 두 파일로 넣는다.
 *        app/src/main/assets/fonts/NanumGothicCoding-Regular.ttf
 *        app/src/main/assets/fonts/NanumGothicCoding-Bold.ttf
 *
 *  웹처럼 Plex Mono(라틴) + 나눔고딕코딩(한글)을 섞지 않는다. Compose FontFamily 는 글자 단위로
 *  다른 커스텀 패밀리에 폴백하지 않으므로, 한글과 라틴을 모두 가진 나눔고딕코딩 하나로 통일한다.
 *
 *  2)·3)이 없으면 시스템 고정폭(FontFamily.Monospace)으로 대체되고, 앱은 정상 동작한다.
 *  res/font 대신 assets 를 쓰는 이유가 이것이다. 파일이 없어도 컴파일이 깨지지 않는다.
 * ─────────────────────────────────────────────────────────────────────────────
 */

/** 동봉된 나눔명조. W700 요청은 ExtraBold 로, W400 이하는 Regular 로 매칭된다. */
val DocSerif = FontFamily(
    Font(R.font.nanum_myeongjo_regular, FontWeight.Normal),
    Font(R.font.nanum_myeongjo_extrabold, FontWeight.ExtraBold),
)

/** 선택 서체의 assets 경로. 파일명은 대소문자까지 정확해야 한다. */
object OptionalFontFiles {
    const val DIRECTORY = "fonts"
    const val UI_REGULAR = "NanumGothicCoding-Regular.ttf"
    const val UI_BOLD = "NanumGothicCoding-Bold.ttf"
    const val PIXEL = "DungGeunMo.ttf"
}

@Immutable
class StolenFonts(
    val ui: FontFamily,
    val doc: FontFamily,
    val pixel: FontFamily,
    /** 둥근모꼴이 실제로 들어 있는지. 없으면 픽셀 텍스트의 자간을 조금 줄인다. */
    val hasPixelFace: Boolean,
)

/** assets/fonts 를 한 번 훑어 있는 서체만 골라 패밀리를 만든다. 테마 생성 시 한 번 호출. */
fun loadStolenFonts(assets: AssetManager): StolenFonts {
    fun listing(dir: String): Set<String> = runCatching { assets.list(dir)?.toSet() }.getOrNull().orEmpty()
    val inFontsDir = listing(OptionalFontFiles.DIRECTORY)
    // assets 바로 아래에 넣어도 찾는다(fonts 폴더를 빠뜨리기 쉽다).
    val inRoot = listing("")

    fun asset(file: String, weight: FontWeight): Font? = when (file) {
        in inFontsDir -> Font("${OptionalFontFiles.DIRECTORY}/$file", assets, weight)
        in inRoot -> Font(file, assets, weight)
        else -> null
    }

    val uiFaces = listOfNotNull(
        asset(OptionalFontFiles.UI_REGULAR, FontWeight.Normal),
        asset(OptionalFontFiles.UI_BOLD, FontWeight.Bold),
    )
    val pixelFace = asset(OptionalFontFiles.PIXEL, FontWeight.Normal)

    return StolenFonts(
        ui = if (uiFaces.isEmpty()) FontFamily.Monospace else FontFamily(uiFaces),
        doc = DocSerif,
        pixel = pixelFace?.let { FontFamily(it) } ?: FontFamily.Monospace,
        hasPixelFace = pixelFace != null,
    )
}

/**
 * 화면 역할별 글자 스타일. Material 의 bodyMedium 같은 이름 대신 쓰임새로 부른다.
 * 기준 크기 13sp 는 웹판 clamp(11px, 3.6cqw, 17px)를 360dp 폭 폰에 대입한 값이다.
 */
@Immutable
data class StolenTypography(
    /** 바 · 버튼 · 메신저 본문 */
    val ui: TextStyle,
    /** 검색 결과 출처 · 보조 설명 */
    val uiSmall: TextStyle,
    /** [PER] 같은 말머리 · 상태바 · 관리번호. 자간을 넓혀 관공서 서식 느낌을 낸다. */
    val tag: TextStyle,
    /** 문서 본문 (명조) */
    val docBody: TextStyle,
    /** 문서 제목 (명조 ExtraBold) */
    val docTitle: TextStyle,
    /** 문서 상단 출처 줄 · 표 왼쪽 칸 */
    val docMeta: TextStyle,
    /** 챕터 카드 · 부팅 타이틀 */
    val chapterTitle: TextStyle,
    /** 독백 바 */
    val monologue: TextStyle,
    /** 홈 시계 */
    val clock: TextStyle,
    /** 키패드 숫자 · 내비게이션 글리프 */
    val keypad: TextStyle,
    /** 키패드 표시창 */
    val keypadDisplay: TextStyle,
    /** 1998 터미널 · 부팅 BIOS */
    val terminal: TextStyle,
    /** FIN */
    val fin: TextStyle,
)

fun stolenTypography(fonts: StolenFonts): StolenTypography {
    val pixelTracking = if (fonts.hasPixelFace) 0.04.em else 0.em
    return StolenTypography(
        ui = TextStyle(fontFamily = fonts.ui, fontSize = 13.sp, lineHeight = 19.5.sp),
        uiSmall = TextStyle(fontFamily = fonts.ui, fontSize = 10.5.sp, lineHeight = 15.sp),
        tag = TextStyle(fontFamily = fonts.ui, fontSize = 9.5.sp, lineHeight = 12.sp, letterSpacing = 0.12.em),
        docBody = TextStyle(fontFamily = fonts.doc, fontSize = 13.sp, lineHeight = 22.sp),
        docTitle = TextStyle(
            fontFamily = fonts.doc,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 14.sp,
            lineHeight = 20.sp,
        ),
        docMeta = TextStyle(fontFamily = fonts.ui, fontSize = 9.sp, lineHeight = 13.sp, letterSpacing = 0.02.em),
        chapterTitle = TextStyle(
            fontFamily = fonts.doc,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 25.sp,
            lineHeight = 32.sp,
        ),
        monologue = TextStyle(fontFamily = fonts.ui, fontSize = 11.5.sp, lineHeight = 17.5.sp),
        clock = TextStyle(fontFamily = fonts.pixel, fontSize = 35.sp, lineHeight = 35.sp, letterSpacing = pixelTracking),
        keypad = TextStyle(fontFamily = fonts.pixel, fontSize = 15.sp, lineHeight = 18.sp),
        keypadDisplay = TextStyle(fontFamily = fonts.pixel, fontSize = 19.sp, lineHeight = 24.sp, letterSpacing = 0.24.em),
        terminal = TextStyle(fontFamily = fonts.pixel, fontSize = 11.sp, lineHeight = 18.sp, letterSpacing = pixelTracking),
        fin = TextStyle(fontFamily = fonts.pixel, fontSize = 36.sp, lineHeight = 40.sp, letterSpacing = 0.5.em),
    )
}

/** ModalBottomSheet 같은 Material 컴포넌트가 기본값으로 집어 가는 슬롯만 최소 매핑. */
fun StolenTypography.asMaterial(): Typography = Typography(
    bodyLarge = ui,
    bodyMedium = ui,
    bodySmall = uiSmall,
    labelLarge = ui,
    labelMedium = uiSmall,
    labelSmall = tag,
    titleMedium = docTitle,
    titleLarge = chapterTitle,
    displaySmall = clock,
)
