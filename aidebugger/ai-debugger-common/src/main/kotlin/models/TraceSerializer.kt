package com.intellij.aidebugger.common.models

import com.google.gson.GsonBuilder
import com.intellij.aidebugger.common.models.entities.EventType
import com.intellij.aidebugger.common.models.entities.Framework
import com.intellij.aidebugger.common.models.entities.TraceEvent
import kotlinx.collections.immutable.toPersistentMap
import kotlinx.collections.immutable.toPersistentSet

data class HierarchicalTraceEvent(
    val id: String,
    val name: String,
    val type: EventType,
    val framework: Framework,
    val timestampStartMs: Long,
    val timestampEndMs: Long,
    val finished: Boolean,
    val payload: Map<String, Any?>? = null,
    val children: List<HierarchicalTraceEvent>? = null
)

data class HierarchicalTraceEventsState(
    val rootEvents: List<HierarchicalTraceEvent>
)

fun serializeTraceEventsState(state: TraceEventsState): String {
    val hierarchicalState = buildHierarchicalStructure(state)
    val gson = GsonBuilder()
        .setPrettyPrinting()
        .create()

    return gson.toJson(hierarchicalState)
}

fun serializeTraceEventsState(state: TraceEventsState, eventId: String): String {
    val hierarchicalState = buildHierarchicalEvent(state, eventId)
    val gson = GsonBuilder()
        .setPrettyPrinting()
        .create()

    return gson.toJson(hierarchicalState)
}

fun deserializeTraceEventsState(json: String): TraceEventsState {
    val gson = GsonBuilder().create()
    val hierarchicalState = gson.fromJson(json, HierarchicalTraceEventsState::class.java)

    return buildFlatStructure(hierarchicalState)
}

private fun buildHierarchicalEvent(state: TraceEventsState, eventId: String): HierarchicalTraceEvent? {
    val event = state.events[eventId] ?: return null

    val children = event.childIds
        .mapNotNull { childId -> buildHierarchicalEvent(state = state, eventId = childId) }
        .sortedBy { it.timestampStartMs }

    return HierarchicalTraceEvent(
        id = event.id,
        name = event.name,
        type = event.type,
        framework = event.framework,
        timestampStartMs = event.timestampStartMs,
        timestampEndMs = event.timestampEndMs,
        finished = event.finished,
        payload = event.payload,
        children = children
    )
}

fun buildHierarchicalStructure(state: TraceEventsState): HierarchicalTraceEventsState {
    val rootEvents = state.roots
        .mapNotNull { rootId -> buildHierarchicalEvent(state = state, eventId = rootId) }
        .sortedBy { it.timestampStartMs }

    return HierarchicalTraceEventsState(rootEvents = rootEvents)
}

fun buildFlatStructure(hierarchicalState: HierarchicalTraceEventsState): TraceEventsState {
    val events = mutableMapOf<String, TraceEvent>()
    val rootIds = mutableSetOf<String>()

    fun flattenEvent(hierarchicalEvent: HierarchicalTraceEvent, parentId: String?) {
        val children = hierarchicalEvent.children ?: emptyList()
        val childIds = children.map { it.id }.toPersistentSet()

        val traceEvent = TraceEvent(
            id = hierarchicalEvent.id,
            parentId = parentId,
            childIds = childIds,
            name = hierarchicalEvent.name,
            type = hierarchicalEvent.type,
            framework = hierarchicalEvent.framework,
            timestampStartMs = hierarchicalEvent.timestampStartMs,
            timestampEndMs = hierarchicalEvent.timestampEndMs,
            finished = hierarchicalEvent.finished,
            payload = hierarchicalEvent.payload ?: emptyMap()
        )

        events[hierarchicalEvent.id] = traceEvent

        if (parentId == null) {
            rootIds.add(hierarchicalEvent.id)
        }

        // Recursively flatten children
        children.forEach { child ->
            flattenEvent(child, hierarchicalEvent.id)
        }
    }

    // Flatten all root events and their children
    hierarchicalState.rootEvents.forEach { rootEvent ->
        flattenEvent(rootEvent, null)
    }

    return TraceEventsState(
        events = events.toPersistentMap(),
        roots = rootIds.toPersistentSet()
    )
}

// TODO: (@gas) maybe store struct itself, without conversion to map? (not urgent)
fun serializeHierarchicalTraceEventsStateToMap(hstate: HierarchicalTraceEventsState): Map<String, Any> {
    val gson = GsonBuilder().create()
    @Suppress("UNCHECKED_CAST")
    return gson.fromJson(gson.toJsonTree(hstate), Map::class.java) as Map<String, Any>
}

fun deserializeTraceEventsStateFromHierarchicalStateMap(map: Map<String, Any?>): TraceEventsState {
    val gson = GsonBuilder().create()
    val hierarchicalState = gson.fromJson(gson.toJsonTree(map), HierarchicalTraceEventsState::class.java)
    return buildFlatStructure(hierarchicalState)
}