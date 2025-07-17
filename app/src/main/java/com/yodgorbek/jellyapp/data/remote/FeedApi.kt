package com.yodgorbek.jellyapp.data.remote

import com.yodgorbek.jellyapp.data.model.YouTubeResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface FeedApi {
    @GET("search")
    suspend fun getYouTubeVideos(
        @Query("part") part: String = "snippet",
        @Query("channelId") channelId: String,
        @Query("order") order: String = "date",
        @Query("maxResults") maxResults: Int = 10,
        @Query("key") apiKey: String
    ): YouTubeResponse
}