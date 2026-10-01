package com.intellij.aiplayground.models.settings

/**
 * Functional interface for retrieving API keys
 */
fun interface ApiKeyProvider {
  /**
   * Retrieves the API key
   */
  fun getApiKey(): String?
}

interface LlmProviderSettings<S : LlmProviderSettings<S>> {
  val displayName: String?
  val enabled: Boolean
  val filterModelsByAge: Long?
    get() = 365 * 24 * 60 * 60 * 1000L

  fun updateName(name: String): S
  fun updateEnabled(enabled: Boolean): S
}

data class LangChainProviderSettings(
  override val displayName: String?,
  override val enabled: Boolean,
  val apiKeyProvider: ApiKeyProvider,
) : LlmProviderSettings<LangChainProviderSettings> {
  override fun updateName(name: String): LangChainProviderSettings = copy(displayName = name)
  override fun updateEnabled(enabled: Boolean): LangChainProviderSettings = copy(enabled = enabled)
  
  /**
   * For backward compatibility and easier migration
   */
  val apiKey: String?
    get() = apiKeyProvider.getApiKey()
}

data class OpenAIProviderSettings(
  override val displayName: String?,
  override val enabled: Boolean,
  val apiKeyProvider: ApiKeyProvider,
  val endpoint: String? = null,
) : LlmProviderSettings<OpenAIProviderSettings> {
  override fun updateName(name: String): OpenAIProviderSettings = copy(displayName = name)
  override fun updateEnabled(enabled: Boolean): OpenAIProviderSettings = copy(enabled = enabled)
  
  /**
   * For backward compatibility and easier migration
   */
  val apiKey: String?
    get() = apiKeyProvider.getApiKey()
}

data class OllamaProviderSettings(
  override val displayName: String?,
  override val enabled: Boolean,
  val endpoint: String?,
) : LlmProviderSettings<OllamaProviderSettings> {
  override fun updateName(name: String): OllamaProviderSettings = copy(displayName = name)
  override fun updateEnabled(enabled: Boolean): OllamaProviderSettings = copy(enabled = enabled)
}

data class AIAssistantProviderSettings(
  override val displayName: String?,
  override val enabled: Boolean,
) : LlmProviderSettings<AIAssistantProviderSettings> {
  override fun updateName(name: String): AIAssistantProviderSettings = copy(displayName = name)
  override fun updateEnabled(enabled: Boolean): AIAssistantProviderSettings = copy(enabled = enabled)
}