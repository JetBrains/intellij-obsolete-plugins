package com.intellij.aidebugger.evaluation.models.llm

import com.anthropic.client.AnthropicClient
import com.anthropic.client.okhttp.AnthropicOkHttpClient
import com.anthropic.models.messages.MessageCreateParams
import com.anthropic.models.messages.TextBlock
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AnthropicLlmProvider<T : Any>(
    private val client: AnthropicClient,
    private val modelName: String,
    private val temperature: Double?,
    private val responseClass: Class<T>,
    private val gson: Gson = Gson()
) : LlmProvider<T> {

    @Suppress("UNCHECKED_CAST")
    override suspend fun complete(prompt: String): T = withContext(Dispatchers.IO) {
        val paramsBuilder = MessageCreateParams.builder()
            .model(modelName)
            .maxTokens(4096L)
            .addUserMessage(prompt)

        temperature?.let { paramsBuilder.temperature(it) }

        val params = paramsBuilder.build()
        val message = client.messages().create(params)

        val textBlock = message.content().firstOrNull() as? TextBlock
            ?: throw IllegalStateException("Anthropic returned no text content")

        val content = textBlock.text()

        if (responseClass == String::class.java) {
            content as T
        } else {
            gson.fromJson(content, responseClass)
        }
    }

    companion object {
        inline fun <reified T : Any> create(
            config: LlmProviderConfig
        ): AnthropicLlmProvider<T> {
            val client = AnthropicOkHttpClient.builder()
                .apiKey(config.apiKey)
                .timeout(config.timeout)
                .build()

            return AnthropicLlmProvider(
                client = client,
                modelName = config.modelName,
                temperature = config.temperature,
                responseClass = T::class.java
            )
        }
    }
}