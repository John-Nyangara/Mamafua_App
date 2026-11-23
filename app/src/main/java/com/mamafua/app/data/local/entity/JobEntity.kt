package com.mamafua.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.mamafua.app.domain.model.Job
import com.mamafua.app.domain.model.JobStatus

@Entity(tableName = "jobs")
data class JobEntity(
    @PrimaryKey val id: Int,
    val clientId: Int,
    val workerId: Int?,
    val title: String,
    val description: String,
    val location: String,
    val estate: String?,
    val category: String?,
    val budget: Double,
    val status: String,
    val paymentStatus: String?,
    val matchingMode: String?,
    val isInstant: Boolean,
    val scheduledAt: String?,
    val workerBringsDetergents: Boolean,
    val startPin: String?,
    val startedAt: String?,
    val createdAt: String?
) {
    fun toDomain(): Job {
        return Job(
            id = id,
            clientId = clientId,
            workerId = workerId,
            title = title,
            description = description,
            location = location,
            estate = estate,
            category = category,
            budget = budget,
            status = JobStatus.fromValue(status),
            paymentStatus = paymentStatus,
            matchingMode = matchingMode,
            isInstant = isInstant,
            scheduledAt = scheduledAt,
            workerBringsDetergents = workerBringsDetergents,
            startPin = startPin,
            startedAt = startedAt,
            createdAt = createdAt
        )
    }
}

fun Job.toEntity(): JobEntity {
    return JobEntity(
        id = id,
        clientId = clientId,
        workerId = workerId,
        title = title,
        description = description,
        location = location,
        estate = estate,
        category = category,
        budget = budget,
        status = status.value,
        paymentStatus = paymentStatus,
        matchingMode = matchingMode,
        isInstant = isInstant,
        scheduledAt = scheduledAt,
        workerBringsDetergents = workerBringsDetergents,
        startPin = startPin,
        startedAt = startedAt,
        createdAt = createdAt
    )
}
