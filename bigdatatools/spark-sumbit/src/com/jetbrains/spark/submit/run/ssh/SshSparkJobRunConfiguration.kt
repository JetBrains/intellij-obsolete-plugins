package com.jetbrains.spark.submit.run.ssh

import com.intellij.execution.Executor
import com.intellij.execution.configurations.RuntimeConfigurationError
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.project.Project
import com.intellij.ssh.config.unified.SshConfigManager
import com.jetbrains.spark.submit.run.cluster.RemoteTargetId
import com.jetbrains.spark.submit.run.cluster.ssh.SshRemoteTarget
import com.jetbrains.spark.submit.run.ssh.ui.SshSparkSubmitConfigurationEditor
import com.jetbrains.spark.submit.util.SparkMessagesBundle

class SshSparkJobRunConfiguration(
  project: Project,
  val factory: SshSparkSubmitConfigurationFactory,
  name: String
) : SshAwareSparkJobRunConfiguration(project, factory, name) {

  var sshConfigId = ""

  override val remoteTargetId: RemoteTargetId?
    get() = sshConfigId.takeIf { it.isNotBlank() }?.let { SshRemoteTarget.createId(it, "ssh config") }

  override suspend fun getSshConfig() = SshConfigManager.getInstance(project).findConfigById(sshConfigId)

  override fun getConfigurationEditor() =
    SshSparkSubmitConfigurationEditor(project, factory)

  override fun getState(executor: Executor, environment: ExecutionEnvironment) =
    SshAwareSparkRunConfigurationProfileState(SshAwareSparkCommandLineModel(project, this), environment)

  override fun getState() = SshAwareSparkCommandLineModel(project, this)

  override fun checkConfiguration() {
    if (sshConfigId.isBlank()) {
      throw RuntimeConfigurationError(SparkMessagesBundle.message("error.ssh.config"))
    }

    super.checkConfiguration()
  }

}