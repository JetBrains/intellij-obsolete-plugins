package com.intellij.aidebugger.koog.listener

import com.intellij.aidebugger.common.services.LifecycleService
import com.intellij.aidebugger.koog.AiDebuggerKoogBundle
import com.intellij.aidebugger.koog.execution.KoogLibDependencyDetector
import com.intellij.execution.ExecutionListener
import com.intellij.execution.runners.ExecutionEnvironment

/**
 * A custom execution listener to detect Koog library version mismatch when an execution is starting.
 *
 * This listener integrates with the execution pipeline to ensure that Koog library dependencies
 * are verified before starting an execution process. The listener checks whether the appropriate
 * Koog library is included in the current project's dependencies.
 *
 * Specifically, this implementation verifies the validity of the Koog library used in the project.
 * If the library is found but doesn't meet the required criteria, it updates the message displayed
 * in the AI Debugger tool window to inform the user about the outdated or incompatible library.
 */
class KoogAiDebuggerExecutionListener : ExecutionListener {

    override fun processStarting(executorId: String, env: ExecutionEnvironment) {

        if (KoogLibDependencyDetector.getInstance(env.project).isIncludeKoogLibrary()) {
            val isValidKoogLibrary = KoogLibDependencyDetector.getInstance(env.project).isIncludeApplicableKoogLibrary()
            if (!isValidKoogLibrary) {
                // Update the empty feed message in the AI Debugger tool window
                // for a case when AI Debugger session and repository are not yet created.
                LifecycleService.getInstance(env.project).aiDebuggerEmptyFeedMessage.value =
                    AiDebuggerKoogBundle.message("aitoolkit.feed.outdatedKoog.text")
            }
        }

        super.processStarting(executorId, env)
    }
}