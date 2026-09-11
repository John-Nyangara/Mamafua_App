package com.mamafua.app.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Job(
    val id: Int,
    val clientId: Int,
    val workerId: Int? = null,
    val title: String,
    val description: String,
    val location: String,
    val budget: Double,
    val status: String,
    val paymentStatus: String,
    val createdAt: String
)

@Serializable
data class Review(
    val id: Int,
    val jobId: Int,
    val reviewerId: Int,
    val revieweeId: Int,
    val rating: Int,
    val comments: String? = null,
    val createdAt: String
)

@Serializable
data class Dispute(
    val id: Int,
    val jobId: Int,
    val raisedBy: Int,
    val againstUser: Int,
    val reason: String,
    val status: String,
    val createdAt: String
)
