package com.intellij.aidebugger.evaluation.settings.models

import com.intellij.openapi.util.NlsContexts

/**
 * Represents an LLM model available from a provider
 */
data class LlmModel(
  val id: String,
  @NlsContexts.Label val displayName: String = id,
  val providerType: com.intellij.aidebugger.evaluation.models.llm.LlmProviderType
)
