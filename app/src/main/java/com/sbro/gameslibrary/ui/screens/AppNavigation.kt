package com.sbro.gameslibrary.ui.screens

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.sbro.gameslibrary.viewmodel.PlatformFilter

object Routes {
    const val HOME = "home"
    const val LIBRARY = "library"
    const val LIBRARY_PLATFORM = "library/{platform}"
    const val ABOUT = "about"

    fun libraryPlatformRoute(platform: String) = "library/$platform"
}

@Composable
fun PSGamesApp() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.HOME,
        enterTransition = {
            slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.Left,
                animationSpec = tween(250)
            )
        },
        exitTransition = {
            slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.Left,
                animationSpec = tween(250)
            )
        },
        popEnterTransition = {
            slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.Right,
                animationSpec = tween(250)
            )
        },
        popExitTransition = {
            slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.Right,
                animationSpec = tween(250)
            )
        }
    ) {
        composable(Routes.HOME) {
            HomeScreen(
                onOpenAllGames = { navController.navigate(Routes.LIBRARY) },
                onOpenPs3Games = { navController.navigate(Routes.libraryPlatformRoute("ps3")) },
                onOpenPcGames = { navController.navigate(Routes.libraryPlatformRoute("pc")) },
                onOpenSwitchGames = { navController.navigate(Routes.libraryPlatformRoute("switch")) },
                onOpenAbout = { navController.navigate(Routes.ABOUT) }
            )
        }

        composable(Routes.LIBRARY) {
            GameLibraryScreen(
                showBackButton = true,
                onBack = { navController.popBackStack() },
                lockedPlatform = null
            )
        }
        composable(
            route = Routes.LIBRARY_PLATFORM,
            arguments = listOf(navArgument("platform") { type = NavType.StringType })
        ) { backStackEntry ->
            val platformArg = backStackEntry.arguments?.getString("platform")?.lowercase()

            val locked = when (platformArg) {
                "ps3" -> PlatformFilter.PS3
                "pc" -> PlatformFilter.PC
                "switch", "nintendo", "nintendoswitch" -> PlatformFilter.SWITCH
                else -> null
            }

            GameLibraryScreen(
                showBackButton = true,
                onBack = { navController.popBackStack() },
                lockedPlatform = locked
            )
        }

        composable(Routes.ABOUT) {
            AboutScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
