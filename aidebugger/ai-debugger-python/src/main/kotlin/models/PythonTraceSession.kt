package com.intellij.aidebugger.python.models

import com.intellij.aidebugger.common.models.SessionCounters
import com.intellij.aidebugger.common.models.SessionCountersImpl
import com.intellij.aidebugger.common.models.TraceSession
import com.intellij.aidebugger.common.services.network.server.DebuggerServerTransportConfig
import com.intellij.aidebugger.common.services.network.server.DebuggerSimpleServerTransport
import com.intellij.aidebugger.common.viewModels.TraceEventsSessionVM
import com.intellij.aidebugger.python.serialization.asSerializableTraceEvents
import com.intellij.openapi.project.Project
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.receiveAsFlow

class PythonTraceSession(
    sessionId: String,
    coroutineScope: CoroutineScope,
    repository: CommonTraceEventsRepository,
    vm: TraceEventsSessionVM,
    sessionCounters: SessionCounters,
    transport: DebuggerSimpleServerTransport,
) : TraceSession<String>(sessionId, coroutineScope, repository, vm, sessionCounters, transport)

fun createPythonTraceSession(port: Int, project: Project): PythonTraceSession {
    val sessionScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    val sessionCounters = SessionCountersImpl()

    val transport = DebuggerSimpleServerTransport(
        DebuggerServerTransportConfig(
            host = "localhost",
            port = port,
        ),
        sessionScope
    )

    val eventsFlow = transport.incoming
        .receiveAsFlow()
        .asSerializableTraceEvents()

    val repository = CommonTraceEventsRepository(
        sessionScope,
        eventsFlow,
        sessionCounters,
    )

    val sessionWithThreadsVm = TraceEventsSessionVM(
        project = project,
        eventsRepository = repository,
        sessionCounters = sessionCounters,
        coroutineScope = sessionScope,
    )

    val session = PythonTraceSession(
        "PythonSession-$port",
        sessionScope,
        repository,
        sessionWithThreadsVm,
        sessionCounters,
        transport
    )

    return session
}