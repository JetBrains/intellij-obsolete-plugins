package com.intellij.aidebugger.evaluation.models.runner

import com.intellij.aidebugger.common.models.serializeHierarchicalTraceEventsStateToMap
import com.intellij.aidebugger.evaluation.models.entities.DataPoint
import com.intellij.aidebugger.evaluation.models.extractor.BaseExtractor
import com.intellij.aidebugger.evaluation.models.extractor.FallbackContext
import com.intellij.aidebugger.evaluation.models.extractor.InputStructurePacker
import com.intellij.aidebugger.evaluation.models.storage.JsonFileStorage
import java.nio.file.Path
import java.time.LocalDateTime
import java.util.concurrent.CancellationException
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Runs agent executions via AiDebuggerGateway and builds datasets.
 * - Raw traces go to:   {outputRootDir}/traces/{experimentId}/{dataPointId}_r{idx}.json
 * - Dataset goes to:    {outputRootDir}/eval/{experimentId}.json
 */
class TracerRunner(
    private val gateway: AiDebuggerGateway,
    private val projectDir: String,
    private val extractor: BaseExtractor,
    private val inputPath: String? = null
) : RunnerBase() {
    override fun run(request: RunRequest): RunResult {
        val experimentId = newExperimentId()
        val outRoot = Path.of(projectDir, request.outputRootDir)
        val datasetsDir = outRoot.resolve("eval")

        JsonFileStorage.ensureDir(datasetsDir)

        val dataPoints = mutableListOf<DataPoint>()
        val runDateTime = LocalDateTime.now().toString()

        run loop@{
            for (spec in request.inputSpecs) {
                if (request.cancelFlag?.get() == true) break
                val rawInput = spec.input.trim()
                if (rawInput.isEmpty()) continue

                // Pack input structure if inputPath is provided
                val packedInput = if (inputPath != null) {
                    // NEW: Use reconstructInput when structure is available
                    if (spec.inputStruct != null) {
                        InputStructurePacker.reconstructInput(inputPath, rawInput, spec.inputStruct)
                    } else {
                        InputStructurePacker.packInput(inputPath, rawInput)
                    }
                } else {
                    rawInput
                }

                val baseId = spec.id
                try {
                    val raw = runTracing(
                        input = packedInput,
                        cancelFlag = request.cancelFlag
                    )
                    var dp = extractor.extract(
                        raw = raw,
                        fallback = FallbackContext(
                            id = baseId,
                            input = rawInput,
                            outputExpected = spec.expectedOutput,
                            experimentId = experimentId
                        )
                    )
                    // add run datetime for dataset consumers
                    dp = dp.copy(runDatetime = runDateTime)
                    // Align datapoint id with the placeholder id used in the UI table to prevent re-rendering
                    dp = dp.copy(id = baseId)
                    // If canceled at this point, do not record or display further outputs
                    if (request.cancelFlag?.get() == true) {
                        throw CancellationException("Canceled")
                    }
                    dataPoints.add(dp)
                    // Use baseId (spec.id) to match placeholder rows in UI during live updates
                    request.onDataPointOutput?.invoke(baseId, dp.outputGen)
                    // Invoke optional callback with full DataPoint for further processing (e.g., evaluation)
                    request.onDataPoint?.invoke(dp)
                } catch (ce: CancellationException) {
                    // stop processing further items
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
        }

        val datasetPath = datasetsDir.resolve("$experimentId.json")
        // Normalize IDs to sequential human-friendly values (1..N) for on-disk datasets
        val normalized = dataPoints.mapIndexed { idx, dp -> dp.copy(id = (idx + 1).toString()) }
        JsonFileStorage.writeJson(datasetPath, normalized)

        return RunResult(
            experimentId = experimentId,
            datasetPath = datasetPath.toString(),
            dataPointsCount = dataPoints.size
        )
    }

    private fun runTracing(
        input: String,
        cancelFlag: AtomicBoolean? = null,
    ): Map<String, Any?> {
        val state = gateway.runOnce(input, cancelFlag)
        require(state.rootEvents.isNotEmpty()) { "No events received from ai-debugger transport" }
        val raw = serializeHierarchicalTraceEventsStateToMap(state)
        return raw
    }
}