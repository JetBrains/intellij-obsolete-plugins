package com.intellij.aidebugger.common.models

import com.intellij.aidebugger.common.AiDebuggerCollector
import com.intellij.aidebugger.common.services.GlobalSettingsService
import com.intellij.aidebugger.common.services.ProjectSettingsService
import com.intellij.aidebugger.common.services.network.DebuggerTransport
import com.intellij.aidebugger.common.toolWindow.openAiDebuggerToolWindow
import com.intellij.aidebugger.common.viewModels.TraceEventsSessionVM
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

abstract class TraceSession<TMessage>(
    override val sessionId: String,
    val coroutineScope: CoroutineScope,
    override val repository: TraceEventsRepository,
    override val vm: TraceEventsSessionVM,
    override val sessionCounters: SessionCounters,
    val transport: DebuggerTransport<TMessage>
): DebuggerSession where TMessage : Any {

    companion object {
        private val logger = thisLogger()
    }

    override suspend fun start(project: Project) {
        logger.info("$sessionId. Starting trace session")

        // Register the started session in the View Model
        vm.setCurrentSession(this)

        // Start a transport
        transport.start()

        // Start a coroutine to report the requirements not met signal.
        coroutineScope.launch {
            repository.requirementsNotMet.first { it.isNotMet }
            AiDebuggerCollector.reportRuntimeRequirementsNotMet(project)
        }

        // Start a coroutine to report the first event received signal.
        coroutineScope.launch {
            sessionCounters
                .eventsCount
                .filter { counters -> counters.isNotEmpty() }
                .first()
            val repositoryEventsState = repository.state.first()
            val rootEvent = repositoryEventsState.roots.take(1).firstOrNull()?.let { repositoryEventsState.events[it] }
            AiDebuggerCollector.reportFirstEventReceived(project, rootEvent?.framework)

            if (GlobalSettingsService.getInstance().showDebuggerOnRun.value) {
                openAiDebuggerToolWindow(project)
            }
        }

        if (!ProjectSettingsService.getInstance(project).isAiProject) {
            ProjectSettingsService.getInstance(project).importsOfInterestPresent = true
        }
    }

    override suspend fun stop() {
        logger.info("$sessionId. Stopping trace session")
        transport.stop()
        AiDebuggerCollector.reportSessionFinished(threadsCount = sessionCounters.threadsCount.value)
    }

    override fun dispose() {
        coroutineScope.cancel()
    }
}
