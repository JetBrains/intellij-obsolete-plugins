package com.intellij.aidebugger.evaluation.models.evaluators

import com.intellij.aidebugger.evaluation.models.entities.EvalResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.sqrt

class AggregationTest {

    @Test
    fun `returns empty aggregation for empty results list`() {
        val result = evalAggregate(emptyList(), "exp-123")

        assertEquals("exp-123", result.experimentId)
        assertTrue(result.evaluatorsStats.isEmpty())
    }

    @Test
    fun `aggregates single result with single evaluator`() {
        val evalResult = createEvalResult(
            evaluator = "LLMJudge",
            score = 0.85,
            id = "test-1",
            experimentId = "exp-001"
        )

        val aggregated = evalAggregate(listOf(evalResult), "exp-001")

        assertEquals("exp-001", aggregated.experimentId)
        assertEquals(1, aggregated.evaluatorsStats.size)

        val stats = aggregated.evaluatorsStats["LLMJudge"]!!
        assertEquals(0.85, stats.mean, 0.001)
        assertEquals(0.0, stats.std, 0.001)
        assertEquals(0.85, stats.min, 0.001)
        assertEquals(0.85, stats.max, 0.001)
        assertEquals(0.85, stats.median, 0.001)
        assertEquals(1, stats.count)
    }

    @Test
    fun `aggregates multiple results with single evaluator`() {
        val results = listOf(
            createEvalResult("Judge1", 0.8, "id-1", "exp-001"),
            createEvalResult("Judge1", 0.6, "id-2", "exp-001"),
            createEvalResult("Judge1", 0.9, "id-3", "exp-001"),
            createEvalResult("Judge1", 0.7, "id-4", "exp-001")
        )

        val aggregated = evalAggregate(results, "exp-001")

        val stats = aggregated.evaluatorsStats["Judge1"]!!
        assertEquals(0.75, stats.mean, 0.001)
        assertEquals(0.6, stats.min, 0.001)
        assertEquals(0.9, stats.max, 0.001)
        assertEquals(0.75, stats.median, 0.001)
        assertEquals(4, stats.count)

        val expectedStd = sqrt((0.0025 + 0.0225 + 0.0225 + 0.0025) / 3)
        assertEquals(expectedStd, stats.std, 0.001)
    }

    @Test
    fun `aggregates results from multiple evaluators`() {
        val results = listOf(
            createEvalResult("LLMJudge", 0.9, "id-1", "exp-002"),
            createEvalResult("LLMJudge", 0.8, "id-2", "exp-002"),
            createEvalResult("RegexMatch", 1.0, "id-1", "exp-002"),
            createEvalResult("RegexMatch", 0.0, "id-2", "exp-002"),
            createEvalResult("ExactMatch", 0.5, "id-1", "exp-002")
        )

        val aggregated = evalAggregate(results, "exp-002")

        assertEquals(3, aggregated.evaluatorsStats.size)

        val llmStats = aggregated.evaluatorsStats["LLMJudge"]!!
        assertEquals(0.85, llmStats.mean, 0.001)
        assertEquals(2, llmStats.count)

        val regexStats = aggregated.evaluatorsStats["RegexMatch"]!!
        assertEquals(0.5, regexStats.mean, 0.001)
        assertEquals(2, regexStats.count)

        val exactStats = aggregated.evaluatorsStats["ExactMatch"]!!
        assertEquals(0.5, exactStats.mean, 0.001)
        assertEquals(1, exactStats.count)
    }

    @Test
    fun `calculates correct median for odd number of scores`() {
        val results = listOf(
            createEvalResult("Judge1", 0.3, "id-1", "exp-003"),
            createEvalResult("Judge1", 0.5, "id-2", "exp-003"),
            createEvalResult("Judge1", 0.7, "id-3", "exp-003")
        )

        val aggregated = evalAggregate(results, "exp-003")
        val stats = aggregated.evaluatorsStats["Judge1"]!!

        assertEquals(0.5, stats.median, 0.001)
    }

    @Test
    fun `calculates correct median for even number of scores`() {
        val results = listOf(
            createEvalResult("Judge1", 0.2, "id-1", "exp-004"),
            createEvalResult("Judge1", 0.4, "id-2", "exp-004"),
            createEvalResult("Judge1", 0.6, "id-3", "exp-004"),
            createEvalResult("Judge1", 0.8, "id-4", "exp-004")
        )

        val aggregated = evalAggregate(results, "exp-004")
        val stats = aggregated.evaluatorsStats["Judge1"]!!

        assertEquals(0.5, stats.median, 0.001)
    }

