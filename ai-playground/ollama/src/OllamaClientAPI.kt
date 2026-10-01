package com.intellij.aiplayground.ollama

import com.intellij.aiplayground.models.chat.ChatMessage
import com.intellij.aiplayground.models.chat.ChatResponseEvent
import kotlinx.coroutines.flow.Flow

// https://github.com/ollama/ollama/blob/main/docs/api.md
interface OllamaClientAPI {
  suspend fun testConnection(baseUrl: String): Boolean
  suspend fun listLocalModels(baseUrl: String): List<OllamaModel>
  suspend fun listRunningModels(baseUrl: String): List<OllamaModel>

  suspend fun chatCompletion(
    baseUrl: String,
    model: OllamaModel,
    messages: List<ChatMessage>,
    options: OllamaOptions? = null,
  ): Flow<ChatResponseEvent>
}