package com.intellij.aiplayground.deepseek

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
internal val DeepseekJson: Json = Json {
  ignoreUnknownKeys = true
  encodeDefaults = false
  prettyPrint = false
  isLenient = true
}

@Serializable
internal data class DeepseekModelListResponse(
  val `object`: String,
  val data: List<DeepseekModelInfo>
)

@Serializable
internal data class DeepseekModelInfo(
  val id: String,
  val `object`: String,
  @SerialName("owned_by") val ownedBy: String,
)

@Serializable
internal data class DeepseekChatRequest(
  val model: String,
  val temperature: Double? = null,
  @SerialName("top_p") val topP: Double = 1.0,
  @SerialName("max_tokens") val maxTokens: Int? = null,
  val stream: Boolean = false,
  val messages: List<DeepseekMessage>
)

@Serializable
internal data class DeepseekChatResponse(
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
    val message: DeepseekMessage? = null,
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
internal data class DeepseekChatStreamChunk(
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

class DeepseekClientAPIImpl(private val getEndpointBaseUrl: () -> String) : DeepseekClientAPI {

  private fun url(path: String) = getEndpointBaseUrl() + path

  companion object {
    private var cachedModels: List<DeepseekModel>? = null
    private var cacheTimestamp: Long = 0
    private val cacheDurationMillis = 24 * 60 * 60 * 1000
    private val cacheMutex = Mutex()
  }

  override suspend fun listModels(apiKey: String): List<DeepseekModel> {
    val currentTime = System.currentTimeMillis()
    if (cachedModels != null && currentTime - cacheTimestamp < cacheDurationMillis) {
      return cachedModels!!
    }

    val request = PlatformHttpClient.requestBuilder(URI(url("/models")))
      .header("Authorization", "Bearer $apiKey")
      .build()
    val body = PlatformHttpClient.client().use { client ->
      PlatformHttpClient.send(client, request, HttpResponse.BodyHandlers.ofString()) ?: throw IOException("Empty response body")
    }

    val modelList = DeepseekJson.decodeFromString(DeepseekModelListResponse.serializer(), body)
    val models = modelList.data.map { DeepseekModel(LlmModelId(it.id)) }

    cacheMutex.withLock {
      cachedModels = models
      cacheTimestamp = System.currentTimeMillis()
    }

    return models
  }

  override suspend fun chatCompletion(apiKey: String, model: DeepseekModel, prompt: String): String {
    val messages = listOf(DeepseekMessage("user", prompt))

    val requestBody = DeepseekChatRequest(
      model = model.id.id,
      temperature = 0.7,
      topP = 1.0,
      maxTokens = null,
      stream = false,
      messages = messages
    )

    val jsonBody = DeepseekJson.encodeToString(DeepseekChatRequest.serializer(), requestBody)

    val request = PlatformHttpClient.requestBuilder(URI(url("/chat/completions")))
      .header("Content-Type", "application/json")
      .header("Authorization", "Bearer $apiKey")
      .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
      .build()
    val body = PlatformHttpClient.client().use { client ->
      PlatformHttpClient.send(client, request, HttpResponse.BodyHandlers.ofString()) ?: throw IOException("Empty response body")
    }

    val chatResponse = DeepseekJson.decodeFromString(DeepseekChatResponse.serializer(), body)
    return chatResponse.choices.firstOrNull()?.message?.content
           ?: throw IOException("No content in response")
  }

  override suspend fun streamingChatCompletion(
    apiKey: String,
    model: DeepseekModel,
    messages: List<DeepseekMessage>,
    temperature: Double?,
    topP: Double,
    maxTokens: Int?,
  ): Flow<ChatResponseEvent> {
    val requestBody = DeepseekChatRequest(
      model = model.id.id,
      temperature = temperature,
      topP = topP,
      maxTokens = maxTokens,
      stream = true,
      messages = messages
    )

    val jsonBody = DeepseekJson.encodeToString(DeepseekChatRequest.serializer(), requestBody)

    val request = PlatformHttpClient.requestBuilder(URI(url("/chat/completions")))
      .header("Content-Type", "application/json")
      .header("Authorization", "Bearer $apiKey")
      .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
      .build()

    return channelFlow {
      processStreamWithRetries(request, this)
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
          val response = client.send(request, HttpResponse.BodyHandlers.ofInputStream())
          BufferedReader(InputStreamReader(response.body(), StandardCharsets.UTF_8)).useLines { lines ->
            for (line in lines) {
              val trimmed = line.removePrefix("data:").trim()

              if (trimmed == "[DONE]") return
              if (trimmed.isBlank()) continue

              try {
                val chunk = DeepseekJson.decodeFromString(
                  DeepseekChatStreamChunk.serializer(), trimmed
                )

                val content = chunk.choices.firstOrNull()?.delta?.content
                if (!content.isNullOrBlank()) {
                  channel.trySend(PartialContentResponseEvent(content)).onFailure {
                    logger<DeepseekClientAPIImpl>().warn("Failed to send chunk to flow", it)
                  }
                }
              }
              catch (e: Exception) {
                throw e
              }
            }
          }

          return // Success, exit retry loop
        }
        catch (e: Exception) {
          lastException = addSuppressed(lastException, e)
          retryCount++

          if (retryCount <= maxRetries) {
            val delayTime = initialDelayMs * (1 shl (retryCount - 1))
            logger<DeepseekClientAPIImpl>().info("Retry $retryCount/$maxRetries after error: ${e.message}. Waiting ${delayTime}ms")
            delay(delayTime)
          }
        }
      }
    }

    if (lastException != null) {
      logger<DeepseekClientAPIImpl>().error("Failed after $maxRetries retries", lastException)
    }
  }
}
