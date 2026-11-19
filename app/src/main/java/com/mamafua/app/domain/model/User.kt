package com.mamafua.app.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class User(
    val id: Int,
    val name: String,
    val email: String,
    val phone: String,
    val role: String,
    val profilePictureUrl: String? = null,
    val isVerified: Boolean
)

@Serializable
data class WorkerProfile(
    val userId: Int,
    val bio: String? = null,
    val hourlyRate: Double,
    val serviceRadiusKm: Double,
    val locationLat: Double? = null,
    val locationLng: Double? = null,
    val availabilityStatus: String,
    val averageRating: Double
)
