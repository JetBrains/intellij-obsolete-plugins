package com.intellij.aidebugger.evaluation.models.storage

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.intellij.aidebugger.evaluation.models.entities.DataPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createTempDirectory

class JsonFileStorageTest {

    private lateinit var tempDir: Path
    private val mapper = jacksonObjectMapper()

    @Before
    fun setup() {
        tempDir = createTempDirectory("json-storage-test")
    }

    @Test
    fun `ensureDir creates directory when it does not exist`() {
        val newDir = tempDir.resolve("new-dir")
        assertFalse(Files.exists(newDir))

        JsonFileStorage.ensureDir(newDir)

        assertTrue(Files.exists(newDir))
        assertTrue(Files.isDirectory(newDir))
    }

    @Test
    fun `ensureDir does not fail when directory already exists`() {
        val existingDir = tempDir.resolve("existing")
        Files.createDirectories(existingDir)
        assertTrue(Files.exists(existingDir))

        JsonFileStorage.ensureDir(existingDir)

        assertTrue(Files.exists(existingDir))
    }

    @Test
    fun `ensureDir creates nested directories`() {
        val nestedDir = tempDir.resolve("level1/level2/level3")
        assertFalse(Files.exists(nestedDir))

        JsonFileStorage.ensureDir(nestedDir)

        assertTrue(Files.exists(nestedDir))
        assertTrue(Files.isDirectory(nestedDir))
    }

    @Test
    fun `writeJson creates parent directory if missing`() {
        val file = tempDir.resolve("new-dir/data.json")
        assertFalse(Files.exists(file.parent))

        val data = mapOf("key" to "value")
        JsonFileStorage.writeJson(file, data)

        assertTrue(Files.exists(file.parent))
        assertTrue(Files.exists(file))
    }

    @Test
    fun `writeJson serializes simple object correctly`() {
        val file = tempDir.resolve("simple.json")
        val data = mapOf("name" to "test", "value" to 42)

        JsonFileStorage.writeJson(file, data)

        val json = Files.readString(file)
        val deserialized = mapper.readValue(json, Map::class.java)
        assertEquals("test", deserialized["name"])
        assertEquals(42, deserialized["value"])
    }

    @Test
    fun `writeJson formats with pretty printer`() {
        val file = tempDir.resolve("pretty.json")
        val data = mapOf("key" to "value")

        JsonFileStorage.writeJson(file, data)

        val json = Files.readString(file)
        assertTrue(json.contains("\n"))
        assertTrue(json.contains("  "))
    }

    @Test
    fun `writeJson overwrites existing file`() {
        val file = tempDir.resolve("overwrite.json")
        JsonFileStorage.writeJson(file, mapOf("first" to 1))
        JsonFileStorage.writeJson(file, mapOf("second" to 2))

        val json = Files.readString(file)
        val deserialized = mapper.readValue(json, Map::class.java)
        assertNull(deserialized["first"])
        assertEquals(2, deserialized["second"])
    }

    @Test
    fun `writeJson handles DataPoint list`() {
        val file = tempDir.resolve("datapoints.json")
        val dataPoints = listOf(
            DataPoint(
                id = "1",
                input = "test input",
                outputGen = "generated",
                outputExpected = "expected",
                experimentId = "exp-1"
            ),
            DataPoint(
                id = "2",
                input = "test input 2",
                outputGen = "generated 2",
                outputExpected = "expected 2",
                experimentId = "exp-1"
            )
        )

        JsonFileStorage.writeJson(file, dataPoints)

        assertTrue(Files.exists(file))
        val json = Files.readString(file)
        assertTrue(json.contains("test input"))
        assertTrue(json.contains("generated"))
    }

    @Test
    fun `readJson deserializes simple map`() {
        val file = tempDir.resolve("read.json")
        val data = mapOf("key" to "value", "number" to 123)
        Files.writeString(file, mapper.writeValueAsString(data))

        val result = JsonFileStorage.readJson(file, Map::class.java)

        assertEquals("value", result["key"])
        assertEquals(123, result["number"])
    }

    @Test
    fun `readJson deserializes DataPoint`() {
        val file = tempDir.resolve("datapoint.json")
        val dataPoint = DataPoint(
            id = "test-1",
            input = "input text",
            outputGen = "generated text",
            outputExpected = "expected text",
            experimentId = "exp-001",
            raw = mapOf("custom" to "data"),
            runDatetime = "2025-01-01T00:00:00"
        )
        Files.writeString(file, mapper.writeValueAsString(dataPoint))

        val result = JsonFileStorage.readJson(file, DataPoint::class.java)

        assertEquals("test-1", result.id)
        assertEquals("input text", result.input)
        assertEquals("generated text", result.outputGen)
        assertEquals("expected text", result.outputExpected)
        assertEquals("exp-001", result.experimentId)
        assertEquals("2025-01-01T00:00:00", result.runDatetime)
    }

