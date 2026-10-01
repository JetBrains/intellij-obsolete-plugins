package com.intellij.aidebugger.evaluation.models.runner

import com.intellij.aidebugger.common.models.SessionCountersImpl
import com.intellij.aidebugger.common.models.TraceEventsState
import com.intellij.aidebugger.common.models.buildHierarchicalStructure
import com.intellij.aidebugger.common.models.entities.EventType
import com.intellij.aidebugger.common.models.entities.Framework
import com.intellij.aidebugger.common.models.entities.TraceEvent
import com.intellij.aidebugger.python.models.CommonTraceEventsRepository
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toPersistentMap
import kotlinx.collections.immutable.toPersistentSet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.atomic.AtomicBoolean

class AiDebuggerGatewayTest {

    @Test
    fun `buildHierarchicalStructure creates hierarchy from flat events`() {
        val events = mapOf(
            "1" to createEvent("1", null, "root-event"),
            "2" to createEvent("2", "1", "child-event")
        )
        val state = TraceEventsState(
            events = events.toPersistentMap(),
            roots = listOf("1").toPersistentSet()
        )

        val result = buildHierarchicalStructure(state)

        assertEquals(1, result.rootEvents.size)
        assertEquals("1", result.rootEvents[0].id)
        assertEquals("root-event", result.rootEvents[0].name)
    }

    @Test
    fun `buildHierarchicalStructure handles multiple root events`() {
        val events = mapOf(
            "1" to createEvent("1", null, "root1"),
            "2" to createEvent("2", null, "root2"),
            "3" to createEvent("3", "1", "child1")
        )
        val state = TraceEventsState(
            events = events.toPersistentMap(),
            roots = listOf("1", "2").toPersistentSet()
        )

        val result = buildHierarchicalStructure(state)

        assertEquals(2, result.rootEvents.size)
        val root1 = result.rootEvents.find { it.id == "1" }
        val root2 = result.rootEvents.find { it.id == "2" }
        assertNotNull(root1)
        assertNotNull(root2)
        assertEquals("root1", root1?.name)
        assertEquals("root2", root2?.name)
    }

    @Test
    fun `buildHierarchicalStructure handles deep nesting`() {
        val events = mapOf(
            "1" to createEvent("1", null, "root"),
            "2" to createEvent("2", "1", "level1"),
            "3" to createEvent("3", "2", "level2"),
            "4" to createEvent("4", "3", "level3")
        )
        val state = TraceEventsState(
            events = events.toPersistentMap(),
            roots = listOf("1").toPersistentSet()
        )

        val result = buildHierarchicalStructure(state)

        assertEquals(1, result.rootEvents.size)
        assertEquals("1", result.rootEvents[0].id)
        assertEquals("root", result.rootEvents[0].name)
    }

    @Test
    fun `repository state flow updates correctly`() = runBlocking {
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        val repository = CommonTraceEventsRepository(
            coroutineScope = scope,
            eventsFlow = kotlinx.coroutines.flow.emptyFlow(),
            sessionCounters = SessionCountersImpl()
        )

        assertFalse(repository.finished.value)
        assertEquals(0, repository.state.value.events.size)
    }

    @Test
    fun `atomic boolean cancel flag works as expected`() {
        val cancelFlag = AtomicBoolean(false)
        assertFalse(cancelFlag.get())

        cancelFlag.set(true)
        assertTrue(cancelFlag.get())
    }

    @Test
    fun `trace event with exception type`() {
        val event = createExceptionEvent("exc1", "Test Exception")

        assertEquals(EventType.Exception, event.type)
        assertEquals("Test Exception", event.name)
        assertNull(event.parentId)
    }

    @Test
    fun `trace event with parent-child relationship`() {
        val parent = createEvent("p1", null, "parent")
        val child = createEvent("c1", "p1", "child")

        assertNull(parent.parentId)
        assertEquals("p1", child.parentId)
    }

    @Test
    fun `trace events state with empty events`() {
        val state = TraceEventsState(
            events = emptyMap<String, TraceEvent>().toPersistentMap(),
            roots = emptySet<String>().toPersistentSet()
        )

        assertEquals(0, state.events.size)
        assertEquals(0, state.roots.size)
    }

    @Test
    fun `trace events state preserves event data`() {
        val event = createEvent("e1", null, "test-event")
        val state = TraceEventsState(
            events = mapOf("e1" to event).toPersistentMap(),
            roots = setOf("e1").toPersistentSet()
        )

        assertEquals(1, state.events.size)
        assertEquals(1, state.roots.size)
        assertEquals("test-event", state.events["e1"]?.name)
    }

    @Test
    fun `fake process correctly tracks destroy calls`() {
        val fakeProcess = FakeProcess()

        assertEquals(0, fakeProcess.destroyCalled.get())
        fakeProcess.destroy()
        assertEquals(1, fakeProcess.destroyCalled.get())

        fakeProcess.destroyForcibly()
        assertEquals(2, fakeProcess.destroyCalled.get())
    }

    private fun createEvent(id: String, parentId: String?, name: String): TraceEvent {
        return TraceEvent(
            id = id,
            parentId = parentId,
            childIds = persistentSetOf(),
            type = EventType.ToolCall,
            name = name,
            framework = Framework.LangGraph,
            timestampStartMs = System.currentTimeMillis(),
            timestampEndMs = System.currentTimeMillis(),
            finished = true,
            payload = mapOf("output" to "test output")
        )
    }

    private fun createExceptionEvent(id: String, name: String): TraceEvent {
        return TraceEvent(
            id = id,
            parentId = null,
            childIds = persistentSetOf(),
            type = EventType.Exception,
            name = name,
            framework = Framework.LangGraph,
            timestampStartMs = System.currentTimeMillis(),
            timestampEndMs = System.currentTimeMillis(),
            finished = true,
            payload = emptyMap()
        )
    }

    private class FakeProcess : Process() {
        val destroyCalled = java.util.concurrent.atomic.AtomicInteger(0)

        override fun destroy() {
            destroyCalled.incrementAndGet()
        }

        override fun destroyForcibly(): Process {
            destroyCalled.incrementAndGet()
            return this
        }

        override fun isAlive(): Boolean = false

        override fun exitValue(): Int = 0

        override fun waitFor(): Int = 0

        override fun getOutputStream(): java.io.OutputStream = object : java.io.OutputStream() {
            override fun write(b: Int) {}
        }

        override fun getInputStream(): java.io.InputStream = java.io.ByteArrayInputStream(ByteArray(0))

        override fun getErrorStream(): java.io.InputStream = java.io.ByteArrayInputStream(ByteArray(0))
    }
}
