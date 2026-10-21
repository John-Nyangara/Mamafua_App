package com.mamafua.app.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class JobStatus(val value: String) {
    @SerialName("open")
    OPEN("open"),
    @SerialName("applied")
    APPLIED("applied"),
    @SerialName("assigned")
    ASSIGNED("assigned"),
    @SerialName("accepted")
    ACCEPTED("accepted"),
    @SerialName("in_progress")
    IN_PROGRESS("in_progress"),
    @SerialName("completed")
    COMPLETED("completed"),
    @SerialName("cancelled")
    CANCELLED("cancelled"),
    @SerialName("disputed")
    DISPUTED("disputed");

    companion object {
        fun fromValue(value: String): JobStatus {
            return entries.find { it.value.equals(value, ignoreCase = true) } ?: OPEN
        }
    }
}
