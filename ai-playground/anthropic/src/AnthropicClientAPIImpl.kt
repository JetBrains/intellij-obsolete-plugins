package com.intellij.aiplayground.anthropic

import com.intellij.aiplayground.models.LlmModelId
import com.intellij.util.net.PlatformHttpClient
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.IOException
import java.net.URI
import java.net.http.HttpResponse
import java.time.Instant

internal val AnthropicJson: Json = Json {
  ignoreUnknownKeys = true
  encodeDefaults = false
}

@Serializable
internal data class ModelInfoDTO(
  val created_at: String,
  val display_name: String,
  val id: String,
  val type: String,
)

@Serializable
internal data class LocalModelsResponse(
  val data: List<ModelInfoDTO>,
  val first_id: String? = null,
  val has_more: Boolean,
  val last_id: String? = null,
)

class AnthropicClientAPIImpl(protected val getEndpointBaseUrl: () -> String) : AnthropicClientAPI {

  private fun url(path: String) = getEndpointBaseUrl() + path

  companion object {
    // Cache for models with 1-day expiration
    private var cachedModelDTOs: List<ModelInfoDTO>? = null
    private var cacheTimestamp: Long = 0
    private val cacheDurationMillis = 24 * 60 * 60 * 1000 // 1 day in milliseconds
    private val cacheMutex = Mutex() // Mutex for thread-safe cache access
  }

  // Function to filter models by creation time
  private fun filterModelsByCreatedAt(models: List<ModelInfoDTO>, filterByCreatedAt: Long?): List<ModelInfoDTO> {
    if (filterByCreatedAt == null) {
      return models
    }

    val currentTimeMillis = System.currentTimeMillis()
    return models.filter { modelInfo ->
      try {
        // Parse the ISO 8601 date string to Instant
        val createdAtInstant = Instant.parse(modelInfo.created_at)
        val createdAtMillis = createdAtInstant.toEpochMilli()

        // Check if the difference between current time and created_at is within the specified interval
        (currentTimeMillis - createdAtMillis) <= filterByCreatedAt
      } catch (e: Exception) {
        // If parsing fails, include the model by default
        true
      }
    }
  }

  // Function to convert ModelInfoDTOs to AnthropicModels
  private fun convertToAnthropicModels(modelInfoDTOs: List<ModelInfoDTO>): List<AnthropicModel> {
    return modelInfoDTOs.map { modelInfo -> AnthropicModel(LlmModelId(modelInfo.id)) }
  }

  override suspend fun listModels(apiKey: String, filterByCreatedAt: Long?): List<AnthropicModel> {
    // Check if cache is valid (less than 1 day old)
    val currentTime = System.currentTimeMillis()
    if (cachedModelDTOs != null && currentTime - cacheTimestamp < cacheDurationMillis) {
      // We have cached data, apply filtering to it
      val filteredData = filterModelsByCreatedAt(cachedModelDTOs!!, filterByCreatedAt)
      return convertToAnthropicModels(filteredData)
    }

    val request = PlatformHttpClient.requestBuilder(URI(url("/v1/models")))
      .header("x-api-key", apiKey)
      .header("anthropic-version", "2023-06-01")
      .build()
    val responseBody = PlatformHttpClient.client().use { client ->
      PlatformHttpClient.send(client, request, HttpResponse.BodyHandlers.ofString())
    }
    if (responseBody != null) {
      val modelsResponse = AnthropicJson.decodeFromString<LocalModelsResponse>(responseBody)

      // Cache all models without filtering
      cacheMutex.withLock {
        cachedModelDTOs = modelsResponse.data
        cacheTimestamp = System.currentTimeMillis()
      }

      // Apply filtering and return
      val filteredData = filterModelsByCreatedAt(modelsResponse.data, filterByCreatedAt)
      return convertToAnthropicModels(filteredData)
    }

    throw IOException("Failed to list models: empty response body")
  }
}
