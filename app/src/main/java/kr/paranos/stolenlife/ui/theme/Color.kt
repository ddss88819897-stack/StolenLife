package kr.paranos.stolenlife.ui.theme

import androidx.compose.ui.graphics.Color

/*
 * 냉전 기밀문서 · 법의학 단말기 팔레트.
 * 최상위 토큰 이름은 웹 프로토타입의 CSS :root 변수와 1:1 로 맞췄다.
 * 원색(핫핑크 · 보라 · 형광 초록)은 쓰지 않는다. 강조는 Gold, 경고와 증거는 Rust 하나뿐이다.
 */

val Void = Color(0xFF0B0D0C)        // --void   상태바 · 내비게이션 바
val Slate = Color(0xFF1E2221)       // --slate  앱 본문 바탕
val Khaki = Color(0xFF2C3230)       // --khaki  카드 · 말풍선
val LineGreen = Color(0xFF4A5A50)   // --line   1px 실선 베벨
val LineInk = Color(0xFF3A3834)     // --line2  단서 칩 테두리
val TitleGreen = Color(0xFF14281D)  // --title  기본 타이틀바
val Blood = Color(0xFF4A1515)       // --blood  금고 · 설정 · 경보 타이틀바
val Paper = Color(0xFFE6E1D6)       // --paper  문서 갱지
val Paper2 = Color(0xFFD8D2C5)      // --paper2 단서 칩 라벨
val Ink = Color(0xFF2A2724)         // --ink    문서 글자
val Ink2 = Color(0xFF5C574F)        // --ink2   문서 보조 글자
val Rust = Color(0xFF8C2D2D)        // --rust   만년필 밑줄 · 도장
val Gold = Color(0xFFC9A96E)        // --gold   증거 버퍼 · 강조
val TextMain = Color(0xFFC9CFC9)    // --text
val Mute = Color(0xFF7F8A84)        // --mute
val Phosphor = Color(0xFF9DBFA6)    // --phos   오실로스코프 · 터미널
val Amber = Color(0xFFC49A4A)       // --amber  금고95
val Paran98 = Color(0xFF0000AA)     // 1998 천리안 화면 전용

/** 상태바 · 독백 바 · 내비게이션 · 서랍 같은 "폰 껍데기" */
object ShellTone {
    val Backdrop = Color(0xFF050605)
    val StatusRule = Color(0xFF161A18)
    val MonoBar = Color(0xFF0F1211)
    val MonoRule = Color(0xFF29302C)
    val MonoTag = Color(0xFF121615)
    val MonoText = Color(0xFFD6DAD4)
    val NavRule = Color(0xFF1B201E)
    val NavIdle = Color(0xFF8E9993)
    val NavPressed = Color(0xFF141816)
    val BadgeDot = Color(0xFFB0453C)
    val DrawerSheet = Color(0xFF202523)
    val DrawerHandle = Color(0xFF4F5A54)
    val DrawerRule = Color(0xFF343B37)
    val DrawerTab = Color(0xFF3D4541)
    val DrawerScrim = Color(0xA3050606)     // rgba(5,6,6,.64)
    val OverlayScrim = Color(0xC2040505)    // rgba(4,5,5,.76)
    val CardScrim = Color(0xFF050606)
    val PinSurface = Color(0xFF241212)
    val PinRule = Color(0xFF5A4630)
    val PinSub = Color(0xFFA58E6A)
    val Popup = Color(0xFF181C1B)
    val PopupRule = Color(0xFF2A302D)
    val BevelShadow = Color(0xFF000000)
}

/** 토스트 알림 */
object ToastTone {
    val Surface = Color(0xF7141816)         // rgba(20,24,22,.97)
    val Border = Color(0xFF3D4541)
    val SpamSurface = Color(0xF7221111)
    val SpamText = Color(0xFFD6C6B5)
    val SpamTag = Color(0xFFB9584C)
    val Message = Color(0xFF5E8C6A)
}

