package com.yodgorbek.jellyapp.presentation.feed

import org.koin.androidx.compose.koinViewModel

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.ui.PlayerView
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView
import com.yodgorbek.jellyapp.data.model.VideoFeedItem
import com.yodgorbek.jellyapp.util.rememberExoPlayer
import org.koin.androidx.compose.koinViewModel

@Composable
fun FeedScreen(
    navController: NavController,
    viewModel: FeedViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val lazyListState = rememberLazyListState()

    LazyColumn(state = lazyListState) {
        when (uiState) {
            is FeedUiState.Loading -> item { Text("Loading...") }
            is FeedUiState.Success -> items((uiState as FeedUiState.Success).videos) { video ->
                VideoPlayerItem(video = video) {
                    navController.navigate("detail/${video.id}")
                }
            }
            is FeedUiState.Error -> item { Text((uiState as FeedUiState.Error).message) }
        }
    }
}

@Composable
fun VideoPlayerItem(video: VideoFeedItem, onClick: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable { onClick() }
    ) {
        AndroidView(
            factory = { ctx ->
                val youTubePlayerView = YouTubePlayerView(ctx)
                lifecycleOwner.lifecycle.addObserver(youTubePlayerView)

                youTubePlayerView.addYouTubePlayerListener(object : AbstractYouTubePlayerListener() {
                    override fun onReady(youTubePlayer: YouTubePlayer) {
                        youTubePlayer.cueVideo(video.id, 0f)
                    }
                })

                youTubePlayerView
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
        )

        Text(text = video.title, modifier = Modifier.padding(top = 4.dp))
    }
}