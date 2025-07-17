package com.yodgorbek.jellyapp.domain.model

import kotlinx.serialization.Serializable


@Serializable
data class Video(
    val url: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
