package com.sbro.gameslibrary.viewmodel

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.concurrent.atomic.AtomicBoolean

object FavoritesRepository {

    private const val PREFS_NAME = "favorites_prefs"
    private const val KEY_IDS = "favorite_ids"
    private const val KEY_IDS_PREFIX = "favorite_ids_"

    private val initLock = Any()
    private val initialized = AtomicBoolean(false)

    private var prefs: SharedPreferences? = null

    private var job: Job = Job()
    private var scope: CoroutineScope = CoroutineScope(Dispatchers.IO + job)

    @SuppressLint("StaticFieldLeak")
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val favoritesCollection = firestore.collection("userFavorites")

    private val _favoriteIds = MutableStateFlow<Set<String>>(emptySet())
    val favoriteIds: StateFlow<Set<String>> = _favoriteIds.asStateFlow()

    private var cloudListener: ListenerRegistration? = null
    @Volatile private var currentUid: String? = null

    fun init(context: Context) {
        if (initialized.get()) return

        synchronized(initLock) {
            if (initialized.get()) return

            prefs = context.applicationContext
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

            _favoriteIds.value = readLocal()

            if (!job.isActive) {
                job = Job()
                scope = CoroutineScope(Dispatchers.IO + job)
            }

            initialized.set(true)
        }

        scope.launch { syncFromCloudIfLoggedIn() }
        attachCloudListenerIfLoggedIn()
    }

    fun clear() {
        detachCloudListener()
        currentUid = null

        scope.cancel()
        initialized.set(false)
        prefs = null
        _favoriteIds.value = emptySet()
    }

    fun isFavorite(id: String): Boolean {
        requirePrefs()
        return _favoriteIds.value.contains(id)
    }

    fun toggleFavorite(gameId: String) {
        requirePrefs()

        val current = _favoriteIds.value.toMutableSet()
        if (current.contains(gameId)) current.remove(gameId) else current.add(gameId)

        _favoriteIds.value = current
        writeLocal(current)

        val user = auth.currentUser ?: return

        scope.launch {
            val docRef = favoritesCollection.document(user.uid)
            try {
                docRef.set(mapOf("gameIds" to current.toList())).await()
            } catch (_: Exception) {
            }
        }
    }

    fun onUserLoggedIn() {
        requirePrefs()

        val uid = auth.currentUser?.uid ?: return
        if (currentUid != uid) {
            detachCloudListener()
            currentUid = uid
            _favoriteIds.value = readLocal(uid)
        }

        scope.launch { syncFromCloudIfLoggedIn() }
        attachCloudListenerIfLoggedIn()
    }

    fun onUserLoggedOut() {
        detachCloudListener()
        currentUid = null
        // Перемикаємося на гостьовий профіль, щоб не показувати чужі вподобані
        _favoriteIds.value = readLocal(null)
    }

    private fun attachCloudListenerIfLoggedIn() {
        requirePrefs()
        val user = auth.currentUser ?: return

        if (cloudListener != null && currentUid == user.uid) return
        currentUid = user.uid

        cloudListener = favoritesCollection.document(user.uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                if (snapshot == null || !snapshot.exists()) return@addSnapshotListener

                val cloudIds =
                    (snapshot.get("gameIds") as? List<*>)?.filterIsInstance<String>()?.toSet()
                        ?: emptySet()

                val localIds = readLocal()
                val merged = (cloudIds + localIds).toSet()

                if (merged != _favoriteIds.value) {
                    _favoriteIds.value = merged
                    writeLocal(merged)
                }
            }
    }

    private fun detachCloudListener() {
        cloudListener?.remove()
        cloudListener = null
    }

    private suspend fun syncFromCloudIfLoggedIn() {
        requirePrefs()

        val user = auth.currentUser ?: return
        val docRef = favoritesCollection.document(user.uid)

        try {
            val snap = docRef.get().await()

            val cloudIds =
                (snap.get("gameIds") as? List<*>)?.filterIsInstance<String>()?.toSet()
                    ?: emptySet()

            val localIds = readLocal(user.uid)
            val merged = (localIds + cloudIds).toSet()

            if (merged != localIds) {
                _favoriteIds.value = merged
                writeLocal(merged)
            }

            if (cloudIds.isEmpty() && merged.isNotEmpty()) {
                try {
                    docRef.set(mapOf("gameIds" to merged.toList())).await()
                } catch (_: Exception) {}
            }

        } catch (e: FirebaseFirestoreException) {
            if (e.code == FirebaseFirestoreException.Code.NOT_FOUND) {
                val localIds = readLocal(user.uid)
                try {
                    docRef.set(mapOf("gameIds" to localIds.toList())).await()
                } catch (_: Exception) {}
            }
        } catch (_: Exception) {}
    }

    private fun readLocal(uid: String? = auth.currentUser?.uid): Set<String> {
        val p = requirePrefs()
        val key = storageKeyFor(uid)
        val scoped = p.getStringSet(key, null)

        // fallback на старий ключ, щоб не загубити дані після міграції
        if (scoped == null) {
            val legacy = p.getStringSet(KEY_IDS, emptySet()) ?: emptySet()
            if (legacy.isNotEmpty()) {
                writeLocal(legacy, uid)
            }
            return legacy
        }
        return scoped
    }

    private fun writeLocal(ids: Set<String>, uid: String? = auth.currentUser?.uid) {
        val p = requirePrefs()
        p.edit { putStringSet(storageKeyFor(uid), ids) }
    }

    private fun storageKeyFor(uid: String?): String {
        val safeUid = uid?.ifBlank { null }
        return if (safeUid == null) "${KEY_IDS_PREFIX}guest" else "$KEY_IDS_PREFIX$safeUid"
    }

    private fun requirePrefs(): SharedPreferences {
        return prefs ?: throw IllegalStateException(
            "FavoritesRepository is not initialized. Call FavoritesRepository.init(context) before use."
        )
    }
}
