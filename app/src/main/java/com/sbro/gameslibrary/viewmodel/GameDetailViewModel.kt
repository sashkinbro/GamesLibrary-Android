package com.sbro.gameslibrary.viewmodel

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
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
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale

class GameDetailViewModel : ViewModel() {

    private val firestore = FirebaseFirestore.getInstance()
    private val testsCollection = firestore.collection("gameTests")
    private val commentsCollection = firestore.collection("testComments")
    private val gameCommentsCollection = firestore.collection("gameComments")

    private val _game = MutableStateFlow<Game?>(null)
    val game = _game.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _isLoadingMoreComments = MutableStateFlow(false)
    val isLoadingMoreComments = _isLoadingMoreComments.asStateFlow()

    private val _gameComments = MutableStateFlow<List<TestComment>>(emptyList())
    val gameComments = _gameComments.asStateFlow()

    private val _commentsByTest =
        MutableStateFlow<Map<String, List<TestComment>>>(emptyMap())
    val commentsByTest = _commentsByTest.asStateFlow()

    private var appContext: Context? = null
    private var currentGameId: String? = null

    private var lastCommentsDoc: DocumentSnapshot? = null
    private var lastGameCommentDoc: DocumentSnapshot? = null
    private var lastTestsDoc: DocumentSnapshot? = null
    private val COMMENTS_PAGE_SIZE = 5
    private val TESTS_PAGE_SIZE = 5

    private var gamesCache: List<Game>? = null

    private val authManager = AuthManager(
        onLoggedIn = { _ -> FavoritesRepository.onUserLoggedIn() },
        onLoggedOut = { FavoritesRepository.onUserLoggedOut() }
    )
    val currentUser = authManager.currentUser

    private var favoritesObserveJob: Job? = null
    private val _hasMoreGameComments = MutableStateFlow(true)
    val hasMoreGameComments = _hasMoreGameComments.asStateFlow()

    private val _isLoadingMoreTests = MutableStateFlow(false)
    val isLoadingMoreTests = _isLoadingMoreTests.asStateFlow()

    private val _hasMoreTests = MutableStateFlow(true)
    val hasMoreTests = _hasMoreTests.asStateFlow()

    fun init(context: Context, gameId: String) {
        if (appContext == null) {
            appContext = context.applicationContext
            FavoritesRepository.init(appContext!!)
            authManager.setAppContext(appContext!!)
        }

        if (currentGameId == gameId && _game.value != null) return

        currentGameId = gameId
        _isLoading.value = true

        favoritesObserveJob?.cancel()
        favoritesObserveJob = null
        _hasMoreTests.value = true
        lastTestsDoc = null
        lastGameCommentDoc = null
        _hasMoreGameComments.value = true

        viewModelScope.launch {
            loadLocalGame(context, gameId)
            observeFavoritesForCurrentGame()

            coroutineScope {
                val testsDeferred = async { syncTestsForGame(context, gameId) }
                val testCommentsDeferred = async {
                    loadCommentsForGameInternal(
                        gameId = gameId,
                        loadMore = false,
                        showGlobalLoading = false
                    )
                }
                val gameCommentsDeferred = async {
                    loadGameCommentsInternal(
                        gameId = gameId,
                        loadMore = false,
                        showGlobalLoading = false
                    )
                }

                testsDeferred.await()
                testCommentsDeferred.await()
                gameCommentsDeferred.await()
            }

            _isLoading.value = false
        }
    }

    private fun observeFavoritesForCurrentGame() {
        val gid = currentGameId ?: return

        favoritesObserveJob = viewModelScope.launch {
            FavoritesRepository.favoriteIds.collectLatest { ids ->
                val g = _game.value
                if (g != null && g.id == gid) {
                    val fav = ids.contains(g.id)
                    if (fav != g.isFavorite) {
                        _game.value = g.copy(isFavorite = fav)
                    }
                }
            }
        }
    }

