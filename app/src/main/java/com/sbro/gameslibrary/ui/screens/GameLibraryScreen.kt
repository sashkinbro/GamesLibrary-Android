package com.sbro.gameslibrary.ui.screens

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.os.SystemClock
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sbro.gameslibrary.R
import com.sbro.gameslibrary.components.Game
import com.sbro.gameslibrary.components.GameCard
import com.sbro.gameslibrary.components.WorkStatus
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
                                keyboardActions = KeyboardActions(
                                    onSearch = { focusManager.clearFocus() }
                                ),
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
                                            stringResource(
                                                R.string.subtitle_games_count,
                                                games.size
                                            )
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
                    onSave = { status, device, gpu, app, appVersion, issueNote, resW, resH, fpsMin, fpsMax ->

                        viewModel.updateGameStatus(
                            context = context,
                            gameId = selectedGame!!.id,
                            newStatus = status,
                            newDevice = device,
                            testedGpuDriver = gpu,
                            testedApp = app,
                            testedAppVersion = appVersion,
                            issueNote = issueNote,
                            resolutionWidth = resW,
                            resolutionHeight = resH,
                            fpsMin = fpsMin,
                            fpsMax = fpsMax
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

@SuppressLint("ConfigurationScreenWidthHeight")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditStatusDialog(
    game: Game,
    onDismiss: () -> Unit,
    onSave: (
        WorkStatus,
        String,
        String,
        String,
        String,
        String,
        String,
        String,
        String,
        String
    ) -> Unit
) {
    var currentStatus by remember { mutableStateOf(WorkStatus.UNTESTED) }
    var deviceText by remember { mutableStateOf("") }
    var gpuText by remember { mutableStateOf("") }
    var issueText by remember { mutableStateOf("") }

    val platform = game.platform.lowercase()
    val appOptions = when {
        platform.contains("switch") || platform.contains("nintendo") -> {
            listOf("Yuzu", "Eden", "Citron", "Torzu", "Sumi", "Sudachi", "Strato")
        }
        platform.contains("pc") || platform.contains("windows") -> {
            listOf("Winlator", "GameHub")
        }
        else -> {
            listOf("RPCSX-UI-Android", "aPS3e")
        }
    }

    var selectedApp by remember { mutableStateOf(appOptions.first()) }
    var appExpanded by remember { mutableStateOf(false) }
    var appVersion by remember { mutableStateOf("") }
    var resW by remember { mutableStateOf("") }
    var resH by remember { mutableStateOf("") }
    var fpsFrom by remember { mutableStateOf("") }
    var fpsTo by remember { mutableStateOf("") }

    val isFormValid by remember(
        currentStatus,
        deviceText,
        gpuText,
        selectedApp,
        appVersion,
        issueText,
        resW,
        resH,
        fpsFrom,
        fpsTo
    ) {
        derivedStateOf {
            val baseValid =
                deviceText.trim().isNotEmpty() &&
                        gpuText.trim().isNotEmpty() &&
                        selectedApp.trim().isNotEmpty() &&
                        appVersion.trim().isNotEmpty() &&
                        resW.trim().isNotEmpty() &&
                        resH.trim().isNotEmpty() &&
                        fpsFrom.trim().isNotEmpty() &&
                        fpsTo.trim().isNotEmpty()

            val issueValid =
                currentStatus != WorkStatus.NOT_WORKING || issueText.trim().isNotEmpty()

            baseValid && issueValid
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

                Spacer(modifier = Modifier.height(12.dp))

                if (currentStatus == WorkStatus.NOT_WORKING) {
                    OutlinedTextField(
                        value = issueText,
                        onValueChange = { issueText = it },
                        label = { Text(stringResource(R.string.dialog_issue_note_label)) },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        maxLines = 4,
                        shape = RoundedCornerShape(16.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                } else {
                    if (issueText.isNotBlank()) issueText = ""
                }

                OutlinedTextField(
                    value = deviceText,
                    onValueChange = { deviceText = it },
                    label = { Text(stringResource(R.string.dialog_edit_status_device_label)) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = gpuText,
                    onValueChange = { gpuText = it },
                    label = { Text(stringResource(R.string.dialog_edit_status_gpu_label)) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.dialog_edit_status_resolution_label),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))

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
                        keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next)
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
                        keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next)
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
                        keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next)
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
                        keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

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
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
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

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = appVersion,
                    onValueChange = { appVersion = it },
                    label = { Text(stringResource(R.string.dialog_edit_status_app_version_label)) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .bringIntoViewRequester(bringIntoViewRequester)
                        .onFocusEvent { state ->
                            if (state.isFocused) {
                                scope.launch {
                                    bringIntoViewRequester.bringIntoView()
                                }
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
                                currentStatus,
                                deviceText.trim(),
                                gpuText.trim(),
                                selectedApp.trim(),
                                appVersion.trim(),
                                issueText.trim(),
                                resW.trim(),
                                resH.trim(),
                                fpsFrom.trim(),
                                fpsTo.trim()
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

                            val device = test.testedDevice.ifBlank {
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

                    val stTextSelected = statusText(t.status)
                    Text(stringResource(R.string.test_history_status, stTextSelected))

                    if (t.testedDevice.isNotBlank()) {
                        Text(stringResource(R.string.test_history_device, t.testedDevice))
                    }

                    if (t.testedGpuDriver.isNotBlank()) {
                        Text(stringResource(R.string.test_history_gpu_driver, t.testedGpuDriver))
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
                        val ver =
                            if (t.testedAppVersion.isNotBlank()) " v${t.testedAppVersion}" else ""
                        Text(stringResource(R.string.test_history_app, "${t.testedApp}$ver"))
                    }

                    if (t.issueNote.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(stringResource(R.string.test_history_issue, t.issueNote))
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
