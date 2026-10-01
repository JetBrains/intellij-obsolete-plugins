package com.intellij.aidebugger.common.models

import com.intellij.aidebugger.common.viewModels.SessionVM
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project

interface DebuggerSession: Disposable {

    val sessionId: String

    val repository: SessionRepository

    val vm: SessionVM

    val sessionCounters: SessionCounters

    suspend fun start(project: Project)

    suspend fun stop()
}