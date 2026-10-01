package com.intellij.aiplayground.models

/** Utilities for common model mapping tasks across providers */
object ModelUtils {
  /**
   * Maps a list of model IDs to LlmModel with a given provider and capabilities.
   */
  fun mapModelIdsToLlmModels(
    modelIds: List<LlmModelId>,
    provider: LlmProvider,
    capabilities: Set<ModelCapability>,
    contextWindow: Int? = null,
    maxTokens: Int? = null,
  ): List<LlmModel> = modelIds.map { id ->
    LlmModel(
      id = id,
      provider = provider,
      displayName = id.id,
      capabilities = capabilities,
      contextWindow = contextWindow,
      maxTokens = maxTokens,
    )
  }
}
