package com.intellij.aidebugger.common.services

import com.google.gson.Gson
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.charset.StandardCharsets

class TracesDatasetsServiceTest {

    private val gson = Gson()

    @Test
    fun `creates dataset file structure`() {
        val storage = InMemoryStorage()

        storage.write("test.json", datasetFile())

        val content = storage.read("test.json")
        assertNotNull(content)
        assertTrue(content!!.contains("\"meta\""))
        assertTrue(content.contains("\"items\""))
    }

    @Test
    fun `adds entry to dataset`() {
        val storage = InMemoryStorage()
        storage.write("dataset.json", datasetFile())

        addEntry(storage, "dataset.json", "e1", "input1", "output1")
        addEntry(storage, "dataset.json", "e2", "input2", "output2")

        val pairs = loadPairs(storage, "dataset.json")
        assertEquals(2, pairs.size)
        assertEquals("input1" to "output1", pairs[0])
        assertEquals("input2" to "output2", pairs[1])
    }

    @Test
    fun `loads pairs from dataset`() {
        val storage = InMemoryStorage()
        storage.write("dataset.json", datasetFile())
        addEntry(storage, "dataset.json", "e1", "What is 2+2?", "4")
        addEntry(storage, "dataset.json", "e2", "Capital of France?", "Paris")

        val pairs = loadPairs(storage, "dataset.json")

        assertEquals(2, pairs.size)
        assertEquals("What is 2+2?" to "4", pairs[0])
        assertEquals("Capital of France?" to "Paris", pairs[1])
    }

    @Test
    fun `saves pairs overwrites existing entries`() {
        val storage = InMemoryStorage()
        storage.write("dataset.json", datasetFile())
        addEntry(storage, "dataset.json", "e1", "old_input", "old_output")

        savePairs(storage, "dataset.json", listOf("new_input" to "new_output"))

        val pairs = loadPairs(storage, "dataset.json")
        assertEquals(1, pairs.size)
        assertEquals("new_input" to "new_output", pairs[0])
    }

    @Test
    fun `handles json primitive values`() {
        val storage = InMemoryStorage()
        storage.write("dataset.json", datasetFile())

        addEntryJson(storage, "dataset.json", "e1", JsonPrimitive("string"), JsonPrimitive(42))
        addEntryJson(storage, "dataset.json", "e2", JsonPrimitive(true), JsonPrimitive(3.14))

        val pairs = loadPairs(storage, "dataset.json")

        assertEquals(2, pairs.size)
        assertEquals("string" to "42", pairs[0])
        assertEquals("true" to "3.14", pairs[1])
    }

    @Test
    fun `handles json object values`() {
        val storage = InMemoryStorage()
        storage.write("dataset.json", datasetFile())
        val inputObj = JsonObject().apply { addProperty("query", "test") }
        val outputObj = JsonObject().apply { addProperty("result", "success") }

        addEntryJson(storage, "dataset.json", "e1", inputObj, outputObj)

        val pairs = loadPairs(storage, "dataset.json")

        assertEquals(1, pairs.size)
        assertTrue(pairs[0].first.contains("query"))
        assertTrue(pairs[0].second.contains("result"))
    }

    @Test
    fun `handles null values`() {
        val storage = InMemoryStorage()
        storage.write("dataset.json", datasetFile())

        addEntryJson(storage, "dataset.json", "e1", JsonNull.INSTANCE, JsonPrimitive("output"))
        addEntryJson(storage, "dataset.json", "e2", JsonPrimitive("input"), JsonNull.INSTANCE)

        val pairs = loadPairs(storage, "dataset.json")

        assertEquals(2, pairs.size)
        assertEquals("" to "output", pairs[0])
        assertEquals("input" to "", pairs[1])
    }

    @Test
    fun `handles empty dataset`() {
        val storage = InMemoryStorage()
        storage.write("dataset.json", datasetFile())

        val pairs = loadPairs(storage, "dataset.json")

        assertEquals(0, pairs.size)
    }

    @Test
    fun `preserves entry order`() {
        val storage = InMemoryStorage()
        storage.write("dataset.json", datasetFile())

        addEntry(storage, "dataset.json", "e1", "first", "1st")
        addEntry(storage, "dataset.json", "e2", "second", "2nd")
        addEntry(storage, "dataset.json", "e3", "third", "3rd")

        val pairs = loadPairs(storage, "dataset.json")

        assertEquals("first" to "1st", pairs[0])
        assertEquals("second" to "2nd", pairs[1])
        assertEquals("third" to "3rd", pairs[2])
    }

