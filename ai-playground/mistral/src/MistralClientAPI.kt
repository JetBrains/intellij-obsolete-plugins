package com.intellij.aiplayground.mistral

import com.intellij.aiplayground.models.LlmModelId
import com.intellij.aiplayground.models.chat.ChatResponseEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

data class MistralModel(val id: LlmModelId)

@Serializable
data class MistralMessage(
  val role: String,
  val content: String
)

interface MistralClientAPI {
  suspend fun listModels(apiKey: String): List<MistralModel>

  suspend fun chatCompletion(apiKey: String, model: MistralModel, prompt: String): String

  suspend fun streamingChatCompletion(apiKey: String, model: MistralModel, messages: List<MistralMessage>, temperature: Double?, topP: Double, maxTokens: Int?): Flow<ChatResponseEvent>
}