    @Test
    fun `readDataPoints deserializes list of DataPoints`() {
        val file = tempDir.resolve("dataset.json")
        val dataPoints = listOf(
            DataPoint(
                id = "1",
                input = "input1",
                outputGen = "gen1",
                outputExpected = "exp1",
                experimentId = "exp-1"
            ),
            DataPoint(
                id = "2",
                input = "input2",
                outputGen = "gen2",
                outputExpected = "exp2",
                experimentId = "exp-1"
            )
        )
        Files.writeString(file, mapper.writeValueAsString(dataPoints))

        val result = JsonFileStorage.readDataPoints(file)

        assertEquals(2, result.size)
        assertEquals("1", result[0].id)
        assertEquals("input1", result[0].input)
        assertEquals("2", result[1].id)
        assertEquals("input2", result[1].input)
    }

    @Test
    fun `readDataPoints handles empty list`() {
        val file = tempDir.resolve("empty.json")
        Files.writeString(file, "[]")

        val result = JsonFileStorage.readDataPoints(file)

        assertTrue(result.isEmpty())
    }

    @Test
    fun `deleteRecursively removes empty directory`() {
        val dir = tempDir.resolve("to-delete")
        Files.createDirectories(dir)
        assertTrue(Files.exists(dir))

        JsonFileStorage.deleteRecursively(dir)

        assertFalse(Files.exists(dir))
    }

    @Test
    fun `deleteRecursively removes directory with files`() {
        val dir = tempDir.resolve("with-files")
        Files.createDirectories(dir)
        Files.writeString(dir.resolve("file1.txt"), "content1")
        Files.writeString(dir.resolve("file2.txt"), "content2")
        assertTrue(Files.exists(dir))

        JsonFileStorage.deleteRecursively(dir)

        assertFalse(Files.exists(dir))
    }

    @Test
    fun `deleteRecursively removes nested directories`() {
        val dir = tempDir.resolve("nested")
        val subDir1 = dir.resolve("sub1")
        val subDir2 = dir.resolve("sub2")
        Files.createDirectories(subDir1)
        Files.createDirectories(subDir2)
        Files.writeString(subDir1.resolve("file.txt"), "content")
        Files.writeString(subDir2.resolve("file.txt"), "content")
        assertTrue(Files.exists(dir))

        JsonFileStorage.deleteRecursively(dir)

        assertFalse(Files.exists(dir))
    }

    @Test
    fun `deleteRecursively does not fail when path does not exist`() {
        val nonExistent = tempDir.resolve("non-existent")
        assertFalse(Files.exists(nonExistent))

        JsonFileStorage.deleteRecursively(nonExistent)

        assertFalse(Files.exists(nonExistent))
    }

    @Test
    fun `deleteRecursively removes single file`() {
        val file = tempDir.resolve("single-file.txt")
        Files.writeString(file, "content")
        assertTrue(Files.exists(file))

        JsonFileStorage.deleteRecursively(file)

        assertFalse(Files.exists(file))
    }

    @Test
    fun `copyRecursively copies empty directory`() {
        val src = tempDir.resolve("src-empty")
        val dst = tempDir.resolve("dst-empty")
        Files.createDirectories(src)

        JsonFileStorage.copyRecursively(src, dst)

        assertTrue(Files.exists(dst))
        assertTrue(Files.isDirectory(dst))
    }

    @Test
    fun `copyRecursively copies directory with files`() {
        val src = tempDir.resolve("src-with-files")
        val dst = tempDir.resolve("dst-with-files")
        Files.createDirectories(src)
        Files.writeString(src.resolve("file1.txt"), "content1")
        Files.writeString(src.resolve("file2.txt"), "content2")

        JsonFileStorage.copyRecursively(src, dst)

        assertTrue(Files.exists(dst.resolve("file1.txt")))
        assertTrue(Files.exists(dst.resolve("file2.txt")))
        assertEquals("content1", Files.readString(dst.resolve("file1.txt")))
        assertEquals("content2", Files.readString(dst.resolve("file2.txt")))
    }