    @Test
    fun `handles zero scores correctly`() {
        val results = listOf(
            createEvalResult("Judge1", 0.0, "id-1", "exp-005"),
            createEvalResult("Judge1", 0.0, "id-2", "exp-005"),
            createEvalResult("Judge1", 0.0, "id-3", "exp-005")
        )

        val aggregated = evalAggregate(results, "exp-005")
        val stats = aggregated.evaluatorsStats["Judge1"]!!

        assertEquals(0.0, stats.mean, 0.001)
        assertEquals(0.0, stats.std, 0.001)
        assertEquals(0.0, stats.min, 0.001)
        assertEquals(0.0, stats.max, 0.001)
        assertEquals(0.0, stats.median, 0.001)
        assertEquals(3, stats.count)
    }

    @Test
    fun `handles perfect scores correctly`() {
        val results = listOf(
            createEvalResult("Judge1", 1.0, "id-1", "exp-006"),
            createEvalResult("Judge1", 1.0, "id-2", "exp-006")
        )

        val aggregated = evalAggregate(results, "exp-006")
        val stats = aggregated.evaluatorsStats["Judge1"]!!

        assertEquals(1.0, stats.mean, 0.001)
        assertEquals(0.0, stats.std, 0.001)
        assertEquals(1.0, stats.min, 0.001)
        assertEquals(1.0, stats.max, 0.001)
        assertEquals(1.0, stats.median, 0.001)
    }

    @Test
    fun `handles mixed positive and negative-like scores`() {
        val results = listOf(
            createEvalResult("Judge1", 0.1, "id-1", "exp-007"),
            createEvalResult("Judge1", 0.2, "id-2", "exp-007"),
            createEvalResult("Judge1", 0.9, "id-3", "exp-007")
        )

        val aggregated = evalAggregate(results, "exp-007")
        val stats = aggregated.evaluatorsStats["Judge1"]!!

        assertEquals(0.4, stats.mean, 0.0667)
        assertEquals(0.1, stats.min, 0.001)
        assertEquals(0.9, stats.max, 0.001)
        assertEquals(0.2, stats.median, 0.001)
    }

    @Test
    fun `preserves evaluator names exactly as provided`() {
        val results = listOf(
            createEvalResult("Custom-Evaluator_v2.1", 0.5, "id-1", "exp-008"),
            createEvalResult("LLM Judge (GPT-4)", 0.7, "id-1", "exp-008")
        )

        val aggregated = evalAggregate(results, "exp-008")

        assertTrue(aggregated.evaluatorsStats.containsKey("Custom-Evaluator_v2.1"))
        assertTrue(aggregated.evaluatorsStats.containsKey("LLM Judge (GPT-4)"))
    }

    @Test
    fun `handles large number of results efficiently`() {
        val results = (1..1000).map { i ->
            createEvalResult("Judge1", i / 1000.0, "id-$i", "exp-009")
        }

        val aggregated = evalAggregate(results, "exp-009")
        val stats = aggregated.evaluatorsStats["Judge1"]!!

        assertEquals(1000, stats.count)
        assertTrue(stats.mean > 0.0)
        assertTrue(stats.std > 0.0)
    }

    @Test
    fun `handles single result with std of zero`() {
        val results = listOf(
            createEvalResult("Judge1", 0.75, "id-1", "exp-010")
        )

        val aggregated = evalAggregate(results, "exp-010")
        val stats = aggregated.evaluatorsStats["Judge1"]!!

        assertEquals(0.0, stats.std, 0.001)
        assertEquals(1, stats.count)
    }

    private fun createEvalResult(
        evaluator: String,
        score: Double,
        id: String,
        experimentId: String
    ): EvalResult {
        return EvalResult(
            evaluator = evaluator,
            type = evaluator,
            score = score,
            id = id,
            input = "test input",
            outputGen = "generated output",
            outputExpected = "expected output",
            experimentId = experimentId,
            extra = emptyMap(),
            raw = emptyMap(),
            runDatetime = "2025-01-01T00:00:00Z"
        )
    }
}
