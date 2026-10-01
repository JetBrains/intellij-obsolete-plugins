package com.intellij.aidebugger.evaluation.models.storage

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.intellij.aidebugger.evaluation.models.entities.AggregatedEvalResult
import com.intellij.aidebugger.evaluation.models.entities.EvalResult
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.writeText

/**
 * Saves evaluation results to disk in a standardized format.
 * Can be used in both plugin and CLI contexts.
 */
fun saveEvaluationResults(
    aggregatedEvalResults: AggregatedEvalResult,
    evalResults: List<EvalResult>,
    outputDir: Path,
    selectedName: String? = null
): Path {
    val perConfigDir = if (!selectedName.isNullOrBlank()) {
        outputDir.resolve(sanitizeNameForFile(selectedName))
    } else {
        outputDir
    }

    Files.createDirectories(perConfigDir)

    val aggFile = perConfigDir.resolve("eval_result.json")
    val rawFile = perConfigDir.resolve("eval_results_raw.json")

    val mapper = ObjectMapper()
        .registerModule(KotlinModule.Builder().build())

    val aggJson = mapper.writerWithDefaultPrettyPrinter()
        .writeValueAsString(aggregatedEvalResults)
    aggFile.writeText(aggJson)

    val rawJson = mapper.writerWithDefaultPrettyPrinter()
        .writeValueAsString(evalResults)
    rawFile.writeText(rawJson)

    return aggFile
}

fun sanitizeNameForFile(name: String): String =
    name.trim().ifEmpty { "config" }.replace(Regex("[\\\\/:*?\"<>|]"), "_")