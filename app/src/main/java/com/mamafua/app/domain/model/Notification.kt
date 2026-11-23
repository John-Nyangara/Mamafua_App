package com.mamafua.app.domain.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonPrimitive

object FlexibleStringSerializer : KSerializer<String> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("FlexibleString", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): String {
        require(decoder is JsonDecoder) { "This serializer can be used only with JSON format" }
        val element = decoder.decodeJsonElement()
        return if (element is JsonPrimitive && element.isString) {
            element.content
        } else {
            element.toString()
        }
    }

    override fun serialize(encoder: Encoder, value: String) {
        encoder.encodeString(value)
    }
}

@Serializable
data class Notification(
    val id: Int,
    @SerialName("user_id") val userId: Int = 0,
    val type: String,
    val title: String,
    val body: String,
    @Serializable(with = FlexibleStringSerializer::class)
    val data: String? = null,
    @SerialName("is_read") val isRead: Boolean = false,
    @SerialName("created_at") val createdAt: String? = null
)
