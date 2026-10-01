package com.intellij.aidebugger.python.models

import com.intellij.aidebugger.common.models.SessionCountersImpl
import com.intellij.aidebugger.common.models.TraceEventsState
import com.intellij.aidebugger.common.models.entities.EventStackFrame
import com.intellij.aidebugger.common.models.entities.EventType
import com.intellij.aidebugger.common.models.entities.Framework
import com.intellij.aidebugger.common.models.entities.InstantTrace
import com.intellij.aidebugger.common.models.entities.PayloadKey
import com.intellij.aidebugger.common.models.entities.SerializableTraceEvent
import com.intellij.aidebugger.common.models.entities.SimpleGraph
import com.intellij.aidebugger.common.models.entities.SimpleGraphEdge
import com.intellij.aidebugger.common.models.entities.SimpleGraphNode
import com.intellij.aidebugger.common.models.entities.SpanEnterTrace
import com.intellij.aidebugger.common.models.entities.SpanExitTrace
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Ignore
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)
class CommonTraceEventRepositoryTest {

    @Before
    fun setUp() {
    }

    @After
    fun tearDown() {
    }

    @Test
    fun `invoke adds as a root event in the state`() = runTest {
        val eventsFlow = flow<SerializableTraceEvent> {
            emit(
                InstantTrace(
                    id = "invoke-1",
                    parentId = null,
                    traceId = "test-trace-1",
                    name = "test_invoke",
                    eventType = EventType.Group,
                    framework = Framework.LangGraph,
                    timestampMs = 1000,
                    payload = mapOf(
                        PayloadKey.StackTrace to listOf<EventStackFrame>(),
                        PayloadKey.Graph to createSimpleTestGraph()
                    )
                )
            )
        }

        val repository = CommonTraceEventsRepository(
            coroutineScope = this,
            eventsFlow = eventsFlow,
            sessionCounters = SessionCountersImpl()
        )

        advanceUntilIdle()

        val state = repository.state.value

        assert(state.roots.size == 1)
    }

    @Test
    fun `one invoke and 2 events should create a tree`() = runTest {
        val eventsFlow = flow {
            emit(
                InstantTrace(
                    id = "invoke-1",
                    parentId = null,
                    traceId = "test-trace-1",
                    name = "invoke-1",
                    eventType = EventType.Group,
                    framework = Framework.LangGraph,
                    timestampMs = 1000,
                    payload = mapOf(
                        PayloadKey.StackTrace to listOf<EventStackFrame>(),
                        PayloadKey.Graph to createSimpleTestGraph()
                    )
                )
            )
            emit(
                SpanEnterTrace(
                    id = "101",
                    parentId = "invoke-1",
                    traceId = "test-trace-1",
                    name = "node1",
                    eventType = EventType.General,
                    framework = Framework.LangGraph,
                    timestampMs = 1100,
                    payload = mapOf(PayloadKey.Inputs to listOf(mapOf("input1" to "value1")))
                )
            )
            emit(
                SpanExitTrace(
                    id = "101",
                    parentId = "invoke-1",
                    traceId = "test-trace-1",
                    name = "node1",
                    eventType = EventType.General,
                    framework = Framework.LangGraph,
                    timestampMs = 1200,
                    payload = mapOf(PayloadKey.Outputs to listOf(mapOf("output1" to "result1")))
                )
            )
        }

        val repository = CommonTraceEventsRepository(
            coroutineScope = this,
            eventsFlow = eventsFlow,
            sessionCounters = SessionCountersImpl()
        )

        advanceUntilIdle()

        val state = repository.state.value

        assertEquals(1, state.roots.size)
        assertEquals(2, state.events.size)

        val firstRootId = state.roots.first()
        val rootEvent = state.child(firstRootId)

        assertEquals(1, rootEvent.childIds.size)

        val firstChildId = rootEvent.childIds.first()
        val childEvent = state.child(firstChildId)

        assertEquals(0, childEvent.childIds.size)
        assertEquals("node1", childEvent.name)
        assertEquals(1100, childEvent.timestampStartMs)
        assertEquals(1200, childEvent.timestampEndMs)
    }

