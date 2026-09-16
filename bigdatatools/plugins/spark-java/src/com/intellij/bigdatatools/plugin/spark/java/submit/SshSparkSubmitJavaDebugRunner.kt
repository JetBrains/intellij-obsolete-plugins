package com.intellij.bigdatatools.plugin.spark.java.submit

import com.intellij.debugger.DebugEnvironment
import com.intellij.debugger.DebuggerManager
import com.intellij.debugger.DebuggerManagerEx
import com.intellij.debugger.engine.DebugProcess
import com.intellij.debugger.engine.DebugProcessListener
import com.intellij.debugger.engine.JavaDebugProcess
import com.intellij.execution.ExecutionResult
import com.intellij.execution.configurations.RemoteConnection
import com.intellij.execution.configurations.RunProfile
import com.intellij.execution.executors.DefaultDebugExecutor
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessListener
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.ui.ConsoleView
import com.intellij.execution.ui.RunContentDescriptor
import com.intellij.openapi.application.EDT
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.Key
import com.intellij.psi.search.ExecutionSearchScopes
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.ssh.config.unified.SshConfig
import com.intellij.util.net.NetUtils
import com.intellij.xdebugger.XDebugProcess
import com.intellij.xdebugger.XDebugProcessStarter
import com.intellij.xdebugger.XDebugSession
import com.intellij.xdebugger.XDebuggerManager
import com.jetbrains.bigdatatools.common.connection.tunnel.BdtSshTunnelService
import com.jetbrains.bigdatatools.common.connection.tunnel.model.ConnectionSshTunnelInfo
import com.jetbrains.spark.submit.run.cluster.ClusterSparkJobRunConfiguration
import com.jetbrains.spark.submit.run.ssh.SshAwareSparkRunConfigurationProfileState
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.seconds

class SshSparkSubmitJavaDebugRunner : SshSparkSubmitJavaRunner() {

  override fun getRunnerId(): String = SSH_SPARK_SUBMIT_RUNNER_DEBUG

  override fun canRun(executorId: String, profile: RunProfile): Boolean {
    return profile is ClusterSparkJobRunConfiguration
           && !profile.factory.isPySpark
           && executorId == DefaultDebugExecutor.EXECUTOR_ID
           && profile.debugSupported()
           && profile.debugDriverEnable
  }

  override suspend fun myShowRunContent(environment: ExecutionEnvironment,
                                        executionResult: ExecutionResult,
                                        state: SshAwareSparkRunConfigurationProfileState): RunContentDescriptor? {
    val configuration = state.myProfileState.configuration as ClusterSparkJobRunConfiguration
    val sshConfig: SshConfig = configuration.getSshConfig()
    val listeningPortDeferred = CompletableDeferred<Int>()
    executionResult.processHandler.addProcessListener(object : ProcessListener {
      override fun onTextAvailable(event: ProcessEvent, outputType: Key<*>) {
        val port = SshJavaDebuggerConsoleFilter.getListeningSocketPort(event.text)
        if (port != null) {
          executionResult.processHandler.removeProcessListener(this)
          listeningPortDeferred.complete(port)
        }
      }
    })
    val processHandler = executionResult.processHandler
    if (!processHandler.isStartNotified) {
      processHandler.startNotify()
    }
    val listeningPort = withTimeoutOrNull(3.seconds) {
      listeningPortDeferred.await()
    }
    val consoleView = executionResult.executionConsole as ConsoleView
    if (listeningPort == null) {
      consoleView.addMessageFilter(SshJavaDebuggerConsoleFilter(sshConfig, executionResult.processHandler))
      return super.myShowRunContent(environment, executionResult, state)
    }
    val remoteConnection = withContext(Dispatchers.IO) {
      createDriverTunnel(executionResult.processHandler, environment.project, configuration, listeningPort, sshConfig)
    }
    return withContext(Dispatchers.EDT) {
      attachDebugger(executionResult, environment, remoteConnection)
    }
  }

  private fun createDriverTunnel(processHandler: ProcessHandler,
                                 project: Project,
                                 configuration: ClusterSparkJobRunConfiguration,
                                 listeningPort: Int,
                                 sshConfig: SshConfig): RemoteConnection {
    val tunnelInfo = ConnectionSshTunnelInfo(sshConfig = sshConfig,
                                             remoteHost = NetUtils.getLocalHostString(),
                                             remotePort = listeningPort)
    return createTunnel(project, configuration.name, tunnelInfo, processHandler)
  }

  private fun createTunnel(project: Project, configurationName: String, tunnelInfo: ConnectionSshTunnelInfo, processHandler: ProcessHandler): RemoteConnection {
    val tunnelHandler = BdtSshTunnelService.createIfRequiredInternal(project, tunnelInfo, configurationName, false)
    checkNotNull(tunnelHandler)
    DebuggerManager.getInstance(project).addDebugProcessListener(processHandler, object : DebugProcessListener {
      override fun processDetached(debugProcess: DebugProcess, closedByUser: Boolean) {
        Disposer.dispose(tunnelHandler)
      }
    })
    return RemoteConnection(true, NetUtils.getLocalHostString(), tunnelHandler.localPort.toString(), false)
  }

  private fun attachDebugger(executionResult: ExecutionResult,
                             environment: ExecutionEnvironment,
                             remoteConnection: RemoteConnection): RunContentDescriptor? {
    val project = environment.project
    val searchScope = ExecutionSearchScopes.executionScope(project, environment.runProfile)
    val debugEnvironment = object : DebugEnvironment {
      override fun createExecutionResult(): ExecutionResult = executionResult
      override fun getSearchScope(): GlobalSearchScope = searchScope
      override fun isRemote(): Boolean = true
      override fun getRemoteConnection(): RemoteConnection = remoteConnection
      override fun getPollTimeout(): Long = DebugEnvironment.LOCAL_START_TIMEOUT.toLong()
      override fun getSessionName(): String = environment.runProfile.name
    }
    val debuggerSession = DebuggerManagerEx.getInstanceEx(project).attachVirtualMachine(debugEnvironment) ?: return null
    val starter = object : XDebugProcessStarter() {
      override fun start(session: XDebugSession): XDebugProcess {
        return JavaDebugProcess.create(session, debuggerSession)
      }
    }
    return XDebuggerManager.getInstance(project).newSessionBuilder(starter)
      .environment(environment)
      .startSession().runContentDescriptor
  }

  companion object {
    const val SSH_SPARK_SUBMIT_RUNNER_DEBUG = "SshSparkSubmitRunnerDebug"
  }
}