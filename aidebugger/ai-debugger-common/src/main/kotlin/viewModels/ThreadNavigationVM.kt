package com.intellij.aidebugger.common.viewModels

interface SessionThreadVM {
    val title: String
    val isActive: Boolean
    val threadId: String
}

class SessionThreads(
    val activeThreads: List<SessionThreadVM>,
    val finishedThreads: List<SessionThreadVM>
)

