package com.mamafua.app.domain.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object JobStatusSerializer : KSerializer<JobStatus> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("JobStatus", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): JobStatus {
        return JobStatus.fromValue(decoder.decodeString())
    }

    override fun serialize(encoder: Encoder, value: JobStatus) {
        encoder.encodeString(value.value)
    }
}

@Serializable(with = JobStatusSerializer::class)
enum class JobStatus(val value: String, val displayLabel: String) {
    OPEN("open", "Open"),
    APPLIED("applied", "Applied"),
    ASSIGNED("assigned", "Assigned"),
    ACCEPTED("accepted", "Accepted"),
    ARRIVED("arrived", "Arrived at Door"),
    IN_PROGRESS("in_progress", "In Progress"),
    COMPLETION_REQUESTED("completion_requested", "Awaiting Confirmation"),
    COMPLETED("completed", "Completed"),
    CANCELLED("cancelled", "Cancelled"),
    DISPUTED("disputed", "Disputed");

    companion object {
        fun fromValue(value: String?): JobStatus {
            if (value.isNullOrBlank()) return OPEN
            return entries.find {
                it.value.equals(value.trim(), ignoreCase = true) ||
                    it.name.equals(value.trim(), ignoreCase = true)
            } ?: OPEN
        }
    }
}
