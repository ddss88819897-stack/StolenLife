package kr.paranos.stolenlife.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import kr.paranos.stolenlife.R
import kr.paranos.stolenlife.data.AppId
import kr.paranos.stolenlife.data.GameRepository
import kr.paranos.stolenlife.data.GameState
import kr.paranos.stolenlife.data.Picture
import kr.paranos.stolenlife.data.SearchHit
import kr.paranos.stolenlife.ui.components.Swatch
import kr.paranos.stolenlife.ui.components.BlinkText
import kr.paranos.stolenlife.ui.components.PixelArt
import kr.paranos.stolenlife.ui.components.PixelImage
import kr.paranos.stolenlife.ui.components.ProfileHeader
import kr.paranos.stolenlife.ui.components.Side
import kr.paranos.stolenlife.ui.components.TipBox
import kr.paranos.stolenlife.ui.components.blinkAlpha
import kr.paranos.stolenlife.ui.components.edge
import kr.paranos.stolenlife.ui.components.emDp
import kr.paranos.stolenlife.ui.components.ems
import kr.paranos.stolenlife.ui.components.pressSurface
import kr.paranos.stolenlife.ui.theme.AlertTone
import kr.paranos.stolenlife.ui.theme.AppTone
import kr.paranos.stolenlife.ui.theme.Gold
import kr.paranos.stolenlife.ui.theme.LineGreen
import kr.paranos.stolenlife.ui.theme.Mute
import kr.paranos.stolenlife.ui.theme.Paper
import kr.paranos.stolenlife.ui.theme.Phosphor
import kr.paranos.stolenlife.ui.theme.Rust
import kr.paranos.stolenlife.ui.theme.StolenTheme
import kr.paranos.stolenlife.viewmodel.AppView
import kr.paranos.stolenlife.viewmodel.GameIntents

/**
 * ParanNet · InstaPixel · FaceBook 95 공용 검색 화면. 키보드는 없다(추적당한다).
 * 검색창을 누르면 단서함이 열리고, 칩을 고르면 그 칩으로 검색한다.
 */
@Composable
fun SearchContent(app: AppId, top: AppView, state: GameState, intents: GameIntents) {
    val query = (top as? AppView.Results)?.chip
    Column(Modifier.fillMaxWidth()) {
        SearchBar(query?.label, intents::openChipPicker)
        if (query == null) {
            TipBox(
                stringResource(
                    when (app) {
                        AppId.INSTA -> R.string.search_tip_insta
                        AppId.FACEBOOK -> R.string.search_tip_fb
                        else -> R.string.search_tip_web
                    },
                ),
            )
            return
        }
        val hits = GameRepository.results(app, query, state)
        if (hits.isEmpty()) {
            NoResults(query.label)
            return
        }
        hits.forEach { hit ->
            when (hit) {
                is SearchHit.Doc -> {
                    val doc = GameRepository.document(hit.id)
                    ResultCard(
                        title = doc.title,
                        source = doc.source,
                        preview = doc.snippet.orEmpty(),
                        unread = !state.hasRead(hit.id),
                    ) { intents.openDocument(hit.id) }
                }

                is SearchHit.Person -> {
                    val profile = GameRepository.profile(hit.id)
                    ResultCard(
                        title = profile.displayName,
                        source = null,
                        preview = profile.headline,
                        unread = hit.id !in state.visitedProfiles,
                    ) { intents.openProfile(hit.id) }
                }
            }
        }
    }
}

