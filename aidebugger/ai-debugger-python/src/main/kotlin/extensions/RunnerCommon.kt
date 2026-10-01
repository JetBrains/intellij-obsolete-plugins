package com.intellij.aidebugger.python.extensions

import com.intellij.aidebugger.common.AiDebuggerCollector
import com.intellij.aidebugger.common.services.GlobalSettingsService
import com.intellij.aidebugger.common.services.ProjectSettingsService
import com.intellij.aidebugger.common.toolWindow.isAiDebuggerToolWindowVisible
import com.intellij.aidebugger.python.PythonRequirements
import com.intellij.aidebugger.python.utility.PythonVersion
import com.intellij.execution.configurations.RunProfile
import com.intellij.execution.target.TargetEnvironment
import com.intellij.openapi.project.Project
import com.jetbrains.python.run.PythonExecution
import com.jetbrains.python.run.PythonRunConfiguration
import com.jetbrains.python.sdk.PythonSdkUtil
import java.util.function.Function

object RunnerCommon {
    enum class SkipReason {
        NOT_SUPPORTED_EXECUTOR,
        DISABLED,
        RUN_CONFIG_IS_NULL,
        SDK_IS_NULL,
        PYTHON_VERSION_IS_NULL,
        PYTHON_VERSION_IS_INVALID,
        LOW_PYTHON_VERSION,
        REMOTE_INTERPRETER,
    }

    fun isSupportedExecutor(executorId: String, profile: RunProfile, supportedExecutors: Set<String>): Boolean {
        return getSkipReason(executorId, profile, supportedExecutors) == null
    }

    fun getSkipReason(executorId: String, profile: RunProfile, supportedExecutors: Set<String>): SkipReason? {
        if (executorId !in supportedExecutors) {
            return SkipReason.NOT_SUPPORTED_EXECUTOR
        }

        if (!GlobalSettingsService.getInstance().isDebuggerEnabled.value) {
            return SkipReason.DISABLED
        }

        if (profile !is PythonRunConfiguration) return SkipReason.RUN_CONFIG_IS_NULL

        val sdk = profile.sdk ?: return SkipReason.SDK_IS_NULL
        val versionString = sdk.versionString ?: return SkipReason.PYTHON_VERSION_IS_NULL
        val pythonVersion = PythonVersion.parse(versionString) ?: return SkipReason.PYTHON_VERSION_IS_INVALID

        if (pythonVersion < PythonRequirements.minimumPythonVersion) return SkipReason.LOW_PYTHON_VERSION
        if (PythonSdkUtil.isRemote(sdk)) return SkipReason.REMOTE_INTERPRETER

        return null
    }

    fun validateConfiguration(project: Project): Boolean {
        if (!isAiDebuggerToolWindowVisible(project) && !ProjectSettingsService.getInstance(project).isAiProject) {
            AiDebuggerCollector.reportCustomRunnerSkipped(
                project,
                com.intellij.aidebugger.common.SkipReason.NON_AI_PROJECT
            )
            return false
        }

        return true
    }
}

@Suppress("UnstableApiUsage")
fun PythonExecution.addAiDebuggerServerParameters(serverPort: Int) {
    addParameters("--server", serverPort.toString())
}

@Suppress("UnstableApiUsage")
fun PythonExecution.addAiDebuggerClientParameters(serverPort: Int) {
    addParameters("--client", serverPort.toString())
}

@Suppress("UnstableApiUsage")
fun PythonExecution.addAiDebuggerScriptParameters(scriptPath: Function<TargetEnvironment, String>) {
    addParameter("--script")
    addParameter(scriptPath)
}

@Suppress("UnstableApiUsage")
fun PythonExecution.addAiDebuggerRemainingParameters(parameters: List<Function<TargetEnvironment, String>>) {
    addParameter("--")  // separate all remaining arguments
    for (parameter in parameters) {
        addParameter(parameter)
    }
}

@Suppress("UnstableApiUsage")
fun PythonExecution.addAiDebuggerRemainingParameters(parameters: List<Function<TargetEnvironment, String>>, environment: TargetEnvironment) {
    addParameter("--")  // separate all remaining arguments
    for (parameter in parameters) {
        val resolvedParameter = parameter.apply(environment)
        if (resolvedParameter != PythonExecution.SKIP_ARGUMENT) {
            addParameter(resolvedParameter)
        }
    }
}