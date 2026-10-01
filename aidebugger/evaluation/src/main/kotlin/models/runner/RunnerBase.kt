package com.intellij.aidebugger.evaluation.models.runner

import com.intellij.aidebugger.evaluation.models.entities.DataPoint
import com.intellij.aidebugger.evaluation.models.entities.InputSpec
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

abstract class RunnerBase {

    protected fun newExperimentId(): String {
        val ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
        return "exp_${ts}_${UUID.randomUUID().toString()}"
    }

    abstract fun run(request: RunRequest): RunResult
}

data class RunRequest(
    val inputSpecs: List<InputSpec>,
    val outputRootDir: String = ".jbeval",
    val cancelFlag: AtomicBoolean? = null,
    val onDataPointOutput: ((String, String) -> Unit)? = null,
    val onDataPoint: ((DataPoint) -> Unit)? = null
)

data class RunResult(
    val experimentId: String,
    val datasetPath: String,
    val dataPointsCount: Int
)
