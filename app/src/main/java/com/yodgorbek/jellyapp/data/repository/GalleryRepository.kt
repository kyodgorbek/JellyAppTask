package com.yodgorbek.jellyapp.data.repository

import com.yodgorbek.jellyapp.data.model.SupabaseVideo
import com.yodgorbek.jellyapp.domain.model.Video
import com.yodgorbek.jellyapp.util.SUPABASE_ANON_KEY
import com.yodgorbek.jellyapp.util.SUPABASE_URL
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.parameter
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

interface GalleryRepository {
    fun getVideos(): Flow<List<Video>>
}

class GalleryRepositoryImpl(
    private val client: HttpClient
) : GalleryRepository {

    override fun getVideos(): Flow<List<Video>> = flow {
        try {
            val response: List<SupabaseVideo> = client.get("$SUPABASE_URL/rest/v1/videos") {
                headers {
                    append(HttpHeaders.Authorization, "Bearer $SUPABASE_ANON_KEY")
                    append("apikey", SUPABASE_ANON_KEY)
                }
                parameter("select", "*")
                parameter("order", "created_at.desc")
            }.body()

            val mapped = response.map {
                Video(
                    url = it.url,
                    createdAt = it.createdAt?.toEpochMilliseconds() ?: System.currentTimeMillis()
                )
            }

            emit(mapped)
        } catch (e: Exception) {
            e.printStackTrace()
            emit(emptyList())
        }
    }
}
