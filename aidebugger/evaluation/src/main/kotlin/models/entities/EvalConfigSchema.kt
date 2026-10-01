package com.intellij.aidebugger.evaluation.models.entities

import com.intellij.aidebugger.evaluation.models.llm.LlmProviderType
import com.intellij.openapi.util.NlsSafe

/**
 * Represents a reference to an evaluation configuration.
 * Used to identify and locate configuration files.
 */
data class ConfigInfo(
    @param:NlsSafe val name: String,
    val fileName: String
)

data class EvaluatorConfig(
  @param:NlsSafe var name: String = "LLMJudge-1",
  @param:NlsSafe var type: String = "llm judge",
  @param:NlsSafe var pattern: String? = null,
  @param:NlsSafe var prompt: String? = null,
) {
    companion object {
        val ALLOWED_PROMPT_VARS: Set<String> = setOf("id", "input", "outputGen", "outputExpected", "experimentId", "raw", "runDatetime", "exception")

        fun getDefaultNameForType(type: String): String {
            return when (type) {
                "llm judge" -> "LLMJudge"
                "regexp" -> "Regexp"
                else -> "Evaluator"
            }
        }
    }

    fun validatePrompt(): Boolean {
        fun hasLlmScoreDefinition(text: String): Boolean {
            val hasScore = Regex("(?i)\"score\"\\s*:|\\bscore\\s*:").containsMatchIn(text)
            val hasExplanation = Regex("(?i)\"explanation\"\\s*:|\\bexplanation\\s*:").containsMatchIn(text)
            return hasScore && hasExplanation
        }

        fun hasOnlyAllowedTemplateVars(text: String): Boolean {
            val vars = Regex("\\{([a-zA-Z0-9_]+)}")
                .findAll(text)
                .map { it.groupValues[1] }
                .toSet()
            return vars.all { it in ALLOWED_PROMPT_VARS }
        }

        return when (type) {
            "llm judge" -> {
                val template = prompt ?: return true
                hasOnlyAllowedTemplateVars(template) && hasLlmScoreDefinition(template)
            }

            "regexp" -> {
                val template = pattern ?: return true
                hasOnlyAllowedTemplateVars(template)
            }

            else -> true
        }
    }
}

/**
 * Represents a complete evaluation run configuration.
 * This is the JSON structure stored in configuration files.
 */
data class EvalRunConfig(
    var name: String = "",
    var description: String? = null,
    var datasetName: String? = null,
    var extractorClass: String = DEFAULT_EXTRACTOR_FQCN,
    var configPath: String? = null,
    var extractorOutputPath: String? = null,
    var datasetMapping: Map<String, Any?>? = null,
    var runConfigName: String = "",
    var providerInstanceId: String? = null,
    var providerType: LlmProviderType? = null,
    var modelName: String? = null,
    var modelParams: Map<String, String>? = null,
    var promptTemplate: String? = null,
    var apiKey: String? = null,
    var evaluators: List<EvaluatorConfig>? = null,
) {
    companion object {
        const val DEFAULT_EXTRACTOR_FQCN: String = "com.intellij.aidebugger.evaluation.extractor.DebuggerTracesExtractor"
    }
}
