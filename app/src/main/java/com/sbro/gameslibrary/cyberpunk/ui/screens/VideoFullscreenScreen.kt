package com.sbro.gameslibrary.cyberpunk.ui.screens

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.SystemClock
import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.options.IFramePlayerOptions
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView

private val CyberYellow = Color(0xFFFCEE0A)
private val CyberRed = Color(0xFFFF003C)
private val CyberBlack = Color(0xFF050505)

private fun Context.findActivity(): Activity? {
    var ctx = this
    while (ctx is android.content.ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

@Composable
fun VideoFullscreenScreen(
    type: String,
    data: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val lifecycleOwner = LocalLifecycleOwner.current

    val lastBackClickAt = remember { mutableLongStateOf(0L) }
    fun safeBack() {
        val now = SystemClock.elapsedRealtime()
        if (now - lastBackClickAt.longValue < 600L) return
        lastBackClickAt.longValue = now
        onBack()
    }

    DisposableEffect(Unit) {
        val a = activity
        val prev = a?.requestedOrientation
        if (a != null) a.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        onDispose {
            if (a != null && prev != null) a.requestedOrientation = prev
        }
    }

    DisposableEffect(Unit) {
        val a = activity
        if (a != null) {
            val window = a.window
            WindowCompat.setDecorFitsSystemWindows(window, false)
            val controller = WindowInsetsControllerCompat(window, window.decorView)
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            onDispose {
                controller.show(WindowInsetsCompat.Type.systemBars())
                WindowCompat.setDecorFitsSystemWindows(window, true)
            }
        } else {
            onDispose { }
        }
    }

    BackHandler { safeBack() }

    val payload = remember(type, data) {
        if (type == "direct") Uri.decode(data) else data
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberBlack)
            .windowInsetsPadding(WindowInsets.displayCutout),
        contentAlignment = Alignment.Center
    ) {
        Fit16x9(modifier = Modifier.fillMaxSize()) { wDp, hDp ->
            if (type == "yt") {
                YouTubePlayerBlock(
                    youtubeId = payload,
                    lifecycleOwner = lifecycleOwner,
                    modifier = Modifier.size(wDp, hDp)
                )
            } else {
                DirectVideoPlayerBlock(
                    url = payload,
                    modifier = Modifier.size(wDp, hDp)
                )
            }
        }

        IconButton(
            onClick = { safeBack() },
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp),
            colors = IconButtonDefaults.iconButtonColors(
                containerColor = CyberRed.copy(alpha = 0.35f),
                contentColor = CyberYellow
            )
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = CyberYellow
            )
        }
    }
}

@Composable
private fun Fit16x9(
    modifier: Modifier = Modifier,
    content: @Composable (widthDp: Dp, heightDp: Dp) -> Unit
) {
    BoxWithConstraints(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        val density = LocalDensity.current
        val maxW = maxWidth
        val maxH = maxHeight

        val maxWPx = with(density) { maxW.toPx() }
        val maxHPx = with(density) { maxH.toPx() }
        val videoRatio = 16f / 9f

        val w1 = maxWPx
        val h1 = w1 / videoRatio
        val h2 = maxHPx
        val w2 = h2 * videoRatio

        val (finalW, finalH) = if (h1 <= maxHPx) w1 to h1 else w2 to h2

        val finalWDp = with(density) { finalW.toDp() }
        val finalHDp = with(density) { finalH.toDp() }

        content(finalWDp, finalHDp)
    }
}

@Composable
private fun YouTubePlayerBlock(
    youtubeId: String,
    lifecycleOwner: LifecycleOwner,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val options = remember {
        IFramePlayerOptions.Builder(context)
            .controls(1)
            .build()
    }

    val view = remember {
        YouTubePlayerView(context).apply {
            enableAutomaticInitialization = false
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            clipChildren = false
            clipToPadding = false
        }
    }

    DisposableEffect(lifecycleOwner, view) {
        lifecycleOwner.lifecycle.addObserver(view)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(view)
            view.release()
        }
    }

    DisposableEffect(view, youtubeId) {
        val listener = object : AbstractYouTubePlayerListener() {
            override fun onReady(youTubePlayer: YouTubePlayer) {
                youTubePlayer.loadVideo(youtubeId, 0f)
            }
        }
        view.initialize(listener, options)
        onDispose { }
    }

    AndroidView(
        modifier = modifier,
        factory = { view }
    )
}

@OptIn(UnstableApi::class)
@Composable
private fun DirectVideoPlayerBlock(
    url: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val player = remember(url) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(url))
            prepare()
            playWhenReady = true
        }
    }

    DisposableEffect(player) {
        onDispose { player.release() }
    }

    AndroidView(
        modifier = modifier,
        factory = {
            PlayerView(it).apply {
                this.player = player
                useController = true
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
            }
        }
    )
}
