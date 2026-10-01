package com.intellij.aiplayground.mistral

import com.intellij.aiplayground.models.LlmModelId
import com.intellij.aiplayground.models.chat.ChatResponseEvent
import com.intellij.aiplayground.models.chat.ChatResponseEvent.PartialContentResponseEvent
import com.intellij.openapi.diagnostic.logger
import com.intellij.util.addSuppressed
import com.intellij.util.net.PlatformHttpClient
import kotlinx.coroutines.Dispatchers
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

internal val MistralJson: Json = Json {
  ignoreUnknownKeys = true
  encodeDefaults = false
  prettyPrint = false
  isLenient = true
}

@Serializable
internal data class MistralModelListResponse(
  val `object`: String,
  val data: List<MistralModelInfo>
)

@Serializable
internal data class MistralModelInfo(
  val id: String,
  val `object`: String,
  val created: Long,
  @SerialName("owned_by") val ownedBy: String,
  val capabilities: MistralModelCapabilities,
  val name: String,
  val description: String,
  @SerialName("max_context_length") val maxContextLength: Int,
  val aliases: List<String> = emptyList(),
  val deprecation: String? = null,
  @SerialName("default_model_temperature") val defaultModelTemperature: Float? = null,
  val type: String
)

@Serializable
internal data class MistralModelCapabilities(
  @SerialName("completion_chat") val completionChat: Boolean = false,
  @SerialName("completion_fim") val completionFim: Boolean = false,
  @SerialName("function_calling") val functionCalling: Boolean = false,
  @SerialName("fine_tuning") val fineTuning: Boolean = false,
  val vision: Boolean = false
)

@Serializable
internal data class MistralChatRequest(
  val model: String,
  val temperature: Double?,
  @SerialName("top_p") val topP: Double,
  @SerialName("max_tokens") val maxTokens: Int?,
  val stream: Boolean,
  val messages: List<MistralMessage>
)

@Serializable
internal data class MistralChatStreamChunk(
  val id: String? = null,
  val `object`: String? = null,
  val created: Long? = null,
  val model: String? = null,
  val choices: List<Choice>
) {
  @Serializable
  internal data class Choice(
    val index: Int,
    val delta: Delta? = null,
    val message: MistralMessage? = null,
    @SerialName("finish_reason") val finishReason: String? = null
  )

  @Serializable
  internal data class Delta(
    val role: String? = null,
    val content: String? = null
  )
}

class MistralClientAPIImpl(protected val getEndpointBaseUrl: () -> String) : MistralClientAPI {

  private fun url(path: String) = getEndpointBaseUrl() + path

  companion object {
    // Cache for models with 1-day expiration
    private var cachedModels: List<MistralModel>? = null
    private var cacheTimestamp: Long = 0
    private val cacheDurationMillis = 24 * 60 * 60 * 1000 // 1 day in milliseconds
    private val cacheMutex = Mutex() // Mutex for thread-safe cache access
  }

  override suspend fun listModels(apiKey: String): List<MistralModel> {
    // Check if cache is valid (less than 1 day old)
    val currentTime = System.currentTimeMillis()
    if (cachedModels != null && currentTime - cacheTimestamp < cacheDurationMillis) {
      return cachedModels!!
    }

    val request = PlatformHttpClient.requestBuilder(URI(url("/v1/models")))
      .header("Authorization", "Bearer $apiKey")
      .build()
    val responseBody = PlatformHttpClient.client().use { client ->
      PlatformHttpClient.send(client, request, HttpResponse.BodyHandlers.ofString())
    }
    if (responseBody != null) {
      val modelList = MistralJson.decodeFromString<MistralModelListResponse>(responseBody)
      val models = modelList.data.filter { it.capabilities.completionChat }.map { MistralModel(LlmModelId(it.id)) }

      // Update cache
      cacheMutex.withLock {
        cachedModels = models
        cacheTimestamp = System.currentTimeMillis()
      }

      return models
    }

    throw IOException("Failed to list models: empty response body")
  }

  override suspend fun chatCompletion(apiKey: String, model: MistralModel, prompt: String): String {
    val messages = listOf(MistralMessage("user", prompt))

    val requestBody = MistralChatRequest(
      model = model.id.id,
      temperature = 0.7,
      topP = 1.0,
      maxTokens = null,
      stream = false,
      messages = messages
    )

    val jsonBody = MistralJson.encodeToString(MistralChatRequest.serializer(), requestBody)

    val request = PlatformHttpClient.requestBuilder(URI(url("/v1/chat/completions")))
      .header("Content-Type", "application/json")
      .header("Authorization", "Bearer $apiKey")
      .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
      .build()

    val responseBody = PlatformHttpClient.client().use { client ->
      PlatformHttpClient.send(client, request, HttpResponse.BodyHandlers.ofString()) ?: throw IOException("Failed to get chat completion: empty response body")
    }

    // Parse the response to extract the content
    val chunk = MistralJson.decodeFromString<MistralChatStreamChunk>(responseBody)

    // Return the content from the first choice
    return chunk.choices.firstOrNull()?.message?.content ?: throw IOException("Failed to get chat completion: no content in response")
  }

  override suspend fun streamingChatCompletion(apiKey: String, model: MistralModel, messages: List<MistralMessage>, temperature: Double?, topP: Double, maxTokens: Int?): Flow<ChatResponseEvent> {
    val requestBody = MistralChatRequest(
      model = model.id.id,
      temperature = temperature,
      topP = topP,
      maxTokens = maxTokens,
      stream = true,
      messages = messages
    )

    val jsonBody = MistralJson.encodeToString(MistralChatRequest.serializer(), requestBody)

    val request = PlatformHttpClient.requestBuilder(URI(url("/v1/chat/completions")))
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
    channel: kotlinx.coroutines.channels.SendChannel<ChatResponseEvent>,
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
            for (line in lines) {
              val trimmed = line.removePrefix("data:").trim()

              if (trimmed == "[DONE]") return
              if (trimmed.isBlank()) continue

              try {
                val chunk = MistralJson.decodeFromString<MistralChatStreamChunk>(trimmed)

                val content = chunk.choices.firstOrNull()?.delta?.content
                              ?: chunk.choices.firstOrNull()?.message?.content

                content?.let {
                  channel.trySend(PartialContentResponseEvent(it)).onFailure {
                    logger<MistralClientAPIImpl>().warn("Failed to send chunk to flow", it)
                  }
                }
              }
              catch (e: Exception) {
                throw e // Rethrow to trigger retry
              }
            }
          }

          // If we get here without exceptions, we're done
          return
        }
        catch (e: Exception) {
          lastException = addSuppressed(lastException, e)
          retryCount++

          if (retryCount <= maxRetries) {
            val delayTime = initialDelayMs * (1 shl (retryCount - 1)) // Exponential backoff
            logger<MistralClientAPIImpl>().info("Retry $retryCount/$maxRetries after error: ${e.message}. Waiting ${delayTime}ms before next attempt.")
            delay(delayTime)
          }
        }
      }
    }

    // If we've exhausted all retries, log the last exception
    if (lastException != null) {
      logger<MistralClientAPIImpl>().error("Failed to process stream after $maxRetries retries", lastException)
    }
  }
}
