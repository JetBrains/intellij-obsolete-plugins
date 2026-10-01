package com.intellij.aiplayground.anthropic

import com.intellij.aiplayground.langchain.BaseLangChainService
import com.intellij.aiplayground.models.ANTHROPIC_PROVIDER
import com.intellij.aiplayground.models.ChatRequestConfig
import com.intellij.aiplayground.models.LlmModel
import com.intellij.aiplayground.models.LlmModelId
import com.intellij.aiplayground.models.ModelCapability
import com.intellij.aiplayground.models.ModelUtils
import com.intellij.aiplayground.models.extension.BaseApiKeyLlmServiceExtension
import com.intellij.aiplayground.models.settings.LangChainProviderSettings
import com.intellij.aiplayground.models.settings.LlmProviderSettings
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import dev.langchain4j.model.anthropic.AnthropicStreamingChatModel
import dev.langchain4j.model.chat.StreamingChatModel

class LangChainAnthropicService : BaseLangChainService() {
  val baseUrl: String = "https://api.anthropic.com"
  val apiImpl: AnthropicClientAPIImpl = AnthropicClientAPIImpl({ baseUrl })

  override fun getDefaultModelName(): LlmModelId {
    return DEFAULT_MODEL_NAME
  }

  override suspend fun getSupportedModels(project: Project, providerSettings: LlmProviderSettings<*>): List<LlmModel> {
    providerSettings as LangChainProviderSettings
    val apiKey = providerSettings.apiKey
    if (apiKey?.isNotEmpty() == true) {
      try {
        val modelList = apiImpl.listModels(apiKey, providerSettings.filterModelsByAge)
        if (modelList.isNotEmpty()) {
          return ModelUtils.mapModelIdsToLlmModels(
            modelIds = modelList.map { it.id },
            provider = ANTHROPIC_PROVIDER,
            capabilities = setOf(
              ModelCapability.CHAT,
              ModelCapability.COMPLETION,
              ModelCapability.CODE_GENERATION,
              ModelCapability.FUNCTION_CALLING
            ),
            contextWindow = 200000
          )
        }
      }
      catch (t: Throwable) {
        thisLogger().warn("Failed to fetch Anthropic models: ${t.message}")
        return emptyList()
      }
    }

    return listOf(
      LlmModel(
        id = LlmModelId("claude-3-5-sonnet-20241022"),
        provider = ANTHROPIC_PROVIDER,
        displayName = "Claude 3.5 Sonnet",
        capabilities = setOf(
          ModelCapability.CHAT,
          ModelCapability.COMPLETION,
          ModelCapability.CODE_GENERATION,
          ModelCapability.FUNCTION_CALLING
        ),
        contextWindow = 200000
      ),
      LlmModel(
        id = LlmModelId("claude-3-opus-20240229"),
        provider = ANTHROPIC_PROVIDER,
        displayName = "Claude 3 Opus",
        capabilities = setOf(
          ModelCapability.CHAT,
          ModelCapability.COMPLETION,
          ModelCapability.CODE_GENERATION,
          ModelCapability.FUNCTION_CALLING
        ),
        contextWindow = 200000
      ),
      LlmModel(
        id = LlmModelId("claude-3-sonnet-20240229"),
        provider = ANTHROPIC_PROVIDER,
        displayName = "Claude 3 Sonnet",
        capabilities = setOf(
          ModelCapability.CHAT,
          ModelCapability.COMPLETION,
          ModelCapability.CODE_GENERATION,
          ModelCapability.FUNCTION_CALLING
        ),
        contextWindow = 200000
      ),
      LlmModel(
        id = LlmModelId("claude-3-haiku-20240307"),
        provider = ANTHROPIC_PROVIDER,
        displayName = "Claude 3 Haiku",
        capabilities = setOf(
          ModelCapability.CHAT,
          ModelCapability.COMPLETION,
          ModelCapability.CODE_GENERATION
        ),
        contextWindow = 200000
      ),
      LlmModel(
        id = LlmModelId("claude-haiku-4-5-20251001"),
        provider = ANTHROPIC_PROVIDER,
        displayName = "Claude 4.5 Haiku",
        capabilities = setOf(
          ModelCapability.CHAT,
          ModelCapability.COMPLETION,
          ModelCapability.CODE_GENERATION
        ),
        contextWindow = 200000,
        isDefault = true
      ),
      LlmModel(
        id = LlmModelId("claude-sonnet-4-5-20250929"),
        provider = ANTHROPIC_PROVIDER,
        displayName = "Claude 4.5 Sonnet",
        capabilities = setOf(
          ModelCapability.CHAT,
          ModelCapability.COMPLETION,
          ModelCapability.CODE_GENERATION
        ),
        contextWindow = 200000
      )
    )
  }

  override fun createStreamingModel(model: LlmModel, providerSettings: LlmProviderSettings<*>, requestConfig: ChatRequestConfig): StreamingChatModel {
    providerSettings as LangChainProviderSettings
    val filteredRequestConfig = requestConfig.supportedParametersFiltered(model)
    return AnthropicStreamingChatModel.builder()
      .apiKey(providerSettings.apiKey)
      .modelName(model.id.id)
      .temperature(filteredRequestConfig.temperature)
      .topP(filteredRequestConfig.topP)
      .maxTokens(filteredRequestConfig.maxTokens)
      .build()
  }

  companion object {
    private val DEFAULT_MODEL_NAME = LlmModelId("claude-haiku-4-5-20251001")
  }

  class Extension : BaseApiKeyLlmServiceExtension(ANTHROPIC_PROVIDER) {
    override fun createService(): LangChainAnthropicService = LangChainAnthropicService()
  }
} 
