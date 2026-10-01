package com.intellij.aidebugger.evaluation.settings

import com.intellij.aidebugger.evaluation.models.llm.LlmProviderType
import com.intellij.aidebugger.evaluation.settings.models.LlmModel
import com.intellij.aidebugger.evaluation.settings.models.ProviderInstance
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Service for fetching available models from LLM providers.
 * Handles API calls to retrieve model lists for different provider types.
 */
@Service(Service.Level.APP)
class AIToolkitModelsService {

    companion object {
        fun getInstance(): AIToolkitModelsService = service()
    }

    /**
     * Fetch available models for a provider instance.
     * Returns a list of filtered models (text/VLM only).
     */
    suspend fun fetchModels(instance: ProviderInstance): List<LlmModel> {
        if (instance.apiKey.isBlank()) return emptyList()

        return withContext(Dispatchers.IO) {
            when (instance.providerType) {
                LlmProviderType.OPENAI, LlmProviderType.OPENAI_COMPATIBLE -> fetchOpenAIModels(instance)
                LlmProviderType.ANTHROPIC -> fetchAnthropicModels(instance)
                LlmProviderType.GEMINI -> fetchGeminiModels(instance)
            }
        }
    }

    /**
     * Test connection to a provider and return success/failure.
     */
    suspend fun testConnection(instance: ProviderInstance): Boolean {
        if (instance.apiKey.isBlank()) return false

        return withContext(Dispatchers.IO) {
            runCatching {
                when (instance.providerType) {
                    LlmProviderType.OPENAI, LlmProviderType.OPENAI_COMPATIBLE -> testOpenAIConnection(instance)
                    LlmProviderType.ANTHROPIC -> testAnthropicConnection(instance)
                    LlmProviderType.GEMINI -> testGeminiConnection(instance)
                }
                true
            }.getOrElse { false }
        }
    }

    private fun fetchOpenAIModels(instance: ProviderInstance): List<LlmModel> {
        val client = com.openai.client.okhttp.OpenAIOkHttpClient.builder()
            .apiKey(instance.apiKey)
            .apply {
                if (instance.baseUrl.isNotBlank()) {
                    baseUrl(instance.baseUrl)
                }
            }
            .build()

        val response = client.models().list()
        return response.data()
            .filter { model -> isTextOrVlmModel(model.id(), instance.providerType) }
            .map { model ->
                LlmModel(
                    id = model.id(),
                    displayName = model.id(),
                    providerType = instance.providerType
                )
            }
            .sortedWith(compareBy { model ->
                when {
                    model.id.startsWith("g", ignoreCase = true) -> "a${model.id.lowercase()}"
                    else -> "z${model.id.lowercase()}"
                }
            })
    }

    private fun fetchAnthropicModels(instance: ProviderInstance): List<LlmModel> {
        // Anthropic doesn't have a models list API, return hardcoded list
        return listOf(
            LlmModel("claude-3-5-sonnet-20241022", providerType = LlmProviderType.ANTHROPIC),
            LlmModel("claude-3-5-haiku-20241022", providerType = LlmProviderType.ANTHROPIC),
            LlmModel("claude-3-opus-20240229", providerType = LlmProviderType.ANTHROPIC),
            LlmModel("claude-3-sonnet-20240229", providerType = LlmProviderType.ANTHROPIC),
            LlmModel("claude-3-haiku-20240307", providerType = LlmProviderType.ANTHROPIC)
        ).sortedWith(compareBy { model ->
            when {
                model.id.startsWith("c", ignoreCase = true) -> "a${model.id.lowercase()}"
                else -> "z${model.id.lowercase()}"
            }
        })
    }