    @Test
    fun `invoke finish event should be handled`() = runTest {
        val eventsFlow = flow<SerializableTraceEvent> {
            emit(
                InstantTrace(
                    id = "invoke-1",
                    parentId = null,
                    traceId = "test-trace-1",
                    name = "invoke-1",
                    eventType = EventType.Group,
                    framework = Framework.LangGraph,
                    timestampMs = 1000,
                    payload = mapOf(
                        PayloadKey.StackTrace to listOf<EventStackFrame>(),
                        PayloadKey.Graph to createSimpleTestGraph()
                    )
                )
            )
            emit(
                InstantTrace(
                    id = "invoke-1",
                    parentId = null,
                    traceId = "test-trace-1",
                    name = "invoke-1",
                    eventType = EventType.Group,
                    framework = Framework.LangGraph,
                    timestampMs = 2000,
                    payload = mapOf()
                )
            )
        }

        val repository = CommonTraceEventsRepository(
            coroutineScope = this,
            eventsFlow = eventsFlow,
            sessionCounters = SessionCountersImpl()
        )

        advanceUntilIdle()

        val state = repository.state.value
        assertEquals(1, state.roots.size)
        assertEquals(1, state.events.size)
    }

    @Test
    fun `exception event should be added as child`() = runTest {
        val eventsFlow = flow {
            emit(
                InstantTrace(
                    id = "invoke-1",
                    parentId = null,
                    traceId = "test-trace-1",
                    name = "invoke-1",
                    eventType = EventType.Group,
                    framework = Framework.LangGraph,
                    timestampMs = 1000,
                    payload = mapOf(
                        PayloadKey.StackTrace to listOf<EventStackFrame>(),
                        PayloadKey.Graph to createSimpleTestGraph()
                    )
                )
            )
            emit(
                InstantTrace(
                    id = "exception-1",
                    parentId = "invoke-1",
                    traceId = "test-trace-1",
                    name = "ValueError",
                    eventType = EventType.Exception,
                    framework = Framework.LangGraph,
                    timestampMs = 1500,
                    payload = mapOf(PayloadKey.StackTrace to listOf<EventStackFrame>())
                )
            )
        }

        val repository = CommonTraceEventsRepository(
            coroutineScope = this,
            eventsFlow = eventsFlow,
            sessionCounters = SessionCountersImpl()
        )

        advanceUntilIdle()

        val state = repository.state.value

        assertEquals(1, state.roots.size)
        assertEquals(2, state.events.size)

        val firstRootId = state.roots.first()
        val rootEvent = state.child(firstRootId)

        assertEquals(1, rootEvent.childIds.size)

        val firstChildId = rootEvent.childIds.first()
        val childEvent = state.child(firstChildId)

        assertEquals("ValueError", childEvent.name)
        assertEquals(EventType.Exception, childEvent.type)
        assertEquals(1500, childEvent.timestampStartMs)
        assertEquals(true, childEvent.finished)
    }

    @Test
    fun `multiple invokes should create separate root events`() = runTest {
        val eventsFlow = flow<SerializableTraceEvent> {
            emit(
                InstantTrace(
                    id = "invoke-1",
                    parentId = null,
                    traceId = "test-trace-1",
                    name = "invoke-1",
                    eventType = EventType.Group,
                    framework = Framework.LangGraph,
                    timestampMs = 1000,
                    payload = mapOf(
                        PayloadKey.StackTrace to listOf<EventStackFrame>(),
                        PayloadKey.Graph to createSimpleTestGraph()
                    )
                )
            )
            emit(
                InstantTrace(
                    id = "invoke-2",
                    parentId = null,
                    traceId = "test-trace-1",
                    name = "invoke-2",
                    eventType = EventType.Group,
                    framework = Framework.LangGraph,
                    timestampMs = 2000,
                    payload = mapOf(
                        PayloadKey.StackTrace to listOf<EventStackFrame>(),
                        PayloadKey.Graph to createSimpleTestGraph()
                    )
                )
            )
        }

        val repository = CommonTraceEventsRepository(
            coroutineScope = this,
            eventsFlow = eventsFlow,
            sessionCounters = SessionCountersImpl()
        )

        advanceUntilIdle()

        val state = repository.state.value

        assertEquals(2, state.roots.size)
        assertEquals(2, state.events.size)
    }

    @Test
    fun `events with same id should be merged`() = runTest {
        val eventsFlow = flow {
            emit(
                InstantTrace(
                    id = "invoke-1",
                    parentId = null,
                    traceId = "test-trace-1",
                    name = "invoke-1",
                    eventType = EventType.Group,
                    framework = Framework.LangGraph,
                    timestampMs = 1000,
                    payload = mapOf(
                        PayloadKey.StackTrace to listOf<EventStackFrame>(),
                        PayloadKey.Graph to createSimpleTestGraph()
                    )
                )
            )
            emit(
                SpanEnterTrace(
                    id = "101",
                    parentId = "invoke-1",
                    traceId = "test-trace-1",
                    name = "node1",
                    eventType = EventType.General,
                    framework = Framework.LangGraph,
                    timestampMs = 1100,
                    payload = mapOf(PayloadKey.Inputs to listOf(mapOf("input1" to "value1")))
                )
            )
            emit(
                SpanExitTrace(
                    id = "101",
                    parentId = "invoke-1",
                    traceId = "test-trace-1",
                    name = "node1",
                    eventType = EventType.General,
                    framework = Framework.LangGraph,
                    timestampMs = 1200,
                    payload = mapOf(PayloadKey.Outputs to listOf(mapOf("output1" to "result1")))
                )
            )
        }

        val repository = CommonTraceEventsRepository(
            coroutineScope = this,
            eventsFlow = eventsFlow,
            sessionCounters = SessionCountersImpl()
        )

        advanceUntilIdle()

        val state = repository.state.value

        assertEquals(1, state.roots.size)
        assertEquals(2, state.events.size)

        val firstRootId = state.roots.first()
        val rootEvent = state.child(firstRootId)
        val childId = rootEvent.childIds.first()
        val mergedEvent = state.child(childId)

        assertEquals(1100, mergedEvent.timestampStartMs)
        assertEquals(1200, mergedEvent.timestampEndMs)
        assertEquals("node1", mergedEvent.name)
    }

