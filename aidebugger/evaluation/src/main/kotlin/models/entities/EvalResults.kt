package com.intellij.aidebugger.evaluation.models.entities

data class EvalResult (
    val evaluator: String,
    val type: String,
    val score: Double,
    val extra: Map<String, Any?> = emptyMap(),
    val id: String,
    val input: String,
    val outputGen: String,
    val outputExpected: String,
    val experimentId: String,
    val raw: Map<String, Any?> = emptyMap(),
    val runDatetime: String? = null,
)

/**
 * Extracts voter explanation text from extra data.
 */
fun EvalResult.extractExtraText(): String {
    val ve = extra["voterExplanations"]
    return when (ve) {
        is List<*> -> ve.filterIsInstance<String>().firstOrNull()?.take(200) ?: ""
        is String -> ve.take(200)
        else -> ""
    }
}

data class ScoreStats(
    val mean: Double,
    val std: Double,
    val min: Double,
    val max: Double,
    val median: Double,
    val count: Int,
)

data class AggregatedEvalResult(
    val experimentId: String,
    val evaluatorsStats: Map<String, ScoreStats> = emptyMap(),
)