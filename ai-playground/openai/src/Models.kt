package com.intellij.aiplayground.openai

import com.intellij.aiplayground.models.LlmModel
import com.intellij.aiplayground.models.LlmModelId
import com.intellij.aiplayground.models.ModelCapability
import com.intellij.aiplayground.models.OPENAI_PROVIDER

internal val models by lazy {
  listOf(
    LlmModel(
      id = LlmModelId("gpt-5"),
      provider = OPENAI_PROVIDER,
      displayName = "GPT-5",
      capabilities = setOf(
        ModelCapability.CHAT,
        ModelCapability.COMPLETION,
        ModelCapability.CODE_GENERATION,
        ModelCapability.FUNCTION_CALLING
      ),
      contextWindow = 128000
    ),
    LlmModel(
      id = LlmModelId("gpt-5-mini"),
      provider = OPENAI_PROVIDER,
      displayName = "GPT-5 Mini",
      capabilities = setOf(
        ModelCapability.CHAT,
        ModelCapability.COMPLETION,
        ModelCapability.CODE_GENERATION,
        ModelCapability.FUNCTION_CALLING
      ),
      contextWindow = 128000,
      isDefault = true,
    ),
    LlmModel(
      id = LlmModelId("gpt-5-nano"),
      provider = OPENAI_PROVIDER,
      displayName = "GPT-5 Nano",
      capabilities = setOf(
        ModelCapability.CHAT,
        ModelCapability.COMPLETION,
        ModelCapability.CODE_GENERATION,
        ModelCapability.FUNCTION_CALLING
      ),
      contextWindow = 128000
    ),
    LlmModel(
      id = LlmModelId("gpt-4.1"),
      provider = OPENAI_PROVIDER,
      displayName = "GPT-4.1",
      capabilities = setOf(
        ModelCapability.CHAT,
        ModelCapability.COMPLETION,
        ModelCapability.CODE_GENERATION,
        ModelCapability.FUNCTION_CALLING
      ),
      contextWindow = 128000
    ),
    LlmModel(
      id = LlmModelId("gpt-4.1-mini"),
      provider = OPENAI_PROVIDER,
      displayName = "GPT-4.1 Mini",
      capabilities = setOf(
        ModelCapability.CHAT,
        ModelCapability.COMPLETION,
        ModelCapability.CODE_GENERATION,
        ModelCapability.FUNCTION_CALLING
      ),
      contextWindow = 128000
    ),
    LlmModel(
      id = LlmModelId("o3"),
      provider = OPENAI_PROVIDER,
      displayName = "O3",
      capabilities = setOf(
        ModelCapability.CHAT,
        ModelCapability.COMPLETION,
        ModelCapability.CODE_GENERATION,
        ModelCapability.FUNCTION_CALLING
      ),
      contextWindow = 128000,
      supportedParameters = emptySet()
    ),
    LlmModel(
      id = LlmModelId("o3-mini"),
      provider = OPENAI_PROVIDER,
      displayName = "O3 Mini",
      capabilities = setOf(
        ModelCapability.CHAT,
        ModelCapability.COMPLETION,
        ModelCapability.CODE_GENERATION,
        ModelCapability.FUNCTION_CALLING
      ),
      contextWindow = 128000
    ),
    )
}