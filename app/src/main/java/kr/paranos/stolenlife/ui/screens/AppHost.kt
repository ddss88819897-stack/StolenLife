package kr.paranos.stolenlife.ui.screens

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import kr.paranos.stolenlife.R
import kr.paranos.stolenlife.data.AppId
import kr.paranos.stolenlife.data.GameRepository
import kr.paranos.stolenlife.data.GameState
import kr.paranos.stolenlife.ui.components.DocumentView
import kr.paranos.stolenlife.ui.components.InkState
import kr.paranos.stolenlife.ui.components.ProfileView
import kr.paranos.stolenlife.ui.components.Side
import kr.paranos.stolenlife.ui.components.edge
import kr.paranos.stolenlife.ui.components.emDp
import kr.paranos.stolenlife.ui.components.ems
import kr.paranos.stolenlife.ui.components.tap
import kr.paranos.stolenlife.ui.theme.AlertTone
import kr.paranos.stolenlife.ui.theme.AppTone
import kr.paranos.stolenlife.ui.theme.Blood
import kr.paranos.stolenlife.ui.theme.ChatTone
import kr.paranos.stolenlife.ui.theme.Gold
import kr.paranos.stolenlife.ui.theme.Paper
import kr.paranos.stolenlife.ui.theme.Paran98
import kr.paranos.stolenlife.ui.theme.ParanTone
import kr.paranos.stolenlife.ui.theme.RoomTone
import kr.paranos.stolenlife.ui.theme.Slate
import kr.paranos.stolenlife.ui.theme.StolenTheme
import kr.paranos.stolenlife.ui.theme.TitleGreen
import kr.paranos.stolenlife.ui.theme.VaultTone
import kr.paranos.stolenlife.viewmodel.AppView
import kr.paranos.stolenlife.viewmodel.GameIntents
import kr.paranos.stolenlife.viewmodel.PhoneNav

/** 앱 타이틀바 높이. [PIN] 버퍼가 이 아래에 뜬다. */
val AppBarHeight: Dp = 30.dp

/**
 * 폰 화면 한가운데. 홈이면 런처, 아니면 앱 창(타이틀바 + 본문).
 * 문서 · 프로필은 어느 앱에서 열려도 같은 모양으로 그린다(웹판 appHTML).
 */
@Composable
fun AppHost(nav: PhoneNav, state: GameState, typing: Boolean, intents: GameIntents, modifier: Modifier = Modifier) {
    val app = nav.app
    if (app == null) {
        HomeScreen(state, intents, modifier)
        return
    }
    val top = nav.top ?: AppView.Root
    val repo = GameRepository
    val title = when (top) {
        is AppView.Doc -> repo.document(top.id).title
        is AppView.Prof -> repo.profile(top.id).name
        else -> if (app == AppId.MESSENGER) {
            repo.contactName(state) + if (state.contactRevealed) "" else stringResource(R.string.msg_secure_suffix)
        } else {
            app.title
        }
    }
    val ink = remember(state) { InkState.of(state) }
    // 같은 화면에 머무는 동안은 스크롤 위치를 지킨다. 다른 화면으로 가면 맨 위에서 시작.
    val scroll = remember(app, top) { ScrollState(0) }
    val bodyColor = when {
        top is AppView.LighthouseOpen -> AlertTone.LighthouseWrap
        else -> when (app) {
            AppId.MESSENGER -> ChatTone.Body
            AppId.INSTA -> AppTone.InstaBody
            AppId.FACEBOOK -> AppTone.FacebookBody
            AppId.PARAN -> Paran98
            AppId.VAULT -> VaultTone.Body
            AppId.ROOM -> RoomTone.Body
            AppId.WEB, AppId.SETTINGS -> Slate
        }
    }
    val pad = when {
        app == AppId.ROOM && top == AppView.Root -> 0.dp
        app == AppId.MESSENGER && top == AppView.Root -> emDp(.75f)
        else -> emDp(.85f)
    }
    val pinPad = if (state.pin != null) emDp(3.2f) else 0.dp

    Column(modifier.fillMaxSize().background(Slate)) {
        AppBar(app, title, intents::back)
        BoxWithConstraints(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(bodyColor)
                .then(if (app == AppId.PARAN) Modifier.paranScanlines() else Modifier),
        ) {
            val bodyWidth = maxWidth
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(scroll)
                    .padding(start = pad, end = pad, bottom = pad, top = maxOf(pad, pinPad)),
            ) {
                when {
                    top is AppView.Doc -> DocumentView(repo.document(top.id), ink, intents)
                    top is AppView.Prof -> ProfileView(repo.profile(top.id), ink, intents)
                    top is AppView.LighthouseOpen -> LighthouseOpenView()
                    else -> when (app) {
                        AppId.MESSENGER -> MessengerContent(state, typing, ink, bodyWidth, scroll, intents)
                        AppId.WEB, AppId.INSTA, AppId.FACEBOOK -> SearchContent(app, top, state, intents)
                        AppId.PARAN -> ParanContent(top, intents)
                        AppId.VAULT -> VaultContent(state, ink, intents)
                        AppId.ROOM -> RoomContent(state, intents)
                        AppId.SETTINGS -> SettingsContent(state, intents)
                    }
                }
            }
        }
    }
}

@Composable
private fun AppBar(app: AppId, title: String, onBack: () -> Unit) {
    val type = StolenTheme.type
    val bar = when {
        app == AppId.PARAN -> ParanTone.Bar
        app.alertChrome -> Blood
        else -> TitleGreen
    }
    val backRule = if (app.alertChrome) AppTone.AlertBarBackRule else AppTone.BarBackRule
    val codeColor = if (app.alertChrome) AppTone.AlertBarCode else AppTone.BarCode
    val backCd = stringResource(R.string.appbar_back_cd)
    Row(
        Modifier
            .fillMaxWidth()
            .height(AppBarHeight)
            .background(bar)
            .drawBehind {
                // 아래 1px 검은 선 + 그 위 1px 아주 옅은 밝은 선 (inset 하이라이트)
                drawRect(Color.White.copy(alpha = 0.04f), Offset(0f, size.height - 2.dp.toPx()), Size(size.width, 1.dp.toPx()))
            }
            .edge(Color.Black, 1.dp, Side.BOTTOM)
            .padding(horizontal = emDp(.65f)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicText(
            text = stringResource(R.string.appbar_back),
            style = type.keypad.copy(fontSize = ems(1.05f), lineHeight = ems(1.3f), color = Gold),
            modifier = Modifier
                .semantics { contentDescription = backCd }
                .border(1.dp, backRule)
                .tap(onClick = onBack)
                .padding(horizontal = emDp(.55f)),
        )
        Spacer(Modifier.width(emDp(.6f)))
        BasicText(
            text = title,
            style = type.ui.copy(fontSize = ems(.82f), color = Paper),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(emDp(.6f)))
        BasicText(app.code, style = type.ui.copy(fontSize = ems(.56f), letterSpacing = 0.14.em, color = codeColor))
    }
}

/** 1998 천리안 화면의 주사선(3px 마다 1px 짙은 줄). 본문 영역 크기만큼만 긋는다. */
private fun Modifier.paranScanlines(): Modifier = this.drawBehind {
    var y = 0f
    val line = Color.Black.copy(alpha = 0.2f)
    while (y < size.height) {
        drawRect(line, Offset(0f, y), Size(size.width, 1f))
        y += 3f
    }
}
