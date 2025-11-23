package com.sbro.gameslibrary.util

import android.content.Context
import android.util.Log
import com.sbro.gameslibrary.components.Game
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.util.Locale

object JsonParser {
    private const val TAG = "CsvParser"

    suspend fun parseFromAssetsJson(
        context: Context,
        assetFileName: String = "games.json"
    ): Result<List<Game>> = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.assets.open(assetFileName)
            val jsonText = inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }

            val jsonArray = JSONArray(jsonText)
            val games = mutableListOf<Game>()

            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)

                val id = obj.optString("id", i.toString())
                val name = obj.optString("name", "")
                if (name.isBlank()) continue

                val year = obj.optString("year", "")
                val genre = obj.optString("genre", "")
                val ratingIgn = obj.optString("rating_ign", "")
                val photoUrl = obj.optString("photo_url", "")

                val description: String = run {
                    val descObj = obj.optJSONObject("description")
                    if (descObj != null) {
                        val localeLang = Locale.getDefault().language.lowercase(Locale.ROOT)

                        val uk = descObj.optString("uk", "")
                        val en = descObj.optString("en", "")
                        val ru = descObj.optString("ru", "")

                        when (localeLang) {
                            "uk" -> uk.ifBlank { en.ifBlank { ru } }
                            "ru" -> ru.ifBlank { en.ifBlank { uk } }
                            else -> en.ifBlank { uk.ifBlank { ru } }
                        }
                    } else {
                        obj.optString("description", "")
                    }
                }

                val platform = obj.optString("platform", "")

                val game = Game(
                    id = id,
                    title = name,
                    year = year,
                    genre = genre,
                    rating = ratingIgn,
                    imageUrl = photoUrl,
                    telegramLink = "",
                    description = description,
                    platform = platform,
                    testResults = emptyList(),
                    isFavorite = false
                )
                games.add(game)
            }

            if (games.isEmpty()) {
                Result.failure(Exception("JSON file parsed but contains 0 games"))
            } else {
                Result.success(games)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing JSON from assets", e)
            Result.failure(e)
        }
    }
}
