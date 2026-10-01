package com.intellij.aiplayground.openai

import com.intellij.aiplayground.models.LlmModelId
import com.intellij.util.net.PlatformHttpClient
import kotlinx.coroutines.future.await
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.IOException
import java.net.URI
import java.net.http.HttpResponse

internal val OpenAiJson: Json = Json {
  ignoreUnknownKeys = true
  encodeDefaults = false
}

@Serializable
internal data class ModelDTO(
  val id: String,
  @SerialName("object")
  val objectType: String,
  val created: Long,
  val owned_by: String
)

@Serializable
internal data class ModelsListResponse(
  @SerialName("object")
  val objectType: String,
  val data: List<ModelDTO>
)

class OpenAiClientAPIImpl() : OpenAiClientAPI {

  companion object {
    private var cachedModelDTOs: List<ModelDTO>? = null
    private var cacheTimestamp: Long = 0
    private val cacheDurationMillis = 24 * 60 * 60 * 1000 // 1 day in milliseconds
    private val cacheMutex = Mutex() // Mutex for thread-safe cache access
  }

  // Function to filter models by creation time
  private fun filterModelsByCreatedAt(models: List<ModelDTO>, filterByCreatedAt: Long?): List<ModelDTO> {
    if (filterByCreatedAt == null) {
      return models
    }

    val currentTimeSeconds = System.currentTimeMillis() / 1000 // Convert to seconds
    return models.filter { model ->
      // Check if the difference between current time and created is within the specified interval
      (currentTimeSeconds - model.created) <= (filterByCreatedAt / 1000) // Convert filterByCreatedAt to seconds
    }
  }

  // Function to filter and convert ModelDTOs to OpenAiModels
  private fun filterAndConvertModels(modelDTOs: List<ModelDTO>): List<OpenAiModel> {
    // Define patterns for non-chat models (models that are definitely not chat-compatible)
    val nonChatPrefixes = listOf(
      "text-embedding", "dall-e", "whisper", "tts-", "gpt-image", "computer-use", "omni-moderation"
    )

    // Define specific non-chat model patterns
    val nonChatPatterns = listOf(
      "-audio", "-realtime", "-search", "-transcribe", "-tts", "-image", "-alpha", "-instruct"
    )

    // Define specific non-chat models (explicitly mentioned or known)
    val specificNonChatModels = setOf(
      "o1-pro", "o1-pro-2025-03-19"
    )

    // Define patterns for chat-compatible models (models that are definitely chat-compatible)
    val chatPrefixes = listOf(
      "gpt-3.5-turbo", "gpt-4-turbo", "gpt-4o-mini", "gpt-4.1", "gpt-4.5", "gpt-5",
      "chatgpt-"
    )

    // Fine-tuned models pattern
    val ftPattern = "^ft:(.+)$".toRegex()

    return modelDTOs
      .filter { model ->
        val id = model.id

        // Step 1: Exclude specific non-chat models
        if (specificNonChatModels.contains(id)) {
          return@filter false
        }

        // Step 2: Exclude models with non-chat prefixes
        if (nonChatPrefixes.any { id.startsWith(it) }) {
          return@filter false
        }

        // Step 3: Exclude models with specific non-chat patterns
        if (nonChatPatterns.any { id.contains(it, ignoreCase = true) }) {
          return@filter false
        }

        // Step 4: Include models with definite chat prefixes
        if (chatPrefixes.any { id.startsWith(it) }) {
          return@filter true
        }

        // Step 5: Special case for o1/o3/o4 models - include only base models
        if ((id == "o1" || id == "o3" || id == "o4" || 
             id.startsWith("o1-") || id.startsWith("o3-") || id.startsWith("o4-")) && 
            !id.contains("-audio") && !id.contains("-realtime") &&
            !id.contains("-search") && !id.contains("-transcribe")) {
          // Include o1/o3/o4 base models and variants without non-chat indicators
          return@filter true
        }

        // Step 6: Handle fine-tuned models
        if (ftPattern.matches(id)) {
          val baseModel = ftPattern.find(id)?.groupValues?.get(1) ?: ""
          // Include fine-tuned models based on chat-compatible base models
          return@filter chatPrefixes.any { baseModel.startsWith(it) } || 
                        baseModel.startsWith("gpt-4o") || 
                        baseModel == "gpt-4" ||
                        baseModel.startsWith("gpt-3.5-turbo")
        }

        // Step 7: Include other common chat models
        id == "gpt-4" || id.startsWith("gpt-4o") || id.startsWith("davinci")
      }
      .map { modelDTO -> 
        // Models that don't support parameters like temperature
        val supportsParameters = !(modelDTO.id == "o1" || modelDTO.id == "o3" || modelDTO.id == "o4" ||
                                   modelDTO.id.startsWith("o1-") || modelDTO.id.startsWith("o3-") || modelDTO.id.startsWith("o4-") ||
                                   modelDTO.id.startsWith("gpt-5"))
        OpenAiModel(LlmModelId(modelDTO.id), supportsParameters)
      }
  }

  override suspend fun listModels(baseUrl: String, apiKey: String, filterByCreatedAt: Long?): List<OpenAiModel> {
    val currentTime = System.currentTimeMillis()

    // Check if we have cached data
    if (cachedModelDTOs != null && currentTime - cacheTimestamp < cacheDurationMillis) {
      // We have cached data, apply filtering to it
      val filteredData = filterModelsByCreatedAt(cachedModelDTOs!!, filterByCreatedAt)
      return filterAndConvertModels(filteredData)
    }

    // No valid cache, fetch from API
    val responseBody = PlatformHttpClient.client().use { client ->
      val request = PlatformHttpClient.requestBuilder(URI("$baseUrl/models"))
        .header("Authorization", "Bearer $apiKey")
        .build()
      PlatformHttpClient.send(client, request, HttpResponse.BodyHandlers.ofString())
    }
    if (responseBody != null) {
      val modelsResponse = OpenAiJson.decodeFromString<ModelsListResponse>(responseBody)

      // Cache all models without filtering
      cacheMutex.withLock {
        cachedModelDTOs = modelsResponse.data
        cacheTimestamp = System.currentTimeMillis()
      }

      // Apply filtering and return
      val filteredData = filterModelsByCreatedAt(modelsResponse.data, filterByCreatedAt)
      return filterAndConvertModels(filteredData)
    }

    throw IOException("Failed to list models: empty response body")
  }
}
