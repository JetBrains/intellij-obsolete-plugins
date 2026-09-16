package com.jetbrains.spark.submit.run.ssh

import com.intellij.execution.RunManagerEx
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.openapi.project.Project
import com.intellij.ssh.config.unified.SshConfig
import com.jetbrains.spark.submit.model.FilePath
import com.jetbrains.spark.submit.run.cluster.RemoteTargetId
import com.jetbrains.spark.submit.run.common.AbstractSparkJobRunConfiguration
import com.jetbrains.spark.submit.run.ssh.upload.UploadBeforeRunTask
import com.jetbrains.spark.submit.run.ssh.upload.UploadBeforeRunTaskProvider

abstract class SshAwareSparkJobRunConfiguration(
  project: Project,
  factory: ConfigurationFactory,
  name: String
) : AbstractSparkJobRunConfiguration<SshAwareSparkCommandLineModel>(project, factory, name) {

  abstract suspend fun getSshConfig(): SshConfig?
  abstract val remoteTargetId: RemoteTargetId?
  var targetDirectory = FilePath()

  override fun checkSettingsBeforeRun() {
    super.checkSettingsBeforeRun()
    // TODO Remove after testing that task are added correctly from UI
    addUploadBeforeRunTask()
  }

  private fun addUploadBeforeRunTask() {
    val runManager = RunManagerEx.getInstanceEx(project)
    val confTasks = runManager.getBeforeRunTasks(this)
    if (confTasks.any { it.providerId == UploadBeforeRunTaskProvider.ID })
      return
    val newTask = UploadBeforeRunTask()
    val resultTasks = confTasks + newTask
    runManager.setBeforeRunTasks(this, resultTasks)
  }
}