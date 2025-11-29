package com.sbro.gameslibrary.ui.screens

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.os.SystemClock
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SnippetFolder
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sbro.gameslibrary.R
import com.sbro.gameslibrary.components.*
import com.sbro.gameslibrary.viewmodel.ErrorType
import com.sbro.gameslibrary.viewmodel.GameViewModel
import com.sbro.gameslibrary.viewmodel.PlatformFilter
import com.sbro.gameslibrary.viewmodel.SortOption
import kotlinx.coroutines.launch

@SuppressLint("ConfigurationScreenWidthHeight")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameLibraryScreen(
    viewModel: GameViewModel = viewModel(),
    showBackButton: Boolean = false,
    onBack: () -> Unit = {},
    lockedPlatform: PlatformFilter? = null
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current

    val screenWidthDp = configuration.screenWidthDp
    val orientation = configuration.orientation
    val isTablet = screenWidthDp >= 600
    val isPortrait = orientation == Configuration.ORIENTATION_PORTRAIT

    val useGridLayout = isTablet && !isPortrait

    val horizontalPadding = when {
        screenWidthDp >= 840 -> 48.dp
        isTablet -> 32.dp
        else -> 16.dp
    }
    val verticalPadding = if (isTablet) 12.dp else 8.dp

    val actionIconSize = if (isTablet) 28.dp else 24.dp

    LaunchedEffect(Unit) {
        viewModel.init(context)
    }

    LaunchedEffect(lockedPlatform) {
        lockedPlatform?.let { viewModel.onPlatformFilterChange(it) }
    }

    val games by viewModel.filteredGames.collectAsState()
    val searchText by viewModel.searchText.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    var showSortMenu by remember { mutableStateOf(false) }
    var isSearchActive by remember { mutableStateOf(false) }

    var showFavoritesOnly by rememberSaveable { mutableStateOf(false) }

    var lastNonFavIndex by rememberSaveable { mutableIntStateOf(0) }
    var lastNonFavOffset by rememberSaveable { mutableIntStateOf(0) }

    var pendingRestore by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(showFavoritesOnly, games.size, pendingRestore) {
        if (!showFavoritesOnly && pendingRestore) {
            pendingRestore = false
            val safeIndex = lastNonFavIndex.coerceIn(0, (games.size - 1).coerceAtLeast(0))
            listState.scrollToItem(safeIndex, lastNonFavOffset)
        }
    }

    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    var showEditDialog by remember { mutableStateOf(false) }
    var selectedGame by remember { mutableStateOf<Game?>(null) }

    var showHistoryDialog by remember { mutableStateOf(false) }
    var historyGame by remember { mutableStateOf<Game?>(null) }

    val topBarLastClick = remember { mutableLongStateOf(0L) }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            Column {
                TopAppBar(
                    modifier = Modifier.statusBarsPadding(),
                    title = {
                        if (isSearchActive) {
                            OutlinedTextField(
                                value = searchText,
                                onValueChange = { viewModel.onSearchChange(it) },
                                placeholder = { Text(stringResource(R.string.search_hint)) },
                                singleLine = true,
                                shape = RoundedCornerShape(24.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(focusRequester),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)
                                )
                            )
                            LaunchedEffect(isSearchActive) {
                                if (isSearchActive) focusRequester.requestFocus()
                            }
                        } else {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {

                                    val titleRes = when (lockedPlatform) {
                                        PlatformFilter.PS3 -> R.string.title_ps3_games_library
                                        PlatformFilter.SWITCH -> R.string.title_switch_games_library
                                        PlatformFilter.PC -> R.string.title_pc_games_library
                                        else -> R.string.title_all_games_library
                                    }

                                    Text(
                                        text = stringResource(titleRes),
                                        fontSize = 19.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )

                                    if (showFavoritesOnly) {
                                        Spacer(modifier = Modifier.padding(4.dp))
                                        Icon(
                                            Icons.Filled.Favorite,
                                            contentDescription = null,
                                            modifier = Modifier.height(16.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }

                                val subTitle = when (uiState) {
                                    is GameViewModel.UiState.Success ->
                                        if (showFavoritesOnly) {
                                            val favCount = games.count { it.isFavorite }
                                            stringResource(R.string.filter_favorites_count, favCount)
                                        } else {
                                            stringResource(R.string.subtitle_games_count, games.size)
                                        }

                                    is GameViewModel.UiState.Error ->
                                        stringResource(R.string.subtitle_error)

                                    is GameViewModel.UiState.Loading ->
                                        stringResource(R.string.subtitle_loading)

                                    else ->
                                        stringResource(R.string.subtitle_select_csv)
                                }

                                Text(
                                    subTitle,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        if (showBackButton) {
                            IconButton(
                                onClick = {
                                    val now = SystemClock.elapsedRealtime()
                                    if (now - topBarLastClick.longValue < 400L) return@IconButton
                                    topBarLastClick.longValue = now
                                    onBack()
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = stringResource(R.string.cd_navigate_back),
                                    modifier = Modifier.size(actionIconSize)
                                )
                            }
                        }
                    },
                    actions = {
                        if (isSearchActive) {
                            IconButton(onClick = {
                                isSearchActive = false
                                viewModel.onSearchChange("")
                            }) {
                                Icon(
                                    Icons.Filled.Close,
                                    contentDescription = stringResource(R.string.cd_close_search),
                                    modifier = Modifier.size(actionIconSize)
                                )
                            }
                        } else {
                            IconButton(onClick = {
                                val current = SystemClock.elapsedRealtime()
                                if (current - topBarLastClick.longValue < 400L) return@IconButton
                                topBarLastClick.longValue = current
                                isSearchActive = true
                            }) {
                                Icon(
                                    Icons.Filled.Search,
                                    contentDescription = stringResource(R.string.cd_open_search),
                                    modifier = Modifier.size(actionIconSize)
                                )
                            }

                            IconButton(onClick = {
                                val current = SystemClock.elapsedRealtime()
                                if (current - topBarLastClick.longValue < 400L) return@IconButton
                                topBarLastClick.longValue = current
                                showSortMenu = true
                            }) {
                                Icon(
                                    Icons.AutoMirrored.Filled.Sort,
                                    contentDescription = stringResource(R.string.cd_open_sort),
                                    modifier = Modifier.size(actionIconSize)
                                )
                            }

                            DropdownMenu(
                                expanded = showSortMenu,
                                onDismissRequest = { showSortMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.sort_status_working_first)) },
                                    onClick = {
                                        viewModel.onSortChange(SortOption.STATUS_WORKING)
                                        coroutineScope.launch { listState.scrollToItem(0) }
                                        showSortMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.sort_status_not_working_first)) },
                                    onClick = {
                                        viewModel.onSortChange(SortOption.STATUS_NOT_WORKING)
                                        coroutineScope.launch { listState.scrollToItem(0) }
                                        showSortMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.sort_status_untested_first)) },
                                    onClick = {
                                        viewModel.onSortChange(SortOption.STATUS_UNTESTED)
                                        coroutineScope.launch { listState.scrollToItem(0) }
                                        showSortMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.sort_name_az)) },
                                    onClick = {
                                        viewModel.onSortChange(SortOption.TITLE)
                                        coroutineScope.launch { listState.scrollToItem(0) }
                                        showSortMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.sort_year_newest)) },
                                    onClick = {
                                        viewModel.onSortChange(SortOption.YEAR_NEW)
                                        coroutineScope.launch { listState.scrollToItem(0) }
                                        showSortMenu = false
                                    }
                                )

                                if (lockedPlatform == null) {
                                    HorizontalDivider()

                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.filter_platform_all)) },
                                        onClick = {
                                            viewModel.onPlatformFilterChange(PlatformFilter.ALL)
                                            coroutineScope.launch { listState.scrollToItem(0) }
                                            showSortMenu = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.filter_platform_pc)) },
                                        onClick = {
                                            viewModel.onPlatformFilterChange(PlatformFilter.PC)
                                            coroutineScope.launch { listState.scrollToItem(0) }
                                            showSortMenu = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.filter_platform_switch)) },
                                        onClick = {
                                            viewModel.onPlatformFilterChange(PlatformFilter.SWITCH)
                                            coroutineScope.launch { listState.scrollToItem(0) }
                                            showSortMenu = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.filter_platform_ps3)) },
                                        onClick = {
                                            viewModel.onPlatformFilterChange(PlatformFilter.PS3)
                                            coroutineScope.launch { listState.scrollToItem(0) }
                                            showSortMenu = false
                                        }
                                    )
                                }
                            }

                            IconButton(onClick = {
                                val current = SystemClock.elapsedRealtime()
                                if (current - topBarLastClick.longValue < 400L) return@IconButton
                                topBarLastClick.longValue = current
                                viewModel.syncFromRemote(context)
                            }) {
                                Icon(
                                    Icons.Filled.Sync,
                                    contentDescription = stringResource(R.string.cd_sync_remote),
                                    modifier = Modifier.size(actionIconSize)
                                )
                            }

                            IconButton(onClick = {
                                val current = SystemClock.elapsedRealtime()
                                if (current - topBarLastClick.longValue < 400L) return@IconButton
                                topBarLastClick.longValue = current

                                val turningOn = !showFavoritesOnly
                                if (turningOn) {
                                    lastNonFavIndex = listState.firstVisibleItemIndex
                                    lastNonFavOffset = listState.firstVisibleItemScrollOffset
                                    showFavoritesOnly = true
                                } else {
                                    pendingRestore = true
                                    showFavoritesOnly = false
                                }
                            }) {
                                Icon(
                                    imageVector = if (showFavoritesOnly) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                    contentDescription = stringResource(R.string.cd_show_favorites),
                                    tint = if (showFavoritesOnly) Color(0xFFE91E63) else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(actionIconSize)
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = Color.Transparent,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                        navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                        actionIconContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )

                HorizontalDivider(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .navigationBarsPadding()
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (val state = uiState) {
                is GameViewModel.UiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                is GameViewModel.UiState.Error -> {
                    val errorMessage = when (state.type) {
                        ErrorType.NO_GAMES ->
                            stringResource(R.string.error_loading_message_no_games)
                        ErrorType.PARSE_ERROR, ErrorType.UNKNOWN ->
                            stringResource(R.string.error_loading_message_generic)
                    }
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Filled.ErrorOutline,
                            null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.height(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            stringResource(R.string.error_loading_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            errorMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.reloadLocal(context) }) {
                            Text(stringResource(R.string.button_try_again))
                        }
                    }
                }

                is GameViewModel.UiState.Success -> {
                    val gamesToDisplay = if (showFavoritesOnly) {
                        games.filter { it.isFavorite }
                    } else games

                    if (gamesToDisplay.isEmpty() && showFavoritesOnly) {
                        Column(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Filled.FavoriteBorder,
                                null,
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.height(64.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                stringResource(R.string.empty_favorites),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    } else {

                        if (useGridLayout) {
                            LazyVerticalGrid(
                                columns = GridCells.Adaptive(minSize = 520.dp),
                                contentPadding = PaddingValues(
                                    horizontal = horizontalPadding,
                                    vertical = verticalPadding
                                ),
                                horizontalArrangement = Arrangement.spacedBy(18.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                gridItems(
                                    items = gamesToDisplay,
                                    key = { it.id }
                                ) { game ->
                                    GameCard(
                                        game = game,
                                        onEditStatus = { g ->
                                            selectedGame = g
                                            showEditDialog = true
                                        },
                                        onToggleFavorite = { g ->
                                            viewModel.toggleFavorite(context, g.id)
                                        },
                                        onShowTestHistory = { g ->
                                            historyGame = g
                                            showHistoryDialog = true
                                        }
                                    )
                                }
                            }
                        } else {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.TopCenter
                            ) {
                                LazyColumn(
                                    state = listState,
                                    contentPadding = PaddingValues(
                                        horizontal = horizontalPadding,
                                        vertical = verticalPadding
                                    ),
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .widthIn(max = if (isTablet) 720.dp else 999.dp)
                                ) {
                                    items(
                                        items = gamesToDisplay,
                                        key = { it.id }
                                    ) { game ->
                                        GameCard(
                                            game = game,
                                            onEditStatus = { g ->
                                                selectedGame = g
                                                showEditDialog = true
                                            },
                                            onToggleFavorite = { g ->
                                                viewModel.toggleFavorite(context, g.id)
                                            },
                                            onShowTestHistory = { g ->
                                                historyGame = g
                                                showHistoryDialog = true
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                is GameViewModel.UiState.Idle -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Filled.SnippetFolder,
                            null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.height(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            stringResource(R.string.no_games_title),
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            stringResource(R.string.no_games_subtitle),
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(onClick = { viewModel.reloadLocal(context) }) {
                            Text(stringResource(R.string.select_csv_button))
                        }
                    }
                }
            }

            if (showEditDialog && selectedGame != null) {
                EditStatusDialog(
                    game = selectedGame!!,
                    onDismiss = { showEditDialog = false },
                    onSave = { result ->
                        viewModel.updateGameStatus(
                            context = context,
                            gameId = selectedGame!!.id,
                            newStatus = result.status,

                            testedAndroidVersion = result.testedAndroidVersion,
                            testedDeviceModel = result.testedDeviceModel,

                            testedGpuModel = result.testedGpuModel,
                            testedRam = result.testedRam,
                            testedWrapper = result.testedWrapper,
                            testedPerformanceMode = result.testedPerformanceMode,

                            testedApp = result.testedApp,
                            testedAppVersion = result.testedAppVersion,
                            testedGameVersionOrBuild = result.testedGameVersionOrBuild,

                            issueType = result.issueType,
                            reproducibility = result.reproducibility,
                            workaround = result.workaround,
                            issueNote = result.issueNote,

                            emulatorBuildType = result.emulatorBuildType,
                            accuracyLevel = result.accuracyLevel,
                            resolutionScale = result.resolutionScale,
                            asyncShaderEnabled = result.asyncShaderEnabled,
                            frameSkip = result.frameSkip,

                            resolutionWidth = result.resolutionWidth,
                            resolutionHeight = result.resolutionHeight,
                            fpsMin = result.fpsMin,
                            fpsMax = result.fpsMax,

                            mediaLink = result.mediaLink
                        )

                        showEditDialog = false
                    }
                )
            }

            if (showHistoryDialog && historyGame != null) {
                TestedHistoryDialog(
                    game = historyGame!!,
                    onDismiss = { showHistoryDialog = false }
                )
            }
        }
    }
}

data class EditDialogResult(
    val status: WorkStatus,
    val testedAndroidVersion: String,
    val testedDeviceModel: String,
    val testedGpuModel: String,
    val testedRam: String,
    val testedWrapper: String,
    val testedPerformanceMode: String,
    val testedApp: String,
    val testedAppVersion: String,
    val testedGameVersionOrBuild: String,
    val issueType: IssueType,
    val reproducibility: Reproducibility,
    val workaround: String,
    val issueNote: String,
    val emulatorBuildType: EmulatorBuildType,
    val accuracyLevel: String,
    val resolutionScale: String,
    val asyncShaderEnabled: Boolean,
    val frameSkip: String,
    val resolutionWidth: String,
    val resolutionHeight: String,
    val fpsMin: String,
    val fpsMax: String,
    val mediaLink: String
)

@SuppressLint("ConfigurationScreenWidthHeight")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditStatusDialog(
    game: Game,
    onDismiss: () -> Unit,
    onSave: (EditDialogResult) -> Unit
) {
    var currentStatus by remember { mutableStateOf(WorkStatus.UNTESTED) }
    val otherLabel = stringResource(R.string.option_other)
    val customLabel = stringResource(R.string.option_custom)

    val platformLower = game.platform.lowercase()
    val isSwitchPlatform = platformLower.contains("switch") || platformLower.contains("nintendo")
    val isPcPlatform = platformLower.contains("pc") || platformLower.contains("windows")
    platformLower.contains("playstation") || platformLower.contains("ps3")

    val appOptions = when {
        isSwitchPlatform -> listOf("Yuzu", "Eden", "Citron", "Torzu", "Sumi", "Sudachi", "Strato")
        isPcPlatform -> listOf("Winlator", "GameHub")
        else -> listOf("RPCSX-UI-Android", "aPS3e")
    }

    var androidVersionSelected by remember { mutableStateOf("") }
    var androidVersionCustom by remember { mutableStateOf("") }

    var deviceModelText by remember { mutableStateOf("") }
    var gpuModelText by remember { mutableStateOf("") }

    var ramSelected by remember { mutableStateOf("") }
    var ramCustom by remember { mutableStateOf("") }

    var wrapperSelected by remember { mutableStateOf("") }
    var wrapperCustom by remember { mutableStateOf("") }

    var perfModeSelected by remember { mutableStateOf("") }
    var perfModeCustom by remember { mutableStateOf("") }


    var selectedApp by remember { mutableStateOf(appOptions.first()) }
    var appExpanded by remember { mutableStateOf(false) }
    var appVersionText by remember { mutableStateOf("") }
    var gameVersionText by remember { mutableStateOf("") }


    var selectedIssueType by remember { mutableStateOf(IssueType.CRASH) }
    var issueExpanded by remember { mutableStateOf(false) }

    var selectedRepro by remember { mutableStateOf(Reproducibility.ALWAYS) }
    var reproExpanded by remember { mutableStateOf(false) }

    var workaroundText by remember { mutableStateOf("") }
    var issueNoteText by remember { mutableStateOf("") }


    var selectedEmuBuild by remember { mutableStateOf(EmulatorBuildType.STABLE) }
    var emuExpanded by remember { mutableStateOf(false) }

    var accuracySelected by remember { mutableStateOf("") }
    var accuracyCustom by remember { mutableStateOf("") }

    var scaleSelected by remember { mutableStateOf("") }
    var scaleCustom by remember { mutableStateOf("") }

    var asyncShaderEnabled by remember { mutableStateOf(false) }

    var frameSkipSelected by remember { mutableStateOf("") }
    var frameSkipCustom by remember { mutableStateOf("") }

    val resolutionPresets = listOf(
        "640×360", "854×480", "960×540", "1280×720", "1600×900", "1920×1080", "2340×1080", "2400×1080", "2560×1440", "3200×1800", "3840×2160", customLabel
    )
    var resolutionPresetSelected by remember { mutableStateOf("") }
    var resPresetExpanded by remember { mutableStateOf(false) }

    var resW by remember { mutableStateOf("") }
    var resH by remember { mutableStateOf("") }

    var fpsFrom by remember { mutableStateOf("") }
    var fpsTo by remember { mutableStateOf("") }

    var mediaLinkText by remember { mutableStateOf("") }

    val androidVersions = listOf("16","15", "14", "13", "12", "11", "10", "9","8","7", otherLabel)
    val ramOptions = listOf("4 GB", "6 GB", "8 GB", "12 GB", "16 GB","24 GB", otherLabel)

    val wrapperOptions = when {
        isPcPlatform -> listOf("DXVK", "VKD3D", "WineD3D", "OpenGL", otherLabel)
        else -> listOf("Vulkan", "OpenGL", "D3D wrapper", otherLabel)
    }

    val perfModeOptions = listOf("Extreme performance", "Performance", "Balanced", "Quality", otherLabel)
    val accuracyOptions = listOf("Performance", "Balanced", "Accuracy", otherLabel)
    val scaleOptions = listOf("0.5x","0.75x", "1x","1.25x", "1.5x","1.75x", "2x","2.25x","2.5x","2.75x","3x", otherLabel)
    val frameSkipOptions = listOf("0", "1", "2", otherLabel)

    val showEmulatorSettings = !isPcPlatform
    val showWrapper = true
    val showPerfMode = true

    val androidVersionFinal =
        if (androidVersionSelected == otherLabel) androidVersionCustom.trim()
        else androidVersionSelected.trim()

    val ramFinal =
        if (ramSelected == otherLabel) ramCustom.trim()
        else ramSelected.trim()

    val wrapperFinal =
        if (wrapperSelected == otherLabel) wrapperCustom.trim()
        else wrapperSelected.trim()

    val perfModeFinal =
        if (perfModeSelected == otherLabel) perfModeCustom.trim()
        else perfModeSelected.trim()

    val accuracyFinal =
        if (accuracySelected == otherLabel) accuracyCustom.trim()
        else accuracySelected.trim()

    val scaleFinal =
        if (scaleSelected == otherLabel) scaleCustom.trim()
        else scaleSelected.trim()

    val frameSkipFinal =
        if (frameSkipSelected == otherLabel) frameSkipCustom.trim()
        else frameSkipSelected.trim()

    LaunchedEffect(resolutionPresetSelected) {
        if (resolutionPresetSelected.isNotBlank() &&
            resolutionPresetSelected != customLabel
        ) {
            val parts = resolutionPresetSelected.split("×")
            if (parts.size == 2) {
                resW = parts[0]
                resH = parts[1]
            }
        }
    }
    val isFormValid by remember(
        currentStatus,
        androidVersionFinal,
        deviceModelText, gpuModelText, ramFinal,
        wrapperFinal, perfModeFinal,
        selectedApp, appVersionText, gameVersionText,
        accuracyFinal, scaleFinal, frameSkipFinal,
        resW, resH, fpsFrom, fpsTo,
        issueNoteText
    ) {
        derivedStateOf {
            val baseValid =
                androidVersionFinal.isNotEmpty() &&
                        deviceModelText.trim().isNotEmpty() &&
                        gpuModelText.trim().isNotEmpty() &&
                        ramFinal.isNotEmpty() &&
                        wrapperFinal.isNotEmpty() &&
                        perfModeFinal.isNotEmpty() &&
                        selectedApp.trim().isNotEmpty() &&
                        appVersionText.trim().isNotEmpty() &&
                        gameVersionText.trim().isNotEmpty() &&
                        resW.trim().isNotEmpty() &&
                        resH.trim().isNotEmpty() &&
                        fpsFrom.trim().isNotEmpty() &&
                        fpsTo.trim().isNotEmpty()

            val issueValid =
                currentStatus != WorkStatus.NOT_WORKING || issueNoteText.trim().isNotEmpty()

            val emuValid =
                !showEmulatorSettings || (
                        accuracyFinal.isNotEmpty() &&
                                scaleFinal.isNotEmpty() &&
                                frameSkipFinal.isNotEmpty()
                        )

            baseValid && issueValid && emuValid
        }
    }

    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp
    val isTablet = screenWidthDp >= 600

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = if (isTablet) 560.dp else 999.dp)
        ) {
            val scrollState = rememberScrollState()
            val maxDialogHeight = configuration.screenHeightDp.dp * 0.9f
            val bringIntoViewRequester = remember { BringIntoViewRequester() }
            val scope = rememberCoroutineScope()

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = maxDialogHeight)
                    .verticalScroll(scrollState)
                    .imePadding()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Text(
                    text = stringResource(R.string.dialog_edit_status_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = stringResource(R.string.dialog_edit_status_description, game.title),
                    style = MaterialTheme.typography.bodySmall
                )

                Spacer(modifier = Modifier.height(12.dp))

                WorkStatusRadioGroup(
                    selected = currentStatus,
                    onSelectedChange = { currentStatus = it }
                )

                Spacer(modifier = Modifier.height(16.dp))
                SectionHeader(stringResource(R.string.section_device_env))

                DropdownWithCustom(
                    labelRes = R.string.label_android_version,
                    options = androidVersions,
                    selected = androidVersionSelected,
                    onSelectedChange = { androidVersionSelected = it },
                    customValue = androidVersionCustom,
                    onCustomChange = { androidVersionCustom = it },
                    customPlaceholderRes = R.string.label_android_version,
                    otherLabel = otherLabel
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = deviceModelText,
                    onValueChange = { deviceModelText = it },
                    label = { Text(stringResource(R.string.label_device_model)) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = gpuModelText,
                    onValueChange = { gpuModelText = it },
                    label = { Text(stringResource(R.string.label_gpu_model)) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                DropdownWithCustom(
                    labelRes = R.string.label_ram,
                    options = ramOptions,
                    selected = ramSelected,
                    onSelectedChange = { ramSelected = it },
                    customValue = ramCustom,
                    onCustomChange = { ramCustom = it },
                    customPlaceholderRes = R.string.label_ram,
                    otherLabel = otherLabel
                )

                Spacer(modifier = Modifier.height(10.dp))

                if (showWrapper) {
                    DropdownWithCustom(
                        labelRes = R.string.label_wrapper,
                        options = wrapperOptions,
                        selected = wrapperSelected,
                        onSelectedChange = { wrapperSelected = it },
                        customValue = wrapperCustom,
                        onCustomChange = { wrapperCustom = it },
                        customPlaceholderRes = R.string.label_wrapper,
                        otherLabel = otherLabel
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }

                if (showPerfMode) {
                    DropdownWithCustom(
                        labelRes = R.string.label_performance_mode,
                        options = perfModeOptions,
                        selected = perfModeSelected,
                        onSelectedChange = { perfModeSelected = it },
                        customValue = perfModeCustom,
                        onCustomChange = { perfModeCustom = it },
                        customPlaceholderRes = R.string.label_performance_mode,
                        otherLabel = otherLabel
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                SectionHeader(stringResource(R.string.section_app_game))

                ExposedDropdownMenuBox(
                    expanded = appExpanded,
                    onExpandedChange = { appExpanded = !appExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedApp,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.dialog_edit_status_app_label)) },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = appExpanded)
                        },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )

                    DropdownMenu(
                        expanded = appExpanded,
                        onDismissRequest = { appExpanded = false }
                    ) {
                        appOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    selectedApp = option
                                    appExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = appVersionText,
                    onValueChange = { appVersionText = it },
                    label = { Text(stringResource(R.string.dialog_edit_status_app_version_label)) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = gameVersionText,
                    onValueChange = { gameVersionText = it },
                    label = { Text(stringResource(R.string.label_game_version_build)) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                if (currentStatus == WorkStatus.NOT_WORKING) {
                    Spacer(modifier = Modifier.height(16.dp))
                    SectionHeader(stringResource(R.string.section_issue_details))

                    ExposedDropdownMenuBox(
                        expanded = issueExpanded,
                        onExpandedChange = { issueExpanded = !issueExpanded }
                    ) {
                        OutlinedTextField(
                            value = stringResource(issueTypeToLabel(selectedIssueType)),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(R.string.label_issue_type)) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = issueExpanded) },
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )

                        DropdownMenu(
                            expanded = issueExpanded,
                            onDismissRequest = { issueExpanded = false }
                        ) {
                            IssueType.entries.forEach { type ->
                                DropdownMenuItem(
                                    text = { Text(stringResource(issueTypeToLabel(type))) },
                                    onClick = {
                                        selectedIssueType = type
                                        issueExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    ExposedDropdownMenuBox(
                        expanded = reproExpanded,
                        onExpandedChange = { reproExpanded = !reproExpanded }
                    ) {
                        OutlinedTextField(
                            value = stringResource(reproToLabel(selectedRepro)),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(R.string.label_reproducibility)) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = reproExpanded) },
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )

                        DropdownMenu(
                            expanded = reproExpanded,
                            onDismissRequest = { reproExpanded = false }
                        ) {
                            Reproducibility.entries.forEach { r ->
                                DropdownMenuItem(
                                    text = { Text(stringResource(reproToLabel(r))) },
                                    onClick = {
                                        selectedRepro = r
                                        reproExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = workaroundText,
                        onValueChange = { workaroundText = it },
                        label = { Text(stringResource(R.string.label_workaround)) },
                        minLines = 1,
                        maxLines = 2,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = issueNoteText,
                        onValueChange = { issueNoteText = it },
                        label = { Text(stringResource(R.string.dialog_issue_note_label)) },
                        minLines = 2,
                        maxLines = 4,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    if (issueNoteText.isNotBlank()) issueNoteText = ""
                    if (workaroundText.isNotBlank()) workaroundText = ""
                }

                if (showEmulatorSettings) {
                    Spacer(modifier = Modifier.height(16.dp))
                    SectionHeader(stringResource(R.string.section_emulator_settings))

                    ExposedDropdownMenuBox(
                        expanded = emuExpanded,
                        onExpandedChange = { emuExpanded = !emuExpanded }
                    ) {
                        OutlinedTextField(
                            value = stringResource(emuBuildToLabel(selectedEmuBuild)),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(R.string.label_emulator_build_type)) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = emuExpanded) },
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )

                        DropdownMenu(
                            expanded = emuExpanded,
                            onDismissRequest = { emuExpanded = false }
                        ) {
                            EmulatorBuildType.entries.forEach { e ->
                                DropdownMenuItem(
                                    text = { Text(stringResource(emuBuildToLabel(e))) },
                                    onClick = {
                                        selectedEmuBuild = e
                                        emuExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    DropdownWithCustom(
                        labelRes = R.string.label_accuracy_level,
                        options = accuracyOptions,
                        selected = accuracySelected,
                        onSelectedChange = { accuracySelected = it },
                        customValue = accuracyCustom,
                        onCustomChange = { accuracyCustom = it },
                        customPlaceholderRes = R.string.label_accuracy_level,
                        otherLabel = otherLabel
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    DropdownWithCustom(
                        labelRes = R.string.label_resolution_scale,
                        options = scaleOptions,
                        selected = scaleSelected,
                        onSelectedChange = { scaleSelected = it },
                        customValue = scaleCustom,
                        onCustomChange = { scaleCustom = it },
                        customPlaceholderRes = R.string.label_resolution_scale,
                        otherLabel = otherLabel
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(
                            checked = asyncShaderEnabled,
                            onCheckedChange = { asyncShaderEnabled = it }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.label_async_shader))
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    DropdownWithCustom(
                        labelRes = R.string.label_frame_skip,
                        options = frameSkipOptions,
                        selected = frameSkipSelected,
                        onSelectedChange = { frameSkipSelected = it },
                        customValue = frameSkipCustom,
                        onCustomChange = { frameSkipCustom = it },
                        customPlaceholderRes = R.string.label_frame_skip,
                        otherLabel = otherLabel
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                SectionHeader(stringResource(R.string.section_result_metrics))

                ExposedDropdownMenuBox(
                    expanded = resPresetExpanded,
                    onExpandedChange = { resPresetExpanded = !resPresetExpanded }
                ) {
                    OutlinedTextField(
                        value = resolutionPresetSelected,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.dialog_edit_status_resolution_label)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = resPresetExpanded) },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )

                    DropdownMenu(
                        expanded = resPresetExpanded,
                        onDismissRequest = { resPresetExpanded = false }
                    ) {
                        resolutionPresets.forEach { p ->
                            DropdownMenuItem(
                                text = { Text(p) },
                                onClick = {
                                    resolutionPresetSelected = p
                                    resPresetExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = resW,
                        onValueChange = { resW = it },
                        label = { Text(stringResource(R.string.resolution_width_hint)) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )

                    Spacer(modifier = Modifier.padding(horizontal = 8.dp))
                    Text(text = "×", style = MaterialTheme.typography.titleMedium)

                    Spacer(modifier = Modifier.padding(horizontal = 8.dp))
                    OutlinedTextField(
                        value = resH,
                        onValueChange = { resH = it },
                        label = { Text(stringResource(R.string.resolution_height_hint)) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = stringResource(R.string.dialog_edit_status_fps_label),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = fpsFrom,
                        onValueChange = { fpsFrom = it },
                        label = { Text(stringResource(R.string.fps_min_hint)) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )

                    Spacer(modifier = Modifier.padding(horizontal = 8.dp))
                    Text(text = "–", style = MaterialTheme.typography.titleMedium)

                    Spacer(modifier = Modifier.padding(horizontal = 8.dp))
                    OutlinedTextField(
                        value = fpsTo,
                        onValueChange = { fpsTo = it },
                        label = { Text(stringResource(R.string.fps_max_hint)) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                SectionHeader(stringResource(R.string.section_media))

                OutlinedTextField(
                    value = mediaLinkText,
                    onValueChange = { mediaLinkText = it },
                    label = { Text(stringResource(R.string.label_media_link)) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .bringIntoViewRequester(bringIntoViewRequester)
                        .onFocusEvent { state ->
                            if (state.isFocused) {
                                scope.launch { bringIntoViewRequester.bringIntoView() }
                            }
                        }
                )

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider()

                if (!isFormValid) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.dialog_fill_all_fields),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.button_cancel))
                    }

                    TextButton(
                        enabled = isFormValid,
                        onClick = {
                            onSave(
                                EditDialogResult(
                                    status = currentStatus,

                                    testedAndroidVersion = androidVersionFinal,
                                    testedDeviceModel = deviceModelText.trim(),
                                    testedGpuModel = gpuModelText.trim(),
                                    testedRam = ramFinal,
                                    testedWrapper = wrapperFinal,
                                    testedPerformanceMode = perfModeFinal,

                                    testedApp = selectedApp.trim(),
                                    testedAppVersion = appVersionText.trim(),
                                    testedGameVersionOrBuild = gameVersionText.trim(),

                                    issueType = selectedIssueType,
                                    reproducibility = selectedRepro,
                                    workaround = workaroundText.trim(),
                                    issueNote = issueNoteText.trim(),

                                    emulatorBuildType = selectedEmuBuild,
                                    accuracyLevel = if (showEmulatorSettings) accuracyFinal else "",
                                    resolutionScale = if (showEmulatorSettings) scaleFinal else "",
                                    asyncShaderEnabled = if (showEmulatorSettings) asyncShaderEnabled else false,
                                    frameSkip = if (showEmulatorSettings) frameSkipFinal else "",

                                    resolutionWidth = resW.trim(),
                                    resolutionHeight = resH.trim(),
                                    fpsMin = fpsFrom.trim(),
                                    fpsMax = fpsTo.trim(),

                                    mediaLink = mediaLinkText.trim()
                                )
                            )
                        }
                    ) {
                        Text(stringResource(R.string.button_save))
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold
    )
    Spacer(modifier = Modifier.height(8.dp))
}

private fun issueTypeToLabel(type: IssueType): Int = when (type) {
    IssueType.CRASH -> R.string.issue_type_crash
    IssueType.BLACK_SCREEN -> R.string.issue_type_black_screen
    IssueType.SOFTLOCK -> R.string.issue_type_softlock
    IssueType.GRAPHICS_GLITCHES -> R.string.issue_type_graphics
    IssueType.AUDIO_ISSUES -> R.string.issue_type_audio
    IssueType.CONTROLS_NOT_WORKING -> R.string.issue_type_controls
    IssueType.SLOW_PERFORMANCE -> R.string.issue_type_performance
}

private fun reproToLabel(r: Reproducibility): Int = when (r) {
    Reproducibility.ALWAYS -> R.string.repro_always
    Reproducibility.OFTEN -> R.string.repro_often
    Reproducibility.RARE -> R.string.repro_rare
    Reproducibility.ONCE -> R.string.repro_once
}

private fun emuBuildToLabel(e: EmulatorBuildType): Int = when (e) {
    EmulatorBuildType.STABLE -> R.string.emu_build_stable
    EmulatorBuildType.CANARY -> R.string.emu_build_canary
    EmulatorBuildType.GIT_HASH -> R.string.emu_build_git
}

@SuppressLint("ConfigurationScreenWidthHeight")
@Composable
fun TestedHistoryDialog(
    game: Game,
    onDismiss: () -> Unit
) {
    @Composable
    fun statusText(status: WorkStatus): String =
        when (status) {
            WorkStatus.WORKING -> stringResource(R.string.work_status_working)
            WorkStatus.UNTESTED -> stringResource(R.string.work_status_untested)
            WorkStatus.NOT_WORKING -> stringResource(R.string.work_status_not_working)
        }

    val uriHandler = LocalUriHandler.current

    val sortedTests = game.testResults.sortedByDescending { it.updatedAtMillis }
    var selected by remember { mutableStateOf(sortedTests.firstOrNull()) }

    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp
    val isTablet = screenWidthDp >= 600

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = if (isTablet) 560.dp else 999.dp)
        ) {
            val scrollState = rememberScrollState()
            val maxDialogHeight = configuration.screenHeightDp.dp * 0.9f

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = maxDialogHeight)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Text(
                    text = stringResource(R.string.dialog_test_history_title, game.title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(12.dp))

                if (sortedTests.isEmpty()) {
                    Text(stringResource(R.string.dialog_test_history_empty))
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text(stringResource(R.string.button_ok))
                        }
                    }
                    return@Column
                }

                sortedTests.forEach { test ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = selected == test,
                            onClick = { selected = test }
                        )
                        Column {
                            Text(
                                text = test.testedDateFormatted.ifBlank { "—" },
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )

                            val device = test.testedDeviceModel.ifBlank {
                                stringResource(R.string.unknown_device)
                            }
                            val stText = statusText(test.status)

                            Text(
                                text = "$device • $stText",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                selected?.let { t ->
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = stringResource(R.string.test_history_details),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Text(stringResource(R.string.test_history_status, statusText(t.status)))

                    if (t.testedAndroidVersion.isNotBlank()) {
                        Text(stringResource(R.string.test_history_android_version, t.testedAndroidVersion))
                    }
                    if (t.testedDeviceModel.isNotBlank()) {
                        Text(stringResource(R.string.test_history_device, t.testedDeviceModel))
                    }
                    if (t.testedGpuModel.isNotBlank()) {
                        Text(stringResource(R.string.test_history_gpu_model, t.testedGpuModel))
                    }
                    if (t.testedRam.isNotBlank()) {
                        Text(stringResource(R.string.test_history_ram, t.testedRam))
                    }
                    if (t.testedWrapper.isNotBlank()) {
                        Text(stringResource(R.string.test_history_wrapper, t.testedWrapper))
                    }
                    if (t.testedPerformanceMode.isNotBlank()) {
                        Text(stringResource(R.string.test_history_perf_mode, t.testedPerformanceMode))
                    }
                    if (t.testedGameVersionOrBuild.isNotBlank()) {
                        Text(stringResource(R.string.test_history_game_version_build, t.testedGameVersionOrBuild))
                    }

                    if (t.status == WorkStatus.NOT_WORKING) {
                        Text(stringResource(R.string.test_history_issue_type, stringResource(issueTypeToLabel(t.issueType))))
                        Text(stringResource(R.string.test_history_repro, stringResource(reproToLabel(t.reproducibility))))

                        if (t.workaround.isNotBlank()) {
                            Text(stringResource(R.string.test_history_workaround, t.workaround))
                        }
                    }

                    if (t.emulatorBuildType != EmulatorBuildType.STABLE || t.accuracyLevel.isNotBlank() ||
                        t.resolutionScale.isNotBlank() || t.frameSkip.isNotBlank()
                    ) {
                        Text(stringResource(R.string.test_history_emulator_build, stringResource(emuBuildToLabel(t.emulatorBuildType))))
                        if (t.accuracyLevel.isNotBlank()) {
                            Text(stringResource(R.string.test_history_accuracy, t.accuracyLevel))
                        }
                        if (t.resolutionScale.isNotBlank()) {
                            Text(stringResource(R.string.test_history_resolution_scale, t.resolutionScale))
                        }
                        Text(
                            stringResource(
                                R.string.test_history_async_shader,
                                if (t.asyncShaderEnabled) stringResource(R.string.value_on) else stringResource(R.string.value_off)
                            )
                        )
                        if (t.frameSkip.isNotBlank()) {
                            Text(stringResource(R.string.test_history_frame_skip, t.frameSkip))
                        }
                    }

                    val hasRes = t.resolutionWidth.isNotBlank() && t.resolutionHeight.isNotBlank()
                    val hasFps = t.fpsMin.isNotBlank() && t.fpsMax.isNotBlank()

                    if (hasRes) {
                        Text(
                            stringResource(
                                R.string.test_history_resolution,
                                "${t.resolutionWidth}×${t.resolutionHeight}"
                            )
                        )
                    }

                    if (hasFps) {
                        Text(
                            stringResource(
                                R.string.test_history_fps,
                                "${t.fpsMin}–${t.fpsMax}"
                            )
                        )
                    }

                    if (t.testedApp.isNotBlank()) {
                        val ver = if (t.testedAppVersion.isNotBlank()) " v${t.testedAppVersion}" else ""
                        Text(stringResource(R.string.test_history_app, "${t.testedApp}$ver"))
                    }

                    if (t.issueNote.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(stringResource(R.string.test_history_issue, t.issueNote))
                    }

                    if (t.mediaLink.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = stringResource(R.string.test_history_media, t.mediaLink),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable {
                                runCatching { uriHandler.openUri(t.mediaLink) }
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider()

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.button_ok))
                    }
                }
            }
        }
    }
}

@Composable
fun WorkStatusRadioGroup(
    selected: WorkStatus,
    onSelectedChange: (WorkStatus) -> Unit
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(
                selected = selected == WorkStatus.WORKING,
                onClick = { onSelectedChange(WorkStatus.WORKING) }
            )
            Text(text = stringResource(id = R.string.work_status_working))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(
                selected = selected == WorkStatus.UNTESTED,
                onClick = { onSelectedChange(WorkStatus.UNTESTED) }
            )
            Text(text = stringResource(id = R.string.work_status_untested))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(
                selected = selected == WorkStatus.NOT_WORKING,
                onClick = { onSelectedChange(WorkStatus.NOT_WORKING) }
            )
            Text(text = stringResource(id = R.string.work_status_not_working))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DropdownWithCustom(
    labelRes: Int,
    options: List<String>,
    selected: String,
    onSelectedChange: (String) -> Unit,
    customValue: String,
    onCustomChange: (String) -> Unit,
    customPlaceholderRes: Int,
    otherLabel: String
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(labelRes)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { opt ->
                DropdownMenuItem(
                    text = { Text(opt) },
                    onClick = {
                        onSelectedChange(opt)
                        expanded = false
                    }
                )
            }
        }
    }

    if (selected == otherLabel) {
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = customValue,
            onValueChange = onCustomChange,
            label = { Text(stringResource(customPlaceholderRes)) },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        )
    }
}