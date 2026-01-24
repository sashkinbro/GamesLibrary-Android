package com.sbro.gameslibrary.cyberpunk.ui.screens

import android.content.Context
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.datastore.preferences.core.edit
import com.sbro.gameslibrary.cyberpunk.ui.components.CyberCommunityRulesDialog
import com.sbro.gameslibrary.util.dataStore
import com.sbro.gameslibrary.util.ONBOARDING_SHOWN
import com.sbro.gameslibrary.util.COMMUNITY_RULES_ACCEPTED
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.sbro.gameslibrary.viewmodel.GameDetailViewModel
import com.sbro.gameslibrary.viewmodel.GameViewModel
import com.sbro.gameslibrary.viewmodel.MyFavoritesViewModel
import com.sbro.gameslibrary.viewmodel.MyTestsViewModel
import com.sbro.gameslibrary.viewmodel.PlatformFilter
import com.sbro.gameslibrary.viewmodel.ProfileViewModel
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch


private fun onboardingShownFlow(context: Context) =
    context.dataStore.data.map { prefs ->
        prefs[ONBOARDING_SHOWN] ?: false
    }

private suspend fun setOnboardingShown(context: Context) {
    context.dataStore.edit { prefs ->
        prefs[ONBOARDING_SHOWN] = true
    }
}

private fun rulesAcceptedFlow(context: Context) =
    context.dataStore.data.map { prefs ->
        prefs[COMMUNITY_RULES_ACCEPTED] ?: false
    }

private suspend fun setRulesAccepted(context: Context) {
    context.dataStore.edit { prefs ->
        prefs[COMMUNITY_RULES_ACCEPTED] = true
    }
}

object Routes {
    const val MAIN = "main"
    const val HOME = "home"
    const val LAST_TESTS = "last_tests"
    const val LIBRARY = "library"
    const val LIBRARY_PLATFORM = "library/{platform}"
    const val ABOUT = "about"
    const val PROFILE = "profile"
    const val MY_TESTS = "my_tests"
    const val MY_COMMENTS = "my_comments"
    const val MY_FAVORITES = "my_favorites"

    const val DETAILS = "details/{gameId}"

    const val EDIT_STATUS = "edit_status/{gameId}"
    const val EDIT_STATUS_DETAIL = "edit_status/{gameId}/{testMillis}"
    const val TEST_HISTORY = "test_history/{gameId}"

    const val TEST_HISTORY_DETAIL = "test_history_detail/{gameId}/{testId}"

    const val SPLASH = "splash"
    const val ONBOARDING = "onboarding"

    const val AUTH_GATE = "auth_gate"
    const val AUTH_LOGIN = "auth_login"
    const val AUTH_REGISTER = "auth_register"
    const val AUTH_RESET = "auth_reset"

    fun libraryPlatformRoute(platform: String) = "library/$platform"
    fun detailsRoute(gameId: String) = "details/$gameId"

    fun editStatusRoute(gameId: String) = "edit_status/$gameId"
    fun editStatusRoute(gameId: String, testMillis: Long) =
        "edit_status/$gameId/$testMillis"

    fun testHistoryRoute(gameId: String) = "test_history/$gameId"

    fun testHistoryDetailRoute(gameId: String, testId: String) =
        "test_history_detail/$gameId/$testId"
}

