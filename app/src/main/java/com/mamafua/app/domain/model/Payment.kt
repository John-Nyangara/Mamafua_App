package com.mamafua.app.domain.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class Payment(
    val id: Int,
    @SerialName("job_id") val jobId: Int,
    val amount: Double,
    val currency: String? = null,
    val status: String,
    @SerialName("mpesa_receipt_number") val mpesaReceiptNumber: String? = null,
    @SerialName("result_description") val resultDescription: String? = null,
    @SerialName("paid_at") val paidAt: String? = null,
    @SerialName("created_at") val createdAt: String? = null
)
