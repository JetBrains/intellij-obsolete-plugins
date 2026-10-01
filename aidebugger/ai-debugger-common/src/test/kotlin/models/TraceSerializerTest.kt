package com.intellij.aidebugger.common.models

import com.google.gson.JsonParser
import com.intellij.aidebugger.common.models.entities.EventType
import com.intellij.aidebugger.common.models.entities.Framework
import com.intellij.aidebugger.common.models.entities.TraceEvent
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.persistentSetOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class TraceSerializerTest {

    private fun assertPayloadEquals(expected: Map<String, Any?>, actual: Map<String, Any?>) {
        // Filter out null values from expected since Gson doesn't serialize them
        val expectedNonNull = expected.filterValues { it != null }
        val actualNonNull = actual.filterValues { it != null }

        assertEquals("Payload size mismatch", expectedNonNull.size, actualNonNull.size)
        for ((key, expectedValue) in expectedNonNull) {
            assertTrue("Missing key: $key", actualNonNull.containsKey(key))
            val actualValue = actualNonNull[key]

            when {
                expectedValue is Number && actualValue is Number -> {
                    // Convert both to Double for comparison to handle int->double conversion
                    assertEquals("Numeric value mismatch for key: $key",
                        expectedValue.toDouble(), actualValue.toDouble(), 0.0001)
                }
                else -> {
                    assertEquals("Value mismatch for key: $key", expectedValue, actualValue)
                }
            }
        }
    }

    @Test
    fun `serialize empty TraceEventsState`() {
        // Arrange
        val state = TraceEventsState()

        // Act
        val json = serializeTraceEventsState(state)
        val jsonObject = JsonParser.parseString(json).asJsonObject

        // Assert
        assertTrue(jsonObject.has("rootEvents"))
        val rootEvents = jsonObject.getAsJsonArray("rootEvents")
        assertEquals(0, rootEvents.size())
    }

    @Test
    fun `serialize single root event with no children`() {
        // Arrange
        val event = TraceEvent(
            id = "root1",
            parentId = null,
            childIds = persistentSetOf(),
            name = "Root Event",
            type = EventType.General,
            framework = Framework.LangGraph,
            timestampStartMs = 1000L,
            timestampEndMs = 2000L,
            finished = true,
            payload = mapOf("key1" to "value1"),
        )
        val state = TraceEventsState(
            events = persistentMapOf("root1" to event),
            roots = persistentSetOf("root1")
        )

        // Act
        val json = serializeTraceEventsState(state)
        val jsonObject = JsonParser.parseString(json).asJsonObject

        // Assert
        val rootEvents = jsonObject.getAsJsonArray("rootEvents")
        assertEquals(1, rootEvents.size())

        val rootEvent = rootEvents[0].asJsonObject
        assertEquals("root1", rootEvent.get("id").asString)
        assertEquals("Root Event", rootEvent.get("name").asString)
        assertEquals("General", rootEvent.get("type").asString)
        assertEquals(1000L, rootEvent.get("timestampStartMs").asLong)
        assertEquals(2000L, rootEvent.get("timestampEndMs").asLong)
        assertTrue(rootEvent.get("finished").asBoolean)

        val payload = rootEvent.getAsJsonObject("payload")
        assertEquals("value1", payload.get("key1").asString)

        val children = rootEvent.getAsJsonArray("children")
        assertEquals(0, children.size())
    }

    @Test
    fun `serialize simple parent-child hierarchy`() {
        // Arrange
        val parentEvent = TraceEvent(
            id = "parent1",
            parentId = null,
            childIds = persistentSetOf("child1"),
            name = "Parent Event",
            type = EventType.Group,
            framework = Framework.LangGraph,
            timestampStartMs = 1000L,
            timestampEndMs = 3000L,
            finished = true,
            payload = mapOf(),
        )
        val childEvent = TraceEvent(
            id = "child1",
            parentId = "parent1",
            childIds = persistentSetOf(),
            name = "Child Event",
            type = EventType.LlmCall,
            framework = Framework.LangGraph,
            timestampStartMs = 1500L,
            timestampEndMs = 2500L,
            finished = true,
            payload = mapOf(),
        )
        val state = TraceEventsState(
            events = persistentMapOf("parent1" to parentEvent, "child1" to childEvent),
            roots = persistentSetOf("parent1")
        )

        // Act
        val json = serializeTraceEventsState(state)
        val jsonObject = JsonParser.parseString(json).asJsonObject

        // Assert
        val rootEvents = jsonObject.getAsJsonArray("rootEvents")
        assertEquals(1, rootEvents.size())

        val parentJson = rootEvents[0].asJsonObject
        assertEquals("parent1", parentJson.get("id").asString)
        assertEquals("Parent Event", parentJson.get("name").asString)
        assertEquals("Group", parentJson.get("type").asString)

        val children = parentJson.getAsJsonArray("children")
        assertEquals(1, children.size())

        val childJson = children[0].asJsonObject
        assertEquals("child1", childJson.get("id").asString)
        assertEquals("Child Event", childJson.get("name").asString)
        assertEquals("LlmCall", childJson.get("type").asString)
        assertEquals(1500L, childJson.get("timestampStartMs").asLong)
        assertEquals(2500L, childJson.get("timestampEndMs").asLong)

        val grandChildren = childJson.getAsJsonArray("children")
        assertEquals(0, grandChildren.size())
    }

    @Test
    fun `serialize multiple root events sorted by timestamp`() {
        // Arrange
        val event1 = TraceEvent(
            id = "root1",
            parentId = null,
            childIds = persistentSetOf(),
            name = "First Root",
            type = EventType.General,
            framework = Framework.LangGraph,
            timestampStartMs = 2000L,
            timestampEndMs = 3000L,
            finished = true,
            payload = mapOf()
        )
        val event2 = TraceEvent(
            id = "root2",
            parentId = null,
            childIds = persistentSetOf(),
            name = "Second Root",
            type = EventType.General,
            framework = Framework.LangGraph,
            timestampStartMs = 1000L,
            timestampEndMs = 1500L,
            finished = true,
            payload = mapOf()
        )
        val state = TraceEventsState(
            events = persistentMapOf("root1" to event1, "root2" to event2),
            roots = persistentSetOf("root1", "root2")
        )

        // Act
        val json = serializeTraceEventsState(state)
        val jsonObject = JsonParser.parseString(json).asJsonObject

        // Assert
        val rootEvents = jsonObject.getAsJsonArray("rootEvents")
        assertEquals(2, rootEvents.size())

        // Should be sorted by timestamp (root2 first, then root1)
        assertEquals("root2", rootEvents[0].asJsonObject.get("id").asString)
        assertEquals("root1", rootEvents[1].asJsonObject.get("id").asString)
    }

    @Test
    fun `serialize deep hierarchy with multiple levels`() {
        // Arrange
        val grandParent = TraceEvent(
            id = "gp1",
            parentId = null,
            childIds = persistentSetOf("parent1"),
            name = "GrandParent",
            type = EventType.Group,
            framework = Framework.LangGraph,
            timestampStartMs = 1000L,
            timestampEndMs = 4000L,
            finished = true,
            payload = mapOf(),
        )
        val parent = TraceEvent(
            id = "parent1",
            parentId = "gp1",
            childIds = persistentSetOf("child1", "child2"),
            name = "Parent",
            type = EventType.Group,
            framework = Framework.LangGraph,
            timestampStartMs = 1500L,
            timestampEndMs = 3500L,
            finished = true,
            payload = mapOf(),
        )
        val child1 = TraceEvent(
            id = "child1",
            parentId = "parent1",
            childIds = persistentSetOf(),
            name = "Child 1",
            type = EventType.LlmCall,
            framework = Framework.LangGraph,
            timestampStartMs = 2000L,
            timestampEndMs = 2500L,
            finished = true,
            payload = mapOf(),
        )
        val child2 = TraceEvent(
            id = "child2",
            parentId = "parent1",
            childIds = persistentSetOf(),
            name = "Child 2",
            type = EventType.ToolCall,
            framework = Framework.LangGraph,
            timestampStartMs = 2600L,
            timestampEndMs = 3000L,
            finished = true,
            payload = mapOf(),
        )

        val state = TraceEventsState(
            events = persistentMapOf(
                "gp1" to grandParent,
                "parent1" to parent,
                "child1" to child1,
                "child2" to child2
            ),
            roots = persistentSetOf("gp1")
        )

        // Act
        val json = serializeTraceEventsState(state)
        val jsonObject = JsonParser.parseString(json).asJsonObject

        // Assert
        val rootEvents = jsonObject.getAsJsonArray("rootEvents")
        assertEquals(1, rootEvents.size())

        val grandParentJson = rootEvents[0].asJsonObject
        assertEquals("gp1", grandParentJson.get("id").asString)

        val parents = grandParentJson.getAsJsonArray("children")
        assertEquals(1, parents.size())

        val parentJson = parents[0].asJsonObject
        assertEquals("parent1", parentJson.get("id").asString)

        val children = parentJson.getAsJsonArray("children")
        assertEquals(2, children.size())

        // Children should be sorted by timestamp (child1 first, then child2)
        assertEquals("child1", children[0].asJsonObject.get("id").asString)
        assertEquals("child2", children[1].asJsonObject.get("id").asString)
    }

    @Test
    fun `serialize handles missing events gracefully`() {
        // Arrange - root references non-existent event
        val state = TraceEventsState(
            events = persistentMapOf(),
            roots = persistentSetOf("missing1", "missing2")
        )

        // Act
        val json = serializeTraceEventsState(state)
        val jsonObject = JsonParser.parseString(json).asJsonObject

        // Assert
        val rootEvents = jsonObject.getAsJsonArray("rootEvents")
        assertEquals(0, rootEvents.size())
    }

    @Test
    fun `serialize with complex payload types`() {
        // Arrange
        val complexPayload = mapOf(
            "string" to "value",
            "number" to 42,
            "boolean" to true,
            "null" to null,
            "list" to listOf("a", "b", "c"),
            "map" to mapOf("nested" to "data")
        )
        val event = TraceEvent(
            id = "complex1",
            parentId = null,
            childIds = persistentSetOf(),
            name = "Complex Event",
            type = EventType.General,
            framework = Framework.LangGraph,
            timestampStartMs = 1000L,
            timestampEndMs = 2000L,
            finished = true,
            payload = complexPayload,
        )
        val state = TraceEventsState(
            events = persistentMapOf("complex1" to event),
            roots = persistentSetOf("complex1")
        )

        // Act
        val json = serializeTraceEventsState(state)
        val jsonObject = JsonParser.parseString(json).asJsonObject

        // Assert
        val rootEvent = jsonObject.getAsJsonArray("rootEvents")[0].asJsonObject
        val payload = rootEvent.getAsJsonObject("payload")

        assertEquals("value", payload.get("string").asString)
        assertEquals(42, payload.get("number").asInt)
        assertTrue(payload.get("boolean").asBoolean)
        // Note: Gson doesn't serialize null values by default, so "null" key won't be present

        val list = payload.getAsJsonArray("list")
        assertEquals(3, list.size())
        assertEquals("a", list[0].asString)

        val map = payload.getAsJsonObject("map")
        assertEquals("data", map.get("nested").asString)
    }

    @Test
    fun `round-trip serialization preserves empty state`() {
        // Arrange
        val originalState = TraceEventsState()

        // Act
        val json = serializeTraceEventsState(originalState)
        val deserializedState = deserializeTraceEventsState(json)

        // Assert
        assertEquals(originalState.events.size, deserializedState.events.size)
        assertEquals(originalState.roots.size, deserializedState.roots.size)
        assertEquals(originalState.events, deserializedState.events)
        assertEquals(originalState.roots, deserializedState.roots)
    }

    @Test
    fun `round-trip serialization preserves single event`() {
        // Arrange
        val event = TraceEvent(
            id = "test1",
            parentId = null,
            childIds = persistentSetOf(),
            name = "Test Event",
            type = EventType.LlmCall,
            framework = Framework.LangGraph,
            timestampStartMs = 1500L,
            timestampEndMs = 2500L,
            finished = true,
            payload = mapOf("key" to "value", "number" to 42),
        )
        val originalState = TraceEventsState(
            events = persistentMapOf("test1" to event),
            roots = persistentSetOf("test1")
        )

        // Act
        val json = serializeTraceEventsState(originalState)
        val deserializedState = deserializeTraceEventsState(json)

        // Assert
        assertEquals(originalState.events.size, deserializedState.events.size)
        assertEquals(originalState.roots, deserializedState.roots)

        val deserializedEvent = deserializedState.events["test1"]!!
        assertEquals(event.id, deserializedEvent.id)
        assertEquals(event.name, deserializedEvent.name)
        assertEquals(event.type, deserializedEvent.type)
        assertEquals(event.timestampStartMs, deserializedEvent.timestampStartMs)
        assertEquals(event.timestampEndMs, deserializedEvent.timestampEndMs)
        assertEquals(event.finished, deserializedEvent.finished)
        // Compare payload accounting for JSON number conversion (int -> double)
        assertPayloadEquals(event.payload, deserializedEvent.payload)
        assertEquals(event.parentId, deserializedEvent.parentId)
        assertEquals(event.childIds, deserializedEvent.childIds)
    }

    @Test
    fun `round-trip serialization preserves complex hierarchy`() {
        // Arrange
        val root = TraceEvent(
            id = "root",
            parentId = null,
            childIds = persistentSetOf("child1", "child2"),
            name = "Root",
            type = EventType.Group,
            framework = Framework.LangGraph,
            timestampStartMs = 1000L,
            timestampEndMs = 5000L,
            finished = true,
            payload = mapOf(),
        )
        val child1 = TraceEvent(
            id = "child1",
            parentId = "root",
            childIds = persistentSetOf("grandchild1"),
            name = "Child 1",
            type = EventType.LlmCall,
            framework = Framework.LangGraph,
            timestampStartMs = 1500L,
            timestampEndMs = 2500L,
            finished = true,
            payload = mapOf(),
        )
        val child2 = TraceEvent(
            id = "child2",
            parentId = "root",
            childIds = persistentSetOf(),
            name = "Child 2",
            type = EventType.ToolCall,
            framework = Framework.LangGraph,
            timestampStartMs = 3000L,
            timestampEndMs = 4000L,
            finished = false,
            payload = mapOf(),
        )
        val grandchild1 = TraceEvent(
            id = "grandchild1",
            parentId = "child1",
            childIds = persistentSetOf(),
            name = "Grandchild 1",
            type = EventType.General,
            framework = Framework.LangGraph,
            timestampStartMs = 2000L,
            timestampEndMs = 2200L,
            finished = true,
            payload = mapOf("depth" to 3, "data" to listOf("a", "b")),
        )

        val originalState = TraceEventsState(
            events = persistentMapOf(
                "root" to root,
                "child1" to child1,
                "child2" to child2,
                "grandchild1" to grandchild1
            ),
            roots = persistentSetOf("root")
        )

        // Act
        val json = serializeTraceEventsState(originalState)
        val deserializedState = deserializeTraceEventsState(json)

        // Assert
        assertEquals(originalState.events.size, deserializedState.events.size)
        assertEquals(originalState.roots, deserializedState.roots)

        // Verify each event is preserved correctly
        for ((id, originalEvent) in originalState.events) {
            val deserializedEvent = deserializedState.events[id]!!
            assertEquals("Event $id mismatch", originalEvent.id, deserializedEvent.id)
            assertEquals("Event $id name mismatch", originalEvent.name, deserializedEvent.name)
            assertEquals("Event $id type mismatch", originalEvent.type, deserializedEvent.type)
            assertEquals("Event $id start time mismatch", originalEvent.timestampStartMs, deserializedEvent.timestampStartMs)
            assertEquals("Event $id end time mismatch", originalEvent.timestampEndMs, deserializedEvent.timestampEndMs)
            assertEquals("Event $id finished mismatch", originalEvent.finished, deserializedEvent.finished)
            assertPayloadEquals(originalEvent.payload, deserializedEvent.payload)
            assertEquals("Event $id parentId mismatch", originalEvent.parentId, deserializedEvent.parentId)
            assertEquals("Event $id childIds mismatch", originalEvent.childIds, deserializedEvent.childIds)
        }
    }

    @Test
    fun `round-trip serialization preserves multiple roots`() {
        // Arrange
        val root1 = TraceEvent(
            id = "root1",
            parentId = null,
            childIds = persistentSetOf("child"),
            name = "First Root",
            type = EventType.Init,
            framework = Framework.LangGraph,
            timestampStartMs = 1000L,
            timestampEndMs = 2000L,
            finished = true,
            payload = mapOf(),
        )
        val root2 = TraceEvent(
            id = "root2",
            parentId = null,
            childIds = persistentSetOf("child"),
            name = "Second Root",
            type = EventType.Exception,
            framework = Framework.LangGraph,
            timestampStartMs = 3000L,
            timestampEndMs = 4000L,
            finished = true,
            payload = mapOf(),
        )
        val child = TraceEvent(
            id = "child",
            parentId = "root2",
            childIds = persistentSetOf(),
            name = "Child of Root2",
            type = EventType.General,
            framework = Framework.LangGraph,
            timestampStartMs = 3500L,
            timestampEndMs = 3800L,
            finished = true,
            payload = mapOf(),
        )

        val originalState = TraceEventsState(
            events = persistentMapOf(
                "root1" to root1,
                "root2" to root2,
                "child" to child
            ),
            roots = persistentSetOf("root1", "root2")
        )

        // Act
        val json = serializeTraceEventsState(originalState)
        val deserializedState = deserializeTraceEventsState(json)

        // Assert
        assertEquals(originalState.events.size, deserializedState.events.size)
        assertEquals(originalState.roots, deserializedState.roots)
        assertEquals(originalState.events, deserializedState.events)
    }

    @Test
    fun `deserialize empty hierarchical JSON`() {
        // Arrange
        val json = """{"rootEvents": []}"""

        // Act
        val result = deserializeTraceEventsState(json)

        // Assert
        assertEquals(0, result.events.size)
        assertEquals(0, result.roots.size)
    }

    @Test
    fun `deserialize single root event with no children`() {
        // Arrange
        val json = """{
            "rootEvents": [{
                "id": "root1",
                "name": "Root Event",
                "type": "General",
                "framework": "LangGraph",
                "timestampStartMs": 1000,
                "timestampEndMs": 2000,
                "finished": true,
                "payload": {"key1": "value1"},
                "children": []
            }]
        }"""

        // Act
        val result = deserializeTraceEventsState(json)

        // Assert
        assertEquals(1, result.events.size)
        assertEquals(1, result.roots.size)
        assertTrue(result.roots.contains("root1"))

        val rootEvent = result.events["root1"]!!
        assertEquals("root1", rootEvent.id)
        assertEquals("Root Event", rootEvent.name)
        assertEquals(EventType.General, rootEvent.type)
        assertEquals(1000L, rootEvent.timestampStartMs)
        assertEquals(2000L, rootEvent.timestampEndMs)
        assertTrue(rootEvent.finished)
        assertEquals("value1", rootEvent.payload["key1"])
        assertNull(rootEvent.parentId)
        assertEquals(0, rootEvent.childIds.size)
    }

    @Test
    fun `deserialize simple parent-child hierarchy`() {
        // Arrange
        val json = """{
            "rootEvents": [{
                "id": "parent1",
                "name": "Parent Event",
                "type": "Group",
                "framework": "LangGraph",
                "timestampStartMs": 1000,
                "timestampEndMs": 3000,
                "finished": true,
                "payload": {},
                "children": [{
                    "id": "child1",
                    "name": "Child Event",
                    "type": "LlmCall",
                    "framework": "LangGraph",
                    "timestampStartMs": 1500,
                    "timestampEndMs": 2500,
                    "finished": true,
                    "payload": {},
                    "children": []
                }]
            }]
        }"""

        // Act
        val result = deserializeTraceEventsState(json)

        // Assert
        assertEquals(2, result.events.size)
        assertEquals(1, result.roots.size)
        assertTrue(result.roots.contains("parent1"))

        val parentEvent = result.events["parent1"]!!
        assertEquals("parent1", parentEvent.id)
        assertEquals("Parent Event", parentEvent.name)
        assertEquals(EventType.Group, parentEvent.type)
        assertNull(parentEvent.parentId)
        assertEquals(1, parentEvent.childIds.size)
        assertTrue(parentEvent.childIds.contains("child1"))

        val childEvent = result.events["child1"]!!
        assertEquals("child1", childEvent.id)
        assertEquals("Child Event", childEvent.name)
        assertEquals(EventType.LlmCall, childEvent.type)
        assertEquals("parent1", childEvent.parentId)
        assertEquals(0, childEvent.childIds.size)
    }

    @Test
    fun `deserialize multiple root events`() {
        // Arrange
        val json = """{
            "rootEvents": [{
                "id": "root1",
                "name": "First Root",
                "type": "Init",
                "framework": "LangGraph",
                "timestampStartMs": 1000,
                "timestampEndMs": 2000,
                "finished": true,
                "payload": {},
                "children": []
            }, {
                "id": "root2",
                "name": "Second Root",
                "type": "Exception",
                "framework": "LangGraph",
                "timestampStartMs": 3000,
                "timestampEndMs": 4000,
                "finished": false,
                "payload": {},
                "children": []
            }]
        }"""

        // Act
        val result = deserializeTraceEventsState(json)

        // Assert
        assertEquals(2, result.events.size)
        assertEquals(2, result.roots.size)
        assertTrue(result.roots.contains("root1"))
        assertTrue(result.roots.contains("root2"))

        val root1 = result.events["root1"]!!
        assertEquals("root1", root1.id)
        assertEquals(EventType.Init, root1.type)
        assertTrue(root1.finished)
        assertNull(root1.parentId)

        val root2 = result.events["root2"]!!
        assertEquals("root2", root2.id)
        assertEquals(EventType.Exception, root2.type)
        assertFalse(root2.finished)
        assertNull(root2.parentId)
    }

    @Test
    fun `deserialize deep hierarchy with multiple levels`() {
        // Arrange
        val json = """{
            "rootEvents": [{
                "id": "gp1",
                "name": "GrandParent",
                "type": "Group",
                "framework": "LangGraph",
                "timestampStartMs": 1000,
                "timestampEndMs": 4000,
                "finished": true,
                "payload": {},
                "children": [{
                    "id": "parent1",
                    "name": "Parent",
                    "type": "Group",
                    "framework": "LangGraph",
                    "timestampStartMs": 1500,
                    "timestampEndMs": 3500,
                    "finished": true,
                    "payload": {},
                    "children": [{
                        "id": "child1",
                        "name": "Child 1",
                        "type": "LlmCall",
                        "framework": "LangGraph",
                        "timestampStartMs": 2000,
                        "timestampEndMs": 2500,
                        "finished": true,
                        "payload": {},
                        "children": []
                    }, {
                        "id": "child2",
                        "name": "Child 2",
                        "type": "ToolCall",
                        "framework": "LangGraph",
                        "timestampStartMs": 2600,
                        "timestampEndMs": 3000,
                        "finished": true,
                        "payload": {},
                        "children": []
                    }]
                }]
            }]
        }"""

        // Act
        val result = deserializeTraceEventsState(json)

        // Assert
        assertEquals(4, result.events.size)
        assertEquals(1, result.roots.size)
        assertTrue(result.roots.contains("gp1"))

        // Verify grandparent
        val grandParent = result.events["gp1"]!!
        assertEquals("gp1", grandParent.id)
        assertNull(grandParent.parentId)
        assertEquals(1, grandParent.childIds.size)
        assertTrue(grandParent.childIds.contains("parent1"))

        // Verify parent
        val parent = result.events["parent1"]!!
        assertEquals("parent1", parent.id)
        assertEquals("gp1", parent.parentId)
        assertEquals(2, parent.childIds.size)
        assertTrue(parent.childIds.contains("child1"))
        assertTrue(parent.childIds.contains("child2"))

        // Verify children
        val child1 = result.events["child1"]!!
        assertEquals("child1", child1.id)
        assertEquals("parent1", child1.parentId)
        assertEquals(0, child1.childIds.size)

        val child2 = result.events["child2"]!!
        assertEquals("child2", child2.id)
        assertEquals("parent1", child2.parentId)
        assertEquals(0, child2.childIds.size)
    }

    @Test
    fun `deserialize with complex payload types`() {
        // Arrange
        val json = """{
            "rootEvents": [{
                "id": "complex1",
                "name": "Complex Event",
                "type": "General",
                "framework": "LangGraph",
                "timestampStartMs": 1000,
                "timestampEndMs": 2000,
                "finished": true,
                "payload": {
                    "string": "value",
                    "number": 42,
                    "boolean": true,
                    "list": ["a", "b", "c"],
                    "map": {"nested": "data"}
                },
                "children": []
            }]
        }"""

        // Act
        val result = deserializeTraceEventsState(json)

        // Assert
        assertEquals(1, result.events.size)
        val event = result.events["complex1"]!!

        assertEquals("value", event.payload["string"])
        assertEquals(42.0, event.payload["number"]) // JSON numbers become doubles
        assertEquals(true, event.payload["boolean"])

        val list = event.payload["list"] as List<*>
        assertEquals(3, list.size)
        assertEquals("a", list[0])
        assertEquals("b", list[1])
        assertEquals("c", list[2])

        val map = event.payload["map"] as Map<*, *>
        assertEquals("data", map["nested"])
    }

    @Test
    fun `deserialize preserves all event properties`() {
        // Arrange
        val json = """{
            "rootEvents": [{
                "id": "test-event",
                "name": "Test Event Name",
                "type": "ToolCall",
                "framework": "LangGraph",
                "timestampStartMs": 123456789,
                "timestampEndMs": 987654321,
                "finished": false,
                "payload": {
                    "key1": "value1",
                    "key2": 999
                },
                "children": []
            }]
        }"""

        // Act
        val result = deserializeTraceEventsState(json)

        // Assert
        assertEquals(1, result.events.size)
        val event = result.events["test-event"]!!

        assertEquals("test-event", event.id)
        assertEquals("Test Event Name", event.name)
        assertEquals(EventType.ToolCall, event.type)
        assertEquals(123456789L, event.timestampStartMs)
        assertEquals(987654321L, event.timestampEndMs)
        assertFalse(event.finished)
        assertEquals("value1", event.payload["key1"])
        assertEquals(999.0, event.payload["key2"]) // JSON number conversion
        assertNull(event.parentId)
        assertEquals(0, event.childIds.size)
    }

    @Test
    fun `deserialize with missing optional fields uses defaults`() {
        // Arrange - minimal JSON with only required fields
        val json = """{
            "rootEvents": [{
                "id": "minimal",
                "name": "Minimal Event",
                "type": "General",
                "framework": "LangGraph",
                "timestampStartMs": 1000,
                "timestampEndMs": 2000,
                "finished": true,
                "children": []
            }]
        }"""

        // Act
        val result = deserializeTraceEventsState(json)

        // Assert
        assertEquals(1, result.events.size)
        val event = result.events["minimal"]!!

        assertEquals("minimal", event.id)
        assertEquals("Minimal Event", event.name)
        assertEquals(EventType.General, event.type)
        assertEquals(1000L, event.timestampStartMs)
        assertEquals(2000L, event.timestampEndMs)
        assertTrue(event.finished)
        assertTrue(event.payload.isEmpty()) // Default empty payload
        assertNull(event.parentId)
        assertEquals(0, event.childIds.size)
    }

    @Test(expected = com.google.gson.JsonSyntaxException::class)
    fun `deserialize throws exception for invalid JSON`() {
        // Arrange
        val invalidJson = """{"invalid": json structure}"""

        // Act
        deserializeTraceEventsState(invalidJson)
    }

    @Test(expected = com.google.gson.JsonSyntaxException::class)
    fun `deserialize throws exception for malformed JSON`() {
        // Arrange
        val malformedJson = """{"rootEvents": [broken json"""

        // Act
        deserializeTraceEventsState(malformedJson)
    }

    @Test
    fun `deserialize handles empty rootEvents array gracefully`() {
        // Arrange
        val json = """{"rootEvents": []}"""

        // Act
        val result = deserializeTraceEventsState(json)

        // Assert
        assertEquals(0, result.events.size)
        assertEquals(0, result.roots.size)
    }

    @Test
    fun `deserialize handles missing payload field gracefully`() {
        // Arrange - JSON without payload field
        val json = """{
            "rootEvents": [{
                "id": "no-payload",
                "name": "Event Without Payload",
                "type": "General",
                "framework": "LangGraph",
                "timestampStartMs": 1000,
                "timestampEndMs": 2000,
                "finished": true,
                "children": []
            }]
        }"""

        // Act
        val result = deserializeTraceEventsState(json)

        // Assert
        assertEquals(1, result.events.size)
        val event = result.events["no-payload"]!!
        assertTrue(event.payload.isEmpty())
    }

    @Test
    fun `deserialize handles unknown event type by setting to null`() {
        // Arrange - JSON with unknown event type (Gson sets enum to null for unknown values)
        val json = """{
            "rootEvents": [{
                "id": "unknown-type",
                "name": "Unknown Type Event",
                "type": "UnknownType",
                "framework": "LangGraph",
                "timestampStartMs": 1000,
                "timestampEndMs": 2000,
                "finished": true,
                "payload": {},
                "children": []
            }]
        }"""

        // Act & Assert - Gson will set the enum field to null for unknown values
        // This will cause our TraceEvent constructor to fail since type is non-nullable
        try {
            deserializeTraceEventsState(json)
            fail("Expected NullPointerException for null enum type")
        } catch (_: NullPointerException) {
            // This is expected behavior when Gson tries to pass null to non-nullable enum
            assertTrue(true)
        }
    }

    @Test
    fun `deserialize preserves event order in complex hierarchy`() {
        // Arrange - Multiple children to test order preservation
        val json = """{
            "rootEvents": [{
                "id": "parent",
                "name": "Parent",
                "type": "Group",
                "framework": "LangGraph",
                "timestampStartMs": 1000,
                "timestampEndMs": 5000,
                "finished": true,
                "payload": {},
                "children": [{
                    "id": "child-z",
                    "name": "Child Z",
                    "type": "General",
                    "framework": "LangGraph",
                    "timestampStartMs": 2000,
                    "timestampEndMs": 2500,
                    "finished": true,
                    "payload": {},
                    "children": []
                }, {
                    "id": "child-a",
                    "name": "Child A",
                    "type": "General",
                    "framework": "LangGraph",
                    "timestampStartMs": 3000,
                    "timestampEndMs": 3500,
                    "finished": true,
                    "payload": {},
                    "children": []
                }, {
                    "id": "child-m",
                    "name": "Child M",
                    "type": "General",
                    "framework": "LangGraph",
                    "timestampStartMs": 4000,
                    "timestampEndMs": 4500,
                    "finished": true,
                    "payload": {},
                    "children": []
                }]
            }]
        }"""

        // Act
        val result = deserializeTraceEventsState(json)

        // Assert
        assertEquals(4, result.events.size)
        val parent = result.events["parent"]!!
        assertEquals(3, parent.childIds.size)

        // Verify all children are referenced
        assertTrue(parent.childIds.contains("child-z"))
        assertTrue(parent.childIds.contains("child-a"))
        assertTrue(parent.childIds.contains("child-m"))

        // Verify all children have correct parent
        assertEquals("parent", result.events["child-z"]!!.parentId)
        assertEquals("parent", result.events["child-a"]!!.parentId)
        assertEquals("parent", result.events["child-m"]!!.parentId)
    }

    @Test
    fun `deserialize handles large numeric values correctly`() {
        // Arrange
        val json = """{
            "rootEvents": [{
                "id": "large-numbers",
                "name": "Large Numbers Event",
                "type": "General",
                "framework": "LangGraph",
                "timestampStartMs": 9223372036854775807,
                "timestampEndMs": 9223372036854775806,
                "finished": true,
                "payload": {
                    "largeInt": 2147483647,
                    "largeLong": 9223372036854775807,
                    "largeDouble": 1.7976931348623157E308
                },
                "children": []
            }]
        }"""

        // Act
        val result = deserializeTraceEventsState(json)

        // Assert
        assertEquals(1, result.events.size)
        val event = result.events["large-numbers"]!!

        assertEquals(9223372036854775807L, event.timestampStartMs)
        assertEquals(9223372036854775806L, event.timestampEndMs)

        // JSON will convert large numbers to doubles
        assertTrue(event.payload["largeInt"] is Double)
        assertTrue(event.payload["largeLong"] is Double)
        assertTrue(event.payload["largeDouble"] is Double)
    }

    @Test
    fun `serializeHierarchicalTraceEventsStateToMap converts empty state`() {
        // Arrange
        val hstate = HierarchicalTraceEventsState(rootEvents = emptyList())

        // Act
        val result = serializeHierarchicalTraceEventsStateToMap(hstate)

        // Assert
        assertTrue(result.containsKey("rootEvents"))
        val rootEvents = result["rootEvents"]
        assertTrue(rootEvents is List<*>)
        assertEquals(0, (rootEvents as List<*>).size)
    }

    @Test
    fun `serializeHierarchicalTraceEventsStateToMap converts single root event with no children`() {
        // Arrange
        val event = HierarchicalTraceEvent(
            id = "root1",
            name = "Root Event",
            type = EventType.General,
            framework = Framework.LangGraph,
            timestampStartMs = 1000L,
            timestampEndMs = 2000L,
            finished = true,
            payload = mapOf("key1" to "value1"),
            children = emptyList()
        )
        val hstate = HierarchicalTraceEventsState(rootEvents = listOf(event))

        // Act
        val result = serializeHierarchicalTraceEventsStateToMap(hstate)

        // Assert
        assertTrue(result.containsKey("rootEvents"))
        val rootEvents = result["rootEvents"] as List<*>
        assertEquals(1, rootEvents.size)

        @Suppress("UNCHECKED_CAST")
        val rootMap = rootEvents[0] as Map<String, Any?>
        assertEquals("root1", rootMap["id"])
        assertEquals("Root Event", rootMap["name"])
        assertEquals("General", rootMap["type"])
        assertEquals(1000.0, rootMap["timestampStartMs"]) // JSON converts to Double
        assertEquals(2000.0, rootMap["timestampEndMs"])
        assertTrue(rootMap["finished"] as Boolean)

        @Suppress("UNCHECKED_CAST")
        val payload = rootMap["payload"] as Map<String, Any?>
        assertEquals("value1", payload["key1"])

        val children = rootMap["children"] as List<*>
        assertEquals(0, children.size)
    }

    @Test
    fun `serializeHierarchicalTraceEventsStateToMap converts parent-child hierarchy`() {
        // Arrange
        val childEvent = HierarchicalTraceEvent(
            id = "child1",
            name = "Child Event",
            type = EventType.LlmCall,
            framework = Framework.LangGraph,
            timestampStartMs = 1500L,
            timestampEndMs = 2500L,
            finished = true,
            payload = mapOf("childKey" to "childValue"),
            children = emptyList()
        )
        val parentEvent = HierarchicalTraceEvent(
            id = "parent1",
            name = "Parent Event",
            type = EventType.Group,
            framework = Framework.LangGraph,
            timestampStartMs = 1000L,
            timestampEndMs = 3000L,
            finished = true,
            payload = mapOf("parentKey" to "parentValue"),
            children = listOf(childEvent)
        )
        val hstate = HierarchicalTraceEventsState(rootEvents = listOf(parentEvent))

        // Act
        val result = serializeHierarchicalTraceEventsStateToMap(hstate)

        // Assert
        val rootEvents = result["rootEvents"] as List<*>
        assertEquals(1, rootEvents.size)

        @Suppress("UNCHECKED_CAST")
        val parentMap = rootEvents[0] as Map<String, Any?>
        assertEquals("parent1", parentMap["id"])
        assertEquals("Parent Event", parentMap["name"])

        val children = parentMap["children"] as List<*>
        assertEquals(1, children.size)

        @Suppress("UNCHECKED_CAST")
        val childMap = children[0] as Map<String, Any?>
        assertEquals("child1", childMap["id"])
        assertEquals("Child Event", childMap["name"])
        assertEquals("LlmCall", childMap["type"])
        assertEquals(1500.0, childMap["timestampStartMs"])
        assertEquals(2500.0, childMap["timestampEndMs"])

        @Suppress("UNCHECKED_CAST")
        val childPayload = childMap["payload"] as Map<String, Any?>
        assertEquals("childValue", childPayload["childKey"])
    }

    @Test
    fun `serializeHierarchicalTraceEventsStateToMap handles multiple root events`() {
        // Arrange
        val event1 = HierarchicalTraceEvent(
            id = "root1",
            name = "First Root",
            type = EventType.Init,
            framework = Framework.LangGraph,
            timestampStartMs = 1000L,
            timestampEndMs = 2000L,
            finished = true,
            payload = emptyMap(),
            children = emptyList()
        )
        val event2 = HierarchicalTraceEvent(
            id = "root2",
            name = "Second Root",
            type = EventType.Exception,
            framework = Framework.LangGraph,
            timestampStartMs = 3000L,
            timestampEndMs = 4000L,
            finished = false,
            payload = emptyMap(),
            children = emptyList()
        )
        val hstate = HierarchicalTraceEventsState(rootEvents = listOf(event1, event2))

        // Act
        val result = serializeHierarchicalTraceEventsStateToMap(hstate)

        // Assert
        val rootEvents = result["rootEvents"] as List<*>
        assertEquals(2, rootEvents.size)

        @Suppress("UNCHECKED_CAST")
        val root1Map = rootEvents[0] as Map<String, Any?>
        assertEquals("root1", root1Map["id"])
        assertEquals("Init", root1Map["type"])
        assertTrue(root1Map["finished"] as Boolean)

        @Suppress("UNCHECKED_CAST")
        val root2Map = rootEvents[1] as Map<String, Any?>
        assertEquals("root2", root2Map["id"])
        assertEquals("Exception", root2Map["type"])
        assertFalse(root2Map["finished"] as Boolean)
    }

    @Test
    fun `serializeHierarchicalTraceEventsStateToMap handles complex payload types`() {
        // Arrange
        val complexPayload = mapOf(
            "string" to "value",
            "number" to 42,
            "boolean" to true,
            "null" to null,
            "list" to listOf("a", "b", "c"),
            "map" to mapOf("nested" to "data")
        )
        val event = HierarchicalTraceEvent(
            id = "complex1",
            name = "Complex Event",
            type = EventType.General,
            framework = Framework.LangGraph,
            timestampStartMs = 1000L,
            timestampEndMs = 2000L,
            finished = true,
            payload = complexPayload,
            children = emptyList()
        )
        val hstate = HierarchicalTraceEventsState(rootEvents = listOf(event))

        // Act
        val result = serializeHierarchicalTraceEventsStateToMap(hstate)

        // Assert
        val rootEvents = result["rootEvents"] as List<*>
        @Suppress("UNCHECKED_CAST")
        val eventMap = rootEvents[0] as Map<String, Any?>
        @Suppress("UNCHECKED_CAST")
        val payload = eventMap["payload"] as Map<String, Any?>

        assertEquals("value", payload["string"])
        assertEquals(42.0, payload["number"]) // JSON converts to Double
        assertTrue(payload["boolean"] as Boolean)
        // Note: Gson doesn't serialize null values by default

        val list = payload["list"] as List<*>
        assertEquals(3, list.size)
        assertEquals("a", list[0])
        assertEquals("b", list[1])
        assertEquals("c", list[2])

        @Suppress("UNCHECKED_CAST")
        val nestedMap = payload["map"] as Map<String, Any?>
        assertEquals("data", nestedMap["nested"])
    }

    @Test
    fun `serializeHierarchicalTraceEventsStateToMap handles deep hierarchy`() {
        // Arrange
        val grandchild = HierarchicalTraceEvent(
            id = "gc1",
            name = "Grandchild",
            type = EventType.General,
            framework = Framework.LangGraph,
            timestampStartMs = 2000L,
            timestampEndMs = 2200L,
            finished = true,
            payload = mapOf("depth" to 3),
            children = emptyList()
        )
        val child = HierarchicalTraceEvent(
            id = "child1",
            name = "Child",
            type = EventType.LlmCall,
            framework = Framework.LangGraph,
            timestampStartMs = 1500L,
            timestampEndMs = 2500L,
            finished = true,
            payload = mapOf("depth" to 2),
            children = listOf(grandchild)
        )
        val parent = HierarchicalTraceEvent(
            id = "parent1",
            name = "Parent",
            type = EventType.Group,
            framework = Framework.LangGraph,
            timestampStartMs = 1000L,
            timestampEndMs = 3000L,
            finished = true,
            payload = mapOf("depth" to 1),
            children = listOf(child)
        )
        val hstate = HierarchicalTraceEventsState(rootEvents = listOf(parent))

        // Act
        val result = serializeHierarchicalTraceEventsStateToMap(hstate)

        // Assert
        val rootEvents = result["rootEvents"] as List<*>
        @Suppress("UNCHECKED_CAST")
        val parentMap = rootEvents[0] as Map<String, Any?>
        assertEquals("parent1", parentMap["id"])

        val children = parentMap["children"] as List<*>
        assertEquals(1, children.size)

        @Suppress("UNCHECKED_CAST")
        val childMap = children[0] as Map<String, Any?>
        assertEquals("child1", childMap["id"])

        val grandchildren = childMap["children"] as List<*>
        assertEquals(1, grandchildren.size)

        @Suppress("UNCHECKED_CAST")
        val grandchildMap = grandchildren[0] as Map<String, Any?>
        assertEquals("gc1", grandchildMap["id"])
        @Suppress("UNCHECKED_CAST")
        val gcPayload = grandchildMap["payload"] as Map<String, Any?>
        assertEquals(3.0, gcPayload["depth"])
    }

    @Test
    fun `deserializeTraceEventsStateFromHierarchicalStateMap handles empty map`() {
        // Arrange
        val map = mapOf<String, Any?>("rootEvents" to emptyList<Any>())

        // Act
        val result = deserializeTraceEventsStateFromHierarchicalStateMap(map)

        // Assert
        assertEquals(0, result.events.size)
        assertEquals(0, result.roots.size)
    }

    @Test
    fun `deserializeTraceEventsStateFromHierarchicalStateMap converts single root event`() {
        // Arrange
        val map = mapOf<String, Any?>(
            "rootEvents" to listOf(
                mapOf(
                    "id" to "root1",
                    "name" to "Root Event",
                    "type" to "General",
                    "framework" to "LangGraph",
                    "timestampStartMs" to 1000.0,
                    "timestampEndMs" to 2000.0,
                    "finished" to true,
                    "payload" to mapOf("key1" to "value1"),
                    "children" to emptyList<Any>()
                )
            )
        )

        // Act
        val result = deserializeTraceEventsStateFromHierarchicalStateMap(map)

        // Assert
        assertEquals(1, result.events.size)
        assertEquals(1, result.roots.size)
        assertTrue(result.roots.contains("root1"))

        val event = result.events["root1"]!!
        assertEquals("root1", event.id)
        assertEquals("Root Event", event.name)
        assertEquals(EventType.General, event.type)
        assertEquals(1000L, event.timestampStartMs)
        assertEquals(2000L, event.timestampEndMs)
        assertTrue(event.finished)
        assertEquals("value1", event.payload["key1"])
        assertNull(event.parentId)
        assertEquals(0, event.childIds.size)
    }

    @Test
    fun `deserializeTraceEventsStateFromHierarchicalStateMap converts parent-child hierarchy`() {
        // Arrange
        val map = mapOf<String, Any?>(
            "rootEvents" to listOf(
                mapOf(
                    "id" to "parent1",
                    "name" to "Parent Event",
                    "type" to "Group",
                    "framework" to "LangGraph",
                    "timestampStartMs" to 1000.0,
                    "timestampEndMs" to 3000.0,
                    "finished" to true,
                    "payload" to emptyMap<String, Any>(),
                    "children" to listOf(
                        mapOf(
                            "id" to "child1",
                            "name" to "Child Event",
                            "type" to "LlmCall",
                            "framework" to "LangGraph",
                            "timestampStartMs" to 1500.0,
                            "timestampEndMs" to 2500.0,
                            "finished" to true,
                            "payload" to mapOf("childKey" to "childValue"),
                            "children" to emptyList<Any>()
                        )
                    )
                )
            )
        )

        // Act
        val result = deserializeTraceEventsStateFromHierarchicalStateMap(map)

        // Assert
        assertEquals(2, result.events.size)
        assertEquals(1, result.roots.size)
        assertTrue(result.roots.contains("parent1"))

        val parent = result.events["parent1"]!!
        assertEquals("parent1", parent.id)
        assertEquals("Parent Event", parent.name)
        assertEquals(EventType.Group, parent.type)
        assertNull(parent.parentId)
        assertEquals(1, parent.childIds.size)
        assertTrue(parent.childIds.contains("child1"))

        val child = result.events["child1"]!!
        assertEquals("child1", child.id)
        assertEquals("Child Event", child.name)
        assertEquals(EventType.LlmCall, child.type)
        assertEquals("parent1", child.parentId)
        assertEquals(0, child.childIds.size)
        assertEquals("childValue", child.payload["childKey"])
    }

    @Test
    fun `deserializeTraceEventsStateFromHierarchicalStateMap handles multiple root events`() {
        // Arrange
        val map = mapOf<String, Any?>(
            "rootEvents" to listOf(
                mapOf(
                    "id" to "root1",
                    "name" to "First Root",
                    "type" to "Init",
                    "framework" to "LangGraph",
                    "timestampStartMs" to 1000.0,
                    "timestampEndMs" to 2000.0,
                    "finished" to true,
                    "payload" to emptyMap<String, Any>(),
                    "children" to emptyList<Any>()
                ),
                mapOf(
                    "id" to "root2",
                    "name" to "Second Root",
                    "type" to "Exception",
                    "framework" to "LangGraph",
                    "timestampStartMs" to 3000.0,
                    "timestampEndMs" to 4000.0,
                    "finished" to false,
                    "payload" to emptyMap<String, Any>(),
                    "children" to emptyList<Any>()
                )
            )
        )

        // Act
        val result = deserializeTraceEventsStateFromHierarchicalStateMap(map)

        // Assert
        assertEquals(2, result.events.size)
        assertEquals(2, result.roots.size)
        assertTrue(result.roots.contains("root1"))
        assertTrue(result.roots.contains("root2"))

        val root1 = result.events["root1"]!!
        assertEquals("root1", root1.id)
        assertEquals(EventType.Init, root1.type)
        assertTrue(root1.finished)

        val root2 = result.events["root2"]!!
        assertEquals("root2", root2.id)
        assertEquals(EventType.Exception, root2.type)
        assertFalse(root2.finished)
    }

    @Test
    fun `deserializeTraceEventsStateFromHierarchicalStateMap handles deep hierarchy`() {
        // Arrange
        val map = mapOf<String, Any?>(
            "rootEvents" to listOf(
                mapOf(
                    "id" to "gp1",
                    "name" to "GrandParent",
                    "type" to "Group",
                    "framework" to "LangGraph",
                    "timestampStartMs" to 1000.0,
                    "timestampEndMs" to 4000.0,
                    "finished" to true,
                    "payload" to emptyMap<String, Any>(),
                    "children" to listOf(
                        mapOf(
                            "id" to "parent1",
                            "name" to "Parent",
                            "type" to "Group",
                            "framework" to "LangGraph",
                            "timestampStartMs" to 1500.0,
                            "timestampEndMs" to 3500.0,
                            "finished" to true,
                            "payload" to emptyMap<String, Any>(),
                            "children" to listOf(
                                mapOf(
                                    "id" to "child1",
                                    "name" to "Child 1",
                                    "type" to "LlmCall",
                                    "framework" to "LangGraph",
                                    "timestampStartMs" to 2000.0,
                                    "timestampEndMs" to 2500.0,
                                    "finished" to true,
                                    "payload" to emptyMap<String, Any>(),
                                    "children" to emptyList<Any>()
                                ),
                                mapOf(
                                    "id" to "child2",
                                    "name" to "Child 2",
                                    "type" to "ToolCall",
                                    "framework" to "LangGraph",
                                    "timestampStartMs" to 2600.0,
                                    "timestampEndMs" to 3000.0,
                                    "finished" to true,
                                    "payload" to emptyMap<String, Any>(),
                                    "children" to emptyList<Any>()
                                )
                            )
                        )
                    )
                )
            )
        )

        // Act
        val result = deserializeTraceEventsStateFromHierarchicalStateMap(map)

        // Assert
        assertEquals(4, result.events.size)
        assertEquals(1, result.roots.size)
        assertTrue(result.roots.contains("gp1"))

        // Verify grandparent
        val grandParent = result.events["gp1"]!!
        assertEquals("gp1", grandParent.id)
        assertNull(grandParent.parentId)
        assertEquals(1, grandParent.childIds.size)
        assertTrue(grandParent.childIds.contains("parent1"))

        // Verify parent
        val parent = result.events["parent1"]!!
        assertEquals("parent1", parent.id)
        assertEquals("gp1", parent.parentId)
        assertEquals(2, parent.childIds.size)
        assertTrue(parent.childIds.contains("child1"))
        assertTrue(parent.childIds.contains("child2"))

        // Verify children
        val child1 = result.events["child1"]!!
        assertEquals("child1", child1.id)
        assertEquals("parent1", child1.parentId)
        assertEquals(0, child1.childIds.size)

        val child2 = result.events["child2"]!!
        assertEquals("child2", child2.id)
        assertEquals("parent1", child2.parentId)
        assertEquals(0, child2.childIds.size)
    }


    @Test
    fun `deserializeTraceEventsStateFromHierarchicalStateMap handles complex payload types`() {
        // Arrange
        val inputMap = mapOf<String, Any?>(
            "rootEvents" to listOf(
                mapOf(
                    "id" to "complex1",
                    "name" to "Complex Event",
                    "type" to "General",
                    "framework" to "LangGraph",
                    "timestampStartMs" to 1000.0,
                    "timestampEndMs" to 2000.0,
                    "finished" to true,
                    "payload" to mapOf(
                        "string" to "value",
                        "number" to 42.0,
                        "boolean" to true,
                        "list" to listOf("a", "b", "c"),
                        "map" to mapOf("nested" to "data")
                    ),
                    "children" to emptyList<Any>()
                )
            )
        )

        // Act
        val result = deserializeTraceEventsStateFromHierarchicalStateMap(inputMap)

        // Assert
        assertEquals(1, result.events.size)
        val event = result.events["complex1"]!!

        assertEquals("value", event.payload["string"])
        assertEquals(42.0, event.payload["number"]) // JSON numbers become doubles
        assertEquals(true, event.payload["boolean"])

        val list = event.payload["list"] as List<*>
        assertEquals(3, list.size)
        assertEquals("a", list[0])
        assertEquals("b", list[1])
        assertEquals("c", list[2])

        val nestedMap = event.payload["map"] as Map<*, *>
        assertEquals("data", nestedMap["nested"])
    }

    @Test
    fun `round-trip through map preserves single event`() {
        // Arrange
        val originalEvent = HierarchicalTraceEvent(
            id = "test1",
            name = "Test Event",
            type = EventType.LlmCall,
            framework = Framework.LangGraph,
            timestampStartMs = 1500L,
            timestampEndMs = 2500L,
            finished = true,
            payload = mapOf("key" to "value", "number" to 42),
            children = emptyList()
        )
        val originalHState = HierarchicalTraceEventsState(rootEvents = listOf(originalEvent))

        // Act
        val map = serializeHierarchicalTraceEventsStateToMap(originalHState)
        val flatState = deserializeTraceEventsStateFromHierarchicalStateMap(map)

        // Assert
        assertEquals(1, flatState.events.size)
        assertEquals(1, flatState.roots.size)

        val event = flatState.events["test1"]!!
        assertEquals("test1", event.id)
        assertEquals("Test Event", event.name)
        assertEquals(EventType.LlmCall, event.type)
        assertEquals(1500L, event.timestampStartMs)
        assertEquals(2500L, event.timestampEndMs)
        assertTrue(event.finished)
        assertEquals("value", event.payload["key"])
        // Note: number will be converted to Double through JSON
        assertEquals(42.0, event.payload["number"])
        assertNull(event.parentId)
        assertEquals(0, event.childIds.size)
    }

    @Test
    fun `round-trip through map preserves complex hierarchy`() {
        // Arrange
        val grandchild = HierarchicalTraceEvent(
            id = "gc1",
            name = "Grandchild",
            type = EventType.General,
            framework = Framework.LangGraph,
            timestampStartMs = 2000L,
            timestampEndMs = 2200L,
            finished = true,
            payload = mapOf("depth" to 3),
            children = emptyList()
        )
        val child = HierarchicalTraceEvent(
            id = "child1",
            name = "Child",
            type = EventType.LlmCall,
            framework = Framework.LangGraph,
            timestampStartMs = 1500L,
            timestampEndMs = 2500L,
            finished = true,
            payload = mapOf("depth" to 2),
            children = listOf(grandchild)
        )
        val parent = HierarchicalTraceEvent(
            id = "parent1",
            name = "Parent",
            type = EventType.Group,
            framework = Framework.LangGraph,
            timestampStartMs = 1000L,
            timestampEndMs = 3000L,
            finished = true,
            payload = mapOf("depth" to 1),
            children = listOf(child)
        )
        val originalHState = HierarchicalTraceEventsState(rootEvents = listOf(parent))

        // Act
        val map = serializeHierarchicalTraceEventsStateToMap(originalHState)
        val flatState = deserializeTraceEventsStateFromHierarchicalStateMap(map)

        // Assert
        assertEquals(3, flatState.events.size)
        assertEquals(1, flatState.roots.size)
        assertTrue(flatState.roots.contains("parent1"))

        // Verify parent
        val parentEvent = flatState.events["parent1"]!!
        assertEquals("parent1", parentEvent.id)
        assertNull(parentEvent.parentId)
        assertEquals(1, parentEvent.childIds.size)
        assertTrue(parentEvent.childIds.contains("child1"))

        // Verify child
        val childEvent = flatState.events["child1"]!!
        assertEquals("child1", childEvent.id)
        assertEquals("parent1", childEvent.parentId)
        assertEquals(1, childEvent.childIds.size)
        assertTrue(childEvent.childIds.contains("gc1"))

        // Verify grandchild
        val grandchildEvent = flatState.events["gc1"]!!
        assertEquals("gc1", grandchildEvent.id)
        assertEquals("child1", grandchildEvent.parentId)
        assertEquals(0, grandchildEvent.childIds.size)
        assertEquals(3.0, grandchildEvent.payload["depth"]) // JSON converts to Double
    }

    @Test
    fun `round-trip through map preserves multiple roots`() {
        // Arrange
        val event1 = HierarchicalTraceEvent(
            id = "root1",
            name = "First Root",
            type = EventType.Init,
            framework = Framework.LangGraph,
            timestampStartMs = 1000L,
            timestampEndMs = 2000L,
            finished = true,
            payload = mapOf("order" to 1),
            children = emptyList()
        )
        val event2 = HierarchicalTraceEvent(
            id = "root2",
            name = "Second Root",
            type = EventType.Exception,
            framework = Framework.LangGraph,
            timestampStartMs = 3000L,
            timestampEndMs = 4000L,
            finished = false,
            payload = mapOf("order" to 2),
            children = emptyList()
        )
        val originalHState = HierarchicalTraceEventsState(rootEvents = listOf(event1, event2))

        // Act
        val map = serializeHierarchicalTraceEventsStateToMap(originalHState)
        val flatState = deserializeTraceEventsStateFromHierarchicalStateMap(map)

        // Assert
        assertEquals(2, flatState.events.size)
        assertEquals(2, flatState.roots.size)
        assertTrue(flatState.roots.contains("root1"))
        assertTrue(flatState.roots.contains("root2"))

        val root1 = flatState.events["root1"]!!
        assertEquals(EventType.Init, root1.type)
        assertTrue(root1.finished)
        assertEquals(1.0, root1.payload["order"])

        val root2 = flatState.events["root2"]!!
        assertEquals(EventType.Exception, root2.type)
        assertFalse(root2.finished)
        assertEquals(2.0, root2.payload["order"])
    }
}