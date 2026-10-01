package com.intellij.aidebugger.evaluation.models.evaluators

import com.intellij.aidebugger.evaluation.models.entities.DataPoint
import com.intellij.aidebugger.evaluation.models.entities.LLMScore
import com.intellij.aidebugger.evaluation.models.llm.LlmProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

class LlmJudgeTest {

    @Test
    fun `evaluates with single vote and returns score`() = runBlocking {
        val provider = MockLlmProvider(0.85)
        val judge = LlmJudge(
            provider = provider,
            promptTemplate = "Rate: {input}",
            inputVariables = mapOf("input" to "input"),
            maxVotes = 1
        )

        val dataPoint = createDataPoint("test input")
        val result = judge.evaluate(dataPoint)

        assertEquals(0.85, result.score, 0.001)
        assertEquals("LLMJudge", result.evaluator)
        assertEquals("test input", result.input)
        assertEquals(1, provider.callCount.get())
    }

    @Test
    fun `evaluates with multiple votes and returns majority score`() = runBlocking {
        val provider = VariableScoreProvider(listOf(0.8, 0.8, 0.6))
        val judge = LlmJudge(
            provider = provider,
            promptTemplate = "Rate: {input}",
            inputVariables = mapOf("input" to "input"),
            maxVotes = 3
        )

        val dataPoint = createDataPoint("test input")
        val result = judge.evaluate(dataPoint)

        assertEquals(0.8, result.score, 0.001)
        assertEquals(3, provider.callCount.get())
    }

    @Test
    fun `evaluates with all different votes returns one of the scores`() = runBlocking {
        val provider = VariableScoreProvider(listOf(0.5, 0.7, 0.9))
        val judge = LlmJudge(
            provider = provider,
            promptTemplate = "Rate: {input}",
            inputVariables = mapOf("input" to "input"),
            maxVotes = 3
        )

        val dataPoint = createDataPoint("test input")
        val result = judge.evaluate(dataPoint)

        assertTrue(result.score in listOf(0.5, 0.7, 0.9))
        assertEquals(3, provider.callCount.get())
    }

    @Test
    fun `collects all explanations in extra field`() = runBlocking {
        val provider = MockLlmProvider(0.75, "Good answer")
        val judge = LlmJudge(
            provider = provider,
            promptTemplate = "Rate: {input}",
            inputVariables = mapOf("input" to "input"),
            maxVotes = 2
        )

        val dataPoint = createDataPoint("test input")
        val result = judge.evaluate(dataPoint)

        assertTrue(result.extra.containsKey("voterExplanations"))
        val explanations = result.extra["voterExplanations"] as List<*>
        assertEquals(1, explanations.size)
        assertEquals("Good answer", explanations[0])
    }

    @Test
    fun `collects unique explanations from multiple votes`() = runBlocking {
        val provider = MultiExplanationProvider(
            listOf(
                LLMScore(0.8, "Explanation A"),
                LLMScore(0.8, "Explanation B"),
                LLMScore(0.8, "Explanation A")
            )
        )
        val judge = LlmJudge(
            provider = provider,
            promptTemplate = "Rate: {input}",
            inputVariables = mapOf("input" to "input"),
            maxVotes = 3
        )

        val dataPoint = createDataPoint("test input")
        val result = judge.evaluate(dataPoint)

        val explanations = result.extra["voterExplanations"] as List<*>
        assertEquals(2, explanations.size)
        assertTrue(explanations.contains("Explanation A"))
        assertTrue(explanations.contains("Explanation B"))
    }

    @Test
    fun `replaces template variable with datapoint field`() = runBlocking {
        val capturingProvider = CapturingLlmProvider(0.9)
        val judge = LlmJudge(
            provider = capturingProvider,
            promptTemplate = "Input: {input}, Expected: {expected}",
            inputVariables = mapOf("input" to "input", "expected" to "outputExpected"),
            maxVotes = 1
        )

        val dataPoint = createDataPoint(
            input = "hello",
            outputExpected = "world"
        )
        judge.evaluate(dataPoint)

        assertEquals("Input: hello, Expected: world", capturingProvider.lastPrompt)
    }

