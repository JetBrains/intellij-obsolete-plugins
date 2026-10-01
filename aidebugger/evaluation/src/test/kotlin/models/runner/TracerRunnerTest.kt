package com.intellij.aidebugger.evaluation.models.runner

import com.intellij.aidebugger.common.models.HierarchicalTraceEvent
import com.intellij.aidebugger.common.models.HierarchicalTraceEventsState
import com.intellij.aidebugger.common.models.entities.EventType
import com.intellij.aidebugger.common.models.entities.Framework
import com.intellij.aidebugger.evaluation.models.entities.DataPoint
import com.intellij.aidebugger.evaluation.models.entities.InputSpec
import com.intellij.aidebugger.evaluation.models.extractor.BaseExtractor
import com.intellij.aidebugger.evaluation.models.extractor.FallbackContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.io.path.createTempDirectory

class TracerRunnerTest {

    private lateinit var tempDir: Path
    private lateinit var mockGateway: MockGateway
    private lateinit var mockExtractor: MockExtractor

    @Before
    fun setup() {
        tempDir = createTempDirectory("tracer-runner-test")
        mockGateway = MockGateway()
        mockExtractor = MockExtractor()
    }

    @Test
    fun `run creates experiment id and dataset file`() {
        val runner = TestableTracerRunner(
            gateway = mockGateway,
            projectDir = tempDir.toString(),
            extractor = mockExtractor
        )

        val request = RunRequest(
            inputSpecs = listOf(InputSpec("1", "test input", "expected output")),
            outputRootDir = "output"
        )

        val result = runner.run(request)

        assertNotNull(result.experimentId)
        assertTrue(result.experimentId.startsWith("exp_"))
        assertTrue(result.datasetPath.contains("output/eval"))
        assertTrue(result.datasetPath.endsWith(".json"))
        assertEquals(1, result.dataPointsCount)
        assertTrue(Files.exists(Path.of(result.datasetPath)))
    }

    @Test
    fun `run processes all input specs`() {
        val runner = TestableTracerRunner(
            gateway = mockGateway,
            projectDir = tempDir.toString(),
            extractor = mockExtractor
        )

        val request = RunRequest(
            inputSpecs = listOf(
                InputSpec("1", "input1", "expected1"),
                InputSpec("2", "input2", "expected2"),
                InputSpec("3", "input3", "expected3")
            )
        )

        val result = runner.run(request)

        assertEquals(3, result.dataPointsCount)
        assertEquals(3, mockGateway.runCount)
        assertEquals(3, mockExtractor.extractCount)
    }

    @Test
    fun `run skips empty inputs`() {
        val runner = TestableTracerRunner(
            gateway = mockGateway,
            projectDir = tempDir.toString(),
            extractor = mockExtractor
        )

        val request = RunRequest(
            inputSpecs = listOf(
                InputSpec("1", "valid input", "expected"),
                InputSpec("2", "", "expected"),
                InputSpec("3", "  ", "expected"),
                InputSpec("4", "another valid", "expected")
            )
        )

        val result = runner.run(request)

        assertEquals(2, result.dataPointsCount)
        assertEquals(2, mockGateway.runCount)
    }

    @Test
    fun `run invokes onDataPointOutput callback`() {
        val runner = TestableTracerRunner(
            gateway = mockGateway,
            projectDir = tempDir.toString(),
            extractor = mockExtractor
        )

        val outputs = mutableListOf<Pair<String, String>>()
        val request = RunRequest(
            inputSpecs = listOf(
                InputSpec("1", "input1", "expected1"),
                InputSpec("2", "input2", "expected2")
            ),
            onDataPointOutput = { id, output -> outputs.add(id to output) }
        )

        runner.run(request)

        assertEquals(2, outputs.size)
        assertEquals("1", outputs[0].first)
        assertEquals("gen-1", outputs[0].second)
        assertEquals("2", outputs[1].first)
        assertEquals("gen-2", outputs[1].second)
    }

    @Test
    fun `run invokes onDataPoint callback`() {
        val runner = TestableTracerRunner(
            gateway = mockGateway,
            projectDir = tempDir.toString(),
            extractor = mockExtractor
        )

        val dataPoints = mutableListOf<DataPoint>()
        val request = RunRequest(
            inputSpecs = listOf(
                InputSpec("1", "input1", "expected1"),
                InputSpec("2", "input2", "expected2")
            ),
            onDataPoint = { dp -> dataPoints.add(dp) }
        )

        runner.run(request)

        assertEquals(2, dataPoints.size)
        assertEquals("1", dataPoints[0].id)
        assertEquals("2", dataPoints[1].id)
    }

