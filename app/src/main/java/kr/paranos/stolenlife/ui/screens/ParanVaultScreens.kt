package kr.paranos.stolenlife.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import kr.paranos.stolenlife.R
import kr.paranos.stolenlife.data.DocId
import kr.paranos.stolenlife.data.GameRepository
import kr.paranos.stolenlife.data.GameState
import kr.paranos.stolenlife.data.LockId
import kr.paranos.stolenlife.data.ParanBoard
import kr.paranos.stolenlife.ui.components.DocumentView
import kr.paranos.stolenlife.ui.components.InkState
import kr.paranos.stolenlife.ui.components.emDp
import kr.paranos.stolenlife.ui.components.ems
import kr.paranos.stolenlife.ui.components.pressSurface
import kr.paranos.stolenlife.ui.theme.Amber
import kr.paranos.stolenlife.ui.theme.ParanTone
import kr.paranos.stolenlife.ui.theme.StolenTheme
import kr.paranos.stolenlife.ui.theme.VaultTone
import kr.paranos.stolenlife.viewmodel.AppView
import kr.paranos.stolenlife.viewmodel.GameIntents

// ═════════════════════════════════════════════════════════════════════════════
//  파란누리 — 천리안 V5.0 터미널 (1998). 키보드 입력 차단, 명령은 버튼으로만.
// ═════════════════════════════════════════════════════════════════════════════

@Composable
fun ParanContent(top: AppView, intents: GameIntents) {
    val type = StolenTheme.type
    val term = type.terminal.copy(fontSize = ems(.84f), lineHeight = ems(.84f * 1.65f), color = ParanTone.Text)
    Column(Modifier.fillMaxWidth()) {
        when (top) {
            AppView.ParanConnected -> {
                BasicText(
                    text = buildAnnotatedString {
                        withStyle(SpanStyle(color = ParanTone.Cyan)) { append(stringResource(R.string.paran_connect)) }
                        append("\n")
                        append(stringResource(R.string.paran_welcome))
                    },
                    style = term,
                )
                TerminalButton(stringResource(R.string.paran_go), intents::enterParan)
                TerminalButton(stringResource(R.string.paran_bye), intents::back)
            }

            AppView.ParanMenu -> {
                BasicText(
                    text = buildAnnotatedString {
                        withStyle(SpanStyle(color = ParanTone.Yellow)) { append(stringResource(R.string.paran_menu_banner)) }
                        append("\n")
                        append(stringResource(R.string.paran_menu_info))
                    },
                    style = term,
                )
                ParanBoard.entries.forEachIndexed { index, board ->
                    TerminalButton(stringResource(R.string.paran_menu_item, index + 1, board.title)) { intents.openBoard(board) }
                }
            }

            is AppView.Board -> {
                val posts = GameRepository.paranBoards.getValue(top.board)
                BasicText(
                    text = AnnotatedString(stringResource(R.string.paran_board_header, top.board.title)),
                    style = term.copy(color = ParanTone.Yellow),
                )
                posts.forEachIndexed { index, id ->
                    val doc = GameRepository.document(id)
                    val number = (posts.size - index).toString().padStart(3, '0')
                    val author = doc.source.substringAfter("] ", "")
                    TerminalButton("$number ${doc.title} · $author") { intents.openDocument(id) }
                }
            }

            else -> {
                BasicText(
                    text = buildAnnotatedString {
                        withStyle(SpanStyle(color = ParanTone.Yellow)) { append(stringResource(R.string.paran_banner)) }
                        append("\n")
                        append(stringResource(R.string.paran_boot))
                    },
                    style = term,
                )
                TerminalButton(stringResource(R.string.paran_dial), intents::dialModem)
            }
        }
    }
}

/** `> 명령` 버튼. 누르는 동안만 파랗게 반전. */
@Composable
private fun TerminalButton(text: String, onClick: () -> Unit) {
    val type = StolenTheme.type
    BasicText(
        text = buildAnnotatedString {
            withStyle(SpanStyle(color = ParanTone.Yellow)) { append(stringResource(R.string.paran_prompt)) }
            append(text)
        },
        style = type.terminal.copy(fontSize = ems(.84f), lineHeight = ems(.84f * 1.4f), color = ParanTone.Text),
        modifier = Modifier
            .fillMaxWidth()
            .pressSurface(androidx.compose.ui.graphics.Color.Transparent, ParanTone.Pressed, onClick = onClick)
            .padding(vertical = emDp(.32f)),
    )
}

// ═════════════════════════════════════════════════════════════════════════════
//  금고95 — 무적문화재단 내부 금고 시스템
// ═════════════════════════════════════════════════════════════════════════════

@Composable
fun VaultContent(state: GameState, ink: InkState, intents: GameIntents) {
    val type = StolenTheme.type
    if (state.otpPressed) {
        DocumentView(GameRepository.document(DocId.VAULT_SUMMARY), ink, intents)
        return
    }
    val keyAccepted = LockId.VAULT in state.unlocked
    Column(
        Modifier
            .fillMaxWidth()
            .padding(vertical = emDp(.4f))
            .border(1.dp, VaultTone.BoxRule)
            .drawBehind {
                // 아주 옅은 2px 가로줄 (repeating-linear-gradient 대신 줄을 직접)
                var y = 0f
                val pitch = 4.dp.toPx()
                while (y < size.height) {
                    drawRect(Amber.copy(alpha = 0.035f), Offset(0f, y), Size(size.width, 2.dp.toPx()))
                    y += pitch
                }
            }
            .padding(horizontal = emDp(1f), vertical = emDp(1.2f)),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val body = type.ui.copy(fontSize = ems(.84f), lineHeight = ems(.84f * 1.85f), color = Amber, textAlign = TextAlign.Center)
        val small = body.copy(fontSize = ems(.7f), color = VaultTone.Small, letterSpacing = 0.04.em)
        BasicText(
            text = stringResource(R.string.vault_title),
            style = type.keypad.copy(fontSize = ems(1.35f), letterSpacing = 0.55.em, color = VaultTone.Title),
            modifier = Modifier.padding(start = emDp(.7f), bottom = emDp(.5f)),
        )
        if (keyAccepted) {
            BasicText(stringResource(R.string.vault_key_ok), style = body)
            BasicText(stringResource(R.string.vault_wait_otp), style = body)
            BasicText(stringResource(R.string.vault_press_token), style = small)
            VaultButton(stringResource(R.string.vault_to_room), intents::goToRoom)
        } else {
            BasicText(stringResource(R.string.vault_org), style = small)
            BasicText(stringResource(R.string.vault_need_key), style = body)
            VaultButton(stringResource(R.string.vault_enter_key)) { intents.openLock(LockId.VAULT) }
        }
    }
}

@Composable
private fun VaultButton(text: String, onClick: () -> Unit) {
    BasicText(
        text = text,
        style = StolenTheme.type.ui.copy(fontSize = ems(.74f), letterSpacing = 0.08.em, color = Amber),
        modifier = Modifier
            .padding(top = emDp(.9f))
            .border(1.dp, VaultTone.ButtonRule)
            .pressSurface(androidx.compose.ui.graphics.Color.Transparent, VaultTone.ButtonPressed, onClick = onClick)
            .padding(horizontal = emDp(1f), vertical = emDp(.5f)),
    )
}
