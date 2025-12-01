package com.sbro.gameslibrary.viewmodel

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.content.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.sbro.gameslibrary.R
import com.sbro.gameslibrary.components.*
import com.sbro.gameslibrary.util.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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

data class TestComment(
    val id: String = "",
    val gameId: String = "",
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

    val testedAndroidVersion: String = "",
    val testedDeviceModel: String = "",
    val testedGpuModel: String = "",
    val testedRam: String = "",
    val testedWrapper: String = "",
    val testedPerformanceMode: String = "",

    val testedApp: String = "",
    val testedAppVersion: String = "",
    val testedGameVersionOrBuild: String = "",

    val issueType: String = IssueType.CRASH.firestoreValue,
    val reproducibility: String = Reproducibility.ALWAYS.firestoreValue,
    val workaround: String = "",
    val issueNote: String = "",

    val emulatorBuildType: String = EmulatorBuildType.STABLE.firestoreValue,
    val accuracyLevel: String = "",
    val resolutionScale: String = "",
    val asyncShaderEnabled: Boolean = false,
    val frameSkip: String = "",

    val resolutionWidth: String = "",
    val resolutionHeight: String = "",
    val fpsMin: String = "",
    val fpsMax: String = "",

    val mediaLink: String = "",

    val updatedAt: Timestamp? = null,

    val authorUid: String? = null,
    val authorName: String? = null,
    val authorEmail: String? = null,
    val authorPhotoUrl: String? = null,
    val fromAccount: Boolean = false,
    val updatedAtMillis: Long? = null
)

class GameViewModel : ViewModel() {

    private val _games = MutableStateFlow<List<Game>>(emptyList())
    val games = _games.asStateFlow()

    private val _searchText = MutableStateFlow("")
    private val _sortOption = MutableStateFlow(SortOption.STATUS_WORKING)
    private val _platformFilter = MutableStateFlow(PlatformFilter.ALL)

