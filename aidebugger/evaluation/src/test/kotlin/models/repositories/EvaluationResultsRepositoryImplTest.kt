package com.intellij.aidebugger.evaluation.models.repositories

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.intellij.aidebugger.evaluation.models.entities.AggregatedEvalResult
import com.intellij.aidebugger.evaluation.models.entities.DataPoint
import com.intellij.aidebugger.evaluation.models.entities.EvalResult
import com.intellij.aidebugger.evaluation.models.entities.ScoreStats
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path

class EvaluationResultsRepositoryImplTest {

    private fun createTestRepository(): Pair<TestableEvaluationResultsRepository, Path> {
        val tempDir = Files.createTempDirectory("eval-results-test")
        return TestableEvaluationResultsRepository(tempDir) to tempDir
    }

    @Test
    fun `saveTableSnapshot creates directory and saves JSON`() {
        val (repository, tempDir) = createTestRepository()
        val snapshot = TableSnapshot(
            ids = listOf("id1", "id2"),
            rows = listOf(
                TableRow("input1", mapOf("eval1" to "0.8"), mapOf("eval1" to "good")),
                TableRow("input2", mapOf("eval1" to "0.6"), mapOf("eval1" to "ok"))
            ),
            outputs = listOf("output1", "output2")
        )

        repository.saveTableSnapshot("test-config", snapshot)

        val expectedFile = tempDir.resolve(".jbeval").resolve("eval").resolve("last_test-config.json")
        assertTrue(Files.exists(expectedFile))

        val mapper = ObjectMapper().registerModule(KotlinModule.Builder().build())
        val loaded = mapper.readValue(Files.readString(expectedFile), TableSnapshot::class.java)
        assertEquals(2, loaded.ids.size)
        assertEquals(2, loaded.rows.size)
        assertEquals("input1", loaded.rows[0].input)
    }

    @Test
    fun `saveTableSnapshot sanitizes config name`() {
        val (repository, tempDir) = createTestRepository()
        val snapshot = TableSnapshot(emptyList(), emptyList(), emptyList())

        repository.saveTableSnapshot("test@config#123", snapshot)

        // New sanitization only replaces filesystem-unsafe characters (\ / : * ? " < > |)
        // Characters like @ and # are now allowed
        val expectedFile = tempDir.resolve(".jbeval").resolve("eval").resolve("last_test@config#123.json")
        assertTrue(Files.exists(expectedFile))
    }

    @Test
    fun `loadTableSnapshot returns empty snapshot when file does not exist`() {
        val (repository, _) = createTestRepository()

        val snapshot = repository.loadTableSnapshot("non-existent-config")

        assertTrue(snapshot.ids.isEmpty())
        assertTrue(snapshot.rows.isEmpty())
        assertTrue(snapshot.outputs.isEmpty())
    }

    @Test
    fun `loadTableSnapshot retrieves saved snapshot`() {
        val (repository, _) = createTestRepository()
        val original = TableSnapshot(
            ids = listOf("id1", "id2", "id3"),
            rows = listOf(
                TableRow("input1", mapOf("eval1" to "0.9"), mapOf()),
                TableRow("input2", mapOf("eval1" to "0.7"), mapOf()),
                TableRow("input3", mapOf("eval1" to "0.5"), mapOf())
            ),
            outputs = listOf("out1", "out2", "out3")
        )

        repository.saveTableSnapshot("my-config", original)
        val loaded = repository.loadTableSnapshot("my-config")

        assertEquals(3, loaded.ids.size)
        assertEquals(3, loaded.rows.size)
        assertEquals(3, loaded.outputs.size)
        assertEquals("input2", loaded.rows[1].input)
        assertEquals("0.7", loaded.rows[1].evaluatorScores["eval1"])
    }

    @Test
    fun `saveEvaluationResult creates directory structure and files`() {
        val (repository, tempDir) = createTestRepository()
        val outputDir = tempDir.resolve("output")
        val aggregated = AggregatedEvalResult(
            experimentId = "exp-123",
            evaluatorsStats = mapOf(
                "eval1" to ScoreStats(0.75, 0.1, 0.6, 0.9, 0.75, 4)
            )
        )
        val results = listOf(
            createEvalResult("eval1", 0.8, "id1"),
            createEvalResult("eval1", 0.7, "id2")
        )

        val resultFile = repository.saveEvaluationResult(aggregated, results, outputDir, "test-config")

        assertTrue(Files.exists(resultFile))
        assertEquals("eval_result.json", resultFile.fileName.toString())
        assertTrue(Files.exists(resultFile.parent.resolve("eval_results_raw.json")))
    }

