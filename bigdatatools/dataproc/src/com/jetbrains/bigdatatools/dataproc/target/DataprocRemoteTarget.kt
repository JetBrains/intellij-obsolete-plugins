package com.jetbrains.bigdatatools.dataproc.target

import com.intellij.bigdatatools.coreUi.util.MessagesBundle
import com.intellij.bigdatatools.dataproc.icons.BigdatatoolsDataprocIcons
import com.intellij.execution.process.ProcessHandler
import com.intellij.openapi.application.EDT
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.platform.util.progress.indeterminateStep
import com.intellij.ssh.config.unified.SshConfig
import com.intellij.ssh.ui.unified.SshUiData
import com.intellij.ui.SimpleColoredComponent
import com.intellij.ui.SimpleTextAttributes
import com.intellij.bigdatatools.coreUi.settings.connections.ConnectionData
import com.jetbrains.bigdatatools.common.integration.MonitoringServiceProvider
import com.jetbrains.bigdatatools.dataproc.data.DataprocDataManager
import com.jetbrains.bigdatatools.dataproc.model.DataprocClusterInfo
import com.jetbrains.bigdatatools.dataproc.util.DataprocMessagesBundle
import com.jetbrains.spark.submit.model.ClusterManagerType
import com.jetbrains.spark.submit.model.DeployModeType
import com.jetbrains.spark.submit.run.cluster.RemoteTarget
import com.jetbrains.spark.submit.run.cluster.RemoteTargetId
import com.jetbrains.spark.submit.run.cluster.RemoteTargetType
import com.jetbrains.spark.submit.run.cluster.ui.AttachedSshConfigsUtil
import com.jetbrains.spark.submit.run.cluster.ui.ClusterSparkSubmitConfigurationEditor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.swing.Icon

internal class DataprocRemoteTarget(
  val project: Project,
  private val dataManager: DataprocDataManager,
  private val clusterDetails: DataprocClusterInfo
) : RemoteTarget(
  clusterDetails.cluster.clusterName,
  RemoteTargetId(dataManager.driver.getExternalId(), clusterDetails.cluster.clusterName, clusterDetails.cluster.clusterName)
) {
  override val type: RemoteTargetType = RemoteTargetType.DATAPROC

  override val icon: Icon = BigdatatoolsDataprocIcons.Dataproc

  override fun setupItem(item: SimpleColoredComponent) {
    item.icon = this@DataprocRemoteTarget.icon
    item.append(this@DataprocRemoteTarget.name)
    item.append("  ")
    item.append(DataprocMessagesBundle.message("remote.target.emr.cluster.remark"), SimpleTextAttributes.GRAYED_ITALIC_ATTRIBUTES)
  }

  override fun updateEditorFields(editor: ClusterSparkSubmitConfigurationEditor) {
    super.updateEditorFields(editor)

    editor.setTargetDependentSettings {
      editor.deployModeField.item = DeployModeType.CLUSTER
      editor.clusterManagerField.item = ClusterManagerType.YARN
    }
  }

  override val sshDetailsLinkCaption: String
    get() = clusterDetails.name

  override suspend fun openSshConfigDialog(editor: ClusterSparkSubmitConfigurationEditor): SshConfig? {
    val currentUiData = SshUiData.create(getOrDefaultInitSshConfig(project))
    return withContext(Dispatchers.EDT) {
      val chosenConfig = AttachedSshConfigsUtil.editAttachedSshConfigSetting(project, currentUiData, clusterDetails.name, null) ?: return@withContext null
      return@withContext chosenConfig
    }
  }

  override suspend fun getOrDefaultInitSshConfig(project: Project): SshConfig = withContext(Dispatchers.IO) {
    indeterminateStep(MessagesBundle.message("ssh.state.init")) {
      try {
        dataManager.dependsManager.getOrCreateSshConfig(project, clusterDetails, withModal = false)
      }
      catch (t: Throwable) {
        thisLogger().warn(t)
        null
      }
    }
  }!!

  override fun getOrCreateSparkConnection(): ConnectionData? {
    return dataManager.createSparkConnection(project, clusterDetails)
  }

  override fun addApplicationToMonitoring(processHandler: ProcessHandler, name: String, focusOnApp: Boolean) {
    val sparkService = MonitoringServiceProvider.getSparkMonitoringService() ?: return
    val connData = dataManager.createSparkConnection(project, clusterDetails) ?: return
    sparkService.startApp(project, connData, name, processHandler, focusOnApp)
  }
}