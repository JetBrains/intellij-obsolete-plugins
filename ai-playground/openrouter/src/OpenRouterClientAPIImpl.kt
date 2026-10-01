package com.intellij.aiplayground.openrouter

import com.intellij.aiplayground.models.LlmModelId
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

// JSON configuration
internal val OpenRouterJson: Json = Json {
  ignoreUnknownKeys = true
  encodeDefaults = false
  prettyPrint = false
  isLenient = true
}

@Serializable
internal data class OpenRouterModelListResponse(
  val data: List<OpenRouterModelInfo>
)

@Serializable
internal data class OpenRouterModelInfo(
  val id: String,
  @SerialName("canonical_slug") val canonicalSlug: String? = null,
  val name: String? = null,
  val created: Long? = null,
  val description: String? = null,
  @SerialName("context_length") val contextLength: Int? = null,
)

@Serializable
internal data class OpenRouterChatRequest(
  val model: String,
  val temperature: Double? = null,
  @SerialName("top_p") val topP: Double = 1.0,
  @SerialName("max_tokens") val maxTokens: Int? = null,
  val stream: Boolean = false,
  val messages: List<OpenRouterMessage>
)

@Serializable
internal data class OpenRouterChatResponse(
  val id: String,
  val `object`: String,
  val created: Long,
  val model: String,
  val choices: List<Choice>,
  val usage: Usage? = null
) {
  @Serializable
  data class Choice(
    val index: Int,
    val message: OpenRouterMessage? = null,
    @SerialName("finish_reason") val finishReason: String? = null
  )

  @Serializable
  data class Usage(
    @SerialName("prompt_tokens") val promptTokens: Int,
    @SerialName("completion_tokens") val completionTokens: Int,
    @SerialName("total_tokens") val totalTokens: Int
  )
}

@Serializable
internal data class OpenRouterChatStreamChunk(
  val id: String? = null,
  val `object`: String? = null,
  val created: Long? = null,
  val model: String? = null,
  val choices: List<Choice>,
) {
  @Serializable
  data class Choice(
    val index: Int,
    val delta: Delta? = null,
    @SerialName("finish_reason") val finishReason: String? = null
  )

  @Serializable
  data class Delta(
    val role: String? = null,
    val content: String? = null
  )
}

class OpenRouterClientAPIImpl(private val getEndpointBaseUrl: () -> String) : OpenRouterClientAPI {

  private fun url(path: String) = getEndpointBaseUrl() + path

  companion object {
    private var cachedModels: List<OpenRouterModel>? = null
    private var cacheTimestamp: Long = 0
    private val cacheDurationMillis = 24 * 60 * 60 * 1000
    private val cacheMutex = Mutex()
  }

  override suspend fun listModels(apiKey: String): List<OpenRouterModel> {
    val currentTime = System.currentTimeMillis()
    if (cachedModels != null && currentTime - cacheTimestamp < cacheDurationMillis) {
      return cachedModels!!
    }

    val body = PlatformHttpClient.client().use { client ->
      // OpenRouter models endpoint does not require API key
      val request = PlatformHttpClient.request(URI(url("/models")))
      PlatformHttpClient.send(client, request, HttpResponse.BodyHandlers.ofString()) ?: throw IOException("Empty response body")
    }
    val modelList = OpenRouterJson.decodeFromString(OpenRouterModelListResponse.serializer(), body)
    val models = modelList.data.map { OpenRouterModel(LlmModelId(it.id)) }

    cacheMutex.withLock {
      cachedModels = models
      cacheTimestamp = System.currentTimeMillis()
    }

    return models
  }

  override suspend fun streamingChatCompletion(
    apiKey: String,
    model: OpenRouterModel,
    messages: List<OpenRouterMessage>,
    temperature: Double?,
    topP: Double,
    maxTokens: Int?
  ): Flow<ChatResponseEvent> {
    val requestBody = OpenRouterChatRequest(
      model = model.id.id,
      temperature = temperature,
      topP = topP,
      maxTokens = maxTokens,
      stream = true,
      messages = messages
    )

    val jsonBody = OpenRouterJson.encodeToString(OpenRouterChatRequest.serializer(), requestBody)

    val request = PlatformHttpClient.requestBuilder(URI(url("/chat/completions")))
      .header("Content-Type", "application/json")
      .header("Authorization", "Bearer $apiKey")
      .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
      .build()

    return channelFlow {
      processStreamWithRetries(request, channel = this)
    }.flowOn(Dispatchers.IO)
  }

  private suspend fun processStreamWithRetries(
    request: HttpRequest,
    channel: SendChannel<ChatResponseEvent>,
    maxRetries: Int = 3,
    initialDelayMs: Long = 1000,
  ) {
    var retryCount = 0
    var lastException: Exception? = null

    PlatformHttpClient.client().use { client ->
      while (retryCount <= maxRetries) {
        try {
          val response = PlatformHttpClient.send(client, request, HttpResponse.BodyHandlers.ofInputStream())
          BufferedReader(InputStreamReader(response, StandardCharsets.UTF_8)).useLines { lines ->
            for (rawLine in lines) {
              val line = rawLine.trimEnd()
              if (line.isBlank()) continue
              if (!line.startsWith("data:")) continue

              val data = line.removePrefix("data:").trim()
              if (data == "[DONE]") return
              if (data.isBlank()) continue

              try {
                val chunk = OpenRouterJson.decodeFromString(
                  OpenRouterChatStreamChunk.serializer(), data
                )

                val content = chunk.choices.firstOrNull()?.delta?.content
                if (!content.isNullOrBlank()) {
                  channel.trySend(PartialContentResponseEvent(content)).onFailure {
                    logger<OpenRouterClientAPIImpl>().warn("Failed to send chunk to flow", it)
                  }
                }
              }
              catch (_: Exception) {
                // Ignore JSON parse errors, continue reading next lines (aligns with sample code)
              }
            }
          }

          return
        }
        catch (e: Exception) {
          lastException = addSuppressed(lastException, e)
          retryCount++
          if (retryCount > maxRetries) break
          val delayMs = initialDelayMs * (1 shl (retryCount - 1))
          logger<OpenRouterClientAPIImpl>().warn("Streaming failed, retrying in ${'$'}delayMs ms (attempt ${'$'}retryCount/${'$'}maxRetries)", e)
          delay(delayMs)
        }
      }
    }

    throw lastException ?: IOException("Unknown error during streaming")
  }
}
