package com.hyunnyApps.nonogram.ui

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.hyunnyApps.nonogram.AppContainer
import com.hyunnyApps.nonogram.ui.game.GameScreen
import com.hyunnyApps.nonogram.ui.game.GameViewModel
import com.hyunnyApps.nonogram.ui.home.HomeScreen
import com.hyunnyApps.nonogram.ui.pack.PackScreen
import com.hyunnyApps.nonogram.ui.pack.PackViewModel
import com.hyunnyApps.nonogram.ui.settings.LicensesScreen
import com.hyunnyApps.nonogram.ui.settings.SettingsScreen

private object Routes {
    const val HOME = "home"
    const val PACK = "pack/{${PackViewModel.ARG_PACK_ID}}"
    const val GAME = "game/{${GameViewModel.ARG_SOURCE}}/{${GameViewModel.ARG_PUZZLE_ID}}"
    const val SETTINGS = "settings"
    const val LICENSES = "licenses"

    fun pack(id: String) = "pack/${Uri.encode(id)}"
    fun game(source: String, id: String) = "game/${Uri.encode(source)}/${Uri.encode(id)}"
}

@Composable
fun NonogramNavHost(container: AppContainer, modifier: Modifier = Modifier) {
    val nav = rememberNavController()
    NavHost(nav, startDestination = Routes.HOME, modifier = modifier) {
        composable(Routes.HOME) {
            HomeScreen(
                onOpenPuzzle = { source, id -> nav.navigate(Routes.game(source, id)) },
                onOpenPack = { nav.navigate(Routes.pack(it)) },
                onOpenSettings = { nav.navigate(Routes.SETTINGS) },
            )
        }
        composable(Routes.PACK, arguments = listOf(navArgument(PackViewModel.ARG_PACK_ID) { type = NavType.StringType })) {
            PackScreen(
                onBack = { nav.popBackStack() },
                onOpenPuzzle = { source, id -> nav.navigate(Routes.game(source, id)) },
            )
        }
        composable(
            Routes.GAME,
            arguments = listOf(
                navArgument(GameViewModel.ARG_SOURCE) { type = NavType.StringType },
                navArgument(GameViewModel.ARG_PUZZLE_ID) { type = NavType.StringType },
            ),
        ) {
            GameScreen(
                rewarded = container.rewarded,
                onBack = { nav.popBackStack() },
                // 다음 퍼즐은 현재 게임 화면을 대체한다 (뒤로 가기 시 목록으로)
                onOpenPuzzle = { source, id ->
                    nav.navigate(Routes.game(source, id)) {
                        popUpTo(Routes.GAME) { inclusive = true }
                    }
                },
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(container.consent, onBack = { nav.popBackStack() }, onOpenLicenses = { nav.navigate(Routes.LICENSES) })
        }
        composable(Routes.LICENSES) { LicensesScreen(onBack = { nav.popBackStack() }) }
    }
}
