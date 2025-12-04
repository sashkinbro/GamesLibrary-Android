package com.sbro.gameslibrary.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseUser
import com.sbro.gameslibrary.auth.AuthManager
import com.sbro.gameslibrary.components.Game
import com.sbro.gameslibrary.util.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ProfileUiState(
    val user: FirebaseUser? = null,
    val games: List<Game> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class ProfileViewModel : ViewModel() {

    private val _state = MutableStateFlow(ProfileUiState())
    val state: StateFlow<ProfileUiState> = _state.asStateFlow()

    private var appContext: Context? = null
    private var favoritesJob: Job? = null

    private val authManager = AuthManager(
        onLoggedIn = { ctx ->
            if (ctx != null) setContextIfNeeded(ctx)
            FavoritesRepository.onUserLoggedIn()
        },
        onLoggedOut = {
            FavoritesRepository.onUserLoggedOut()
            _state.value = _state.value.copy(user = null)
        }
    )

    val currentUser = authManager.currentUser

    init {
        viewModelScope.launch {
            currentUser.collect { user ->
                _state.value = _state.value.copy(user = user)

                val ctx = appContext
                if (ctx != null) {
                    refresh(ctx)
                }
            }
        }
    }

    fun init(context: Context) {
        setContextIfNeeded(context)
        observeFavorites()
        refresh(context)
    }

    private fun setContextIfNeeded(context: Context) {
        if (appContext == null) {
            appContext = context.applicationContext
            FavoritesRepository.init(appContext!!)
            authManager.setAppContext(appContext!!)
        }
    }

    private fun observeFavorites() {
        favoritesJob?.cancel()
        favoritesJob = viewModelScope.launch {
            FavoritesRepository.favoriteIds.collectLatest { ids ->
                val currentGames = _state.value.games
                if (currentGames.isEmpty()) return@collectLatest

                val updatedGames = currentGames.map { g ->
                    val fav = ids.contains(g.id)
                    if (g.isFavorite == fav) g else g.copy(isFavorite = fav)
                }

                if (updatedGames != currentGames) {
                    _state.value = _state.value.copy(games = updatedGames)
                }
            }
        }
    }

    fun refresh(context: Context) {
        viewModelScope.launch {
            val user = currentUser.value

            _state.value = _state.value.copy(
                isLoading = true,
                errorMessage = null,
                user = user
            )

            val gamesFromAssets = withContext(Dispatchers.IO) {
                JsonParser.parseFromAssetsJson(context)
                    .getOrDefault(emptyList())
            }

            try {
                val favoriteIds = FavoritesRepository.favoriteIds.value
                val mergedGames = gamesFromAssets.map { g ->
                    g.copy(isFavorite = favoriteIds.contains(g.id))
                }

                _state.value = _state.value.copy(
                    games = mergedGames,
                    isLoading = false
                )
            } catch (e: Exception) {
                e.printStackTrace()
                _state.value = _state.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "Unknown error"
                )
            }
        }
    }

    fun getGoogleSignInIntent(context: Context) =
        authManager.getGoogleSignInIntent(context)

    fun handleGoogleResult(
        context: Context,
        data: android.content.Intent?,
        onError: (String) -> Unit = {}
    ) {
        setContextIfNeeded(context)
        authManager.handleGoogleResult(context, data, onError)
    }

    fun signOut(context: Context) {
        setContextIfNeeded(context)
        authManager.signOut(context)
    }

    override fun onCleared() {
        favoritesJob?.cancel()
        super.onCleared()
    }
}
