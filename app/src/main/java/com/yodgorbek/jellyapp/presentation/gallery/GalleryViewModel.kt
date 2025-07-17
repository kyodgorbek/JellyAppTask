package com.yodgorbek.jellyapp.presentation.gallery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yodgorbek.jellyapp.domain.model.Video
import com.yodgorbek.jellyapp.domain.usacase.GetLocalVideosUseCase
import com.yodgorbek.jellyapp.domain.usacase.GetVideosUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class GalleryViewModel(
    private val getVideosUseCase: GetVideosUseCase,
    private val getLocalVideosUseCase: GetLocalVideosUseCase
) : ViewModel() {

    private val _videos: StateFlow<List<Video>> = combine(
        getVideosUseCase(),
        getLocalVideosUseCase()
    ) { remoteVideos, localVideos ->
        (localVideos + remoteVideos).distinctBy { it.url } // Remove duplicates
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val videos: StateFlow<List<Video>> = _videos
}
