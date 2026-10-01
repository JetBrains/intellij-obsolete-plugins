package com.intellij.aiplayground.openai

import com.intellij.aiplayground.models.LlmModelId

data class OpenAiModel(val id: LlmModelId, val supportsParameters: Boolean = true)

interface OpenAiClientAPI {
  suspend fun listModels(baseUrl: String, apiKey: String, filterByCreatedAt: Long?): List<OpenAiModel>
}
