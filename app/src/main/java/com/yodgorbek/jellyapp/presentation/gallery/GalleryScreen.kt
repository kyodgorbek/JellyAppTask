package com.yodgorbek.jellyapp.presentation.gallery

import android.graphics.Bitmap
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.model.LazyHeaders
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.signature.ObjectKey
import com.yodgorbek.jellyapp.util.SUPABASE_ANON_KEY
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.androidx.compose.koinViewModel
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@Composable
fun GalleryScreen(
    navController: NavController,
    viewModel: GalleryViewModel = koinViewModel()
) {
    val videos by viewModel.videos.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        coroutineScope.launch {
            viewModel.loadVideos()
        }
    }

    if (videos.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "Loading videos...")
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .padding(8.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(4.dp)
        ) {
            items(videos) { video ->
                val headers = HashMap<String, String>()
                headers.put(HttpHeaders.Authorization, "Bearer $SUPABASE_ANON_KEY")
                headers.put("apikey", SUPABASE_ANON_KEY)

                val videoUrl = video.url.trim().replace("\\s".toRegex(), "")
                VideoThumbnail(videoUrl, headers, modifier = Modifier) {
                    //navController.navigate("player/$videoUrl")
                    val encodedUrl = URLEncoder.encode(videoUrl, StandardCharsets.UTF_8.toString())
                    navController.navigate("player/$encodedUrl")

                }
            }
        }
    }
}

@Composable
fun VideoThumbnail(
    videoUrl: String,
    headers: Map<String, String>,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    val context = LocalContext.current

    val glideUrl = remember(videoUrl, headers) {
        getGlideUrlWithHeaders(videoUrl, headers)
    }

    Log.i("__TAG_VIDEO_URL", videoUrl)
    val cacheKey = remember(headers) {
        headers.entries.joinToString(separator = ";") { "${it.key}:${it.value}" }
    }

    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(glideUrl, cacheKey) {
        withContext(Dispatchers.IO) {
            try {
                val frameTimeMicros = 1_000_000L // 1 second
                val loadedBitmap = Glide.with(context)
                    .asBitmap()
                    .load(glideUrl)
                    .apply(
                        RequestOptions()
                            .frame(frameTimeMicros)
                            .diskCacheStrategy(DiskCacheStrategy.DATA)
                    )
                    .signature(ObjectKey(cacheKey))
                    .submit()
                    .get()
                bitmap = loadedBitmap
                errorMessage = null
            } catch (e: Exception) {
                Log.e("VideoThumbnail", "Failed to load thumbnail", e)
                errorMessage = e.localizedMessage ?: "Unknown error"
            }
        }
    }

    Box(
        modifier = modifier
            .clickable(onClick = onClick)
            .fillMaxWidth()
            .height(200.dp)
    ) {
        when {
            bitmap != null -> {
                Image(
                    bitmap = bitmap!!.asImageBitmap(),
                    contentDescription = "Video thumbnail",
                    modifier = Modifier.fillMaxSize()
                )
            }

            errorMessage != null -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Log.i("__TAG", errorMessage.toString())
                    Text(text = errorMessage.toString())
                }
            }

            else -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "Loading thumbnail...")
                }
            }
        }
    }
}

fun getGlideUrlWithHeaders(url: String, headers: Map<String, String>): GlideUrl {
    val lazyHeadersBuilder = LazyHeaders.Builder()
    headers.forEach { (key, value) ->
        lazyHeadersBuilder.addHeader(key, value)
    }
    return GlideUrl(url, lazyHeadersBuilder.build())
}

