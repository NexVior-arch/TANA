package com.tana.data.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class OpenRouterImageUrl(
    @Json(name = "url") val url: String
)

@JsonClass(generateAdapter = true)
data class OpenRouterContentPart(
    @Json(name = "type") val type: String,
    @Json(name = "text") val text: String? = null,
    @Json(name = "image_url") val imageUrl: OpenRouterImageUrl? = null
)

@JsonClass(generateAdapter = true)
data class OpenRouterRequestMessage(
    @Json(name = "role") val role: String,
    @Json(name = "content") val content: Any
)

@JsonClass(generateAdapter = true)
data class OpenRouterMessage(
    @Json(name = "role") val role: String,
    @Json(name = "content") val content: String? = null
)

@JsonClass(generateAdapter = true)
data class OpenRouterChatRequest(
    @Json(name = "model") val model: String,
    @Json(name = "messages") val messages: List<OpenRouterRequestMessage>,
    @Json(name = "temperature") val temperature: Double = 0.7,
    @Json(name = "stream") val stream: Boolean? = null
)

@JsonClass(generateAdapter = true)
data class OpenRouterStreamDelta(
    @Json(name = "role") val role: String? = null,
    @Json(name = "content") val content: String? = null
)

@JsonClass(generateAdapter = true)
data class OpenRouterStreamChoice(
    @Json(name = "index") val index: Int? = null,
    @Json(name = "delta") val delta: OpenRouterStreamDelta? = null,
    @Json(name = "finish_reason") val finishReason: String? = null
)

@JsonClass(generateAdapter = true)
data class OpenRouterStreamChunk(
    @Json(name = "id") val id: String? = null,
    @Json(name = "choices") val choices: List<OpenRouterStreamChoice>? = null,
    @Json(name = "error") val error: OpenRouterError? = null
)

@JsonClass(generateAdapter = true)
data class OpenRouterChoice(
    @Json(name = "message") val message: OpenRouterMessage?,
    @Json(name = "finish_reason") val finishReason: String? = null
)

@JsonClass(generateAdapter = true)
data class OpenRouterChatResponse(
    @Json(name = "id") val id: String? = null,
    @Json(name = "choices") val choices: List<OpenRouterChoice>? = null,
    @Json(name = "error") val error: OpenRouterError? = null
)

@JsonClass(generateAdapter = true)
data class OpenRouterError(
    @Json(name = "message") val message: String?,
    @Json(name = "code") val code: Int? = null
)

@JsonClass(generateAdapter = true)
data class OpenRouterPricing(
    @Json(name = "prompt") val prompt: String? = "0",
    @Json(name = "completion") val completion: String? = "0",
    @Json(name = "image") val image: String? = null,
    @Json(name = "request") val request: String? = null
)

@JsonClass(generateAdapter = true)
data class OpenRouterModel(
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String? = null,
    @Json(name = "description") val description: String? = null,
    @Json(name = "context_length") val context_length: Int? = 128000,
    @Json(name = "pricing") val pricing: OpenRouterPricing? = null
)

@JsonClass(generateAdapter = true)
data class OpenRouterModelsResponse(
    @Json(name = "data") val data: List<OpenRouterModel>? = null
)

