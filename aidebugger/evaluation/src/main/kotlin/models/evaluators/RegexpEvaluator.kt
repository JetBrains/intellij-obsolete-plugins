package com.intellij.aidebugger.evaluation.models.evaluators

import com.intellij.aidebugger.evaluation.models.entities.DataPoint
import com.intellij.aidebugger.evaluation.models.entities.EvalResult
import java.time.LocalDateTime

class RegexpEvaluator(
    private val patternTemplate: String = ".*{outputExpected}.*"
) : Evaluator {

    companion object {
        const val DEFAULT_NAME = "Regexp"
        const val EVALUATOR_TYPE = "regexp"
    }

    override suspend fun evaluate(dataPoint: DataPoint): EvalResult {
        val pattern = resolvePattern(patternTemplate, dataPoint)
        val regex = Regex(pattern)
        val matches = regex.containsMatchIn(dataPoint.outputGen)
        val score = if (matches) 1.0 else 0.0

        return EvalResult(
            evaluator = DEFAULT_NAME,
            type = EVALUATOR_TYPE,
            score = score,
            extra = emptyMap(),
            id = dataPoint.id,
            input = dataPoint.input,
            outputGen = dataPoint.outputGen,
            outputExpected = dataPoint.outputExpected,
            experimentId = dataPoint.experimentId,
            raw = dataPoint.raw,
            runDatetime = dataPoint.runDatetime ?: LocalDateTime.now().toString(),
        )
    }

    private fun resolvePattern(template: String, dataPoint: DataPoint): String {
        var result = template
        result = result.replace("{id}", Regex.escape(dataPoint.id))
        result = result.replace("{input}", Regex.escape(dataPoint.input ?: ""))
        result = result.replace("{outputGen}", Regex.escape(dataPoint.outputGen))
        result = result.replace("{outputExpected}", Regex.escape(dataPoint.outputExpected ?: ""))
        result = result.replace("{experimentId}", Regex.escape(dataPoint.experimentId))
        result = result.replace("{runDatetime}", Regex.escape(dataPoint.runDatetime ?: ""))
        result = result.replace("{exception}", Regex.escape(dataPoint.exception ?: ""))
        return result
    }
}