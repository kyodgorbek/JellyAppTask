package com.yodgorbek.jellyapp.presentation.gallery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yodgorbek.jellyapp.domain.model.Video
import com.yodgorbek.jellyapp.domain.usacase.GetVideosUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class GalleryViewModel(
    private val getVideosUseCase: GetVideosUseCase
) : ViewModel() {
    val videos: StateFlow<List<Video>> = getVideosUseCase()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
}