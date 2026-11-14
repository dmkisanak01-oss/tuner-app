package com.example.tuner.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.tuner.AppContainer
import com.example.tuner.ui.screens.GameDetailScreen
import com.example.tuner.ui.screens.GamesScreen
import com.example.tuner.ui.screens.OnboardingScreen
import com.example.tuner.ui.screens.PrivilegeScreen
import com.example.tuner.ui.screens.RenderingScreen
import com.example.tuner.ui.screens.SettingsScreen
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

val LocalContainer = compositionLocalOf<AppContainer> { error("AppContainer belum disediakan") }

object Routes {
    const val ONBOARDING = "onboarding"
    const val PRIVILEGE = "privilege"
    const val GAMES = "games"
    const val GAME = "game/{pkg}"
    const val SETTINGS = "settings"
    const val RENDERING = "rendering"
    fun game(pkg: String) = "game/$pkg"
}

@Composable
fun AppNav() {
    val c = LocalContainer.current
    val scope = rememberCoroutineScope()
    // null = DataStore belum terbaca; NavHost baru dibuat setelah itu supaya start destination benar.
    val onboardingDone by c.settings.onboardingDone.map<Boolean, Boolean?> { it }.collectAsStateWithLifecycle(initialValue = null)
    val done = onboardingDone ?: return

    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = if (done) Routes.GAMES else Routes.ONBOARDING) {
        composable(Routes.ONBOARDING) {
            OnboardingScreen(onAccept = {
                scope.launch {
                    c.settings.setOnboardingDone()
                    nav.navigate(Routes.GAMES) { popUpTo(Routes.ONBOARDING) { inclusive = true } }
                }
            })
        }
        composable(Routes.PRIVILEGE) { PrivilegeScreen(onDone = { nav.popBackStack() }) }
        composable(Routes.GAMES) {
            GamesScreen(
                onOpenGame = { nav.navigate(Routes.game(it)) },
                onSetupAccess = { nav.navigate(Routes.PRIVILEGE) },
                onOpenSettings = { nav.navigate(Routes.SETTINGS) },
            )
        }
        composable(Routes.GAME) { entry ->
            GameDetailScreen(
                pkg = entry.arguments?.getString("pkg").orEmpty(),
                onBack = { nav.popBackStack() },
                onSetupAccess = { nav.navigate(Routes.PRIVILEGE) },
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBack = { nav.popBackStack() },
                onAccess = { nav.navigate(Routes.PRIVILEGE) },
                onRendering = { nav.navigate(Routes.RENDERING) },
            )
        }
        composable(Routes.RENDERING) {
            RenderingScreen(onBack = { nav.popBackStack() }, onSetupAccess = { nav.navigate(Routes.PRIVILEGE) })
        }
    }
}
