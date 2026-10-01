package com.intellij.aidebugger.evaluation.models.evaluators

import com.intellij.aidebugger.evaluation.models.entities.DataPoint
import com.intellij.aidebugger.evaluation.models.entities.EvalResult
import com.intellij.aidebugger.evaluation.models.entities.LLMScore
import com.intellij.aidebugger.evaluation.models.llm.LlmProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.time.LocalDateTime

class LlmJudge(
    private val provider: LlmProvider<LLMScore>,
    private val promptTemplate: String,
    private val inputVariables: Map<String, String>,
    private val maxVotes: Int = 3,
    maxConcurrency: Int = 5,
) : Evaluator {

    private val semaphore = Semaphore(maxConcurrency)

    companion object {
        const val DEFAULT_NAME = "LLMJudge"
        const val EVALUATOR_TYPE = "llm judge"
    }

    override suspend fun evaluate(dataPoint: DataPoint): EvalResult = coroutineScope {
        val tasks = (1..maxVotes).map {
            async(Dispatchers.IO) {
                semaphore.withPermit {
                    val prompt = buildPrompt(dataPoint)
                    val score = provider.complete(prompt)
                    score.clipped()
                }
            }
        }
        val results = tasks.awaitAll()
        val scores = results.map { it.score }
        val explanations = results.mapNotNull { it.explanation }.toSet().toList()

        val majorityScore = if (scores.isEmpty()) 0.0
        else scores.groupingBy { it }.eachCount().maxBy { it.value }.key

        val extra = mapOf("voterExplanations" to explanations)

        EvalResult(
            evaluator = DEFAULT_NAME,
            type = EVALUATOR_TYPE,
            score = majorityScore,
            extra = extra,
            id = dataPoint.id,
            input = dataPoint.input,
            outputGen = dataPoint.outputGen,
            outputExpected = dataPoint.outputExpected,
            experimentId = dataPoint.experimentId,
            raw = dataPoint.raw,
            runDatetime = dataPoint.runDatetime ?: LocalDateTime.now().toString(),
        )
    }

    private fun buildPrompt(dp: DataPoint): String {
        var tpl = promptTemplate
        for ((tplVar, fieldName) in inputVariables) {
            val value = resolveField(dp, fieldName)
            tpl = tpl.replace("{$tplVar}", value)
        }
        return tpl
    }

    private fun resolveField(dp: DataPoint, name: String): String {
        return try {
            val kClass = DataPoint::class
            val property = kClass.members.firstOrNull { it.name == name }

            if (property != null) {
                val value = property.call(dp)
                value?.toString() ?: ""
            } else {
                dp.raw[name]?.toString() ?: ""
            }
        } catch (e: Exception) {
            dp.raw[name]?.toString() ?: ""
        }
    }
}