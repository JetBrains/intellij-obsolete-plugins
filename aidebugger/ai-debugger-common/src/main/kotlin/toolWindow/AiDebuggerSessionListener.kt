package com.intellij.aidebugger.common.toolWindow

import com.intellij.aidebugger.common.services.DebuggerService
import com.intellij.aidebugger.common.services.GlobalSettingsService
import com.intellij.aidebugger.common.services.ProjectSettingsService
import com.intellij.openapi.application.EDT
import com.intellij.openapi.project.Project
import com.intellij.xdebugger.XDebugProcess
import com.intellij.xdebugger.XDebugSession
import com.intellij.xdebugger.XDebuggerManagerListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Adds the "AI Agents Tracer" tab to every debug session in projects
 * where AI libraries have been detected and the debugger is enabled.
 * Fired by [com.intellij.xdebugger.XDebuggerManager.TOPIC] after the debug process is created.
 * The session tab UI may not be initialized yet, so we poll until it becomes available.
 */
class AiDebuggerSessionListener(private val project: Project) : XDebuggerManagerListener {

    override fun processStarted(debugProcess: XDebugProcess) {
        if (!GlobalSettingsService.getInstance().isDebuggerEnabled.value) return
        if (!ProjectSettingsService.getInstance(project).isAiProject) return

        DebuggerService.getInstance(project).coroutineScope.launch(Dispatchers.EDT) {
            val ui = awaitSessionUi(debugProcess.session)
            if (ui != null) {
                TracerTabUtil.addTracerTab(project, ui)
            }
        }
    }

    private suspend fun awaitSessionUi(session: XDebugSession): com.intellij.execution.ui.RunnerLayoutUi? {
        repeat(50) {
            val ui = session.ui
            if (ui != null) return ui
            delay(100)
        }
        return null
    }
}
