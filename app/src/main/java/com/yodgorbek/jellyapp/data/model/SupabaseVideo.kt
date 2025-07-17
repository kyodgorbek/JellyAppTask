package com.yodgorbek.jellyapp.data.model


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.datetime.Instant

@Serializable
data class SupabaseVideo(
    val url: String,
    @SerialName("created_at") val createdAt: Instant? = null
)