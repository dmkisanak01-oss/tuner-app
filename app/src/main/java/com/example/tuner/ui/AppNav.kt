package com.example.tuner.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.tuner.AppContainer
import com.example.tuner.ui.screens.GameDetailScreen
import com.example.tuner.ui.screens.GamesScreen
import com.example.tuner.ui.screens.PrivilegeScreen

val LocalContainer = compositionLocalOf<AppContainer> { error("AppContainer belum disediakan") }

object Routes {
    const val PRIVILEGE = "privilege"
    const val GAMES = "games"
    const val GAME = "game/{pkg}"
    fun game(pkg: String) = "game/$pkg"
}

@Composable
fun AppNav() {
    val nav = rememberNavController()
    // Start di daftar game. Kalau belum ada akses, banner di S03 mengarahkan ke S02.
    NavHost(navController = nav, startDestination = Routes.GAMES) {
        composable(Routes.PRIVILEGE) { PrivilegeScreen(onDone = { nav.popBackStack() }) }
        composable(Routes.GAMES) {
            GamesScreen(
                onOpenGame = { nav.navigate(Routes.game(it)) },
                onSetupAccess = { nav.navigate(Routes.PRIVILEGE) },
            )
        }
        composable(Routes.GAME) { entry ->
            GameDetailScreen(
                pkg = entry.arguments?.getString("pkg").orEmpty(),
                onBack = { nav.popBackStack() },
                onSetupAccess = { nav.navigate(Routes.PRIVILEGE) },
            )
        }
    }
}
