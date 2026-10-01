package com.intellij.aidebugger.evaluation

enum class EvaluationError(val key: String) {
    LLM_PROVIDER_NOT_FOUND("eval.error.llm.provider.not.found"),
    API_KEY_ERROR("eval.error.api.key"),
    AGENT_RUN_ERROR("eval.error.agent.run"),
    DATASET_READ_ERROR("eval.error.dataset.read")
}

class EvaluationException(val error: EvaluationError, cause: Throwable? = null) : RuntimeException(cause)