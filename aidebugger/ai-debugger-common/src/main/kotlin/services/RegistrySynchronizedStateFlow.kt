package com.intellij.aidebugger.common.services

import com.intellij.openapi.util.registry.Registry
import com.intellij.openapi.util.registry.RegistryValue
import com.intellij.openapi.util.registry.RegistryValueListener
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.atomic.AtomicBoolean

/**
 * A MutableStateFlow that synchronizes its value with a Registry flag.
 * When the flow's value changes, the Registry flag is updated, and vice versa.
 */
@Suppress("OPT_IN_TO_INHERITANCE")
class RegistrySynchronizedStateFlow(
    private val registryKey: String
) : MutableStateFlow<Boolean> {

    companion object {
        // Use AtomicBoolean for CAS
        private val listenerRegistered = AtomicBoolean(false)

        // Shared listener that dispatches to all instances
        private val sharedListener = object : RegistryValueListener {
            override fun afterValueChanged(value: RegistryValue) {
                synchronized(instances) {
                    instances[value.key]?.forEach { flow ->
                        flow.onRegistryChanged()
                    }
                }
            }
        }

        // Map of registry key to all flows watching that key
        private val instances = mutableMapOf<String, MutableList<RegistrySynchronizedStateFlow>>()

        private fun registerInstance(key: String, flow: RegistrySynchronizedStateFlow) {
            synchronized(instances) {
                instances.getOrPut(key) { mutableListOf() }.add(flow)

                // Register the shared listener only once for all instances
                if (listenerRegistered.compareAndSet(false, true)) {
                    Registry.setValueChangeListener(sharedListener)
                }
            }
        }
    }

    private val _stateFlow by lazy {
        MutableStateFlow(Registry.`is`(registryKey))
    }

    init {
        registerInstance(registryKey, this)
    }

    private fun onRegistryChanged() {
        _stateFlow.value = Registry.`is`(registryKey)
    }

    override val replayCache: List<Boolean>
        get() = _stateFlow.replayCache

    override val subscriptionCount: StateFlow<Int>
        get() = _stateFlow.subscriptionCount

    override var value: Boolean
        get() = _stateFlow.value
        set(value) {
            Registry.get(registryKey).setValue(value)
            _stateFlow.value = value
        }

    override fun compareAndSet(expect: Boolean, update: Boolean): Boolean {
        if (_stateFlow.compareAndSet(expect, update)) {
            Registry.get(registryKey).setValue(update)
            return true
        }
        return false
    }

    override suspend fun emit(value: Boolean) {
        Registry.get(registryKey).setValue(value)
        _stateFlow.emit(value)
    }

    override fun tryEmit(value: Boolean): Boolean {
        Registry.get(registryKey).setValue(value)
        return _stateFlow.tryEmit(value)
    }

    override suspend fun collect(collector: FlowCollector<Boolean>): Nothing {
        _stateFlow.collect(collector)
    }

    @ExperimentalCoroutinesApi
    override fun resetReplayCache() {
        _stateFlow.resetReplayCache()
    }
}