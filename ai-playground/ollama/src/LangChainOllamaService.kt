package com.intellij.aiplayground.ollama

import com.intellij.aiplayground.models.ChatRequestConfig
import com.intellij.aiplayground.models.LlmModel
import com.intellij.aiplayground.models.LlmModelId
import com.intellij.aiplayground.models.LlmProvider
import com.intellij.aiplayground.models.LlmProviderInstance
import com.intellij.aiplayground.models.LlmProviderInstanceId
import com.intellij.aiplayground.models.LlmService
import com.intellij.aiplayground.models.ModelCapability
import com.intellij.aiplayground.models.OLLAMA_PROVIDER
import com.intellij.aiplayground.models.chat.ChatMessage
import com.intellij.aiplayground.models.chat.ChatResponseEvent
import com.intellij.aiplayground.models.chat.UserMessage
import com.intellij.aiplayground.models.extension.LlmServiceExtension
import com.intellij.aiplayground.models.settings.LlmProviderSettings
import com.intellij.aiplayground.models.settings.OllamaProviderSettings
import com.intellij.aiplayground.models.settings.PlaygroundSettings
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import kotlinx.coroutines.flow.Flow

class LangChainOllamaService : LlmService {
  val defaultBaseUrl: String = "http://localhost:11434" // Default Ollama server URL
  val apiImpl: OllamaKtorClientAPIImpl = OllamaKtorClientAPIImpl()

  private fun getBaseUrl(providerSettings: LlmProviderSettings<*>): String {
    providerSettings as OllamaProviderSettings
    return providerSettings.endpoint ?: defaultBaseUrl
  }

  override suspend fun getSupportedModels(project: Project, providerSettings: LlmProviderSettings<*>): List<LlmModel> {
    val baseUrl = getBaseUrl(providerSettings)
    try {
      // Get available models from OllamaModels
      val ollamaModels = apiImpl.listLocalModels(baseUrl)

      // Convert to LlmModel list
      return ollamaModels.map { ollamaModel ->
        LlmModel(
          id = ollamaModel.id,
          provider = OLLAMA_PROVIDER,
          displayName = ollamaModel.id.id.split(":").joinToString(" ") { part ->
            part.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
          },
          capabilities = setOf(
            ModelCapability.CHAT,
            ModelCapability.COMPLETION,
            ModelCapability.CODE_GENERATION
          ),
          contextWindow = 4096,
          isDefault = ollamaModel.id == getDefaultModelName()
        )
      }
    }
    catch (e: Exception) {
      thisLogger().warn("Failed to fetch Ollama models: ${e.message}")

      // Return a default model if we can't fetch the list
      return listOf(
        LlmModel(
          id = getDefaultModelName(),
          provider = OLLAMA_PROVIDER,
          displayName = "Llama 2",
          capabilities = setOf(
            ModelCapability.CHAT,
            ModelCapability.COMPLETION,
            ModelCapability.CODE_GENERATION
          ),
          contextWindow = 4096,
          isDefault = true
        )
      )
    }
  }

  override suspend fun streamingComplete(project: Project, prompt: String, systemPrompt: String?, model: LlmModel, providerSettings: LlmProviderSettings<*>, requestConfig: ChatRequestConfig, messages: List<ChatMessage>?): Flow<ChatResponseEvent> {
    val resultMessages = mutableListOf<ChatMessage>()
    // Add system message first if provided
    systemPrompt?.takeIf { it.isNotBlank() }?.let {
      resultMessages.add(com.intellij.aiplayground.models.chat.SystemMessage(com.intellij.aiplayground.models.chat.ChatMessageId(), it))
    }
    if (messages.isNullOrEmpty()) {
      resultMessages.add(UserMessage(content = prompt))
    } else {
      resultMessages.addAll(messages)
    }

    val options = OllamaOptions(numPredict = requestConfig.maxTokens,
                                topP = requestConfig.topP,
                                temperature = requestConfig.temperature)
    return apiImpl.chatCompletion(getBaseUrl(providerSettings), OllamaModel(model.id), resultMessages, options = options)
  }

  override suspend fun testConnection(project: Project, providerSettings: LlmProviderSettings<*>) {
    apiImpl.testConnection(getBaseUrl(providerSettings))
  }

  override fun getDefaultModelName(): LlmModelId {
    return DEFAULT_MODEL_NAME
  }

  companion object {
    private val DEFAULT_MODEL_NAME = LlmModelId("llama3.1:latest")
  }

  /**
   * Extension point implementation for Ollama service using LangChain4j
   */
  class Extension : LlmServiceExtension {
    override fun getProviderType(): LlmProvider = OLLAMA_PROVIDER

    override fun createService(): LangChainOllamaService = LangChainOllamaService()

    override fun createSettings(settings: PlaygroundSettings.ProviderSettings): LlmProviderSettings<*> {
      return OllamaProviderSettings(
        enabled = settings.enabled,
        displayName = settings.displayName,
        endpoint = settings.properties["endpoint"] ?: "http://localhost:11434"
      )
    }

    override fun removeSettings(instanceId: LlmProviderInstanceId) {}

    override fun updateSettings(instance: LlmProviderInstance): PlaygroundSettings.ProviderSettings {
      val settings = instance.settings as OllamaProviderSettings
      return PlaygroundSettings.ProviderSettings(
        id = instance.id.id,
        providerId = OLLAMA_PROVIDER.id.id,
        enabled = settings.enabled,
        displayName = settings.displayName,
        properties = buildMap {
          settings.endpoint?.let { put("endpoint", it) }
        }
      )
    }
  }
}

internal val LOG = logger<LangChainOllamaService>()
