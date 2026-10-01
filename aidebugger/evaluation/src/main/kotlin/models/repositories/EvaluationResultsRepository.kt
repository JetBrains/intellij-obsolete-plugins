package com.intellij.aidebugger.evaluation.models.repositories

import com.intellij.aidebugger.evaluation.models.entities.AggregatedEvalResult
import com.intellij.aidebugger.evaluation.models.entities.EvalResult
import com.intellij.aidebugger.evaluation.models.entities.ScoreStats
import java.nio.file.Path

interface EvaluationResultsRepository {
    fun saveTableSnapshot(configName: String, tableSnapshot: TableSnapshot)
    fun loadTableSnapshot(configName: String): TableSnapshot
    fun saveEvaluationResult(aggregatedEvalResults: AggregatedEvalResult, evalResults: List<EvalResult>, outputDir: Path, selectedName: String?): Path
    fun getAverageScore(configName: String): Double
    fun getAverageAgentTime(configName: String): Double
    fun getAverageTokens(configName: String): Int
    fun getLastRunTimestamp(configName: String): Long?
    fun getLastAggregatedResult(configName: String): AggregatedEvalResult?
    fun removeResults(configName: String)
}

data class TableRow(
    val input: String,
    val evaluatorScores: Map<String, String>,
    val evaluatorExtras: Map<String, String>
)

data class TableSnapshot(
    val ids: List<String>,
    val rows: List<TableRow>,
    val outputs: List<String>,
    val aggregatedStats: Map<String, ScoreStats> = emptyMap()
)