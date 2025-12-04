package com.sbro.gameslibrary.viewmodel

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.sbro.gameslibrary.R
import com.sbro.gameslibrary.auth.AuthManager
import com.sbro.gameslibrary.components.EmulatorBuildType
import com.sbro.gameslibrary.components.Game
import com.sbro.gameslibrary.components.GameTestResult
import com.sbro.gameslibrary.components.IssueType
import com.sbro.gameslibrary.components.Reproducibility
import com.sbro.gameslibrary.components.WorkStatus
import com.sbro.gameslibrary.util.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

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
    val games = _games.asStateFlow()

    private val _searchText = MutableStateFlow("")
    private val _sortOption = MutableStateFlow(SortOption.ORIGINAL)
    private val _platformFilter = MutableStateFlow(PlatformFilter.ALL)

    private val _filteredGames = MutableStateFlow<List<Game>>(emptyList())
    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)

    private val CACHE_FILE_NAME = "games_cache.json"
    private val PENDING_TESTS_FILE = "pending_tests.json"

    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val testsCollection = firestore.collection("gameTests")

    private val authManager = AuthManager(
        onLoggedIn = { _ -> FavoritesRepository.onUserLoggedIn() },
        onLoggedOut = { FavoritesRepository.onUserLoggedOut() }
    )
    val currentUser = authManager.currentUser

    private var appContext: Context? = null

    private var favoritesJob: Job? = null

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

        if (_games.value.isNotEmpty() || _uiState.value is UiState.Loading) return

        _uiState.value = UiState.Loading
        loadFromCacheOrAssets(context)

        viewModelScope.launch(Dispatchers.IO) {
            flushPendingTests(context)
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

    data class NewTestPayload(
        val newStatus: WorkStatus,

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

    fun updateGameStatus(
        context: Context,
        gameId: String,
        payload: NewTestPayload
    ) {
        val now = Timestamp.now()
        val nowMillis = now.toDate().time
        val testId = "${gameId}_${nowMillis}"

        val formattedDate = now.toDate().let { date ->
            val formatter = SimpleDateFormat("d MMM yyyy • HH:mm", Locale.getDefault())
            formatter.format(date)
        }

        val currentGameTitle = _games.value.firstOrNull { it.id == gameId }?.title.orEmpty()
        val user = currentUser.value

        val newTest = GameTestResult(
            testId = testId,
            status = payload.newStatus,

            testedAndroidVersion = payload.testedAndroidVersion,
            testedDeviceModel = payload.testedDeviceModel,
            testedGpuModel = payload.testedGpuModel,
            testedRam = payload.testedRam,
            testedWrapper = payload.testedWrapper,
            testedPerformanceMode = payload.testedPerformanceMode,

            testedApp = payload.testedApp,
            testedAppVersion = payload.testedAppVersion,
            testedGameVersionOrBuild = payload.testedGameVersionOrBuild,

            issueType = payload.issueType,
            reproducibility = payload.reproducibility,
            workaround = payload.workaround,
            issueNote = payload.issueNote,

            emulatorBuildType = payload.emulatorBuildType,
            accuracyLevel = payload.accuracyLevel,
            resolutionScale = payload.resolutionScale,
            asyncShaderEnabled = payload.asyncShaderEnabled,
            frameSkip = payload.frameSkip,

            resolutionWidth = payload.resolutionWidth,
            resolutionHeight = payload.resolutionHeight,
            fpsMin = payload.fpsMin,
            fpsMax = payload.fpsMax,

            mediaLink = payload.mediaLink,

            testedDateFormatted = formattedDate,
            updatedAtMillis = nowMillis,

            authorUid = user?.uid,
            authorName = user?.displayName,
            authorEmail = user?.email,
            authorPhotoUrl = user?.photoUrl?.toString(),
            fromAccount = user != null
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
            val data = newTest.toFirestoreMap(
                gameId = gameId,
                title = currentGameTitle,
                now = now,
                nowMillis = nowMillis,
                user = user
            )

            try {
                testsCollection.add(data).await()

                flushPendingTests(context)

                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        context.getString(R.string.toast_status_saved),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                enqueuePendingTest(context, data)

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

    fun editTestResult(
        context: Context,
        gameId: String,
        testMillis: Long,
        newResult: GameTestResult
    ) {
        val user = currentUser.value ?: return
        val game = _games.value.firstOrNull { it.id == gameId } ?: return
        val old = game.testResults.firstOrNull { it.updatedAtMillis == testMillis } ?: return
        if (old.authorUid != user.uid) return

        val updatedTests = game.testResults.map {
            if (it.updatedAtMillis == testMillis) {
                newResult.copy(
                    testId = old.testId,
                    updatedAtMillis = old.updatedAtMillis,
                    testedDateFormatted = old.testedDateFormatted,
                    authorUid = old.authorUid,
                    authorName = old.authorName,
                    authorEmail = old.authorEmail,
                    authorPhotoUrl = old.authorPhotoUrl,
                    fromAccount = old.fromAccount
                )
            } else it
        }

        val updatedGames = _games.value.map { g ->
            if (g.id == gameId) g.copy(testResults = updatedTests) else g
        }
        _games.value = updatedGames
        recomputeFiltered()
        saveToCache(context, updatedGames)

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val snap = if (old.testId.isNotBlank()) {
                    testsCollection
                        .whereEqualTo("testId", old.testId)
                        .limit(1)
                        .get()
                        .await()
                } else {
                    testsCollection
                        .whereEqualTo("gameId", gameId)
                        .whereEqualTo("updatedAtMillis", testMillis)
                        .limit(1)
                        .get()
                        .await()
                }

                val doc = snap.documents.firstOrNull() ?: return@launch
                doc.reference.update(
                    mapOf(
                        "testId" to old.testId,
                        "status" to newResult.status.name,
                        "testedAndroidVersion" to newResult.testedAndroidVersion,
                        "testedDeviceModel" to newResult.testedDeviceModel,
                        "testedGpuModel" to newResult.testedGpuModel,
                        "testedRam" to newResult.testedRam,
                        "testedWrapper" to newResult.testedWrapper,
                        "testedPerformanceMode" to newResult.testedPerformanceMode,
                        "testedApp" to newResult.testedApp,
                        "testedAppVersion" to newResult.testedAppVersion,
                        "testedGameVersionOrBuild" to newResult.testedGameVersionOrBuild,
                        "issueType" to newResult.issueType.firestoreValue,
                        "reproducibility" to newResult.reproducibility.firestoreValue,
                        "workaround" to newResult.workaround,
                        "issueNote" to newResult.issueNote,
                        "emulatorBuildType" to newResult.emulatorBuildType.firestoreValue,
                        "accuracyLevel" to newResult.accuracyLevel,
                        "resolutionScale" to newResult.resolutionScale,
                        "asyncShaderEnabled" to newResult.asyncShaderEnabled,
                        "frameSkip" to newResult.frameSkip,
                        "resolutionWidth" to newResult.resolutionWidth,
                        "resolutionHeight" to newResult.resolutionHeight,
                        "fpsMin" to newResult.fpsMin,
                        "fpsMax" to newResult.fpsMax,
                        "mediaLink" to newResult.mediaLink
                    )
                ).await()

                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        context.getString(R.string.test_updated),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    @Serializable
    private data class PendingTestsDto(
        val items: List<Map<String, @Serializable(with = AnyAsStringSerializer::class) Any?>> =
            emptyList()
    )

    private object AnyAsStringSerializer : kotlinx.serialization.KSerializer<Any?> {
        override val descriptor =
            kotlinx.serialization.descriptors.PrimitiveSerialDescriptor(
                "AnyAsString",
                kotlinx.serialization.descriptors.PrimitiveKind.STRING
            )

        override fun deserialize(decoder: kotlinx.serialization.encoding.Decoder): Any =
            decoder.decodeString()

        override fun serialize(
            encoder: kotlinx.serialization.encoding.Encoder,
            value: Any?
        ) {
            encoder.encodeString(value?.toString() ?: "")
        }
    }

    private fun enqueuePendingTest(context: Context, data: Map<String, Any?>) {
        try {
            val file = File(context.filesDir, PENDING_TESTS_FILE)
            val current = if (file.exists()) {
                runCatching { json.decodeFromString<PendingTestsDto>(file.readText()) }
                    .getOrNull()?.items.orEmpty()
            } else emptyList()

            val updated = current + data
            file.writeText(json.encodeToString(PendingTestsDto(updated)))
        } catch (_: Exception) {}
    }

    private suspend fun flushPendingTests(context: Context) {
        val file = File(context.filesDir, PENDING_TESTS_FILE)
        if (!file.exists()) return

        val dto = runCatching { json.decodeFromString<PendingTestsDto>(file.readText()) }
            .getOrNull() ?: return

        if (dto.items.isEmpty()) {
            file.delete()
            return
        }

        val remaining = mutableListOf<Map<String, Any?>>()

        for (item in dto.items) {
            try {
                testsCollection.add(item).await()
            } catch (_: Exception) {
                remaining += item
            }
        }

        if (remaining.isEmpty()) file.delete()
        else file.writeText(json.encodeToString(PendingTestsDto(remaining)))
    }

    private fun GameTestResult.toFirestoreMap(
        gameId: String,
        title: String,
        now: Timestamp,
        nowMillis: Long,
        user: com.google.firebase.auth.FirebaseUser?
    ): Map<String, Any?> {
        return mapOf(
            "testId" to testId,
            "gameId" to gameId,
            "title" to title,
            "status" to status.name,

            "testedAndroidVersion" to testedAndroidVersion,
            "testedDeviceModel" to testedDeviceModel,

            "testedGpuModel" to testedGpuModel,
            "testedRam" to testedRam,
            "testedWrapper" to testedWrapper,
            "testedPerformanceMode" to testedPerformanceMode,

            "testedApp" to testedApp,
            "testedAppVersion" to testedAppVersion,
            "testedGameVersionOrBuild" to testedGameVersionOrBuild,

            "issueType" to issueType.firestoreValue,
            "reproducibility" to reproducibility.firestoreValue,
            "workaround" to workaround,
            "issueNote" to issueNote,

            "emulatorBuildType" to emulatorBuildType.firestoreValue,
            "accuracyLevel" to accuracyLevel,
            "resolutionScale" to resolutionScale,
            "asyncShaderEnabled" to asyncShaderEnabled,
            "frameSkip" to frameSkip,

            "updatedAt" to now,
            "updatedAtMillis" to nowMillis,

            "resolutionWidth" to resolutionWidth,
            "resolutionHeight" to resolutionHeight,
            "fpsMin" to fpsMin,
            "fpsMax" to fpsMax,

            "mediaLink" to mediaLink,

            "authorUid" to user?.uid,
            "authorName" to user?.displayName,
            "authorEmail" to user?.email,
            "authorPhotoUrl" to user?.photoUrl?.toString(),
            "fromAccount" to (user != null)
        )
    }

    @Serializable
    private data class GamesCacheDto(val games: List<Game> = emptyList())
}
