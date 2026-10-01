package com.intellij.aidebugger.python.extensions

import com.intellij.aidebugger.common.AiDebuggerCollector
import com.intellij.aidebugger.common.AiDebuggerPlugin
import com.intellij.aidebugger.common.services.DebuggerService
import com.intellij.aidebugger.common.services.network.getFreePort
import com.intellij.aidebugger.python.models.createPythonTraceSession
import com.intellij.execution.ExecutionManager
import com.intellij.execution.configurations.RunProfile
import com.intellij.execution.configurations.RunnerSettings
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessListener
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.runners.ProgramRunner
import com.intellij.execution.runners.showRunContent
import com.intellij.execution.target.TargetProgressIndicatorAdapter
import com.intellij.execution.target.value.constant
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.progress.EmptyProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.jetbrains.python.run.PythonExecution
import com.jetbrains.python.run.PythonModuleExecution
import com.jetbrains.python.run.PythonRunner
import com.jetbrains.python.run.PythonScriptCommandLineState
import com.jetbrains.python.run.PythonScriptExecution
import com.jetbrains.python.run.PythonScriptTargetedCommandLineBuilder
import com.jetbrains.python.run.target.HelpersAwareTargetEnvironmentRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jetbrains.concurrency.resolvedPromise

class AiDebuggerRunner : ProgramRunner<RunnerSettings> {
    @Suppress("CompanionObjectInExtension")
    companion object {
        val supportedExecutors = setOf("Run")
    }

    override fun getRunnerId(): String = "aidebugger.runner"
    override fun canRun(executorId: String, profile: RunProfile): Boolean {
        return RunnerCommon.isSupportedExecutor(executorId, profile, supportedExecutors)
    }

    @Suppress("UnstableApiUsage")
    override fun execute(environment: ExecutionEnvironment) {
        val state = environment.state as? PythonScriptCommandLineState ?: return
        val project = environment.project

        if (!RunnerCommon.validateConfiguration(project)) {
            PythonRunner().execute(environment)
            return
        }

        AiDebuggerCollector.reportCustomRunnerSelected(project, environment.executor.id)

        // Record the active run configuration name for downstream services (e.g., runners mapping)
        kotlin.runCatching {
            val runConfigName = environment.runProfile.name
            if (runConfigName.isNotBlank()) {
                PropertiesComponent.getInstance(project).setValue("aidebugger.tracer.lastRunConfigName", runConfigName)
            }
        }

        // Start a run profile
        ExecutionManager.getInstance(environment.project).startRunProfile(environment) {
            FileDocumentManager.getInstance().saveAllDocuments()

            // Create Debugger session
            val expectedPort = getFreePort()
            val session = createPythonTraceSession(port = expectedPort, project = project)

            // Start Debugger session
            val builder = object : PythonScriptTargetedCommandLineBuilder {
                override fun build(
                    helpersAwareTargetRequest: HelpersAwareTargetEnvironmentRequest,
                    pythonScript: PythonExecution
                ): PythonExecution = PythonScriptExecution().apply {
                    pythonScriptPath = constant(AiDebuggerPlugin.aiDebuggerPath)
                    addAiDebuggerClientParameters(expectedPort)
                    addPythonScriptAsNamedParameter(pythonScript)

                    val progressIndicator = ProgressManager.getInstance().progressIndicator ?: EmptyProgressIndicator()
                    val environment = environment.targetEnvironmentRequest.prepareEnvironment(
                        TargetProgressIndicatorAdapter(progressIndicator)
                    )

                    addAiDebuggerRemainingParameters(pythonScript.parameters, environment)

                    envs.putAll(pythonScript.envs)
                    workingDir = pythonScript.workingDir
                }
            }

            val result = showRunContent(
                state.execute(environment.executor, builder),
                environment
            )

            result?.processHandler?.addProcessListener(object : ProcessListener {
                override fun startNotified(event: ProcessEvent) {
                    super.startNotified(event)
                    DebuggerService.getInstance(project).coroutineScope.launch(Dispatchers.IO) {
                        DebuggerService.getInstance(project).startSession(session)
                    }
                }

                override fun processTerminated(event: ProcessEvent) {
                    super.processTerminated(event)
                    DebuggerService.getInstance(project).coroutineScope.launch(Dispatchers.IO) {
                        session.stop()
                    }
                }
            })
            resolvedPromise(result)
        }
    }
}


@Suppress("UnstableApiUsage")
fun PythonExecution.addPythonScriptAsNamedParameter(targetScript: PythonExecution) {
    when (targetScript) {
        is PythonScriptExecution -> targetScript.pythonScriptPath?.let { pythonScriptPath ->
            addAiDebuggerScriptParameters(pythonScriptPath)
        } ?: throw IllegalArgumentException("Python script path must be set")

        is PythonModuleExecution -> targetScript.moduleName?.let { moduleName -> addParameters("--module", moduleName) }
            ?: throw IllegalArgumentException("Python module name must be set")

        else -> throw IllegalArgumentException("Unsupported Python script execution type: ${targetScript::class.simpleName}")
    }
}