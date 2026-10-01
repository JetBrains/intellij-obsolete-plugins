package com.intellij.aidebugger.evaluation.onboarding

import androidx.compose.ui.geometry.Rect
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import java.util.concurrent.ConcurrentHashMap

object OnboardingAnchorRegistry {
    private val map = ConcurrentHashMap<String, Rect>()
    private val providers = ConcurrentHashMap<String, () -> Rect?>()
    private val flags = ConcurrentHashMap<String, Boolean>()
    private val flagFlows = ConcurrentHashMap<String, MutableSharedFlow<Boolean>>( /* … */ )

    fun set(key: String, rect: Rect) {
        map[key] = rect
    }

    fun setProvider(key: String, provider: () -> Rect?) {
        providers[key] = provider
    }

    fun get(key: String): Rect? {
        val provider = providers[key]
        return if (provider != null) {
            try {
                val rect = provider.invoke()
                if (rect == null) {
                    val snapshot = map[key]
                    snapshot
                } else {
                    rect
                }
            } catch (t: Throwable) {
                val snapshot = map[key]
                snapshot
            }
        } else map[key]
    }

    fun remove(key: String) {
        map.remove(key)
        providers.remove(key)
    }

    fun setFlag(key: String, isActive: Boolean) {
        val prev = flags.put(key, isActive)
        if (prev == null || prev != isActive) {
            flagFlows.getOrPut(key) { MutableSharedFlow(replay = 1) }.tryEmit(isActive)
        }
    }

    fun observeFlag(key: String): Flow<Boolean> =
        flagFlows.getOrPut(key) { MutableSharedFlow(replay = 1) }.also { flow ->
            flags[key]?.let { flow.tryEmit(it) }
        }

    fun getFlag(key: String): Boolean? = flags[key]

    fun removeFlag(key: String) {
        flags.remove(key)
        flagFlows.remove(key)
    }
}
