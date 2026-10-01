package com.intellij.aidebugger.common.models.data

import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.nio.file.Files

class JsonDatasetReaderTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun `reads traces dataset format with items`() {
        val json = """{
        "meta": {},
        "items": [
            {"id": 1, "input": "2+2", "output": "4"},
            {"id": 2, "input": "3*3", "output": "9"}
        ]
    }"""
        val file = createJsonFile(json)

        val result = JsonDatasetReader.readJson(file)

        assertEquals(2, result.size)
        assertEquals("2+2" to "4", result[0])
        assertEquals("3*3" to "9", result[1])
    }

    @Test
    fun `reads simple array of objects`() {
        val json = """[
            {"input": "hello world", "expectedOutput": "greeting"},
            {"input": "goodbye", "expectedOutput": "farewell"}
        ]"""
        val file = createJsonFile(json)

        val result = JsonDatasetReader.readJson(file)

        assertEquals(2, result.size)
        assertEquals("hello world" to "greeting", result[0])
        assertEquals("goodbye" to "farewell", result[1])
    }

    @Test
    fun `handles field aliases`() {
        val json = """[
            {"prompt": "what is AI?", "expected": "artificial intelligence"},
            {"question": "define ML", "output": "machine learning"},
            {"input": "explain NN", "expectedOutput": "neural network"}
        ]"""
        val file = createJsonFile(json)

        val result = JsonDatasetReader.readJson(file)

        assertEquals(3, result.size)
        assertEquals("what is AI?" to "artificial intelligence", result[0])
        assertEquals("define ML" to "machine learning", result[1])
        assertEquals("explain NN" to "neural network", result[2])
    }

    @Test
    fun `reads array of strings`() {
        val json = """["analyze this code", "refactor that function", "optimize performance"]"""
        val file = createJsonFile(json)

        val result = JsonDatasetReader.readJson(file)

        assertEquals(3, result.size)
        assertEquals("analyze this code" to "", result[0])
        assertEquals("refactor that function" to "", result[1])
        assertEquals("optimize performance" to "", result[2])
    }

    @Test
    fun `reads columnar format`() {
        val json = """{
            "input": ["debug error", "fix bug", "test code"],
            "expectedOutput": ["find issue", "patch fix", "run tests"]
        }"""
        val file = createJsonFile(json)

        val result = JsonDatasetReader.readJson(file)

        assertEquals(3, result.size)
        assertEquals("debug error" to "find issue", result[0])
        assertEquals("fix bug" to "patch fix", result[1])
        assertEquals("test code" to "run tests", result[2])
    }

    @Test
    fun `handles mismatched column lengths`() {
        val json = """{
            "input": ["first", "second", "third"],
            "expected": ["uno"]
        }"""
        val file = createJsonFile(json)

        val result = JsonDatasetReader.readJson(file)

        assertEquals(3, result.size)
        assertEquals("first" to "uno", result[0])
        assertEquals("second" to "", result[1])
        assertEquals("third" to "", result[2])
    }

    @Test
    fun `reads indexed object format`() {
        val json = """{
            "0": "first prompt",
            "1": "second prompt",
            "2": "third prompt"
        }"""
        val file = createJsonFile(json)

        val result = JsonDatasetReader.readJson(file)

        assertEquals(3, result.size)
        assertEquals("first prompt" to "", result[0])
        assertEquals("second prompt" to "", result[1])
        assertEquals("third prompt" to "", result[2])
    }

    @Test
    fun `skips completely empty objects`() {
        val json = """[
            {"input": "valid", "output": "data"},
            {},
            {"input": "more", "output": "data"}
        ]"""
        val file = createJsonFile(json)

        val result = JsonDatasetReader.readJson(file)

        assertEquals(2, result.size)
        assertEquals("valid" to "data", result[0])
        assertEquals("more" to "data", result[1])
    }

    @Test
    fun `handles partial data in objects`() {
        val json = """[
            {"input": "only input"},
            {"output": "only output"},
            {"input": "both", "expectedOutput": "fields"}
        ]"""
        val file = createJsonFile(json)

        val result = JsonDatasetReader.readJson(file)

        assertEquals(3, result.size)
        assertEquals("only input" to "", result[0])
        assertEquals("" to "only output", result[1])
        assertEquals("both" to "fields", result[2])
    }

    @Test
    fun `handles null values`() {
        val json = """[
            {"input": null, "output": "result"},
            {"input": "query", "output": null}
        ]"""
        val file = createJsonFile(json)

        val result = JsonDatasetReader.readJson(file)

        assertEquals(2, result.size)
        assertEquals("" to "result", result[0])
        assertEquals("query" to "", result[1])
    }

    @Test
    fun `returns empty list for empty array`() {
        val json = "[]"
        val file = createJsonFile(json)

        val result = JsonDatasetReader.readJson(file)

        assertEquals(0, result.size)
    }

    @Test
    fun `returns empty list for empty object`() {
        val json = "{}"
        val file = createJsonFile(json)

        val result = JsonDatasetReader.readJson(file)

        assertEquals(0, result.size)
    }

    private fun createJsonFile(content: String) = tempFolder.root.toPath().resolve("test.json").also {
        Files.writeString(it, content)
    }
}
