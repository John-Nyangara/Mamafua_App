package com.mamafua.app.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Dispute(
    val id: Int,
    @SerialName("job_id") val jobId: Int,
    @SerialName("raised_by") val raisedBy: Int,
    val reason: String,
    val description: String,
    val status: String,
    @SerialName("resolution_notes") val resolutionNotes: String? = null,
    @SerialName("resolved_at") val resolvedAt: String? = null,
    @SerialName("created_at") val createdAt: String
)