    @Test
    fun `copyRecursively copies nested directories`() {
        val src = tempDir.resolve("src-nested")
        val dst = tempDir.resolve("dst-nested")
        val subDir = src.resolve("subdir")
        Files.createDirectories(subDir)
        Files.writeString(src.resolve("root.txt"), "root content")
        Files.writeString(subDir.resolve("nested.txt"), "nested content")

        JsonFileStorage.copyRecursively(src, dst)

        assertTrue(Files.exists(dst.resolve("root.txt")))
        assertTrue(Files.exists(dst.resolve("subdir/nested.txt")))
        assertEquals("root content", Files.readString(dst.resolve("root.txt")))
        assertEquals("nested content", Files.readString(dst.resolve("subdir/nested.txt")))
    }

    @Test
    fun `copyRecursively replaces existing files`() {
        val src = tempDir.resolve("src-replace")
        val dst = tempDir.resolve("dst-replace")
        Files.createDirectories(src)
        Files.createDirectories(dst)
        Files.writeString(src.resolve("file.txt"), "new content")
        Files.writeString(dst.resolve("file.txt"), "old content")

        JsonFileStorage.copyRecursively(src, dst)

        assertEquals("new content", Files.readString(dst.resolve("file.txt")))
    }

    @Test
    fun `copyRecursively copies multiple levels deep`() {
        val src = tempDir.resolve("src-deep")
        val dst = tempDir.resolve("dst-deep")
        val level1 = src.resolve("level1")
        val level2 = level1.resolve("level2")
        val level3 = level2.resolve("level3")
        Files.createDirectories(level3)
        Files.writeString(level3.resolve("deep.txt"), "deep content")

        JsonFileStorage.copyRecursively(src, dst)

        assertTrue(Files.exists(dst.resolve("level1/level2/level3")))
        assertTrue(Files.exists(dst.resolve("level1/level2/level3/deep.txt")))
        assertEquals("deep content", Files.readString(dst.resolve("level1/level2/level3/deep.txt")))
    }

    @Test
    fun `writeJson and readJson round trip preserves data`() {
        val file = tempDir.resolve("roundtrip.json")
        val original = mapOf(
            "string" to "value",
            "number" to 42,
            "boolean" to true,
            "nested" to mapOf("key" to "nested value")
        )

        JsonFileStorage.writeJson(file, original)
        val result = JsonFileStorage.readJson(file, Map::class.java)

        assertEquals("value", result["string"])
        assertEquals(42, result["number"])
        assertEquals(true, result["boolean"])
        assertTrue(result["nested"] is Map<*, *>)
    }

    @Test
    fun `writeJson and readDataPoints round trip for DataPoints`() {
        val file = tempDir.resolve("datapoints-roundtrip.json")
        val original = listOf(
            DataPoint(
                id = "dp-1",
                input = "input text",
                outputGen = "generated output",
                outputExpected = "expected output",
                experimentId = "exp-test",
                raw = mapOf("trace" to "data"),
                runDatetime = "2025-01-15T12:00:00",
                exception = null
            )
        )

        JsonFileStorage.writeJson(file, original)
        val result = JsonFileStorage.readDataPoints(file)

        assertEquals(1, result.size)
        assertEquals("dp-1", result[0].id)
        assertEquals("input text", result[0].input)
        assertEquals("generated output", result[0].outputGen)
        assertEquals("expected output", result[0].outputExpected)
        assertEquals("exp-test", result[0].experimentId)
        assertEquals("2025-01-15T12:00:00", result[0].runDatetime)
        assertNull(result[0].exception)
    }

    @Test
    fun `copyRecursively preserves directory structure`() {
        val src = tempDir.resolve("src-structure")
        Files.createDirectories(src.resolve("dir1"))
        Files.createDirectories(src.resolve("dir2/subdir"))
        Files.writeString(src.resolve("dir1/file1.txt"), "content1")
        Files.writeString(src.resolve("dir2/file2.txt"), "content2")
        Files.writeString(src.resolve("dir2/subdir/file3.txt"), "content3")
        val dst = tempDir.resolve("dst-structure")

        JsonFileStorage.copyRecursively(src, dst)

        assertTrue(Files.isDirectory(dst.resolve("dir1")))
        assertTrue(Files.isDirectory(dst.resolve("dir2")))
        assertTrue(Files.isDirectory(dst.resolve("dir2/subdir")))
        assertTrue(Files.exists(dst.resolve("dir1/file1.txt")))
        assertTrue(Files.exists(dst.resolve("dir2/file2.txt")))
        assertTrue(Files.exists(dst.resolve("dir2/subdir/file3.txt")))
    }
}
