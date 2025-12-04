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

    private val _game = MutableStateFlow<Game?>(null)
    val game = _game.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _isLoadingMoreComments = MutableStateFlow(false)
    val isLoadingMoreComments = _isLoadingMoreComments.asStateFlow()

    private val _commentsByTest =
        MutableStateFlow<Map<String, List<TestComment>>>(emptyMap())
    val commentsByTest = _commentsByTest.asStateFlow()

    private var appContext: Context? = null
    private var currentGameId: String? = null

    private var lastCommentsDoc: DocumentSnapshot? = null
    private val COMMENTS_PAGE_SIZE = 50

    private var gamesCache: List<Game>? = null

    private val authManager = AuthManager(
        onLoggedIn = { _ -> FavoritesRepository.onUserLoggedIn() },
        onLoggedOut = { FavoritesRepository.onUserLoggedOut() }
    )
    val currentUser = authManager.currentUser

    private var favoritesObserveJob: Job? = null

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

        viewModelScope.launch {
            loadLocalGame(context, gameId)
            observeFavoritesForCurrentGame()

            coroutineScope {
                val testsDeferred = async { syncTestsForGame(context, gameId) }
                val commentsDeferred = async {
                    loadCommentsForGameInternal(gameId, loadMore = false)
                }

                testsDeferred.await()
                commentsDeferred.await()
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
                try {
                    testsCollection
                        .whereEqualTo("gameId", gameId)
                        .orderBy("updatedAt", Query.Direction.DESCENDING)
                        .get()
                        .await()
                } catch (e: Exception) {
                    val msg = e.message.orEmpty()
                    if (msg.contains("requires an index", true) ||
                        msg.contains("FAILED_PRECONDITION", true)
                    ) {
                        testsCollection
                            .whereEqualTo("gameId", gameId)
                            .get()
                            .await()
                    } else throw e
                }
            }

            val tests = snap.documents.mapNotNull { doc ->
                doc.toGameTestResult(gameId)
            }.sortedByDescending { it.updatedAtMillis }

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

    fun loadCommentsForGame(gameId: String) {
        viewModelScope.launch {
            loadCommentsForGameInternal(gameId, loadMore = false)
        }
    }

    fun loadMoreCommentsForGame(gameId: String) {
        viewModelScope.launch {
            loadCommentsForGameInternal(gameId, loadMore = true)
        }
    }

    private suspend fun loadCommentsForGameInternal(gameId: String, loadMore: Boolean) {
        if (loadMore) {
            _isLoadingMoreComments.value = true
        } else {
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
                .groupBy { c ->
                    c.testId.ifBlank { "legacy_${c.testMillis}" }
                }
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
            if (!loadMore) _isLoading.value = false
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
        val isAcc = user != null

        val data = hashMapOf(
            "gameId" to gameId,
            "testId" to testId,
            "testMillis" to testMillis,
            "text" to text.trim(),
            "createdAt" to Timestamp.now(),
            "authorDevice" to android.os.Build.MODEL,

            "fromAccount" to isAcc,
            "authorUid" to user?.uid,
            "authorName" to user?.displayName,
            "authorEmail" to user?.email,
            "authorPhotoUrl" to user?.photoUrl?.toString()
        )

        viewModelScope.launch(Dispatchers.IO) {
            try {
                commentsCollection.add(data).await()
                loadCommentsForGame(gameId)
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

        viewModelScope.launch(Dispatchers.IO) {
            try {
                commentsCollection.document(commentId)
                    .update(
                        mapOf(
                            "text" to text,
                            "editedAt" to Timestamp.now()
                        )
                    )
                    .await()
                currentGameId?.let { loadCommentsForGame(it) }
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

    private fun DocumentSnapshot.toGameTestResult(gameId: String): GameTestResult? {
        return try {
            val statusStr = getString("status") ?: WorkStatus.UNTESTED.name

            val testedAndroidVersion = getString("testedAndroidVersion") ?: ""
            val testedDeviceModel = getString("testedDeviceModel") ?: ""
            val testedGpuModel = getString("testedGpuModel") ?: ""
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

            val formattedDate = updatedAt?.toDate()?.let { date ->
                val formatter =
                    SimpleDateFormat("d MMM yyyy • HH:mm", Locale.getDefault())
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
                fromAccount = fromAccount
            )
        } catch (_: Exception) {
            null
        }
    }
}