    @Test
    fun `run handles gateway exceptions gracefully`() {
        mockGateway.shouldThrow = true
        val runner = TestableTracerRunner(
            gateway = mockGateway,
            projectDir = tempDir.toString(),
            extractor = mockExtractor
        )

        val request = RunRequest(
            inputSpecs = listOf(
                InputSpec("1", "input1", "expected1"),
                InputSpec("2", "input2", "expected2")
            )
        )

        val result = runner.run(request)

        assertEquals(2, result.dataPointsCount)
        val dataPoints = readDataPoints(result.datasetPath)
        assertTrue(dataPoints[0].exception!!.contains("Gateway error"))
        assertTrue(dataPoints[1].exception!!.contains("Gateway error"))
    }

    @Test
    fun `run handles extractor exceptions gracefully`() {
        mockExtractor.shouldThrow = true
        val runner = TestableTracerRunner(
            gateway = mockGateway,
            projectDir = tempDir.toString(),
            extractor = mockExtractor
        )

        val request = RunRequest(
            inputSpecs = listOf(InputSpec("1", "input1", "expected1"))
        )

        val result = runner.run(request)

        assertEquals(1, result.dataPointsCount)
        val dataPoints = readDataPoints(result.datasetPath)
        assertNotNull(dataPoints[0].exception)
        assertTrue(dataPoints[0].exception!!.contains("Extractor error"))
    }

    @Test
    fun `run respects cancel flag during processing`() {
        val runner = TestableTracerRunner(
            gateway = mockGateway,
            projectDir = tempDir.toString(),
            extractor = mockExtractor
        )

        val cancelFlag = AtomicBoolean(false)
        mockGateway.onRun = { cancelFlag.set(true) }

        val request = RunRequest(
            inputSpecs = listOf(
                InputSpec("1", "input1", "expected1"),
                InputSpec("2", "input2", "expected2"),
                InputSpec("3", "input3", "expected3")
            ),
            cancelFlag = cancelFlag
        )

        val result = runner.run(request)

        assertTrue(result.dataPointsCount < 3)
        assertTrue(mockGateway.runCount < 3)
    }

    @Test
    fun `run respects cancel flag set during iteration`() {
        val runner = TestableTracerRunner(
            gateway = mockGateway,
            projectDir = tempDir.toString(),
            extractor = mockExtractor
        )

        val cancelFlag = AtomicBoolean(false)
        mockGateway.onRun = {
            if (mockGateway.runCount == 2) {
                cancelFlag.set(true)
            }
        }

        val request = RunRequest(
            inputSpecs = listOf(
                InputSpec("1", "input1", "expected1"),
                InputSpec("2", "input2", "expected2"),
                InputSpec("3", "input3", "expected3")
            ),
            cancelFlag = cancelFlag
        )

        val result = runner.run(request)

        assertTrue(result.dataPointsCount <= 2)
        assertTrue(mockGateway.runCount <= 2)
    }

    @Test
    fun `run normalizes datapoint ids in saved dataset`() {
        val runner = TestableTracerRunner(
            gateway = mockGateway,
            projectDir = tempDir.toString(),
            extractor = mockExtractor
        )

        val request = RunRequest(
            inputSpecs = listOf(
                InputSpec("placeholder-1", "input1", "expected1"),
                InputSpec("placeholder-2", "input2", "expected2"),
                InputSpec("placeholder-3", "input3", "expected3")
            )
        )

        val result = runner.run(request)

        val dataPoints = readDataPoints(result.datasetPath)
        assertEquals("1", dataPoints[0].id)
        assertEquals("2", dataPoints[1].id)
        assertEquals("3", dataPoints[2].id)
    }

    @Test
    fun `run sets runDatetime on all datapoints`() {
        val runner = TestableTracerRunner(
            gateway = mockGateway,
            projectDir = tempDir.toString(),
            extractor = mockExtractor
        )

        val request = RunRequest(
            inputSpecs = listOf(
                InputSpec("1", "input1", "expected1"),
                InputSpec("2", "input2", "expected2")
            )
        )

        val result = runner.run(request)

        val dataPoints = readDataPoints(result.datasetPath)
        assertNotNull(dataPoints[0].runDatetime)
        assertNotNull(dataPoints[1].runDatetime)
        assertEquals(dataPoints[0].runDatetime, dataPoints[1].runDatetime)
    }

    @Test
    fun `run uses same experiment id for all datapoints`() {
        val runner = TestableTracerRunner(
            gateway = mockGateway,
            projectDir = tempDir.toString(),
            extractor = mockExtractor
        )

        val request = RunRequest(
            inputSpecs = listOf(
                InputSpec("1", "input1", "expected1"),
                InputSpec("2", "input2", "expected2")
            )
        )

        val result = runner.run(request)

        val dataPoints = readDataPoints(result.datasetPath)
        assertEquals(result.experimentId, dataPoints[0].experimentId)
        assertEquals(result.experimentId, dataPoints[1].experimentId)
    }

