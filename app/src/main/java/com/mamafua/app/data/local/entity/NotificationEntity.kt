package com.mamafua.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.mamafua.app.domain.model.Notification

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey val id: Int,
    val userId: Int,
    val type: String,
    val title: String,
    val body: String,
    val data: String?,
    val isRead: Boolean,
    val createdAt: String
) {
    fun toDomain(): Notification {
        return Notification(id, userId, type, title, body, data, isRead, createdAt)
    }
}

fun Notification.toEntity(): NotificationEntity {
    return NotificationEntity(id, userId, type, title, body, data, isRead, createdAt)
}
