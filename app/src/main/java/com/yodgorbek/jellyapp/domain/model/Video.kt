package com.yodgorbek.jellyapp.domain.model


data class Video(
    val url: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
