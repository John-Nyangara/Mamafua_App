package com.mamafua.app.data.remote.dto

import kotlinx.serialization.Serializable
import com.mamafua.app.domain.model.User

@Serializable
data class LoginReq(val email: String, val password: String)

@Serializable
data class RegisterReq(val name: String, val email: String, val phone: String, val password: String, val role: String)

@Serializable
data class AuthRes(val token: String, val user: User)

@Serializable
data class UpdateProfileReq(
    val name: String? = null,
    val bio: String? = null,
    val hourlyRate: Double? = null,
    val serviceRadiusKm: Double? = null
)

@Serializable
data class CreateJobReq(
    val title: String,
    val description: String,
    val location: String,
    val budget: Double
)

@Serializable
data class UpdateJobStatusReq(val status: String)

@Serializable
data class StkPushReq(val jobId: Int, val phone: String)
