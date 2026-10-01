package com.intellij.aiplayground.deepseek

import com.intellij.aiplayground.models.LlmModelId
import com.intellij.aiplayground.models.chat.ChatResponseEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

data class DeepseekModel(val id: LlmModelId)

@Serializable
data class DeepseekMessage(
  val role: String, // "system", "user", "assistant", "tool"
  val content: String? = null,
  val name: String? = null,
  @SerialName("tool_call_id") val toolCallId: String? = null,
  val prefix: Boolean? = null,
  @SerialName("reasoning_content") val reasoningContent: String? = null
)

interface DeepseekClientAPI {
  suspend fun listModels(apiKey: String): List<DeepseekModel>

  suspend fun chatCompletion(apiKey: String, model: DeepseekModel, prompt: String): String

  suspend fun streamingChatCompletion(apiKey: String, model: DeepseekModel, messages: List<DeepseekMessage>, temperature: Double?, topP: Double, maxTokens: Int?): Flow<ChatResponseEvent>
}
