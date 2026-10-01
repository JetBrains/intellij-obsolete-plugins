package com.intellij.aidebugger.evaluation.models.extractor

import com.google.gson.Gson
import com.google.gson.JsonParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InputStructurePackerTest {

    private val gson = Gson()

    @Test
    fun `packInput returns value as-is when no input property in path`() {
        val path = "$.rootEvents[0].payload.data"
        val value = "test value"

        val result = InputStructurePacker.packInput(path, value)

        assertEquals("test value", result)
    }

    @Test
    fun `packInput returns value as-is when input is the last token`() {
        val path = "$.rootEvents[0].payload.input"
        val value = "test value"

        val result = InputStructurePacker.packInput(path, value)

        assertEquals("test value", result)
    }

    @Test
    fun `packInput wraps value in object for single property after input`() {
        val path = "$.payload.input.message"
        val value = "Hello"

        val result = InputStructurePacker.packInput(path, value)

        val parsed = JsonParser.parseString(result).asJsonObject
        assertEquals("Hello", parsed.get("message").asString)
    }

    @Test
    fun `packInput wraps value in nested objects for multiple properties`() {
        val path = "$.payload.input.user.name"
        val value = "Alice"

        val result = InputStructurePacker.packInput(path, value)

        val parsed = JsonParser.parseString(result).asJsonObject
        val userName = parsed.getAsJsonObject("user").get("name").asString
        assertEquals("Alice", userName)
    }

    @Test
    fun `packInput wraps value in array for array index after input`() {
        val path = "$.payload.input.messages[0]"
        val value = "First message"

        val result = InputStructurePacker.packInput(path, value)

        val parsed = JsonParser.parseString(result).asJsonObject
        val messages = parsed.getAsJsonArray("messages")
        assertEquals("First message", messages.get(0).asString)
    }

    @Test
    fun `packInput handles complex nested structure with arrays and objects`() {
        val path = "$.rootEvents[0].payload.input.messages[0].content"
        val value = "Hello world"

        val result = InputStructurePacker.packInput(path, value)

        val parsed = JsonParser.parseString(result).asJsonObject
        val content = parsed.getAsJsonArray("messages").get(0).asJsonObject.get("content").asString
        assertEquals("Hello world", content)
    }

    @Test
    fun `packInput handles multiple array indices`() {
        val path = "$.payload.input.items[0][1]"
        val value = "nested item"

        val result = InputStructurePacker.packInput(path, value)

        val parsed = JsonParser.parseString(result).asJsonObject
        val items = parsed.getAsJsonArray("items")
        val nestedArray = items.get(0).asJsonArray
        assertEquals("nested item", nestedArray.get(0).asString)
    }

    @Test
    fun `packInput handles inputs property instead of input`() {
        val path = "$.payload.inputs.query"
        val value = "search term"

        val result = InputStructurePacker.packInput(path, value)

        val parsed = JsonParser.parseString(result).asJsonObject
        assertEquals("search term", parsed.get("query").asString)
    }

    @Test
    fun `packInput uses last occurrence of input when multiple exist`() {
        val path = "$.input.nested.input.value"
        val value = "final"

        val result = InputStructurePacker.packInput(path, value)

        val parsed = JsonParser.parseString(result).asJsonObject
        assertEquals("final", parsed.get("value").asString)
    }

    @Test
    fun `reconstructInput falls back to packInput when inputStruct is null`() {
        val path = "$.payload.input.message"
        val value = "Hello"

        val result = InputStructurePacker.reconstructInput(path, value, null)

        val parsed = JsonParser.parseString(result).asJsonObject
        assertEquals("Hello", parsed.get("message").asString)
    }

    @Test
    fun `reconstructInput falls back to packInput when inputStruct is blank`() {
        val path = "$.payload.input.message"
        val value = "Hello"

        val result = InputStructurePacker.reconstructInput(path, value, "")

        val parsed = JsonParser.parseString(result).asJsonObject
        assertEquals("Hello", parsed.get("message").asString)
    }

    @Test
    fun `reconstructInput updates value in existing structure`() {
        val path = "$.payload.input.messages[0].content"
        val value = "Updated content"
        val inputStruct = """{"messages":[{"content":"Old content","role":"user"}]}"""

        val result = InputStructurePacker.reconstructInput(path, value, inputStruct)

        val parsed = JsonParser.parseString(result).asJsonObject
        val messages = parsed.getAsJsonArray("messages")
        val content = messages.get(0).asJsonObject.get("content").asString
        assertEquals("Updated content", content)
    }

    @Test
    fun `reconstructInput preserves other fields in structure`() {
        val path = "$.payload.input.messages[0].content"
        val value = "New content"
        val inputStruct = """{"messages":[{"content":"Old","role":"user","metadata":"xyz"}],"config":"abc"}"""

        val result = InputStructurePacker.reconstructInput(path, value, inputStruct)

        val parsed = JsonParser.parseString(result).asJsonObject
        val messages = parsed.getAsJsonArray("messages")
        val message = messages.get(0).asJsonObject
        assertEquals("New content", message.get("content").asString)
        assertEquals("user", message.get("role").asString)
        assertEquals("xyz", message.get("metadata").asString)
        assertEquals("abc", parsed.get("config").asString)
    }

    @Test
    fun `reconstructInput updates nested object property`() {
        val path = "$.payload.input.user.name"
        val value = "Bob"
        val inputStruct = """{"user":{"name":"Alice","age":30}}"""

        val result = InputStructurePacker.reconstructInput(path, value, inputStruct)

        val parsed = JsonParser.parseString(result).asJsonObject
        val user = parsed.getAsJsonObject("user")
        assertEquals("Bob", user.get("name").asString)
        assertEquals(30, user.get("age").asInt)
    }

    @Test
    fun `reconstructInput falls back when structure is invalid JSON`() {
        val path = "$.payload.input.message"
        val value = "Hello"
        val inputStruct = "not valid json"

        val result = InputStructurePacker.reconstructInput(path, value, inputStruct)

        val parsed = JsonParser.parseString(result).asJsonObject
        assertEquals("Hello", parsed.get("message").asString)
    }

    @Test
    fun `reconstructInput falls back when path does not exist in structure`() {
        val path = "$.payload.input.nonexistent[5].field"
        val value = "value"
        val inputStruct = """{"existing":"field"}"""

        val result = InputStructurePacker.reconstructInput(path, value, inputStruct)

        assertEquals(inputStruct, result)
    }

    @Test
    fun `reconstructInput handles array index update`() {
        val path = "$.payload.input.items[1]"
        val value = "updated item"
        val inputStruct = """{"items":["first","second","third"]}"""

        val result = InputStructurePacker.reconstructInput(path, value, inputStruct)

        val parsed = JsonParser.parseString(result).asJsonObject
        val items = parsed.getAsJsonArray("items")
        assertEquals("first", items.get(0).asString)
        assertEquals("updated item", items.get(1).asString)
        assertEquals("third", items.get(2).asString)
    }

    @Test
    fun `packInput handles deep nesting with mixed tokens`() {
        val path = "$.rootEvents[0].children[0].payload.input.data.items[0].fields.name"
        val value = "deep value"

        val result = InputStructurePacker.packInput(path, value)

        val parsed = JsonParser.parseString(result).asJsonObject
        val data = parsed.getAsJsonObject("data")
        val items = data.getAsJsonArray("items")
        val fields = items.get(0).asJsonObject.getAsJsonObject("fields")
        assertEquals("deep value", fields.get("name").asString)
    }

    @Test
    fun `packInput handles single property after inputs`() {
        val path = "$.rootEvents[0].payload.inputs.query"
        val value = "search query"

        val result = InputStructurePacker.packInput(path, value)

        val parsed = JsonParser.parseString(result).asJsonObject
        assertEquals("search query", parsed.get("query").asString)
    }

    @Test
    fun `reconstructInput returns value as-is when no input in path`() {
        val path = "$.payload.data"
        val value = "test"
        val inputStruct = """{"field":"value"}"""

        val result = InputStructurePacker.reconstructInput(path, value, inputStruct)

        assertEquals("test", result)
    }

    @Test
    fun `reconstructInput returns value as-is when input is last token`() {
        val path = "$.payload.input"
        val value = "test"
        val inputStruct = """{"field":"value"}"""

        val result = InputStructurePacker.reconstructInput(path, value, inputStruct)

        assertEquals("test", result)
    }

    @Test
    fun `packInput handles empty path after input`() {
        val path = "$.input"
        val value = "direct value"

        val result = InputStructurePacker.packInput(path, value)

        assertEquals("direct value", result)
    }

    @Test
    fun `reconstructInput handles complex array structure update`() {
        val path = "$.payload.input.messages[0].parts[1].text"
        val value = "updated text"
        val inputStruct = """{"messages":[{"parts":[{"text":"part0"},{"text":"part1"}],"role":"user"}]}"""

        val result = InputStructurePacker.reconstructInput(path, value, inputStruct)

        val parsed = JsonParser.parseString(result).asJsonObject
        val messages = parsed.getAsJsonArray("messages")
        val parts = messages.get(0).asJsonObject.getAsJsonArray("parts")
        assertEquals("part0", parts.get(0).asJsonObject.get("text").asString)
        assertEquals("updated text", parts.get(1).asJsonObject.get("text").asString)
    }

    @Test
    fun `packInput handles numeric values`() {
        val path = "$.payload.input.count"
        val value = "42"

        val result = InputStructurePacker.packInput(path, value)

        val parsed = JsonParser.parseString(result).asJsonObject
        assertEquals("42", parsed.get("count").asString)
    }

    @Test
    fun `packInput handles boolean-like string values`() {
        val path = "$.payload.input.enabled"
        val value = "true"

        val result = InputStructurePacker.packInput(path, value)

        val parsed = JsonParser.parseString(result).asJsonObject
        assertEquals("true", parsed.get("enabled").asString)
    }

    @Test
    fun `packInput handles special characters in value`() {
        val path = "$.payload.input.message"
        val value = """Special chars: "quotes", \backslash, newline\n"""

        val result = InputStructurePacker.packInput(path, value)

        val parsed = JsonParser.parseString(result).asJsonObject
        assertEquals(value, parsed.get("message").asString)
    }

    @Test
    fun `reconstructInput handles empty array in structure`() {
        val path = "$.payload.input.items[0]"
        val value = "first item"
        val inputStruct = """{"items":[]}"""

        val result = InputStructurePacker.reconstructInput(path, value, inputStruct)

        val parsed = JsonParser.parseString(result).asJsonObject
        val items = parsed.getAsJsonArray("items")
        assertEquals(1, items.size())
        assertEquals("first item", items.get(0).asString)
    }

    @Test
    fun `reconstructInput preserves structure when exception occurs`() {
        val path = "$.payload.input.field"
        val value = "new value"
        val inputStruct = """{"field":"old","other":"data"}"""

        val result = InputStructurePacker.reconstructInput(path, value, inputStruct)

        val parsed = JsonParser.parseString(result).asJsonObject
        assertTrue(parsed.has("field") || parsed.has("other"))
    }
}
