package com.intellij.aidebugger.common.viewModels

import com.intellij.aidebugger.common.AiDebuggerCollector
import com.intellij.aidebugger.common.models.DebuggerSession
import com.intellij.aidebugger.common.models.HierarchicalTraceEventsState
import com.intellij.aidebugger.common.models.RequirementsNotMetInfo
import com.intellij.aidebugger.common.models.SessionCounters
import com.intellij.aidebugger.common.models.TraceEventsRepository
import com.intellij.aidebugger.common.models.TraceEventsState
import com.intellij.aidebugger.common.models.buildHierarchicalStructure
import com.intellij.aidebugger.common.models.entities.EmptyStackTrace
import com.intellij.aidebugger.common.models.entities.EventStackFrame
import com.intellij.aidebugger.common.models.entities.EventType
import com.intellij.aidebugger.common.models.entities.PayloadKey
import com.intellij.aidebugger.common.models.entities.SimpleGraph
import com.intellij.aidebugger.common.models.entities.TraceEvent
import com.intellij.aidebugger.common.models.entities.getPayloadOr
import com.intellij.aidebugger.common.models.serializeTraceEventsState
import com.intellij.aidebugger.common.services.DebuggerService
import com.intellij.aidebugger.common.services.GlobalSettingsService
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import kotlinx.collections.immutable.PersistentSet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.nio.file.Files
import java.nio.file.Paths
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter


