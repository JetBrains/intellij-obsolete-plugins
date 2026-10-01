package com.intellij.aiplayground.ollama

import com.intellij.aiplayground.models.LlmModelId
import com.intellij.aiplayground.models.chat.AssistantMessage
import com.intellij.aiplayground.models.chat.ChatMessage
import com.intellij.aiplayground.models.chat.ChatResponseEvent
import com.intellij.aiplayground.models.chat.ChatResponseEvent.PartialContentResponseEvent
import com.intellij.aiplayground.models.chat.SystemMessage
import com.intellij.aiplayground.models.chat.UserMessage
import com.intellij.aiplayground.ollama.dto.ChatCompletionRequest
import com.intellij.aiplayground.ollama.dto.ChatCompletionResponse
import com.intellij.aiplayground.ollama.dto.MessageDTO
import com.intellij.aiplayground.ollama.dto.ModelInfoDTO
import com.intellij.aiplayground.ollama.dto.RoleDTO
import com.intellij.openapi.diagnostic.debug
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.sse.SSE
import io.ktor.client.request.get
import io.ktor.client.request.preparePost
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.utils.io.readUTF8Line
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.io.IOException

internal val OllamaJson: Json = Json {
  ignoreUnknownKeys = true
  encodeDefaults = false
}

@Serializable
data class LocalModelsResponse(val models: List<ModelInfoDTO>)

open class OllamaKtorClientAPIImpl() : OllamaClientAPI {
  private val httpClient = PlaygroundKtorHttpClientUtil.httpClient().config {
    install(ContentNegotiation) {
      json(OllamaJson)
    }
    install(SSE)
  }

  companion object {
    // Cache for models with 1-day expiration
    private var cachedLocalModels: List<OllamaModel>? = null
    private var cachedRunningModels: List<OllamaModel>? = null
    private var localModelsCacheTimestamp: Long = 0
    private var runningModelsCacheTimestamp: Long = 0
    private val cacheDurationMillis = 24 * 60 * 60 * 1000 // 1 day in milliseconds
    private val cacheMutex = Mutex() // Mutex for thread-safe cache access
  }

  override suspend fun testConnection(baseUrl: String): Boolean {
    try {
      val response = httpClient.get("$baseUrl/")
      return response.status == HttpStatusCode.OK && response.bodyAsText().contains("Ollama is running", ignoreCase = true)
    }
    catch (e: IOException) {
      LOG.warn("Failed to connect to Ollama @ $baseUrl/: $e")
      return false
    }
  }

  override suspend fun listLocalModels(baseUrl: String): List<OllamaModel> {
    // Check if cache is valid (less than 1 day old)
    val currentTime = System.currentTimeMillis()
    if (cachedLocalModels != null && currentTime - localModelsCacheTimestamp < cacheDurationMillis) {
      return cachedLocalModels!!
    }

    val response: HttpResponse = httpClient.get("$baseUrl/api/tags")
    if (response.status != HttpStatusCode.OK) {
      throw IOException("Failed to list local models: status ${response.status.value}\n${response.bodyAsText()}")
    }
    val models = response.body<LocalModelsResponse>().models
      .map { modelInfo -> OllamaModel(LlmModelId(modelInfo.model)) }

    // Update cache
    cacheMutex.withLock {
      cachedLocalModels = models
      localModelsCacheTimestamp = System.currentTimeMillis()
    }

    return models
  }

  override suspend fun listRunningModels(baseUrl: String): List<OllamaModel> {
    // Check if cache is valid (less than 1 day old)
    val currentTime = System.currentTimeMillis()
    if (cachedRunningModels != null && currentTime - runningModelsCacheTimestamp < cacheDurationMillis) {
      return cachedRunningModels!!
    }

    val response: HttpResponse = httpClient.get("$baseUrl/api/ps")
    if (response.status != HttpStatusCode.OK) {
      throw IOException("Failed to list running models: status ${response.status.value}\n${response.bodyAsText()}")
    }
    val models = response.body<LocalModelsResponse>().models
      .map { modelInfo -> OllamaModel(LlmModelId(modelInfo.model)) }

    // Update cache
    cacheMutex.withLock {
      cachedRunningModels = models
      runningModelsCacheTimestamp = System.currentTimeMillis()
    }

    return models
  }

  override suspend fun chatCompletion(
    baseUrl: String,
    model: OllamaModel,
    messages: List<ChatMessage>,
    options: OllamaOptions?,
  ): Flow<ChatResponseEvent> {
    val chatCompletionRequest = ChatCompletionRequest(
      model = model.id.id,
      messages = messages.toMessageDTOs(),
      stream = true,
      keepAlive = "30m",
      options = options?.toJson()
    )
    LOG.debug {
      "Ollama chat completion called for model $model\n"
    }
    return channelFlow {
      try {
        httpClient.preparePost("$baseUrl/api/chat") {
          contentType(ContentType.Application.Json)
          setBody(chatCompletionRequest)
        }.execute { httpResponse ->
          if (httpResponse.status != HttpStatusCode.OK) {
            throw IOException("Chat completion failure: status ${httpResponse.status.value}\n${httpResponse.bodyAsText()}")
          }
          val readChannel = httpResponse.bodyAsChannel()
          while (true) {
            val line = readChannel.readUTF8Line() ?: throw IOException("unexpected EOF")
            LOG.debug { "Response part: $line" }
            val completionResponse: ChatCompletionResponse = OllamaJson.decodeFromString(line)
            val msg = completionResponse.message
            if (msg.role != RoleDTO.ASSISTANT) {
              throw IOException("Chat completion failure: expected assistant role, got ${msg.role}\n${httpResponse.bodyAsText()}")
            }
            if (msg.content != "") {
              trySend(PartialContentResponseEvent(msg.content))
            }
            if (completionResponse.done) {
              break
            }
          }
        }
      }
      catch (e: IOException) {
        LOG.warn("Failed to chat completion: $e")
        throw e
      }
    }
  }
}

private fun List<ChatMessage>.toMessageDTOs(): List<MessageDTO> = map { message ->
  when (message) {
    is SystemMessage -> MessageDTO(
      role = RoleDTO.SYSTEM,
      content = message.content
    )
    is AssistantMessage -> MessageDTO(
      role = RoleDTO.ASSISTANT,
      content = message.content
    )
    is UserMessage -> MessageDTO(
      role = RoleDTO.USER,
      content = message.content
    )
  }
}.mergeConsequentAssistantMessages()

private fun List<MessageDTO>.mergeConsequentAssistantMessages(): List<MessageDTO> {
  val result = mutableListOf<MessageDTO>()
  for (m in this) {
    when (m.role) {
      RoleDTO.ASSISTANT -> {
        if (result.isNotEmpty() && result.last().role == RoleDTO.ASSISTANT) {
          val last = result.removeLast()
          result.add(MessageDTO(
            role = RoleDTO.ASSISTANT,
            content = last.content + "\n" + m.content,
            toolCalls = last.toolCalls + m.toolCalls
          ))
        }
        else {
          result.add(m)
        }
      }
      else -> {
        result.add(m)
      }
    }
  }
  return result
}

private fun OllamaOptions.toJson(): JsonObject = JsonObject(buildMap {
  contextSize?.let { put("num_ctx", JsonPrimitive(it)) }
  put("num_predict", JsonPrimitive(numPredict))
  put("temperature", JsonPrimitive(temperature))
  put("top_p", JsonPrimitive(topP))
})
