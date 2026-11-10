package kr.paranos.stolenlife.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import kr.paranos.stolenlife.R
import kr.paranos.stolenlife.data.AppId
import kr.paranos.stolenlife.data.Chip
import kr.paranos.stolenlife.data.GameRepository
import kr.paranos.stolenlife.data.GameState
import kr.paranos.stolenlife.data.Picture
import kr.paranos.stolenlife.ui.components.PixelArt
import kr.paranos.stolenlife.ui.components.PixelImage
import kr.paranos.stolenlife.ui.components.bevel
import kr.paranos.stolenlife.ui.components.dashedBorder
import kr.paranos.stolenlife.ui.components.emDp
import kr.paranos.stolenlife.ui.components.ems
import kr.paranos.stolenlife.ui.components.tap
import kr.paranos.stolenlife.ui.theme.AppTone
import kr.paranos.stolenlife.ui.theme.Gold
import kr.paranos.stolenlife.ui.theme.Mute
import kr.paranos.stolenlife.ui.theme.Paper
import kr.paranos.stolenlife.ui.theme.Phosphor
import kr.paranos.stolenlife.ui.theme.Rust
import kr.paranos.stolenlife.ui.theme.StolenTheme
import kr.paranos.stolenlife.ui.theme.Void
import kr.paranos.stolenlife.viewmodel.GameIntents

/** 홈에 보이는 앱. 이야기가 진행되면서 하나씩 늘어난다(웹판 homeApps). */
fun homeApps(state: GameState): List<AppId> = buildList {
    addAll(listOf(AppId.MESSENGER, AppId.WEB, AppId.INSTA, AppId.FACEBOOK))
    if (state.holds(Chip.PARANNURI)) add(AppId.PARAN)
    if (state.chapter >= 2) add(AppId.ROOM)
    if (state.binderFound) add(AppId.VAULT)
    add(AppId.SETTINGS)
}

/** 홈 런처: 디더링된 벽 사진 + 시계 위젯 + 4열 아이콘 */
@Composable
fun HomeScreen(state: GameState, intents: GameIntents, modifier: Modifier = Modifier) {
    val type = StolenTheme.type
    val repo = GameRepository
    Box(modifier.fillMaxSize().background(Void)) {
        PixelImage(PixelArt.picture(Picture.WALL), Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        // 원본은 위→아래 어두워지는 그라데이션. 여기서는 단색 막 한 장으로 대신한다.
        Box(Modifier.fillMaxSize().background(HOME_SCRIM))
        Column(Modifier.fillMaxSize().padding(horizontal = emDp(1f), vertical = emDp(1.1f))) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .bevel()
                    .background(AppTone.ClockWidget)
                    .padding(start = emDp(.9f), end = emDp(.9f), top = emDp(.75f), bottom = emDp(.65f)),
            ) {
                BasicText(state.clock, style = type.clock.copy(fontSize = ems(2.7f), lineHeight = ems(2.7f), color = Paper))
                BasicText(
                    text = stringResource(R.string.home_date, repo.homePlace(state)),
                    style = type.ui.copy(fontSize = ems(.72f), color = Phosphor),
                    modifier = Modifier.padding(top = emDp(.4f)),
                )
                BasicText(
                    text = repo.homeTicker(state),
                    style = type.ui.copy(fontSize = ems(.64f), letterSpacing = 0.02.em, color = Mute),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .padding(top = emDp(.55f))
                        .fillMaxWidth()
                        .dashedBorder(AppTone.TickerRule, bottom = false, start = false, end = false)
                        .padding(top = emDp(.45f)),
                )
            }
            Spacer(Modifier.height(emDp(1.5f)))
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val iconSize = minOf(64.dp, maxWidth * 0.18f)
                Column(verticalArrangement = Arrangement.spacedBy(emDp(1.2f))) {
                    homeApps(state).chunked(4).forEach { row ->
                        Row(Modifier.fillMaxWidth()) {
                            row.forEach { app ->
                                HomeIcon(
                                    app = app,
                                    size = iconSize,
                                    fresh = app !in state.seenApps,
                                    badge = if (app == AppId.MESSENGER) state.unread else 0,
                                    modifier = Modifier.weight(1f),
                                ) { intents.openApp(app) }
                            }
                            repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            BasicText(
                text = stringResource(R.string.home_build),
                style = type.ui.copy(fontSize = ems(.58f), letterSpacing = 0.16.em, color = AppTone.BuildLabel, textAlign = TextAlign.Center),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun HomeIcon(
    app: AppId,
    size: androidx.compose.ui.unit.Dp,
    fresh: Boolean,
    badge: Int,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val type = StolenTheme.type
    Column(modifier.tap(onClick = onClick), horizontalAlignment = Alignment.CenterHorizontally) {
        Box {
            PixelImage(
                PixelArt.icon(app),
                Modifier
                    // 처음 보는 앱: 바깥에 금색 1px 테두리를 한 겹 더
                    .then(if (fresh) Modifier.border(1.dp, Gold).padding(1.dp) else Modifier)
                    .bevel()
                    .size(size),
            )
            if (badge > 0) {
                BasicText(
                    text = badge.toString(),
                    style = type.keypad.copy(fontSize = ems(.68f), lineHeight = ems(.9f), color = Paper),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 6.dp, y = (-6).dp)
                        .border(1.dp, Color.Black)
                        .background(Rust)
                        .padding(horizontal = emDp(.3f)),
                )
            }
        }
        BasicText(
            text = app.homeLabel,
            style = type.ui.copy(fontSize = ems(.64f), letterSpacing = 0.06.em, color = AppTone.IconLabel, textAlign = TextAlign.Center),
            maxLines = 1,
            modifier = Modifier.padding(top = emDp(.45f)),
        )
    }
}

private val HOME_SCRIM = Color(0x9E0B0D0C)