    @Test
    @Ignore("Need to rethink this logic")
    fun `AI node type should be detected from outputs`() = runTest {
        val aiOutputs = mapOf(
            "messages" to listOf(
                mapOf(
                    "type" to "ai",
                    "content" to "AI response"
                )
            )
        )

        val eventsFlow = flow {
            emit(
                InstantTrace(
                    id = "invoke-1",
                    parentId = null,
                    traceId = "test-trace-1",
                    name = "invoke-1",
                    eventType = EventType.Group,
                    framework = Framework.LangGraph,
                    timestampMs = 1000,
                    payload = mapOf(
                        PayloadKey.StackTrace to listOf<EventStackFrame>(),
                        PayloadKey.Graph to createSimpleTestGraph()
                    )
                )
            )
            emit(
                SpanExitTrace(
                    id = "101",
                    parentId = "invoke-1",
                    traceId = "test-trace-1",
                    name = "ai_node",
                    eventType = EventType.LlmCall,
                    framework = Framework.LangGraph,
                    timestampMs = 1200,
                    payload = mapOf(PayloadKey.Outputs to aiOutputs)
                )
            )
        }

        val repository = CommonTraceEventsRepository(
            coroutineScope = this,
            eventsFlow = eventsFlow,
            sessionCounters = SessionCountersImpl()
        )

        advanceUntilIdle()

        val state = repository.state.value
        val rootEvent = state.child(state.roots.first())
        val aiEvent = state.child(rootEvent.childIds.first())

        assertEquals(EventType.LlmCall, aiEvent.type)
    }

    @Test
    @Ignore("Need to rethink this logic")
    fun `Tool node type should be detected from outputs`() = runTest {
        val toolOutputs = mapOf(
            "messages" to listOf(
                mapOf(
                    "type" to "tool",
                    "content" to "Tool result"
                )
            )
        )

        val eventsFlow = flow {
            emit(
                InstantTrace(
                    id = "invoke-1",
                    parentId = null,
                    traceId = "test-trace-1",
                    name = "invoke-1",
                    eventType = EventType.Group,
                    framework = Framework.LangGraph,
                    timestampMs = 1000,
                    payload = mapOf(
                        PayloadKey.StackTrace to listOf<EventStackFrame>(),
                        PayloadKey.Graph to createSimpleTestGraph()
                    )
                )
            )
            emit(
                SpanExitTrace(
                    id = "101",
                    parentId = "invoke-1",
                    traceId = "test-trace-1",
                    name = "tool_node",
                    eventType = EventType.ToolCall,
                    framework = Framework.LangGraph,
                    timestampMs = 1200,
                    payload = mapOf(PayloadKey.Outputs to toolOutputs)
                )
            )
        }

        val repository = CommonTraceEventsRepository(
            coroutineScope = this,
            eventsFlow = eventsFlow,
            sessionCounters = SessionCountersImpl()
        )

        advanceUntilIdle()

        val state = repository.state.value
        val rootEvent = state.child(state.roots.first())
        val toolEvent = state.child(rootEvent.childIds.first())

        assertEquals(EventType.ToolCall, toolEvent.type)
    }

