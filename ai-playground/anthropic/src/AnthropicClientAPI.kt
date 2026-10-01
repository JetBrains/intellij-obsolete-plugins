package com.intellij.aiplayground.anthropic

import com.intellij.aiplayground.models.LlmModelId

data class AnthropicModel(val id: LlmModelId)

interface AnthropicClientAPI {
  suspend fun listModels(apiKey: String, filterByCreatedAt: Long?): List<AnthropicModel>
}