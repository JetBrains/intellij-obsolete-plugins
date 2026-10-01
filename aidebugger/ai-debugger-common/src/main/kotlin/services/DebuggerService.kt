package com.intellij.aidebugger.common.services

import com.intellij.aidebugger.common.models.DebuggerSession
import com.intellij.aidebugger.common.viewModels.SessionThreadVM
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.debug
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

@Service(Service.Level.PROJECT)
class DebuggerService(
    val project: Project,
    val coroutineScope: CoroutineScope
) {

    companion object {
        private val logger = thisLogger()

        fun getInstance(project: Project): DebuggerService = project.service()
    }

    private val _sessions: MutableStateFlow<List<DebuggerSession>> =
        MutableStateFlow(emptyList())

    private val _currentSession: MutableStateFlow<DebuggerSession?> =
        MutableStateFlow(null)

    val sessions: StateFlow<List<DebuggerSession>> =
        _sessions

    val currentSession: StateFlow<DebuggerSession?> =
        _currentSession

    // Accumulated threads across all sessions
    private val _accumulatedThreads =
        MutableStateFlow<Map<String, SessionThreadVM>>(emptyMap())

    val accumulatedThreads: StateFlow<Map<String, SessionThreadVM>> =
        _accumulatedThreads

    // Map threadId to the session that owns it
    private val _threadToSession =
        MutableStateFlow<Map<String, DebuggerSession>>(emptyMap())

    val threadToSession: StateFlow<Map<String, DebuggerSession>> =
        _threadToSession

    fun addThread(thread: SessionThreadVM, session: DebuggerSession) {
        _accumulatedThreads.update { current ->
            current + (thread.threadId to thread)
        }
        _threadToSession.update { current ->
            current + (thread.threadId to session)
        }
    }

    suspend fun startSession(session: DebuggerSession) {
        logger.debug { "Debugger Service. Start AI Debugger session: ${session.sessionId}" }

        session.start(project)
        _sessions.update { it + session }

        // Set as the current active session (to switch to the active session in dropdown)
        _currentSession.update { session }
    }

    suspend fun stopSession(session: DebuggerSession) {
        logger.debug { "Debugger Service. Stopping AI Debugger session: ${session.sessionId}" }

        // Stop the session but do not dispose (required for displaying accumulated threads)
        session.stop()

        _sessions.update { it.filter { it.sessionId != session.sessionId } }

        // Keep the session as current (don't set to null) so accumulated threads remain visible
        // Only switch if there is s a newer active session
        _currentSession.update { current ->
            if (current?.sessionId == session.sessionId) {
                _sessions.value.lastOrNull() ?: current
            } else {
                current
            }
        }
    }

    suspend fun stopCurrentSession() {
        _currentSession.update { old ->
            logger.debug { "Debugger Service. Stopping current AI Debugger session" }
            old?.stop()

            // Remove from active sessions
            old?.let { sessionToRemove ->
                _sessions.update { currentSessions ->
                    currentSessions.filter { it.sessionId != sessionToRemove.sessionId }
                }
            }

            // Keep the current session if no other sessions exist
            _sessions.value.lastOrNull() ?: old
        }
    }
}
