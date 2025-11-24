package com.sbro.gameslibrary.viewmodel

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.sbro.gameslibrary.R
import com.sbro.gameslibrary.util.JsonParser
import com.sbro.gameslibrary.components.Game
import com.sbro.gameslibrary.components.GameTestResult
import com.sbro.gameslibrary.components.WorkStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.text.SimpleDateFormat
import java.util.Locale

enum class SortOption {
    TITLE,
    STATUS_WORKING,
    STATUS_NOT_WORKING,
    STATUS_UNTESTED,
    YEAR_NEW,
    PLATFORM
}

enum class PlatformFilter {
    ALL,
    PC,
    SWITCH,
    PS3
}

enum class ErrorType {
    NO_GAMES,
    PARSE_ERROR,
    UNKNOWN
}

data class RemoteTestResult(
    val gameId: String = "",
    val title: String = "",
    val status: String = WorkStatus.UNTESTED.name,
    val testedDevice: String = "",
    val testedGpuDriver: String = "",
    val testedApp: String = "",
    val testedAppVersion: String = "",
    val issueNote: String = "",
    val updatedAt: com.google.firebase.Timestamp? = null,
    val resolutionWidth: String = "",
    val resolutionHeight: String = "",
    val fpsMin: String = "",
    val fpsMax: String = ""
)

class GameViewModel : ViewModel() {

    private val _games = MutableStateFlow<List<Game>>(emptyList())
    private val _searchText = MutableStateFlow("")
    private val _sortOption = MutableStateFlow(SortOption.STATUS_WORKING)

    private val _platformFilter = MutableStateFlow(PlatformFilter.ALL)

    fun onPlatformFilterChange(filter: PlatformFilter) {
        _platformFilter.value = filter
        recomputeFiltered()
    }