    @Test
    fun `run preserves input spec id for callbacks`() {
        val runner = TestableTracerRunner(
            gateway = mockGateway,
            projectDir = tempDir.toString(),
            extractor = mockExtractor
        )

        val callbackIds = mutableListOf<String>()
        val request = RunRequest(
            inputSpecs = listOf(
                InputSpec("custom-id-1", "input1", "expected1"),
                InputSpec("custom-id-2", "input2", "expected2")
            ),
            onDataPointOutput = { id, _ -> callbackIds.add(id) }
        )

        runner.run(request)

        assertEquals("custom-id-1", callbackIds[0])
        assertEquals("custom-id-2", callbackIds[1])
    }

    @Test
    fun `run with inputPath packs input`() {
        val runner = TestableTracerRunner(
            gateway = mockGateway,
            projectDir = tempDir.toString(),
            extractor = mockExtractor,
            inputPath = "test_input_path"
        )

        val request = RunRequest(
            inputSpecs = listOf(InputSpec("1", "raw-input", "expected"))
        )

        runner.run(request)

        assertEquals(1, mockGateway.runCount)
        assertTrue(mockGateway.lastInput!!.contains("test_input_path"))
        assertTrue(mockGateway.lastInput!!.contains("raw-input"))
    }

    @Test
    fun `run without inputPath uses raw input`() {
        val runner = TestableTracerRunner(
            gateway = mockGateway,
            projectDir = tempDir.toString(),
            extractor = mockExtractor,
            inputPath = null
        )

        val request = RunRequest(
            inputSpecs = listOf(InputSpec("1", "raw-input", "expected"))
        )

        runner.run(request)

        assertEquals("raw-input", mockGateway.lastInput)
    }

    @Test
    fun `run handles empty input specs list`() {
        val runner = TestableTracerRunner(
            gateway = mockGateway,
            projectDir = tempDir.toString(),
            extractor = mockExtractor
        )

        val request = RunRequest(inputSpecs = emptyList())

        val result = runner.run(request)

        assertEquals(0, result.dataPointsCount)
        assertEquals(0, mockGateway.runCount)
    }

    @Test
    fun `run creates nested output directories`() {
        val runner = TestableTracerRunner(
            gateway = mockGateway,
            projectDir = tempDir.toString(),
            extractor = mockExtractor
        )

        val request = RunRequest(
            inputSpecs = listOf(InputSpec("1", "input", "expected")),
            outputRootDir = "nested/output/dir"
        )

        val result = runner.run(request)

        assertTrue(Files.exists(Path.of(result.datasetPath)))
        assertTrue(result.datasetPath.contains("nested/output/dir"))
    }

    @Test
    fun `run passes cancel flag to gateway`() {
        val cancelFlag = AtomicBoolean(false)
        val runner = TestableTracerRunner(
            gateway = mockGateway,
            projectDir = tempDir.toString(),
            extractor = mockExtractor
        )

        val request = RunRequest(
            inputSpecs = listOf(InputSpec("1", "input", "expected")),
            cancelFlag = cancelFlag
        )

        runner.run(request)

        assertNotNull(mockGateway.lastCancelFlag)
        assertSame(cancelFlag, mockGateway.lastCancelFlag)
    }

    @Test
    fun `run handles exception with empty message`() {
        mockGateway.throwWithEmptyMessage = true
        val runner = TestableTracerRunner(
            gateway = mockGateway,
            projectDir = tempDir.toString(),
            extractor = mockExtractor
        )

        val request = RunRequest(
            inputSpecs = listOf(InputSpec("1", "input", "expected"))
        )

        val result = runner.run(request)

        val dataPoints = readDataPoints(result.datasetPath)
        assertNotNull(dataPoints[0].exception)
        assertFalse(dataPoints[0].exception!!.isBlank())
    }

    private fun readDataPoints(path: String): List<DataPoint> {
        val mapper = com.fasterxml.jackson.module.kotlin.jacksonObjectMapper()
        val type = mapper.typeFactory.constructCollectionType(
            java.util.List::class.java,
            DataPoint::class.java
        )
        return Files.newBufferedReader(Path.of(path)).use { br ->
            mapper.readValue(br, type)
        }
    }

