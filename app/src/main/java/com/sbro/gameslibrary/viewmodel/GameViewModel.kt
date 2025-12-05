package com.sbro.gameslibrary.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.sbro.gameslibrary.auth.AuthManager
import com.sbro.gameslibrary.components.Game
import com.sbro.gameslibrary.components.WorkStatus
import com.sbro.gameslibrary.util.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

enum class SortOption {
    TITLE,
    STATUS_WORKING,
    STATUS_NOT_WORKING,
    STATUS_UNTESTED,
    YEAR_NEW,
    PLATFORM,
    RATING_HIGH,
    GENRE_AZ,
    ORIGINAL
}

data class TestComment(
    val id: String = "",
    val gameId: String = "",
    val testId: String = "",
    val testMillis: Long = 0L,
    val text: String = "",
    val authorDevice: String = "",
    val createdAt: Timestamp? = null,
    val authorUid: String? = null,
    val authorName: String? = null,
    val authorEmail: String? = null,
    val authorPhotoUrl: String? = null,
    val fromAccount: Boolean = false
)

enum class PlatformFilter { ALL, PC, SWITCH, PS3 }
enum class ErrorType { NO_GAMES, PARSE_ERROR, UNKNOWN }

class GameViewModel : ViewModel() {

    private val _games = MutableStateFlow<List<Game>>(emptyList())

    private val _searchText = MutableStateFlow("")
    private val _sortOption = MutableStateFlow(SortOption.ORIGINAL)
    private val _platformFilter = MutableStateFlow(PlatformFilter.ALL)

