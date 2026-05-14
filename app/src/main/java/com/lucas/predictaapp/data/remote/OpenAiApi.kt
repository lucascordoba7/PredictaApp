package com.lucas.predictaapp.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import okhttp3.MultipartBody
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

@Serializable
data class WhisperResponse(
    val text: String,
)

interface OpenAiApi {
    @Multipart
    @POST("audio/transcriptions")
    suspend fun transcribeAudio(
        @Part audio: MultipartBody.Part,
        @Part("model") model: okhttp3.RequestBody,
        @Part("language") language: okhttp3.RequestBody,
        @Part("response_format") responseFormat: okhttp3.RequestBody,
    ): WhisperResponse
}
