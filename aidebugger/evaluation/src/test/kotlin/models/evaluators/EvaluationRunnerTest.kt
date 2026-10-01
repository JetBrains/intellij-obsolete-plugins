package com.intellij.aidebugger.evaluation.models.evaluators

import com.intellij.aidebugger.evaluation.models.entities.DataPoint
import com.intellij.aidebugger.evaluation.models.entities.EvalResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class EvaluationRunnerTest {

    @Test
    fun `evaluateDataPoint returns results for all evaluators`() = runBlocking {
        val dataPoint = createDataPoint("id-1", "input text", "expected output")
        val evaluators = listOf(
            EvaluatorEntry("eval1", "eval1", MockEvaluator(0.8)),
            EvaluatorEntry("eval2", "eval2", MockEvaluator(0.6))
        )

        val results = EvaluationRunner.evaluateDataPoint(dataPoint, evaluators)

        assertEquals(2, results.size)
        assertTrue(results.containsKey("eval1"))
        assertTrue(results.containsKey("eval2"))
        assertEquals(0.8, results["eval1"]!!.score, 0.001)
        assertEquals(0.6, results["eval2"]!!.score, 0.001)
    }

    @Test
    fun `evaluateDataPoint tags results with correct evaluator name`() = runBlocking {
        val dataPoint = createDataPoint("id-1", "input", "expected")
        val evaluators = listOf(EvaluatorEntry("custom-evaluator", "custom-evaluator", MockEvaluator(0.9)))

        val results = EvaluationRunner.evaluateDataPoint(dataPoint, evaluators)

        assertEquals("custom-evaluator", results["custom-evaluator"]!!.evaluator)
    }

    @Test
    fun `evaluateDataPoint handles evaluator exceptions gracefully`() = runBlocking {
        val dataPoint = createDataPoint("id-1", "input", "expected")
        val evaluators = listOf(
            EvaluatorEntry("working", "working", MockEvaluator(0.7)),
            EvaluatorEntry("failing", "failing", FailingEvaluator("Test error"))
        )

        val results = EvaluationRunner.evaluateDataPoint(dataPoint, evaluators)

        assertEquals(2, results.size)
        assertEquals(0.7, results["working"]!!.score, 0.001)

        val errorResult = results["failing"]!!
        assertEquals(0.0, errorResult.score, 0.001)
        assertEquals("failing", errorResult.evaluator)
        assertTrue(errorResult.extra.containsKey("error"))
        assertEquals("Test error", errorResult.extra["error"])
    }

    @Test
    fun `evaluateDataPoint preserves data point fields in error results`() = runBlocking {
        val dataPoint = createDataPoint(
            id = "test-id",
            input = "test input",
            outputExpected = "expected",
            experimentId = "exp-123"
        )
        val evaluators = listOf(EvaluatorEntry("failing", "failing", FailingEvaluator("Error")))

        val results = EvaluationRunner.evaluateDataPoint(dataPoint, evaluators)

        val result = results["failing"]!!
        assertEquals("test-id", result.id)
        assertEquals("test input", result.input)
        assertEquals("expected", result.outputExpected)
        assertEquals("exp-123", result.experimentId)
    }

    @Test
    fun `evaluateDataset processes all data points with all evaluators`() = runBlocking {
        val dataPoints = listOf(
            createDataPoint("id-1", "input1", "expected1"),
            createDataPoint("id-2", "input2", "expected2")
        )
        val evaluators = listOf(
            EvaluatorEntry("eval1", "eval1", MockEvaluator(0.8)),
            EvaluatorEntry("eval2", "eval2", MockEvaluator(0.6))
        )

        val results = EvaluationRunner.evaluateDataset(dataPoints, evaluators)

        assertEquals(4, results.size)

        val eval1Results = results.filter { it.evaluator == "eval1" }
        assertEquals(2, eval1Results.size)
        assertTrue(eval1Results.all { it.score == 0.8 })

        val eval2Results = results.filter { it.evaluator == "eval2" }
        assertEquals(2, eval2Results.size)
        assertTrue(eval2Results.all { it.score == 0.6 })
    }

    @Test
    fun `evaluateDataset invokes progress callback correctly`() = runBlocking {
        val dataPoints = listOf(
            createDataPoint("id-1", "input1", "expected1"),
            createDataPoint("id-2", "input2", "expected2")
        )
        val evaluators = listOf(
            EvaluatorEntry("eval1", "eval1", MockEvaluator(0.8)),
            EvaluatorEntry("eval2", "eval2", MockEvaluator(0.6))
        )

        val progressCalls = mutableListOf<ProgressCall>()

        EvaluationRunner.evaluateDataset(dataPoints, evaluators) { index, total, dpId, evalName ->
            progressCalls.add(ProgressCall(index, total, dpId, evalName))
        }

        assertEquals(4, progressCalls.size)
        assertTrue(progressCalls.all { it.total == 2 })

        val firstDpCalls = progressCalls.filter { it.index == 0 }
        assertEquals(2, firstDpCalls.size)
        assertTrue(firstDpCalls.all { it.dataPointId == "id-1" })

        val secondDpCalls = progressCalls.filter { it.index == 1 }
        assertEquals(2, secondDpCalls.size)
        assertTrue(secondDpCalls.all { it.dataPointId == "id-2" })
    }

    @Test
    fun `evaluateDataset handles partial failures without stopping`() = runBlocking {
        val dataPoints = listOf(
            createDataPoint("id-1", "input1", "expected1"),
            createDataPoint("id-2", "input2", "expected2")
        )
        val evaluators = listOf(
            EvaluatorEntry("working", "working", MockEvaluator(0.7)),
            EvaluatorEntry("failing", "failing", FailingEvaluator("Error"))
        )

        val results = EvaluationRunner.evaluateDataset(dataPoints, evaluators)

        assertEquals(4, results.size)

        val workingResults = results.filter { it.evaluator == "working" }
        assertEquals(2, workingResults.size)
        assertTrue(workingResults.all { it.score == 0.7 })

        val failingResults = results.filter { it.evaluator == "failing" }
        assertEquals(2, failingResults.size)
        assertTrue(failingResults.all { it.score == 0.0 })
        assertTrue(failingResults.all { it.extra.containsKey("error") })
    }

    @Test
    fun `evaluateAndAggregate returns aggregated stats and individual results`() = runBlocking {
        val dataPoints = listOf(
            createDataPoint("id-1", "input1", "expected1", "exp-001"),
            createDataPoint("id-2", "input2", "expected2", "exp-001")
        )
        val evaluators = listOf(
            EvaluatorEntry("eval1", "eval1", MockEvaluator(0.8)),
            EvaluatorEntry("eval2", "eval2", MockEvaluator(0.6))
        )

        val (aggregated, allResults) = EvaluationRunner.evaluateAndAggregate(dataPoints, evaluators)

        assertEquals(4, allResults.size)
        assertEquals("exp-001", aggregated.experimentId)
        assertEquals(2, aggregated.evaluatorsStats.size)

        val eval1Stats = aggregated.evaluatorsStats["eval1"]!!
        assertEquals(0.8, eval1Stats.mean, 0.001)
        assertEquals(2, eval1Stats.count)

        val eval2Stats = aggregated.evaluatorsStats["eval2"]!!
        assertEquals(0.6, eval2Stats.mean, 0.001)
        assertEquals(2, eval2Stats.count)
    }

    @Test
    fun `evaluateAndAggregate throws when dataset is empty`() {
        val evaluators = listOf(EvaluatorEntry("eval1", "eval1", MockEvaluator(0.8)))

        try {
            runBlocking {
                EvaluationRunner.evaluateAndAggregate(emptyList(), evaluators)
            }
            fail("Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("empty") == true)
        }
    }

    @Test
    fun `evaluateAndAggregate throws when no evaluators provided`() {
        val dataPoints = listOf(createDataPoint("id-1", "input", "expected"))

        try {
            runBlocking {
                EvaluationRunner.evaluateAndAggregate(dataPoints, emptyList())
            }
            fail("Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("evaluator") == true)
        }
    }

    @Test
    fun `extractScoreAndExtraMaps formats scores and extracts extras`() {
        val results = mapOf(
            "eval1" to createEvalResult("eval1", 0.856, extra = emptyMap()),
            "eval2" to createEvalResult("eval2", 0.5, extra = mapOf("explanation" to "Good result")),
            "eval3" to createEvalResult("eval3", 1.0, extra = mapOf("key" to "value"))
        )

        val (scoresMap, extrasMap) = EvaluationRunner.extractScoreAndExtraMaps(results)

        assertEquals(3, scoresMap.size)
        assertEquals("0.86", scoresMap["eval1"])
        assertEquals("0.50", scoresMap["eval2"])
        assertEquals("1.00", scoresMap["eval3"])

        assertEquals(2, extrasMap.size)
        assertEquals("Good result", extrasMap["eval2"])
        assertEquals("key: value", extrasMap["eval3"])
    }

    @Test
    fun `extractScoreAndExtraMaps handles explanation fields specially`() {
        val results = mapOf(
            "eval1" to createEvalResult("eval1", 0.8, extra = mapOf("explanation" to "This is good")),
            "eval2" to createEvalResult("eval2", 0.6, extra = mapOf("reasoning" to "Because X")),
            "eval3" to createEvalResult("eval3", 0.7, extra = mapOf("feedback" to "Nice work"))
        )

        val (_, extrasMap) = EvaluationRunner.extractScoreAndExtraMaps(results)

        assertEquals("This is good", extrasMap["eval1"])
        assertEquals("Because X", extrasMap["eval2"])
        assertEquals("Nice work", extrasMap["eval3"])
    }

    @Test
    fun `extractScoreAndExtraMaps excludes empty extras`() {
        val results = mapOf(
            "eval1" to createEvalResult("eval1", 0.8, extra = emptyMap()),
            "eval2" to createEvalResult("eval2", 0.6, extra = mapOf("key" to "value"))
        )

        val (scoresMap, extrasMap) = EvaluationRunner.extractScoreAndExtraMaps(results)

        assertEquals(2, scoresMap.size)
        assertEquals(1, extrasMap.size)
        assertFalse(extrasMap.containsKey("eval1"))
        assertTrue(extrasMap.containsKey("eval2"))
    }

    @Test
    fun `extractScoreAndExtraMaps handles multiple extra fields`() {
        val results = mapOf(
            "eval1" to createEvalResult(
                "eval1", 0.8,
                extra = mapOf(
                    "detail1" to "value1",
                    "detail2" to "value2",
                    "detail3" to "value3"
                )
            )
        )

        val (_, extrasMap) = EvaluationRunner.extractScoreAndExtraMaps(results)

        val extraText = extrasMap["eval1"]!!
        assertTrue(extraText.contains("detail1: value1"))
        assertTrue(extraText.contains("detail2: value2"))
        assertTrue(extraText.contains("detail3: value3"))
    }

    @Test
    fun `evaluateDataPoint with single evaluator`() = runBlocking {
        val dataPoint = createDataPoint("id-1", "test input", "test expected")
        val evaluators = listOf(EvaluatorEntry("single", "single", MockEvaluator(0.95)))

        val results = EvaluationRunner.evaluateDataPoint(dataPoint, evaluators)

        assertEquals(1, results.size)
        assertEquals(0.95, results["single"]!!.score, 0.001)
    }

    @Test
    fun `evaluateDataset with empty evaluators list returns empty results`() = runBlocking {
        val dataPoints = listOf(createDataPoint("id-1", "input", "expected"))

        val results = EvaluationRunner.evaluateDataset(dataPoints, emptyList())

        assertTrue(results.isEmpty())
    }

    @Test
    fun `evaluateDataset with empty data points returns empty results`() = runBlocking {
        val evaluators = listOf(EvaluatorEntry("eval1", "eval1", MockEvaluator(0.8)))

        val results = EvaluationRunner.evaluateDataset(emptyList(), evaluators)

        assertTrue(results.isEmpty())
    }

    @Test
    fun `evaluateDataPoint preserves all datapoint fields in results`() = runBlocking {
        val dataPoint = createDataPoint(
            id = "test-123",
            input = "custom input",
            outputGen = "generated output",
            outputExpected = "expected output",
            experimentId = "exp-456",
            raw = mapOf("key" to "value"),
            runDatetime = "2025-01-01T12:00:00"
        )
        val evaluators = listOf(EvaluatorEntry("eval1", "eval1", MockEvaluator(0.75)))

        val results = EvaluationRunner.evaluateDataPoint(dataPoint, evaluators)

        val result = results["eval1"]!!
        assertEquals("test-123", result.id)
        assertEquals("custom input", result.input)
        assertEquals("generated output", result.outputGen)
        assertEquals("expected output", result.outputExpected)
        assertEquals("exp-456", result.experimentId)
        assertEquals("2025-01-01T12:00:00", result.runDatetime)
    }

    private fun createDataPoint(
        id: String,
        input: String,
        outputExpected: String,
        experimentId: String = "exp-test",
        outputGen: String = "generated",
        raw: Map<String, Any?> = emptyMap(),
        runDatetime: String? = "2025-01-01T00:00:00"
    ): DataPoint {
        return DataPoint(
            id = id,
            input = input,
            outputGen = outputGen,
            outputExpected = outputExpected,
            experimentId = experimentId,
            raw = raw,
            runDatetime = runDatetime
        )
    }

    private fun createEvalResult(
        evaluator: String,
        score: Double,
        extra: Map<String, Any?> = emptyMap()
    ): EvalResult {
        return EvalResult(
            evaluator = evaluator,
            type = "test",
            score = score,
            extra = extra,
            id = "id-1",
            input = "input",
            outputGen = "gen",
            outputExpected = "expected",
            experimentId = "exp",
            raw = emptyMap(),
            runDatetime = "2025-01-01T00:00:00"
        )
    }

    private data class ProgressCall(
        val index: Int,
        val total: Int,
        val dataPointId: String,
        val evaluatorName: String
    )

    private class MockEvaluator(private val score: Double) : Evaluator {
        override suspend fun evaluate(dataPoint: DataPoint): EvalResult {
            return EvalResult(
                evaluator = "mock",
                type = "test",
                score = score,
                extra = emptyMap(),
                id = dataPoint.id,
                input = dataPoint.input,
                outputGen = dataPoint.outputGen,
                outputExpected = dataPoint.outputExpected,
                experimentId = dataPoint.experimentId,
                raw = dataPoint.raw,
                runDatetime = dataPoint.runDatetime ?: "2025-01-01T00:00:00"
            )
        }
    }

    private class FailingEvaluator(private val errorMessage: String) : Evaluator {
        override suspend fun evaluate(dataPoint: DataPoint): EvalResult {
            throw RuntimeException(errorMessage)
        }
    }
}
