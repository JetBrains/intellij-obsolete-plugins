package com.intellij.aidebugger.evaluation.models.evaluators

import com.intellij.aidebugger.evaluation.models.entities.EvaluatorConfig
import com.intellij.aidebugger.evaluation.models.entities.LLMScore
import com.intellij.aidebugger.evaluation.models.llm.LlmProvider

object EvaluatorFactory {
    fun createFromConfig(
        config: EvaluatorConfig,
        provider: LlmProvider<LLMScore>?,
        inputVariables: Map<String, String>
    ): Evaluator {
        return when (config.type) {
            "llm judge" -> {
                require(provider != null) { "LlmProvider required for llm judge evaluator" }
                require(config.prompt != null) { "Prompt template required for llm judge evaluator" }
                LlmJudge(
                    provider = provider,
                    promptTemplate = config.prompt!!,
                    inputVariables = inputVariables,
                    maxVotes = 3,
                    maxConcurrency = 5
                )
            }
            "regexp" -> {
                val pattern = config.pattern ?: ".*{outputExpected}.*"
                RegexpEvaluator(pattern)
            }
            else -> throw IllegalArgumentException("Unknown evaluator type: ${config.type}")
        }
    }
}
