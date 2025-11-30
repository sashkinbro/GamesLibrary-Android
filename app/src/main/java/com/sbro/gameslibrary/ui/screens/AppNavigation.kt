package com.sbro.gameslibrary.ui.screens

import android.content.Context
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.sbro.gameslibrary.viewmodel.GameViewModel
import com.sbro.gameslibrary.viewmodel.PlatformFilter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private val Context.dataStore by preferencesDataStore(name = "settings")

private object PrefKeys {
    val ONBOARDING_SHOWN = booleanPreferencesKey("onboarding_shown")
}

private fun onboardingShownFlow(context: Context) =
    context.dataStore.data.map { prefs ->
        prefs[PrefKeys.ONBOARDING_SHOWN] ?: false
    }

private suspend fun setOnboardingShown(context: Context) {
    context.dataStore.edit { prefs ->
        prefs[PrefKeys.ONBOARDING_SHOWN] = true
    }
}

object Routes {
    const val MAIN = "main"
    const val HOME = "home"
    const val LAST_TESTS = "last_tests"
    const val LIBRARY = "library"
    const val LIBRARY_PLATFORM = "library/{platform}"
    const val ABOUT = "about"

    const val DETAILS = "details/{gameId}"

    const val EDIT_STATUS = "edit_status/{gameId}"
    const val TEST_HISTORY = "test_history/{gameId}"

    const val TEST_HISTORY_DETAIL = "test_history_detail/{gameId}/{testMillis}"
    const val SPLASH = "splash"
    const val ONBOARDING = "onboarding"

    fun libraryPlatformRoute(platform: String) = "library/$platform"
    fun detailsRoute(gameId: String) = "details/$gameId"
    fun editStatusRoute(gameId: String) = "edit_status/$gameId"
    fun testHistoryRoute(gameId: String) = "test_history/$gameId"

    fun testHistoryDetailRoute(gameId: String, testMillis: Long) =
        "test_history_detail/$gameId/$testMillis"
}

@Composable
fun PSGamesApp() {
    val navController = rememberNavController()
    val vm: GameViewModel = viewModel()
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current
    val hasSeenOnboarding by produceState<Boolean?>(initialValue = null) {
        onboardingShownFlow(context).collect { value = it }
    }

    NavHost(
        navController = navController,
        startDestination = Routes.SPLASH,
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
        composable(Routes.MAIN) {
            MainScreen(
                viewModel = vm,
                onOpenPlatforms = { navController.navigate(Routes.HOME) },
                onOpenLastTests = { navController.navigate(Routes.LAST_TESTS) },
                onOpenAbout = { navController.navigate(Routes.ABOUT) }
            )
        }

        composable(Routes.LAST_TESTS) {
            LastTestsScreen(
                viewModel = vm,
                onBack = { navController.popBackStack() },
                onOpenLastTestDetails = { gameId ->
                    navController.navigate(Routes.detailsRoute(gameId))
                }
            )
        }

        composable(Routes.HOME) {
            HomeScreen(
                onBack = { navController.popBackStack() },
                onOpenAllGames = { navController.navigate(Routes.LIBRARY) },
                onOpenPs3Games = { navController.navigate(Routes.libraryPlatformRoute("ps3")) },
                onOpenPcGames = { navController.navigate(Routes.libraryPlatformRoute("pc")) },
                onOpenSwitchGames = { navController.navigate(Routes.libraryPlatformRoute("switch")) },
                onOpenAbout = { navController.navigate(Routes.ABOUT) }
            )
        }

        composable(Routes.LIBRARY) {
            GameLibraryScreen(
                viewModel = vm,
                showBackButton = true,
                onBack = { navController.popBackStack() },
                lockedPlatform = null,
                onOpenDetails = { game ->
                    navController.navigate(Routes.detailsRoute(game.id))
                },
                onOpenEditStatus = { game ->
                    navController.navigate(Routes.editStatusRoute(game.id))
                },
                onOpenTestHistory = { game ->
                    navController.navigate(Routes.testHistoryRoute(game.id))
                }
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
                viewModel = vm,
                showBackButton = true,
                onBack = { navController.popBackStack() },
                lockedPlatform = locked,
                onOpenDetails = { game ->
                    navController.navigate(Routes.detailsRoute(game.id))
                },
                onOpenEditStatus = { game ->
                    navController.navigate(Routes.editStatusRoute(game.id))
                },
                onOpenTestHistory = { game ->
                    navController.navigate(Routes.testHistoryRoute(game.id))
                }
            )
        }

        composable(Routes.ABOUT) {
            AboutScreen(onBack = { navController.popBackStack() })
        }

        composable(
            route = Routes.DETAILS,
            arguments = listOf(navArgument("gameId") { type = NavType.StringType })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getString("gameId") ?: return@composable

            GameDetailScreen(
                viewModel = vm,
                gameId = id,
                onBack = { navController.popBackStack() },
                onOpenEditStatus = { gameId2 ->
                    navController.navigate(Routes.editStatusRoute(gameId2))
                },
                onOpenTestHistory = { gameId2 ->
                    navController.navigate(Routes.testHistoryRoute(gameId2))
                }
            )
        }

        composable(
            route = Routes.EDIT_STATUS,
            arguments = listOf(navArgument("gameId") { type = NavType.StringType })
        ) { entry ->
            val gameId = entry.arguments?.getString("gameId") ?: return@composable
            EditStatusScreen(
                viewModel = vm,
                gameId = gameId,
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Routes.TEST_HISTORY,
            arguments = listOf(navArgument("gameId") { type = NavType.StringType })
        ) { entry ->
            val gameId = entry.arguments?.getString("gameId") ?: return@composable
            TestHistoryScreen(
                viewModel = vm,
                gameId = gameId,
                onBack = { navController.popBackStack() },
                onOpenTestDetails = { millis ->
                    navController.navigate(Routes.testHistoryDetailRoute(gameId, millis))
                }
            )
        }
        composable(
            route = Routes.TEST_HISTORY_DETAIL,
            arguments = listOf(
                navArgument("gameId") { type = NavType.StringType },
                navArgument("testMillis") { type = NavType.LongType }
            )
        ) { entry ->
            val gameId = entry.arguments?.getString("gameId") ?: return@composable
            val testMillis = entry.arguments?.getLong("testMillis") ?: 0L

            TestHistoryDetailScreen(
                viewModel = vm,
                gameId = gameId,
                testMillis = testMillis,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.SPLASH) {
            SplashScreen(
                hasSeenOnboarding = hasSeenOnboarding,
                onNavigateNext = { goToOnboarding ->
                    navController.navigate(
                        if (goToOnboarding) Routes.ONBOARDING else Routes.MAIN
                    ) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.ONBOARDING) {
            OnboardingScreen(
                onFinish = {
                    scope.launch {
                        setOnboardingShown(context)
                        navController.navigate(Routes.MAIN) {
                            popUpTo(Routes.ONBOARDING) { inclusive = true }
                        }
                    }
                }
            )
        }
    }
}
