package com.intellij.aidebugger.evaluation.models.evaluators

import com.intellij.aidebugger.evaluation.models.entities.DataPoint
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RegexpEvaluatorTest {

    @Test
    fun `evaluates match with default pattern`() = runBlocking {
        val evaluator = RegexpEvaluator()
        val dataPoint = createDataPoint(
            outputGen = "The result is expected value here",
            outputExpected = "expected value"
        )

        val result = evaluator.evaluate(dataPoint)

        assertEquals(1.0, result.score, 0.001)
        assertEquals("Regexp", result.evaluator)
        assertEquals(dataPoint.id, result.id)
        assertEquals(dataPoint.outputGen, result.outputGen)
        assertEquals(dataPoint.outputExpected, result.outputExpected)
    }

    @Test
    fun `evaluates no match with default pattern`() = runBlocking {
        val evaluator = RegexpEvaluator()
        val dataPoint = createDataPoint(
            outputGen = "The result is different",
            outputExpected = "expected value"
        )

        val result = evaluator.evaluate(dataPoint)

        assertEquals(0.0, result.score, 0.001)
        assertEquals("Regexp", result.evaluator)
    }

    @Test
    fun `evaluates with custom exact match pattern`() = runBlocking {
        val evaluator = RegexpEvaluator(patternTemplate = "^{outputExpected}$")

        val matchingDataPoint = createDataPoint(
            outputGen = "exact",
            outputExpected = "exact"
        )
        val matchingResult = evaluator.evaluate(matchingDataPoint)
        assertEquals(1.0, matchingResult.score, 0.001)

        val nonMatchingDataPoint = createDataPoint(
            outputGen = "exact match",
            outputExpected = "exact"
        )
        val nonMatchingResult = evaluator.evaluate(nonMatchingDataPoint)
        assertEquals(0.0, nonMatchingResult.score, 0.001)
    }

    @Test
    fun `evaluates with custom prefix pattern`() = runBlocking {
        val evaluator = RegexpEvaluator(patternTemplate = "^{outputExpected}")
        val dataPoint = createDataPoint(
            outputGen = "Hello world",
            outputExpected = "Hello"
        )

        val result = evaluator.evaluate(dataPoint)

        assertEquals(1.0, result.score, 0.001)
    }

    @Test
    fun `evaluates with custom suffix pattern`() = runBlocking {
        val evaluator = RegexpEvaluator(patternTemplate = "{outputExpected}$")
        val dataPoint = createDataPoint(
            outputGen = "world Hello",
            outputExpected = "Hello"
        )

        val result = evaluator.evaluate(dataPoint)

        assertEquals(1.0, result.score, 0.001)
    }

    @Test
    fun `handles special regex characters in outputExpected`() = runBlocking {
        val evaluator = RegexpEvaluator()
        val dataPoint = createDataPoint(
            outputGen = "The answer is (a+b)*c",
            outputExpected = "(a+b)*c"
        )

        val result = evaluator.evaluate(dataPoint)

        assertEquals(1.0, result.score, 0.001)
    }

    @Test
    fun `handles dots and brackets in outputExpected`() = runBlocking {
        val evaluator = RegexpEvaluator()
        val dataPoint = createDataPoint(
            outputGen = "Result: [1.5, 2.5]",
            outputExpected = "[1.5, 2.5]"
        )

        val result = evaluator.evaluate(dataPoint)

        assertEquals(1.0, result.score, 0.001)
    }

    @Test
    fun `substitutes id placeholder correctly`() = runBlocking {
        val evaluator = RegexpEvaluator(patternTemplate = "ID:{id}.*")
        val dataPoint = createDataPoint(
            id = "test-123",
            outputGen = "ID:test-123 result",
            outputExpected = "irrelevant"
        )

        val result = evaluator.evaluate(dataPoint)

        assertEquals(1.0, result.score, 0.001)
    }

    @Test
    fun `substitutes input placeholder correctly`() = runBlocking {
        val evaluator = RegexpEvaluator(patternTemplate = "Input was: {input}")
        val dataPoint = createDataPoint(
            input = "user query",
            outputGen = "Input was: user query",
            outputExpected = "irrelevant"
        )

        val result = evaluator.evaluate(dataPoint)

        assertEquals(1.0, result.score, 0.001)
    }

    @Test
    fun `substitutes experimentId placeholder correctly`() = runBlocking {
        val evaluator = RegexpEvaluator(patternTemplate = "Exp: {experimentId}")
        val dataPoint = createDataPoint(
            experimentId = "exp-001",
            outputGen = "Exp: exp-001",
            outputExpected = "irrelevant"
        )

        val result = evaluator.evaluate(dataPoint)

        assertEquals(1.0, result.score, 0.001)
    }

    @Test
    fun `handles empty input field`() = runBlocking {
        val evaluator = RegexpEvaluator(patternTemplate = "{input}.*")
        val dataPoint = DataPoint(
            id = "test-1",
            input = "",
            outputGen = "some output",
            outputExpected = "expected",
            experimentId = "exp-1"
        )

        val result = evaluator.evaluate(dataPoint)

        assertEquals(1.0, result.score, 0.001)
    }

    @Test
    fun `handles empty outputExpected field`() = runBlocking {
        val evaluator = RegexpEvaluator()
        val dataPoint = DataPoint(
            id = "test-1",
            input = "input",
            outputGen = "output",
            outputExpected = "",
            experimentId = "exp-1"
        )

        val result = evaluator.evaluate(dataPoint)

        assertEquals(1.0, result.score, 0.001)
    }

    @Test
    fun `handles null runDatetime field`() = runBlocking {
        val evaluator = RegexpEvaluator(patternTemplate = "{runDatetime}.*")
        val dataPoint = DataPoint(
            id = "test-1",
            input = "input",
            outputGen = "output",
            outputExpected = "expected",
            experimentId = "exp-1",
            runDatetime = null
        )

        val result = evaluator.evaluate(dataPoint)

        assertEquals(1.0, result.score, 0.001)
    }

    @Test
    fun `handles null exception field`() = runBlocking {
        val evaluator = RegexpEvaluator(patternTemplate = "{exception}.*")
        val dataPoint = DataPoint(
            id = "test-1",
            input = "input",
            outputGen = "output",
            outputExpected = "expected",
            experimentId = "exp-1",
            exception = null
        )

        val result = evaluator.evaluate(dataPoint)

        assertEquals(1.0, result.score, 0.001)
    }

    @Test
    fun `preserves all dataPoint fields in result`() = runBlocking {
        val evaluator = RegexpEvaluator()
        val dataPoint = createDataPoint(
            id = "id-123",
            input = "test input",
            outputGen = "generated output with expected",
            outputExpected = "expected",
            experimentId = "exp-456",
            runDatetime = "2025-01-15T10:30:00"
        )

        val result = evaluator.evaluate(dataPoint)

        assertEquals("id-123", result.id)
        assertEquals("test input", result.input)
        assertEquals("generated output with expected", result.outputGen)
        assertEquals("expected", result.outputExpected)
        assertEquals("exp-456", result.experimentId)
        assertEquals("2025-01-15T10:30:00", result.runDatetime)
    }

    @Test
    fun `sets runDatetime when null in dataPoint`() = runBlocking {
        val evaluator = RegexpEvaluator()
        val dataPoint = DataPoint(
            id = "test-1",
            input = "input",
            outputGen = "output expected",
            outputExpected = "expected",
            experimentId = "exp-1",
            runDatetime = null
        )

        val result = evaluator.evaluate(dataPoint)

        assertNotNull(result.runDatetime)
        assertFalse(result.runDatetime?.isEmpty() ?: true)
    }

    @Test
    fun `handles empty expected in default pattern`() = runBlocking {
        val evaluator = RegexpEvaluator()
        val dataPoint = DataPoint(
            id = "test-1",
            input = "input",
            outputGen = "anything",
            outputExpected = "",
            experimentId = "exp-1"
        )

        val result = evaluator.evaluate(dataPoint)

        assertEquals(1.0, result.score, 0.001)
    }

    @Test
    fun `handles empty generated output`() = runBlocking {
        val evaluator = RegexpEvaluator()
        val dataPoint = DataPoint(
            id = "test-1",
            input = "input",
            outputGen = "",
            outputExpected = "expected",
            experimentId = "exp-1"
        )

        val result = evaluator.evaluate(dataPoint)

        assertEquals(0.0, result.score, 0.001)
    }

    @Test
    fun `case sensitive matching by default`() = runBlocking {
        val evaluator = RegexpEvaluator()
        val dataPoint = createDataPoint(
            outputGen = "The result is EXPECTED",
            outputExpected = "expected"
        )

        val result = evaluator.evaluate(dataPoint)

        assertEquals(0.0, result.score, 0.001)
    }

    @Test
    fun `handles multiline output`() = runBlocking {
        val evaluator = RegexpEvaluator()
        val dataPoint = createDataPoint(
            outputGen = "Line 1\nLine 2 with expected value\nLine 3",
            outputExpected = "expected value"
        )

        val result = evaluator.evaluate(dataPoint)

        assertEquals(1.0, result.score, 0.001)
    }

    @Test
    fun `handles complex pattern with multiple placeholders`() = runBlocking {
        val evaluator = RegexpEvaluator(patternTemplate = "Input: {input}.*Output: {outputExpected}")
        val dataPoint = createDataPoint(
            input = "query",
            outputGen = "Input: query and some data Output: result",
            outputExpected = "result"
        )

        val result = evaluator.evaluate(dataPoint)

        assertEquals(1.0, result.score, 0.001)
    }

    @Test
    fun `preserves extra and raw fields as empty maps`() = runBlocking {
        val evaluator = RegexpEvaluator()
        val dataPoint = DataPoint(
            id = "test-1",
            input = "input",
            outputGen = "output expected",
            outputExpected = "expected",
            experimentId = "exp-1",
            raw = mapOf("key" to "value")
        )

        val result = evaluator.evaluate(dataPoint)

        assertTrue(result.extra.isEmpty())
        assertEquals(mapOf("key" to "value"), result.raw)
    }

    @Test
    fun `returns correct evaluator name`() = runBlocking {
        val evaluator = RegexpEvaluator()
        val dataPoint = createDataPoint(
            outputGen = "output",
            outputExpected = "expected"
        )

        val result = evaluator.evaluate(dataPoint)

        assertEquals("Regexp", result.evaluator)
    }

    @Test
    fun `handles pattern without placeholders`() = runBlocking {
        val evaluator = RegexpEvaluator(patternTemplate = "^SUCCESS$")

        val matchingDataPoint = createDataPoint(
            outputGen = "SUCCESS",
            outputExpected = "irrelevant"
        )
        val matchingResult = evaluator.evaluate(matchingDataPoint)
        assertEquals(1.0, matchingResult.score, 0.001)

        val nonMatchingDataPoint = createDataPoint(
            outputGen = "FAILURE",
            outputExpected = "irrelevant"
        )
        val nonMatchingResult = evaluator.evaluate(nonMatchingDataPoint)
        assertEquals(0.0, nonMatchingResult.score, 0.001)
    }

    private fun createDataPoint(
        id: String = "test-id",
        input: String = "test input",
        outputGen: String,
        outputExpected: String,
        experimentId: String = "exp-test",
        runDatetime: String? = "2025-01-01T00:00:00"
    ): DataPoint {
        return DataPoint(
            id = id,
            input = input,
            outputGen = outputGen,
            outputExpected = outputExpected,
            experimentId = experimentId,
            raw = emptyMap(),
            runDatetime = runDatetime,
            exception = null
        )
    }
}
