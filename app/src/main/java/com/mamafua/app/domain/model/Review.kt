package com.mamafua.app.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Review(
    val id: Int,
    @SerialName("job_id") val jobId: Int,
    @SerialName("reviewer_id") val reviewerId: Int,
    @SerialName("reviewee_id") val revieweeId: Int,
    val rating: Int,
    val comment: String? = null,
    @SerialName("created_at") val createdAt: String
)
