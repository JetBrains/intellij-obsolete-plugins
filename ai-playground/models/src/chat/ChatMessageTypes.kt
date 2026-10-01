package com.intellij.aiplayground.models.chat

import com.intellij.aiplayground.models.ChatRequestConfig
import com.intellij.aiplayground.models.LlmModelId
import com.intellij.aiplayground.models.LlmProviderId
import com.intellij.aiplayground.models.LlmProviderInstanceId
import com.intellij.openapi.util.NlsSafe
import java.util.UUID

@JvmInline
value class ChatMessageId(val id: String = UUID.randomUUID().toString())

/**
 * Sealed class representing different types of chat messages
 */
sealed interface ChatMessage {
  val id: ChatMessageId
  val timestamp: Long
  val content: String
  fun updateText(text: String): ChatMessage
}

/**
 * Represents a message from the user
 */
data class UserMessage(
  override val id: ChatMessageId = ChatMessageId(),
  override val content: String,
  override val timestamp: Long = System.currentTimeMillis(),
) : ChatMessage {
  override fun updateText(text: String): ChatMessage {
    return copy(content = text, timestamp = System.currentTimeMillis())
  }
}

/**
 * Represents a response from the assistant
 */
data class AssistantMessage(
  override val id: ChatMessageId,
  override val content: String,
  val tokenUsage: TokenUsage? = null,
  val isStreaming: Boolean = false,
  val isError: Boolean = false,
  val model: ChatModelLink,
  override val timestamp: Long = System.currentTimeMillis(),
) : ChatMessage {
  override fun updateText(text: String): ChatMessage {
    return copy(content = text, timestamp = System.currentTimeMillis())
  }
}

/**
 * Represents a system message in the chat
 */
data class SystemMessage(
  override val id: ChatMessageId,
  override val content: String,
  override val timestamp: Long = System.currentTimeMillis(),
) : ChatMessage {
  override fun updateText(text: String): ChatMessage {
    return copy(content = text, timestamp = System.currentTimeMillis())
  }
}

sealed interface ChatResponseEvent {
  data class TokenUsageResponseEvent(val tokenUsage: TokenUsage) : ChatResponseEvent
  data class PartialContentResponseEvent(val partialContent: String) : ChatResponseEvent
}

/**
 * Data class for token usage information
 */
data class TokenUsage(
  val inputTokenCount: Int,
  val outputTokenCount: Int,
  val totalTokenCount: Int,
)

@JvmInline
value class ChatId(val id: String = UUID.randomUUID().toString())

/**
 * Represents a chat session with its messages and metadata
 */
data class Chat(
  val id: ChatId = ChatId(),
  @NlsSafe val title: String? = null,
  val systemPrompt: String = "",
  val messages: List<ChatMessage> = emptyList(),
  val createdAt: Long = System.currentTimeMillis(),
  val updatedAt: Long = System.currentTimeMillis(),
  val activeModels: List<ChatModelLink> = emptyList(), // Provider ID and Model ID pairs
  val isStreaming: Boolean = false,
) {

  /**
   * Extract a summary from the chat messages to use as a title
   * if one hasn't been explicitly set
   */
  fun generateSummaryTitle(): String? {
    val firstUserMessage = messages.filterIsInstance<UserMessage>().firstOrNull()
    return firstUserMessage?.let {
      // Take first 30 chars of first user message as a title
      val summaryText = it.content.take(30).trim()
      if (summaryText.length < it.content.length) "$summaryText..." else summaryText
    }
  }
}

@JvmInline
value class ChatModelLinkId(val id: String = UUID.randomUUID().toString())

data class ChatModelLink(
  val id: ChatModelLinkId = ChatModelLinkId(),
  val providerId: LlmProviderId,
  val instanceId: LlmProviderInstanceId,
  val modelId: LlmModelId,
  val parameters: ChatRequestConfig = ChatRequestConfig.createDefault(),
  val show: Boolean = true,
)