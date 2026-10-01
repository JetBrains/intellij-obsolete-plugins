package com.intellij.aiplayground.gemini

import com.intellij.aiplayground.models.ChatRequestConfig
import com.intellij.aiplayground.models.GEMINI_PROVIDER
import com.intellij.aiplayground.models.LlmModel
import com.intellij.aiplayground.models.LlmModelId
import com.intellij.aiplayground.models.LlmService
import com.intellij.aiplayground.models.ModelCapability
import com.intellij.aiplayground.models.chat.AssistantMessage
import com.intellij.aiplayground.models.chat.ChatMessage
import com.intellij.aiplayground.models.chat.ChatResponseEvent
import com.intellij.aiplayground.models.chat.UserMessage
import com.intellij.aiplayground.models.extension.BaseApiKeyLlmServiceExtension
import com.intellij.aiplayground.models.settings.LangChainProviderSettings
import com.intellij.aiplayground.models.settings.LlmProviderSettings
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import dev.langchain4j.data.message.AiMessage
import dev.langchain4j.data.message.SystemMessage
import dev.langchain4j.model.chat.StreamingChatLanguageModel
import dev.langchain4j.model.chat.request.ChatRequest
import dev.langchain4j.model.chat.response.ChatResponse
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler
import dev.langchain4j.model.googleai.GoogleAiGeminiStreamingChatModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class LangChainGeminiService : LlmService {

  private val apiImpl = GeminiClientAPIImpl()

  override suspend fun getSupportedModels(
    project: Project,
    providerSettings: LlmProviderSettings<*>
  ): List<LlmModel> {
    providerSettings as LangChainProviderSettings
    val apiKey = providerSettings.apiKey ?: return emptyList()

    try {
      return apiImpl.listModels(apiKey).map {
        LlmModel(
          id = it.id,
          provider = GEMINI_PROVIDER,
          displayName = it.id.id,
          capabilities = setOf(
            ModelCapability.CHAT,
            ModelCapability.COMPLETION,
            ModelCapability.CODE_GENERATION,
            ModelCapability.FUNCTION_CALLING
          ),
          contextWindow = 200_000
        )
      }
    }
    catch (t: Throwable) {
      thisLogger().warn("Failed to fetch Gemini models: ${t.message}")
      return emptyList()
    }
  }

  override suspend fun streamingComplete(
    project: Project,
    prompt: String,
    systemPrompt: String?,
    model: LlmModel,
    providerSettings: LlmProviderSettings<*>,
    requestConfig: ChatRequestConfig,
    messages: List<ChatMessage>?,
  ): Flow<com.intellij.aiplayground.models.chat.ChatResponseEvent> {
    val streamingModel = createStreamingModel(model, providerSettings, requestConfig)

    val langChainMessages = buildLangChainMessages(prompt, systemPrompt, messages)
    val chatRequest = createChatRequest(langChainMessages)

    return channelFlow {
      suspendCancellableCoroutine { continuation ->
        streamingModel.chat(chatRequest, object : StreamingChatResponseHandler {
          override fun onPartialResponse(token: String) {
            trySend(ChatResponseEvent.PartialContentResponseEvent(token))
          }

          override fun onCompleteResponse(response: ChatResponse) {
            continuation.resume(Unit)
          }

          override fun onError(error: Throwable) {
            continuation.resumeWithException(error)
          }
        })
      }
    }
      .flowOn(Dispatchers.IO)
  }

  fun createChatRequest(
    messages: List<dev.langchain4j.data.message.ChatMessage>,
  ): ChatRequest {
    return ChatRequest.builder()
      .messages(messages)
      .build()
  }

  override fun getDefaultModelName(): LlmModelId {
    return DEFAULT_MODEL_NAME
  }

  fun createStreamingModel(model: LlmModel, providerSettings: LlmProviderSettings<*>, requestConfig: ChatRequestConfig): StreamingChatLanguageModel {
    providerSettings as LangChainProviderSettings
    val filteredRequestConfig = requestConfig.supportedParametersFiltered(model)
    return GoogleAiGeminiStreamingChatModel.builder()
      .apiKey(providerSettings.apiKey)
      .modelName(model.id.id.substringAfter("/"))
      .temperature(filteredRequestConfig.temperature)
      .topP(filteredRequestConfig.topP)
      .maxOutputTokens(filteredRequestConfig.maxTokens)
      .build()
  }

  override suspend fun testConnection(project: Project, providerSettings: LlmProviderSettings<*>) {
    streamingComplete(
      project,
      "test",
      "",
      getSupportedModels(project, providerSettings).first { it.id == DEFAULT_MODEL_NAME },
      providerSettings,
      ChatRequestConfig.createDefault(),
      emptyList()
    ).collect()
  }

  companion object {
    private val DEFAULT_MODEL_NAME = LlmModelId("models/gemini-2.5-flash")
  }

  class Extension : BaseApiKeyLlmServiceExtension(GEMINI_PROVIDER) {

    override fun createService(): LangChainGeminiService = LangChainGeminiService()
  }

  protected fun buildLangChainMessages(
    prompt: String,
    systemPrompt: String?,
    messages: List<ChatMessage>?,
  ): List<dev.langchain4j.data.message.ChatMessage> {
    return buildList {
      // Add system message if provided
      systemPrompt?.takeIf { it.isNotBlank() }?.let {
        add(SystemMessage(it))
      }

      if (messages.isNullOrEmpty()) {
        // If no messages provided, add the prompt as a user message
        add(dev.langchain4j.data.message.UserMessage(prompt))
        return@buildList
      }

      // Process messages to ensure proper alternation
      var lastRole: String? = null
      var index = 0

      while (index < messages.size) {
        when (val message = messages[index]) {
          is UserMessage -> {
            add(dev.langchain4j.data.message.UserMessage(message.content))
            lastRole = "user"
            index++
          }
          is AssistantMessage -> {
            // Skip if we already added an assistant message
            if (lastRole == "assistant") {
              index++
              continue
            }

            // Find all consecutive assistant messages
            val consecutiveAssistantMessages = messages.subList(index, messages.size)
              .takeWhile { it is AssistantMessage }
              .filterIsInstance<AssistantMessage>()

            // Select the appropriate assistant message (matching model ID or first one)
            consecutiveAssistantMessages.firstOrNull()?.let { firstMessage ->
              add(AiMessage(firstMessage.content))
              lastRole = "assistant"
            }

            // Skip past all consecutive assistant messages
            index += consecutiveAssistantMessages.size
          }
          is com.intellij.aiplayground.models.chat.SystemMessage -> {
            add(SystemMessage(message.content))
            index++
          }
          else -> index++ // Skip unsupported message types
        }
      }

      // Ensure the last message is from a user
      if (isNotEmpty() && lastRole == "assistant") {
        removeLast()
      }
    }
  }
}