    private val _filteredGames = MutableStateFlow<List<Game>>(emptyList())
    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)

    private val CACHE_FILE_NAME = "games_cache.json"

    private val authManager = AuthManager(
        onLoggedIn = { _ -> FavoritesRepository.onUserLoggedIn() },
        onLoggedOut = { FavoritesRepository.onUserLoggedOut() }
    )
    val currentUser = authManager.currentUser

    private var appContext: Context? = null

    private var favoritesJob: Job? = null
    private var searchJob: Job? = null

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun onPlatformFilterChange(filter: PlatformFilter) {
        _platformFilter.value = filter
        recomputeFiltered()
    }

    sealed class UiState {
        object Idle : UiState()
        object Loading : UiState()
        data class Error(val type: ErrorType) : UiState()
        data class Success(val count: Int) : UiState()
    }

    val uiState = _uiState.asStateFlow()
    val searchText = _searchText.asStateFlow()
    val filteredGames = _filteredGames.asStateFlow()

    fun init(context: Context) {
        if (appContext == null) {
            appContext = context.applicationContext
            FavoritesRepository.init(appContext!!)
            authManager.setAppContext(appContext!!)
        }

        observeFavorites()
        observeSearch()

        if (_games.value.isNotEmpty() || _uiState.value is UiState.Loading) return

        _uiState.value = UiState.Loading
        loadFromCacheOrAssets(context)
    }

    @OptIn(FlowPreview::class)
    private fun observeSearch() {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _searchText
                .debounce(300)
                .distinctUntilChanged()
                .collectLatest {
                    recomputeFiltered()
                }
        }
    }

    private fun observeFavorites() {
        favoritesJob?.cancel()
        favoritesJob = viewModelScope.launch {
            FavoritesRepository.favoriteIds.collectLatest { ids ->
                val current = _games.value
                if (current.isEmpty()) return@collectLatest

                val updated = current.map { g ->
                    val fav = ids.contains(g.id)
                    if (g.isFavorite == fav) g else g.copy(isFavorite = fav)
                }

                if (updated != current) {
                    _games.value = updated
                    recomputeFiltered()
                    appContext?.let { saveToCache(it, updated) }
                }
            }
        }
    }

    fun reloadLocal(context: Context) {
        _uiState.value = UiState.Loading
        loadFromAssets(context)
    }

    private fun saveToCache(context: Context, games: List<Game>) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val file = File(context.filesDir, CACHE_FILE_NAME)
                val dto = GamesCacheDto(games)
                file.writeText(json.encodeToString(dto))
            } catch (_: Exception) {}
        }
    }

    private fun loadFromCacheOrAssets(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val file = File(context.filesDir, CACHE_FILE_NAME)
            if (!file.exists()) {
                withContext(Dispatchers.Main) { loadFromAssets(context) }
                return@launch
            }

            try {
                val text = file.readText()
                val cachedDto = json.decodeFromString<GamesCacheDto>(text)
                val cached = cachedDto.games

                if (cached.isNotEmpty()) {
                    val ids = FavoritesRepository.favoriteIds.value
                    val synced = cached.map { g ->
                        val fav = ids.contains(g.id)
                        if (g.isFavorite == fav) g else g.copy(isFavorite = fav)
                    }
                    _games.value = synced
                    recomputeFiltered()
                    _uiState.value = UiState.Success(synced.size)
                } else {
                    withContext(Dispatchers.Main) { loadFromAssets(context) }
                }
            } catch (_: Exception) {
                withContext(Dispatchers.Main) { loadFromAssets(context) }
            }
        }
    }

    private fun loadFromAssets(context: Context) {
        viewModelScope.launch {
            val result = JsonParser.parseFromAssetsJson(context)
            result.onSuccess { parsedGames ->
                if (parsedGames.isEmpty()) {
                    _games.value = emptyList()
                    _filteredGames.value = emptyList()
                    _uiState.value = UiState.Error(ErrorType.NO_GAMES)
                } else {
                    val ids = FavoritesRepository.favoriteIds.value
                    val synced = parsedGames.map { g ->
                        val fav = ids.contains(g.id)
                        if (g.isFavorite == fav) g else g.copy(isFavorite = fav)
                    }

                    _games.value = synced
                    recomputeFiltered()
                    saveToCache(context, synced)
                    _uiState.value = UiState.Success(synced.size)
                }
            }.onFailure {
                _games.value = emptyList()
                _filteredGames.value = emptyList()
                _uiState.value = UiState.Error(ErrorType.PARSE_ERROR)
            }
        }
    }

    fun onSearchChange(text: String) {
        _searchText.value = text
    }

    fun onSortChange(option: SortOption) {
        _sortOption.value = option
        recomputeFiltered()
    }

    private fun isPcPlatform(platform: String): Boolean {
        val p = platform.lowercase()
        return p.contains("pc") || p.contains("windows")
    }
    private fun isSwitchPlatform(platform: String): Boolean {
        val p = platform.lowercase()
        return p.contains("switch") || p.contains("nintendo")
    }
    private fun isPs3Platform(platform: String): Boolean {
        val p = platform.lowercase()
        return p.contains("playstation") || p.contains("ps3")
    }

    private fun ratingAsFloat(r: String): Float =
        r.trim().replace(",", ".").toFloatOrNull() ?: -1f

    private fun statusRankForWorkingFirst(status: WorkStatus): Int = when (status) {
        WorkStatus.WORKING -> 0
        WorkStatus.NOT_WORKING -> 1
        WorkStatus.UNTESTED -> 2
    }

    private fun statusRankForNotWorkingFirst(status: WorkStatus): Int = when (status) {
        WorkStatus.NOT_WORKING -> 0
        WorkStatus.WORKING -> 1
        WorkStatus.UNTESTED -> 2
    }

    private fun recomputeFiltered() {
        val query = _searchText.value.trim().lowercase()
        var list = _games.value

        if (query.isNotEmpty()) {
            list = list.filter {
                it.title.contains(query, ignoreCase = true) ||
                        it.genre.contains(query, ignoreCase = true)
            }
        }

        list = when (_platformFilter.value) {
            PlatformFilter.ALL -> list
            PlatformFilter.PC -> list.filter { isPcPlatform(it.platform) }
            PlatformFilter.SWITCH -> list.filter { isSwitchPlatform(it.platform) }
            PlatformFilter.PS3 -> list.filter { isPs3Platform(it.platform) }
        }

        list = when (_sortOption.value) {
            SortOption.ORIGINAL -> list
            SortOption.TITLE -> list.sortedBy { it.title.lowercase() }
            SortOption.RATING_HIGH ->
                list.sortedWith(compareByDescending<Game> { ratingAsFloat(it.rating) }
                    .thenBy { it.title.lowercase() })
            SortOption.GENRE_AZ ->
                list.sortedWith(compareBy<Game> { it.genre.lowercase() }
                    .thenBy { it.title.lowercase() })
            SortOption.STATUS_WORKING ->
                list.sortedWith(compareBy<Game> { statusRankForWorkingFirst(it.overallStatus()) }
                    .thenBy { it.title.lowercase() })
            SortOption.STATUS_NOT_WORKING ->
                list.sortedWith(compareBy<Game> { statusRankForNotWorkingFirst(it.overallStatus()) }
                    .thenBy { it.title.lowercase() })
            SortOption.STATUS_UNTESTED ->
                list.sortedWith(compareBy<Game> { it.overallStatus() != WorkStatus.UNTESTED }
                    .thenBy { it.title.lowercase() })
            SortOption.YEAR_NEW -> list.sortedByDescending { it.year }
            SortOption.PLATFORM ->
                list.sortedWith(compareBy<Game> { it.platform.lowercase() }
                    .thenBy { it.title.lowercase() })
        }

        _filteredGames.value = list
    }

    fun toggleFavorite(gameId: String) {
        FavoritesRepository.toggleFavorite(gameId)
    }

    @Serializable
    private data class GamesCacheDto(val games: List<Game> = emptyList())
}
