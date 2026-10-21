package com.mamafua.app.domain.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class Job(
    val id: Int,
    @SerialName("client_id") val clientId: Int = 0,
    @SerialName("worker_id") val workerId: Int? = null,
    val title: String,
    val description: String,
    val location: String,
    val budget: Double,
    val status: JobStatus,
    @SerialName("payment_status") val paymentStatus: String? = null,
    @SerialName("created_at") val createdAt: String? = null
)
