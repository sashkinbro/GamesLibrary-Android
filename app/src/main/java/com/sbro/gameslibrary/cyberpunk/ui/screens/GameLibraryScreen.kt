package com.sbro.gameslibrary.cyberpunk.ui.screens

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.os.SystemClock
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sbro.gameslibrary.R
import com.sbro.gameslibrary.components.Game
// Use the cyberpunk variant of GameCard instead of the classic version
import com.sbro.gameslibrary.cyberpunk.components.GameCard
import com.sbro.gameslibrary.viewmodel.ErrorType
import com.sbro.gameslibrary.viewmodel.GameViewModel
import com.sbro.gameslibrary.viewmodel.PlatformFilter
import com.sbro.gameslibrary.viewmodel.SortOption
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch

private val CyberYellow = Color(0xFFFCEE0A)
private val CyberRed = Color(0xFFFF003C)
private val CyberBlue = Color(0xFF00F0FF)
private val CyberBlack = Color(0xFF050505)
private val CyberDark = Color(0xFF0F0F0F)

@SuppressLint("ConfigurationScreenWidthHeight")
@OptIn(ExperimentalMaterial3Api::class, FlowPreview::class)
@Composable
fun GameLibraryScreen(
    viewModel: GameViewModel = viewModel(),
    showBackButton: Boolean = false,
    onBack: () -> Unit = {},
    lockedPlatform: PlatformFilter? = null,
    onOpenDetails: (Game) -> Unit = {},
    onOpenEditStatus: (Game) -> Unit = {},
    onOpenTestHistory: (Game) -> Unit = {}
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
        if (lockedPlatform != null) {
            viewModel.onPlatformFilterChange(lockedPlatform)
        } else {
            viewModel.onPlatformFilterChange(PlatformFilter.ALL)
        }
    }

    val games by viewModel.decoratedGames.collectAsState()
    val searchText by viewModel.searchText.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    val listState = rememberLazyListState()
    val gridState = rememberLazyGridState()

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
    val topBarLastClick = remember { mutableLongStateOf(0L) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberBlack)
    ) {
        CyberGridBackground()
        ScanlinesEffect()
        VignetteEffect()

        Scaffold(
            contentWindowInsets = WindowInsets(0),
            containerColor = Color.Transparent,
            topBar = {
                Column {
                    TopAppBar(
                        modifier = Modifier.statusBarsPadding(),
                        title = {
                            if (isSearchActive) {
                                OutlinedTextField(
                                    value = searchText,
                                    onValueChange = { viewModel.onSearchChange(it) },
                                    placeholder = {
                                        Text(
                                            stringResource(R.string.search_hint),
                                            color = CyberYellow.copy(alpha = 0.7f),
                                            fontFamily = FontFamily.Monospace
                                        )
                                    },
                                    singleLine = true,
                                    shape = RoundedCornerShape(24.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .focusRequester(focusRequester),
                                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                        imeAction = ImeAction.Search
                                    ),
                                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                                        onSearch = { focusManager.clearFocus() }
                                    ),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = CyberRed,
                                        unfocusedBorderColor = CyberRed.copy(alpha = 0.35f),
                                        focusedTextColor = CyberYellow,
                                        unfocusedTextColor = CyberYellow,
                                        cursorColor = CyberYellow
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

                                        GlitchText(
                                            text = stringResource(titleRes).uppercase(),
                                            style = MaterialTheme.typography.titleLarge.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace,
                                                letterSpacing = 1.sp
                                            )
                                        )

                                        if (showFavoritesOnly) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Icon(
                                                Icons.Filled.Favorite,
                                                contentDescription = null,
                                                modifier = Modifier.height(16.dp),
                                                tint = CyberRed
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
                                        color = CyberYellow.copy(alpha = 0.7f),
                                        fontFamily = FontFamily.Monospace
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
                                    },
                                    modifier = Modifier
                                        .padding(start = 6.dp)
                                        .clip(CutCornerShape(topEnd = 10.dp, bottomStart = 10.dp))
                                        .background(CyberDark)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = null,
                                        modifier = Modifier.size(actionIconSize),
                                        tint = CyberRed
                                    )
                                }
                            }
                        },
                        actions = {
                            if (isSearchActive) {
                                IconButton(
                                    onClick = {
                                        isSearchActive = false
                                        viewModel.onSearchChange("")
                                    }
                                ) {
                                    Icon(
                                        Icons.Filled.Close,
                                        contentDescription = stringResource(R.string.cd_close_search),
                                        modifier = Modifier.size(actionIconSize),
                                        tint = CyberYellow
                                    )
                                }
                            } else {
                                IconButton(
                                    onClick = {
                                        val current = SystemClock.elapsedRealtime()
                                        if (current - topBarLastClick.longValue < 400L) return@IconButton
                                        topBarLastClick.longValue = current
                                        isSearchActive = true
                                    }
                                ) {
                                    Icon(
                                        Icons.Filled.Search,
                                        contentDescription = stringResource(R.string.cd_open_search),
                                        modifier = Modifier.size(actionIconSize),
                                        tint = CyberBlue
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        val current = SystemClock.elapsedRealtime()
                                        if (current - topBarLastClick.longValue < 400L) return@IconButton
                                        topBarLastClick.longValue = current
                                        showSortMenu = true
                                    }
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.Sort,
                                        contentDescription = stringResource(R.string.cd_open_sort),
                                        modifier = Modifier.size(actionIconSize),
                                        tint = CyberYellow
                                    )
                                }

                                DropdownMenu(
                                    expanded = showSortMenu,
                                    onDismissRequest = { showSortMenu = false },
                                    modifier = Modifier
                                        .background(
                                            CyberDark,
                                            shape = CutCornerShape(topEnd = 12.dp, bottomStart = 12.dp)
                                        ),
                                    shape = CutCornerShape(topEnd = 12.dp, bottomStart = 12.dp),
                                    containerColor = CyberDark,
                                    tonalElevation = 0.dp,
                                    shadowElevation = 12.dp,
                                    border = BorderStroke(
                                        1.dp,
                                        Brush.horizontalGradient(listOf(CyberRed, CyberYellow))
                                    )
                                ) {
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                stringResource(R.string.sort_original),
                                                color = CyberYellow,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        },
                                        onClick = {
                                            viewModel.onSortChange(SortOption.ORIGINAL)
                                            coroutineScope.launch { listState.scrollToItem(0) }
                                            showSortMenu = false
                                        },
                                        colors = MenuDefaults.itemColors(
                                            textColor = CyberYellow,
                                            leadingIconColor = CyberYellow,
                                            trailingIconColor = CyberYellow,
                                            disabledTextColor = CyberYellow.copy(alpha = 0.4f)
                                        )
                                    )

                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                stringResource(R.string.sort_rating_high),
                                                color = CyberYellow,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        },
                                        onClick = {
                                            viewModel.onSortChange(SortOption.RATING_HIGH)
                                            coroutineScope.launch { listState.scrollToItem(0) }
                                            showSortMenu = false
                                        },
                                        colors = MenuDefaults.itemColors(
                                            textColor = CyberYellow,
                                            leadingIconColor = CyberYellow,
                                            trailingIconColor = CyberYellow,
                                            disabledTextColor = CyberYellow.copy(alpha = 0.4f)
                                        )
                                    )

                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                stringResource(R.string.sort_genre_az),
                                                color = CyberYellow,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        },
                                        onClick = {
                                            viewModel.onSortChange(SortOption.GENRE_AZ)
                                            coroutineScope.launch { listState.scrollToItem(0) }
                                            showSortMenu = false
                                        },
                                        colors = MenuDefaults.itemColors(
                                            textColor = CyberYellow,
                                            leadingIconColor = CyberYellow,
                                            trailingIconColor = CyberYellow,
                                            disabledTextColor = CyberYellow.copy(alpha = 0.4f)
                                        )
                                    )

                                    HorizontalDivider(
                                        color = CyberRed.copy(alpha = 0.6f),
                                        thickness = 1.dp
                                    )

                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                stringResource(R.string.sort_name_az),
                                                color = CyberYellow,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        },
                                        onClick = {
                                            viewModel.onSortChange(SortOption.TITLE)
                                            coroutineScope.launch { listState.scrollToItem(0) }
                                            showSortMenu = false
                                        },
                                        colors = MenuDefaults.itemColors(
                                            textColor = CyberYellow,
                                            leadingIconColor = CyberYellow,
                                            trailingIconColor = CyberYellow,
                                            disabledTextColor = CyberYellow.copy(alpha = 0.4f)
                                        )
                                    )

                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                stringResource(R.string.sort_year_newest),
                                                color = CyberYellow,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        },
                                        onClick = {
                                            viewModel.onSortChange(SortOption.YEAR_NEW)
                                            coroutineScope.launch { listState.scrollToItem(0) }
                                            showSortMenu = false
                                        },
                                        colors = MenuDefaults.itemColors(
                                            textColor = CyberYellow,
                                            leadingIconColor = CyberYellow,
                                            trailingIconColor = CyberYellow,
                                            disabledTextColor = CyberYellow.copy(alpha = 0.4f)
                                        )
                                    )

                                    if (lockedPlatform == null) {
                                        HorizontalDivider(
                                            color = CyberRed.copy(alpha = 0.6f),
                                            thickness = 1.dp
                                        )

                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    stringResource(R.string.filter_platform_all),
                                                    color = CyberYellow,
                                                    fontFamily = FontFamily.Monospace
                                                )
                                            },
                                            onClick = {
                                                viewModel.onPlatformFilterChange(PlatformFilter.ALL)
                                                coroutineScope.launch { listState.scrollToItem(0) }
                                                showSortMenu = false
                                            },
                                            colors = MenuDefaults.itemColors(
                                                textColor = CyberYellow,
                                                leadingIconColor = CyberYellow,
                                                trailingIconColor = CyberYellow,
                                                disabledTextColor = CyberYellow.copy(alpha = 0.4f)
                                            )
                                        )

                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    stringResource(R.string.filter_platform_pc),
                                                    color = CyberYellow,
                                                    fontFamily = FontFamily.Monospace
                                                )
                                            },
                                            onClick = {
                                                viewModel.onPlatformFilterChange(PlatformFilter.PC)
                                                coroutineScope.launch { listState.scrollToItem(0) }
                                                showSortMenu = false
                                            },
                                            colors = MenuDefaults.itemColors(
                                                textColor = CyberYellow,
                                                leadingIconColor = CyberYellow,
                                                trailingIconColor = CyberYellow,
                                                disabledTextColor = CyberYellow.copy(alpha = 0.4f)
                                            )
                                        )

                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    stringResource(R.string.filter_platform_switch),
                                                    color = CyberYellow,
                                                    fontFamily = FontFamily.Monospace
                                                )
                                            },
                                            onClick = {
                                                viewModel.onPlatformFilterChange(PlatformFilter.SWITCH)
                                                coroutineScope.launch { listState.scrollToItem(0) }
                                                showSortMenu = false
                                            },
                                            colors = MenuDefaults.itemColors(
                                                textColor = CyberYellow,
                                                leadingIconColor = CyberYellow,
                                                trailingIconColor = CyberYellow,
                                                disabledTextColor = CyberYellow.copy(alpha = 0.4f)
                                            )
                                        )

                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    stringResource(R.string.filter_platform_ps3),
                                                    color = CyberYellow,
                                                    fontFamily = FontFamily.Monospace
                                                )
                                            },
                                            onClick = {
                                                viewModel.onPlatformFilterChange(PlatformFilter.PS3)
                                                coroutineScope.launch { listState.scrollToItem(0) }
                                                showSortMenu = false
                                            },
                                            colors = MenuDefaults.itemColors(
                                                textColor = CyberYellow,
                                                leadingIconColor = CyberYellow,
                                                trailingIconColor = CyberYellow,
                                                disabledTextColor = CyberYellow.copy(alpha = 0.4f)
                                            )
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = {
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
                                    }
                                ) {
                                    Icon(
                                        imageVector = if (showFavoritesOnly)
                                            Icons.Filled.Favorite
                                        else
                                            Icons.Filled.FavoriteBorder,
                                        contentDescription = stringResource(R.string.cd_show_favorites),
                                        tint = if (showFavoritesOnly) CyberRed else CyberYellow.copy(alpha = 0.7f),
                                        modifier = Modifier.size(actionIconSize)
                                    )
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent,
                            scrolledContainerColor = Color.Transparent,
                            titleContentColor = CyberYellow,
                            navigationIconContentColor = CyberYellow,
                            actionIconContentColor = CyberYellow
                        )
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(CyberRed, CyberYellow, Color.Transparent)
                                )
                            )
                    )
                }
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                when (val state = uiState) {
                    is GameViewModel.UiState.Loading -> {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center),
                            color = CyberYellow
                        )
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
                                tint = CyberRed,
                                modifier = Modifier.height(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                stringResource(R.string.error_loading_title),
                                style = MaterialTheme.typography.titleMedium,
                                color = CyberYellow
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                errorMessage,
                                style = MaterialTheme.typography.bodySmall,
                                color = CyberYellow.copy(alpha = 0.7f)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { viewModel.reloadLocal(context) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = CyberDark,
                                    contentColor = CyberYellow
                                ),
                                border = ButtonDefaults.outlinedButtonBorder.copy(
                                    brush = Brush.horizontalGradient(listOf(CyberRed, CyberYellow))
                                )
                            ) {
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
                                    tint = CyberYellow.copy(alpha = 0.5f),
                                    modifier = Modifier.height(64.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    stringResource(R.string.empty_favorites),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = CyberYellow
                                )
                            }
                        } else {
                            val prefetchWindow = if (isTablet) 16 else 10

                            if (!useGridLayout) {
                                LaunchedEffect(gamesToDisplay, listState) {
                                    snapshotFlow { listState.layoutInfo.visibleItemsInfo }
                                        .debounce(120)
                                        .collect { visibleItems ->
                                            if (visibleItems.isEmpty()) return@collect

                                            val from = visibleItems.minOf { it.index }
                                            val lastVisible = visibleItems.maxOf { it.index }
                                            val to = (lastVisible + prefetchWindow)
                                                .coerceAtMost(gamesToDisplay.lastIndex)

                                            val idsToPrefetch = gamesToDisplay
                                                .subList(from, to + 1)
                                                .map { it.id }

                                            viewModel.loadBadgesForGameIds(idsToPrefetch)
                                        }
                                }
                            }

                            if (useGridLayout) {
                                LaunchedEffect(gamesToDisplay, gridState) {
                                    snapshotFlow { gridState.layoutInfo.visibleItemsInfo }
                                        .debounce(120)
                                        .collect { visibleItems ->
                                            if (visibleItems.isEmpty()) return@collect

                                            val from = visibleItems.minOf { it.index }
                                            val lastVisible = visibleItems.maxOf { it.index }
                                            val to = (lastVisible + prefetchWindow)
                                                .coerceAtMost(gamesToDisplay.lastIndex)

                                            val idsToPrefetch = gamesToDisplay
                                                .subList(from, to + 1)
                                                .map { it.id }

                                            viewModel.loadBadgesForGameIds(idsToPrefetch)
                                        }
                                }
                            }

                            if (useGridLayout) {
                                LazyVerticalGrid(
                                    state = gridState,
                                    columns = GridCells.Adaptive(minSize = 520.dp),
                                    contentPadding = PaddingValues(
                                        start = horizontalPadding,
                                        end = horizontalPadding,
                                        top = verticalPadding,
                                        bottom = verticalPadding
                                    ),
                                    horizontalArrangement = Arrangement.spacedBy(18.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier
                                        .fillMaxSize()
                                ) {
                                    gridItems(
                                        items = gamesToDisplay,
                                        key = { it.id }
                                    ) { game ->
                                        GameCard(
                                            game = game,
                                            onEditStatus = { g -> onOpenEditStatus(g) },
                                            onToggleFavorite = { g ->
                                                viewModel.toggleFavorite(g.id)
                                            },
                                            onShowTestHistory = { g -> onOpenTestHistory(g) },
                                            onOpenDetails = onOpenDetails,
                                        showTestBadges = true,
                                        showTestHistoryButton = false
                                        )
                                    }
                                    item {
                                        Spacer(
                                            modifier = Modifier.windowInsetsBottomHeight(
                                                WindowInsets.navigationBars
                                            )
                                        )
                                    }
                                }
                            } else {
                                LazyColumn(
                                    state = listState,
                                    contentPadding = PaddingValues(
                                        start = horizontalPadding,
                                        end = horizontalPadding,
                                        top = verticalPadding,
                                        bottom = verticalPadding
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .widthIn(max = if (isTablet) 720.dp else 999.dp)
                                ) {
                                    items(
                                        items = gamesToDisplay,
                                        key = { it.id }
                                    ) { game ->
                                        GameCard(
                                            game = game,
                                            onEditStatus = { g -> onOpenEditStatus(g) },
                                            onToggleFavorite = { g ->
                                                viewModel.toggleFavorite(g.id)
                                            },
                                            onShowTestHistory = { g -> onOpenTestHistory(g) },
                                            onOpenDetails = onOpenDetails,
                                        showTestBadges = true,
                                        showTestHistoryButton = false
                                        )
                                    }
                                    item {
                                        Spacer(
                                            modifier = Modifier.windowInsetsBottomHeight(
                                                WindowInsets.navigationBars
                                            )
                                        )
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
                                tint = CyberYellow.copy(alpha = 0.5f),
                                modifier = Modifier.height(64.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                stringResource(R.string.no_games_title),
                                style = MaterialTheme.typography.headlineSmall,
                                color = CyberYellow
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                stringResource(R.string.no_games_subtitle),
                                color = CyberYellow.copy(alpha = 0.7f)
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(
                                onClick = { viewModel.reloadLocal(context) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = CyberDark,
                                    contentColor = CyberYellow
                                ),
                                border = ButtonDefaults.outlinedButtonBorder.copy(
                                    brush = Brush.horizontalGradient(listOf(CyberRed, CyberYellow))
                                )
                            ) {
                                Text(stringResource(R.string.select_csv_button))
                            }
                        }
                    }
                }
            }
        }
    }
}