    @Test
    fun `saveEvaluationResult without config name uses outputDir directly`() {
        val (repository, tempDir) = createTestRepository()
        val outputDir = tempDir.resolve("output")
        val aggregated = AggregatedEvalResult("exp-123", emptyMap())

        val resultFile = repository.saveEvaluationResult(aggregated, emptyList(), outputDir, null)

        assertEquals(outputDir.resolve("eval_result.json"), resultFile)
    }

    @Test
    fun `getAverageScore returns NaN when file does not exist`() {
        val (repository, _) = createTestRepository()

        val score = repository.getAverageScore("non-existent")

        assertTrue(score.isNaN())
    }

    @Test
    fun `getAverageScore reads from aggregated stats mean`() {
        val (repository, tempDir) = createTestRepository()
        val evalDir = tempDir.resolve(".jbeval").resolve("eval")
        Files.createDirectories(evalDir)
        val json = """
            {
                "aggregated": {
                    "stats": {
                        "mean": 0.85
                    }
                }
            }
        """.trimIndent()
        Files.writeString(evalDir.resolve("last_test-config.json"), json)

        val score = repository.getAverageScore("test-config")

        assertEquals(0.85, score, 0.001)
    }

    @Test
    fun `getAverageScore calculates from scores array when stats missing`() {
        val (repository, tempDir) = createTestRepository()
        val evalDir = tempDir.resolve(".jbeval").resolve("eval")
        Files.createDirectories(evalDir)
        val json = """
            {
                "scores": ["0.8", "0.6", "0.9"]
            }
        """.trimIndent()
        Files.writeString(evalDir.resolve("last_test-config.json"), json)

        val score = repository.getAverageScore("test-config")

        assertEquals(0.7666, score, 0.001)
    }

    @Test
    fun `getAverageScore returns NaN for empty scores`() {
        val (repository, tempDir) = createTestRepository()
        val evalDir = tempDir.resolve(".jbeval").resolve("eval")
        Files.createDirectories(evalDir)
        val json = """{"scores": []}"""
        Files.writeString(evalDir.resolve("last_test-config.json"), json)

        val score = repository.getAverageScore("test-config")

        assertTrue(score.isNaN())
    }

    @Test
    fun `getAverageAgentTime returns NaN when no data points`() {
        val (repository, _) = createTestRepository()

        val time = repository.getAverageAgentTime("non-existent")

        assertTrue(time.isNaN())
    }

    @Test
    fun `getAverageAgentTime returns NaN for invalid data`() {
        val (repository, tempDir) = createTestRepository()
        val evalDir = tempDir.resolve(".jbeval").resolve("eval")
        Files.createDirectories(evalDir)
        val dataPoints = listOf(
            createDataPointWithTraceEvents("id1", 1000, 3000),
            createDataPointWithTraceEvents("id2", 2000, 5000)
        )
        writeDataPoints(evalDir.resolve("test-config.json"), dataPoints)

        val avgTime = repository.getAverageAgentTime("test-config")

        assertTrue(avgTime.isNaN())
    }

    @Test
    fun `getAverageTokens returns zero when no data points`() {
        val (repository, _) = createTestRepository()

        val tokens = repository.getAverageTokens("non-existent")

        assertEquals(0, tokens)
    }

    @Test
    fun `getAverageTokens returns zero for invalid data`() {
        val (repository, tempDir) = createTestRepository()
        val evalDir = tempDir.resolve(".jbeval").resolve("eval")
        Files.createDirectories(evalDir)
        val dataPoints = listOf(
            createDataPointWithTokens("id1", 100),
            createDataPointWithTokens("id2", 200)
        )
        writeDataPoints(evalDir.resolve("test-config.json"), dataPoints)

        val avgTokens = repository.getAverageTokens("test-config")

        assertEquals(0, avgTokens)
    }

    @Test
    fun `getLastRunTimestamp returns null when no files exist`() {
        val (repository, _) = createTestRepository()

        val timestamp = repository.getLastRunTimestamp("non-existent")

        assertNull(timestamp)
    }

    @Test
    fun `getLastRunTimestamp returns modification time of last file`() {
        val (repository, tempDir) = createTestRepository()
        val evalDir = tempDir.resolve(".jbeval").resolve("eval")
        Files.createDirectories(evalDir)
        val lastFile = evalDir.resolve("last_test-config.json")
        Files.writeString(lastFile, "{}")
        Thread.sleep(10)

        val timestamp = repository.getLastRunTimestamp("test-config")

        assertNotNull(timestamp)
        assertTrue(timestamp!! > 0)
    }

