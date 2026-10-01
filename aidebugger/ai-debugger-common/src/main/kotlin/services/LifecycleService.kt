package com.intellij.aidebugger.common.services

import com.intellij.aidebugger.common.views.eventsFeed.getAiDebuggerToolWindowEmptyText
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow

@Service(Service.Level.PROJECT)
class LifecycleService(val coroutineScope: CoroutineScope) {
    companion object {
        fun getInstance(project: Project): LifecycleService = project.service()
    }

    val aiDebuggerEmptyFeedMessage: MutableStateFlow<String> =
        MutableStateFlow(getAiDebuggerToolWindowEmptyText())
}