    private val _filteredGames = MutableStateFlow<List<Game>>(emptyList())
    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)

    private val CACHE_FILE_NAME = "games_cache.dat"

    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val testsCollection = firestore.collection("gameTests")
    private val commentsCollection = firestore.collection("testComments")
    private val favoritesCollection = firestore.collection("userFavorites")

    private val _commentsByTest =
        MutableStateFlow<Map<Long, List<TestComment>>>(emptyMap())
    val commentsByTest = _commentsByTest.asStateFlow()

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    private val _currentUser = MutableStateFlow(auth.currentUser)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()
    private val _isLoggedIn = MutableStateFlow(auth.currentUser != null)

    private var appContext: Context? = null

    private val PENDING_FAVS_PREF = "pending_favorites"
    private val PENDING_FAVS_KEY = "pending_game_ids"

    private val SYNC_PREF = "sync_state"
    private val LAST_TESTS_SYNC_KEY = "last_tests_sync_millis"

    private fun getLastTestsSyncMillis(context: Context): Long {
        val prefs = context.getSharedPreferences(SYNC_PREF, Context.MODE_PRIVATE)
        return prefs.getLong(LAST_TESTS_SYNC_KEY, 0L)
    }

    private fun saveLastTestsSyncMillis(context: Context, millis: Long) {
        val prefs = context.getSharedPreferences(SYNC_PREF, Context.MODE_PRIVATE)
        prefs.edit { putLong(LAST_TESTS_SYNC_KEY, millis) }
    }

    private var lastCommentsDoc: DocumentSnapshot? = null
    private var lastCommentsGameId: String? = null
    private val COMMENTS_PAGE_SIZE = 50

    private var lastAllCommentsDoc: DocumentSnapshot? = null
    private val ALL_COMMENTS_PAGE_SIZE = 200

    fun onPlatformFilterChange(filter: PlatformFilter) {
        _platformFilter.value = filter
        recomputeFiltered()
    }

    private fun getPendingFavorites(context: Context): Set<String> {
        val prefs = context.getSharedPreferences(PENDING_FAVS_PREF, Context.MODE_PRIVATE)
        return prefs.getStringSet(PENDING_FAVS_KEY, emptySet()) ?: emptySet()
    }

    private fun savePendingFavorites(context: Context, ids: Set<String>) {
        val prefs = context.getSharedPreferences(PENDING_FAVS_PREF, Context.MODE_PRIVATE)
        prefs.edit { putStringSet(PENDING_FAVS_KEY, ids) }
    }

    private fun clearPendingFavorites(context: Context) {
        val prefs = context.getSharedPreferences(PENDING_FAVS_PREF, Context.MODE_PRIVATE)
        prefs.edit { remove(PENDING_FAVS_KEY) }
    }

    private fun mergePendingFavoritesIntoRemote(context: Context) {
        val user = auth.currentUser ?: return

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val pending = getPendingFavorites(context)
                if (pending.isEmpty()) {
                    syncFavoritesFromRemote()
                    return@launch
                }

                val docRef = favoritesCollection.document(user.uid)
                val snap = docRef.get().await()
                val remoteIds =
                    (snap.get("gameIds") as? List<*>)?.filterIsInstance<String>().orEmpty()

                val mergedIds = (remoteIds + pending).toSet().toList()
                docRef.set(mapOf("gameIds" to mergedIds)).await()

                clearPendingFavorites(context)

                val mergedGames = _games.value.map { g ->
                    g.copy(isFavorite = mergedIds.contains(g.id))
                }

                withContext(Dispatchers.Main) {
                    _games.value = mergedGames
                    recomputeFiltered()
                    saveToCache(context, mergedGames)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                syncFavoritesFromRemote()
            }
        }
    }

    init {
        auth.addAuthStateListener { fa ->
            _currentUser.value = fa.currentUser
            _isLoggedIn.value = fa.currentUser != null

            val ctx = appContext
            if (fa.currentUser != null) {
                if (ctx != null) mergePendingFavoritesIntoRemote(ctx)
                else syncFavoritesFromRemote()
            } else {
                val cleared = _games.value.map { it.copy(isFavorite = false) }
                _games.value = cleared
                recomputeFiltered()
            }
        }
    }

    fun getGoogleSignInIntent(context: Context): Intent {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        val client = GoogleSignIn.getClient(context, gso)
        return client.signInIntent
    }

    fun handleGoogleResult(
        context: Context,
        data: Intent?,
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                val task = GoogleSignIn.getSignedInAccountFromIntent(data)
                val account = task.getResult(ApiException::class.java)
                val credential =
                    GoogleAuthProvider.getCredential(account.idToken, null)

                auth.signInWithCredential(credential).await()

                Toast.makeText(
                    context,
                    context.getString(R.string.auth_success),
                    Toast.LENGTH_SHORT
                ).show()

                mergePendingFavoritesIntoRemote(context)

            } catch (e: Exception) {
                e.printStackTrace()
                onError(e.message ?: "Auth failed")
                Toast.makeText(
                    context,
                    context.getString(R.string.auth_failed),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    fun signOut(context: Context) {
        viewModelScope.launch {
            auth.signOut()
            GoogleSignIn.getClient(
                context,
                GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()
            ).signOut()

            Toast.makeText(
                context,
                context.getString(R.string.auth_signed_out),
                Toast.LENGTH_SHORT
            ).show()
        }
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
        if (appContext == null) appContext = context.applicationContext

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
                withContext(Dispatchers.Main) { loadFromAssets(context) }
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

                        if (auth.currentUser != null) {
                            mergePendingFavoritesIntoRemote(context)
                        }

                    } else {
                        withContext(Dispatchers.Main) { loadFromAssets(context) }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
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
                    _games.value = parsedGames
                    recomputeFiltered()
                    saveToCache(context, parsedGames)
                    _uiState.value = UiState.Success(parsedGames.size)

                    syncFromRemote(context)

                    if (auth.currentUser != null) {
                        mergePendingFavoritesIntoRemote(context)
                    }
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
        val user = auth.currentUser

        val current = _games.value
        val newFavState = current.firstOrNull { it.id == gameId }?.isFavorite?.not() ?: true

        val updatedList = current.map { game ->
            if (game.id == gameId) {
                game.copy(isFavorite = newFavState)
            } else game
        }

        _games.value = updatedList
        recomputeFiltered()
        saveToCache(context, updatedList)

        if (user == null) {
            val pending = getPendingFavorites(context).toMutableSet()
            if (newFavState) pending.add(gameId) else pending.remove(gameId)
            savePendingFavorites(context, pending)
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val docRef = favoritesCollection.document(user.uid)
                try {
                    if (newFavState) {
                        docRef.update("gameIds", FieldValue.arrayUnion(gameId)).await()
                    } else {
                        docRef.update("gameIds", FieldValue.arrayRemove(gameId)).await()
                    }
                } catch (_: Exception) {
                    docRef.set(mapOf("gameIds" to listOf(gameId))).await()
                }

                val pending = getPendingFavorites(context).toMutableSet()
                pending.remove(gameId)
                savePendingFavorites(context, pending)

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun syncFavoritesFromRemote() {
        val user = auth.currentUser ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val snap = favoritesCollection.document(user.uid).get().await()
                val favIds =
                    (snap.get("gameIds") as? List<*>)?.filterIsInstance<String>().orEmpty().toSet()

                val merged = _games.value.map { g ->
                    g.copy(isFavorite = favIds.contains(g.id))
                }

                withContext(Dispatchers.Main) {
                    _games.value = merged
                    recomputeFiltered()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun syncFromRemote(context: Context) {
        viewModelScope.launch {
            try {
                val lastSync = getLastTestsSyncMillis(context)

                val testsSnapshot = withContext(Dispatchers.IO) {
                    testsCollection
                        .whereGreaterThan("updatedAtMillis", lastSync)
                        .get()
                        .await()
                }

                val remoteTests = testsSnapshot.documents.mapNotNull { doc ->
                    try {
                        val gameId = doc.getString("gameId") ?: doc.getString("id") ?: ""
                        val title = doc.getString("title") ?: ""
                        val statusStr = doc.getString("status") ?: WorkStatus.UNTESTED.name

                        val testedAndroidVersion = doc.getString("testedAndroidVersion") ?: ""
                        val testedDeviceModel = doc.getString("testedDeviceModel") ?: ""
                        val testedGpuModel = doc.getString("testedGpuModel") ?: ""
                        val testedRam = doc.getString("testedRam") ?: ""
                        val testedWrapper = doc.getString("testedWrapper") ?: ""
                        val testedPerformanceMode = doc.getString("testedPerformanceMode") ?: ""

                        val testedApp = doc.getString("testedApp") ?: ""
                        val testedAppVersion = doc.getString("testedAppVersion") ?: ""
                        val testedGameVersionOrBuild = doc.getString("testedGameVersionOrBuild") ?: ""

                        val issueType = doc.getString("issueType") ?: IssueType.CRASH.firestoreValue
                        val reproducibility =
                            doc.getString("reproducibility")
                                ?: Reproducibility.ALWAYS.firestoreValue
                        val workaround = doc.getString("workaround") ?: ""
                        val issueNote = doc.getString("issueNote") ?: ""

                        val emulatorBuildType =
                            doc.getString("emulatorBuildType")
                                ?: EmulatorBuildType.STABLE.firestoreValue
                        val accuracyLevel = doc.getString("accuracyLevel") ?: ""
                        val resolutionScale = doc.getString("resolutionScale") ?: ""
                        val asyncShaderEnabled = doc.getBoolean("asyncShaderEnabled") ?: false
                        val frameSkip = doc.getString("frameSkip") ?: ""

                        val updatedAt = doc.getTimestamp("updatedAt")
                        val updatedAtMillis = doc.getLong("updatedAtMillis")
                        val resolutionWidth = doc.getString("resolutionWidth") ?: ""
                        val resolutionHeight = doc.getString("resolutionHeight") ?: ""
                        val fpsMin = doc.getString("fpsMin") ?: ""
                        val fpsMax = doc.getString("fpsMax") ?: ""

                        val mediaLink = doc.getString("mediaLink") ?: ""

                        val authorUid = doc.getString("authorUid")
                        val authorName = doc.getString("authorName")
                        val authorEmail = doc.getString("authorEmail")
                        val authorPhotoUrl = doc.getString("authorPhotoUrl")
                        val fromAccount = doc.getBoolean("fromAccount") ?: false

                        if (gameId.isBlank()) null else {
                            RemoteTestResult(
                                gameId = gameId,
                                title = title,
                                status = statusStr,

                                testedAndroidVersion = testedAndroidVersion,
                                testedDeviceModel = testedDeviceModel,
                                testedGpuModel = testedGpuModel,
                                testedRam = testedRam,
                                testedWrapper = testedWrapper,
                                testedPerformanceMode = testedPerformanceMode,

                                testedApp = testedApp,
                                testedAppVersion = testedAppVersion,
                                testedGameVersionOrBuild = testedGameVersionOrBuild,

                                issueType = issueType,
                                reproducibility = reproducibility,
                                workaround = workaround,
                                issueNote = issueNote,

                                emulatorBuildType = emulatorBuildType,
                                accuracyLevel = accuracyLevel,
                                resolutionScale = resolutionScale,
                                asyncShaderEnabled = asyncShaderEnabled,
                                frameSkip = frameSkip,

                                resolutionWidth = resolutionWidth,
                                resolutionHeight = resolutionHeight,
                                fpsMin = fpsMin,
                                fpsMax = fpsMax,

                                mediaLink = mediaLink,
                                updatedAt = updatedAt,

                                authorUid = authorUid,
                                authorName = authorName,
                                authorEmail = authorEmail,
                                authorPhotoUrl = authorPhotoUrl,
                                fromAccount = fromAccount,
                                updatedAtMillis = updatedAtMillis
                            )
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("GameViewModel", "Skip bad test doc ${doc.id}", e)
                        null
                    }
                }

                if (remoteTests.isEmpty()) return@launch

                val groupedNew = remoteTests.groupBy { it.gameId }

                val updated = _games.value.map { game ->
                    val existing = game.testResults

                    val newForGame = groupedNew[game.id].orEmpty()
                        .map { remote ->
                            val millis =
                                remote.updatedAtMillis
                                    ?: remote.updatedAt?.toDate()?.time
                                    ?: 0L

                            val formattedDate = remote.updatedAt?.toDate()?.let { date ->
                                val formatter =
                                    SimpleDateFormat("d MMM yyyy • HH:mm", Locale.getDefault())
                                formatter.format(date)
                            } ?: ""

                            GameTestResult(
                                status = when (remote.status) {
                                    WorkStatus.WORKING.name -> WorkStatus.WORKING
                                    WorkStatus.NOT_WORKING.name -> WorkStatus.NOT_WORKING
                                    else -> WorkStatus.UNTESTED
                                },

                                testedAndroidVersion = remote.testedAndroidVersion,
                                testedDeviceModel = remote.testedDeviceModel,
                                testedGpuModel = remote.testedGpuModel,
                                testedRam = remote.testedRam,
                                testedWrapper = remote.testedWrapper,
                                testedPerformanceMode = remote.testedPerformanceMode,

                                testedApp = remote.testedApp,
                                testedAppVersion = remote.testedAppVersion,
                                testedGameVersionOrBuild = remote.testedGameVersionOrBuild,

                                issueType = IssueType.fromFirestore(remote.issueType),
                                reproducibility = Reproducibility.fromFirestore(remote.reproducibility),
                                workaround = remote.workaround,
                                issueNote = remote.issueNote,

                                emulatorBuildType = EmulatorBuildType.fromFirestore(remote.emulatorBuildType),
                                accuracyLevel = remote.accuracyLevel,
                                resolutionScale = remote.resolutionScale,
                                asyncShaderEnabled = remote.asyncShaderEnabled,
                                frameSkip = remote.frameSkip,

                                resolutionWidth = remote.resolutionWidth,
                                resolutionHeight = remote.resolutionHeight,
                                fpsMin = remote.fpsMin,
                                fpsMax = remote.fpsMax,

                                mediaLink = remote.mediaLink,

                                testedDateFormatted = formattedDate,
                                updatedAtMillis = millis,

                                authorUid = remote.authorUid,
                                authorName = remote.authorName,
                                authorEmail = remote.authorEmail,
                                authorPhotoUrl = remote.authorPhotoUrl,
                                fromAccount = remote.fromAccount
                            )
                        }

                    val merged = (existing + newForGame)
                        .distinctBy { it.updatedAtMillis }
                        .sortedByDescending { it.updatedAtMillis }

                    game.copy(testResults = merged)
                }

                _games.value = updated
                recomputeFiltered()
                saveToCache(context, updated)

                val maxMillis = remoteTests.maxOfOrNull { it.updatedAtMillis ?: 0L } ?: lastSync
                if (maxMillis > lastSync) {
                    saveLastTestsSyncMillis(context, maxMillis)
                }

                if (auth.currentUser != null) {
                    mergePendingFavoritesIntoRemote(context)
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

    fun loadCommentsForGame(gameId: String) {
        loadCommentsForGameInternal(gameId, loadMore = false)
    }

    fun loadMoreCommentsForGame(gameId: String) {
        loadCommentsForGameInternal(gameId, loadMore = true)
    }

    private fun loadCommentsForGameInternal(gameId: String, loadMore: Boolean) {
        viewModelScope.launch {
            try {
                if (!loadMore || lastCommentsGameId != gameId) {
                    lastCommentsDoc = null
                    lastCommentsGameId = gameId
                }

                val snapshot = withContext(Dispatchers.IO) {
                    var q = commentsCollection
                        .whereEqualTo("gameId", gameId)
                        .orderBy("createdAt", Query.Direction.DESCENDING)
                        .limit(COMMENTS_PAGE_SIZE.toLong())

                    if (loadMore && lastCommentsDoc != null) {
                        q = q.startAfter(lastCommentsDoc!!)
                    }

                    q.get().await()
                }

                if (!snapshot.isEmpty) {
                    lastCommentsDoc = snapshot.documents.last()
                }

                val comments = snapshot.documents.mapNotNull { d ->
                    val text = d.getString("text") ?: return@mapNotNull null
                    val millis = d.getLong("testMillis") ?: return@mapNotNull null

                    TestComment(
                        id = d.id,
                        gameId = d.getString("gameId") ?: gameId,
                        testMillis = millis,
                        text = text,
                        authorDevice = d.getString("authorDevice") ?: "",
                        createdAt = d.getTimestamp("createdAt"),

                        authorUid = d.getString("authorUid"),
                        authorName = d.getString("authorName"),
                        authorEmail = d.getString("authorEmail"),
                        authorPhotoUrl = d.getString("authorPhotoUrl"),
                        fromAccount = d.getBoolean("fromAccount") ?: false
                    )
                }

                val groupedNew = comments
                    .groupBy { it.testMillis }
                    .mapValues { (_, list) ->
                        list.sortedBy { it.createdAt?.toDate()?.time ?: 0L }
                    }

                val merged = if (loadMore) {
                    val current = _commentsByTest.value.toMutableMap()
                    groupedNew.forEach { (k, v) ->
                        val old = current[k].orEmpty()
                        current[k] = (old + v).distinctBy { it.id }
                    }
                    current
                } else {
                    groupedNew.toMutableMap()
                }

                _commentsByTest.value = merged

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun addTestComment(
        context: Context,
        gameId: String,
        testMillis: Long,
        text: String
    ) {
        val trimmed = text.trim()
        if (trimmed.isBlank()) return

        val now = Timestamp.now()
        val deviceName =
            "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}".trim()

        val user = auth.currentUser
        val fromAccount = user != null

        val newLocal = TestComment(
            id = "local_${now.toDate().time}",
            gameId = gameId,
            testMillis = testMillis,
            text = trimmed,
            authorDevice = deviceName,
            createdAt = now,

            authorUid = user?.uid,
            authorName = user?.displayName,
            authorEmail = user?.email,
            authorPhotoUrl = user?.photoUrl?.toString(),
            fromAccount = fromAccount
        )

        val currentMap = _commentsByTest.value.toMutableMap()
        val list = currentMap[testMillis].orEmpty().toMutableList()
        list.add(newLocal)
        currentMap[testMillis] = list
        _commentsByTest.value = currentMap

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val user = currentUser.value
                val fromAccount = user != null

                val data = mapOf(
                    "gameId" to gameId,
                    "testMillis" to testMillis,
                    "text" to trimmed,
                    "authorDevice" to deviceName,
                    "createdAt" to now,

                    "authorUid" to user?.uid,
                    "authorName" to user?.displayName,
                    "authorEmail" to user?.email,
                    "authorPhotoUrl" to user?.photoUrl?.toString(),
                    "fromAccount" to fromAccount
                )
                commentsCollection.add(data).await()

                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        context.getString(R.string.test_comment_saved),
                        Toast.LENGTH_SHORT
                    ).show()
                }

                loadCommentsForGame(gameId)

            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        context.getString(R.string.test_comment_saved_offline),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    fun editComment(
        context: Context,
        commentId: String,
        newText: String
    ) {
        val user = auth.currentUser ?: return
        val trimmed = newText.trim()
        if (trimmed.isBlank()) return

        val map = _commentsByTest.value.toMutableMap()
        map.forEach { (testMillis, list) ->
            val updated = list.map { c ->
                if (c.id == commentId && c.authorUid == user.uid) c.copy(text = trimmed)
                else c
            }
            map[testMillis] = updated
        }
        _commentsByTest.value = map

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val docRef = commentsCollection.document(commentId)
                val snap = docRef.get().await()
                if (snap.getString("authorUid") != user.uid) return@launch

                docRef.update("text", trimmed).await()
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        context.getString(R.string.comment_updated),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (_: Exception) {}
        }
    }

    fun loadAllComments() {
        loadAllCommentsInternal(loadMore = false)
    }

    fun loadMoreAllComments() {
        loadAllCommentsInternal(loadMore = true)
    }

    private fun loadAllCommentsInternal(loadMore: Boolean) {
        viewModelScope.launch {
            try {
                if (!loadMore) lastAllCommentsDoc = null

                val snapshot = withContext(Dispatchers.IO) {
                    var q = commentsCollection
                        .orderBy("createdAt", Query.Direction.DESCENDING)
                        .limit(ALL_COMMENTS_PAGE_SIZE.toLong())

                    if (loadMore && lastAllCommentsDoc != null) {
                        q = q.startAfter(lastAllCommentsDoc!!)
                    }

                    q.get().await()
                }

                if (!snapshot.isEmpty) {
                    lastAllCommentsDoc = snapshot.documents.last()
                }

                val comments = snapshot.documents.mapNotNull { d ->
                    val text = d.getString("text") ?: return@mapNotNull null
                    val millis = d.getLong("testMillis") ?: return@mapNotNull null

                    TestComment(
                        id = d.id,
                        gameId = d.getString("gameId") ?: "",
                        testMillis = millis,
                        text = text,
                        authorDevice = d.getString("authorDevice") ?: "",
                        createdAt = d.getTimestamp("createdAt"),
                        authorUid = d.getString("authorUid"),
                        authorName = d.getString("authorName"),
                        authorEmail = d.getString("authorEmail"),
                        authorPhotoUrl = d.getString("authorPhotoUrl"),
                        fromAccount = d.getBoolean("fromAccount") ?: false
                    )
                }

                val groupedNew = comments
                    .groupBy { it.testMillis }
                    .mapValues { (_, list) ->
                        list.sortedBy { it.createdAt?.toDate()?.time ?: 0L }
                    }

                val merged = if (loadMore) {
                    val current = _commentsByTest.value.toMutableMap()
                    groupedNew.forEach { (k, v) ->
                        val old = current[k].orEmpty()
                        current[k] = (old + v).distinctBy { it.id }
                    }
                    current
                } else {
                    groupedNew.toMutableMap()
                }

                _commentsByTest.value = merged

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }


    fun updateGameStatus(
        context: Context,
        gameId: String,
        newStatus: WorkStatus,

        testedAndroidVersion: String,
        testedDeviceModel: String,

        testedGpuModel: String,
        testedRam: String,
        testedWrapper: String,
        testedPerformanceMode: String,

        testedApp: String,
        testedAppVersion: String,
        testedGameVersionOrBuild: String,

        issueType: IssueType,
        reproducibility: Reproducibility,
        workaround: String,
        issueNote: String,

        emulatorBuildType: EmulatorBuildType,
        accuracyLevel: String,
        resolutionScale: String,
        asyncShaderEnabled: Boolean,
        frameSkip: String,

        resolutionWidth: String,
        resolutionHeight: String,
        fpsMin: String,
        fpsMax: String,

        mediaLink: String
    ) {
        val now = Timestamp.now()

        val formattedDate = now.toDate().let { date ->
            val formatter = SimpleDateFormat("d MMM yyyy • HH:mm", Locale.getDefault())
            formatter.format(date)
        }

        val currentGameTitle = _games.value.firstOrNull { it.id == gameId }?.title.orEmpty()
        val user = auth.currentUser

        val newTest = GameTestResult(
            status = newStatus,

            testedAndroidVersion = testedAndroidVersion,
            testedDeviceModel = testedDeviceModel,

            testedGpuModel = testedGpuModel,
            testedRam = testedRam,
            testedWrapper = testedWrapper,
            testedPerformanceMode = testedPerformanceMode,

            testedApp = testedApp,
            testedAppVersion = testedAppVersion,
            testedGameVersionOrBuild = testedGameVersionOrBuild,

            issueType = issueType,
            reproducibility = reproducibility,
            workaround = workaround,
            issueNote = issueNote,

            emulatorBuildType = emulatorBuildType,
            accuracyLevel = accuracyLevel,
            resolutionScale = resolutionScale,
            asyncShaderEnabled = asyncShaderEnabled,
            frameSkip = frameSkip,

            resolutionWidth = resolutionWidth,
            resolutionHeight = resolutionHeight,
            fpsMin = fpsMin,
            fpsMax = fpsMax,

            mediaLink = mediaLink,

            testedDateFormatted = formattedDate,
            updatedAtMillis = now.toDate().time,

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
            try {
                val data = mapOf(
                    "gameId" to gameId,
                    "title" to currentGameTitle,
                    "status" to newStatus.name,

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
                    "updatedAtMillis" to now.toDate().time,

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
                testsCollection.add(data).await()

                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        context.getString(R.string.toast_status_saved),
                        Toast.LENGTH_SHORT
                    ).show()
                }

                saveLastTestsSyncMillis(context, now.toDate().time)

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

    fun editTestResult(
        context: Context,
        gameId: String,
        testMillis: Long,
        newResult: GameTestResult
    ) {
        val user = auth.currentUser ?: return
        val game = _games.value.firstOrNull { it.id == gameId } ?: return
        val old = game.testResults.firstOrNull { it.updatedAtMillis == testMillis } ?: return

        if (old.authorUid != user.uid) return

        val updatedTests = game.testResults.map {
            if (it.updatedAtMillis == testMillis) {
                newResult.copy(
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
                val snap = testsCollection
                    .whereEqualTo("gameId", gameId)
                    .whereEqualTo("updatedAtMillis", testMillis)
                    .get()
                    .await()

                val doc = snap.documents.firstOrNull() ?: return@launch
                doc.reference.update(
                    mapOf(
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

                saveLastTestsSyncMillis(context, System.currentTimeMillis())

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
