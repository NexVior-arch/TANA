package com.tana.data.api

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Streaming

interface OpenRouterService {
    @POST("chat/completions")
    suspend fun createChatCompletion(
        @Header("Authorization") authorization: String,
        @Header("HTTP-Referer") referer: String = "https://ai.studio",
        @Header("X-Title") title: String = "TANA",
        @Body request: OpenRouterChatRequest
    ): Response<OpenRouterChatResponse>

    @Streaming
    @POST("chat/completions")
    suspend fun createChatCompletionStream(
        @Header("Authorization") authorization: String,
        @Header("HTTP-Referer") referer: String = "https://ai.studio",
        @Header("X-Title") title: String = "TANA",
        @Body request: OpenRouterChatRequest
    ): Response<ResponseBody>

    @GET("models")
    suspend fun getModels(): Response<OpenRouterModelsResponse>
}

