package com.yodgorbek.jellyapp.presentation.gallery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yodgorbek.jellyapp.data.repository.GalleryRepository
import com.yodgorbek.jellyapp.data.repository.GalleryRepositoryImpl
import com.yodgorbek.jellyapp.domain.model.Video
import io.ktor.client.HttpClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent

class GalleryViewModel(
    private val repository: GalleryRepository = GalleryRepositoryImpl(HttpClient())
) : ViewModel(), KoinComponent {

    private val _videos = MutableStateFlow<List<Video>>(emptyList())
    val videos: StateFlow<List<Video>> = _videos

    fun loadVideos() {
        viewModelScope.launch {
            repository.getVideos().collect { videoList ->
                _videos.value = videoList
            }
        }
    }
}