    private suspend fun loadLocalGame(context: Context, gameId: String) {
        if (gamesCache == null) {
            val result = withContext(Dispatchers.IO) {
                JsonParser.parseFromAssetsJson(context)
            }
            result.onSuccess { list ->
                gamesCache = list
            }
        }

        val list = gamesCache.orEmpty()
        val found = list.firstOrNull { it.id == gameId }

        if (found != null) {
            _game.value = found.copy(
                isFavorite = FavoritesRepository.isFavorite(found.id)
            )
        }
    }

    suspend fun syncTestsForGame(context: Context, gameId: String) {
        try {
            val snap = withContext(Dispatchers.IO) {
                var q = testsCollection
                    .whereEqualTo("gameId", gameId)
                    .orderBy("updatedAt", Query.Direction.DESCENDING)
                    .limit(TESTS_PAGE_SIZE.toLong())

                q.get().await()
            }

            val tests = snap.documents.mapNotNull { doc ->
                doc.toGameTestResult(gameId)
            }.sortedByDescending { it.updatedAtMillis }

            lastTestsDoc = snap.documents.lastOrNull()
            _hasMoreTests.value = snap.size() == TESTS_PAGE_SIZE

            val current = _game.value
            if (current != null) {
                _game.value = current.copy(testResults = tests)
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

    fun loadCommentsForGame(gameId: String, showGlobalLoading: Boolean = true) {
        viewModelScope.launch {
            loadCommentsForGameInternal(
                gameId = gameId,
                loadMore = false,
                showGlobalLoading = showGlobalLoading
            )
        }
    }

    fun loadMoreCommentsForGame(gameId: String) {
        viewModelScope.launch {
            loadCommentsForGameInternal(gameId, loadMore = true)
        }
    }

    fun loadMoreTestsForGame(gameId: String) {
        viewModelScope.launch {
            if (_isLoadingMoreTests.value || !_hasMoreTests.value) return@launch
            _isLoadingMoreTests.value = true
            try {
                val snap = withContext(Dispatchers.IO) {
                    var q = testsCollection
                        .whereEqualTo("gameId", gameId)
                        .orderBy("updatedAt", Query.Direction.DESCENDING)
                        .limit(TESTS_PAGE_SIZE.toLong())

                    if (lastTestsDoc != null) {
                        q = q.startAfter(lastTestsDoc!!)
                    }

                    q.get().await()
                }

                lastTestsDoc = snap.documents.lastOrNull()
                _hasMoreTests.value = snap.size() == TESTS_PAGE_SIZE

                val newTests = snap.documents.mapNotNull { d ->
                    d.toGameTestResult(gameId)
                }

                if (newTests.isNotEmpty()) {
                    val current = _game.value
                    if (current != null) {
                        val merged = (current.testResults + newTests)
                            .distinctBy { it.updatedAtMillis }
                            .sortedByDescending { it.updatedAtMillis }
                        _game.value = current.copy(testResults = merged)
                    }
                }
            } catch (_: Exception) {
                _hasMoreTests.value = false
            } finally {
                _isLoadingMoreTests.value = false
            }
        }
    }

    fun loadGameComments(gameId: String, showGlobalLoading: Boolean = true) {
        viewModelScope.launch {
            loadGameCommentsInternal(
                gameId = gameId,
                loadMore = false,
                showGlobalLoading = showGlobalLoading
            )
        }
    }

    fun loadMoreGameComments(gameId: String) {
        viewModelScope.launch {
            if (_isLoadingMoreComments.value || !_hasMoreGameComments.value) return@launch
            loadGameCommentsInternal(
                gameId = gameId,
                loadMore = true,
                showGlobalLoading = false
            )
        }
    }

    private suspend fun loadCommentsForGameInternal(
        gameId: String,
        loadMore: Boolean,
        showGlobalLoading: Boolean = true
    ) {
        if (loadMore) {
            _isLoadingMoreComments.value = true
        } else if (showGlobalLoading) {
            _isLoading.value = true
        }

        try {
            if (!loadMore) lastCommentsDoc = null

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
                val millis = d.getLong("testMillis") ?: 0L
                val testId = d.getString("testId").orEmpty()

                TestComment(
                    id = d.id,
                    gameId = d.getString("gameId") ?: gameId,
                    testId = testId,
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
                .groupBy { c -> c.testId.ifBlank { "legacy_${c.testMillis}" } }
                .mapValues { (_, list) ->
                    list.sortedBy { it.createdAt?.toDate()?.time ?: 0L }
                }

            val merged = if (loadMore) {
                val currentMap = _commentsByTest.value.toMutableMap()
                groupedNew.forEach { (k, v) ->
                    val old = currentMap[k].orEmpty()
                    currentMap[k] = (old + v).distinctBy { it.id }
                }
                currentMap
            } else groupedNew.toMutableMap()

            _commentsByTest.value = merged

        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            _isLoadingMoreComments.value = false
            if (!loadMore && showGlobalLoading) {
                _isLoading.value = false
            }
        }
    }

    private suspend fun loadGameCommentsInternal(
        gameId: String,
        loadMore: Boolean,
        showGlobalLoading: Boolean
    ) {
        if (loadMore) {
            _isLoadingMoreComments.value = true
        } else if (showGlobalLoading) {
            _isLoading.value = true
        }

        if (!loadMore) {
            _gameComments.value = emptyList()
            _hasMoreGameComments.value = true
        }

        try {
            if (!loadMore) lastGameCommentDoc = null

            val snapshot = withContext(Dispatchers.IO) {
                var q = gameCommentsCollection
                    .whereEqualTo("gameId", gameId)
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .limit(COMMENTS_PAGE_SIZE.toLong())

                if (loadMore && lastGameCommentDoc != null) {
                    q = q.startAfter(lastGameCommentDoc!!)
                }

                q.get().await()
            }

            if (!snapshot.isEmpty) {
                lastGameCommentDoc = snapshot.documents.last()
            }
            _hasMoreGameComments.value = snapshot.size() == COMMENTS_PAGE_SIZE

            val comments = snapshot.documents.mapNotNull { d ->
                val text = d.getString("text") ?: return@mapNotNull null

                TestComment(
                    id = d.id,
                    gameId = d.getString("gameId") ?: gameId,
                    testId = "",
                    testMillis = 0L,
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

            val merged = if (loadMore) {
                (_gameComments.value + comments).distinctBy { it.id }
            } else comments

            _gameComments.value = merged
        } catch (e: Exception) {
            e.printStackTrace()
            if (!loadMore) _gameComments.value = emptyList()
            _hasMoreGameComments.value = false
        } finally {
            _isLoadingMoreComments.value = false
            if (!loadMore && showGlobalLoading) {
                _isLoading.value = false
            }
        }
    }

    fun toggleFavorite() {
        val g = _game.value ?: return
        FavoritesRepository.toggleFavorite(g.id)
    }

    fun addTestComment(
        context: Context,
        gameId: String,
        testId: String,
        testMillis: Long,
        text: String
    ) {
        val user = currentUser.value
        if (user == null) {
            Toast.makeText(
                context,
                context.getString(R.string.login_to_comment),
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val data = hashMapOf(
            "gameId" to gameId,
            "testId" to testId,
            "testMillis" to testMillis,
            "text" to text.trim(),
            "createdAt" to Timestamp.now(),
            "authorDevice" to android.os.Build.MODEL,

            "fromAccount" to true,
            "authorUid" to user.uid,
            "authorName" to user.displayName,
            "authorEmail" to user.email,
            "authorPhotoUrl" to user.photoUrl?.toString()
        )

        viewModelScope.launch(Dispatchers.IO) {
            try {
                commentsCollection.add(data).await()
                loadCommentsForGame(gameId, showGlobalLoading = false)
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

    fun addGameComment(
        context: Context,
        gameId: String,
        text: String
    ) {
        val user = currentUser.value
        if (user == null) {
            Toast.makeText(
                context,
                context.getString(R.string.login_to_comment),
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val data = hashMapOf(
            "gameId" to gameId,
            "text" to text.trim(),
            "createdAt" to Timestamp.now(),
            "authorDevice" to android.os.Build.MODEL,

            "fromAccount" to true,
            "authorUid" to user.uid,
            "authorName" to user.displayName,
            "authorEmail" to user.email,
            "authorPhotoUrl" to user.photoUrl?.toString()
        )

        viewModelScope.launch(Dispatchers.IO) {
            try {
                gameCommentsCollection.add(data).await()
                loadGameComments(gameId, showGlobalLoading = false)
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

    fun editComment(
        context: Context,
        commentId: String,
        newText: String
    ) {
        val text = newText.trim()
        if (text.isBlank()) return
        val user = currentUser.value
        if (user == null) {
            Toast.makeText(
                context,
                context.getString(R.string.login_required),
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val user = currentUser.value
                if (user == null) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            context,
                            context.getString(R.string.login_required),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    return@launch
                }

                val docRef = commentsCollection.document(commentId)
                val snap = docRef.get().await()
                val authorUid = snap.getString("authorUid")

                if (authorUid != user.uid) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            context,
                            context.getString(R.string.cannot_edit_other_comment),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    return@launch
                }

                docRef.update(
                    mapOf(
                        "text" to text,
                        "editedAt" to Timestamp.now()
                    )
                ).await()

                currentGameId?.let { loadCommentsForGame(it, showGlobalLoading = false) }

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

    fun editGameComment(
        context: Context,
        commentId: String,
        newText: String
    ) {
        val text = newText.trim()
        if (text.isBlank()) return
        val user = currentUser.value
        if (user == null) {
            Toast.makeText(
                context,
                context.getString(R.string.login_required),
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val docRef = gameCommentsCollection.document(commentId)
                val snap = docRef.get().await()
                val authorUid = snap.getString("authorUid")

                if (authorUid != user.uid) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            context,
                            context.getString(R.string.cannot_edit_other_comment),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    return@launch
                }

                docRef.update(
                    mapOf(
                        "text" to text,
                        "editedAt" to Timestamp.now()
                    )
                ).await()

                currentGameId?.let { loadGameComments(it, showGlobalLoading = false) }

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

    data class NewTestPayload(
        val newStatus: WorkStatus,

        val testedAndroidVersion: String,
        val testedDeviceModel: String,
        val testedGpuModel: String,
        val testedDriverVersion: String,
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

        val mediaLink: String,
        val audioDriver: String,
        val downloadSize: String,
        val wineVersion: String,
        val winlatorFork: String,
        val turnipVersion: String,
        val controllerSupport: String,
        val box64Preset: String,
        val box64Version: String,
        val startupSelection: String,
        val envVariables: String,
        val vkd3dVersion: String,
        val dxvkVersion: String,
        val anisotropicFilter: String,
        val antiAliasing: String,
        val vSync: String,
        val windowAdaptingFilter: String,
        val spuThreads: String,
        val spuBlockSize: String,
        val dockedMode: Boolean,
        val audioOutputEngine: String,
        val diskShaderCache: Boolean,
        val reactiveFlushing: Boolean,
        val cpuBackend: String
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
            val formatter = SimpleDateFormat("d MMM yyyy, HH:mm", Locale.getDefault())
            formatter.format(date)
        }

        val currentGameTitle = _game.value?.title.orEmpty()
        val user = currentUser.value
        if (user == null) {
            Toast.makeText(
                context,
                context.getString(R.string.login_to_save_tests),
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val newTest = GameTestResult(
            testId = testId,
            status = payload.newStatus,

            testedAndroidVersion = payload.testedAndroidVersion,
            testedDeviceModel = payload.testedDeviceModel,
            testedGpuModel = payload.testedGpuModel,
            testedDriverVersion = payload.testedDriverVersion,
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

            authorUid = user.uid,
            authorName = user.displayName,
            authorEmail = user.email,
            authorPhotoUrl = user.photoUrl?.toString(),
            fromAccount = true,
            audioDriver = payload.audioDriver,
            downloadSize = payload.downloadSize,
            wineVersion = payload.wineVersion,
            winlatorFork = payload.winlatorFork,
            turnipVersion = payload.turnipVersion,
            controllerSupport = payload.controllerSupport,
            box64Preset = payload.box64Preset,
            box64Version = payload.box64Version,
            startupSelection = payload.startupSelection,
            envVariables = payload.envVariables,
            vkd3dVersion = payload.vkd3dVersion,
            dxvkVersion = payload.dxvkVersion,
            anisotropicFilter = payload.anisotropicFilter,
            antiAliasing = payload.antiAliasing,
            vSync = payload.vSync,
            windowAdaptingFilter = payload.windowAdaptingFilter,
            spuThreads = payload.spuThreads,
            spuBlockSize = payload.spuBlockSize,
            dockedMode = payload.dockedMode,
            audioOutputEngine = payload.audioOutputEngine,
            diskShaderCache = payload.diskShaderCache,
            reactiveFlushing = payload.reactiveFlushing,
            cpuBackend = payload.cpuBackend
        )

        val current = _game.value
        if (current != null && current.id == gameId) {
            val updatedLocal = current.copy(
                testResults = (current.testResults + newTest)
                    .sortedByDescending { it.updatedAtMillis }
            )
            _game.value = updatedLocal
        }

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

    fun editTestResult(
        context: Context,
        gameId: String,
        testMillis: Long,
        newResult: GameTestResult
    ) {
        val user = currentUser.value ?: return
        val current = _game.value ?: return
        if (current.id != gameId) return

        val old = current.testResults.firstOrNull { it.updatedAtMillis == testMillis } ?: return
        if (old.authorUid != user.uid) return

        val updatedTests = current.testResults.map {
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

        _game.value = current.copy(testResults = updatedTests)

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
                        "testedDriverVersion" to newResult.testedDriverVersion,
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
                        "mediaLink" to newResult.mediaLink,
                        "audioDriver" to newResult.audioDriver,
                        "downloadSize" to newResult.downloadSize,
                        "wineVersion" to newResult.wineVersion,
                        "winlatorFork" to newResult.winlatorFork,
                        "turnipVersion" to newResult.turnipVersion,
                        "controllerSupport" to newResult.controllerSupport,
                        "box64Preset" to newResult.box64Preset,
                        "box64Version" to newResult.box64Version,
                        "startupSelection" to newResult.startupSelection,
                        "envVariables" to newResult.envVariables,
                        "vkd3dVersion" to newResult.vkd3dVersion,
                        "dxvkVersion" to newResult.dxvkVersion,
                        "anisotropicFilter" to newResult.anisotropicFilter,
                        "antiAliasing" to newResult.antiAliasing,
                        "vSync" to newResult.vSync,
                        "windowAdaptingFilter" to newResult.windowAdaptingFilter,
                        "spuThreads" to newResult.spuThreads,
                        "spuBlockSize" to newResult.spuBlockSize,
                        "dockedMode" to newResult.dockedMode,
                        "audioOutputEngine" to newResult.audioOutputEngine,
                        "diskShaderCache" to newResult.diskShaderCache,
                        "reactiveFlushing" to newResult.reactiveFlushing,
                        "cpuBackend" to newResult.cpuBackend
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
            "testedDriverVersion" to testedDriverVersion,
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
            "fromAccount" to true,
            "audioDriver" to audioDriver,
            "downloadSize" to downloadSize,
            "wineVersion" to wineVersion,
            "winlatorFork" to winlatorFork,
            "turnipVersion" to turnipVersion,
            "controllerSupport" to controllerSupport,
            "box64Preset" to box64Preset,
            "box64Version" to box64Version,
            "startupSelection" to startupSelection,
            "envVariables" to envVariables,
            "vkd3dVersion" to vkd3dVersion,
            "dxvkVersion" to dxvkVersion,
            "anisotropicFilter" to anisotropicFilter,
            "antiAliasing" to antiAliasing,
            "vSync" to vSync,
            "windowAdaptingFilter" to windowAdaptingFilter,
            "spuThreads" to spuThreads,
            "spuBlockSize" to spuBlockSize,
            "dockedMode" to dockedMode,
            "audioOutputEngine" to audioOutputEngine,
            "diskShaderCache" to diskShaderCache,
            "reactiveFlushing" to reactiveFlushing,
            "cpuBackend" to cpuBackend
        )
    }

    fun refresh(context: Context, gameId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            currentGameId = gameId
            lastTestsDoc = null
            _hasMoreTests.value = true
            lastGameCommentDoc = null
            _hasMoreGameComments.value = true

            favoritesObserveJob?.cancel()
            favoritesObserveJob = null
            observeFavoritesForCurrentGame()

            loadLocalGame(context, gameId)
            coroutineScope {
                val testsDeferred = async { syncTestsForGame(context, gameId) }
                val testCommentsDeferred = async {
                    loadCommentsForGameInternal(
                        gameId = gameId,
                        loadMore = false,
                        showGlobalLoading = false
                    )
                }
                val gameCommentsDeferred = async {
                    loadGameCommentsInternal(
                        gameId = gameId,
                        loadMore = false,
                        showGlobalLoading = false
                    )
                }
                testsDeferred.await()
                testCommentsDeferred.await()
                gameCommentsDeferred.await()
            }

            _isLoading.value = false
        }
    }

    private fun DocumentSnapshot.toGameTestResult(gameId: String): GameTestResult? {
        return try {
            val statusStr = getString("status") ?: WorkStatus.UNTESTED.name

            val testedAndroidVersion = getString("testedAndroidVersion") ?: ""
            val testedDeviceModel = getString("testedDeviceModel") ?: ""
            val testedGpuModel = getString("testedGpuModel") ?: ""
            val testedDriverVersion = getString("testedDriverVersion") ?: ""
            val testedRam = getString("testedRam") ?: ""
            val testedWrapper = getString("testedWrapper") ?: ""
            val testedPerformanceMode = getString("testedPerformanceMode") ?: ""

            val testedApp = getString("testedApp") ?: ""
            val testedAppVersion = getString("testedAppVersion") ?: ""
            val testedGameVersionOrBuild = getString("testedGameVersionOrBuild") ?: ""

            val issueType = getString("issueType") ?: IssueType.CRASH.firestoreValue
            val reproducibility =
                getString("reproducibility") ?: Reproducibility.ALWAYS.firestoreValue
            val workaround = getString("workaround") ?: ""
            val issueNote = getString("issueNote") ?: ""

            val emulatorBuildType =
                getString("emulatorBuildType") ?: EmulatorBuildType.STABLE.firestoreValue
            val accuracyLevel = getString("accuracyLevel") ?: ""
            val resolutionScale = getString("resolutionScale") ?: ""
            val asyncShaderEnabled = getBoolean("asyncShaderEnabled") ?: false
            val frameSkip = getString("frameSkip") ?: ""

            val updatedAt = getTimestamp("updatedAt")
            val updatedAtMillis = updatedAt?.toDate()?.time ?: 0L

            val resolutionWidth = getString("resolutionWidth") ?: ""
            val resolutionHeight = getString("resolutionHeight") ?: ""
            val fpsMin = getString("fpsMin") ?: ""
            val fpsMax = getString("fpsMax") ?: ""

            val mediaLink = getString("mediaLink") ?: ""

            val authorUid = getString("authorUid")
            val authorName = getString("authorName")
            val authorEmail = getString("authorEmail")
            val authorPhotoUrl = getString("authorPhotoUrl")
            val fromAccount = getBoolean("fromAccount") ?: false
            val audioDriver = getString("audioDriver") ?: ""
            val downloadSize = getString("downloadSize") ?: ""
            val wineVersion = getString("wineVersion") ?: ""
            val winlatorFork = getString("winlatorFork") ?: ""
            val turnipVersion = getString("turnipVersion") ?: ""
            val controllerSupport = getString("controllerSupport") ?: ""
            val box64Preset = getString("box64Preset") ?: ""
            val box64Version = getString("box64Version") ?: ""
            val startupSelection = getString("startupSelection") ?: ""
            val envVariables = getString("envVariables") ?: ""
            val vkd3dVersion = getString("vkd3dVersion") ?: ""
            val dxvkVersion = getString("dxvkVersion") ?: ""

            val anisotropicFilter = getString("anisotropicFilter") ?: ""
            val antiAliasing = getString("antiAliasing") ?: ""
            val vSync = getString("vSync") ?: ""
            val windowAdaptingFilter = getString("windowAdaptingFilter") ?: ""

            val spuThreads = getString("spuThreads") ?: ""
            val spuBlockSize = getString("spuBlockSize") ?: ""

            val dockedMode = getBoolean("dockedMode") ?: false
            val audioOutputEngine = getString("audioOutputEngine") ?: ""
            val diskShaderCache = getBoolean("diskShaderCache") ?: false
            val reactiveFlushing = getBoolean("reactiveFlushing") ?: false
            val cpuBackend = getString("cpuBackend") ?: ""


            val formattedDate = updatedAt?.toDate()?.let { date ->
                val formatter =
                    SimpleDateFormat("d MMM yyyy, HH:mm", Locale.getDefault())
                formatter.format(date)
            } ?: ""

            val testIdFromCloud = getString("testId").orEmpty()
            val finalTestId =
                testIdFromCloud.ifBlank { "${gameId}_${updatedAtMillis}" }

            GameTestResult(
                testId = finalTestId,
                status = when (statusStr) {
                    WorkStatus.WORKING.name -> WorkStatus.WORKING
                    WorkStatus.NOT_WORKING.name -> WorkStatus.NOT_WORKING
                    else -> WorkStatus.UNTESTED
                },

                testedAndroidVersion = testedAndroidVersion,
                testedDeviceModel = testedDeviceModel,
                testedGpuModel = testedGpuModel,
                testedDriverVersion = testedDriverVersion,
                testedRam = testedRam,
                testedWrapper = testedWrapper,
                testedPerformanceMode = testedPerformanceMode,

                testedApp = testedApp,
                testedAppVersion = testedAppVersion,
                testedGameVersionOrBuild = testedGameVersionOrBuild,

                issueType = IssueType.fromFirestore(issueType),
                reproducibility = Reproducibility.fromFirestore(reproducibility),
                workaround = workaround,
                issueNote = issueNote,

                emulatorBuildType = EmulatorBuildType.fromFirestore(emulatorBuildType),
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
                updatedAtMillis = updatedAtMillis,

                authorUid = authorUid,
                authorName = authorName,
                authorEmail = authorEmail,
                authorPhotoUrl = authorPhotoUrl,
                fromAccount = fromAccount,
                audioDriver = audioDriver,
                downloadSize = downloadSize,
                wineVersion = wineVersion,
                winlatorFork = winlatorFork,
                turnipVersion = turnipVersion,
                controllerSupport = controllerSupport,
                box64Preset = box64Preset,
                box64Version = box64Version,
                startupSelection = startupSelection,
                envVariables = envVariables,
                vkd3dVersion = vkd3dVersion,
                dxvkVersion = dxvkVersion,
                anisotropicFilter = anisotropicFilter,
                antiAliasing = antiAliasing,
                vSync = vSync,
                windowAdaptingFilter = windowAdaptingFilter,
                spuThreads = spuThreads,
                spuBlockSize = spuBlockSize,
                dockedMode = dockedMode,
                audioOutputEngine = audioOutputEngine,
                diskShaderCache = diskShaderCache,
                reactiveFlushing = reactiveFlushing,
                cpuBackend = cpuBackend
            )
        } catch (_: Exception) {
            null
        }
    }

    override fun onCleared() {
        favoritesObserveJob?.cancel()
        authManager.clear()
        super.onCleared()
    }
}