    @Test
    fun `getLastRunTimestamp returns max of both files`() {
        val (repository, tempDir) = createTestRepository()
        val evalDir = tempDir.resolve(".jbeval").resolve("eval")
        Files.createDirectories(evalDir)
        Files.writeString(evalDir.resolve("last_test-config.json"), "{}")
        Thread.sleep(100)
        val dataFile = evalDir.resolve("test-config.json")
        Files.writeString(dataFile, "[]")
        val dataFileTime = Files.getLastModifiedTime(dataFile).toMillis()

        val timestamp = repository.getLastRunTimestamp("test-config")

        assertNotNull(timestamp)
        assertTrue("Expected timestamp ($timestamp) >= dataFileTime ($dataFileTime)", timestamp!! >= dataFileTime)
    }

    @Test
    fun `loadTableSnapshot handles corrupted JSON gracefully`() {
        val (repository, tempDir) = createTestRepository()
        val evalDir = tempDir.resolve(".jbeval").resolve("eval")
        Files.createDirectories(evalDir)
        Files.writeString(evalDir.resolve("last_corrupted.json"), "{ invalid json }")

        val snapshot = repository.loadTableSnapshot("corrupted")

        assertTrue(snapshot.ids.isEmpty())
        assertTrue(snapshot.rows.isEmpty())
    }

    @Test
    fun `saveTableSnapshot overwrites existing file`() {
        val (repository, _) = createTestRepository()
        val snapshot1 = TableSnapshot(
            ids = listOf("id1"),
            rows = listOf(TableRow("input1", emptyMap(), emptyMap())),
            outputs = listOf("output1")
        )
        val snapshot2 = TableSnapshot(
            ids = listOf("id2", "id3"),
            rows = listOf(
                TableRow("input2", emptyMap(), emptyMap()),
                TableRow("input3", emptyMap(), emptyMap())
            ),
            outputs = listOf("output2", "output3")
        )

        repository.saveTableSnapshot("config", snapshot1)
        repository.saveTableSnapshot("config", snapshot2)
        val loaded = repository.loadTableSnapshot("config")

        assertEquals(2, loaded.ids.size)
        assertEquals("id2", loaded.ids[0])
        assertEquals("input3", loaded.rows[1].input)
    }

    private fun createEvalResult(evaluator: String, score: Double, id: String): EvalResult {
        return EvalResult(
            evaluator = evaluator,
            type = "test",
            score = score,
            extra = emptyMap(),
            id = id,
            input = "input-$id",
            outputGen = "output-$id",
            outputExpected = "expected-$id",
            experimentId = "exp-test",
            raw = emptyMap(),
            runDatetime = "2025-01-01T00:00:00"
        )
    }

    private fun createDataPointWithTraceEvents(id: String, startMs: Long, endMs: Long): DataPoint {
        val raw = mapOf(
            "events" to mapOf(
                "event1" to mapOf(
                    "timestampStartMs" to startMs,
                    "timestampEndMs" to endMs,
                    "type" to "General"
                )
            )
        )
        return DataPoint(
            id = id,
            input = "input",
            outputGen = "output",
            outputExpected = "expected",
            experimentId = "exp",
            raw = raw
        )
    }

    private fun createDataPointWithTokens(id: String, totalTokens: Int): DataPoint {
        val raw = mapOf(
            "events" to mapOf(
                "event1" to mapOf(
                    "type" to "LlmCall",
                    "timestampStartMs" to 1000,
                    "timestampEndMs" to 2000,
                    "payload" to mapOf(
                        "outputs" to mapOf(
                            "messages" to listOf(
                                mapOf(
                                    "response_metadata" to mapOf(
                                        "token_usage" to mapOf(
                                            "total_tokens" to totalTokens
                                        )
                                    )
                                )
                            )
                        )
                    )
                )
            )
        )
        return DataPoint(
            id = id,
            input = "input",
            outputGen = "output",
            outputExpected = "expected",
            experimentId = "exp",
            raw = raw
        )
    }

    private fun writeDataPoints(path: Path, dataPoints: List<DataPoint>) {
        val mapper = ObjectMapper().registerModule(KotlinModule.Builder().build())
        Files.newBufferedWriter(path).use { writer ->
            mapper.writeValue(writer, dataPoints)
        }
    }
}

