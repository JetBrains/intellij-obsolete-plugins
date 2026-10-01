package com.intellij.aidebugger.common.services

import com.intellij.aidebugger.common.AiDebuggerCollector
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@Service(Service.Level.APP)
class GlobalSettingsService {
    companion object {
        private const val DEBUGGER_ENABLED_FLAG = "aitoolkit.aidebugger.enabled"
        private const val DEBUGGER_ALWAYS_SHOW_ON_RUN_FLAG = "aitoolkit.aidebugger.showOnRun"
        private const val DEBUGGER_SHOW_EXTRA_DEBUG_BUTTON = "aitoolkit.aidebugger.extraDebugButton"
        private const val EVALUATION_ENABLED_FLAG = "aitoolkit.aidebugger.evaluation.enabled"
        private const val DEBUGGING_FUNCTIONALITY = "aitoolkit.aidebugger.debugging_functionality"

        fun getInstance(): GlobalSettingsService = service()
    }

    private val _isDebuggerEnabled: MutableStateFlow<Boolean> = RegistrySynchronizedStateFlow(
        registryKey = DEBUGGER_ENABLED_FLAG
    )

    private val _showDebuggerOnRun: MutableStateFlow<Boolean> = RegistrySynchronizedStateFlow(
        registryKey = DEBUGGER_ALWAYS_SHOW_ON_RUN_FLAG
    )

    private val _isEvaluationEnabled: MutableStateFlow<Boolean> = RegistrySynchronizedStateFlow(
        registryKey = EVALUATION_ENABLED_FLAG
    )

    private val _debuggingFunctionality: MutableStateFlow<Boolean> = RegistrySynchronizedStateFlow(
        registryKey = DEBUGGING_FUNCTIONALITY
    )

    private val _showExtraDebugButton: MutableStateFlow<Boolean> = RegistrySynchronizedStateFlow(
        registryKey = DEBUGGER_SHOW_EXTRA_DEBUG_BUTTON
    )

    val isDebuggerEnabled: StateFlow<Boolean> = _isDebuggerEnabled
    val showDebuggerOnRun: StateFlow<Boolean> = _showDebuggerOnRun
    val isEvaluationEnabled: StateFlow<Boolean> = _isEvaluationEnabled
    val debuggingFunctionality: StateFlow<Boolean> = _debuggingFunctionality
    val showExtraDebugButton: StateFlow<Boolean> = _showExtraDebugButton

    fun disableDebugger() {
        _isDebuggerEnabled.value = false
        AiDebuggerCollector.reportAiDebuggerDisable()
    }

    fun enableDebugger() {
        _isDebuggerEnabled.value = true
        AiDebuggerCollector.reportAiDebuggerEnable()
    }

    fun toggleShowDebuggerOnRun() {
        _showDebuggerOnRun.value = !_showDebuggerOnRun.value

        if (!_showDebuggerOnRun.value) {
            AiDebuggerCollector.reportDisableAutoShow()
        }
    }
}

