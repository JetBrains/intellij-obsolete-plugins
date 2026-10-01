package com.intellij.aidebugger.evaluation.models.storage

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.intellij.aidebugger.evaluation.models.entities.AggregatedEvalResult
import com.intellij.aidebugger.evaluation.models.entities.EvalResult
import com.intellij.aidebugger.evaluation.models.entities.ScoreStats
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.nio.file.Files
import kotlin.io.path.readText

class EvaluationResultsStorageTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val mapper = ObjectMapper().registerModule(KotlinModule.Builder().build())

    @Test
    fun `sanitizeNameForFile replaces special characters with underscore`() {
        // Spaces are preserved (not replaced)
        assertEquals("test config", sanitizeNameForFile("test config"))
        // Filesystem-unsafe characters are replaced
        assertEquals("test_config", sanitizeNameForFile("test/config"))
        assertEquals("test_config", sanitizeNameForFile("test\\config"))
        // Characters like @ and # are now allowed (only filesystem-unsafe chars replaced)
        assertEquals("test@config#123", sanitizeNameForFile("test@config#123"))
    }

    @Test
    fun `sanitizeNameForFile preserves alphanumeric and allowed chars`() {
        assertEquals("test-config_v1.2", sanitizeNameForFile("test-config_v1.2"))
        assertEquals("TestConfig123", sanitizeNameForFile("TestConfig123"))
        assertEquals("my.config-name_v2", sanitizeNameForFile("my.config-name_v2"))
    }

    @Test
    fun `sanitizeNameForFile handles empty string`() {
        assertEquals("config", sanitizeNameForFile(""))
        assertEquals("config", sanitizeNameForFile("   "))
    }

    @Test
    fun `sanitizeNameForFile trims whitespace`() {
        assertEquals("test", sanitizeNameForFile("  test  "))
        // Spaces within the name are preserved after trimming
        assertEquals("test config", sanitizeNameForFile("  test config  "))
    }

    @Test
    fun `sanitizeNameForFile handles only special characters`() {
        // Only filesystem-unsafe characters are replaced, others are preserved
        assertEquals("@#$%^&_", sanitizeNameForFile("@#$%^&*"))
        assertEquals("_", sanitizeNameForFile("/"))
    }

    @Test
    fun `saveEvaluationResults creates output directory`() {
        val outputDir = tempFolder.newFolder().toPath()
        val subDir = outputDir.resolve("nonexistent")

        val aggregated = createAggregatedResult()
        val evalResults = createEvalResults()

        saveEvaluationResults(aggregated, evalResults, subDir)

        assertTrue(Files.exists(subDir))
        assertTrue(Files.isDirectory(subDir))
    }

    @Test
    fun `saveEvaluationResults creates both json files`() {
        val outputDir = tempFolder.newFolder().toPath()
        val aggregated = createAggregatedResult()
        val evalResults = createEvalResults()

        saveEvaluationResults(aggregated, evalResults, outputDir)

        val aggFile = outputDir.resolve("eval_result.json")
        val rawFile = outputDir.resolve("eval_results_raw.json")

        assertTrue(Files.exists(aggFile))
        assertTrue(Files.exists(rawFile))
    }

    @Test
    fun `saveEvaluationResults returns path to aggregated file`() {
        val outputDir = tempFolder.newFolder().toPath()
        val aggregated = createAggregatedResult()
        val evalResults = createEvalResults()

        val returnedPath = saveEvaluationResults(aggregated, evalResults, outputDir)

        assertEquals(outputDir.resolve("eval_result.json"), returnedPath)
    }

    @Test
    fun `saveEvaluationResults writes valid aggregated json`() {
        val outputDir = tempFolder.newFolder().toPath()
        val aggregated = createAggregatedResult()
        val evalResults = createEvalResults()

        saveEvaluationResults(aggregated, evalResults, outputDir)

        val aggFile = outputDir.resolve("eval_result.json")
        val json = aggFile.readText()
        val deserialized = mapper.readValue<AggregatedEvalResult>(json)

        assertEquals(aggregated.experimentId, deserialized.experimentId)
        assertEquals(aggregated.evaluatorsStats.size, deserialized.evaluatorsStats.size)
    }

    @Test
    fun `saveEvaluationResults writes valid raw results json`() {
        val outputDir = tempFolder.newFolder().toPath()
        val aggregated = createAggregatedResult()
        val evalResults = createEvalResults()

        saveEvaluationResults(aggregated, evalResults, outputDir)

        val rawFile = outputDir.resolve("eval_results_raw.json")
        val json = rawFile.readText()
        val deserialized = mapper.readValue<List<EvalResult>>(json)

        assertEquals(evalResults.size, deserialized.size)
        assertEquals(evalResults[0].id, deserialized[0].id)
        assertEquals(evalResults[0].score, deserialized[0].score, 0.001)
    }

    @Test
    fun `saveEvaluationResults creates subdirectory when selectedName provided`() {
        val outputDir = tempFolder.newFolder().toPath()
        val aggregated = createAggregatedResult()
        val evalResults = createEvalResults()

        saveEvaluationResults(aggregated, evalResults, outputDir, "test-config")

        val subDir = outputDir.resolve("test-config")
        assertTrue(Files.exists(subDir))
        assertTrue(Files.exists(subDir.resolve("eval_result.json")))
        assertTrue(Files.exists(subDir.resolve("eval_results_raw.json")))
    }

    @Test
    fun `saveEvaluationResults sanitizes selectedName`() {
        val outputDir = tempFolder.newFolder().toPath()
        val aggregated = createAggregatedResult()
        val evalResults = createEvalResults()

        saveEvaluationResults(aggregated, evalResults, outputDir, "test/config*name")

        // Only filesystem-unsafe characters (/ and *) are replaced with underscores
        val subDir = outputDir.resolve("test_config_name")
        assertTrue(Files.exists(subDir))
        assertTrue(Files.exists(subDir.resolve("eval_result.json")))
    }

    @Test
    fun `saveEvaluationResults skips subdirectory when selectedName is null`() {
        val outputDir = tempFolder.newFolder().toPath()
        val aggregated = createAggregatedResult()
        val evalResults = createEvalResults()

        saveEvaluationResults(aggregated, evalResults, outputDir, null)

        assertTrue(Files.exists(outputDir.resolve("eval_result.json")))
        assertTrue(Files.exists(outputDir.resolve("eval_results_raw.json")))
    }

    @Test
    fun `saveEvaluationResults skips subdirectory when selectedName is blank`() {
        val outputDir = tempFolder.newFolder().toPath()
        val aggregated = createAggregatedResult()
        val evalResults = createEvalResults()

        saveEvaluationResults(aggregated, evalResults, outputDir, "   ")

        assertTrue(Files.exists(outputDir.resolve("eval_result.json")))
        assertTrue(Files.exists(outputDir.resolve("eval_results_raw.json")))
    }

    @Test
    fun `saveEvaluationResults handles empty eval results list`() {
        val outputDir = tempFolder.newFolder().toPath()
        val aggregated = createAggregatedResult()

        saveEvaluationResults(aggregated, emptyList(), outputDir)

        val rawFile = outputDir.resolve("eval_results_raw.json")
        val json = rawFile.readText()
        val deserialized = mapper.readValue<List<EvalResult>>(json)

        assertTrue(deserialized.isEmpty())
    }

    @Test
    fun `saveEvaluationResults handles empty evaluatorsStats`() {
        val outputDir = tempFolder.newFolder().toPath()
        val aggregated = AggregatedEvalResult(
            experimentId = "exp-001",
            evaluatorsStats = emptyMap()
        )
        val evalResults = createEvalResults()

        saveEvaluationResults(aggregated, evalResults, outputDir)

        val aggFile = outputDir.resolve("eval_result.json")
        val json = aggFile.readText()
        val deserialized = mapper.readValue<AggregatedEvalResult>(json)

        assertTrue(deserialized.evaluatorsStats.isEmpty())
    }

    @Test
    fun `saveEvaluationResults preserves multiple evaluator stats`() {
        val outputDir = tempFolder.newFolder().toPath()
        val aggregated = AggregatedEvalResult(
            experimentId = "exp-001",
            evaluatorsStats = mapOf(
                "LLMJudge" to ScoreStats(0.85, 0.1, 0.7, 0.95, 0.85, 10),
                "RegexMatch" to ScoreStats(0.90, 0.05, 0.85, 0.95, 0.90, 10),
                "ExactMatch" to ScoreStats(0.75, 0.15, 0.5, 0.95, 0.75, 10)
            )
        )
        val evalResults = createEvalResults()

        saveEvaluationResults(aggregated, evalResults, outputDir)

        val aggFile = outputDir.resolve("eval_result.json")
        val json = aggFile.readText()
        val deserialized = mapper.readValue<AggregatedEvalResult>(json)

        assertEquals(3, deserialized.evaluatorsStats.size)
        assertNotNull(deserialized.evaluatorsStats["LLMJudge"])
        assertNotNull(deserialized.evaluatorsStats["RegexMatch"])
        assertNotNull(deserialized.evaluatorsStats["ExactMatch"])
    }

    @Test
    fun `saveEvaluationResults preserves extra and raw fields in eval results`() {
        val outputDir = tempFolder.newFolder().toPath()
        val aggregated = createAggregatedResult()
        val evalResults = listOf(
            EvalResult(
                evaluator = "test",
                type = "test",
                score = 1.0,
                id = "test-1",
                input = "input",
                outputGen = "output",
                outputExpected = "expected",
                experimentId = "exp-001",
                extra = mapOf("key1" to "value1", "key2" to 42),
                raw = mapOf("rawKey" to "rawValue"),
                runDatetime = "2025-01-15T10:00:00"
            )
        )

        saveEvaluationResults(aggregated, evalResults, outputDir)

        val rawFile = outputDir.resolve("eval_results_raw.json")
        val json = rawFile.readText()
        val deserialized = mapper.readValue<List<EvalResult>>(json)

        assertEquals(mapOf("key1" to "value1", "key2" to 42), deserialized[0].extra)
        assertEquals(mapOf("rawKey" to "rawValue"), deserialized[0].raw)
    }

    @Test
    fun `saveEvaluationResults formats json with pretty printer`() {
        val outputDir = tempFolder.newFolder().toPath()
        val aggregated = createAggregatedResult()
        val evalResults = createEvalResults()

        saveEvaluationResults(aggregated, evalResults, outputDir)

        val aggFile = outputDir.resolve("eval_result.json")
        val json = aggFile.readText()

        assertTrue(json.contains("\n"))
        assertTrue(json.contains("  "))
    }

    @Test
    fun `saveEvaluationResults overwrites existing files`() {
        val outputDir = tempFolder.newFolder().toPath()
        val aggregated1 = AggregatedEvalResult("exp-001", emptyMap())
        val aggregated2 = AggregatedEvalResult("exp-002", emptyMap())

        saveEvaluationResults(aggregated1, emptyList(), outputDir)
        saveEvaluationResults(aggregated2, emptyList(), outputDir)

        val aggFile = outputDir.resolve("eval_result.json")
        val json = aggFile.readText()
        val deserialized = mapper.readValue<AggregatedEvalResult>(json)

        assertEquals("exp-002", deserialized.experimentId)
    }

    private fun createAggregatedResult(): AggregatedEvalResult {
        return AggregatedEvalResult(
            experimentId = "exp-001",
            evaluatorsStats = mapOf(
                "LLMJudge" to ScoreStats(
                    mean = 0.85,
                    std = 0.1,
                    min = 0.7,
                    max = 0.95,
                    median = 0.85,
                    count = 10
                )
            )
        )
    }

    private fun createEvalResults(): List<EvalResult> {
        return listOf(
            EvalResult(
                evaluator = "LLMJudge",
                type = "test",
                score = 0.85,
                id = "test-1",
                input = "test input",
                outputGen = "generated output",
                outputExpected = "expected output",
                experimentId = "exp-001",
                extra = emptyMap(),
                raw = emptyMap(),
                runDatetime = "2025-01-15T10:00:00"
            ),
            EvalResult(
                evaluator = "LLMJudge",
                type = "test",
                score = 0.90,
                id = "test-2",
                input = "test input 2",
                outputGen = "generated output 2",
                outputExpected = "expected output 2",
                experimentId = "exp-001",
                extra = emptyMap(),
                raw = emptyMap(),
                runDatetime = "2025-01-15T10:05:00"
            )
        )
    }
}
