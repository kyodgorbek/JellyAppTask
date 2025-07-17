package com.yodgorbek.jellyapp.domain.usacase

import android.content.Context
import com.yodgorbek.jellyapp.domain.model.Video
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.File

class GetLocalVideosUseCase(
    private val context: Context
) {
    operator fun invoke(): Flow<List<Video>> = flow {
        val videoList = mutableListOf<Video>()
        val videosDir = File(context.getExternalFilesDir(null), "videos")
        if (videosDir.exists()) {
            videosDir.listFiles()?.forEach { file ->
                if (file.extension == "mp4") {
                    videoList.add(
                        Video(
                            url = file.absolutePath, // local file path
                            createdAt = file.lastModified()
                        )
                    )
                }
            }
        }
        emit(videoList.sortedByDescending { it.createdAt })
    }
}
