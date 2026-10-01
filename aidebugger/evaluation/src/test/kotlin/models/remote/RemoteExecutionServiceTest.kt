package com.intellij.aidebugger.evaluation.models.remote

import com.intellij.aidebugger.evaluation.services.ProvisioningConf
import com.intellij.aidebugger.evaluation.services.RemoteExecutionRequest
import com.intellij.aidebugger.evaluation.services.RemoteExecutionResult
import com.intellij.aidebugger.evaluation.services.RemoteExecutionService
import com.intellij.aidebugger.evaluation.services.RemoteExecutionStatus
import com.intellij.mock.MockProject
import com.intellij.openapi.util.Disposer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createTempDirectory
import kotlin.io.path.writeText

class RemoteExecutionServiceTest {

    @Test
    fun `parseResults extracts mean score from aggregated results`() {
        val (service, tempDir) = createServiceWithTempDir()
        val outputDir = tempDir.resolve("results")
        Files.createDirectories(outputDir)

        val aggFile = outputDir.resolve("eval_result.json")
        aggFile.writeText("""{"stats":{"mean":0.856}}""")

        val result = service.parseResults(outputDir, null)

        assertEquals("0.856", result.meanScore)
    }

//    @Test
//    fun `parseResults extracts per-input results from raw file`() {
//        val (service, tempDir) = createServiceWithTempDir()
//        val outputDir = tempDir.resolve("results")
//        Files.createDirectories(outputDir)
//
//        val rawFile = outputDir.resolve("eval_results_raw.json")
//        rawFile.writeText(
//            """
//            [
//                {"input":"test1","evaluator":"eval1","score":0.8,"output_gen":"output1"},
//                {"input":"test1","evaluator":"eval2","score":0.6,"output_gen":"output1"},
//                {"input":"test2","evaluator":"eval1","score":0.9,"output_gen":"output2"}
//            ]
//            """.trimIndent()
//        )
//
//        val result = service.parseResults(outputDir, null)
//
//        assertEquals(2, result.resultsByInput.size)
//        assertTrue(result.resultsByInput.containsKey("test1"))
//        assertTrue(result.resultsByInput.containsKey("test2"))
//
//        val test1Results = result.resultsByInput["test1"]!!.first
//        assertEquals("0.8", test1Results["eval1"])
//        assertEquals("0.6", test1Results["eval2"])
//
//        val test2Results = result.resultsByInput["test2"]!!.first
//        assertEquals("0.9", test2Results["eval1"])
//    }

    @Test
    fun `parseResults handles missing files gracefully`() {
        val (service, tempDir) = createServiceWithTempDir()
        val outputDir = tempDir.resolve("empty-results")
        Files.createDirectories(outputDir)

        val result = service.parseResults(outputDir, null)

        assertNull(result.meanScore)
        assertTrue(result.resultsByInput.isEmpty())
        assertTrue(result.outputsByInput.isEmpty())
    }

//    @Test
//    fun `parseResults formats scores with proper precision`() {
//        val (service, tempDir) = createServiceWithTempDir()
//        val outputDir = tempDir.resolve("results")
//        Files.createDirectories(outputDir)
//
//        val rawFile = outputDir.resolve("eval_results_raw.json")
//        rawFile.writeText(
//            """
//            [
//                {"input":"test1","evaluator":"eval1","score":0.123456,"output_gen":"out"}
//            ]
//            """.trimIndent()
//        )
//
//        val result = service.parseResults(outputDir, null)
//
//        val score = result.resultsByInput["test1"]!!.first["eval1"]
//        assertEquals("0.123", score)
//    }

//    @Test
//    fun `parseResults extracts voterExplanations from extra field`() {
//        val (service, tempDir) = createServiceWithTempDir()
//        val outputDir = tempDir.resolve("results")
//        Files.createDirectories(outputDir)
//
//        val mapper = jacksonObjectMapper()
//        val evalResults = listOf(
//            EvalResult(
//                id = "1",
//                input = "test1",
//                outputGen = "output",
//                outputExpected = "expected",
//                experimentId = "exp1",
//                evaluator = "eval1",
//                type = "test",
//                score = 0.8,
//                extra = mapOf("voterExplanations" to listOf("Good result from voter")),
//                raw = emptyMap(),
//                runDatetime = "2025-01-01T00:00:00"
//            )
//        )
//
//        val rawFile = outputDir.resolve("eval_results_raw.json")
//        rawFile.writeText(mapper.writeValueAsString(evalResults))
//
//        val result = service.parseResults(outputDir, null)
//
//        val extras = result.resultsByInput["test1"]!!.second
//        assertEquals("Good result from voter", extras["eval1"])
//    }