@OptIn(ExperimentalCoroutinesApi::class)
class TraceEventsSessionVM(
    private val project: Project,
    private val eventsRepository: TraceEventsRepository,
    private val sessionCounters: SessionCounters,
    coroutineScope: CoroutineScope,
): SessionVM {

    companion object {
        private val TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS")
            .withZone(ZoneId.systemDefault())
    }

    // Make scope publicly accessible for ViewModel initialization
    val scope: CoroutineScope = coroutineScope

    // Current session reference, set when session starts
    private var currentSession: DebuggerSession? = null

    internal fun setCurrentSession(session: DebuggerSession) {
        this.currentSession = session
    }

    val isSessionFinished: StateFlow<Boolean> = eventsRepository.finished
    // TODO:
    val requirementsNotMet: StateFlow<RequirementsNotMetInfo> = eventsRepository.requirementsNotMet

    val debugMode: StateFlow<Boolean> = GlobalSettingsService.getInstance().debuggingFunctionality
        .stateIn(
            scope = coroutineScope,
            started = SharingStarted.Eagerly,
            initialValue = false
        )

    private val _knownThreads = mutableSetOf<String>()
    private val _selectedThreadId = MutableStateFlow<String?>(null)

    private val _hstate = MutableStateFlow(HierarchicalTraceEventsState(rootEvents = emptyList()))
    val hstate: StateFlow<HierarchicalTraceEventsState> = _hstate

    val selectedThread: StateFlow<SessionThreadVM?> = _selectedThreadId
        .combine(DebuggerService.getInstance(project).threadToSession) { threadId, threadToSession ->
            threadId to threadToSession
        }
        .flatMapLatest { (threadId, threadToSession) ->
            if (threadId == null) {
                return@flatMapLatest flowOf(null)
            }

            val threadOwner = threadToSession[threadId]

            if (threadOwner != null && threadOwner.sessionId != currentSession?.sessionId) {
                // Thread from a different session. Get it from that session's repository
                val otherRepo = threadOwner.repository as? TraceEventsRepository
                otherRepo?.state?.map { state -> state.events[threadId]?.let { toGroupVM(it, state) } }
                    ?: flowOf(null)
            } else {
                // Thread from the current session
                eventsRepository.state.map { state ->
                    state.events[threadId]?.let { toGroupVM(it, state) }
                }
            }
        }
        .stateIn(
            scope = coroutineScope,
            started = SharingStarted.Lazily,
            initialValue = null
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    private val currentSessionThreads: StateFlow<SessionThreads> = eventsRepository.state
        .flatMapLatest { state ->
            val rootEvents = state.roots.map { id -> state.events[id]!! }

            flowOf(value = SessionThreads(
                activeThreads = rootEvents
                    .filter { event -> !event.finished }
                    .map { event -> toGroupVM(event, state) },
                finishedThreads = rootEvents
                    .filter { event -> event.finished }
                    .map { event -> toGroupVM(event, state) },
            ))
        }
        .stateIn(
            scope = coroutineScope,
            started = SharingStarted.Lazily,
            initialValue = SessionThreads(emptyList(), emptyList())
        )

    // Combine current session threads with accumulated threads from all sessions
    @OptIn(ExperimentalCoroutinesApi::class)
    val threads: StateFlow<SessionThreads> =
        currentSessionThreads
            .combine(DebuggerService.getInstance(project).accumulatedThreads) { current, accumulated ->
                val currentSessionThreads = (current.activeThreads + current.finishedThreads)
                    .map { it.threadId }.toSet()

                val storedThreads = accumulated.values.filter { it.threadId !in currentSessionThreads }

                // All stored threads from accumulator are finished (from previous sessions)
                // Current session may have both active and finished threads
                SessionThreads(
                    activeThreads = current.activeThreads,
                    finishedThreads = current.finishedThreads + storedThreads
                )
            }
            .stateIn(
                scope = coroutineScope,
                started = SharingStarted.Lazily,
                initialValue = SessionThreads(emptyList(), emptyList())
            )

    // Look up events from the correct session for the selected thread
    @OptIn(ExperimentalCoroutinesApi::class)
    val events: StateFlow<List<EventVM>> =
        _selectedThreadId
            .combine(DebuggerService.getInstance(project).threadToSession) { threadId, threadToSession ->
                threadId to threadToSession
            }
            .flatMapLatest { (selectedThreadId, threadToSession) ->
                if (selectedThreadId != null) {
                    // Find the session that owns this thread
                    val threadOwner = threadToSession[selectedThreadId]

                    if (threadOwner != null && threadOwner.sessionId != currentSession?.sessionId) {
                        // Thread from a different session, get events from that session
                        val otherRepo = threadOwner.repository as? TraceEventsRepository
                        otherRepo?.state?.map { state ->
                            val eventIds = state.events[selectedThreadId]?.childIds ?: emptyList()
                            eventIds.map { id -> state.events[id]!! }.map { event -> toEventVM(event, state) }
                        } ?: flowOf(emptyList())
                    } else {
                        // Thread is from the current session
                        eventsRepository.state.map { state ->
                            val eventIds = state.events[selectedThreadId]?.childIds ?: emptyList()
                            eventIds.map { id -> state.events[id]!! }.map { event -> toEventVM(event, state) }
                        }
                    }
                } else {
                    // No thread selected, show the current session's root events
                    eventsRepository.state.map { state ->
                        state.roots.map { id -> state.events[id]!! }.map { event -> toEventVM(event, state) }
                    }
                }
            }
            .stateIn(
                scope = coroutineScope,
                started = SharingStarted.Lazily,
                initialValue = emptyList()
            )

    init {
        coroutineScope.launch {
            // Listen to current session threads only (not combined) to add to the accumulator
            currentSessionThreads.collect { sessionThreads ->
                // Process all threads (active and finished) and update the accumulator
                val allThreads = sessionThreads.activeThreads + sessionThreads.finishedThreads

                for (thread in allThreads) {
                    val isNew = thread.threadId !in _knownThreads

                    if (isNew) {
                        _knownThreads.add(thread.threadId)
                        // Select the active thread
                        if (thread.isActive) {
                            setSelectedThread(thread.threadId)
                        }
                    }

                    // Update the thread in accumulator (to reflect active -> finished transitions)
                    currentSession?.let { session ->
                        DebuggerService.getInstance(project).addThread(thread, session)
                    }
                }
            }
        }

        coroutineScope.launch {
            eventsRepository.state.collect { flatState ->
                _hstate.value = buildHierarchicalStructure(flatState)
            }
        }
    }

    fun setSelectedThread(threadId: String?) {
        _selectedThreadId.value = threadId
        sessionCounters.reportLastSelectedThread(threadId)
    }

    override fun onClose() {
    }

    private fun toGroupVM(event: TraceEvent, state: TraceEventsState): EventsGroupVM {
        val formattedTime = TIME_FORMATTER.format(Instant.ofEpochMilli(event.timestampStartMs))

        val stackTrace = event.getPayloadOr(PayloadKey.StackTrace, listOf<EventStackFrame>())

        // Use the provided state instead of eventsRepository.state.value for cross-session support
        val parentName = state.events[event.parentId]?.name
        val title = if (parentName != null) {
            "$formattedTime ($parentName > ${event.name})"
        } else {
            "$formattedTime (${event.name})"
        }

        return EventsGroupVM(
            threadId = event.id,
            title = title,
            isActive = !event.finished,
            stackTrace = stackTrace,
            graph = event.getPayloadOr(PayloadKey.Graph, SimpleGraph.Empty),
            lastEventName = MutableStateFlow(null)
        )
    }

    private fun toEventVM(event: TraceEvent, state: TraceEventsState): EventVM {
        return when (event.type) {
            EventType.ToolCall,
            EventType.LlmCall,
            EventType.General,
            EventType.Group -> {
                val nicerViewVms = getNicerViews(event)

                if (event.finished && nicerViewVms.isEmpty()) {
                    AiDebuggerCollector.reportNoPrettyViewFound()
                }

                val childrenIds = event.childIds

                TraceEventVM(
                    name = event.name,
                    eventType = event.type,
                    framework = event.framework,
                    finished = event.finished,
                    children = idsAsVMs(childrenIds, state),
                    niceVms = nicerViewVms,
                    widgets = getNodeWidgets(event),
                    rawJson = serializeTraceEventsState(state, event.id),
                    defaultExpanded = event.type != EventType.General || event.payload.containsKey(PayloadKey.Exception)
                )
            }
            EventType.Exception -> {
                val stackTrace = event.getPayloadOr(
                    PayloadKey.StackTrace,
                    EmptyStackTrace
                )

                ExceptionEventVM(
                    name = "Exception",
                    exception = event.name,
                    stackTrace = stackTrace
                )
            }
            else -> throw IllegalArgumentException("Unknown event type: ${event.type}")
        }
    }

    private fun idsAsVMs(ids: PersistentSet<String>, state: TraceEventsState): List<EventVM> {
        return ids.map { id -> state.events[id]!! }.map { event -> toEventVM(event, state) }
    }

    fun saveState() {
        val serializedState = serializeTraceEventsState(eventsRepository.state.value)

        val timestamp = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")
            .withZone(ZoneId.systemDefault())
            .format(Instant.now())

        val fileName = "trace_events_state_$timestamp.json"
        val projectDir = System.getProperty("user.dir")
        val filePath = Paths.get(projectDir, fileName)

        Files.write(filePath, serializedState.toByteArray())
    }

    fun saveState(project: Project) {
        val fileName = "trace.json"
        val serializedState = serializeTraceEventsState(eventsRepository.state.value)
        val basePath = project.basePath ?: return  // no physical project on disk
        val baseDir = LocalFileSystem.getInstance().refreshAndFindFileByPath(basePath) ?: return

        WriteAction.run<RuntimeException> {
            val file = baseDir.findChild(fileName) ?: baseDir.createChildData(this, fileName)
            VfsUtil.saveText(file, serializedState) // or: file.setBinaryContent(json.toByteArray())
        }
    }
}