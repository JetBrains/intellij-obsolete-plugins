package com.intellij.aidebugger.evaluation.models.llm

import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

private data class GeminiRequest(
    val contents: List<Content>,
    val generationConfig: GenerationConfig? = null
) {
    data class Content(val parts: List<Part>)
    data class Part(val text: String)
    data class GenerationConfig(val temperature: Double? = null)
}

private data class GeminiResponse(
    val candidates: List<Candidate>
) {
    data class Candidate(val content: Content)
    data class Content(val parts: List<Part>)
    data class Part(val text: String)
}

class GeminiLlmProvider<T : Any>(
    private val httpClient: OkHttpClient,
    private val apiKey: String,
    private val modelName: String,
    private val temperature: Double?,
    private val responseClass: Class<T>,
    private val gson: Gson = Gson()
) : LlmProvider<T> {

    private val baseUrl = "https://generativelanguage.googleapis.com/v1beta"

    @Suppress("UNCHECKED_CAST")
    override suspend fun complete(prompt: String): T = withContext(Dispatchers.IO) {
        val request = GeminiRequest(
            contents = listOf(
                GeminiRequest.Content(
                    parts = listOf(GeminiRequest.Part(text = prompt))
                )
            ),
            generationConfig = temperature?.let { GeminiRequest.GenerationConfig(temperature = it) }
        )

        val requestBody = gson.toJson(request)
            .toRequestBody("application/json; charset=utf-8".toMediaType())

        val httpRequest = Request.Builder()
            .url("$baseUrl/models/$modelName:generateContent?key=$apiKey")
            .post(requestBody)
            .build()

        val response = httpClient.newCall(httpRequest).execute()

        if (!response.isSuccessful) {
            throw IllegalStateException("Gemini API call failed: ${response.code} ${response.message}")
        }

        val responseBody = response.body?.string()
            ?: throw IllegalStateException("Gemini returned empty response")

        val geminiResponse = gson.fromJson(responseBody, GeminiResponse::class.java)
        val text = geminiResponse.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text
            ?: throw IllegalStateException("Gemini returned no text content")

        if (responseClass == String::class.java) {
            text as T
        } else {
            gson.fromJson(text, responseClass)
        }
    }

    companion object {
        inline fun <reified T : Any> create(
            config: LlmProviderConfig
        ): GeminiLlmProvider<T> {
            val httpClient = OkHttpClient.Builder()
                .connectTimeout(config.timeout)
                .readTimeout(config.timeout)
                .writeTimeout(config.timeout)
                .build()

            return GeminiLlmProvider(
                httpClient = httpClient,
                apiKey = config.apiKey,
                modelName = config.modelName,
                temperature = config.temperature,
                responseClass = T::class.java
            )
        }
    }
}