    @Test
    fun `handles multiple datasets`() {
        val storage = InMemoryStorage()
        storage.write("dataset1.json", datasetFile())
        storage.write("dataset2.json", datasetFile())

        addEntry(storage, "dataset1.json", "e1", "data1", "result1")
        addEntry(storage, "dataset2.json", "e1", "data2", "result2")

        val pairs1 = loadPairs(storage, "dataset1.json")
        val pairs2 = loadPairs(storage, "dataset2.json")

        assertEquals("data1" to "result1", pairs1[0])
        assertEquals("data2" to "result2", pairs2[0])
    }

    @Test
    fun `lists dataset files`() {
        val storage = InMemoryStorage()
        storage.write("dataset1.json", datasetFile())
        storage.write("dataset2.json", datasetFile())
        storage.write("dataset3.json", datasetFile())

        val files = storage.list().filter { it.endsWith(".json") }

        assertEquals(3, files.size)
    }

    @Test
    fun `supports legacy data field`() {
        val storage = InMemoryStorage()
        val legacyContent = """
            {
                "meta": {},
                "data": [
                    {"id": "e1", "input": "old_input", "output": "old_output", "raw": null}
                ],
                "items": []
            }
        """.trimIndent()
        storage.write("legacy.json", legacyContent)

        val parsed = gson.fromJson(storage.read("legacy.json"), DatasetFile::class.java)

        assertEquals(0, parsed.items.size)
        assertEquals(1, parsed.data?.size ?: 0)
    }

    @Test
    fun `updates itemsCount in metadata`() {
        val storage = InMemoryStorage()
        storage.write("dataset.json", datasetFile())

        addEntry(storage, "dataset.json", "e1", "input1", "output1")
        addEntry(storage, "dataset.json", "e2", "input2", "output2")
        addEntry(storage, "dataset.json", "e3", "input3", "output3")

        val content = gson.fromJson(storage.read("dataset.json"), DatasetFile::class.java)
        assertEquals(3, content.items.size)
    }

    private fun datasetFile(): String {
        val file = DatasetFile(JsonObject(), mutableListOf())
        return gson.toJson(file)
    }

    private fun addEntry(storage: InMemoryStorage, fileName: String, id: String, input: String, output: String) {
        addEntryJson(storage, fileName, id, JsonPrimitive(input), JsonPrimitive(output))
    }

    private fun addEntryJson(
        storage: InMemoryStorage,
        fileName: String,
        id: String,
        input: com.google.gson.JsonElement,
        output: com.google.gson.JsonElement
    ) {
        val content = gson.fromJson(storage.read(fileName), DatasetFile::class.java)
        content.items.add(Entry(id, input, output, JsonNull.INSTANCE))
        storage.write(fileName, gson.toJson(content))
    }

    private fun savePairs(storage: InMemoryStorage, fileName: String, pairs: List<Pair<String, String>>) {
        val content = gson.fromJson(storage.read("dataset.json"), DatasetFile::class.java)
        content.items.clear()
        pairs.forEachIndexed { index, (input, output) ->
            content.items.add(
                Entry(
                    "item_${index + 1}",
                    JsonPrimitive(input),
                    JsonPrimitive(output),
                    JsonNull.INSTANCE
                )
            )
        }
        storage.write(fileName, gson.toJson(content))
    }

    private fun loadPairs(storage: InMemoryStorage, fileName: String): List<Pair<String, String>> {
        val content = gson.fromJson(storage.read(fileName), DatasetFile::class.java)
        return content.items.map { entry ->
            val inp = when {
                entry.input == null || entry.input is JsonNull -> ""
                entry.input.isJsonPrimitive -> entry.input.asJsonPrimitive.asString
                else -> entry.input.toString()
            }
            val out = when {
                entry.output == null || entry.output is JsonNull -> ""
                entry.output.isJsonPrimitive -> entry.output.asJsonPrimitive.asString
                else -> entry.output.toString()
            }
            inp to out
        }
    }

    class InMemoryStorage {
        private val files = mutableMapOf<String, ByteArray>()

        fun write(name: String, content: String) {
            files[name] = content.toByteArray(StandardCharsets.UTF_8)
        }

        fun read(name: String): String? {
            return files[name]?.toString(StandardCharsets.UTF_8)
        }

        fun list(): List<String> = files.keys.toList()
    }

    data class DatasetFile(
        val meta: JsonObject,
        val items: MutableList<Entry>,
        val data: MutableList<Entry>? = null
    )

    data class Entry(
        val id: String,
        val input: com.google.gson.JsonElement?,
        val output: com.google.gson.JsonElement?,
        val raw: com.google.gson.JsonElement?
    )
}