    @Test
    fun `parseResults handles malformed json gracefully`() {
        val (service, tempDir) = createServiceWithTempDir()
        val outputDir = tempDir.resolve("results")
        Files.createDirectories(outputDir)

        val rawFile = outputDir.resolve("eval_results_raw.json")
        rawFile.writeText("""{"invalid": json here""")

        val result = service.parseResults(outputDir, null)

        assertTrue(result.resultsByInput.isEmpty())
        assertTrue(result.outputsByInput.isEmpty())
    }

    @Test
    fun `parseResults returns empty maps when no raw file exists`() {
        val (service, tempDir) = createServiceWithTempDir()
        val outputDir = tempDir.resolve("no-raw")
        Files.createDirectories(outputDir)

        val aggFile = outputDir.resolve("eval_result.json")
        aggFile.writeText("""{"stats":{"mean":0.75}}""")

        val result = service.parseResults(outputDir, null)

        assertEquals("0.75", result.meanScore)
        assertTrue(result.resultsByInput.isEmpty())
    }

//    @Test
//    fun `parseResults groups multiple evaluators per input`() {
//        val (service, tempDir) = createServiceWithTempDir()
//        val outputDir = tempDir.resolve("results")
//        Files.createDirectories(outputDir)
//
//        val rawFile = outputDir.resolve("eval_results_raw.json")
//        rawFile.writeText(
//            """
//            [
//                {"input":"input-A","evaluator":"eval1","score":0.8,"output_gen":"out-A"},
//                {"input":"input-A","evaluator":"eval2","score":0.6,"output_gen":"out-A"},
//                {"input":"input-A","evaluator":"eval3","score":0.9,"output_gen":"out-A"}
//            ]
//            """.trimIndent()
//        )
//
//        val result = service.parseResults(outputDir, null)
//
//        assertEquals(1, result.resultsByInput.size)
//        val inputAScores = result.resultsByInput["input-A"]!!.first
//        assertEquals(3, inputAScores.size)
//        assertEquals("0.8", inputAScores["eval1"])
//        assertEquals("0.6", inputAScores["eval2"])
//        assertEquals("0.9", inputAScores["eval3"])
//    }

//    @Test
//    fun `parseResults handles scores of 0 and 1 correctly`() {
//        val (service, tempDir) = createServiceWithTempDir()
//        val outputDir = tempDir.resolve("results")
//        Files.createDirectories(outputDir)
//
//        val rawFile = outputDir.resolve("eval_results_raw.json")
//        rawFile.writeText(
//            """
//            [
//                {"input":"test1","evaluator":"eval1","score":0.0,"output_gen":"out"},
//                {"input":"test2","evaluator":"eval2","score":1.0,"output_gen":"out"}
//            ]
//            """.trimIndent()
//        )
//
//        val result = service.parseResults(outputDir, null)
//
//        assertEquals("0.0", result.resultsByInput["test1"]!!.first["eval1"])
//        assertEquals("1.0", result.resultsByInput["test2"]!!.first["eval2"])
//    }

//    @Test
//    fun `parseResults extracts voterExplanations as string from extra field`() {
//        val (service, tempDir) = createServiceWithTempDir()
//        val outputDir = tempDir.resolve("results")
//        Files.createDirectories(outputDir)
//
//        val mapper = jacksonObjectMapper()
//        val evalResults = listOf(
//            EvalResult(
//                id = "1",
//                input = "test1",
//                outputGen = "output",
//                outputExpected = "expected",
//                experimentId = "exp1",
//                evaluator = "eval1",
//                type = "test",
//                score = 0.8,
//                extra = mapOf("voterExplanations" to "Direct string explanation"),
//                raw = emptyMap(),
//                runDatetime = "2025-01-01T00:00:00"
//            )
//        )
//
//        val rawFile = outputDir.resolve("eval_results_raw.json")
//        rawFile.writeText(mapper.writeValueAsString(evalResults))
//
//        val result = service.parseResults(outputDir, null)
//
//        val extras = result.resultsByInput["test1"]!!.second
//        assertEquals("Direct string explanation", extras["eval1"])
//    }

//    @Test
//    fun `parseResults processes fallback untyped json structure`() {
//        val (service, tempDir) = createServiceWithTempDir()
//        val outputDir = tempDir.resolve("results")
//        Files.createDirectories(outputDir)
//
//        val rawFile = outputDir.resolve("eval_results_raw.json")
//        rawFile.writeText(
//            """
//            [
//                {"input":"test1","evaluator":"custom-eval","score":0.85,"explanation":"Great"}
//            ]
//            """.trimIndent()
//        )
//
//        val result = service.parseResults(outputDir, null)
//
//        assertTrue(result.resultsByInput.containsKey("test1"))
//        val scores = result.resultsByInput["test1"]!!.first
//        assertEquals("0.85", scores["custom-eval"])
//    }

//    @Test
//    fun `parseResults ignores empty input entries`() {
//        val (service, tempDir) = createServiceWithTempDir()
//        val outputDir = tempDir.resolve("results")
//        Files.createDirectories(outputDir)
//
//        val rawFile = outputDir.resolve("eval_results_raw.json")
//        rawFile.writeText(
//            """
//            [
//                {"input":"","evaluator":"eval1","score":0.5,"output_gen":"out"},
//                {"input":"valid","evaluator":"eval2","score":0.8,"output_gen":"out"}
//            ]
//            """.trimIndent()
//        )
//
//        val result = service.parseResults(outputDir, null)
//
//        assertEquals(1, result.resultsByInput.size)
//        assertTrue(result.resultsByInput.containsKey("valid"))
//        assertFalse(result.resultsByInput.containsKey(""))
//    }

