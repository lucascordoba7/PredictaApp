package com.lucas.predictaapp.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

@Serializable
data class AnthropicMessageRequest(
    val model: String,
    @SerialName("max_tokens") val maxTokens: Int,
    val system: String,
    val messages: List<AnthropicMessage>,
)

@Serializable
data class AnthropicMessage(
    val role: String,
    val content: List<ContentBlock>,
)

@Serializable
sealed class ContentBlock {
    @Serializable
    @SerialName("text")
    data class Text(val type: String = "text", val text: String) : ContentBlock()

    @Serializable
    @SerialName("image")
    data class Image(
        val type: String = "image",
        val source: ImageSource,
    ) : ContentBlock()
}

@Serializable
data class ImageSource(
    val type: String = "base64",
    @SerialName("media_type") val mediaType: String,
    val data: String,
)

@Serializable
data class AnthropicMessageResponse(
    val content: List<ContentBlockResponse>,
)

@Serializable
data class ContentBlockResponse(
    val type: String,
    val text: String? = null,
)

interface AnthropicApi {
    @POST("messages")
    suspend fun createMessage(
        @Body request: AnthropicMessageRequest,
    ): AnthropicMessageResponse
}
