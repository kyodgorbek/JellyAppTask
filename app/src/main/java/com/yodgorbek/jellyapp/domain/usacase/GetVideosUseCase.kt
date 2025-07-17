package com.yodgorbek.jellyapp.domain.usacase


import com.yodgorbek.jellyapp.data.repository.GalleryRepository
import com.yodgorbek.jellyapp.domain.model.Video
import kotlinx.coroutines.flow.Flow


class GetVideosUseCase(
    private val repository: GalleryRepository
) {
    operator fun invoke(): Flow<List<Video>> {
        return repository.getVideos()
    }
}
