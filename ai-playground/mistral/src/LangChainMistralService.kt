package com.intellij.aiplayground.mistral

import com.intellij.aiplayground.langchain.BaseLangChainService
import com.intellij.aiplayground.models.ChatRequestConfig
import com.intellij.aiplayground.models.LlmModel
import com.intellij.aiplayground.models.LlmModelId
import com.intellij.aiplayground.models.MISTRAL_PROVIDER
import com.intellij.aiplayground.models.ModelCapability
import com.intellij.aiplayground.models.chat.ChatMessage
import com.intellij.aiplayground.models.chat.ChatResponseEvent
import com.intellij.aiplayground.models.chat.GenericMessage
import com.intellij.aiplayground.models.chat.buildGenericMessages
import com.intellij.aiplayground.models.extension.BaseApiKeyLlmServiceExtension
import com.intellij.aiplayground.models.settings.LangChainProviderSettings
import com.intellij.aiplayground.models.settings.LlmProviderSettings
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import dev.langchain4j.model.chat.StreamingChatModel
import dev.langchain4j.model.mistralai.MistralAiStreamingChatModel
import kotlinx.coroutines.flow.Flow


/**
 * Implementation of LlmService for Mistral using LangChain4j with coroutine support
 */
class LangChainMistralService : BaseLangChainService() {
  val baseUrl: String = "https://api.mistral.ai"
  val apiImpl: MistralClientAPIImpl = MistralClientAPIImpl({ baseUrl })

  override fun getDefaultModelName(): LlmModelId {
    return DEFAULT_MODEL_NAME
  }

  /**
   * Gets the list of supported models for Mistral
   */
  override suspend fun getSupportedModels(project: Project, providerSettings: LlmProviderSettings<*>): List<LlmModel> {
    providerSettings as LangChainProviderSettings
    val apiKey = providerSettings.apiKey
    if (apiKey?.isNotEmpty() == true) {
      try {
        val modelList = apiImpl.listModels(apiKey)
        if (modelList.isNotEmpty()) {
          return modelList.map { modelId ->
            LlmModel(
              id = modelId.id,
              provider = MISTRAL_PROVIDER,
              displayName = modelId.id.id,
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
      }
      catch (t: Throwable) {
        thisLogger().warn("Failed to fetch Mistral models: ${t.message}")
        return emptyList()
      }
    }

    return listOf(
      LlmModel(
        id = LlmModelId("mistral-small-2506"),
        provider = MISTRAL_PROVIDER,
        displayName = "Mistral Small",
        capabilities = setOf(
          ModelCapability.CHAT,
          ModelCapability.COMPLETION,
          ModelCapability.CODE_GENERATION,
          ModelCapability.FUNCTION_CALLING,
        ),
        contextWindow = 128000,
        isDefault = true
      ),
      LlmModel(
        id = LlmModelId("mistral-medium-2508"),
        provider = MISTRAL_PROVIDER,
        displayName = "Mistral Medium",
        capabilities = setOf(
          ModelCapability.CHAT,
          ModelCapability.COMPLETION,
          ModelCapability.CODE_GENERATION,
          ModelCapability.FUNCTION_CALLING,
        ),
        contextWindow = 128000
      ),
      LlmModel(
        id = LlmModelId("mistral-large-2411"),
        provider = MISTRAL_PROVIDER,
        displayName = "Mistral Large",
        capabilities = setOf(
          ModelCapability.CHAT,
          ModelCapability.COMPLETION,
          ModelCapability.CODE_GENERATION,
          ModelCapability.FUNCTION_CALLING,
        ),
        contextWindow = 128000
      ),
    )
  }

  override fun createStreamingModel(model: LlmModel, providerSettings: LlmProviderSettings<*>, requestConfig: ChatRequestConfig): StreamingChatModel {
    providerSettings as LangChainProviderSettings
    val filteredRequestConfig = requestConfig.supportedParametersFiltered(model)
    return MistralAiStreamingChatModel.builder()
      .apiKey(providerSettings.apiKey)
      .modelName(model.id.id)
      .temperature(filteredRequestConfig.temperature)
      .topP(filteredRequestConfig.topP)
      .maxTokens(filteredRequestConfig.maxTokens)
      .build()
  }

  override suspend fun streamingComplete(
    project: Project,
    prompt: String,
    systemPrompt: String?,
    model: LlmModel,
    providerSettings: LlmProviderSettings<*>,
    requestConfig: ChatRequestConfig,
    messages: List<ChatMessage>?,
  ): Flow<ChatResponseEvent> {
    val settings = providerSettings as LangChainProviderSettings
    val apiKey = settings.apiKey ?: error("API key is missing")

    val mistralMessages = buildMistralMessages(model, prompt, systemPrompt, messages)

    return apiImpl.streamingChatCompletion(
      apiKey = apiKey,
      model = MistralModel(model.id),
      messages = mistralMessages,
      temperature = requestConfig.temperature,
      topP = requestConfig.topP ?: 1.0,
      maxTokens = requestConfig.maxTokens
    )
  }


  private fun buildMistralMessages(
    model: LlmModel,
    prompt: String,
    systemPrompt: String?,
    messages: List<ChatMessage>?,
  ): List<MistralMessage> {
    val generic: List<GenericMessage> = buildGenericMessages(model, prompt, systemPrompt, messages)
    return generic.map { MistralMessage(role = it.role, content = it.content) }
  }

  companion object {
    private val DEFAULT_MODEL_NAME = LlmModelId("mistral-small-2506")
  }

  /**
   * Extension point implementation for Mistral service using LangChain4j
   */
  class Extension : BaseApiKeyLlmServiceExtension(MISTRAL_PROVIDER) {
    override fun createService(): LangChainMistralService = LangChainMistralService()
  }
} 
