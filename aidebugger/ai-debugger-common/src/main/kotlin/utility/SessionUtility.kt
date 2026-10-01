package com.intellij.aidebugger.common.utility

import com.intellij.aidebugger.common.models.TraceEventsState
import com.intellij.aidebugger.common.models.createInMemorySession
import com.intellij.aidebugger.common.models.entities.EventType
import com.intellij.aidebugger.common.services.DebuggerService
import com.intellij.aidebugger.common.toolWindow.openAiDebuggerToolWindow
import com.intellij.aidebugger.common.viewModels.TraceEventsSessionVM
import com.intellij.openapi.project.Project
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

fun openTraceToolWindowWithState(project: Project, state: TraceEventsState) {
    val session = createInMemorySession(project = project, state = state)
    val nonPromptThreadId = state.roots.firstOrNull { rootId ->
        val event = state.events[rootId]
        event != null && (event.type != EventType.General && event.name != "PromptTemplate")
    }
    DebuggerService.getInstance(project).coroutineScope.launch(Dispatchers.IO) {
        DebuggerService.getInstance(project).startSession(session)
        openAiDebuggerToolWindow(project)
        if (!nonPromptThreadId.isNullOrEmpty()) {
            (session.vm as? TraceEventsSessionVM)?.setSelectedThread(nonPromptThreadId)
        }
    }
}