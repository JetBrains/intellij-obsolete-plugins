package com.intellij.aidebugger.common.models

import com.intellij.aidebugger.common.models.entities.EventType
import com.intellij.aidebugger.common.models.entities.TraceEvent
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.Serializable

@Serializable
data class TraceEventsState(
    val events: PersistentMap<String, TraceEvent> = persistentMapOf(),
    val roots: PersistentSet<String> = persistentSetOf()
) {
    fun preOrder(startId: String): Sequence<TraceEvent> = sequence {
        yield(events[startId] ?: return@sequence)
        for (childId in events[startId]?.childIds ?: return@sequence) {
            yieldAll(preOrder(childId))
        }
    }
}

@Serializable
data class RequirementsNotMetInfo(
    val isNotMet: Boolean = false,
    val message: String? = null
)

interface SessionRepository {
    val finished: StateFlow<Boolean>
    val requirementsNotMet: StateFlow<RequirementsNotMetInfo>

    /**
     * Report that runtime requirements are not met and optionally provide a custom message
     * that should be shown to the user in the TraceEventsSessionView.
     */
    fun reportRequirementsNotMet(text: String? = null)
}

interface TraceEventsRepository : SessionRepository {
    val state: StateFlow<TraceEventsState>
}

abstract class TraceEventsRepositoryBase : TraceEventsRepository {
    @Suppress("PropertyName")
    protected val _state = MutableStateFlow(TraceEventsState())
    override val state: StateFlow<TraceEventsState> = _state

    @Suppress("PropertyName")
    protected val _finished = MutableStateFlow(false)
    override val finished: StateFlow<Boolean> = _finished

    @Suppress("PropertyName")
    protected val _requirementsNotMetInfo = MutableStateFlow(RequirementsNotMetInfo())
    override val requirementsNotMet: StateFlow<RequirementsNotMetInfo> = _requirementsNotMetInfo

    override fun reportRequirementsNotMet(text: String?) {
        _requirementsNotMetInfo.value = RequirementsNotMetInfo(isNotMet = true, message = text)
    }

    /**
     * Adds a trace event to the current state, updating the state to reflect the new event.
     * If the event is a top-level event (i.e., it has no parent), it is added as a root.
     * Otherwise, it is added as a child to its specified parent event.
     * Modifies the `_state` field by updating the events and roots as appropriate.
     *
     * @param traceEvent The TraceEvent object to be added to the state. This object contains
     *        information such as its ID, parent ID, child IDs, and other event-specific properties.
     */
    fun addEventToState(traceEvent: TraceEvent) {

        // the root events should always have the Group type, and only the top-most of them has parentId == null
        // but just in case we failed to detect parentId correctly, we check it here as set it as a root event as well
        // TODO remove this check, we should be certain enough that parentId is not null here. Anyway we know the connection by traceId!
        if (traceEvent.type == EventType.Group || traceEvent.parentId == null) {
            // Top level event
            _state.update { oldState ->
                oldState.copy(
                    events = oldState.events.put(traceEvent.id, traceEvent),
                    roots = oldState.roots.add(traceEvent.id)
                )
            }

            return
        }



        _state.update { oldState ->
            // Event has a parent
            val parentEvent = oldState.events[traceEvent.parentId] ?: return

            oldState.copy(
                events = oldState.events
                    .put(traceEvent.id, traceEvent)
                    .put(traceEvent.parentId,
                        parentEvent.copy(
                            childIds = parentEvent.childIds.add(traceEvent.id),
                        )
                    )
            )
        }
    }

