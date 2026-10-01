package com.intellij.aidebugger.evaluation.models.entities

import com.fasterxml.jackson.annotation.JsonCreator

data class LLMScore @JsonCreator constructor(
    val score: Double,
    val explanation: String,
) {
    fun clipped(): LLMScore {
        val s = when {
            score.isNaN() -> 0.0
            score < 0.0 -> 0.0
            score > 1.0 -> 1.0
            else -> score
        }
        return if (s == score) this else LLMScore(s, explanation)
    }
}