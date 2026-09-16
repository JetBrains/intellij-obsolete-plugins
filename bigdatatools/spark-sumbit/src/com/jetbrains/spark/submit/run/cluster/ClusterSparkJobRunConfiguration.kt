package com.jetbrains.spark.submit.run.cluster

import com.intellij.execution.Executor
import com.intellij.execution.RunManagerEx
import com.intellij.execution.configurations.RuntimeConfigurationError
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.ssh.config.unified.SshConfig
import com.intellij.util.xmlb.annotations.Transient
import com.jetbrains.bigdatatools.common.rfs.driver.SafeExecutor
import com.jetbrains.spark.submit.model.SelectedArtifactInfo
import com.jetbrains.spark.submit.run.cluster.ui.ClusterSparkSubmitConfigurationEditor
import com.jetbrains.spark.submit.run.ssh.SshAwareSparkJobRunConfiguration
import com.jetbrains.spark.submit.run.ssh.SshAwareSparkRunConfigurationProfileState
import com.jetbrains.spark.submit.run.ssh.SshAwareSparkCommandLineModel
import com.jetbrains.spark.submit.util.SparkMessagesBundle

class ClusterSparkJobRunConfiguration(
  project: Project,
  val factory: ClusterSparkSubmitConfigurationFactory,
  name: String,
  var isTemplate: Boolean = true
) : SshAwareSparkJobRunConfiguration(project, factory, name) {
  override var remoteTargetId: RemoteTargetId? = null
  @get:Transient
  var remoteTargetValid: Boolean? = null

  //JDWP
  var debugDriverEnable: Boolean = true
  var debugDriverPort: Int? = null
  var debugDriverSuspend: Boolean? = null


  @get:Transient
  var selectedArtifactInfo: SelectedArtifactInfo? = null

  override suspend fun getSshConfig(): SshConfig {
    val targetId = remoteTargetId ?: error(SparkMessagesBundle.message("error.target.config"))
    val target = RemoteTargetProvider.getTarget(project, targetId) ?: error(SparkMessagesBundle.message("error.target.config"))
    return target.getOrDefaultInitSshConfig(project)
  }

  override fun getConfigurationEditor(): ClusterSparkSubmitConfigurationEditor {
    val parentDisposable = Disposer.newDisposable()
    val safeExecutor = SafeExecutor.createInstance(parentDisposable, "ClusterSparkSubmitConfigurationEditor")
    val editor = ClusterSparkSubmitConfigurationEditor(project, factory, safeExecutor.coroutineScope)
    Disposer.register(editor, parentDisposable)
    return editor
  }

  override fun getState(executor: Executor, environment: ExecutionEnvironment) =
    SshAwareSparkRunConfigurationProfileState(SshAwareSparkCommandLineModel(project, this), environment)

  override fun getState() = SshAwareSparkCommandLineModel(project, this)

  override fun checkConfiguration() {
    if (remoteTargetId == null) {
      throw RuntimeConfigurationError(SparkMessagesBundle.message("error.target.config"))
    }
    if (remoteTargetValid == false) {
      throw RuntimeConfigurationError(SparkMessagesBundle.message("error.target.config.unresolved"))
    }

    super.checkConfiguration()
  }

  override fun checkSettingsBeforeRun() {
    super.checkSettingsBeforeRun()
    // TODO Remove after testing that task are added correctly from UI
    selectedArtifactInfo?.let { addBeforeRunTasks(it) }
  }

  private fun addBeforeRunTasks(selectedArtifactInfo: SelectedArtifactInfo) {
    val runManager = RunManagerEx.getInstanceEx(project)
    val resultTasks = selectedArtifactInfo.createBeforeTasks(runManager.getBeforeRunTasks(this))
    runManager.setBeforeRunTasks(this, resultTasks)
  }

}