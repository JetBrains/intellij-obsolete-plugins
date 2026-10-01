package com.intellij.aidebugger.evaluation.models.extractor

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DebuggerTracesExtractorTest {

    private val gson = Gson()

    @Test
    fun `extract returns DataPoint with outputGen from simple path`() {
        val extractor = DebuggerTracesExtractor("$.output")
        val raw = mapOf("output" to "generated result")
        val fallback = FallbackContext(
            id = "test-1",
            input = "test input",
            outputExpected = "expected output",
            experimentId = "exp-001"
        )

        val result = extractor.extract(raw, fallback)

        assertEquals("test-1", result.id)
        assertEquals("test input", result.input)
        assertEquals("generated result", result.outputGen)
        assertEquals("expected output", result.outputExpected)
        assertEquals("exp-001", result.experimentId)
        assertEquals(raw, result.raw)
    }

    @Test
    fun `extract returns DataPoint with nested path`() {
        val extractor = DebuggerTracesExtractor("$.rootEvents[0].payload.output")
        val raw = mapOf(
            "rootEvents" to listOf(
                mapOf(
                    "payload" to mapOf("output" to "nested output")
                )
            )
        )
        val fallback = FallbackContext(
            id = "test-2",
            input = "input",
            outputExpected = null,
            experimentId = "exp-002"
        )

        val result = extractor.extract(raw, fallback)

        assertEquals("nested output", result.outputGen)
        assertEquals("", result.outputExpected)
    }

    @Test
    fun `extract returns empty string when path not found`() {
        val extractor = DebuggerTracesExtractor("$.nonexistent.path")
        val raw = mapOf("other" to "data")
        val fallback = FallbackContext(
            id = "test-3",
            input = "input",
            outputExpected = "expected",
            experimentId = "exp-003"
        )

        val result = extractor.extract(raw, fallback)

        assertEquals("", result.outputGen)
        assertEquals("test-3", result.id)
        assertEquals("input", result.input)
    }

    @Test
    fun `extract handles array index in path`() {
        val extractor = DebuggerTracesExtractor("$.messages[0].content")
        val raw = mapOf(
            "messages" to listOf(
                mapOf("content" to "first message"),
                mapOf("content" to "second message")
            )
        )
        val fallback = FallbackContext(
            id = "test-4",
            input = "query",
            outputExpected = "answer",
            experimentId = "exp-004"
        )

        val result = extractor.extract(raw, fallback)

        assertEquals("first message", result.outputGen)
    }

    @Test
    fun `extract handles deep nested structure`() {
        val extractor = DebuggerTracesExtractor("$.rootEvents[0].children[1].payload.outputs.messages[0].text")
        val raw = mapOf(
            "rootEvents" to listOf(
                mapOf(
                    "children" to listOf(
                        mapOf("id" to "1"),
                        mapOf(
                            "payload" to mapOf(
                                "outputs" to mapOf(
                                    "messages" to listOf(
                                        mapOf("text" to "deep output")
                                    )
                                )
                            )
                        )
                    )
                )
            )
        )
        val fallback = FallbackContext(
            id = "test-5",
            input = "deep query",
            outputExpected = null,
            experimentId = "exp-005"
        )

        val result = extractor.extract(raw, fallback)

        assertEquals("deep output", result.outputGen)
    }

    @Test
    fun `extract preserves fallback context values`() {
        val extractor = DebuggerTracesExtractor("$.result")
        val raw = mapOf("result" to "extracted")
        val fallback = FallbackContext(
            id = "custom-id",
            input = "custom input",
            outputExpected = "custom expected",
            experimentId = "custom-exp"
        )

        val result = extractor.extract(raw, fallback)

        assertEquals("custom-id", result.id)
        assertEquals("custom input", result.input)
        assertEquals("custom expected", result.outputExpected)
        assertEquals("custom-exp", result.experimentId)
    }

    @Test
    fun `extract returns empty experimentId when fallback is null`() {
        val extractor = DebuggerTracesExtractor("$.output")
        val raw = mapOf("output" to "result")
        val fallback = FallbackContext(
            id = "test-6",
            input = "input",
            outputExpected = null,
            experimentId = null
        )

        val result = extractor.extract(raw, fallback)

        assertEquals("", result.experimentId)
    }

    @Test
    fun `extract handles numeric values in output path`() {
        val extractor = DebuggerTracesExtractor("$.count")
        val raw = mapOf("count" to 42)
        val fallback = FallbackContext(
            id = "test-7",
            input = "input",
            outputExpected = "expected",
            experimentId = "exp-007"
        )

        val result = extractor.extract(raw, fallback)

        assertEquals("42", result.outputGen)
    }

    @Test
    fun `extract handles boolean values in output path`() {
        val extractor = DebuggerTracesExtractor("$.success")
        val raw = mapOf("success" to true)
        val fallback = FallbackContext(
            id = "test-8",
            input = "input",
            outputExpected = "expected",
            experimentId = "exp-008"
        )

        val result = extractor.extract(raw, fallback)

        assertEquals("true", result.outputGen)
    }

    @Test
    fun `extract handles object as output path result`() {
        val extractor = DebuggerTracesExtractor("$.data")
        val raw = mapOf("data" to mapOf("key" to "value", "count" to 10))
        val fallback = FallbackContext(
            id = "test-9",
            input = "input",
            outputExpected = "expected",
            experimentId = "exp-009"
        )

        val result = extractor.extract(raw, fallback)

        assertTrue(result.outputGen.contains("key"))
        assertTrue(result.outputGen.contains("value"))
    }

    @Test
    fun `extract handles array as output path result`() {
        val extractor = DebuggerTracesExtractor("$.items")
        val raw = mapOf("items" to listOf("item1", "item2", "item3"))
        val fallback = FallbackContext(
            id = "test-10",
            input = "input",
            outputExpected = "expected",
            experimentId = "exp-010"
        )

        val result = extractor.extract(raw, fallback)

        assertTrue(result.outputGen.contains("item1"))
        assertTrue(result.outputGen.contains("item2"))
    }

    @Test
    fun `extract handles empty raw map`() {
        val extractor = DebuggerTracesExtractor("$.output")
        val raw = emptyMap<String, Any?>()
        val fallback = FallbackContext(
            id = "test-11",
            input = "input",
            outputExpected = "expected",
            experimentId = "exp-011"
        )

        val result = extractor.extract(raw, fallback)

        assertEquals("", result.outputGen)
        assertEquals(raw, result.raw)
    }

    @Test
    fun `extract handles null values in raw map`() {
        val extractor = DebuggerTracesExtractor("$.output")
        val raw = mapOf("output" to null)
        val fallback = FallbackContext(
            id = "test-12",
            input = "input",
            outputExpected = "expected",
            experimentId = "exp-012"
        )

        val result = extractor.extract(raw, fallback)

        assertEquals("", result.outputGen)
    }

    @Test
    fun `extract handles root dollar path`() {
        val extractor = DebuggerTracesExtractor("$")
        val raw = mapOf("key" to "value")
        val fallback = FallbackContext(
            id = "test-13",
            input = "input",
            outputExpected = "expected",
            experimentId = "exp-013"
        )

        val result = extractor.extract(raw, fallback)

        assertTrue(result.outputGen.isNotEmpty())
    }

    @Test
    fun `extract handles negative array index`() {
        val extractor = DebuggerTracesExtractor("$.messages[-1].content")
        val raw = mapOf(
            "messages" to listOf(
                mapOf("content" to "first"),
                mapOf("content" to "second"),
                mapOf("content" to "last")
            )
        )
        val fallback = FallbackContext(
            id = "test-14",
            input = "input",
            outputExpected = "expected",
            experimentId = "exp-014"
        )

        val result = extractor.extract(raw, fallback)

        assertEquals("last", result.outputGen)
    }

    @Test
    fun `extract handles complex nested arrays`() {
        val extractor = DebuggerTracesExtractor("$.data[0][1].value")
        val raw = mapOf(
            "data" to listOf(
                listOf(
                    mapOf("value" to "0-0"),
                    mapOf("value" to "0-1")
                )
            )
        )
        val fallback = FallbackContext(
            id = "test-15",
            input = "input",
            outputExpected = "expected",
            experimentId = "exp-015"
        )

        val result = extractor.extract(raw, fallback)

        assertEquals("0-1", result.outputGen)
    }

    @Test
    fun `extract preserves raw map exactly as provided`() {
        val extractor = DebuggerTracesExtractor("$.output")
        val raw = mapOf(
            "output" to "result",
            "metadata" to mapOf("timestamp" to 12345L),
            "error" to null,
            "log" to "execution log"
        )
        val fallback = FallbackContext(
            id = "test-16",
            input = "input",
            outputExpected = "expected",
            experimentId = "exp-016"
        )

        val result = extractor.extract(raw, fallback)

        assertEquals(raw, result.raw)
        assertEquals("result", result.outputGen)
    }

    @Test
    fun `extract with path to string containing json`() {
        val extractor = DebuggerTracesExtractor("$.response")
        val raw = mapOf("response" to """{"nested": "json"}""")
        val fallback = FallbackContext(
            id = "test-17",
            input = "input",
            outputExpected = "expected",
            experimentId = "exp-017"
        )

        val result = extractor.extract(raw, fallback)

        assertEquals("""{"nested": "json"}""", result.outputGen)
    }

    @Test
    fun `extract handles special characters in string output`() {
        val extractor = DebuggerTracesExtractor("$.message")
        val raw = mapOf("message" to "Output with \"quotes\" and \n newlines")
        val fallback = FallbackContext(
            id = "test-18",
            input = "input",
            outputExpected = "expected",
            experimentId = "exp-018"
        )

        val result = extractor.extract(raw, fallback)

        assertEquals("Output with \"quotes\" and \n newlines", result.outputGen)
    }

    @Test
    fun `extract handles mixed types in array`() {
        val extractor = DebuggerTracesExtractor("$.values[2]")
        val raw = mapOf("values" to listOf("string", 123, true, null))
        val fallback = FallbackContext(
            id = "test-19",
            input = "input",
            outputExpected = "expected",
            experimentId = "exp-019"
        )

        val result = extractor.extract(raw, fallback)

        assertEquals("true", result.outputGen)
    }

    @Test
    fun `extract handles path with multiple array accesses`() {
        val extractor = DebuggerTracesExtractor("$.events[0].messages[1].parts[0]")
        val raw = mapOf(
            "events" to listOf(
                mapOf(
                    "messages" to listOf(
                        mapOf("parts" to listOf("a", "b")),
                        mapOf("parts" to listOf("target", "other"))
                    )
                )
            )
        )
        val fallback = FallbackContext(
            id = "test-20",
            input = "input",
            outputExpected = "expected",
            experimentId = "exp-020"
        )

        val result = extractor.extract(raw, fallback)

        assertEquals("target", result.outputGen)
    }
}
