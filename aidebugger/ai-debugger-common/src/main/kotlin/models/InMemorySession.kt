package com.intellij.aidebugger.common.models

import com.intellij.aidebugger.common.viewModels.TraceEventsSessionVM
import com.intellij.openapi.project.Project
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class InMemorySession(
    override val sessionId: String,
    override val repository: TraceEventsRepository,
    override val vm: TraceEventsSessionVM,
    override val sessionCounters: SessionCounters,
) : DebuggerSession {

    override suspend fun start(project: Project) {
        vm.setCurrentSession(this)
    }

    override suspend fun stop() {
    }

    override fun dispose() {
    }
}

fun createInMemorySession(project: Project, state: TraceEventsState): DebuggerSession {
    val sessionScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    val sessionCounters = SessionCountersImpl()
    val repository = InMemoryRepository(state)

    val sessionWithThreadsVm = TraceEventsSessionVM(
        project = project,
        eventsRepository = repository,
        sessionCounters = sessionCounters,
        coroutineScope = sessionScope,
    )

    val session = InMemorySession(
        sessionId = "InMemorySession-${System.currentTimeMillis()}",
        repository = repository,
        vm = sessionWithThreadsVm,
        sessionCounters = sessionCounters,
    )

    return session
}