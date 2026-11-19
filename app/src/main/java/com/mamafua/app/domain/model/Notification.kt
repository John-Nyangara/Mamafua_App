package com.mamafua.app.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Notification(
    val id: Int,
    val userId: Int,
    val type: String,
    val title: String,
    val body: String,
    val data: String? = null,
    val isRead: Boolean,
    val createdAt: String
)
