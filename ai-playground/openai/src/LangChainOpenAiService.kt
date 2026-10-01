package com.intellij.aiplayground.openai

import com.intellij.aiplayground.langchain.BaseLangChainService
import com.intellij.aiplayground.models.ALL_PARAMETERS_SUPPORTED
import com.intellij.aiplayground.models.ChatRequestConfig
import com.intellij.aiplayground.models.LlmModel
import com.intellij.aiplayground.models.LlmModelId
import com.intellij.aiplayground.models.LlmProvider
import com.intellij.aiplayground.models.LlmProviderInstance
import com.intellij.aiplayground.models.LlmProviderInstanceId
import com.intellij.aiplayground.models.ModelCapability
import com.intellij.aiplayground.models.ModelUtils
import com.intellij.aiplayground.models.OPENAI_PROVIDER
import com.intellij.aiplayground.models.extension.LlmServiceExtension
import com.intellij.aiplayground.models.settings.ApplicationSettingsManagerService
import com.intellij.aiplayground.models.settings.LlmProviderSettings
import com.intellij.aiplayground.models.settings.OpenAIProviderSettings
import com.intellij.aiplayground.models.settings.PlaygroundSettings
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import dev.langchain4j.http.client.jdk.JdkHttpClientBuilder
import dev.langchain4j.model.chat.StreamingChatModel
import dev.langchain4j.model.openai.OpenAiStreamingChatModel

class LangChainOpenAiService : BaseLangChainService() {

  val apiImpl: OpenAiClientAPIImpl = OpenAiClientAPIImpl()

  override fun getDefaultModelName(): LlmModelId {
    return DEFAULT_MODEL_NAME
  }

  override suspend fun getSupportedModels(project: Project, providerSettings: LlmProviderSettings<*>): List<LlmModel> {
    providerSettings as OpenAIProviderSettings
    val baseUrl = providerSettings.endpoint?.takeIf { it.isNotBlank() } ?: DEFAULT_BASE_URL
    val apiKey = providerSettings.apiKey
    if (apiKey?.isNotEmpty() == true) {
      try {
        val modelList = apiImpl.listModels(baseUrl, apiKey, providerSettings.filterModelsByAge)
        if (modelList.isNotEmpty()) {
          val baseModels = ModelUtils.mapModelIdsToLlmModels(
            modelIds = modelList.map { it.id },
            provider = OPENAI_PROVIDER,
            capabilities = setOf(
              ModelCapability.CHAT,
              ModelCapability.COMPLETION,
              ModelCapability.CODE_GENERATION,
              ModelCapability.FUNCTION_CALLING
            ),
            contextWindow = 200000
          )
          return baseModels.zip(modelList).map { (base, dto) ->
            base.copy(supportedParameters = if (dto.supportsParameters) ALL_PARAMETERS_SUPPORTED else emptySet())
          }
        }
      }
      catch (t: Throwable) {
        thisLogger().warn("Failed to fetch OpenAI models: ${t.message}")
        return emptyList()
      }
    }

    return models
  }


  override fun createStreamingModel(model: LlmModel, providerSettings: LlmProviderSettings<*>, requestConfig: ChatRequestConfig): StreamingChatModel {
    providerSettings as OpenAIProviderSettings
    val baseUrl = providerSettings.endpoint?.takeIf { it.isNotBlank() } ?: DEFAULT_BASE_URL
    val filteredRequestConfig = requestConfig.supportedParametersFiltered(model)
    return OpenAiStreamingChatModel.builder()
      .baseUrl(baseUrl)
      .apiKey(providerSettings.apiKey)
      .modelName(model.id.id)
      .temperature(filteredRequestConfig.temperature)
      .topP(filteredRequestConfig.topP)
      .maxTokens(filteredRequestConfig.maxTokens)
      .httpClientBuilder(
        JdkHttpClientBuilder()
      )
      .build()
  }

  companion object {
    private val DEFAULT_MODEL_NAME = LlmModelId("gpt-5-mini")
    private const val DEFAULT_BASE_URL: String = "https://api.openai.com/v1"
  }

  class Extension : LlmServiceExtension {
    override fun getProviderType(): LlmProvider = OPENAI_PROVIDER

    override fun createService(): LangChainOpenAiService = LangChainOpenAiService()

    override fun createSettings(settings: PlaygroundSettings.ProviderSettings): LlmProviderSettings<*> {
      val settingsManagerService = service<ApplicationSettingsManagerService>()
      return OpenAIProviderSettings(
        enabled = settings.enabled,
        displayName = settings.displayName,
        apiKeyProvider = { settingsManagerService.getProviderApiKey(OPENAI_PROVIDER.id) },
        endpoint = settings.properties["endpoint"]
      )
    }

    override fun removeSettings(instanceId: LlmProviderInstanceId) {
      service<ApplicationSettingsManagerService>().removeProviderApiKey(OPENAI_PROVIDER.id)
    }

    override fun updateSettings(instance: LlmProviderInstance): PlaygroundSettings.ProviderSettings {
      val settings = instance.settings as OpenAIProviderSettings
      settings.apiKey?.let {
        service<ApplicationSettingsManagerService>().storeInstanceApiKey(instance.id, it)
        service<ApplicationSettingsManagerService>().storeProviderApiKey(instance.provider.id, it)
      }
      return PlaygroundSettings.ProviderSettings(
        id = instance.id.id,
        providerId = OPENAI_PROVIDER.id.id,
        enabled = settings.enabled,
        displayName = settings.displayName,
        properties = buildMap {
          settings.endpoint?.let { put("endpoint", it) }
        }
      )
    }
  }
} 