/** 윈도우95풍 대화상자 · 키패드 · 교차검증 */
object WindowTone {
    val Button = Color(0xFF252B29)
    val ButtonPressed = Color(0xFF323A37)
    val ButtonDisabledText = Color(0xFF5D6662)
    val ButtonDisabledBorder = Color(0xFF333A37)
    val HintText = Color(0xFFCDBF9F)
    val HintSurface = Color(0xFF191D1C)
    val Display = Color(0xFF0A0D0B)
    val DisplayRule = Color(0xFF2F3733)
    val DisplayPlaceholder = Color(0xFF33413A)
    val Key = Color(0xFF242A28)
    val KeyPressed = Color(0xFF3A423E)
    val CrossHeading = Color(0xFFDBA595)
    val CrossConclusion = Color(0xFF0C0E0D)
    val ChapterHint = Color(0xFF56605A)
}

/** 홈 런처와 일반 앱 화면 */
object AppTone {
    val ClockWidget = Color(0xDB0D100F)     // rgba(13,16,15,.86)
    val TickerRule = Color(0xFF333B37)
    val IconLabel = Color(0xFFB5BEB8)
    val IconWell = Color(0xFF0F1312)
    val BuildLabel = Color(0xFF4F5953)
    val BarCode = Color(0xFF6F8A78)
    val BarBackRule = Color(0xFF2F4A39)
    val AlertBarBackRule = Color(0xFF6A2A2A)
    val AlertBarCode = Color(0xFFA87A6E)
    val SearchField = Color(0xFF131716)
    val Card = Color(0xFF242A28)
    val CardRule = Color(0xFF333B37)
    val CardPressed = Color(0xFF2C3431)
    val CardIdleAccent = Color(0xFF3D4541)
    val SubGreen = Color(0xFF7C9A86)
    val NewBadge = Color(0xFFD08A7C)
    val TipText = Color(0xFFA7B0AA)
    val TipSurface = Color(0xFF1A1E1D)
    val PostText = Color(0xFFCFD4CF)
    val CommentRule = Color(0xFF3B4440)
    val CommentText = Color(0xFFA9B1AC)
    val MemoryRule = Color(0xFF5A4A30)
    val MemoryBannerRule = Color(0xFF4A3D28)
    val InstaBody = Color(0xFF151817)
    val FacebookBody = Color(0xFF1A1F21)
    val GroupRule = Color(0xFF3A423E)
    val GroupText = Color(0xFFC3CAC5)
    val HoldSurface = Color(0xFF2A1010)
    val HoldText = Color(0xFFE8C9BF)
    val HoldFill = Color(0x73C0504A)        // rgba(192,80,74,.45)
}

/** 보안 메신저 */
object ChatTone {
    val Body = Color(0xFF131715)
    val Bubble = Color(0xFF222926)
    val BubbleRule = Color(0xFF33403A)
    val BubbleText = Color(0xFFD3D9D4)
    val Time = Color(0xFF6C7A72)
    val AlertBubble = Color(0xFF2A1010)
    val AlertText = Color(0xFFE2A597)
    val AlertEdge = Color(0xFFC0504A)
    val LogBubble = Color(0xFF0A0D0B)
    val Footer = Color(0xFF4F5953)
}

/** 1998 천리안 터미널 */
object ParanTone {
    val Bar = Color(0xFF0A0D2A)
    val Text = Color(0xFFE8E8F2)
    val Yellow = Color(0xFFE8D27A)
    val Cyan = Color(0xFF8FD3D3)
    val Pressed = Color(0xFF2424C4)
    val MetaRule = Color(0xFF3434C8)
    val Vignette = Color(0xB300001E)
}

/** 금고95 */
object VaultTone {
    val Body = Color(0xFF0C0A07)
    val BoxRule = Color(0xFF5A4422)
    val Title = Color(0xFFD8B46A)
    val Small = Color(0xFF8A6C38)
    val ButtonRule = Color(0xFF6B5228)
    val ButtonPressed = Color(0xFF1D160C)
    val OtpDigits = Color(0xFFD26A5E)
    val OtpRule = Color(0xFF3A1A1A)
    val Document = Color(0xFF110E09)
    val DocumentRule = Color(0xFF3B2E18)
}

/** 파란여관 203호 */
object RoomTone {
    val Body = Color(0xFF080908)
    val Marker = Color(0xFFECE2C8)
    val MarkerName = Color(0xFFD8CDB2)
    val ListRule = Color(0xFF1B201E)
    val ListPressed = Color(0xFF121615)
    val ListText = Color(0xFFCFD4CF)
    val ListMark = Color(0xFF8A958F)
    val DoneText = Color(0xFF6C7771)
    val DoneMark = Color(0xFF4C5651)
    val SirenRed = Color(0x61961E1E)        // rgba(150,30,30,.38)
    val SirenBlue = Color(0x611E3C96)
}

