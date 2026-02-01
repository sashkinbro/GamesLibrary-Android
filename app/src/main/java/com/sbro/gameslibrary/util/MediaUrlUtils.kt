package com.sbro.gameslibrary.util

import androidx.core.net.toUri

fun isValidHttpUrl(url: String): Boolean {
    val u = url.trim()
    if (u.isBlank()) return false
    return runCatching {
        val parsed = u.toUri()
        val scheme = parsed.scheme?.lowercase()
        (scheme == "http" || scheme == "https") && !parsed.host.isNullOrBlank()
    }.getOrDefault(false)
}

fun extractYouTubeId(url: String): String? {
    val u = url.trim()
    return when {
        u.contains("youtu.be/") ->
            u.substringAfter("youtu.be/").substringBefore("?").substringBefore("&")
        u.contains("youtube.com/watch") && u.contains("v=") ->
            u.substringAfter("v=").substringBefore("&").substringBefore("?")
        u.contains("youtube.com/shorts/") ->
            u.substringAfter("shorts/").substringBefore("?").substringBefore("&")
        else -> null
    }?.takeIf { it.isNotBlank() }
}

fun isImageUrl(url: String): Boolean {
    val u = url.lowercase()
    return u.endsWith(".jpg") || u.endsWith(".jpeg") ||
            u.endsWith(".png") || u.endsWith(".webp") ||
            u.endsWith(".gif")
}

fun isDirectVideoUrl(url: String): Boolean {
    val u = url.lowercase()
    return u.endsWith(".mp4") || u.endsWith(".webm") ||
            u.endsWith(".m3u8") || u.endsWith(".mov")
}