    private class TestableTracerRunner(
        private val gateway: MockGateway,
        projectDir: String,
        extractor: BaseExtractor,
        inputPath: String? = null
    ) : RunnerBase() {
        private val projectDirPath = projectDir
        private val extractorInstance = extractor
        private val inputPathInstance = inputPath

        override fun run(request: RunRequest): RunResult {
            val experimentId = newExperimentId()
            val outRoot = Path.of(projectDirPath, request.outputRootDir)
            val datasetsDir = outRoot.resolve("eval")

            Files.createDirectories(datasetsDir)

            val dataPoints = mutableListOf<DataPoint>()
            val runDateTime = java.time.LocalDateTime.now().toString()

            for (spec in request.inputSpecs) {
                if (request.cancelFlag?.get() == true) break
                val rawInput = spec.input.trim()
                if (rawInput.isEmpty()) continue

                val packedInput = if (inputPathInstance != null) {
                    "packed:$inputPathInstance:$rawInput"
                } else {
                    rawInput
                }

                val baseId = spec.id
                try {
                    val raw = gateway.runOnce(packedInput, request.cancelFlag)
                    val rawMap = mapOf("events" to raw.rootEvents.map { it.payload })

                    var dp = extractorInstance.extract(
                        raw = rawMap,
                        fallback = FallbackContext(
                            id = baseId,
                            input = rawInput,
                            outputExpected = spec.expectedOutput,
                            experimentId = experimentId
                        )
                    )
                    dp = dp.copy(runDatetime = runDateTime)
                    dp = dp.copy(id = baseId)

                    if (request.cancelFlag?.get() == true) {
                        throw java.util.concurrent.CancellationException("Canceled")
                    }
                    dataPoints.add(dp)
                    request.onDataPointOutput?.invoke(baseId, dp.outputGen)
                    request.onDataPoint?.invoke(dp)
                } catch (ce: java.util.concurrent.CancellationException) {
                    break
                } catch (t: Throwable) {
                    val errorMsg = t.message?.takeIf { it.isNotBlank() } ?: (t::class.simpleName ?: "Error")
                    val dp = DataPoint(
                        id = baseId,
                        input = rawInput,
                        outputGen = "",
                        outputExpected = spec.expectedOutput ?: "",
                        experimentId = experimentId,
                        raw = emptyMap(),
                        runDatetime = runDateTime,
                        exception = errorMsg,
                    )
                    dataPoints.add(dp)
                    request.onDataPointOutput?.invoke(baseId, dp.exception ?: "Error")
                }
            }

            val datasetPath = datasetsDir.resolve("$experimentId.json")
            val normalized = dataPoints.mapIndexed { idx, dp -> dp.copy(id = (idx + 1).toString()) }

            val mapper = com.fasterxml.jackson.module.kotlin.jacksonObjectMapper()
            val json = mapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(normalized)
            Files.write(datasetPath, json)

            return RunResult(
                experimentId = experimentId,
                datasetPath = datasetPath.toString(),
                dataPointsCount = dataPoints.size
            )
        }
    }

    private class MockGateway {
        var runCount = 0
        var shouldThrow = false
        var throwWithEmptyMessage = false
        var lastInput: String? = null
        var lastCancelFlag: AtomicBoolean? = null
        var onRun: (() -> Unit)? = null

        fun runOnce(input: String, cancelFlag: AtomicBoolean?): HierarchicalTraceEventsState {
            runCount++
            lastInput = input
            lastCancelFlag = cancelFlag
            onRun?.invoke()

            if (shouldThrow) {
                throw RuntimeException("Gateway error")
            }
            if (throwWithEmptyMessage) {
                throw RuntimeException("")
            }

            return createMockState()
        }

        private fun createMockState(): HierarchicalTraceEventsState {
            val event = HierarchicalTraceEvent(
                id = "evt-$runCount",
                name = "test-event",
                type = EventType.ToolCall,
                framework = Framework.LangGraph,
                timestampStartMs = System.currentTimeMillis(),
                timestampEndMs = System.currentTimeMillis(),
                finished = true,
                payload = mapOf("output" to "test-output-$runCount"),
                children = emptyList()
            )
            return HierarchicalTraceEventsState(rootEvents = listOf(event))
        }
    }

    private class MockExtractor : BaseExtractor {
        var extractCount = 0
        var shouldThrow = false
        var onExtract: (() -> Unit)? = null

        override fun extract(raw: Map<String, Any?>, fallback: FallbackContext): DataPoint {
            extractCount++
            onExtract?.invoke()

            if (shouldThrow) {
                throw RuntimeException("Extractor error")
            }

            return DataPoint(
                id = fallback.id,
                input = fallback.input,
                outputGen = "gen-${fallback.id}",
                outputExpected = fallback.outputExpected ?: "",
                experimentId = fallback.experimentId ?: "exp-test",
                raw = raw
            )
        }
    }
}