    @Test
    fun `resolves field from raw map when not in datapoint properties`() = runBlocking {
        val capturingProvider = CapturingLlmProvider(0.9)
        val judge = LlmJudge(
            provider = capturingProvider,
            promptTemplate = "Custom: {customField}",
            inputVariables = mapOf("customField" to "metadata"),
            maxVotes = 1
        )

        val dataPoint = createDataPoint(
            input = "test",
            raw = mapOf("metadata" to "custom value")
        )
        judge.evaluate(dataPoint)

        assertEquals("Custom: custom value", capturingProvider.lastPrompt)
    }

    @Test
    fun `handles missing field gracefully with empty string`() = runBlocking {
        val capturingProvider = CapturingLlmProvider(0.9)
        val judge = LlmJudge(
            provider = capturingProvider,
            promptTemplate = "Field: {missing}",
            inputVariables = mapOf("missing" to "nonExistent"),
            maxVotes = 1
        )

        val dataPoint = createDataPoint("test")
        judge.evaluate(dataPoint)

        assertEquals("Field: ", capturingProvider.lastPrompt)
    }

    @Test
    fun `clips score above 1 to 1`() = runBlocking {
        val provider = MockLlmProvider(1.5)
        val judge = LlmJudge(
            provider = provider,
            promptTemplate = "Rate: {input}",
            inputVariables = mapOf("input" to "input"),
            maxVotes = 1
        )

        val dataPoint = createDataPoint("test")
        val result = judge.evaluate(dataPoint)

        assertEquals(1.0, result.score, 0.001)
    }

    @Test
    fun `clips score below 0 to 0`() = runBlocking {
        val provider = MockLlmProvider(-0.5)
        val judge = LlmJudge(
            provider = provider,
            promptTemplate = "Rate: {input}",
            inputVariables = mapOf("input" to "input"),
            maxVotes = 1
        )

        val dataPoint = createDataPoint("test")
        val result = judge.evaluate(dataPoint)

        assertEquals(0.0, result.score, 0.001)
    }

    @Test
    fun `clips NaN score to 0`() = runBlocking {
        val provider = MockLlmProvider(Double.NaN)
        val judge = LlmJudge(
            provider = provider,
            promptTemplate = "Rate: {input}",
            inputVariables = mapOf("input" to "input"),
            maxVotes = 1
        )

        val dataPoint = createDataPoint("test")
        val result = judge.evaluate(dataPoint)

        assertEquals(0.0, result.score, 0.001)
    }

    @Test
    fun `preserves all datapoint fields in result`() = runBlocking {
        val provider = MockLlmProvider(0.75)
        val judge = LlmJudge(
            provider = provider,
            promptTemplate = "Rate: {input}",
            inputVariables = mapOf("input" to "input"),
            maxVotes = 1
        )

        val dataPoint = createDataPoint(
            id = "test-123",
            input = "test input",
            outputGen = "generated",
            outputExpected = "expected",
            experimentId = "exp-456",
            raw = mapOf("key" to "value"),
            runDatetime = "2025-01-01T12:00:00"
        )

        val result = judge.evaluate(dataPoint)

        assertEquals("test-123", result.id)
        assertEquals("test input", result.input)
        assertEquals("generated", result.outputGen)
        assertEquals("expected", result.outputExpected)
        assertEquals("exp-456", result.experimentId)
        assertEquals("2025-01-01T12:00:00", result.runDatetime)
    }

    @Test
    fun `uses current datetime when datapoint runDatetime is null`() = runBlocking {
        val provider = MockLlmProvider(0.8)
        val judge = LlmJudge(
            provider = provider,
            promptTemplate = "Rate: {input}",
            inputVariables = mapOf("input" to "input"),
            maxVotes = 1
        )

        val dataPoint = createDataPoint(
            input = "test",
            runDatetime = null
        )

        val result = judge.evaluate(dataPoint)

        assertNotNull(result.runDatetime)
        assertTrue(result.runDatetime!!.isNotEmpty())
    }

    @Test
    fun `respects maxConcurrency limit`() = runBlocking {
        val provider = ConcurrencyTrackingProvider()
        val judge = LlmJudge(
            provider = provider,
            promptTemplate = "Rate: {input}",
            inputVariables = mapOf("input" to "input"),
            maxVotes = 10,
            maxConcurrency = 3
        )

        val dataPoint = createDataPoint("test")
        judge.evaluate(dataPoint)

        assertTrue(provider.maxConcurrent.get() <= 3)
    }