    /**
     * Updates an existing trace event in the current state by applying a transformation function.
     * This update may also modify parent event relationships if applicable.
     *
     * @param eventId The unique identifier of the trace event to be updated.
     * @param update A lambda function that takes the current state of the target `TraceEvent`
     *               and returns an updated version of it.
     */
    fun updateEventInState(
        eventId: String,
        update: (TraceEvent) -> TraceEvent
    ) {
        val traceEvent = _state.value.events[eventId] ?: error("Event with id $eventId not found")
        val parentEvent = traceEvent.parentId?.let { parentId -> _state.value.events[parentId] }

        // This is the final event that collects aggregated data from both start and finish events
        // It will replace the state event by provided 'id'.
        val updatedEvent = update(traceEvent)

        _state.update { oldState ->
            val events = oldState.events.put(updatedEvent.id, updatedEvent)
            parentEvent?.let { parentEvent ->
                events.put(
                    parentEvent.id,
                    parentEvent.copy(childIds = parentEvent.childIds.add(traceEvent.id))
                )
            }

            oldState.copy(events = events)
        }
    }

    /**
     * Creates or updates a trace event in the current state. If an event with the given `eventId`
     * exists, the `onUpdate` function is applied to the event to produce an updated version.
     * Otherwise, the `onCreate` function is invoked to generate a new trace event. The resulting
     * trace event is then added to the state, and if a `parentId` is provided, the event is
     * associated with the specified parent event.
     *
     * @param eventId The unique identifier of the trace event to be created or updated.
     * @param parentId The unique identifier of the parent event. If null, the trace event is treated as a top-level event.
     * @param onCreate A lambda function that generates a new `TraceEvent` object when an event with the specified `eventId` does not exist.
     * @param onUpdate A lambda function that takes the existing `TraceEvent` as input and returns an updated version of it.
     */
    fun createOrUpdateEvent(
        eventId: String,
        parentId: String? = null,
        onCreate: () -> TraceEvent,
        onUpdate: (TraceEvent) -> TraceEvent
    ) {
        val traceEvent = _state.value.events[eventId]
            ?.let { traceEvent -> onUpdate(traceEvent) }
            ?: onCreate()

        putEventIntoState(
            parentId = parentId,
            traceEvent = traceEvent
        )
    }

    /**
     * Inserts a trace event into the current state, updating the state to reflect the new event.
     * If the `parentId` is null, the event is treated as a top-level event and added to the roots.
     * Otherwise, the event is associated as a child of the specified parent event.
     *
     * @param parentId The unique identifier of the parent event. If null, the event is treated as a top-level event.
     * @param traceEvent The `TraceEvent` object to be added to the state. This includes the event's details such as its ID, parent ID, and child IDs.
     */
    fun putEventIntoState(parentId: String?, traceEvent: TraceEvent) {
        // Top level event
        if (traceEvent.type == EventType.Group || parentId == null) {
            _state.update { oldState ->
                oldState.copy(
                    events = oldState.events.put(traceEvent.id, traceEvent),
                    roots = oldState.roots.add(traceEvent.id)
                )
            }
            return
        }

        // Event has a parent
        _state.update { oldState ->
            val parentEvent = oldState.events[parentId] ?: return

            oldState.copy(
                events = oldState.events
                    .put(traceEvent.id, traceEvent)
                    .put(parentId,
                        parentEvent.copy(
                            childIds = parentEvent.childIds.add(traceEvent.id),
                        )
                    )
            )
        }
    }

    /**
     * Recursively retrieves the root event ID for the given event ID by traversing its parent hierarchy.
     *
     * @param id The unique identifier of the event whose root event ID is to be found.
     * @return The root event ID associated with the given event ID, or the given ID itself if it has no parent.
     */
    fun getRootId(id: String): String {
        val parentId = _state.value.events[id]?.parentId ?: return id
        return getRootId(parentId)
    }

    // TODO better description
    /**
     * Recursively retrieves the group root event ID for the given event ID by traversing its parent hierarchy.
     *
     * @param id The unique identifier of the event whose root event ID is to be found.
     * @return The root event ID associated with the given event ID, or the given ID itself if it has no parent.
     */
    fun getGroupRootId(id: String): String {
        if (_state.value.events[id]?.type == EventType.Group) return id
        val parentId = _state.value.events[id]?.parentId ?: return id
        return getGroupRootId(parentId)
    }
}