package com.intellij.aidebugger.koog.session

import ai.koog.agents.core.feature.message.FeatureMessage
import ai.koog.agents.core.feature.remote.client.config.DefaultClientConnectionConfig
import com.intellij.aidebugger.common.models.SessionCounters
import com.intellij.aidebugger.common.models.SessionCountersImpl
import com.intellij.aidebugger.common.models.TraceEventsRepository
import com.intellij.aidebugger.common.models.TraceSession
import com.intellij.aidebugger.common.services.LifecycleService
import com.intellij.aidebugger.common.services.network.DebuggerTransport
import com.intellij.aidebugger.common.viewModels.TraceEventsSessionVM
import com.intellij.openapi.project.Project
import com.intellij.platform.util.coroutines.childScope
import io.ktor.http.URLProtocol
import kotlinx.coroutines.CoroutineScope

/**
 * Represents a session for handling and managing Koog event traces.
 * This class extends [TraceSession] to provide specific functionality for Koog events.
 *
 * @constructor
 * Initializes the KoogEventsSession with the provided configuration and components.
 *
 * @param sessionId Unique identifier of the session.
 * @param coroutineScope The coroutine scope used for managing the lifecycle of asynchronous tasks within the session.
 * @param repository The repository handling trace event data for the session.
 * @param transport The transport mechanism for receiving and managing debugger feature messages.
 * @param vm The view model responsible for managing the session's state and data.
 * @param sessionCounters The counters used for tracking session metrics or events.
 */
class KoogEventsSession(
    sessionId: String,
    coroutineScope: CoroutineScope,
    repository: TraceEventsRepository,
    transport: DebuggerTransport<FeatureMessage>,
    vm: TraceEventsSessionVM,
    sessionCounters: SessionCounters,
): TraceSession<FeatureMessage>(sessionId, coroutineScope, repository, vm, sessionCounters, transport)

/**
 * Creates a new instance of a [KoogEventsSession] for handling and managing Koog event sessions.
 *
 * @param project The current project context in which the session is created.
 * @param port An optional port number to establish the connection. Defaults to a predefined port if null.
 * @return A [KoogEventsSession] instance initialized with the specified project and connection settings.
 */
fun createKoogEventsSession(project: Project, port: Int): KoogEventsSession {
    val coroutineScope = LifecycleService.getInstance(project).coroutineScope
    @Suppress("UnstableApiUsage")
    val sessionScope = coroutineScope.childScope("KoogSession")

    val sessionCounters = SessionCountersImpl()

    val connectionConfig = DefaultClientConnectionConfig(
        host = "127.0.0.1",
        port = port,
        protocol = URLProtocol.HTTP,
    )

    val debuggerTransport = KoogDebuggerTransport(
        coroutineScope = sessionScope,
        connectionConfig = connectionConfig
    )

    val repository = KoogTraceEventsRepository(
        project = project,
        coroutineScope = sessionScope,
        transport = debuggerTransport
    )

    repository.startEventsProcessing()

    val viewModel = TraceEventsSessionVM(
        project = project,
        eventsRepository = repository,
        sessionCounters = sessionCounters,
        coroutineScope = sessionScope,
    )

    val session = KoogEventsSession(
        sessionId = "KoogEventsSession-$port",
        coroutineScope = sessionScope,
        repository = repository,
        transport = debuggerTransport,
        vm = viewModel,
        sessionCounters = sessionCounters,
    )

    return session
}
