package com.yodgorbek.jellyapp.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.google.android.exoplayer2.ExoPlayer

@Composable
fun rememberExoPlayer(url: String, init: (ExoPlayer) -> Unit): ExoPlayer {
    val context = LocalContext.current
    val exoPlayer = remember(url) {
        ExoPlayer.Builder(context).build().apply { init(this) }
    }
    DisposableEffect(url) {
        onDispose { exoPlayer.release() }
    }
    return exoPlayer
}