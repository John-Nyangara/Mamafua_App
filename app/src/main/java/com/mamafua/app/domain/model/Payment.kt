@file:OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)

package com.mamafua.app.domain.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

@Serializable
data class Payment(
    val id: Int = 0,
    @JsonNames("job_id", "jobId")
    val jobId: Int = 0,
    val amount: Double = 0.0,
    val currency: String? = "KES",
    val status: String = "pending",
    @JsonNames("payment_type", "paymentType")
    val paymentType: String? = null,
    @JsonNames("checkout_request_id", "checkoutRequestId")
    val checkoutRequestId: String? = null,
    @JsonNames("mpesa_receipt_number", "mpesaReceiptNumber")
    val mpesaReceiptNumber: String? = null,
    @JsonNames("result_description", "resultDescription")
    val resultDescription: String? = null,
    @JsonNames("paid_at", "paidAt")
    val paidAt: String? = null,
    @JsonNames("created_at", "createdAt")
    val createdAt: String? = null
)
