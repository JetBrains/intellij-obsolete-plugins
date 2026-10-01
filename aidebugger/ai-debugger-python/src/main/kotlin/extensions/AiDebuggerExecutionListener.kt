package com.intellij.aidebugger.python.extensions

import com.intellij.aidebugger.common.AiDebuggerCollector
import com.intellij.aidebugger.python.utility.toFusString
import com.intellij.execution.ExecutionListener
import com.intellij.execution.runners.ExecutionEnvironment

class AiDebuggerExecutionListener: ExecutionListener {
    companion object {
        val allSupportedExecutors = AiDebuggerRunner.supportedExecutors + AiDebuggerDebugRunner.supportedExecutors
    }

    override fun processStartScheduled(executorId: String, env: ExecutionEnvironment) {
        val project = env.project
        val profile = env.runProfile

        val skipReason = RunnerCommon.getSkipReason(executorId, profile, allSupportedExecutors)
        if (skipReason != null) {
            AiDebuggerCollector.reportCustomRunnerSkipped(
                project,
                skipReason.toFusString()
            )
        }
    }
}