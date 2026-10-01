package com.intellij.aiplayground.gemini

import com.intellij.aiplayground.models.LlmModelId
import com.intellij.aiplayground.models.chat.ChatResponseEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

data class GeminiModel(val id: LlmModelId)

@Serializable
data class GeminiMessage(
  val role: String,          // "user", "model", "system"
  val content: String
)

interface GeminiClientAPI {
  suspend fun listModels(apiKey: String): List<GeminiModel>
  suspend fun chatCompletion(apiKey: String, model: GeminiModel, prompt: String): String
  suspend fun streamingChatCompletion(
    apiKey: String,
    model: GeminiModel,
    messages: List<GeminiMessage>,
    temperature: Double?,
    topP: Double,
    maxTokens: Int?,
  ): Flow<ChatResponseEvent>
}