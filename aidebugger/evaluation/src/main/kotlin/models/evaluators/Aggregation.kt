package com.intellij.aidebugger.evaluation.models.evaluators

import com.intellij.aidebugger.evaluation.models.entities.AggregatedEvalResult
import com.intellij.aidebugger.evaluation.models.entities.EvalResult
import com.intellij.aidebugger.evaluation.models.entities.ScoreStats
import kotlin.math.sqrt

fun evalAggregate(results: List<EvalResult>, experimentId: String): AggregatedEvalResult {
    if (results.isEmpty()) {
        return AggregatedEvalResult(
            experimentId = experimentId,
            evaluatorsStats = emptyMap()
        )
    }

    val perEvaluatorStats = results.groupBy { it.type }
        .mapValues { (_, evalResults) ->
            calculateStats(evalResults.map { it.score })
        }

    return AggregatedEvalResult(
        experimentId = experimentId,
        evaluatorsStats = perEvaluatorStats
    )
}

private fun calculateStats(scores: List<Double>): ScoreStats {
    if (scores.isEmpty()) {
        return ScoreStats(mean = 0.0, std = 0.0, min = 0.0, max = 0.0, median = 0.0, count = 0)
    }

    val mean = scores.average()
    val std = if (scores.size > 1) {
        val variance = scores.sumOf { (it - mean) * (it - mean) } / (scores.size - 1)
        sqrt(variance)
    } else 0.0
    val min = scores.minOrNull() ?: 0.0
    val max = scores.maxOrNull() ?: 0.0
    val median = scores.sorted().let { s ->
        if (s.isEmpty()) 0.0
        else if (s.size % 2 == 1) s[s.size / 2]
        else (s[s.size / 2 - 1] + s[s.size / 2]) / 2.0
    }

    return ScoreStats(mean = mean, std = std, min = min, max = max, median = median, count = scores.size)
}