    @Test
    fun `parseResults handles mean score of 0`() {
        val (service, tempDir) = createServiceWithTempDir()
        val outputDir = tempDir.resolve("results")
        Files.createDirectories(outputDir)

        val aggFile = outputDir.resolve("eval_result.json")
        aggFile.writeText("""{"stats":{"mean":0.0}}""")

        val result = service.parseResults(outputDir, null)

        assertEquals("0.0", result.meanScore)
    }

    @Test
    fun `parseResults handles mean score of 1`() {
        val (service, tempDir) = createServiceWithTempDir()
        val outputDir = tempDir.resolve("results")
        Files.createDirectories(outputDir)

        val aggFile = outputDir.resolve("eval_result.json")
        aggFile.writeText("""{"stats":{"mean":1.0}}""")

        val result = service.parseResults(outputDir, null)

        assertEquals("1.0", result.meanScore)
    }

//    @Test
//    fun `parseResults extracts outputs per input`() {
//        val (service, tempDir) = createServiceWithTempDir()
//        val outputDir = tempDir.resolve("results")
//        Files.createDirectories(outputDir)
//
//        val rawFile = outputDir.resolve("eval_results_raw.json")
//        rawFile.writeText(
//            """
//            [
//                {"input":"test1","evaluator":"eval1","score":0.8,"output_gen":"output1"},
//                {"input":"test2","evaluator":"eval1","score":0.6,"output_gen":"output2"}
//            ]
//            """.trimIndent()
//        )
//
//        val result = service.parseResults(outputDir, null)
//
//        assertEquals(2, result.outputsByInput.size)
//        assertTrue(result.outputsByInput.containsKey("test1"))
//        assertTrue(result.outputsByInput.containsKey("test2"))
//    }

    @Test
    fun `RemoteExecutionRequest can be created with all fields`() {
        val request = RemoteExecutionRequest(
            name = "Test Execution",
            root = "/workspace",
            workingDir = "/workspace/src",
            command = "python test.py",
            dockerImage = "python:3.9",
            dockerAdditionalArgs = "--network host",
            includesPath = listOf("src", "lib"),
            outputPath = "results",
            pythonVersion = "3.9",
            pipRequirementsPath = "requirements.txt",
            poetryDirPath = null,
            variables = mapOf("KEY" to "value"),
            secretVariables = mapOf("SECRET" to "token"),
            runConfigName = "test-config",
            provisioningConf = ProvisioningConf("A100", 2, 8, 32)
        )

        assertEquals("Test Execution", request.name)
        assertEquals("python test.py", request.command)
        assertEquals("3.9", request.pythonVersion)
        assertNotNull(request.provisioningConf)
        assertEquals("A100", request.provisioningConf?.gpuType)
    }

    @Test
    fun `RemoteExecutionRequest can be created with minimal fields`() {
        val request = RemoteExecutionRequest(
            name = null,
            root = null,
            workingDir = null,
            command = "echo test",
            dockerImage = null,
            dockerAdditionalArgs = null,
            includesPath = null,
            outputPath = null,
            pythonVersion = null,
            pipRequirementsPath = null,
            poetryDirPath = null,
            variables = null,
            secretVariables = null,
            runConfigName = null,
            provisioningConf = null
        )

        assertEquals("echo test", request.command)
        assertNull(request.pythonVersion)
        assertNull(request.provisioningConf)
    }

    @Test
    fun `RemoteExecutionStatus represents execution state`() {
        val status = RemoteExecutionStatus(
            id = "exec-123",
            status = "Running",
            isTerminal = false
        )

        assertEquals("exec-123", status.id)
        assertEquals("Running", status.status)
        assertFalse(status.isTerminal)
    }