@Composable
private fun SearchBar(query: String?, onClick: () -> Unit) {
    val type = StolenTheme.type
    Row(
        Modifier
            .fillMaxWidth()
            .padding(bottom = emDp(.85f))
            .border(1.dp, LineGreen)
            .pressSurface(AppTone.SearchField, AppTone.CardPressed, onClick = onClick)
            .padding(horizontal = emDp(.75f), vertical = emDp(.6f)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicText(stringResource(R.string.search_prompt), style = type.ui.copy(fontSize = ems(.7f), letterSpacing = 0.06.em, color = Gold))
        Spacer(Modifier.width(emDp(.6f)))
        BasicText(
            text = query ?: stringResource(R.string.search_placeholder),
            style = type.ui.copy(
                fontSize = ems(.8f),
                color = if (query != null) Paper else Mute,
                fontWeight = if (query != null) FontWeight.Medium else FontWeight.Normal,
            ),
            maxLines = 1,
        )
        // 입력 대기 커서
        Swatch(
            Phosphor.copy(alpha = 0.65f),
            Modifier
                .padding(start = 2.dp)
                .alpha(blinkAlpha(1000))
                .width(emDp(.4f))
                .height(emDp(.84f)),
        )
    }
}

@Composable
private fun ResultCard(title: String, source: String?, preview: String, unread: Boolean, onClick: () -> Unit) {
    val type = StolenTheme.type
    Column(
        Modifier
            .fillMaxWidth()
            .padding(bottom = emDp(.5f))
            .border(1.dp, AppTone.CardRule)
            .pressSurface(AppTone.Card, AppTone.CardPressed, onClick = onClick)
            .edge(if (unread) Rust else AppTone.CardIdleAccent, 2.dp, Side.START)
            .padding(start = emDp(.75f) + 2.dp, end = emDp(.75f), top = emDp(.62f), bottom = emDp(.62f)),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BasicText(title, style = type.ui.copy(fontSize = ems(.86f), lineHeight = ems(1.25f), color = Paper), modifier = Modifier.weight(1f, fill = false))
            if (unread) {
                Spacer(Modifier.width(emDp(.5f)))
                BasicText(
                    text = stringResource(R.string.search_new),
                    style = type.ui.copy(fontSize = ems(.52f), letterSpacing = 0.1.em, color = AppTone.NewBadge),
                    modifier = Modifier
                        .border(1.dp, Rust)
                        .padding(horizontal = emDp(.3f)),
                )
            }
        }
        source?.let {
            BasicText(it, style = type.ui.copy(fontSize = ems(.64f), letterSpacing = 0.02.em, color = AppTone.SubGreen), modifier = Modifier.padding(top = emDp(.2f)))
        }
        if (preview.isNotEmpty()) {
            BasicText(preview, style = type.ui.copy(fontSize = ems(.72f), lineHeight = ems(1.05f), color = Mute), modifier = Modifier.padding(top = emDp(.2f)))
        }
    }
}

@Composable
private fun NoResults(label: String) {
    val type = StolenTheme.type
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = emDp(1f), vertical = emDp(3f)),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        BasicText(
            text = stringResource(R.string.search_none_title),
            style = type.keypad.copy(fontSize = ems(1.12f), letterSpacing = 0.06.em, color = Paper),
            modifier = Modifier.padding(bottom = emDp(.5f)),
        )
        BasicText(
            text = stringResource(R.string.search_none_body, label),
            style = type.ui.copy(fontSize = ems(.8f), color = Mute, textAlign = TextAlign.Center),
        )
    }
}

/** 엔딩: @lighthouse_1998 비공개 계정. 라디오 부스와 CCTV, 같은 얼굴. */
@Composable
fun LighthouseOpenView() {
    val type = StolenTheme.type
    val ending = GameRepository.ending
    Column(Modifier.fillMaxWidth()) {
        ProfileHeader("@lighthouse_1998", ending.lighthouseHeadline, PixelArt.picture(Picture.LIGHT))
        Row(Modifier.fillMaxWidth()) {
            ending.lighthouseFigures.forEachIndexed { index, (picture, caption) ->
                if (index > 0) Spacer(Modifier.width(emDp(.55f)))
                Column(Modifier.weight(1f)) {
                    PixelImage(
                        PixelArt.picture(picture),
                        Modifier
                            .fillMaxWidth()
                            .height(emDp(9f))
                            .border(1.dp, AppTone.CardRule)
                            .padding(1.dp)
                            .background(androidx.compose.ui.graphics.Color.Black),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    )
                    BasicText(caption, style = type.ui.copy(fontSize = ems(.6f), color = Mute), modifier = Modifier.padding(top = emDp(.3f)))
                }
            }
        }
        BlinkText(
            text = ending.sameFace,
            style = type.docTitle.copy(fontSize = ems(1.25f), lineHeight = ems(1.6f), letterSpacing = 0.32.em, color = AlertTone.Text, textAlign = TextAlign.Center),
            periodMs = 1300,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = emDp(1.1f)),
        )
    }
}
