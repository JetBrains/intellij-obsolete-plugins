package com.intellij.aidebugger.common.models

import com.intellij.aidebugger.common.models.entities.EventType
import com.intellij.aidebugger.common.models.entities.Framework
import com.intellij.aidebugger.common.models.entities.TraceEvent
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.persistentSetOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TraceEventsStateTest {

    @Test
    fun `preOrder should return single event when no children exist`() {
        val event = TraceEvent(
            id = "event1",
            parentId = null,
            childIds = persistentSetOf(),
            name = "Root Event",
            type = EventType.General,
            framework = Framework.LangGraph,
            finished = true,
            timestampStartMs = 1000,
            timestampEndMs = 1100,
            payload = mapOf()
        )

        val state = TraceEventsState(
            events = persistentMapOf("event1" to event),
            roots = persistentSetOf("event1")
        )

        val result = state.preOrder("event1").toList()

        assertEquals(1, result.size)
        assertEquals("event1", result[0].id)
        assertEquals("Root Event", result[0].name)
    }

    @Test
    fun `preOrder should return empty sequence for non-existent event`() {
        val state = TraceEventsState()

        val result = state.preOrder("nonexistent").toList()

        assertEquals(0, result.size)
    }

    @Test
    fun `preOrder should traverse linear chain correctly`() {
        val event1 = TraceEvent(
            id = "event1",
            parentId = null,
            childIds = persistentSetOf("event2"),
            name = "Root",
            type = EventType.General,
            framework = Framework.LangGraph,
            timestampStartMs = 1000,
            timestampEndMs = 1400,
            finished = true,
            payload = mapOf()
        )

        val event2 = TraceEvent(
            id = "event2",
            parentId = "event1",
            childIds = persistentSetOf("event3"),
            name = "Child",
            type = EventType.General,
            framework = Framework.LangGraph,
            timestampStartMs = 1100,
            timestampEndMs = 1300,
            finished = true,
            payload = mapOf()
        )

        val event3 = TraceEvent(
            id = "event3",
            parentId = "event2",
            childIds = persistentSetOf(),
            name = "Leaf",
            type = EventType.General,
            framework = Framework.LangGraph,
            timestampStartMs = 1200,
            timestampEndMs = 1250,
            finished = true,
            payload = mapOf()
        )

        val state = TraceEventsState(
            events = persistentMapOf(
                "event1" to event1,
                "event2" to event2,
                "event3" to event3
            ),
            roots = persistentSetOf("event1")
        )

        val result = state.preOrder("event1").toList()

        assertEquals(3, result.size)
        assertEquals("event1", result[0].id)
        assertEquals("event2", result[1].id)
        assertEquals("event3", result[2].id)
    }

    @Test
    fun `preOrder should traverse tree with multiple children correctly`() {
        val root = TraceEvent(
            id = "root",
            parentId = null,
            childIds = persistentSetOf("child1", "child2"),
            name = "Root",
            type = EventType.General,
            framework = Framework.LangGraph,
            timestampStartMs = 1000,
            timestampEndMs = 2000,
            finished = true,
            payload = mapOf()
        )

        val child1 = TraceEvent(
            id = "child1",
            parentId = "root",
            childIds = persistentSetOf("grandchild1", "grandchild2"),
            name = "Child 1",
            type = EventType.General,
            framework = Framework.LangGraph,
            timestampStartMs = 1100,
            timestampEndMs = 1500,
            finished = true,
            payload = mapOf()
        )

        val child2 = TraceEvent(
            id = "child2",
            parentId = "root",
            childIds = persistentSetOf("grandchild3"),
            name = "Child 2",
            type = EventType.General,
            framework = Framework.LangGraph,
            timestampStartMs = 1600,
            timestampEndMs = 1900,
            finished = true,
            payload = mapOf()
        )

        val grandchild1 = TraceEvent(
            id = "grandchild1",
            parentId = "child1",
            childIds = persistentSetOf(),
            name = "Grandchild 1",
            type = EventType.General,
            framework = Framework.LangGraph,
            timestampStartMs = 1200,
            timestampEndMs = 1300,
            finished = true,
            payload = mapOf()
        )

        val grandchild2 = TraceEvent(
            id = "grandchild2",
            parentId = "child1",
            childIds = persistentSetOf(),
            name = "Grandchild 2",
            type = EventType.General,
            framework = Framework.LangGraph,
            timestampStartMs = 1350,
            timestampEndMs = 1450,
            finished = true,
            payload = mapOf()
        )

        val grandchild3 = TraceEvent(
            id = "grandchild3",
            parentId = "child2",
            childIds = persistentSetOf(),
            name = "Grandchild 3",
            type = EventType.General,
            framework = Framework.LangGraph,
            timestampStartMs = 1700,
            timestampEndMs = 1800,
            finished = true,
            payload = mapOf()
        )

        val state = TraceEventsState(
            events = persistentMapOf(
                "root" to root,
                "child1" to child1,
                "child2" to child2,
                "grandchild1" to grandchild1,
                "grandchild2" to grandchild2,
                "grandchild3" to grandchild3
            ),
            roots = persistentSetOf("root")
        )

        val result = state.preOrder("root").toList()

        assertEquals(6, result.size)
        assertEquals("root", result[0].id)

        // Pre-order traversal should visit children in the order they appear in childIds
        // Since childIds is a set, we need to verify the structure is correct
        assertEquals("child1", result[1].id)
        assertEquals("grandchild1", result[2].id)
        assertEquals("grandchild2", result[3].id)
        assertEquals("child2", result[4].id)
        assertEquals("grandchild3", result[5].id)
    }

    @Test
    fun `preOrder should start from any valid node in the tree`() {
        val root = TraceEvent(
            id = "root",
            parentId = null,
            childIds = persistentSetOf("child1", "child2"),
            name = "Root",
            type = EventType.General,
            framework = Framework.LangGraph,
            timestampStartMs = 1000,
            timestampEndMs = 2000,
            finished = true,
            payload = mapOf()
        )

        val child1 = TraceEvent(
            id = "child1",
            parentId = "root",
            childIds = persistentSetOf("grandchild1"),
            name = "Child 1",
            type = EventType.General,
            framework = Framework.LangGraph,
            timestampStartMs = 1100,
            timestampEndMs = 1500,
            finished = true,
            payload = mapOf()
        )

        val child2 = TraceEvent(
            id = "child2",
            parentId = "root",
            childIds = persistentSetOf(),
            name = "Child 2",
            type = EventType.General,
            framework = Framework.LangGraph,
            timestampStartMs = 1600,
            timestampEndMs = 1900,
            finished = true,
            payload = mapOf()
        )

        val grandchild1 = TraceEvent(
            id = "grandchild1",
            parentId = "child1",
            childIds = persistentSetOf(),
            name = "Grandchild 1",
            type = EventType.General,
            framework = Framework.LangGraph,
            timestampStartMs = 1200,
            timestampEndMs = 1300,
            finished = true,
            payload = mapOf()
        )

        val state = TraceEventsState(
            events = persistentMapOf(
                "root" to root,
                "child1" to child1,
                "child2" to child2,
                "grandchild1" to grandchild1
            ),
            roots = persistentSetOf("root")
        )

        // Start from child1
        val resultFromChild1 = state.preOrder("child1").toList()
        assertEquals(2, resultFromChild1.size)
        assertEquals("child1", resultFromChild1[0].id)
        assertEquals("grandchild1", resultFromChild1[1].id)

        // Start from child2 (leaf node)
        val resultFromChild2 = state.preOrder("child2").toList()
        assertEquals(1, resultFromChild2.size)
        assertEquals("child2", resultFromChild2[0].id)

        // Start from grandchild1 (leaf node)
        val resultFromGrandchild = state.preOrder("grandchild1").toList()
        assertEquals(1, resultFromGrandchild.size)
        assertEquals("grandchild1", resultFromGrandchild[0].id)
    }

    @Test
    fun `preOrder should handle orphaned child references gracefully`() {
        val parent = TraceEvent(
            id = "parent",
            parentId = null,
            childIds = persistentSetOf("child1", "nonexistent_child", "child2"),
            name = "Parent",
            type = EventType.General,
            framework = Framework.LangGraph,
            timestampStartMs = 1000,
            timestampEndMs = 2000,
            finished = true,
            payload = mapOf()
        )

        val child1 = TraceEvent(
            id = "child1",
            parentId = "parent",
            childIds = persistentSetOf(),
            name = "Child 1",
            type = EventType.General,
            framework = Framework.LangGraph,
            timestampStartMs = 1100,
            timestampEndMs = 1200,
            finished = true,
            payload = mapOf()
        )

        val child2 = TraceEvent(
            id = "child2",
            parentId = "parent",
            childIds = persistentSetOf(),
            name = "Child 2",
            type = EventType.General,
            framework = Framework.LangGraph,
            timestampStartMs = 1300,
            timestampEndMs = 1400,
            finished = true,
            payload = mapOf()
        )

        val state = TraceEventsState(
            events = persistentMapOf(
                "parent" to parent,
                "child1" to child1,
                "child2" to child2
                // Note: "nonexistent_child" is not in the events map
            ),
            roots = persistentSetOf("parent")
        )

        val result = state.preOrder("parent").toList()

        // Should only include existing events, skipping the nonexistent child
        assertEquals(3, result.size)
        assertEquals("parent", result[0].id)

        val childIds = result.drop(1).map { it.id }.toSet()
        assertTrue(childIds.contains("child1"))
        assertTrue(childIds.contains("child2"))
    }

    @Test
    fun `preOrder should handle empty childIds set`() {
        val event = TraceEvent(
            id = "event1",
            parentId = null,
            childIds = persistentSetOf(), // explicitly empty
            name = "Single Event",
            type = EventType.General,
            framework = Framework.LangGraph,
            timestampStartMs = 1000,
            timestampEndMs = 1100,
            finished = true,
            payload = mapOf()
        )

        val state = TraceEventsState(
            events = persistentMapOf("event1" to event),
            roots = persistentSetOf("event1")
        )

        val result = state.preOrder("event1").toList()

        assertEquals(1, result.size)
        assertEquals("event1", result[0].id)
    }

    @Test
    fun `preOrder should work with different event types`() {
        val root = TraceEvent(
            id = "root",
            parentId = null,
            childIds = persistentSetOf("llm_call", "tool_call"),
            name = "Root",
            type = EventType.Group,
            framework = Framework.LangGraph,
            timestampStartMs = 1000,
            timestampEndMs = 3000,
            finished = true,
            payload = mapOf()
        )

        val llmCall = TraceEvent(
            id = "llm_call",
            parentId = "root",
            childIds = persistentSetOf(),
            name = "LLM Call",
            type = EventType.LlmCall,
            framework = Framework.LangGraph,
            timestampStartMs = 1100,
            timestampEndMs = 2000,
            finished = true,
            payload = mapOf()
        )

        val toolCall = TraceEvent(
            id = "tool_call",
            parentId = "root",
            childIds = persistentSetOf(),
            name = "Tool Call",
            type = EventType.ToolCall,
            framework = Framework.LangGraph,
            timestampStartMs = 2100,
            timestampEndMs = 2900,
            finished = true,
            payload = mapOf()
        )

        val state = TraceEventsState(
            events = persistentMapOf(
                "root" to root,
                "llm_call" to llmCall,
                "tool_call" to toolCall
            ),
            roots = persistentSetOf("root")
        )

        val result = state.preOrder("root").toList()

        assertEquals(3, result.size)
        assertEquals("root", result[0].id)
        assertEquals(EventType.Group, result[0].type)

        // Verify the child events are included
        val childEvents = result.drop(1)
        val llmEvent = childEvents.find { it.id == "llm_call" }
        val toolEvent = childEvents.find { it.id == "tool_call" }

        assertNotNull(llmEvent)
        assertEquals(EventType.LlmCall, llmEvent?.type)

        assertNotNull(toolEvent)
        assertEquals(EventType.ToolCall, toolEvent?.type)
    }

    @Test
    fun `preOrder should preserve event properties during traversal`() {
        val parent = TraceEvent(
            id = "parent",
            parentId = null,
            childIds = persistentSetOf("child"),
            name = "Parent Event",
            type = EventType.General,
            framework = Framework.LangGraph,
            timestampStartMs = 1000,
            timestampEndMs = 2000,
            finished = true,
            payload = mapOf("key1" to "value1", "key2" to 42)
        )

        val child = TraceEvent(
            id = "child",
            parentId = "parent",
            childIds = persistentSetOf(),
            name = "Child Event",
            type = EventType.LlmCall,
            framework = Framework.LangGraph,
            timestampStartMs = 1100,
            timestampEndMs = 1900,
            finished = false,
            payload = mapOf("model" to "gpt-4", "temperature" to 0.7)
        )

        val state = TraceEventsState(
            events = persistentMapOf(
                "parent" to parent,
                "child" to child
            ),
            roots = persistentSetOf("parent")
        )

        val result = state.preOrder("parent").toList()

        assertEquals(2, result.size)

        // Verify parent event properties
        val parentResult = result[0]
        assertEquals("parent", parentResult.id)
        assertEquals("Parent Event", parentResult.name)
        assertEquals(EventType.General, parentResult.type)
        assertEquals(1000, parentResult.timestampStartMs)
        assertEquals(2000, parentResult.timestampEndMs)
        assertTrue(parentResult.finished)
        assertEquals("value1", parentResult.payload["key1"])
        assertEquals(42, parentResult.payload["key2"])

        // Verify child event properties
        val childResult = result[1]
        assertEquals("child", childResult.id)
        assertEquals("Child Event", childResult.name)
        assertEquals(EventType.LlmCall, childResult.type)
        assertEquals(1100, childResult.timestampStartMs)
        assertEquals(1900, childResult.timestampEndMs)
        assertFalse(childResult.finished)
        assertEquals("gpt-4", childResult.payload["model"])
        assertEquals(0.7, childResult.payload["temperature"])
    }
}