    @Test
    fun `returns zero score when no votes completed`() = runBlocking {
        val provider = EmptyResultProvider()
        val judge = LlmJudge(
            provider = provider,
            promptTemplate = "Rate: {input}",
            inputVariables = mapOf("input" to "input"),
            maxVotes = 0
        )

        val dataPoint = createDataPoint("test")
        val result = judge.evaluate(dataPoint)

        assertEquals(0.0, result.score, 0.001)
    }

    @Test
    fun `handles multiple template variables in single prompt`() = runBlocking {
        val capturingProvider = CapturingLlmProvider(0.9)
        val judge = LlmJudge(
            provider = capturingProvider,
            promptTemplate = "{var1} and {var2} and {var3}",
            inputVariables = mapOf(
                "var1" to "input",
                "var2" to "outputGen",
                "var3" to "outputExpected"
            ),
            maxVotes = 1
        )

        val dataPoint = createDataPoint(
            input = "A",
            outputGen = "B",
            outputExpected = "C"
        )
        judge.evaluate(dataPoint)

        assertEquals("A and B and C", capturingProvider.lastPrompt)
    }

    @Test
    fun `majority vote with 5 votes prefers most frequent score`() = runBlocking {
        val provider = VariableScoreProvider(listOf(0.7, 0.7, 0.7, 0.8, 0.9))
        val judge = LlmJudge(
            provider = provider,
            promptTemplate = "Rate: {input}",
            inputVariables = mapOf("input" to "input"),
            maxVotes = 5
        )

        val dataPoint = createDataPoint("test")
        val result = judge.evaluate(dataPoint)

        assertEquals(0.7, result.score, 0.001)
    }

    private fun createDataPoint(
        input: String = "default input",
        id: String = "id-1",
        outputGen: String = "generated",
        outputExpected: String = "expected",
        experimentId: String = "exp-test",
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

    private class MockLlmProvider(
        private val score: Double,
        private val explanation: String = "Mock explanation"
    ) : LlmProvider<LLMScore> {
        val callCount = AtomicInteger(0)

        override suspend fun complete(prompt: String): LLMScore {
            callCount.incrementAndGet()
            return LLMScore(score, explanation)
        }
    }

    private class VariableScoreProvider(private val scores: List<Double>) : LlmProvider<LLMScore> {
        val callCount = AtomicInteger(0)

        override suspend fun complete(prompt: String): LLMScore {
            val index = callCount.getAndIncrement()
            val score = if (index < scores.size) scores[index] else scores.last()
            return LLMScore(score, "Explanation $index")
        }
    }

    private class MultiExplanationProvider(private val scores: List<LLMScore>) : LlmProvider<LLMScore> {
        val callCount = AtomicInteger(0)

        override suspend fun complete(prompt: String): LLMScore {
            val index = callCount.getAndIncrement()
            return if (index < scores.size) scores[index] else scores.last()
        }
    }

    private class CapturingLlmProvider(private val score: Double) : LlmProvider<LLMScore> {
        var lastPrompt: String = ""

        override suspend fun complete(prompt: String): LLMScore {
            lastPrompt = prompt
            return LLMScore(score, "Captured")
        }
    }

    private class ConcurrencyTrackingProvider : LlmProvider<LLMScore> {
        val currentConcurrent = AtomicInteger(0)
        val maxConcurrent = AtomicInteger(0)

        override suspend fun complete(prompt: String): LLMScore {
            val current = currentConcurrent.incrementAndGet()

            var max = maxConcurrent.get()
            while (current > max) {
                if (maxConcurrent.compareAndSet(max, current)) {
                    break
                }
                max = maxConcurrent.get()
            }

            delay(10)
            currentConcurrent.decrementAndGet()
            return LLMScore(0.8, "Concurrent test")
        }
    }

    private class EmptyResultProvider : LlmProvider<LLMScore> {
        override suspend fun complete(prompt: String): LLMScore {
            return LLMScore(0.0, "")
        }
    }
}
