@file:OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)

package com.mamafua.app.domain.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

@Serializable
data class Job(
    val id: Int,
    @JsonNames("client_id", "clientId")
    val clientId: Int = 0,
    @JsonNames("worker_id", "workerId", "assigned_worker_id")
    val workerId: Int? = null,
    val title: String = "Cleaning Service",
    val description: String = "",
    val location: String = "",
    val estate: String? = null,
    @JsonNames("latitude", "lat")
    val latitude: Double? = null,
    @JsonNames("longitude", "lng", "lon")
    val longitude: Double? = null,
    val category: String? = null,
    val budget: Double = 0.0,
    val status: JobStatus = JobStatus.OPEN,
    @JsonNames("payment_status", "paymentStatus")
    val paymentStatus: String? = null,
    @JsonNames("matching_mode", "matchingMode")
    val matchingMode: String? = null,
    @JsonNames("is_instant", "isInstant")
    val isInstant: Boolean = true,
    @JsonNames("scheduled_at", "scheduledAt")
    val scheduledAt: String? = null,
    @JsonNames("worker_brings_detergents", "workerBringsDetergents")
    val workerBringsDetergents: Boolean = false,
    @JsonNames("start_pin", "startPin")
    val startPin: String? = null,
    @JsonNames("started_at", "startedAt")
    val startedAt: String? = null,
    @JsonNames("created_at", "createdAt")
    val createdAt: String? = null
)

@Serializable
data class JobProgress(
    @JsonNames("job_id", "jobId", "id")
    val jobId: Int = 0,
    val title: String? = null,
    val location: String? = null,
    val estate: String? = null,
    @JsonNames("latitude", "lat")
    val latitude: Double? = null,
    @JsonNames("longitude", "lng", "lon")
    val longitude: Double? = null,
    val status: JobStatus = JobStatus.OPEN,
    @JsonNames("payment_status", "paymentStatus")
    val paymentStatus: String? = null,
    val budget: Double? = null,
    @JsonNames("start_pin", "startPin")
    val startPin: String? = null,
    @JsonNames("started_at", "startedAt")
    val startedAt: String? = null,
    @JsonNames("completed_at", "completedAt")
    val completedAt: String? = null,
    val worker: ProgressWorkerInfo? = null,
    val client: ProgressClientInfo? = null,
    @JsonNames("checklist_steps", "checklistSteps")
    val checklistSteps: ChecklistSteps = ChecklistSteps()
)

@Serializable
data class ChecklistSteps(
    val assigned: Boolean = false,
    val arrived: Boolean = false,
    @JsonNames("started_cleaning", "startedCleaning", "in_progress")
    val startedCleaning: Boolean = false,
    @JsonNames("completion_requested", "completionRequested")
    val completionRequested: Boolean = false,
    val completed: Boolean = false
)

@Serializable
data class ProgressWorkerInfo(
    val id: Int = 0,
    @JsonNames("user_id", "userId")
    val userId: Int? = null,
    val name: String = "Assigned Mama Fua",
    val phone: String? = null,
    @Serializable(with = FlexibleStringListSerializer::class)
    val badges: List<String> = emptyList(),
    @JsonNames("is_id_verified", "isIdVerified", "is_verified")
    val isIdVerified: Boolean = false,
    @JsonNames("profile_image_url", "profileImageUrl", "profile_image")
    val profileImageUrl: String? = null
)

@Serializable
data class ProgressClientInfo(
    val id: Int = 0,
    val name: String = "Client",
    val phone: String? = null
)

@Serializable
data class ScopeExtraItem(
    val code: String = "",
    val name: String = "",
    val quantity: Int = 1,
    val amount: Double = 0.0
)

@Serializable
data class ScopeAdjustment(
    val id: Int = 0,
    @JsonNames("job_id", "jobId")
    val jobId: Int = 0,
    @JsonNames("additional_amount", "additionalAmount", "amount")
    val additionalAmount: Double = 0.0,
    val reason: String = "",
    val status: String = "pending",
    @JsonNames("extra_items", "extraItems")
    val extraItems: List<ScopeExtraItem> = emptyList(),
    @JsonNames("created_at", "createdAt")
    val createdAt: String? = null
)
