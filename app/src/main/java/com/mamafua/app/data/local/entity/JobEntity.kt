package com.mamafua.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.mamafua.app.domain.model.Job

@Entity(tableName = "jobs")
data class JobEntity(
    @PrimaryKey val id: Int,
    val clientId: Int,
    val workerId: Int?,
    val title: String,
    val description: String,
    val location: String,
    val budget: Double,
    val status: String,
    val paymentStatus: String?,
    val createdAt: String?
) {
    fun toDomain(): Job {
        return Job(id, clientId, workerId, title, description, location, budget, com.mamafua.app.domain.model.JobStatus.fromValue(status), paymentStatus, createdAt)
    }
}

fun Job.toEntity(): JobEntity {
    return JobEntity(id, clientId, workerId, title, description, location, budget, status.value, paymentStatus, createdAt)
}