    private fun fetchGeminiModels(instance: ProviderInstance): List<LlmModel> {
        val httpClient = okhttp3.OkHttpClient.Builder()
            .connectTimeout(java.time.Duration.ofSeconds(10))
            .build()

        val url = "https://generativelanguage.googleapis.com/v1beta/models?key=${instance.apiKey}"
        val request = okhttp3.Request.Builder()
            .url(url)
            .get()
            .build()

        val response = httpClient.newCall(request).execute()
        val body = response.body?.string() ?: return emptyList()

        val gson = com.google.gson.Gson()
        val jsonResponse = gson.fromJson(body, com.google.gson.JsonObject::class.java)
        val models = jsonResponse.getAsJsonArray("models") ?: return emptyList()

        return models.mapNotNull { element ->
            val modelObj = element.asJsonObject
            val name = modelObj.get("name")?.asString ?: return@mapNotNull null
            val modelId = name.substringAfterLast("/")

            val supportedMethods = modelObj.getAsJsonArray("supportedGenerationMethods")
            val supportsGeneration = supportedMethods?.any {
                it.asString == "generateContent"
            } ?: false

            if (!supportsGeneration) {
                return@mapNotNull null
            }

            LlmModel(
                id = modelId,
                displayName = modelObj.get("displayName")?.asString ?: modelId,
                providerType = LlmProviderType.GEMINI
            )
        }.sortedWith(compareBy { model ->
            when {
                model.id.startsWith("g", ignoreCase = true) -> "a${model.id.lowercase()}"
                else -> "z${model.id.lowercase()}"
            }
        })
    }

    private fun testOpenAIConnection(instance: ProviderInstance) {
        val client = com.openai.client.okhttp.OpenAIOkHttpClient.builder()
            .apiKey(instance.apiKey)
            .apply {
                if (instance.baseUrl.isNotBlank()) {
                    baseUrl(instance.baseUrl)
                }
            }
            .build()
        client.models().list()
    }

    private fun testAnthropicConnection(instance: ProviderInstance) {
        val client = com.anthropic.client.okhttp.AnthropicOkHttpClient.builder()
            .apiKey(instance.apiKey)
            .build()
        client.messages().create(
            com.anthropic.models.messages.MessageCreateParams.builder()
                .model("claude-3-5-sonnet-20241022")
                .maxTokens(1)
                .addUserMessage("test")
                .build()
        )
    }

    private fun testGeminiConnection(instance: ProviderInstance) {
        val httpClient = okhttp3.OkHttpClient.Builder()
            .connectTimeout(java.time.Duration.ofSeconds(10))
            .build()

        val url = "https://generativelanguage.googleapis.com/v1beta/models?key=${instance.apiKey}"
        val request = okhttp3.Request.Builder()
            .url(url)
            .get()
            .build()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IllegalStateException("Gemini API test failed: ${response.code}")
            }
        }
    }

    private fun isTextOrVlmModel(modelId: String, providerType: LlmProviderType): Boolean {
        return when (providerType) {
            LlmProviderType.OPENAI, LlmProviderType.OPENAI_COMPATIBLE -> {
                when {
                    modelId.startsWith("dall-e") -> false
                    modelId.startsWith("tts-") -> false
                    modelId.startsWith("whisper-") -> false
                    modelId.contains("embedding") -> false
                    modelId.contains("moderation") -> false
                    modelId.startsWith("text-") && !modelId.startsWith("text-embedding") -> false
                    modelId.startsWith("babbage") -> false
                    modelId.startsWith("davinci") -> false
                    modelId.startsWith("curie") -> false
                    modelId.startsWith("ada") -> false
                    modelId.startsWith("gpt-3.5-turbo-instruct") -> false
                    modelId.matches(Regex("gpt-3\\.5-turbo-\\d{4}")) -> false
                    else -> modelId.startsWith("gpt-") || modelId.startsWith("o1") || modelId.startsWith("o3")
                }
            }
            LlmProviderType.GEMINI -> {
                when {
                    modelId.contains("embedding") -> false
                    modelId.contains("aqa") -> false
                    modelId.startsWith("gemini-pro-vision") -> false
                    else -> modelId.startsWith("gemini-")
                }
            }
            LlmProviderType.ANTHROPIC -> true
        }
    }
}
