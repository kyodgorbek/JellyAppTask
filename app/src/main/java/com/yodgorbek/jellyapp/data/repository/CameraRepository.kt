package com.yodgorbek.jellyapp.data.repository

import android.content.Context
import android.net.Uri
import com.yodgorbek.jellyapp.util.BUCKET_NAME
import com.yodgorbek.jellyapp.util.SUPABASE_ANON_KEY
import com.yodgorbek.jellyapp.util.SUPABASE_URL
import com.yodgorbek.jellyapp.util.VideoMerger
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

interface CameraRepository {
    suspend fun recordDualCameraVideo(videoFile: File): Result<String>
    suspend fun mergeDualCameraVideos(
        frontUri: Uri,
        backUri: Uri,
        context: Context
    ): Result<String>
}

class CameraRepositoryImpl(
    private val client: HttpClient // Inject from Koin or provide in ViewModel
) : CameraRepository {

    override suspend fun recordDualCameraVideo(videoFile: File): Result<String> {
        return try {
            val publicUrl = uploadToSupabase(videoFile)
            saveVideoMetadata(publicUrl)
            Result.success(publicUrl)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun mergeDualCameraVideos(
        frontUri: Uri,
        backUri: Uri,
        context: Context
    ): Result<String> {
        return try {
            val frontFile = getFileFromUri(context, frontUri)
            val backFile = getFileFromUri(context, backUri)

            val mergedFile = VideoMerger.merge(
                context,
                frontFile.absolutePath,
                backFile.absolutePath
            )

            val publicUrl = uploadToSupabase(mergedFile)
            saveVideoMetadata(publicUrl)
            Result.success(publicUrl)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun uploadToSupabase(file: File): String {
        val filename = "video_${System.currentTimeMillis()}.mp4"

        val response: HttpResponse = client.submitFormWithBinaryData(
            url = "$SUPABASE_URL/storage/v1/object/$BUCKET_NAME/$filename",
            formData = formData {
                append("file", file.readBytes(), Headers.build {
                    append(HttpHeaders.ContentType, "video/mp4")
                    append(HttpHeaders.ContentDisposition, "filename=$filename")
                })
            }
        ) {
            headers {
                append("Authorization", "Bearer $SUPABASE_ANON_KEY")
                append("apikey", SUPABASE_ANON_KEY)
            }
        }

        if (response.status.isSuccess()) {
            return "$SUPABASE_URL/storage/v1/object/public/$BUCKET_NAME/$filename"
        } else {
            throw Exception("Supabase upload failed: ${response.status}")
        }
    }

    private suspend fun saveVideoMetadata(url: String) {
        val response = client.post("$SUPABASE_URL/rest/v1/videos") {
            contentType(ContentType.Application.Json)
            headers {
                append("Authorization", "Bearer $SUPABASE_ANON_KEY")
                append("apikey", SUPABASE_ANON_KEY)
                append("Prefer", "return=minimal")
            }
            setBody(Json.encodeToString(mapOf("url" to url)))
        }

        if (!response.status.isSuccess()) {
            throw Exception("Failed to save metadata: ${response.status}")
        }
    }

    private fun getFileFromUri(context: Context, uri: Uri): File {
        return if (uri.scheme == "file") {
            File(uri.path ?: throw IllegalArgumentException("Invalid file URI"))
        } else {
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: throw IllegalArgumentException("Cannot open URI: $uri")
            val tempFile = File.createTempFile("video", ".mp4", context.cacheDir)
            inputStream.use { input ->
                tempFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            tempFile
        }
    }
}