    @Test
    fun `RemoteExecutionStatus can represent terminal state`() {
        val status = RemoteExecutionStatus(
            id = "exec-456",
            status = "Finished",
            isTerminal = true
        )

        assertTrue(status.isTerminal)
    }

    @Test
    fun `RemoteExecutionResult represents complete result set`() {
        val result = RemoteExecutionResult(
            meanScore = "0.85",
            resultsByInput = mapOf(
                "input1" to (mapOf("eval1" to "0.8") to mapOf("eval1" to "explanation"))
            ),
            outputsByInput = mapOf("input1" to "output1")
        )

        assertEquals("0.85", result.meanScore)
        assertEquals(1, result.resultsByInput.size)
        assertEquals(1, result.outputsByInput.size)
    }

    @Test
    fun `ProvisioningConf can be created with all fields`() {
        val conf = ProvisioningConf(
            gpuType = "A100",
            gpuCount = 2,
            cpuCount = 8,
            ram = 32
        )

        assertEquals("A100", conf.gpuType)
        assertEquals(2, conf.gpuCount)
        assertEquals(8, conf.cpuCount)
        assertEquals(32, conf.ram)
    }

    @Test
    fun `ProvisioningConf can be created with null fields`() {
        val conf = ProvisioningConf(
            gpuType = null,
            gpuCount = null,
            cpuCount = 4,
            ram = null
        )

        assertNull(conf.gpuType)
        assertNull(conf.gpuCount)
        assertEquals(4, conf.cpuCount)
        assertNull(conf.ram)
    }

//    @Test
//    fun `parseResults handles complex nested structures`() {
//        val (service, tempDir) = createServiceWithTempDir()
//        val outputDir = tempDir.resolve("results")
//        Files.createDirectories(outputDir)
//
//        val mapper = jacksonObjectMapper()
//        val evalResults = listOf(
//            EvalResult(
//                id = "1",
//                input = "complex-input",
//                outputGen = """{"nested": {"data": "value"}}""",
//                outputExpected = "expected",
//                experimentId = "exp1",
//                evaluator = "eval1",
//                type = "test",
//                score = 0.7,
//                extra = mapOf("explanation" to "Complex evaluation"),
//                raw = mapOf("metadata" to mapOf("key" to "value")),
//                runDatetime = "2025-01-01T00:00:00"
//            )
//        )
//
//        val rawFile = outputDir.resolve("eval_results_raw.json")
//        rawFile.writeText(mapper.writeValueAsString(evalResults))
//
//        val result = service.parseResults(outputDir, null)
//
//        assertTrue(result.resultsByInput.containsKey("complex-input"))
//        assertEquals("0.7", result.resultsByInput["complex-input"]!!.first["eval1"])
//    }

    @Test
    fun `parseResults handles single object instead of array gracefully`() {
        val (service, tempDir) = createServiceWithTempDir()
        val outputDir = tempDir.resolve("results")
        Files.createDirectories(outputDir)

        val rawFile = outputDir.resolve("eval_results_raw.json")
        rawFile.writeText(
            """
            {
                "input":"test1",
                "evaluator":"eval1",
                "score":0.85,
                "output_gen":"output"
            }
            """.trimIndent()
        )

        val result = service.parseResults(outputDir, null)

        assertTrue(result.resultsByInput.isEmpty())
        assertTrue(result.outputsByInput.isEmpty())
    }

    @Test
    fun `parseResults handles empty array`() {
        val (service, tempDir) = createServiceWithTempDir()
        val outputDir = tempDir.resolve("results")
        Files.createDirectories(outputDir)

        val rawFile = outputDir.resolve("eval_results_raw.json")
        rawFile.writeText("[]")

        val result = service.parseResults(outputDir, null)

        assertTrue(result.resultsByInput.isEmpty())
        assertTrue(result.outputsByInput.isEmpty())
    }

    @Test
    fun `parseResults handles empty object`() {
        val (service, tempDir) = createServiceWithTempDir()
        val outputDir = tempDir.resolve("results")
        Files.createDirectories(outputDir)

        val rawFile = outputDir.resolve("eval_results_raw.json")
        rawFile.writeText("{}")

        val result = service.parseResults(outputDir, null)

        assertTrue(result.resultsByInput.isEmpty())
        assertTrue(result.outputsByInput.isEmpty())
    }

    private fun createServiceWithTempDir(): Pair<RemoteExecutionService, Path> {
        val tempDir = createTempDirectory("remote-exec-test")
        val project = ProjectStub(Disposer.newDisposable(), tempDir.toString())
        val service = RemoteExecutionService(project)
        return service to tempDir
    }

    private class ProjectStub(
        disposable: com.intellij.openapi.Disposable,
        private val projectBasePath: String?
    ) : MockProject(null, disposable) {
        override fun getBasePath() = projectBasePath
    }
}
