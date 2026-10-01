package com.intellij.aidebugger.python.serialization

import com.intellij.aidebugger.common.models.entities.EventStackFrame
import com.intellij.aidebugger.common.models.entities.PayloadKey
import com.intellij.aidebugger.common.models.entities.SimpleGraph
import com.intellij.aidebugger.common.models.entities.TraceLlmModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CommonTraceEventDeserializerTest {

    @Test
    fun `deserializePayload should return unchanged map when no special keys present`() {
        val inputPayload = mapOf(
            "someKey" to "someValue",
            "anotherKey" to 42,
            "listKey" to listOf("a", "b", "c")
        )

        val result = deserializePayload(inputPayload)

        assertEquals(inputPayload, result)
    }

    @Test
    fun `deserializePayload should parse graph when PayloadKey Graph is present`() {
        val graphData = mapOf(
            "nodes" to listOf(
                mapOf("id" to "node1", "type" to "start", "data" to mapOf("key" to "value")),
                mapOf("id" to "node2", "type" to "end", "data" to null)
            ),
            "edges" to listOf(
                mapOf("source" to "node1", "target" to "node2")
            )
        )

        val inputPayload = mapOf(
            PayloadKey.Graph to graphData,
            "otherKey" to "otherValue"
        )

        val result = deserializePayload(inputPayload)

        assertTrue(result[PayloadKey.Graph] is SimpleGraph)
        val graph = result[PayloadKey.Graph] as SimpleGraph

        assertEquals(2, graph.nodes.size)
        assertEquals(1, graph.edges.size)

        assertEquals("node1", graph.nodes[0].id)
        assertEquals("start", graph.nodes[0].type)
        assertEquals(mapOf("key" to "value"), graph.nodes[0].data)

        assertEquals("node2", graph.nodes[1].id)
        assertEquals("end", graph.nodes[1].type)
        assertNull(graph.nodes[1].data)

        assertEquals("node1", graph.edges[0].source)
        assertEquals("node2", graph.edges[0].target)

        assertEquals("otherValue", result["otherKey"])
    }

    @Test
    fun `deserializePayload should parse stack trace when PayloadKey StackTrace is present`() {
        val stackTraceData = listOf(
            mapOf("filePath" to "/path/to/file1.py", "lineNumber" to 42, "functionName" to "function1"),
            mapOf("filePath" to "/path/to/file2.py", "lineNumber" to "123", "functionName" to "function2"),
            mapOf("filePath" to "/path/to/file3.py", "lineNumber" to 456.0, "functionName" to "function3")
        )

        val inputPayload = mapOf(
            PayloadKey.StackTrace to stackTraceData,
            "otherKey" to "otherValue"
        )

        val result = deserializePayload(inputPayload)

        assertTrue(result[PayloadKey.StackTrace] is List<*>)
        @Suppress("UNCHECKED_CAST")
        val stackTrace = result[PayloadKey.StackTrace] as List<EventStackFrame>

        assertEquals(3, stackTrace.size)

        assertEquals("/path/to/file1.py", stackTrace[0].filePath)
        assertEquals(42, stackTrace[0].lineNumber)
        assertEquals("function1", stackTrace[0].functionName)

        assertEquals("/path/to/file2.py", stackTrace[1].filePath)
        assertEquals(123, stackTrace[1].lineNumber)
        assertEquals("function2", stackTrace[1].functionName)

        assertEquals("/path/to/file3.py", stackTrace[2].filePath)
        assertEquals(456, stackTrace[2].lineNumber)
        assertEquals("function3", stackTrace[2].functionName)

        assertEquals("otherValue", result["otherKey"])
    }

    @Test
    fun `deserializePayload should handle both graph and stack trace in same payload`() {
        val graphData = mapOf(
            "nodes" to listOf(mapOf("id" to "node1", "type" to "test", "data" to null)),
            "edges" to emptyList<Map<String, String>>()
        )

        val stackTraceData = listOf(
            mapOf("filePath" to "/test.py", "lineNumber" to 1, "functionName" to "test_func")
        )

        val inputPayload = mapOf(
            PayloadKey.Graph to graphData,
            PayloadKey.StackTrace to stackTraceData,
            "normalKey" to "normalValue"
        )

        val result = deserializePayload(inputPayload)

        assertTrue(result[PayloadKey.Graph] is SimpleGraph)
        assertTrue(result[PayloadKey.StackTrace] is List<*>)
        assertEquals("normalValue", result["normalKey"])
    }

    @Test
    fun `deserializePayload should handle malformed graph data gracefully`() {
        val malformedGraphData = mapOf(
            "nodes" to "not a list",
            "edges" to null
        )

        val inputPayload = mapOf(
            PayloadKey.Graph to malformedGraphData
        )

        val result = deserializePayload(inputPayload)

        assertNull(result[PayloadKey.Graph])
    }

    @Test
    fun `deserializePayload should handle malformed stack trace data gracefully`() {
        val malformedStackTraceData = listOf(
            mapOf("filePath" to "/test.py"), // missing lineNumber and functionName
            mapOf("lineNumber" to 42, "functionName" to "test"), // missing filePath
            mapOf("filePath" to "/test2.py", "lineNumber" to "invalid", "functionName" to "test2"), // invalid lineNumber
            "not a map" // completely wrong type
        )

        val inputPayload = mapOf(
            PayloadKey.StackTrace to malformedStackTraceData
        )

        val result = deserializePayload(inputPayload)

        assertTrue(result[PayloadKey.StackTrace] is List<*>)
        @Suppress("UNCHECKED_CAST")
        val stackTrace = result[PayloadKey.StackTrace] as List<EventStackFrame>

        // Should only contain valid entries (none in this case)
        assertEquals(0, stackTrace.size)
    }

    @Test
    fun `deserializePayload should handle empty graph data`() {
        val emptyGraphData = mapOf(
            "nodes" to emptyList<Map<String, Any>>(),
            "edges" to emptyList<Map<String, Any>>()
        )

        val inputPayload = mapOf(
            PayloadKey.Graph to emptyGraphData
        )

        val result = deserializePayload(inputPayload)

        assertTrue(result[PayloadKey.Graph] is SimpleGraph)
        val graph = result[PayloadKey.Graph] as SimpleGraph

        assertEquals(0, graph.nodes.size)
        assertEquals(0, graph.edges.size)
    }

    @Test
    fun `deserializePayload should handle empty stack trace data`() {
        val emptyStackTraceData = emptyList<Map<String, Any>>()

        val inputPayload = mapOf(
            PayloadKey.StackTrace to emptyStackTraceData
        )

        val result = deserializePayload(inputPayload)

        assertTrue(result[PayloadKey.StackTrace] is List<*>)
        @Suppress("UNCHECKED_CAST")
        val stackTrace = result[PayloadKey.StackTrace] as List<EventStackFrame>

        assertEquals(0, stackTrace.size)
    }

    @Test
    fun `deserializePayload should handle null values for special keys`() {
        val inputPayload = mapOf(
            PayloadKey.Graph to null,
            PayloadKey.StackTrace to null,
            "normalKey" to "normalValue"
        )

        val result = deserializePayload(inputPayload)

        assertNull(result[PayloadKey.Graph])
        assertNull(result[PayloadKey.StackTrace])
        assertEquals("normalValue", result["normalKey"])
    }

    @Test
    fun `deserializePayload should handle graph with missing required fields`() {
        val graphData = mapOf(
            "nodes" to listOf(
                mapOf("id" to "node1"), // missing type and data
                mapOf("type" to "test"), // missing id
                mapOf("data" to "some data") // missing id
            ),
            "edges" to listOf(
                mapOf("source" to "node1"), // missing target
                mapOf("target" to "node2"), // missing source
                mapOf("source" to "node1", "target" to "node2") // valid edge
            )
        )

        val inputPayload = mapOf(
            PayloadKey.Graph to graphData
        )

        val result = deserializePayload(inputPayload)

        assertTrue(result[PayloadKey.Graph] is SimpleGraph)
        val graph = result[PayloadKey.Graph] as SimpleGraph

        // Should only contain valid nodes and edges
        assertEquals(1, graph.nodes.size) // only the first node with id "node1"
        assertEquals("node1", graph.nodes[0].id)
        assertNull(graph.nodes[0].type)
        assertNull(graph.nodes[0].data)

        assertEquals(1, graph.edges.size) // only the valid edge
        assertEquals("node1", graph.edges[0].source)
        assertEquals("node2", graph.edges[0].target)
    }

    @Test
    fun `deserializePayload should handle wrong type for graph key`() {
        val inputPayload = mapOf(
            PayloadKey.Graph to "not a map",
            PayloadKey.StackTrace to "not a list"
        )

        val result = deserializePayload(inputPayload)

        assertNull(result[PayloadKey.Graph])
        assertNull(result[PayloadKey.StackTrace])
    }

    @Test
    fun `deserializePayload should handle partially valid stack trace entries`() {
        val stackTraceData = listOf(
            mapOf("filePath" to "/valid.py", "lineNumber" to 42, "functionName" to "valid_func"),
            mapOf("filePath" to "/invalid.py", "lineNumber" to "not_a_number", "functionName" to "invalid_func"),
            mapOf("filePath" to "/another_valid.py", "lineNumber" to 123, "functionName" to "another_valid_func"),
            42 // completely wrong type
        )

        val inputPayload = mapOf(
            PayloadKey.StackTrace to stackTraceData
        )

        val result = deserializePayload(inputPayload)

        assertTrue(result[PayloadKey.StackTrace] is List<*>)
        @Suppress("UNCHECKED_CAST")
        val stackTrace = result[PayloadKey.StackTrace] as List<EventStackFrame>

        // Should only contain valid entries
        assertEquals(2, stackTrace.size)

        assertEquals("/valid.py", stackTrace[0].filePath)
        assertEquals(42, stackTrace[0].lineNumber)
        assertEquals("valid_func", stackTrace[0].functionName)

        assertEquals("/another_valid.py", stackTrace[1].filePath)
        assertEquals(123, stackTrace[1].lineNumber)
        assertEquals("another_valid_func", stackTrace[1].functionName)
    }

    @Test
    fun `deserializePayload should parse model when PayloadKey Model is present`() {
        val modelData = mapOf(
            "model" to "gpt-4",
            "model_name" to "GPT-4",
            "temperature" to 0.7,
            "tools" to listOf(
                mapOf("name" to "calculator", "description" to "Performs calculations"),
                mapOf("name" to "search", "description" to "Searches the web")
            )
        )

        val inputPayload = mapOf(
            PayloadKey.Model to modelData,
            "otherKey" to "otherValue"
        )

        val result = deserializePayload(inputPayload)

        assertTrue(result[PayloadKey.Model] is TraceLlmModel)
        val model = result[PayloadKey.Model] as TraceLlmModel

        assertEquals("gpt-4", model.model)
        assertEquals("GPT-4", model.modelName)
        assertEquals(0.7, model.temperature!!, 0.001)
        assertEquals(2, model.tools.size)

        assertEquals("calculator", model.tools[0].name)
        assertEquals("Performs calculations", model.tools[0].description)

        assertEquals("search", model.tools[1].name)
        assertEquals("Searches the web", model.tools[1].description)

        assertEquals("otherValue", result["otherKey"])
    }

    @Test
    fun `deserializePayload should parse model with camelCase field names`() {
        val modelData = mapOf(
            "model" to "claude-3",
            "modelName" to "Claude-3", // camelCase instead of snake_case
            "temperature" to 0.5,
            "tools" to emptyList<Map<String, String>>()
        )

        val inputPayload = mapOf(
            PayloadKey.Model to modelData
        )

        val result = deserializePayload(inputPayload)

        assertTrue(result[PayloadKey.Model] is TraceLlmModel)
        val model = result[PayloadKey.Model] as TraceLlmModel

        assertEquals("claude-3", model.model)
        assertEquals("Claude-3", model.modelName)
        assertEquals(0.5, model.temperature!!, 0.001)
        assertEquals(0, model.tools.size)
    }

    @Test
    fun `deserializePayload should handle model with nullable fields`() {
        val modelData = mapOf(
            "model" to null,
            "model_name" to null,
            "temperature" to null,
            "tools" to emptyList<Map<String, String>>()
        )

        val inputPayload = mapOf(
            PayloadKey.Model to modelData
        )

        val result = deserializePayload(inputPayload)

        assertTrue(result[PayloadKey.Model] is TraceLlmModel)
        val model = result[PayloadKey.Model] as TraceLlmModel

        assertNull(model.model)
        assertNull(model.modelName)
        assertNull(model.temperature)
        assertEquals(0, model.tools.size)
    }

    @Test
    fun `deserializePayload should handle model with missing fields`() {
        val modelData = mapOf(
            "model" to "gpt-3.5-turbo"
            // missing model_name, temperature, and tools
        )

        val inputPayload = mapOf(
            PayloadKey.Model to modelData
        )

        val result = deserializePayload(inputPayload)

        assertTrue(result[PayloadKey.Model] is TraceLlmModel)
        val model = result[PayloadKey.Model] as TraceLlmModel

        assertEquals("gpt-3.5-turbo", model.model)
        assertNull(model.modelName)
        assertNull(model.temperature)
        assertEquals(0, model.tools.size) // tools defaults to empty list
    }

    @Test
    fun `deserializePayload should handle model with string temperature`() {
        val modelData = mapOf(
            "model" to "gpt-4",
            "temperature" to "0.8", // temperature as string
            "tools" to emptyList<Map<String, String>>()
        )

        val inputPayload = mapOf(
            PayloadKey.Model to modelData
        )

        val result = deserializePayload(inputPayload)

        assertTrue(result[PayloadKey.Model] is TraceLlmModel)
        val model = result[PayloadKey.Model] as TraceLlmModel

        assertEquals("gpt-4", model.model)
        assertEquals(0.8, model.temperature!!, 0.001)
    }

    @Test
    fun `deserializePayload should handle model with invalid string temperature`() {
        val modelData = mapOf(
            "model" to "gpt-4",
            "temperature" to "not_a_number",
            "tools" to emptyList<Map<String, String>>()
        )

        val inputPayload = mapOf(
            PayloadKey.Model to modelData
        )

        val result = deserializePayload(inputPayload)

        assertTrue(result[PayloadKey.Model] is TraceLlmModel)
        val model = result[PayloadKey.Model] as TraceLlmModel

        assertEquals("gpt-4", model.model)
        assertNull(model.temperature)
    }

    @Test
    fun `deserializePayload should handle model with malformed tools`() {
        val modelData = mapOf(
            "model" to "gpt-4",
            "tools" to listOf(
                mapOf("name" to "valid_tool", "description" to "Valid tool"),
                mapOf("description" to "Missing name tool"), // missing name
                mapOf("name" to "missing_description_tool"), // missing description (should default to empty)
                "not_a_map", // invalid tool entry
                mapOf("name" to "another_valid", "description" to "Another valid tool")
            )
        )

        val inputPayload = mapOf(
            PayloadKey.Model to modelData
        )

        val result = deserializePayload(inputPayload)

        assertTrue(result[PayloadKey.Model] is TraceLlmModel)
        val model = result[PayloadKey.Model] as TraceLlmModel

        assertEquals("gpt-4", model.model)
        assertEquals(3, model.tools.size) // only valid tools

        assertEquals("valid_tool", model.tools[0].name)
        assertEquals("Valid tool", model.tools[0].description)

        assertEquals("missing_description_tool", model.tools[1].name)
        assertEquals("", model.tools[1].description) // defaults to empty string

        assertEquals("another_valid", model.tools[2].name)
        assertEquals("Another valid tool", model.tools[2].description)
    }

    @Test
    fun `deserializePayload should handle malformed model data gracefully`() {
        val inputPayload = mapOf(
            PayloadKey.Model to "not a map"
        )

        val result = deserializePayload(inputPayload)

        assertNull(result[PayloadKey.Model])
    }

    @Test
    fun `deserializePayload should handle null model data`() {
        val inputPayload = mapOf(
            PayloadKey.Model to null
        )

        val result = deserializePayload(inputPayload)

        assertNull(result[PayloadKey.Model])
    }

    @Test
    fun `deserializePayload should handle all special keys together`() {
        val graphData = mapOf(
            "nodes" to listOf(mapOf("id" to "node1", "type" to "test", "data" to null)),
            "edges" to emptyList<Map<String, String>>()
        )

        val stackTraceData = listOf(
            mapOf("filePath" to "/test.py", "lineNumber" to 1, "functionName" to "test_func")
        )

        val modelData = mapOf(
            "model" to "gpt-4",
            "model_name" to "GPT-4",
            "temperature" to 0.7,
            "tools" to listOf(
                mapOf("name" to "calculator", "description" to "Performs calculations")
            )
        )

        val inputPayload = mapOf(
            PayloadKey.Graph to graphData,
            PayloadKey.StackTrace to stackTraceData,
            PayloadKey.Model to modelData,
            "normalKey" to "normalValue"
        )

        val result = deserializePayload(inputPayload)

        assertTrue(result[PayloadKey.Graph] is SimpleGraph)
        assertTrue(result[PayloadKey.StackTrace] is List<*>)
        assertTrue(result[PayloadKey.Model] is TraceLlmModel)
        assertEquals("normalValue", result["normalKey"])

        val model = result[PayloadKey.Model] as TraceLlmModel
        assertEquals("gpt-4", model.model)
        assertEquals("GPT-4", model.modelName)
        assertEquals(0.7, model.temperature!!, 0.001)
        assertEquals(1, model.tools.size)
        assertEquals("calculator", model.tools[0].name)
    }
}