package com.intellij.aiplayground.models

import com.intellij.aiplayground.models.chat.ChatMessage
import com.intellij.aiplayground.models.chat.ChatResponseEvent
import com.intellij.aiplayground.models.settings.LlmProviderSettings
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsSafe
import kotlinx.coroutines.flow.Flow
import java.util.UUID

/**
 * Interface for LLM service that manages communication with language model providers
 */
interface LlmService {
  /**
   * Gets the list of supported models for a specific provider
   */
  suspend fun getSupportedModels(project: Project, providerSettings: LlmProviderSettings<*>): List<LlmModel>

  /**
   * Streaming completion request with callback for partial responses
   */
  suspend fun streamingComplete(
    project: Project,
    prompt: String,
    systemPrompt: String? = null,
    model: LlmModel,
    providerSettings: LlmProviderSettings<*>,
    requestConfig: ChatRequestConfig,
    messages: List<ChatMessage>? = null,
  ): Flow<ChatResponseEvent>

  suspend fun testConnection(project: Project, providerSettings: LlmProviderSettings<*>)

  fun getDefaultModelName(): LlmModelId
}

@JvmInline
value class LlmProviderId(val id: String)

data class LlmProvider(
  val id: LlmProviderId,
  @NlsSafe val displayName: String,
  val canHaveMultipleInstances: Boolean = false,
  val predefined: Boolean = false,
)

// Built-in providers as constants for convenience
val OPENAI_PROVIDER: LlmProvider = LlmProvider(LlmProviderId("openai"), "OpenAI")
val OPENAI_COMPATIBLE_PROVIDER: LlmProvider = LlmProvider(LlmProviderId("openai_compatible"), "OpenAI Compatible", true)
val ANTHROPIC_PROVIDER: LlmProvider = LlmProvider(LlmProviderId("anthropic"), "Anthropic")
val MISTRAL_PROVIDER: LlmProvider = LlmProvider(LlmProviderId("mistral"), "Mistral")
val OLLAMA_PROVIDER: LlmProvider = LlmProvider(LlmProviderId("ollama"), "Ollama")
val AI_ASSISTANT_PROVIDER: LlmProvider = LlmProvider(LlmProviderId("aiassistant"), "AI Assistant", predefined = true)
val DEEPSEEK_PROVIDER: LlmProvider = LlmProvider(LlmProviderId("deepseek"), "Deepseek")
val GEMINI_PROVIDER: LlmProvider = LlmProvider(LlmProviderId("gemini"), "Gemini")
val OPEN_ROUTER_PROVIDER: LlmProvider = LlmProvider(LlmProviderId("open_router"), "OpenRouter")


@JvmInline
value class LlmProviderInstanceId(val id: String = UUID.randomUUID().toString())

data class LlmProviderInstance(
  val id: LlmProviderInstanceId = LlmProviderInstanceId(),
  val provider: LlmProvider,
  val settings: LlmProviderSettings<*>,
)

@JvmInline
value class LlmModelId(val id: String)

/**
 * Data class representing a model supported by an LLM provider
 */
data class LlmModel(
  val id: LlmModelId,
  val provider: LlmProvider,
  @NlsSafe val displayName: String,
  val capabilities: Set<ModelCapability> = emptySet(),
  val contextWindow: Int? = null,
  val maxTokens: Int? = null,
  val isDefault: Boolean = false,
  val supportedParameters: Set<ModelParameter> = ALL_PARAMETERS_SUPPORTED,
)

/**
 * Enum representing capabilities of an LLM model
 */
enum class ModelCapability {
  CHAT,
  COMPLETION,
  EMBEDDING,
  CODE_GENERATION,
  FUNCTION_CALLING,
  IMAGE_GENERATION,
}

enum class ModelParameter {
  MAX_TOKENS,
  TOP_P,
  TEMPERATURE
}

val ALL_PARAMETERS_SUPPORTED: Set<ModelParameter> = ModelParameter.entries.toSet()

/**
 * Data class for configuring a specific chat request
 */
data class ChatRequestConfig(
  val temperature: Double? = null,
  val maxTokens: Int? = null,
  val topP: Double? = null,
  val presencePenalty: Double? = null,
  val frequencyPenalty: Double? = null,
  val stopSequences: List<String>? = null,
  val timeout: Long? = null,
  // Additional provider-specific parameters can be added as a map
  val additionalParams: Map<String, Any>? = null,
) {
  fun supportedParametersFiltered(model: LlmModel): ChatRequestConfig {
    val newTemperature = temperature?.let { if (model.supportedParameters.contains(ModelParameter.TEMPERATURE)) it else null }
    val newMaxTokens = maxTokens?.let { if (model.supportedParameters.contains(ModelParameter.MAX_TOKENS)) it else null }
    val newTopP = topP?.let { if (model.supportedParameters.contains(ModelParameter.TOP_P)) it else null }
    return ChatRequestConfig(newTemperature, newMaxTokens, newTopP, presencePenalty, frequencyPenalty, stopSequences, timeout, additionalParams)
  }

  companion object {
    fun createDefault(temperature: Double = 0.7, timeoutSeconds: Long = 60): ChatRequestConfig {
      return ChatRequestConfig(
        temperature = temperature,
        timeout = timeoutSeconds * 1000
      )
    }
  }
}
