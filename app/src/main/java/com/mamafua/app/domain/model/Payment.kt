package com.mamafua.app.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Payment(
    val id: Int,
    val jobId: Int,
    val clientId: Int,
    val workerId: Int,
    val amount: Double,
    val status: String,
    val checkoutRequestId: String? = null,
    val mpesaReceiptNumber: String? = null
)
