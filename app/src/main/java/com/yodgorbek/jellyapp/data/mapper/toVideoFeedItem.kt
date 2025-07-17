package com.yodgorbek.jellyapp.data.mapper

import com.yodgorbek.jellyapp.data.model.VideoFeedItem
import com.yodgorbek.jellyapp.data.model.YouTubeItem

fun YouTubeItem.toVideoFeedItem(): VideoFeedItem {
    return VideoFeedItem(
        id = id.videoId,
        url = "https://www.youtube.com/watch?v=${id.videoId}",
        thumbnail = snippet.thumbnails.high.url,
        title = snippet.title
    )
}