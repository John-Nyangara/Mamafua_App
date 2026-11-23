@file:OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)

package com.mamafua.app.domain.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

@Serializable
data class Review(
    val id: Int = 0,
    @JsonNames("job_id", "jobId")
    val jobId: Int = 0,
    @JsonNames("reviewer_id", "reviewerId")
    val reviewerId: Int = 0,
    @JsonNames("reviewee_id", "revieweeId")
    val revieweeId: Int = 0,
    @JsonNames("reviewer_name", "reviewerName")
    val reviewerName: String? = null,
    val rating: Int = 5,
    @JsonNames("comment", "comments")
    val comment: String? = null,
    @JsonNames("created_at", "createdAt")
    val createdAt: String = ""
)
