@file:OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)

package com.mamafua.app.domain.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

@Serializable
data class RadarOffer(
    @JsonNames("offer_id", "offerId", "id")
    val offerId: Int = 0,
    @JsonNames("job_id", "jobId")
    val jobId: Int = 0,
    @JsonNames("dispatch_type", "dispatchType")
    val dispatchType: String = "auto_broadcast",
    @JsonNames("distance_km", "distanceKm")
    val distanceKm: Double? = null,
    @JsonNames("estimated_payout", "estimatedPayout", "net_payout", "budget")
    val estimatedPayout: Double = 0.0,
    @JsonNames("expires_at", "expiresAt")
    val expiresAt: String? = null,
    @JsonNames("seconds_remaining", "secondsRemaining")
    val secondsRemaining: Int = 0,
    val title: String = "Incoming Cleaning Request",
    val estate: String? = null,
    val location: String? = null,
    @JsonNames("latitude", "lat")
    val latitude: Double? = null,
    @JsonNames("longitude", "lng", "lon")
    val longitude: Double? = null,
    @JsonNames("worker_brings_detergents", "workerBringsDetergents")
    val workerBringsDetergents: Boolean = false,
    @JsonNames("payment_status", "paymentStatus")
    val paymentStatus: String? = null,
    @JsonNames("client_name", "clientName")
    val clientName: String? = null
)

@Serializable
data class WorkerEarningsData(
    @JsonNames("platform_commission_rate", "platformCommissionRate")
    val platformCommissionRate: Double = 0.1,
    val summary: EarningsSummary = EarningsSummary(),
    val payouts: List<PayoutEntry> = emptyList()
)

@Serializable
data class EarningsSummary(
    @JsonNames("total_gross_earnings", "totalGrossEarnings")
    val totalGrossEarnings: Double = 0.0,
    @JsonNames("total_platform_fees", "totalPlatformFees")
    val totalPlatformFees: Double = 0.0,
    @JsonNames("total_net_earnings", "totalNetEarnings")
    val totalNetEarnings: Double = 0.0,
    @JsonNames("paid_out_balance", "paidOutBalance")
    val paidOutBalance: Double = 0.0,
    @JsonNames("pending_payout_balance", "pendingPayoutBalance", "availableBalance")
    val pendingPayoutBalance: Double = 0.0,
    @JsonNames("today_net_earnings", "todayNetEarnings")
    val todayNetEarnings: Double = 0.0,
    @JsonNames("weekly_net_earnings", "weeklyNetEarnings")
    val weeklyNetEarnings: Double = 0.0
)

@Serializable
data class PayoutEntry(
    val id: Int = 0,
    @JsonNames("job_id", "jobId")
    val jobId: Int? = null,
    @JsonNames("job_title", "jobTitle", "title")
    val jobTitle: String? = null,
    @JsonNames("gross_amount", "grossAmount", "amount")
    val grossAmount: Double = 0.0,
    @JsonNames("platform_fee", "platformFee", "commission")
    val platformFee: Double = 0.0,
    @JsonNames("net_amount", "netAmount", "payout_amount", "payoutAmount")
    val netAmount: Double = 0.0,
    val status: String = "pending",
    @JsonNames("mpesa_receipt_number", "mpesaReceiptNumber", "b2c_conversation_id", "receipt")
    val mpesaReceipt: String? = null,
    @JsonNames("created_at", "createdAt", "paid_at", "paidAt")
    val createdAt: String? = null
)

@Serializable
data class AvailabilitySlot(
    val id: Int = 0,
    @JsonNames("day_of_week", "dayOfWeek")
    val dayOfWeek: String,
    @JsonNames("start_time", "startTime")
    val startTime: String,
    @JsonNames("end_time", "endTime")
    val endTime: String,
    @JsonNames("is_available", "isAvailable")
    val isAvailable: Boolean = true
)

@Serializable
data class WorkerReviewsSummary(
    @JsonNames("average_rating", "averageRating")
    val averageRating: Double = 0.0,
    @JsonNames("total_reviews", "totalReviews", "count")
    val totalReviews: Int = 0,
    val reviews: List<Review> = emptyList()
)
