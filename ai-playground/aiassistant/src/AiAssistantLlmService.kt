package com.intellij.aiplayground.aiassistant

import com.intellij.aiplayground.models.AI_ASSISTANT_PROVIDER
import com.intellij.aiplayground.models.ALL_PARAMETERS_SUPPORTED
import com.intellij.aiplayground.models.ChatRequestConfig
import com.intellij.aiplayground.models.LlmModel
import com.intellij.aiplayground.models.LlmModelId
import com.intellij.aiplayground.models.LlmProvider
import com.intellij.aiplayground.models.LlmProviderInstance
import com.intellij.aiplayground.models.LlmProviderInstanceId
import com.intellij.aiplayground.models.LlmService
import com.intellij.aiplayground.models.ModelCapability
import com.intellij.aiplayground.models.ModelParameter
import com.intellij.aiplayground.models.chat.ChatMessage
import com.intellij.aiplayground.models.chat.ChatResponseEvent
import com.intellij.aiplayground.models.extension.LlmServiceExtension
import com.intellij.aiplayground.models.settings.AIAssistantProviderSettings
import com.intellij.aiplayground.models.settings.LlmProviderSettings
import com.intellij.aiplayground.models.settings.PlaygroundSettings
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collect

// No need for these imports as we're using suspend functions directly

class AiAssistantLlmService : LlmService {

  override suspend fun getSupportedModels(project: Project, providerSettings: LlmProviderSettings<*>): List<LlmModel> {
    try {
      val aiAssistantModelsNames = AIAssistantModelsProvider.getSupportedModelsNames(project)?.map { LlmModelId(it) } ?: return emptyList()

      return aiAssistantModelsNames
        .filter { !notSupportedModels.contains(it) }
        .map { modelName ->
          LlmModel(
            id = modelName,
            displayName = modelName.id,
            capabilities = setOf(
              ModelCapability.CHAT,
              ModelCapability.COMPLETION,
              ModelCapability.CODE_GENERATION
            ),
            contextWindow = 200000,
            isDefault = modelName == DEFAULT_MODEL_NAME,
            provider = AI_ASSISTANT_PROVIDER,
            supportedParameters = getSupportedParametersForModel(modelName)
          )
        }
    }
    catch (e: Exception) {
      thisLogger().warn("Failed to fetch AIA models: ${e.message}")
      return emptyList()
    }
  }

  private fun getSupportedParametersForModel(modelName: LlmModelId): Set<ModelParameter> {
    if (modelName.id == "openai-o1" || modelName.id == "openai-o1-mini" || modelName.id == "openai-o3-mini" ||
        modelName.id.contains("openai-gpt-5")) {
      return emptySet()
    }
    return ALL_PARAMETERS_SUPPORTED
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
    val filteredRequestConfig = requestConfig.supportedParametersFiltered(model)

    val genericMessages = com.intellij.aiplayground.models.chat.buildGenericMessages(
      model = model,
      prompt = prompt,
      systemPrompt = systemPrompt,
      messages = messages,
    )
    val resultMessages = genericMessages.map { gm ->
      when (gm.role) {
        "system" -> PlaygroundChatMessage(PlaygroundChatRole.SYSTEM, gm.content)
        "assistant" -> PlaygroundChatMessage(PlaygroundChatRole.ASSISTANT, gm.content)
        else -> PlaygroundChatMessage(PlaygroundChatRole.USER, gm.content)
      }
    }

    val parameters = PlaygroundParameters(filteredRequestConfig.temperature, filteredRequestConfig.maxTokens, filteredRequestConfig.topP)

    return channelFlow {
      AIAssistantModelsProvider.makeRequest(
        project,
        model.id.id,
        resultMessages,
        parameters,
        object : PlaygroundStreamingChatResponseHandler {
          override fun onChatResponseEvent(event: ChatResponseEvent) {
            trySend(event)
          }

          override fun onError(error: Throwable) {
            close(error)
          }
        })
    }
  }

  override suspend fun testConnection(
    project: Project,
    providerSettings: LlmProviderSettings<*>,
  ) {
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
    private val DEFAULT_MODEL_NAME = LlmModelId("openai-gpt-5-mini")

    val notSupportedModels: List<LlmModelId> = listOf("openai-instruct-gpt", "openai-embedding-ada", "openai-embedding-large", "openai-embedding-small")
      .map { LlmModelId(it) }
  }

  class Extension : LlmServiceExtension {
    override fun getProviderType(): LlmProvider = AI_ASSISTANT_PROVIDER

    override fun createService(): AiAssistantLlmService = AiAssistantLlmService()

    override fun createSettings(settings: PlaygroundSettings.ProviderSettings): LlmProviderSettings<*> {
      return AIAssistantProviderSettings(
        enabled = settings.enabled,
        displayName = settings.displayName,
      )
    }

    override fun updateSettings(instance: LlmProviderInstance): PlaygroundSettings.ProviderSettings {
      val settings = instance.settings as AIAssistantProviderSettings
      return PlaygroundSettings.ProviderSettings(
        id = instance.id.id,
        providerId = AI_ASSISTANT_PROVIDER.id.id,
        enabled = settings.enabled,
        displayName = settings.displayName,
      )
    }

    override fun removeSettings(instanceId: LlmProviderInstanceId) {}

    override fun isAvailable(project: Project): StateFlow<Boolean> {
      return AIAssistantModelsProvider.isAvailable(project)
    }
  }
}
