package com.sbro.gameslibrary.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.sbro.gameslibrary.util.PhoneDbItem
import com.sbro.gameslibrary.util.loadPhonesFromAssets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Locale

data class MyDevicesState(
    val isLoggedIn: Boolean = false,
    val isLoading: Boolean = false,
    val deviceIds: List<Int> = emptyList(),
    val devices: List<PhoneDbItem> = emptyList(),
    val error: String? = null
)

class MyDevicesViewModel : ViewModel() {

    companion object {
        private const val MAX_DEVICES = 5
        private const val SEARCH_LIMIT = 40
    }

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private var sub: ListenerRegistration? = null
    private var phoneDbCache: List<PhoneDbItem> = emptyList()

    private var inited = false
    private var currentUid: String? = null

    private val _state = MutableStateFlow(MyDevicesState())
    val state: StateFlow<MyDevicesState> = _state

    private val authListener = FirebaseAuth.AuthStateListener { fa ->
        val user = fa.currentUser
        val uid = user?.uid

        if (uid == null) {
            detachDevicesListener()
            currentUid = null
            _state.update {
                it.copy(
                    isLoggedIn = false,
                    isLoading = false,
                    deviceIds = emptyList(),
                    devices = emptyList(),
                    error = null
                )
            }
        } else if (uid != currentUid) {
            currentUid = uid
            attachDevicesListener(uid)
        }
    }

    fun init(context: Context) {
        if (inited) return
        inited = true

        viewModelScope.launch(Dispatchers.IO) {
            phoneDbCache = loadPhonesFromAssets(context)

            withContext(Dispatchers.Main) {
                auth.addAuthStateListener(authListener)
                auth.currentUser?.uid?.let { uid ->
                    currentUid = uid
                    attachDevicesListener(uid)
                } ?: run {
                    _state.update { it.copy(isLoggedIn = false, isLoading = false) }
                }
            }
        }
    }

    private fun attachDevicesListener(uid: String) {
        detachDevicesListener()

        _state.update { it.copy(isLoggedIn = true, isLoading = true, error = null) }

        val docRef = db.collection("users")
            .document(uid)
            .collection("my_devices")
            .document("list")

        sub = docRef.addSnapshotListener { snap, e ->
            if (e != null) {
                _state.update { it.copy(isLoading = false, error = e.message) }
                return@addSnapshotListener
            }

            val raw = snap?.get("deviceIds") as? List<*>
            val ids: List<Int> = raw?.mapNotNull { v ->
                when (v) {
                    is Long -> v.toInt()
                    is Int -> v
                    else -> null
                }
            } ?: emptyList()

            val devices = ids.mapNotNull { id ->
                phoneDbCache.firstOrNull { it.id == id }
            }

            _state.update {
                it.copy(
                    isLoggedIn = true,
                    isLoading = false,
                    deviceIds = ids.take(MAX_DEVICES),
                    devices = devices.take(MAX_DEVICES),
                    error = null
                )
            }
        }
    }

    private fun detachDevicesListener() {
        sub?.remove()
        sub = null
    }

    fun searchPhones(query: String): List<PhoneDbItem> {
        val q = query.trim().lowercase(Locale.getDefault())
        if (q.length < 2) return emptyList()

        return phoneDbCache.asSequence()
            .filter { it.name.orEmpty().lowercase(Locale.getDefault()).contains(q) }
            .take(SEARCH_LIMIT)
            .toList()
    }

    fun addDevice(spec: PhoneDbItem) {
        val uid = auth.currentUser?.uid ?: return
        val current = _state.value.deviceIds

        if (current.size >= MAX_DEVICES) return
        if (current.contains(spec.id)) return

        val updated = (current + spec.id).take(MAX_DEVICES)

        viewModelScope.launch(Dispatchers.IO) {
            try {
                db.collection("users")
                    .document(uid)
                    .collection("my_devices")
                    .document("list")
                    .set(mapOf("deviceIds" to updated))
                    .await()
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message) }
            }
        }
    }

    fun removeDevice(id: Int) {
        val uid = auth.currentUser?.uid ?: return
        val current = _state.value.deviceIds
        val updated = current.filterNot { it == id }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                db.collection("users")
                    .document(uid)
                    .collection("my_devices")
                    .document("list")
                    .set(mapOf("deviceIds" to updated))
                    .await()
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message) }
            }
        }
    }

    override fun onCleared() {
        detachDevicesListener()
        auth.removeAuthStateListener(authListener)
        super.onCleared()
    }
}
