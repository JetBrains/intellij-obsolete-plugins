package com.intellij.aidebugger.python.extensions

import com.intellij.aidebugger.common.AiDebuggerPlugin
import com.intellij.aidebugger.common.services.DebuggerService
import com.intellij.aidebugger.common.services.network.getFreePort
import com.intellij.aidebugger.python.models.PythonTraceSession
import com.intellij.aidebugger.python.models.createPythonTraceSession
import com.intellij.execution.ExecutionResult
import com.intellij.execution.configurations.RunProfile
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessListener
import com.intellij.execution.target.value.constant
import com.intellij.openapi.project.Project
import com.intellij.xdebugger.XDebugSession
import com.jetbrains.python.debugger.PyDebugProcess
import com.jetbrains.python.debugger.PyDebugRunner
import com.jetbrains.python.run.PythonCommandLineState
import com.jetbrains.python.run.PythonExecution
import com.jetbrains.python.run.PythonScriptExecution
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.ServerSocket

class AiDebuggerDebugRunner : PyDebugRunner() {
    companion object {
        val supportedExecutors = setOf("Debug")
    }

    var session: PythonTraceSession? = null

    override fun canRun(executorId: String, profile: RunProfile): Boolean {
        return RunnerCommon.isSupportedExecutor(executorId, profile, supportedExecutors)
                && super.canRun(executorId, profile)
    }

    private fun attach(project: Project, result: ExecutionResult) {
        if (session == null) return

        result.processHandler?.addProcessListener(object : ProcessListener {
            override fun startNotified(event: ProcessEvent) {
                super.startNotified(event)
                DebuggerService.getInstance(project).coroutineScope.launch(Dispatchers.IO) {
                    session?.let { session -> DebuggerService.getInstance(project).startSession(session) }
                }
            }

            override fun processTerminated(event: ProcessEvent) {
                super.processTerminated(event)
                DebuggerService.getInstance(project).coroutineScope.launch(Dispatchers.IO) {
                    session?.stop()
                }
            }
        })
    }

    override fun createDebugProcess(
        session: XDebugSession,
        serverSocket: ServerSocket?,
        result: ExecutionResult?,
        pyState: PythonCommandLineState?
    ): PyDebugProcess {
        result?.let { result -> attach(session.project, result) }
        return super.createDebugProcess(session, serverSocket, result, pyState)
    }

    override fun createDebugProcess(
        session: XDebugSession,
        serverPort: Int,
        result: ExecutionResult?
    ): PyDebugProcess {
        result?.let { result -> attach(session.project, result) }
        return super.createDebugProcess(session, serverPort, result)
    }

    @Suppress("UnstableApiUsage")
    override fun configureDebugParameters(
        project: Project,
        pyState: PythonCommandLineState,
        debuggerScript: PythonExecution,
        debuggerScriptInServerMode: Boolean
    ) {
        if (!RunnerCommon.validateConfiguration(project)) {
            super.configureDebugParameters(project, pyState, debuggerScript, debuggerScriptInServerMode)
            return
        }

        // Create Debugger session
        val expectedPort = getFreePort()
        session = createPythonTraceSession(port = expectedPort, project = project)

        if (debuggerScript is PythonScriptExecution) {
            val originalScriptPath = debuggerScript.pythonScriptPath
            val originalParams = debuggerScript.parameters.toList()

            debuggerScript.parameters.clear()
            debuggerScript.pythonScriptPath = constant(AiDebuggerPlugin.aiDebuggerPath)

            debuggerScript.addAiDebuggerClientParameters(expectedPort)

            originalScriptPath?.let {
                debuggerScript.addAiDebuggerScriptParameters(it)
            }

            debuggerScript.addAiDebuggerRemainingParameters(parameters = originalParams)
        }

        super.configureDebugParameters(
            project,
            pyState,
            debuggerScript,
            debuggerScriptInServerMode
        )
    }
}