@Composable
fun CyberGamesApp() {
    val navController = rememberNavController()
    val vm: GameViewModel = viewModel()
    val profileVm: ProfileViewModel = viewModel()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val hasSeenOnboarding by produceState<Boolean?>(initialValue = null) {
        onboardingShownFlow(context).collect { value = it }
    }
    val rulesAccepted by produceState<Boolean?>(initialValue = null) {
        rulesAcceptedFlow(context).collect { value = it }
    }
    var showRulesDialog by rememberSaveable { mutableStateOf(false) }
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    LaunchedEffect(currentRoute, rulesAccepted) {
        if (currentRoute == Routes.MAIN && rulesAccepted == false) {
            showRulesDialog = true
        }
        if (rulesAccepted == true && showRulesDialog && currentRoute == Routes.MAIN) {
            showRulesDialog = false
        }
    }

    val openRulesDialog: () -> Unit = { showRulesDialog = true }
    val confirmRules: () -> Unit = {
        showRulesDialog = false
        scope.launch {
            setRulesAccepted(context)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
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
                    onOpenPlatforms = { navController.navigate(Routes.HOME) },
                    onOpenLastTests = { navController.navigate(Routes.LAST_TESTS) },
                    onOpenAbout = { navController.navigate(Routes.ABOUT) },
                    onOpenProfile = { navController.navigate(Routes.PROFILE) }
                )
            }

            composable(Routes.LAST_TESTS) {
                LastTestsScreen(
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
                    onOpenProfile = { navController.navigate(Routes.PROFILE) }
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
                AboutScreen(
                    onBack = { navController.popBackStack() },
                    onShowRules = openRulesDialog
                )
            }

            composable(
                Routes.DETAILS,
                arguments = listOf(navArgument("gameId") { type = NavType.StringType })
            ) { backStackEntry ->
                val gameId = backStackEntry.arguments?.getString("gameId") ?: return@composable
                val detailVm: GameDetailViewModel = viewModel()
                val savedStateHandle = backStackEntry.savedStateHandle
                val testSavedFlow = savedStateHandle.getStateFlow("test_saved", false)
                val testSaved by testSavedFlow.collectAsState()

                GameDetailScreen(
                    viewModel = detailVm,
                    gameId = gameId,
                    onBack = { navController.popBackStack() },
                    onOpenEditStatus = { id ->
                        navController.navigate(Routes.editStatusRoute(id))
                    },
                    onOpenTestHistory = { id ->
                        navController.navigate(Routes.testHistoryRoute(id))
                    },
                    onTestSavedConsumed = {
                        savedStateHandle["test_saved"] = false
                    },
                    testSaved = testSaved
                )
            }

            composable(
                Routes.EDIT_STATUS,
                arguments = listOf(navArgument("gameId") { type = NavType.StringType })
            ) { entry ->
                val gameId = entry.arguments?.getString("gameId") ?: return@composable
                val detailVm: GameDetailViewModel = viewModel()

                EditStatusScreen(
                    viewModel = detailVm,
                    gameId = gameId,
                    onBack = { navController.popBackStack() },
                    onTestSaved = {
                        navController.previousBackStackEntry
                            ?.savedStateHandle
                            ?.set("test_saved", true)
                    }
                )
            }

            composable(
                route = Routes.EDIT_STATUS_DETAIL,
                arguments = listOf(
                    navArgument("gameId") { type = NavType.StringType },
                    navArgument("testMillis") { type = NavType.LongType }
                )
            ) { entry ->
                val gameId = entry.arguments?.getString("gameId") ?: return@composable
                val testMillis = entry.arguments?.getLong("testMillis") ?: return@composable
                val detailVm: GameDetailViewModel = viewModel()

                EditStatusScreen(
                    viewModel = detailVm,
                    gameId = gameId,
                    testMillis = testMillis,
                    onBack = { navController.popBackStack() },
                    onTestSaved = {
                        navController.previousBackStackEntry
                            ?.savedStateHandle
                            ?.set("test_saved", true)
                    }
                )
            }

            composable(
                route = Routes.TEST_HISTORY,
                arguments = listOf(navArgument("gameId") { type = NavType.StringType })
            ) { entry ->
                val gameId = entry.arguments?.getString("gameId") ?: return@composable
                val detailVm: GameDetailViewModel = viewModel()

                TestHistoryScreen(
                    viewModel = detailVm,
                    gameId = gameId,
                    onBack = { navController.popBackStack() },
                    onOpenTestDetails = { millis ->
                        val testId = "${gameId}_$millis"
                        navController.navigate(
                            Routes.testHistoryDetailRoute(gameId, testId)
                        )
                    }
                )
            }

            composable(
                route = Routes.TEST_HISTORY_DETAIL,
                arguments = listOf(
                    navArgument("gameId") { type = NavType.StringType },
                    navArgument("testId") { type = NavType.StringType }
                )
            ) { entry ->
                val gameId = entry.arguments?.getString("gameId") ?: return@composable
                val testId = entry.arguments?.getString("testId") ?: return@composable
                val testMillis = testId.substringAfterLast("_").toLongOrNull() ?: 0L

                val detailVm: GameDetailViewModel = viewModel()
                val context = LocalContext.current

                val savedStateHandle = entry.savedStateHandle
                val testSavedFlow = savedStateHandle.getStateFlow("test_saved", false)
                val testSaved by testSavedFlow.collectAsState()

                LaunchedEffect(testSaved) {
                    if (testSaved) {
                        detailVm.refresh(context, gameId)
                        savedStateHandle["test_saved"] = false
                    }
                }
                TestHistoryDetailScreen(
                    viewModel = detailVm,
                    gameId = gameId,
                    testMillis = testMillis,
                    onBack = { navController.popBackStack() },
                    onEditTest = { millis ->
                        navController.navigate(Routes.editStatusRoute(gameId, millis))
                    }
                )
            }

            composable(Routes.SPLASH) {
                val state by profileVm.state.collectAsState()

                SplashScreen(
                    hasSeenOnboarding = hasSeenOnboarding,
                    onNavigateNext = { goToOnboarding ->
                        when {
                            goToOnboarding -> {
                                navController.navigate(Routes.ONBOARDING) {
                                    popUpTo(Routes.SPLASH) { inclusive = true }
                                }
                            }

                            state.user != null -> {
                                navController.navigate(Routes.MAIN) {
                                    popUpTo(Routes.SPLASH) { inclusive = true }
                                }
                            }

                            else -> {
                                navController.navigate(Routes.AUTH_GATE) {
                                    popUpTo(Routes.SPLASH) { inclusive = true }
                                }
                            }
                        }
                    }
                )
            }

            composable(Routes.ONBOARDING) {
                OnboardingScreen(
                    onFinish = {
                        scope.launch {
                            setOnboardingShown(context)
                            navController.navigate(Routes.AUTH_GATE) {
                                popUpTo(Routes.ONBOARDING) { inclusive = true }
                            }
                        }
                    }
                )
            }

            composable(Routes.AUTH_GATE) {
                val state by profileVm.state.collectAsState()

                AuthGateScreenCyber(
                    user = state.user,
                    onContinueAsGuest = {
                        navController.navigate(Routes.MAIN) {
                            popUpTo(Routes.AUTH_GATE) { inclusive = true }
                        }
                    },
                    onOpenLogin = { navController.navigate(Routes.AUTH_LOGIN) },
                    onOpenRegister = { navController.navigate(Routes.AUTH_REGISTER) }
                )
            }

            composable(Routes.PROFILE) {
                val state by profileVm.state.collectAsState()

                ProfileScreen(
                    viewModel = profileVm,
                    user = state.user,
                    onBack = { navController.popBackStack() },
                    onOpenMyTests = { navController.navigate(Routes.MY_TESTS) },
                    onOpenMyComments = { navController.navigate(Routes.MY_COMMENTS) },
                    onOpenMyFavorites = { navController.navigate(Routes.MY_FAVORITES) },
                    onOpenMyDevices = { navController.navigate("my_devices") },
                    onOpenLogin = { navController.navigate(Routes.AUTH_LOGIN) },
                    onOpenRegister = { navController.navigate(Routes.AUTH_REGISTER) }
                )
            }

            composable(Routes.AUTH_LOGIN) {
                LoginScreen(
                    viewModel = profileVm,
                    onBack = { navController.popBackStack() },
                    onGoRegister = { navController.navigate(Routes.AUTH_REGISTER) },
                    onGoReset = { navController.navigate(Routes.AUTH_RESET) },
                    onLoggedIn = {
                        navController.navigate(Routes.MAIN) {
                            popUpTo(Routes.AUTH_GATE) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                )
            }

            composable(Routes.AUTH_REGISTER) {
                RegisterScreen(
                    viewModel = profileVm,
                    onBack = { navController.popBackStack() },
                    onGoLogin = {
                        navController.navigate(Routes.AUTH_LOGIN) {
                            popUpTo(Routes.AUTH_REGISTER) { inclusive = true }
                        }
                    },
                    onRegistered = {
                        navController.popBackStack()
                    }
                )
            }

            composable(Routes.AUTH_RESET) {
                ResetPasswordScreen(
                    viewModel = profileVm,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Routes.MY_TESTS) {
                val ctx = LocalContext.current
                val myTestsVm: MyTestsViewModel = viewModel(
                    factory = MyTestsViewModel.factory(ctx)
                )

                MyTestsScreen(
                    viewModel = myTestsVm,
                    onBack = { navController.popBackStack() },
                    onOpenTestDetails = { gameId, testId ->
                        navController.navigate(
                            Routes.testHistoryDetailRoute(gameId, testId)
                        )
                    }
                )
            }

            composable(Routes.MY_COMMENTS) {
                MyCommentsScreen(
                    onBack = { navController.popBackStack() },
                    onOpenComment = { gameId, testMillis, isGameComment ->
                        if (isGameComment) {
                            navController.navigate(Routes.detailsRoute(gameId))
                        } else {
                            navController.navigate(Routes.testHistoryRoute(gameId))
                        }
                    }
                )
            }

            composable(Routes.MY_FAVORITES) {
                val favVm: MyFavoritesViewModel = viewModel()

                MyFavoritesScreen(
                    viewModel = favVm,
                    onBack = { navController.popBackStack() },
                    onOpenGame = { gameId ->
                        navController.navigate(Routes.detailsRoute(gameId))
                    }
                )
            }

            composable("my_devices") {
                MyDevicesScreen(
                    onBack = { navController.popBackStack() }
                )
            }
        }

    if (showRulesDialog) {
        CyberCommunityRulesDialog(onAccept = confirmRules)
    }
}
}
