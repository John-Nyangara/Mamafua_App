package com.mamafua.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.mamafua.app.domain.model.User

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val email: String,
    val phone: String,
    val role: String,
    val profilePictureUrl: String?,
    val isVerified: Boolean
) {
    fun toDomain(): User {
        return User(id, name, email, phone, role, profilePictureUrl, isVerified)
    }
}

fun User.toEntity(): UserEntity {
    return UserEntity(id, name, email, phone, role, profilePictureUrl, isVerified)
}