    @Test
    fun `nested events should create proper hierarchy`() = runTest {
        val eventsFlow = flow {
            emit(
                InstantTrace(
                    id = "invoke-1",
                    parentId = null,
                    traceId = "test-trace-1",
                    name = "invoke-1",
                    eventType = EventType.Group,
                    framework = Framework.LangGraph,
                    timestampMs = 1000,
                    payload = mapOf(
                        PayloadKey.StackTrace to listOf<EventStackFrame>(),
                        PayloadKey.Graph to createSimpleTestGraph()
                    )
                )
            )
            emit(
                SpanEnterTrace(
                    id = "101",
                    parentId = "invoke-1",
                    traceId = "test-trace-1",
                    name = "parent_node",
                    eventType = EventType.General,
                    framework = Framework.LangGraph,
                    timestampMs = 1100,
                    payload = mapOf()
                )
            )
            emit(
                SpanEnterTrace(
                    id = "102",
                    parentId = "101",
                    traceId = "test-trace-1",
                    name = "child_node",
                    eventType = EventType.General,
                    framework = Framework.LangGraph,
                    timestampMs = 1150,
                    payload = mapOf()
                )
            )
            emit(
                SpanExitTrace(
                    id = "102",
                    parentId = "101",
                    traceId = "test-trace-1",
                    name = "child_node",
                    eventType = EventType.General,
                    framework = Framework.LangGraph,
                    timestampMs = 1180,
                    payload = mapOf()
                )
            )
            emit(
                SpanExitTrace(
                    id = "101",
                    parentId = "invoke-1",
                    traceId = "test-trace-1",
                    name = "parent_node",
                    eventType = EventType.General,
                    framework = Framework.LangGraph,
                    timestampMs = 1200,
                    payload = mapOf()
                )
            )
        }

        val repository = CommonTraceEventsRepository(
            coroutineScope = this,
            eventsFlow = eventsFlow,
            sessionCounters = SessionCountersImpl()
        )

        advanceUntilIdle()

        val state = repository.state.value

        assertEquals(1, state.roots.size)
        assertEquals(3, state.events.size)

        val rootEvent = state.child(state.roots.first())
        assertEquals(1, rootEvent.childIds.size)
    }

    @Test
    fun `events with unknown invoke id should be ignored`() = runTest {
        val eventsFlow = flow<SerializableTraceEvent> {
            emit(
                SpanEnterTrace(
                    id = "101",
                    parentId = "unknown-invoke",
                    traceId = "test-trace-1",
                    name = "orphan_node",
                    eventType = EventType.General,
                    framework = Framework.LangGraph,
                    timestampMs = 1100,
                    payload = mapOf()
                )
            )
        }

        val repository = CommonTraceEventsRepository(
            coroutineScope = this,
            eventsFlow = eventsFlow,
            sessionCounters = SessionCountersImpl()
        )

        advanceUntilIdle()

        val state = repository.state.value

        assertEquals(0, state.roots.size)
        assertEquals(0, state.events.size)
    }

    @Test
    fun `payload should be properly set for enter and exit events`() = runTest {
        val inputs = listOf(mapOf("param1" to "value1"))
        val outputs = listOf(mapOf("result1" to "output1"))

        val eventsFlow = flow {
            emit(
                InstantTrace(
                    id = "invoke-1",
                    parentId = null,
                    traceId = "test-trace-1",
                    name = "invoke-1",
                    eventType = EventType.Group,
                    framework = Framework.LangGraph,
                    timestampMs = 1000,
                    payload = mapOf(
                        PayloadKey.StackTrace to listOf<EventStackFrame>(),
                        PayloadKey.Graph to createSimpleTestGraph()
                    )
                )
            )
            emit(
                SpanEnterTrace(
                    id = "101",
                    parentId = "invoke-1",
                    traceId = "test-trace-1",
                    name = "test_node",
                    eventType = EventType.General,
                    framework = Framework.LangGraph,
                    timestampMs = 1100,
                    payload = mapOf(PayloadKey.Inputs to inputs)
                )
            )
            emit(
                SpanExitTrace(
                    id = "101",
                    parentId = "invoke-1",
                    traceId = "test-trace-1",
                    name = "test_node",
                    eventType = EventType.General,
                    framework = Framework.LangGraph,
                    timestampMs = 1200,
                    payload = mapOf(PayloadKey.Outputs to outputs)
                )
            )
        }

        val repository = CommonTraceEventsRepository(
            coroutineScope = this,
            eventsFlow = eventsFlow,
            sessionCounters = SessionCountersImpl()
        )

        advanceUntilIdle()

        val state = repository.state.value
        val rootEvent = state.child(state.roots.first())
        val testEvent = state.child(rootEvent.childIds.first())

        assertEquals(inputs, testEvent.payload[PayloadKey.Inputs])
        assertEquals(outputs, testEvent.payload[PayloadKey.Outputs])
    }

    private fun createSimpleTestGraph(): SimpleGraph {
        val nodes = listOf(
            SimpleGraphNode("node1", "test", null),
            SimpleGraphNode("node2", "test", null)
        )
        val edges = listOf(
            SimpleGraphEdge("node1", "node2")
        )
        return SimpleGraph(nodes, edges)
    }

    companion object {
        private fun TraceEventsState.child(id: String) = this.events[id] ?: error("Event with id $id not found")
    }
}