package com.intellij.aidebugger.common.models

import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

interface SessionCounters {
    val threadsCount: StateFlow<Int>
    val eventsCount: StateFlow<PersistentMap<String, Int>>
    val lastSelectedThread: StateFlow<String?>

    fun reportEvent(rootId: String)
    fun reportLastSelectedThread(threadId: String?)
    fun incrementThreadsCount()
}

class SessionCountersImpl : SessionCounters {
    private val _threadsCount: MutableStateFlow<Int> = MutableStateFlow(0)
    override val threadsCount: StateFlow<Int> = _threadsCount

    private val _eventsCount: MutableStateFlow<PersistentMap<String, Int>> = MutableStateFlow(persistentMapOf())
    override val eventsCount: StateFlow<PersistentMap<String, Int>> = _eventsCount

    private val _lastSelectedThread: MutableStateFlow<String?> = MutableStateFlow(null)
    override val lastSelectedThread: StateFlow<String?> = _lastSelectedThread

    override fun reportEvent(rootId: String) {
        _eventsCount.update { counter ->
            counter.put(rootId, counter.getOrDefault(rootId, 0) + 1)
        }
    }

    override fun reportLastSelectedThread(threadId: String?) {
        _lastSelectedThread.update { threadId }
    }

    override fun incrementThreadsCount() {
        _threadsCount.update { value -> value + 1 }
    }
}