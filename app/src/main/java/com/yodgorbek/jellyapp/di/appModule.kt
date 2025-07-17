package com.yodgorbek.jellyapp.di


import com.google.gson.GsonBuilder
import com.yodgorbek.jellyapp.BuildConfig
import com.yodgorbek.jellyapp.data.remote.FeedApi
import com.yodgorbek.jellyapp.data.repository.CameraRepository
import com.yodgorbek.jellyapp.data.repository.CameraRepositoryImpl
import com.yodgorbek.jellyapp.data.repository.FeedRepository
import com.yodgorbek.jellyapp.data.repository.FeedRepositoryImpl
import com.yodgorbek.jellyapp.data.repository.GalleryRepository
import com.yodgorbek.jellyapp.data.repository.GalleryRepositoryImpl
import com.yodgorbek.jellyapp.domain.usacase.FetchFeedUseCase
import com.yodgorbek.jellyapp.domain.usacase.GetVideosUseCase
import com.yodgorbek.jellyapp.domain.usacase.RecordVideoUseCase
import com.yodgorbek.jellyapp.presentation.camera.CameraViewModel
import com.yodgorbek.jellyapp.presentation.feed.FeedViewModel
import com.yodgorbek.jellyapp.presentation.gallery.GalleryViewModel
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

val appModule = module {

    // Logging interceptor
    single {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()
    }

    // Retrofit + Gson (lenient mode to avoid JSON parsing crash)
    single {
        val gson = GsonBuilder()
            .setLenient()
            .create()

        Retrofit.Builder()
            .baseUrl("https://www.googleapis.com/youtube/v3/") // make sure this is a valid JSON API base URL
            .client(get())
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(FeedApi::class.java)
    }
    single {
        HttpClient(OkHttp) {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                })
            }
        }
    }

    // Feed
    single<FeedRepository> { FeedRepositoryImpl(get(), BuildConfig.YOUTUBE_API_KEY) }
    single { FetchFeedUseCase(get()) }
    viewModel { FeedViewModel(get()) }

    // Camera
    single<CameraRepository> { CameraRepositoryImpl(get()) }
    single { RecordVideoUseCase(get()) }
    viewModel { CameraViewModel(get(), get()) }

    // Gallery
    single<GalleryRepository> { GalleryRepositoryImpl(get()) }
    single { GetVideosUseCase(get()) }
    viewModel { GalleryViewModel(get()) }
}
