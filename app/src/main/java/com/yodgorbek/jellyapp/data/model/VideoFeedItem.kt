package com.yodgorbek.jellyapp.data.model

data class VideoFeedItem(
    val id: String,
    val url: String,
    val thumbnail: String,
    val title: String
)

// YouTube API response model

data class YouTubeResponse(
    val items: List<YouTubeItem>
)

data class YouTubeItem(
    val id: VideoId,
    val snippet: Snippet
)

data class VideoId(
    val videoId: String
)

data class Snippet(
    val title: String,
    val thumbnails: Thumbnails
)

data class Thumbnails(
    val high: Thumbnail
)

data class Thumbnail(
    val url: String
)