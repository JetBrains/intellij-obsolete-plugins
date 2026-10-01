package com.intellij.aidebugger.python.models

import com.intellij.aidebugger.common.models.SessionCounters
import com.intellij.aidebugger.common.models.TraceEventsRepositoryBase
import com.intellij.aidebugger.common.models.entities.BasicTrace
import com.intellij.aidebugger.common.models.entities.EventStackFrame
import com.intellij.aidebugger.common.models.entities.EventType
import com.intellij.aidebugger.common.models.entities.InstantTrace
import com.intellij.aidebugger.common.models.entities.PayloadKey
import com.intellij.aidebugger.common.models.entities.SerializableTraceEvent
import com.intellij.aidebugger.common.models.entities.SpanEnterTrace
import com.intellij.aidebugger.common.models.entities.SpanExitTrace
import com.intellij.aidebugger.common.models.entities.SpanTrace
import com.intellij.aidebugger.common.models.entities.TraceEvent
import com.intellij.aidebugger.common.models.entities.getPayloadOr
import com.intellij.aidebugger.python.viewModels.getLastMessage
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class CommonTraceEventsRepository(
    coroutineScope: CoroutineScope,
    private val eventsFlow: Flow<SerializableTraceEvent>,
    private val sessionCounters: SessionCounters,
): TraceEventsRepositoryBase() {
    init {
        coroutineScope.launch {
            eventsFlow.collect { dispatchEvent(it) }
            _finished.value = true
        }
    }

    private fun dispatchEvent(event: SerializableTraceEvent) {
        // TODO parent id through attaching to root is done here, but we do it in python already!
        // so probably no changes required here as parentId will always exist anyway...
//        val parentId = (event as? BasicTrace)?.parentId?.let { getRootId(it) }  // attaches event to the common root (level 0 root) or make it new root
//        val parentId = (event as? BasicTrace)?.parentId?.let { getGroupRootId(it) }  // attaches event to its true root or make it the new root
        val parentId = (event as? BasicTrace)?.parentId  // attaches event to its true root or make it the new root

        when (event) {
            is SpanTrace -> createOrUpdateEvent(
                eventId = event.id,
                parentId = parentId,
                onCreate = {
                    reportEvent(parentId)

                    TraceEvent(
                        id = event.id,
                        parentId = parentId,
                        childIds = persistentSetOf(),
                        name = event.name,
                        type = event.eventType,
                        framework = event.framework,
                        timestampStartMs = getStartTimestamp(event) ?: 0,
                        timestampEndMs = getEndTimestamp(event) ?: 0,
                        finished = false,
                        payload = event.payload,
                    )
                },
                onUpdate = { traceEvent -> traceEvent.copy(
                    type = getLangGraphNodeType(event) ?: traceEvent.type,
                    timestampStartMs = getStartTimestamp(event) ?: traceEvent.timestampStartMs,
                    timestampEndMs = getEndTimestamp(event) ?: traceEvent.timestampEndMs,
                    finished = traceEvent.finished || event is SpanExitTrace,
                    payload = traceEvent.payload + event.payload,
                ) }
            )
            is InstantTrace -> {
                reportEvent(parentId)

                putEventIntoState(
                    parentId = parentId,
                    traceEvent = TraceEvent(
                        event.id,
                        parentId,
                        childIds = persistentSetOf(),
                        name = event.name,
                        type = event.eventType,
                        framework = event.framework,
                        timestampStartMs = event.timestampMs,
                        timestampEndMs = event.timestampMs,
                        finished = true,
                        payload = event.payload
                    )
                )
            }
            else -> throw IllegalArgumentException("Unexpected event type: ${event::class.simpleName}")
        }
    }

    private fun reportEvent(parentId: String?) {
        if (parentId == null) {
            sessionCounters.incrementThreadsCount()
        } else {
            sessionCounters.reportEvent(getRootId(parentId))
        }
    }

    private fun getStartTimestamp(event: SerializableTraceEvent): Long? = when (event) {
        is SpanEnterTrace -> event.timestampMs
        else -> null
    }

    private fun getEndTimestamp(event: SerializableTraceEvent): Long? = when (event) {
        is SpanExitTrace -> event.timestampMs
        else -> null
    }

    // TODO: move this logic to Python:
    private fun cleanStackTrace(stackTrace: List<EventStackFrame>): List<EventStackFrame> {
        // TODO: implement on the Python side
//        return stackTrace.filter { !it.filePath.contains(AiDebuggerPlugin.scriptsPath) }
        return stackTrace
    }
//
    private fun getLangGraphNodeType(event: SerializableTraceEvent): EventType? {
        val exitNode = event as? SpanExitTrace ?: return null
        val outputs = exitNode.getPayloadOr<Map<String, Any>>(PayloadKey.Outputs, emptyMap())
        val message = (getLastMessage(outputs) as? Map<*, *>) ?: return null
        val type = message["type"] as? String ?: return null

        return when(type.lowercase()) {
            "ai" -> EventType.LlmCall
            "tool" -> EventType.ToolCall
            else -> null
        }
    }
}