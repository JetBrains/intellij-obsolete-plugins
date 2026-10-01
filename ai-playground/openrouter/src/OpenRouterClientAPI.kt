package com.intellij.aiplayground.openrouter

import com.intellij.aiplayground.models.LlmModelId
import com.intellij.aiplayground.models.chat.ChatResponseEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

data class OpenRouterModel(val id: LlmModelId)

@Serializable
data class OpenRouterMessage(
  val role: String,
  val content: String
)

interface OpenRouterClientAPI {
  suspend fun listModels(apiKey: String): List<OpenRouterModel>

  suspend fun streamingChatCompletion(apiKey: String, model: OpenRouterModel, messages: List<OpenRouterMessage>, temperature: Double?, topP: Double, maxTokens: Int?): Flow<ChatResponseEvent>
}
