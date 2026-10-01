package com.intellij.aiplayground.ollama.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * Data class representing the request body for generating text completions using OpenAI-compatible REST API.
 *
 * @property model The model to use for generating text, e.g., "text-davinci-003".
 * @property prompt The input prompt to generate a completion for.
 * @property maxTokens The maximum number of tokens to generate. Default is null.
 * @property temperature Sampling temperature. Higher values make the output more random. Default is null.
 * @property topP Nucleus sampling, where the model considers the top `topP` probability mass. Default is null.
 * @property numberOfCompletions The number of completions to generate for the given prompt. Default is null.
 * @property stream Whether to stream the response (i.e., receive tokens as they are generated). Default is null.
 * @property logprobs Number of top tokens to log probabilities for. Default is null.
 * @property stop A list of tokens at which to stop generating further text. Default is null.
 * @property presencePenalty A penalty for new tokens based on whether they appear in the text already. Default is null.
 * @property frequencyPenalty A penalty for new tokens based on their frequency in the text. Default is null.
 * @property bestOf The number of completions to generate and choose the best one from. Default is null.
 * @property logitBias A map of token bias adjustments to apply for particular tokens. Default is null.
 * @property user An optional user-provided identifier. Default is null.
 */
@Serializable
data class OpenAICompletionRequest(
  val model: String,
  val prompt: String,
  @SerialName("max_tokens") val maxTokens: Int? = null,
  val temperature: Double? = null,
  @SerialName("top_p") val topP: Double? = null,
  @SerialName("n") val numberOfCompletions: Int? = null,
  val stream: Boolean? = null,
  val logprobs: Int? = null,
  val stop: List<String>? = null,
  @SerialName("presence_penalty") val presencePenalty: Double? = null,
  @SerialName("frequency_penalty") val frequencyPenalty: Double? = null,
  @SerialName("best_of") val bestOf: Int? = null,
  @SerialName("logit_bias") val logitBias: Map<String, Int>? = null,
  val user: String? = null,
)

@Serializable
data class CompletionData(
  val id: String,
  val objectType: String,
  val created: Long,
  val model: String,
  val choices: List<Choice> = emptyList(),
)

@Serializable
data class Choice(
  val text: String,
  val index: Int,
  @SerialName("logprobs") val logProbabilities: String? = null,
  @SerialName("finish_reason") val finishReason: String,
)


@Serializable
data class ModelInfoDTO(
  val name: String,
  val model: String
)

@Serializable
enum class RoleDTO {
  @SerialName("system") SYSTEM,
  @SerialName("user") USER,
  @SerialName("assistant") ASSISTANT,
  @SerialName("tool") TOOL,
}

// https://github.com/ollama/ollama/blob/main/docs/api.md#generate-a-chat-completion
@Serializable
data class ChatCompletionRequest(
  val model: String,
  val messages: List<MessageDTO>,
  val tools: List<ToolDTO> = emptyList(),
  val stream: Boolean? = null,
  @SerialName("keep_alive")
  val keepAlive: String? = null,
  val format: String? = null,
  // https://github.com/ollama/ollama/blob/main/docs/modelfile.md#parameter
  val options: JsonObject? = null
)

@Serializable
data class MessageDTO(
  val role: RoleDTO,
  val content: String = "",
  @SerialName("tool_calls")
  val toolCalls: List<ToolCallDTO> = emptyList(),
)

@Serializable
enum class ToolTypeDTO {
  @SerialName("function") FUNCTION
}

@Serializable
data class ToolDTO(
  val type: ToolTypeDTO?,
  val function: ToolFunctionDTO,
)

@Serializable
data class ToolFunctionDTO(
  val name: String,
  val description: String = "",
  val parameters: Map<String, ParameterInfoDTO> = emptyMap(),
  val required: List<String> = emptyList()
)

@Serializable
data class ParameterInfoDTO(
  val type: String, // TODO TypeDTO?
  val description: String = "",
  val enum: List<String> = emptyList()
)

@Serializable
data class ToolCallDTO(
  val function: ToolCallFunctionDTO
)

@Serializable
data class ToolCallFunctionDTO(
  val name: String,
  val arguments: JsonObject,
)

@Serializable
data class ChatCompletionResponse(
  val model: String,
  @SerialName("created_at")
  val createdAt: String? = null,
  val message: MessageDTO,
  val done: Boolean = false,
  @SerialName("done_reason")
  val doneReason: String? = null
)