    private val _filteredGames = MutableStateFlow<List<Game>>(emptyList())
    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)

    private val CACHE_FILE_NAME = "games_cache.dat"

    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    private val testsCollection = firestore.collection("gameTests")

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
        if (_games.value.isNotEmpty() || _uiState.value is UiState.Loading) return

        _uiState.value = UiState.Loading
        loadFromCache(context)
    }

    fun reloadLocal(context: Context) {
        _uiState.value = UiState.Loading
        loadFromAssets(context)
    }

    private fun saveToCache(context: Context, games: List<Game>) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val file = File(context.filesDir, CACHE_FILE_NAME)
                ObjectOutputStream(file.outputStream()).use { out ->
                    out.writeObject(games)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun loadFromCache(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val file = File(context.filesDir, CACHE_FILE_NAME)
            if (!file.exists()) {
                withContext(Dispatchers.Main) {
                    loadFromAssets(context)
                }
                return@launch
            }

            try {
                ObjectInputStream(file.inputStream()).use { input ->
                    @Suppress("UNCHECKED_CAST")
                    val cached = input.readObject() as? List<Game>
                    if (cached != null && cached.isNotEmpty()) {
                        _games.value = cached
                        recomputeFiltered()
                        _uiState.value = UiState.Success(cached.size)
                        withContext(Dispatchers.Main) {
                            syncFromRemote(context)
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            loadFromAssets(context)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    loadFromAssets(context)
                }
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
                    _games.value = parsedGames
                    recomputeFiltered()
                    saveToCache(context, parsedGames)
                    _uiState.value = UiState.Success(parsedGames.size)

                    syncFromRemote(context)
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
        recomputeFiltered()
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

            SortOption.TITLE ->
                list.sortedBy { it.title.lowercase() }

            SortOption.STATUS_WORKING ->
                list.sortedBy { it.overallStatus().ordinal }

            SortOption.STATUS_NOT_WORKING ->
                list.sortedByDescending { it.overallStatus().ordinal }

            SortOption.STATUS_UNTESTED ->
                list.sortedWith(
                    compareBy<Game> { it.overallStatus() == WorkStatus.UNTESTED }.reversed()
                        .thenBy { it.title.lowercase() }
                )

            SortOption.YEAR_NEW ->
                list.sortedByDescending { it.year }

            SortOption.PLATFORM ->
                list.sortedWith(
                    compareBy<Game> { it.platform.lowercase() }
                        .thenBy { it.title.lowercase() }
                )
        }

        _filteredGames.value = list
    }

    fun toggleFavorite(context: Context, gameId: String) {
        val currentList = _games.value
        val updatedList = currentList.map { game ->
            if (game.id == gameId) {
                game.copy(isFavorite = !game.isFavorite)
            } else game
        }
        _games.value = updatedList
        recomputeFiltered()
        saveToCache(context, updatedList)
    }

    fun syncFromRemote(context: Context) {
        viewModelScope.launch {
            try {
                val testsSnapshot = withContext(Dispatchers.IO) {
                    testsCollection.get().await()
                }

                val remoteTests = testsSnapshot.documents.mapNotNull { doc ->
                    try {
                        val gameId = doc.getString("gameId") ?: doc.getString("id") ?: ""
                        val title = doc.getString("title") ?: ""
                        val statusStr = doc.getString("status") ?: WorkStatus.UNTESTED.name
                        val testedDevice = doc.getString("testedDevice") ?: ""
                        val testedGpuDriver = doc.getString("testedGpuDriver") ?: ""
                        val testedApp = doc.getString("testedApp") ?: ""
                        val testedAppVersion = doc.getString("testedAppVersion") ?: ""
                        val issueNote = doc.getString("issueNote") ?: ""
                        val updatedAt = doc.getTimestamp("updatedAt")
                        val resolutionWidth = doc.getString("resolutionWidth") ?: ""
                        val resolutionHeight = doc.getString("resolutionHeight") ?: ""
                        val fpsMin = doc.getString("fpsMin") ?: ""
                        val fpsMax = doc.getString("fpsMax") ?: ""

                        if (gameId.isBlank()) null else {
                            RemoteTestResult(
                                gameId = gameId,
                                title = title,
                                status = statusStr,
                                testedDevice = testedDevice,
                                testedGpuDriver = testedGpuDriver,
                                testedApp = testedApp,
                                testedAppVersion = testedAppVersion,
                                issueNote = issueNote,
                                updatedAt = updatedAt,
                                resolutionWidth = resolutionWidth,
                                resolutionHeight = resolutionHeight,
                                fpsMin = fpsMin,
                                fpsMax = fpsMax
                            )
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("GameViewModel", "Skip bad test doc ${doc.id}", e)
                        null
                    }
                }

                val grouped = remoteTests.groupBy { it.gameId }

                val updated = _games.value.map { game ->
                    val testsForGame = grouped[game.id].orEmpty()
                        .map { remote ->
                            val millis = remote.updatedAt?.toDate()?.time ?: 0L
                            val formattedDate = remote.updatedAt?.toDate()?.let { date ->
                                val formatter = SimpleDateFormat("d MMM yyyy • HH:mm", Locale.getDefault())
                                formatter.format(date)
                            } ?: ""

                            GameTestResult(
                                status = when (remote.status) {
                                    WorkStatus.WORKING.name -> WorkStatus.WORKING
                                    WorkStatus.NOT_WORKING.name -> WorkStatus.NOT_WORKING
                                    else -> WorkStatus.UNTESTED
                                },
                                testedDevice = remote.testedDevice,
                                testedGpuDriver = remote.testedGpuDriver,
                                testedApp = remote.testedApp,
                                testedAppVersion = remote.testedAppVersion,
                                testedDateFormatted = formattedDate,
                                issueNote = remote.issueNote,
                                updatedAtMillis = millis,
                                resolutionWidth = remote.resolutionWidth,
                                resolutionHeight = remote.resolutionHeight,
                                fpsMin = remote.fpsMin,
                                fpsMax = remote.fpsMax
                            )
                        }
                        .sortedByDescending { it.updatedAtMillis }

                    game.copy(
                        testResults = testsForGame
                    )
                }

                _games.value = updated
                recomputeFiltered()
                saveToCache(context, updated)

                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        context.getString(R.string.toast_sync_success),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        context.getString(R.string.toast_sync_error),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    fun updateGameStatus(
        context: Context,
        gameId: String,
        newStatus: WorkStatus,
        newDevice: String,
        testedGpuDriver: String,
        testedApp: String,
        testedAppVersion: String,
        issueNote: String,
        resolutionWidth: String,
        resolutionHeight: String,
        fpsMin: String,
        fpsMax: String
    ) {
        val now = com.google.firebase.Timestamp.now()

        val formattedDate = now.toDate().let { date ->
            val formatter = SimpleDateFormat("d MMM yyyy • HH:mm", Locale.getDefault())
            formatter.format(date)
        }

        val currentGameTitle = _games.value.firstOrNull { it.id == gameId }?.title.orEmpty()

        val newTest = GameTestResult(
            status = newStatus,
            testedDevice = newDevice,
            testedGpuDriver = testedGpuDriver,
            testedApp = testedApp,
            testedAppVersion = testedAppVersion,
            testedDateFormatted = formattedDate,
            issueNote = issueNote,
            updatedAtMillis = now.toDate().time,
            resolutionWidth = resolutionWidth,
            resolutionHeight = resolutionHeight,
            fpsMin = fpsMin,
            fpsMax = fpsMax
        )

        val updated = _games.value.map { game ->
            if (game.id == gameId) {
                game.copy(
                    testResults = (game.testResults + newTest)
                        .sortedByDescending { it.updatedAtMillis }
                )
            } else game
        }

        _games.value = updated
        recomputeFiltered()
        saveToCache(context, updated)

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val data = mapOf(
                    "gameId" to gameId,
                    "title" to currentGameTitle,
                    "status" to newStatus.name,
                    "testedDevice" to newDevice,
                    "testedGpuDriver" to testedGpuDriver,
                    "testedApp" to testedApp,
                    "testedAppVersion" to testedAppVersion,
                    "issueNote" to issueNote,
                    "updatedAt" to now,
                    "resolutionWidth" to resolutionWidth,
                    "resolutionHeight" to resolutionHeight,
                    "fpsMin" to fpsMin,
                    "fpsMax" to fpsMax
                )
                testsCollection.add(data).await()

                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        context.getString(R.string.toast_status_saved),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        context.getString(R.string.toast_status_saved_offline),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
}
