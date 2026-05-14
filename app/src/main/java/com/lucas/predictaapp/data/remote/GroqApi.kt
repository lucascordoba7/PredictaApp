package com.lucas.predictaapp.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.POST

@Serializable
data class ChatMessage(
    val role: String,
    val content: String,
)

@Serializable
data class ChatCompletionRequest(
    val model: String,
    val messages: List<ChatMessage>,
    @SerialName("response_format") val responseFormat: ResponseFormat = ResponseFormat(),
    val temperature: Double = 0.3,
)

@Serializable
data class ResponseFormat(val type: String = "json_object")

@Serializable
data class ChatCompletionResponse(
    val choices: List<ChatChoice>,
)

@Serializable
data class ChatChoice(
    val message: ChatMessage,
)

interface GroqApi {
    @POST("chat/completions")
    suspend fun chatCompletion(@Body request: ChatCompletionRequest): ChatCompletionResponse
}
