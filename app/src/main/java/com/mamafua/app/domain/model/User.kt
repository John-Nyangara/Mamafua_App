@file:OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)

package com.mamafua.app.domain.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonNames
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive

object FlexibleStringListSerializer : KSerializer<List<String>> {
    override val descriptor: SerialDescriptor =
        ListSerializer(String.serializer()).descriptor

    override fun deserialize(decoder: Decoder): List<String> {
        val jsonDecoder = decoder as? JsonDecoder
            ?: return emptyList()
        return when (val element = jsonDecoder.decodeJsonElement()) {
            is JsonArray -> element.mapNotNull { el ->
                runCatching { el.jsonPrimitive.content.trim() }.getOrNull()?.takeIf { it.isNotEmpty() }
            }
            is JsonPrimitive -> {
                if (element.isString) {
                    val raw = element.content.trim()
                    if (raw.isEmpty() || raw == "null") emptyList()
                    else raw.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                } else emptyList()
            }
            else -> emptyList()
        }
    }

    override fun serialize(encoder: Encoder, value: List<String>) {
        encoder.encodeSerializableValue(ListSerializer(String.serializer()), value)
    }
}

@Serializable
data class User(
    val id: Int = 0,
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val role: String = "client",
    @JsonNames("profile_picture_url", "profile_image_url", "profileImageUrl")
    val profilePictureUrl: String? = null,
    @JsonNames("is_verified", "isVerified")
    val isVerified: Boolean = false
)

@Serializable
data class WorkerProfile(
    val id: Int = 0,
    @JsonNames("user_id", "userId")
    val userId: Int = 0,
    @JsonNames("worker_id", "workerId")
    val workerIdFallback: Int? = null,
    @JsonNames("name", "worker_name", "full_name")
    val name: String? = null,
    val phone: String? = null,
    val service: String? = null,
    val location: String? = null,
    val estate: String? = null,
    @JsonNames("latitude", "lat")
    val latitude: Double? = null,
    @JsonNames("longitude", "lng", "lon")
    val longitude: Double? = null,
    val price: Double? = null,
    @JsonNames("hourly_rate", "hourlyRate")
    val hourlyRate: Double? = null,
    @JsonNames("average_rating", "averageRating", "rating")
    val averageRating: Double? = 0.0,
    @JsonNames("completed_jobs_count", "completedJobsCount")
    val completedJobsCount: Int = 0,
    val bio: String? = null,
    @Serializable(with = FlexibleStringListSerializer::class)
    val skills: List<String> = emptyList(),
    @Serializable(with = FlexibleStringListSerializer::class)
    val badges: List<String> = emptyList(),
    @JsonNames("is_id_verified", "isIdVerified", "is_verified")
    val isIdVerified: Boolean = false,
    @JsonNames("id_number", "idNumber")
    val idNumber: String? = null,
    @JsonNames("availability_status", "availabilityStatus", "status")
    val availabilityStatus: String? = null,
    @JsonNames("profile_image_url", "profileImageUrl", "profile_image", "profile_picture_url")
    val profileImageUrl: String? = null,
    @JsonNames("application_status", "applicationStatus")
    val applicationStatus: String? = null,
    @JsonNames("note", "favorite_note")
    val favoriteNote: String? = null
) {
    val resolvedUserId: Int
        get() = when {
            userId != 0 -> userId
            workerIdFallback != null && workerIdFallback != 0 -> workerIdFallback
            else -> id
        }

    val resolvedWorkerId: Int
        get() = when {
            id != 0 -> id
            workerIdFallback != null && workerIdFallback != 0 -> workerIdFallback
            else -> userId
        }

    val displayName: String
        get() = name?.takeIf { it.isNotBlank() } ?: "Mama Fua #$resolvedUserId"

    val displayRate: Double
        get() = (price ?: hourlyRate ?: 0.0)

    val isOnline: Boolean
        get() = availabilityStatus.equals("online", ignoreCase = true) ||
            availabilityStatus.equals("available", ignoreCase = true)
}

@Serializable
data class BlockedUser(
    val id: Int = 0,
    @JsonNames("blocked_user_id", "blockedUserId", "user_id")
    val blockedUserId: Int = 0,
    val name: String = "Blocked User",
    val email: String? = null,
    @JsonNames("created_at", "createdAt")
    val createdAt: String? = null
)
