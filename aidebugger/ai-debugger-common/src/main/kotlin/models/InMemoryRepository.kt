package com.intellij.aidebugger.common.models

class InMemoryRepository(
    state: TraceEventsState
): TraceEventsRepositoryBase() {
    init {
        _state.value = state
    }
}