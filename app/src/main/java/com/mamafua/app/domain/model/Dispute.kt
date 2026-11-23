@file:OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)

package com.mamafua.app.domain.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

@Serializable
data class Dispute(
    val id: Int = 0,
    @JsonNames("job_id", "jobId")
    val jobId: Int = 0,
    @JsonNames("raised_by", "raisedBy")
    val raisedBy: Int = 0,
    val reason: String = "",
    val description: String = "",
    val status: String = "open",
    @JsonNames("resolution_notes", "resolutionNotes")
    val resolutionNotes: String? = null,
    @JsonNames("resolved_at", "resolvedAt")
    val resolvedAt: String? = null,
    @JsonNames("created_at", "createdAt")
    val createdAt: String = ""
)
