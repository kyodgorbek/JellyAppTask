package com.yodgorbek.jellyapp.presentation.gallery

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.ui.PlayerView
import com.yodgorbek.jellyapp.domain.model.Video
import com.yodgorbek.jellyapp.util.rememberExoPlayer
import org.koin.androidx.compose.koinViewModel
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@Composable
fun GalleryScreen(
    navController: NavController,
    viewModel: GalleryViewModel = koinViewModel()
) {
    val videos by viewModel.videos.collectAsState()

    LazyVerticalGrid(columns = GridCells.Fixed(2)) {
        items(videos) { video ->
            VideoThumbnail(
                video = video,
                onClick = {
                    val encodedUrl = URLEncoder.encode(
                        video.url,
                        StandardCharsets.UTF_8.toString()
                    )
                    navController.navigate("player?videoUrl=$encodedUrl")
                }
            )
        }
    }
}


@Composable
fun VideoThumbnail(video: Video, onClick: () -> Unit) {
    val context = LocalContext.current
    val exoPlayer = rememberExoPlayer(video.url) { player ->
        player.setMediaItem(MediaItem.fromUri(video.url))
        player.prepare()
        player.playWhenReady = false // Preview only
    }

    AndroidView(
        factory = {
            PlayerView(context).apply {
                player = exoPlayer
                useController = false // No UI controls for thumbnail
            }
        },
        modifier = Modifier
            .padding(4.dp)
            .clickable(onClick = onClick)
    )
}