class TestableEvaluationResultsRepository(
    testBasePath: Path
) : EvaluationResultsRepository {

    private val fakeProject = FakeProject(testBasePath)
    private val impl = EvaluationResultsRepositoryImpl(fakeProject)

    override fun saveTableSnapshot(configName: String, tableSnapshot: TableSnapshot) {
        impl.saveTableSnapshot(configName, tableSnapshot)
    }

    override fun loadTableSnapshot(configName: String): TableSnapshot {
        return impl.loadTableSnapshot(configName)
    }

    override fun saveEvaluationResult(
        aggregatedEvalResults: AggregatedEvalResult,
        evalResults: List<EvalResult>,
        outputDir: Path,
        selectedName: String?
    ): Path {
        return impl.saveEvaluationResult(aggregatedEvalResults, evalResults, outputDir, selectedName)
    }

    override fun getAverageScore(configName: String): Double {
        return impl.getAverageScore(configName)
    }

    override fun getAverageAgentTime(configName: String): Double {
        return impl.getAverageAgentTime(configName)
    }

    override fun getAverageTokens(configName: String): Int {
        return impl.getAverageTokens(configName)
    }

    override fun getLastRunTimestamp(configName: String): Long? {
        return impl.getLastRunTimestamp(configName)
    }

    override fun getLastAggregatedResult(configName: String): AggregatedEvalResult? {
        return impl.getLastAggregatedResult(configName)
    }

    override fun removeResults(configName: String) {
        impl.removeResults(configName)
    }
}

@Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
class FakeProject(private val testBasePath: Path) : com.intellij.openapi.project.Project {
    override fun getBasePath(): String = testBasePath.toString()
    @Deprecated("")
    override fun getBaseDir(): com.intellij.openapi.vfs.VirtualFile? = null
    override fun getName(): String = "test-project"
    override fun getProjectFilePath(): String? = null
    override fun getProjectFile(): com.intellij.openapi.vfs.VirtualFile? = null
    override fun getWorkspaceFile(): com.intellij.openapi.vfs.VirtualFile? = null
    override fun getLocationHash(): String = "test"
    override fun save() {}
    override fun isOpen(): Boolean = true
    override fun isInitialized(): Boolean = true
    override fun isDisposed(): Boolean = false
    override fun <T : Any> getService(serviceClass: Class<T>): T? = null
    @Deprecated("")
    override fun <T : Any> getComponent(interfaceClass: Class<T>): T? = null
    override fun hasComponent(interfaceClass: Class<*>): Boolean = false
    override fun <T : Any> instantiateClass(aClass: Class<T>, pluginId: com.intellij.openapi.extensions.PluginId): T =
        throw UnsupportedOperationException()
    override fun <T : Any> instantiateClass(className: String, pluginDescriptor: com.intellij.openapi.extensions.PluginDescriptor): T =
        throw UnsupportedOperationException()
    override fun <T : Any> instantiateClassWithConstructorInjection(aClass: Class<T>, key: Any, pluginId: com.intellij.openapi.extensions.PluginId): T =
        throw UnsupportedOperationException()
    override fun createError(error: Throwable, pluginId: com.intellij.openapi.extensions.PluginId): RuntimeException =
        RuntimeException(error)
    override fun createError(message: String, pluginId: com.intellij.openapi.extensions.PluginId): RuntimeException =
        RuntimeException(message)
    override fun createError(message: String, error: Throwable?, pluginId: com.intellij.openapi.extensions.PluginId, attachments: MutableMap<String, String>?): RuntimeException =
        RuntimeException(message, error)
    override fun <T : Any> loadClass(className: String, pluginDescriptor: com.intellij.openapi.extensions.PluginDescriptor): Class<T> =
        throw UnsupportedOperationException()
    override fun getActivityCategory(isExtension: Boolean): com.intellij.diagnostic.ActivityCategory =
        throw UnsupportedOperationException()
    override fun getExtensionArea(): com.intellij.openapi.extensions.ExtensionsArea =
        throw UnsupportedOperationException()
    override fun getMessageBus(): com.intellij.util.messages.MessageBus =
        throw UnsupportedOperationException()
    override fun getDisposed(): com.intellij.openapi.util.Condition<*> =
        throw UnsupportedOperationException()
    override fun dispose() {}
    override fun <T : Any> getUserData(key: com.intellij.openapi.util.Key<T>): T? = null
    override fun <T : Any> putUserData(key: com.intellij.openapi.util.Key<T>, value: T?) {}
    override fun getPresentableUrl(): String? = null
    override fun isInjectionForExtensionSupported(): Boolean = false
}
