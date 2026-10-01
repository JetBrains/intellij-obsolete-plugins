package com.intellij.aiplayground.deepseek

import com.intellij.aiplayground.models.ChatRequestConfig
import com.intellij.aiplayground.models.DEEPSEEK_PROVIDER
import com.intellij.aiplayground.models.LlmModel
import com.intellij.aiplayground.models.LlmModelId
import com.intellij.aiplayground.models.LlmService
import com.intellij.aiplayground.models.ModelCapability
import com.intellij.aiplayground.models.ModelUtils
import com.intellij.aiplayground.models.chat.ChatMessage
import com.intellij.aiplayground.models.chat.ChatResponseEvent
import com.intellij.aiplayground.models.chat.GenericMessage
import com.intellij.aiplayground.models.chat.buildGenericMessages
import com.intellij.aiplayground.models.extension.BaseApiKeyLlmServiceExtension
import com.intellij.aiplayground.models.settings.LangChainProviderSettings
import com.intellij.aiplayground.models.settings.LlmProviderSettings
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect


/**
 * Implementation of LlmService for Deepseek using LangChain4j with coroutine support
 */
class LangChainDeepseekService : LlmService {
  val baseUrl: String = "https://api.deepseek.com"
  val apiImpl: DeepseekClientAPIImpl = DeepseekClientAPIImpl({ baseUrl })

  /**
   * Gets the list of supported models for Deepseek
   */
  override suspend fun getSupportedModels(project: Project, providerSettings: LlmProviderSettings<*>): List<LlmModel> {
    providerSettings as LangChainProviderSettings
    val apiKey = providerSettings.apiKey
    if (apiKey?.isNotEmpty() == true) {
      try {
        val modelList = apiImpl.listModels(apiKey)
        if (modelList.isNotEmpty()) {
          return ModelUtils.mapModelIdsToLlmModels(
            modelIds = modelList.map { it.id },
            provider = DEEPSEEK_PROVIDER,
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
        thisLogger().warn("Failed to fetch Deepseek models: ${t.message}")
        return emptyList()
      }
    }

    return emptyList()
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

    val deepseekMessages = buildDeepseekMessages(model, prompt, systemPrompt, messages)

    return apiImpl.streamingChatCompletion(
      apiKey = apiKey,
      model = DeepseekModel(model.id),
      messages = deepseekMessages,
      temperature = requestConfig.temperature,
      topP = requestConfig.topP ?: 1.0,
      maxTokens = requestConfig.maxTokens
    )
  }

  override suspend fun testConnection(project: Project, providerSettings: LlmProviderSettings<*>) {
    return streamingComplete(
      project,
      "test",
      "",
      getSupportedModels(project, providerSettings).first { it.id == getDefaultModelName() },
      providerSettings,
      ChatRequestConfig.createDefault(),
      listOf()
    ).collect()
  }

  override fun getDefaultModelName(): LlmModelId {
    return DEFAULT_MODEL_NAME
  }

  private fun buildDeepseekMessages(
    model: LlmModel,
    prompt: String,
    systemPrompt: String?,
    messages: List<ChatMessage>?,
  ): List<DeepseekMessage> {
    val generic: List<GenericMessage> = buildGenericMessages(model, prompt, systemPrompt, messages)
    return generic.map { DeepseekMessage(role = it.role, content = it.content) }
  }

  companion object {
    private val DEFAULT_MODEL_NAME = LlmModelId("deepseek-chat")
  }

  /**
   * Extension point implementation for Deepseek service using LangChain4j
   */
  class Extension : BaseApiKeyLlmServiceExtension(DEEPSEEK_PROVIDER) {
    override fun createService(): LangChainDeepseekService = LangChainDeepseekService()
  }
} 
