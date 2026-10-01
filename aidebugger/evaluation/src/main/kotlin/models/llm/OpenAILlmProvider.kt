
package com.intellij.aidebugger.evaluation.models.llm

import com.openai.client.OpenAIClient
import com.openai.client.okhttp.OpenAIOkHttpClient
import com.openai.models.chat.completions.StructuredChatCompletionCreateParams
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class OpenAiLlmProvider<T : Any>(
    private val client: OpenAIClient,
    private val modelName: String,
    private val temperature: Double?,
    private val responseClass: Class<T>
) : LlmProvider<T> {

    @Suppress("UNCHECKED_CAST")
    override suspend fun complete(prompt: String): T = withContext(Dispatchers.IO) {
        val builder = StructuredChatCompletionCreateParams.builder<Any>()
            .model(modelName)
            .addUserMessage(prompt)
            .apply { temperature?.let { temperature(it) } }
            .responseFormat(responseClass as Class<Any>)

        val params = builder.build()
        val completion = client.chat().completions().create(params)

        val message = completion.choices().firstOrNull()?.message()
            ?: throw IllegalStateException("OpenAI returned no choices")

        message.content().orElseThrow {
            IllegalStateException("OpenAI returned empty content")
        } as T
    }

    companion object {
        inline fun <reified T : Any> create(
            config: LlmProviderConfig
        ): OpenAiLlmProvider<T> {
            val client = OpenAIOkHttpClient.builder()
                .apiKey(config.apiKey)
                .timeout(config.timeout)
                .build()

            return OpenAiLlmProvider(
                client = client,
                modelName = config.modelName,
                temperature = config.temperature,
                responseClass = T::class.java
            )
        }
    }
}