package com.mamafua.app.domain.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class User(
    val id: Int,
    val name: String,
    val email: String,
    val phone: String,
    val role: String,
    @SerialName("profile_picture_url") val profilePictureUrl: String? = null,
    @SerialName("is_verified") val isVerified: Boolean = false
)

@Serializable
data class WorkerProfile(
    val id: Int = 0,
    @SerialName("user_id") val userId: Int = 0,
    @SerialName("worker_id") val workerIdFallback: Int? = null,
    val name: String? = "Worker",
    val service: String? = null,
    val location: String? = null,
    val price: Double? = 0.0,
    @SerialName("hourly_rate") val hourlyRate: Double? = 0.0,
    @SerialName("average_rating") val averageRating: Double? = 0.0,
    val bio: String? = null,
    @SerialName("availability_status") val availabilityStatus: String? = null,
    @SerialName("profile_image") val profileImage: String? = null
) {
    val resolvedUserId: Int
        get() = if (userId != 0) userId else (workerIdFallback ?: id)
}