/** 문서(종이) 스타일별 바탕과 잉크 */
object PaperTone {
    val Grain = Color(0x125A503C)           // rgba(90,80,60,.07) 3px 점묘
    val Rule = Color(0x522A2724)            // rgba(42,39,36,.32)
    val TableRule = Color(0x6B2A2724)
    val TableLabel = Color(0x122A2724)
    val Wiki = Color(0xFFEAE6DC)
    val Blog = Color(0xFFEDE8DD)
    val Binder = Color(0xFFCDBF9E)
    val BinderGrain = Color(0x1C3C280A)
    val BinderTabText = Color(0xFF5C4A2A)
    val Cy = Color(0xFFF3EFE6)
    val CyFrame = Color(0xFFB8743A)
    val CyMeta = Color(0xFF9A5A28)
    val Spam = Color(0xFF151313)
    val SpamText = Color(0xFFD6C6A8)
    val SpamFrame = Color(0xFF6B5A3A)
    val SpamMeta = Color(0xFF8A7A5A)
    val SpamRule = Color(0xFF3A3226)
    val DiaryLine = Color(0x29465070)       // rgba(70,80,110,.16)
    val DiaryMargin = Color(0x738C2D2D)
    val LogStripe = Color(0x1C6E8C6E)       // rgba(110,140,110,.11)
    val LogPunch = Slate
    val Stamp = Color(0xD98C2D2D)           // rgba(140,45,45,.85)
    val StampOnDark = Color(0xD9D26A5E)
    val Photo = Color(0xFFE3DDCF)           // 1-bit 사진 전경
    val PhotoGround = Color(0xFF26231F)     // 1-bit 사진 배경
}

/** 문서 속 인라인 토큰: 칩 밑줄 · 핀 문장 · 삭제선 */
object InkTone {
    val ChipPlus = Rust
    val ChipPlusOnDark = Color(0xFFE09080)
    val ChipHeldWash = Color(0x142A2724)
    val ChipHeldWashOnDark = Color(0x12FFFFFF)
    val PinWash = Color(0x1F8C2D2D)         // rgba(140,45,45,.12)
    val PinWashPinned = Color(0x428C2D2D)   // rgba(140,45,45,.26)
    val PinRuleOnDark = Color(0xFFC46A5E)
    val PinWashOnDark = Color(0x26C46A5E)
    val Redaction = Color(0xFF141210)
    val RedactionOnDark = Color(0xFF3A3836)
    val RedactionLabel = Color(0xFFB9584C)
    val Revealed = Color(0xFF9E2A2A)
    val RevealedWash = Color(0x149E2A2A)
    val RevealedOnDark = Color(0xFFE0796A)
}

/** 경보 · 프록시 해제 · 엔딩 */
object AlertTone {
    val Text = Color(0xFFD26A5E)
    val Edge = Color(0xFFC0504A)
    val LighthouseWrap = Color(0xFF060707)
    val RaidShout = Color(0xFFE8DFC8)
    val RaidDrop = Color(0xFF0C0F0E)
    val RaidDropRule = Color(0xFF2A302D)
    val FinRule = Color(0xFF3A1A1A)
    val FinSub = Color(0xFFBFB7A6)
    val Torch = Color(0xD9FFFAE6)
    val ChromaRed = Color(0x8CA02828)
    val ChromaTeal = Color(0x733C786E)
}

/** 부팅 화면 */
object BootTone {
    val Surface = Color(0xFF050606)
    val Text = Color(0xFF6F8A78)
    val Foot = Color(0xFF4F5953)
}

/** 오실로스코프 · CRT */
object ScopeTone {
    val Grid = Color(0x219DBFA6)            // rgba(157,191,166,.13)
    val Calm = Phosphor
    val Exposed = Color(0xFFD26A5E)         // 프록시 해제 후 파형
    val Scanline = Color(0x21000000)
    val Vignette = Color(0x80000000)
}

/** 홈 화면 아이콘(64px 1-bit) */
object IconTone {
    val Ink = Color(0xFFC9D3C9)
    val Ground = Color(0xFF0F1312)
}
