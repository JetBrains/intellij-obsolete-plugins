package com.intellij.bigdatatools.plugin.spark.java.submit

import com.intellij.debugger.impl.attach.JavaDebuggerConsoleFilterProvider
import com.intellij.execution.ExecutionResult
import com.intellij.execution.configurations.RunProfile
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.filters.ConsoleFilterProvider
import com.intellij.execution.impl.ConsoleViewImpl
import com.intellij.execution.impl.ConsoleViewUtil
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.ui.ConsoleView
import com.intellij.execution.ui.RunContentDescriptor
import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.ssh.config.unified.SshConfig
import com.jetbrains.spark.submit.run.cluster.ClusterSparkJobRunConfiguration
import com.jetbrains.spark.submit.run.ssh.SshAwareSparkRunConfigurationProfileState
import com.jetbrains.spark.submit.run.ssh.runner.SshSparkSubmitRunner

open class SshSparkSubmitJavaRunner : SshSparkSubmitRunner() {
  override fun getRunnerId(): String = "SshSparkSubmitJavaRunner"
  override fun canRun(executorId: String, profile: RunProfile): Boolean {
    return profile is ClusterSparkJobRunConfiguration
           && !profile.factory.isPySpark
           && executorId == DefaultRunExecutor.EXECUTOR_ID
  }

  override suspend fun myShowRunContent(environment: ExecutionEnvironment,
                                        executionResult: ExecutionResult,
                                        state: SshAwareSparkRunConfigurationProfileState): RunContentDescriptor? {
    val configuration = state.configuration as ClusterSparkJobRunConfiguration
    val sshConfig: SshConfig = configuration.getSshConfig()
    val consoleView = executionResult.executionConsole as ConsoleView
    consoleView.addMessageFilter(SshJavaDebuggerConsoleFilter(sshConfig, executionResult.processHandler))
    return super.myShowRunContent(environment, executionResult, state)
  }

  override fun createConsoleView(project: Project): ConsoleView {
    val searchScope = GlobalSearchScope.allScope(project)
    return ConsoleViewImpl(project, searchScope, true, false).also { consoleView ->
      for (eachProvider in ConsoleFilterProvider.FILTER_PROVIDERS.extensionList) {
        if (eachProvider !is JavaDebuggerConsoleFilterProvider) {
          for (filter in ConsoleViewUtil.computeConsoleFilters(eachProvider, project, consoleView, searchScope)) {
            consoleView.addMessageFilter(filter)
          }
        }
      }
    }
  }
}