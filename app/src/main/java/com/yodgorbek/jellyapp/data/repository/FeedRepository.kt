package com.yodgorbek.jellyapp.data.repository

import com.yodgorbek.jellyapp.data.mapper.toVideoFeedItem
import com.yodgorbek.jellyapp.data.model.VideoFeedItem
import com.yodgorbek.jellyapp.data.remote.FeedApi

interface FeedRepository {
    suspend fun getVideoFeed(): Result<List<VideoFeedItem>>
}

class FeedRepositoryImpl(
    private val api: FeedApi,
    private val apiKey: String // <- Inject this
) : FeedRepository {
    override suspend fun getVideoFeed(): Result<List<VideoFeedItem>> {
        return try {
            val response = api.getYouTubeVideos(
                channelId = "UC_x5XG1OV2P6uZZ5FSM9Ttw",
                apiKey = "AIzaSyAoiVygOytDpJWL0s0rvSd5B4n7DGSGIhE" // <- Use the injected key
            )
            val videos = response.items.map { it.toVideoFeedItem() }
            Result.success(videos)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}