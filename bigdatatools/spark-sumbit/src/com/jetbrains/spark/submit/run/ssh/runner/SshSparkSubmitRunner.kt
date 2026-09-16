package com.jetbrains.spark.submit.run.ssh.runner

import com.intellij.execution.ExecutionManager
import com.intellij.execution.ExecutionResult
import com.intellij.execution.configurations.RunProfile
import com.intellij.execution.configurations.RunnerSettings
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.impl.ConsoleViewImpl
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.runners.ProgramRunner
import com.intellij.execution.runners.showRunContent
import com.intellij.execution.ui.ConsoleView
import com.intellij.execution.ui.RunContentDescriptor
import com.intellij.openapi.application.EDT
import com.intellij.openapi.progress.runBlockingMaybeCancellable
import com.intellij.openapi.project.Project
import com.jetbrains.spark.submit.run.cluster.ClusterSparkJobRunConfiguration
import com.jetbrains.spark.submit.run.ssh.SshAwareSparkJobRunConfiguration
import com.jetbrains.spark.submit.run.ssh.SshAwareSparkRunConfigurationProfileState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.concurrency.runAsync

open class SshSparkSubmitRunner : ProgramRunner<RunnerSettings> {
  override fun getRunnerId(): String = SSH_SPARK_SUBMIT_RUNNER

  override fun canRun(executorId: String, profile: RunProfile): Boolean {
    return profile is SshAwareSparkJobRunConfiguration
           && (profile !is ClusterSparkJobRunConfiguration || profile.factory.isPySpark)
           && executorId == DefaultRunExecutor.EXECUTOR_ID
  }

  override fun execute(environment: ExecutionEnvironment) {
    val state = environment.state as? SshAwareSparkRunConfigurationProfileState ?: return
    ExecutionManager.getInstance(environment.project).startRunProfile(environment) {
      runAsync {
        runBlockingMaybeCancellable {
          val executionResult = withContext(Dispatchers.IO) {
            state.execute(environment.executor, this@SshSparkSubmitRunner)
          }
          myShowRunContent(environment, executionResult, state)
        }
      }
    }
  }

  protected open suspend fun myShowRunContent(environment: ExecutionEnvironment,
                                              executionResult: ExecutionResult,
                                              state: SshAwareSparkRunConfigurationProfileState): RunContentDescriptor? {
    return withContext(Dispatchers.EDT) {
      showRunContent(executionResult, environment)
    }
  }

  open fun createConsoleView(project: Project): ConsoleView =
    ConsoleViewImpl(project, true)

  companion object {
    const val SSH_SPARK_SUBMIT_RUNNER = "SshSparkSubmitRunner"
  }
}