package com.sbro.gameslibrary.util



import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStreamReader

data class PhoneDbItem(
    val id: Int = 0,
    val name: String? = null,
    val cpu: String? = null,
    val ram: String? = null
)

suspend fun loadPhonesFromAssets(context: Context): List<PhoneDbItem> =
    withContext(Dispatchers.IO) {
        runCatching {
            context.assets.open("phone.json").use { input ->
                InputStreamReader(input).use { reader ->
                    val type = object : TypeToken<List<PhoneDbItem>>() {}.type
                    Gson().fromJson<List<PhoneDbItem>>(reader, type) ?: emptyList()
                }
            }
        }.getOrElse { emptyList() }
    }

