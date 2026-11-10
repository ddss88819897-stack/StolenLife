package kr.paranos.stolenlife.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import kotlinx.serialization.Serializable
import kr.paranos.stolenlife.ui.components.CrtScanlines
import kr.paranos.stolenlife.ui.components.EvidenceDrawer
import kr.paranos.stolenlife.ui.components.MonologueBar
import kr.paranos.stolenlife.ui.components.NavBarHeight
import kr.paranos.stolenlife.ui.components.OverlayHost
import kr.paranos.stolenlife.ui.components.PinBuffer
import kr.paranos.stolenlife.ui.components.PinMenuPopup
import kr.paranos.stolenlife.ui.components.StatusBar
import kr.paranos.stolenlife.ui.components.StatusBarHeight
import kr.paranos.stolenlife.ui.components.SystemNavBar
import kr.paranos.stolenlife.ui.components.ToastStack
import kr.paranos.stolenlife.ui.components.chromaticAberration
import kr.paranos.stolenlife.ui.components.emDp
import kr.paranos.stolenlife.ui.components.screenBurst
import kr.paranos.stolenlife.ui.components.screenFlash
import kr.paranos.stolenlife.ui.components.screenShake
import kr.paranos.stolenlife.ui.screens.AppBarHeight
import kr.paranos.stolenlife.ui.screens.AppHost
import kr.paranos.stolenlife.ui.screens.BootScreen
import kr.paranos.stolenlife.ui.screens.RaidOverlay
import kr.paranos.stolenlife.ui.theme.Void
import kr.paranos.stolenlife.viewmodel.GameViewModel

/** 부팅 화면 */
@Serializable
object BootRoute

/** ParanOS 98 폰 (홈 · 앱 · 문서). 앱 안의 화면 쌓기는 게임 상태라서 ViewModel 이 관리한다. */
@Serializable
object PhoneRoute

private const val MONO_IDLE = "…"

/**
 * 폰 껍데기 전체.
 *
 *   ┌ 상태바 ───────────────┐
 *   │ NavHost(부팅 | 폰)      │ ← 글리치 색수차 · 화면 찢김은 이 영역에만
 *   ├ 독백 바 ───────────────┤
 *   └ 하단 시스템 바 ─────────┘
 *   위에 겹치는 층: [PIN] 버퍼 → 단서함 → 토스트 → 핀 메뉴 → 창 → CRT 주사선
 */
@Composable
fun StolenLifeApp(vm: GameViewModel) {
    val state by vm.state.collectAsStateWithLifecycle()
    val ui by vm.ui.collectAsStateWithLifecycle()
    val mono by vm.monologue.collectAsStateWithLifecycle()
    val boot by vm.boot.collectAsStateWithLifecycle()

    val navController = rememberNavController()
    val entry by navController.currentBackStackEntryAsState()
    val onBoot = entry?.destination?.hasRoute<BootRoute>() ?: true
    // 핀 메뉴를 띄울 자리: 마지막으로 손가락이 닿은 곳
    var anchor by remember { mutableStateOf(Offset.Zero) }

    Box(
        Modifier
            .fillMaxSize()
            .background(Void)
            .windowInsetsPadding(WindowInsets.displayCutout)
            .screenShake(ui.fx.shake)
            .screenFlash(ui.fx.flash)
            .pointerInput(Unit) {
                awaitEachGesture {
                    anchor = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial).position
                }
            },
    ) {
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f).fillMaxWidth()) {
                Column(Modifier.fillMaxSize()) {
                    StatusBar(state.clock, state.battery, state.proxyOff, ui.fx.beacon, ui.fx.beaconRed)
                    Box(
                        Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .clipToBounds()
                            .chromaticAberration(state.glitch)
                            .screenBurst(ui.fx.burst),
                    ) {
                        NavHost(
                            navController = navController,
                            startDestination = BootRoute,
                            enterTransition = { EnterTransition.None },
                            exitTransition = { ExitTransition.None },
                        ) {
                            composable<BootRoute> {
                                BootScreen(
                                    boot = boot,
                                    onNew = {
                                        vm.newGame()
                                        navController.navigate(PhoneRoute) { popUpTo<BootRoute> { inclusive = true } }
                                    },
                                    onContinue = {
                                        vm.continueGame()
                                        navController.navigate(PhoneRoute) { popUpTo<BootRoute> { inclusive = true } }
                                    },
                                )
                            }
                            composable<PhoneRoute> {
                                val canHandle = ui.locked || ui.overlay != null || ui.pinMenu != null || ui.drawer.open || !ui.nav.atHome
                                BackHandler(enabled = canHandle) { vm.handleSystemBack() }
                                AppHost(ui.nav, state, ui.typing, vm)
                            }
                        }
                    }
                }
                ui.raid?.let { RaidOverlay(it) }
            }
            MonologueBar(
                text = mono.text.ifEmpty { if (onBoot) MONO_IDLE else "" },
                talking = mono.talking,
                more = mono.more,
                glitch = state.glitch,
                exposed = state.proxyOff,
                onTap = vm::tapMonologue,
            )
            SystemNavBar(
                chipCount = state.chips.size,
                drawerOpen = ui.drawer.open,
                dot = ui.drawer.dot,
                enabled = !onBoot,
                onBack = vm::back,
                onHome = vm::home,
                onDrawer = vm::toggleDrawer,
                onHint = vm::hint,
            )
        }

        if (!onBoot) {
            state.pin?.let { pin ->
                PinBuffer(
                    pin = pin,
                    onClear = vm::unpin,
                    modifier = Modifier.padding(top = StatusBarHeight + AppBarHeight + 3.dp, start = emDp(.6f), end = emDp(.6f)),
                )
            }
            EvidenceDrawer(
                drawer = ui.drawer,
                chips = state.chips,
                fresh = ui.freshChips,
                onClose = vm::closeDrawer,
                onFilter = vm::setDrawerFilter,
                onChip = vm::tapTrayChip,
                modifier = Modifier.padding(bottom = NavBarHeight),
            )
        }
        ToastStack(
            toasts = ui.toasts,
            onTap = vm::tapToast,
            modifier = Modifier.padding(top = emDp(1.7f), start = emDp(.6f), end = emDp(.6f)),
        )
        ui.pinMenu?.let { PinMenuPopup(it, anchor, vm::choosePinOption, vm::dismissPinMenu) }
        ui.overlay?.let { OverlayHost(it, vm) }
        CrtScanlines(state.glitch)
    }
}
