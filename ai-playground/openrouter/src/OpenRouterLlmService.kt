package com.intellij.aiplayground.openrouter

import com.intellij.aiplayground.models.ChatRequestConfig
import com.intellij.aiplayground.models.LlmModel
import com.intellij.aiplayground.models.LlmModelId
import com.intellij.aiplayground.models.LlmService
import com.intellij.aiplayground.models.ModelCapability
import com.intellij.aiplayground.models.ModelUtils
import com.intellij.aiplayground.models.OPEN_ROUTER_PROVIDER
import com.intellij.aiplayground.models.chat.ChatMessage
import com.intellij.aiplayground.models.chat.ChatResponseEvent
import com.intellij.aiplayground.models.chat.GenericMessage
import com.intellij.aiplayground.models.chat.buildGenericMessages
import com.intellij.aiplayground.models.extension.BaseApiKeyLlmServiceExtension
import com.intellij.aiplayground.models.settings.LangChainProviderSettings
import com.intellij.aiplayground.models.settings.LlmProviderSettings
import com.intellij.openapi.project.Project
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect

class OpenRouterLlmService: LlmService {
  val baseUrl: String = "https://openrouter.ai/api/v1"
  val apiImpl: OpenRouterClientAPIImpl = OpenRouterClientAPIImpl({ baseUrl })

  override suspend fun getSupportedModels(project: Project, providerSettings: LlmProviderSettings<*>): List<LlmModel> {
    val settings = providerSettings as LangChainProviderSettings
    val apiKey = settings.apiKey
    return try {
      val models = apiImpl.listModels(apiKey ?: "")
      if (models.isNotEmpty()) {
        ModelUtils.mapModelIdsToLlmModels(
          modelIds = models.map { it.id },
          provider = OPEN_ROUTER_PROVIDER,
          capabilities = setOf(
            ModelCapability.CHAT,
            ModelCapability.COMPLETION,
            ModelCapability.CODE_GENERATION,
            ModelCapability.FUNCTION_CALLING
          )
        )
      }
      else emptyList()
    }
    catch (_: Throwable) {
      emptyList()
    }
  }

  override suspend fun streamingComplete(project: Project, prompt: String, systemPrompt: String?, model: LlmModel, providerSettings: LlmProviderSettings<*>, requestConfig: ChatRequestConfig, messages: List<ChatMessage>?): Flow<ChatResponseEvent> {
    val settings = providerSettings as LangChainProviderSettings
    val apiKey = settings.apiKey ?: error("API key is missing")

    val openRouterMessages = buildOpenRouterMessages(model, prompt, systemPrompt, messages)

    return apiImpl.streamingChatCompletion(
      apiKey = apiKey,
      model = OpenRouterModel(model.id),
      messages = openRouterMessages,
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

  companion object {
    private val DEFAULT_MODEL_NAME = LlmModelId("openai/gpt-5-mini")
  }

  private fun buildOpenRouterMessages(
    model: LlmModel,
    prompt: String,
    systemPrompt: String?,
    messages: List<ChatMessage>?,
  ): List<OpenRouterMessage> {
    val generic: List<GenericMessage> = buildGenericMessages(model, prompt, systemPrompt, messages)
    return generic.map { OpenRouterMessage(it.role, it.content) }
  }

  /**
   * Extension point implementation for OpenRouter service using common API-key base
   */
  class Extension : BaseApiKeyLlmServiceExtension(OPEN_ROUTER_PROVIDER) {
    override fun createService(): OpenRouterLlmService = OpenRouterLlmService()
  }
}