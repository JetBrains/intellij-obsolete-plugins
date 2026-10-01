package com.intellij.aidebugger.evaluation.models.evaluators

import com.intellij.aidebugger.evaluation.models.entities.AggregatedEvalResult
import com.intellij.aidebugger.evaluation.models.entities.DataPoint
import com.intellij.aidebugger.evaluation.models.entities.EvalResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDateTime

data class EvaluatorEntry(
    val name: String,
    val type: String,
    val instance: Evaluator
)

/**
 * Shared evaluation runner that executes multiple evaluators on a dataset.
 * Ensures proper isolation between evaluators and consistent result handling.
 */
object EvaluationRunner {

    /**
     * Run evaluation for a single data point with multiple evaluators.
     * Each evaluator is executed independently and results are tagged with the evaluator name.
     *
     * @param dataPoint The data point to evaluate
     * @param evaluators List of EvaluatorEntry
     * @return Map of evaluator name to EvalResult
     */
    suspend fun evaluateDataPoint(
        dataPoint: DataPoint,
        evaluators: List<EvaluatorEntry>
    ): Map<String, EvalResult> = withContext(Dispatchers.IO) {
        val results = mutableMapOf<String, EvalResult>()

        for (entry in evaluators) {
            try {
                val result = entry.instance.evaluate(dataPoint)
                results[entry.name] = result.copy(
                    evaluator = entry.name,
                    type = entry.type
                )
            } catch (e: Exception) {
                results[entry.name] = EvalResult(
                    evaluator = entry.name,
                    type = entry.type,
                    score = 0.0,
                    extra = mapOf("error" to (e.message ?: "Unknown error")),
                    id = dataPoint.id ?: "",
                    input = dataPoint.input ?: "",
                    outputGen = dataPoint.outputGen ?: "",
                    outputExpected = dataPoint.outputExpected ?: "",
                    experimentId = dataPoint.experimentId ?: "",
                    raw = dataPoint.raw,
                    runDatetime = LocalDateTime.now().toString()
                )
            }
        }
        results
    }

    /**
     * Run evaluation for multiple data points with multiple evaluators.
     *
     * @param dataPoints List of data points to evaluate
     * @param evaluators List of EvaluatorEntry
     * @param progressCallback Optional callback for progress updates (currentIndex, totalCount, dataPointId, evaluatorName)
     * @return List of all EvalResults (flattened across data points and evaluators)
     */
    suspend fun evaluateDataset(
        dataPoints: List<DataPoint>,
        evaluators: List<EvaluatorEntry>,
        progressCallback: ((Int, Int, String, String) -> Unit)? = null
    ): List<EvalResult> = withContext(Dispatchers.IO) {
        val allResults = mutableListOf<EvalResult>()
        val total = dataPoints.size

        dataPoints.forEachIndexed { index, dataPoint ->
            for (entry in evaluators) {
                progressCallback?.invoke(index, total, dataPoint.id ?: "", entry.name)
                try {
                    val result = entry.instance.evaluate(dataPoint)
                    allResults.add(result.copy(
                        evaluator = entry.name,
                        type = entry.type
                    ))
                } catch (e: Exception) {
                    allResults.add(EvalResult(
                        evaluator = entry.name,
                        type = entry.type,
                        score = 0.0,
                        extra = mapOf("error" to (e.message ?: "Unknown error")),
                        id = dataPoint.id ?: "",
                        input = dataPoint.input ?: "",
                        outputGen = dataPoint.outputGen ?: "",
                        outputExpected = dataPoint.outputExpected ?: "",
                        experimentId = dataPoint.experimentId ?: "",
                        raw = dataPoint.raw,
                        runDatetime = LocalDateTime.now().toString()
                    ))
                }
            }
        }
        allResults
    }

    /**
     * Run evaluation and compute aggregated statistics.
     *
     * @param dataPoints List of data points to evaluate
     * @param evaluators List of EvaluatorEntry
     * @param progressCallback Optional callback for progress updates
     * @return Pair of (aggregatedResult, allIndividualResults)
     */
    suspend fun evaluateAndAggregate(
        dataPoints: List<DataPoint>,
        evaluators: List<EvaluatorEntry>,
        progressCallback: ((Int, Int, String, String) -> Unit)? = null
    ): Pair<AggregatedEvalResult, List<EvalResult>> = withContext(Dispatchers.IO) {
        require(dataPoints.isNotEmpty()) { "Dataset is empty" }
        require(evaluators.isNotEmpty()) { "No evaluators provided" }

        val allResults = evaluateDataset(dataPoints, evaluators, progressCallback)
        val experimentId = dataPoints.first().experimentId ?: "unknown_exp"

        val aggregated = evalAggregate(allResults, experimentId)

        aggregated to allResults
    }

    /**
     * Extract score and extra maps from evaluation results grouped by evaluator.
     * Useful for UI display where you need per-evaluator columns.
     *
     * @param results Map of evaluator name to EvalResult
     * @return Pair of (scoresMap, extrasMap)
     */
    fun extractScoreAndExtraMaps(results: Map<String, EvalResult>): Pair<Map<String, String>, Map<String, String>> {
        val scoresMap = mutableMapOf<String, String>()
        val extrasMap = mutableMapOf<String, String>()

        for ((evaluatorName, result) in results) {
            scoresMap[evaluatorName] = String.format("%.2f", result.score)

            val extraMap = result.extra
            if (extraMap.isNotEmpty()) {
                // Check for explicit explanation keys first
                val explicitExplanation = sequenceOf("explanation", "reasoning", "feedback", "voterExplanations")
                    .mapNotNull { key -> extraMap[key]?.toString() }
                    .firstOrNull()

                if (explicitExplanation != null) {
                    extrasMap[evaluatorName] = explicitExplanation
                } else {
                    // Fallback: join all keys
                    val joined = extraMap.entries.joinToString("\n") { (k, v) -> "$k: $v" }
                    if (joined.isNotBlank()) {
                        extrasMap[evaluatorName] = joined
                    }
                }
            }
        }
        return scoresMap to extrasMap
    }
}