package com.intellij.aiplayground.langchain

import com.intellij.aiplayground.models.ChatRequestConfig
import com.intellij.aiplayground.models.LlmModel
import com.intellij.aiplayground.models.LlmService
import com.intellij.aiplayground.models.chat.AssistantMessage
import com.intellij.aiplayground.models.chat.ChatMessage
import com.intellij.aiplayground.models.chat.ChatResponseEvent
import com.intellij.aiplayground.models.chat.ChatResponseEvent.PartialContentResponseEvent
import com.intellij.aiplayground.models.chat.ChatResponseEvent.TokenUsageResponseEvent
import com.intellij.aiplayground.models.chat.TokenUsage
import com.intellij.aiplayground.models.chat.UserMessage
import com.intellij.aiplayground.models.settings.LlmProviderSettings
import com.intellij.openapi.project.Project
import dev.langchain4j.data.message.AiMessage
import dev.langchain4j.data.message.SystemMessage
import dev.langchain4j.model.chat.StreamingChatModel
import dev.langchain4j.model.chat.request.ChatRequest
import dev.langchain4j.model.chat.response.ChatResponse
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

abstract class BaseLangChainService : LlmService {
  /**
   * Get the streaming chat model instance
   */
  protected abstract fun createStreamingModel(
    model: LlmModel,
    providerSettings: LlmProviderSettings<*>,
    requestConfig: ChatRequestConfig,
  ): StreamingChatModel

  override suspend fun streamingComplete(
    project: Project,
    prompt: String,
    systemPrompt: String?,
    model: LlmModel,
    providerSettings: LlmProviderSettings<*>,
    requestConfig: ChatRequestConfig,
    messages: List<ChatMessage>?,
  ): Flow<ChatResponseEvent> {
    val streamingModel = createStreamingModel(model, providerSettings, requestConfig)

    val langChainMessages = buildLangChainMessages(prompt, systemPrompt, messages)
    val chatRequest = createChatRequest(langChainMessages)

    return channelFlow {
      suspendCancellableCoroutine { continuation ->
        streamingModel.chat(chatRequest, object : StreamingChatResponseHandler {
          override fun onPartialResponse(token: String) {
            trySend(PartialContentResponseEvent(token))
          }

          override fun onCompleteResponse(response: ChatResponse) {
            response.tokenUsage()?.let { tokenUsage ->
              val usage = TokenUsage(
                tokenUsage.inputTokenCount() ?: -1,
                tokenUsage.outputTokenCount() ?: -1,
                tokenUsage.totalTokenCount() ?: -1
              )
              trySend(TokenUsageResponseEvent(usage))
            }
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

  /**
   * Create a chat request with the given parameters
   */
  protected fun createChatRequest(
    messages: List<dev.langchain4j.data.message.ChatMessage>,
  ): ChatRequest {
    return ChatRequest.builder()
      .messages(messages)
      .build()
  }

  /**
   * Convert chat messages to LangChain format
   */
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
