package com.intellij.aidebugger.koog.execution

import ai.koog.agents.core.annotation.ExperimentalAgentsApi
import ai.koog.agents.core.feature.config.FeatureSystemVariables
import ai.koog.agents.core.feature.debugger.Debugger
import ai.koog.agents.core.feature.remote.client.config.DefaultClientConnectionConfig
import com.intellij.aidebugger.common.AiDebuggerCollector
import com.intellij.aidebugger.common.SkipReason
import com.intellij.aidebugger.common.services.DebuggerService
import com.intellij.aidebugger.common.services.GlobalSettingsService
import com.intellij.aidebugger.common.services.ProjectSettingsService
import com.intellij.aidebugger.common.services.network.getFreePort
import com.intellij.aidebugger.common.toolWindow.isAiDebuggerToolWindowVisible
import com.intellij.aidebugger.koog.session.createKoogEventsSession
import com.intellij.execution.Executor
import com.intellij.execution.RunConfigurationExtension
import com.intellij.execution.configurations.JavaParameters
import com.intellij.execution.configurations.RunConfigurationBase
import com.intellij.execution.configurations.RunnerSettings
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessListener
import com.intellij.openapi.diagnostic.debug
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.util.Key
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

private val KOOG_DEBUGGER_PORT_USER_DATA_KEY = Key.create<Int>("koog.debugger.port.user.data")

class KoogRunConfigurationExtension : RunConfigurationExtension() {

    companion object {
        private val logger = thisLogger()
    }

    override fun isApplicableFor(configuration: RunConfigurationBase<*>): Boolean {
        val isKoogLibraryApplicable =
            KoogLibDependencyDetector.getInstance(configuration.project).isIncludeApplicableKoogLibrary()

        // Set a persistent state for a project when project dependencies are detected.
        ProjectSettingsService.getInstance(configuration.project).importsOfInterestPresent = isKoogLibraryApplicable

        return isKoogLibraryApplicable
    }

    override fun <T : RunConfigurationBase<*>?> updateJavaParameters(
        configuration: T & Any,
        params: JavaParameters,
        runnerSettings: RunnerSettings?,
        executor: Executor
    ) {
        val port = getFreePort()
        val waitConnectionTimeout = 30.seconds

        // Set Koog Debugger feature request
        @OptIn(ExperimentalAgentsApi::class)
        params.vmParametersList.add("-D${FeatureSystemVariables.KOOG_FEATURES_VM_OPTION_NAME}=${Debugger.key.name}")

        // Get Koog debugger port and connection timeout to update VM options for the process
        @OptIn(ExperimentalAgentsApi::class)
        params.vmParametersList.add("-D${Debugger.KOOG_DEBUGGER_WAIT_CONNECTION_TIMEOUT_MS_VM_OPTION}=${waitConnectionTimeout.inWholeMilliseconds}")

        @OptIn(ExperimentalAgentsApi::class)
        params.vmParametersList.add("-D${Debugger.KOOG_DEBUGGER_PORT_VM_OPTION}=$port")

        // Update configuration user data to re-use the same port later when we attach to a process
        configuration.putUserData(KOOG_DEBUGGER_PORT_USER_DATA_KEY, port)

        super.updateJavaParameters(configuration, params, runnerSettings, executor)
    }

    override fun <T : RunConfigurationBase<*>?> updateJavaParameters(
        config: T & Any,
        params: JavaParameters,
        settings: RunnerSettings?
    ) { }

    override fun attachToProcess(
        configuration: RunConfigurationBase<*>,
        handler: ProcessHandler,
        runnerSettings: RunnerSettings?
    ) {
        val project = configuration.project

        // Check AI Debugger is enabled
        if (!GlobalSettingsService.getInstance().isDebuggerEnabled.value) {
            return
        }

        // Fetch port value from VM options if it was added when starting a configuration.
        // Otherwise, use the default Koog agent server port.
        val koogDebuggerPort = configuration.getUserData(KOOG_DEBUGGER_PORT_USER_DATA_KEY)
            ?: DefaultClientConnectionConfig.DEFAULT_PORT

        // Tool Window check
        if (!isAiDebuggerToolWindowVisible(project) && !ProjectSettingsService.getInstance(project).isAiProject) {
            logger.debug {
                "Koog Runner. Skipping execution of ${configuration.id} because " +
                        "the configuration run for a project missing Koog library dependency"
            }

            AiDebuggerCollector.reportCustomRunnerSkipped(project, SkipReason.NON_AI_PROJECT)
            return
        }

        val koogSession = createKoogEventsSession(project = project, port = koogDebuggerPort)

        handler.addProcessListener(object : ProcessListener {
            override fun startNotified(event: ProcessEvent) {
                super.startNotified(event)
                DebuggerService.getInstance(project).coroutineScope.launch(Dispatchers.IO) {
                    DebuggerService.getInstance(project).startSession(koogSession)
                }
            }

            override fun processTerminated(event: ProcessEvent) {
                super.processTerminated(event)
                DebuggerService.getInstance(project).coroutineScope.launch(Dispatchers.IO) {
                    // Stop the specific session when the process terminates
                    // This will remove it from the active sessions list but keep the UI state
                    DebuggerService.getInstance(project).stopSession(koogSession)
                }
            }
        })

        super.attachToProcess(configuration, handler, runnerSettings)
    }
}
