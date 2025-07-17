package com.yodgorbek.jellyapp.domain.usacase

import com.yodgorbek.jellyapp.data.model.VideoFeedItem
import com.yodgorbek.jellyapp.data.repository.FeedRepository



class FetchFeedUseCase(
    private val repository: FeedRepository
) {
    suspend operator fun invoke(): Result<List<VideoFeedItem>> {
        return repository.getVideoFeed()
    }
}
