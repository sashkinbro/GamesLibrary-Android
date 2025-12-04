package com.sbro.gameslibrary.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.sbro.gameslibrary.auth.AuthManager
import com.sbro.gameslibrary.components.Game
import com.sbro.gameslibrary.util.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class MyCommentsViewModel : ViewModel() {

    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val commentsCollection = firestore.collection("testComments")

    private val _games = MutableStateFlow(emptyList<Game>())
    val games: StateFlow<List<Game>> = _games.asStateFlow()

    private val _commentsByTest =
        MutableStateFlow<Map<String, List<TestComment>>>(emptyMap())
    val commentsByTest: StateFlow<Map<String, List<TestComment>>> =
        _commentsByTest.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _hasLoadedComments = MutableStateFlow(false)
    val hasLoadedComments: StateFlow<Boolean> = _hasLoadedComments.asStateFlow()

    private var appContext: Context? = null

    private var lastMyCommentsDoc: DocumentSnapshot? = null
    private val PAGE_SIZE = 100

    private var commentsJob: Job? = null
    private var initJob: Job? = null

    private val authManager = AuthManager(
        onLoggedIn = { loadMyComments() },
        onLoggedOut = {
            viewModelScope.launch(Dispatchers.Main) {
                _commentsByTest.value = emptyMap()
                _hasLoadedComments.value = false
            }
        }
    )
    val currentUser = authManager.currentUser

    fun init(context: Context) {
        if (appContext == null) {
            appContext = context.applicationContext
            authManager.setAppContext(appContext!!)
        }
        if (_games.value.isNotEmpty() || initJob != null) return

        initJob = viewModelScope.launch {
            withContext(Dispatchers.Main) {
                _isLoading.value = true
            }

            val parsedGames = withContext(Dispatchers.IO) {
                JsonParser.parseFromAssetsJson(context)
                    .getOrElse { emptyList() }
            }

            withContext(Dispatchers.Main) {
                _games.value = parsedGames
                _isLoading.value = false
                initJob = null
            }
        }
    }

    fun loadMyComments() {
        loadMyCommentsInternal(loadMore = false)
    }

    fun loadMoreMyComments() {
        loadMyCommentsInternal(loadMore = true)
    }

    private fun loadMyCommentsInternal(loadMore: Boolean) {
        commentsJob?.cancel()

        commentsJob = viewModelScope.launch(Dispatchers.IO) {
            if (!loadMore) {
                withContext(Dispatchers.Main) {
                    _hasLoadedComments.value = false
                }
            }
            withContext(Dispatchers.Main) {
                _isLoading.value = true
            }

            try {
                val uid = currentUser.value?.uid
                if (uid == null) {
                    withContext(Dispatchers.Main) {
                        _commentsByTest.value = emptyMap()
                    }
                    return@launch
                }

                if (!loadMore) lastMyCommentsDoc = null

                var q = commentsCollection
                    .whereEqualTo("authorUid", uid)
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .limit(PAGE_SIZE.toLong())

                if (loadMore && lastMyCommentsDoc != null) {
                    q = q.startAfter(lastMyCommentsDoc!!)
                }

                val snapshot = q.get().await()

                if (!snapshot.isEmpty) {
                    lastMyCommentsDoc = snapshot.documents.last()
                }

                val comments = snapshot.documents.mapNotNull { d ->
                    val text = d.getString("text") ?: return@mapNotNull null

                    val testId = d.getString("testId") ?: ""
                    val testMillis = d.getLong("testMillis") ?: 0L

                    TestComment(
                        id = d.id,
                        gameId = d.getString("gameId") ?: "",
                        testId = testId,
                        testMillis = testMillis,
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
                    val current = _commentsByTest.value.toMutableMap()
                    groupedNew.forEach { (k, v) ->
                        val old = current[k].orEmpty()
                        current[k] = (old + v).distinctBy { it.id }
                    }
                    current
                } else {
                    groupedNew.toMutableMap()
                }

                withContext(Dispatchers.Main) {
                    _commentsByTest.value = merged
                }

            } catch (_: Exception) {
                if (!loadMore) {
                    withContext(Dispatchers.Main) {
                        _commentsByTest.value = emptyMap()
                    }
                }
            } finally {
                withContext(Dispatchers.Main) {
                    _isLoading.value = false
                    if (!loadMore) _hasLoadedComments.value = true
                }
            }
        }
    }
}
