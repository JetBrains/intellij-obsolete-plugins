package com.intellij.aiplayground.gemini

import com.intellij.aiplayground.models.chat.ChatResponseEvent
import com.intellij.aiplayground.models.chat.ChatResponseEvent.PartialContentResponseEvent
import com.intellij.openapi.diagnostic.logger
import com.intellij.util.addSuppressed
import com.intellij.util.net.PlatformHttpClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.onFailure
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.URI
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets

internal val GeminiJson: Json = Json {
  ignoreUnknownKeys = true
  encodeDefaults = false
  prettyPrint = false
  isLenient = true
}

@Serializable
internal data class GeminiModelListResponse(
  val models: List<GeminiModelInfo>,
  @SerialName("nextPageToken") val nextPageToken: String? = null
)

@Serializable
internal data class GeminiModelInfo(
  val name: String
)

@Serializable internal data class Part(val text: String? = null)

@Serializable internal data class Content(
  val role: String? = null,
  val parts: List<Part>
)

@Serializable internal data class GenerationConfig(
  val temperature: Double? = null,
  @SerialName("topP") val topP: Double? = null,
  @SerialName("maxOutputTokens") val maxOutputTokens: Int? = null
)

@Serializable internal data class GeminiGenerateContentRequest(
  val contents: List<Content>,
  @SerialName("generationConfig") val generationConfig: GenerationConfig? = null
)

@Serializable internal data class Candidate(val content: Content? = null)

@Serializable internal data class GeminiGenerateContentResponse(
  val candidates: List<Candidate>? = null
)

@Serializable internal data class GeminiStreamChunk(
  val candidates: List<Candidate>? = null
)

class GeminiClientAPIImpl(private val httpTimeoutMs: Long = 300_000) : GeminiClientAPI {

  companion object {
    private const val BASE_URL = "https://generativelanguage.googleapis.com"
    private const val API_VERSION = "v1beta"
    private var cachedModels: List<GeminiModel>? = null
    private var cacheTimestamp: Long = 0
    private val cacheDuration = 86_400_000L
    private val cacheMutex = Mutex()
  }

  private fun listModelsUrl(apiKey: String) = "$BASE_URL/$API_VERSION/models?key=$apiKey"
  private fun generateUrl(model: GeminiModel, apiKey: String) =
    "$BASE_URL/$API_VERSION/models/${model.id.id}:generateContent?key=$apiKey"
  private fun streamUrl(model: GeminiModel, apiKey: String) =
    "$BASE_URL/$API_VERSION/models/${model.id.id}:streamGenerateContent?alt=sse&key=$apiKey"

  override suspend fun listModels(apiKey: String): List<GeminiModel> {
    val now = System.currentTimeMillis()
    cacheMutex.withLock {
      if (cachedModels != null && now - cacheTimestamp < cacheDuration) return cachedModels!!
    }
    val request = PlatformHttpClient.requestBuilder(URI(listModelsUrl(apiKey))).build()
    val body = PlatformHttpClient.client().use { client ->
      PlatformHttpClient.send(client, request, HttpResponse.BodyHandlers.ofString()) ?: throw IOException("Empty response body")
    }
    val list = GeminiJson.decodeFromString(GeminiModelListResponse.serializer(), body)
    val models = list.models.map { GeminiModel(com.intellij.aiplayground.models.LlmModelId(it.name)) }
    cacheMutex.withLock {
      cachedModels = models
      cacheTimestamp = System.currentTimeMillis()
    }
    return models
  }

  override suspend fun chatCompletion(apiKey: String, model: GeminiModel, prompt: String): String {
    val requestBody = GeminiGenerateContentRequest(
      contents = listOf(Content(role = "user", parts = listOf(Part(prompt)))),
      generationConfig = GenerationConfig(temperature = 0.7)
    )
    val jsonBody = GeminiJson.encodeToString(GeminiGenerateContentRequest.serializer(), requestBody)
    val httpRequest = PlatformHttpClient.requestBuilder(URI(generateUrl(model, apiKey)))
      .header("Content-Type", "application/json")
      .timeout(java.time.Duration.ofMillis(httpTimeoutMs))
      .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
      .build()
    val body = PlatformHttpClient.client().use { client ->
      PlatformHttpClient.send(client, httpRequest, HttpResponse.BodyHandlers.ofString()) ?: throw IOException("Empty response body")
    }
    val genResponse = GeminiJson.decodeFromString(GeminiGenerateContentResponse.serializer(), body)
    return genResponse.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
           ?: throw IOException("No content in response")
  }

  override suspend fun streamingChatCompletion(
    apiKey: String,
    model: GeminiModel,
    messages: List<GeminiMessage>,
    temperature: Double?,
    topP: Double,
    maxTokens: Int?,
  ): Flow<ChatResponseEvent> {
    val contents = messages.map { Content(role = it.role, parts = listOf(Part(it.content))) }
    val requestBody = GeminiGenerateContentRequest(
      contents = contents,
      generationConfig = GenerationConfig(temperature = temperature, topP = topP, maxOutputTokens = maxTokens)
    )
    val jsonBody = GeminiJson.encodeToString(GeminiGenerateContentRequest.serializer(), requestBody)
    val httpRequest = PlatformHttpClient.requestBuilder(URI(streamUrl(model, apiKey)))
      .header("Content-Type", "application/json")
      .timeout(java.time.Duration.ofMillis(httpTimeoutMs))
      .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
      .build()
    return channelFlow { processStreamWithRetries(httpRequest, this) }.flowOn(Dispatchers.IO)
  }

  private suspend fun processStreamWithRetries(
    request: HttpRequest,
    channel: SendChannel<ChatResponseEvent>,
    maxRetries: Int = 3,
    initialDelayMs: Long = 1_000,
  ) {
    var attempts = 0
    var lastError: Exception? = null
    PlatformHttpClient.client().use { client ->
      while (attempts <= maxRetries) {
        try {
          val response = client.send(request, HttpResponse.BodyHandlers.ofInputStream())
          BufferedReader(InputStreamReader(response.body(), StandardCharsets.UTF_8)).useLines { lines ->
            for (raw in lines) {
              val line = raw.removePrefix("data:").trim()
              if (line == "[DONE]") break
              if (line.isBlank()) continue
              val chunk = GeminiJson.decodeFromString(GeminiStreamChunk.serializer(), line)
              val delta = chunk.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
              if (!delta.isNullOrEmpty()) {
                channel.trySend(PartialContentResponseEvent(delta)).onFailure {
                  logger<GeminiClientAPIImpl>().warn("Failed to emit chunk", it)
                }
              }
            }
          }
          return
        }
        catch (e: Exception) {
          lastError = addSuppressed(lastError, e)
          attempts++
          if (attempts <= maxRetries) {
            val backoff = initialDelayMs * (1 shl (attempts - 1))
            logger<GeminiClientAPIImpl>().info("Retry $attempts/$maxRetries after ${e.message}. Waiting ${backoff} ms")
            delay(backoff)
          }
        }
      }
    }
    logger<GeminiClientAPIImpl>().error("Streaming failed after $maxRetries retries", lastError)
  }
}
