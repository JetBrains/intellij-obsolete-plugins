package com.intellij.bigdatatools.emr.target

import com.intellij.bigdatatools.emr.data.EmrDataManager
import com.intellij.bigdatatools.emr.model.EmrClusterDetails
import com.intellij.bigdatatools.emr.model.EmrClusterSummary
import com.intellij.bigdatatools.emr.util.EmrMessagesBundle
import com.intellij.bigdatatools.hdfs.icons.BigdatatoolsHdfsIcons
import com.intellij.execution.process.ProcessHandler
import com.intellij.openapi.project.Project
import com.intellij.ssh.config.unified.SshConfig
import com.intellij.ui.SimpleColoredComponent
import com.intellij.ui.SimpleTextAttributes
import com.intellij.bigdatatools.coreUi.settings.connections.ConnectionData
import com.jetbrains.bigdatatools.common.integration.MonitoringServiceProvider
import com.jetbrains.bigdatatools.common.rfs.driver.Driver
import com.intellij.bigdatatools.coreUi.util.executeOnPooledThread
import com.jetbrains.spark.submit.model.ClusterManagerType
import com.jetbrains.spark.submit.model.DeployModeType
import com.jetbrains.spark.submit.run.cluster.RemoteTarget
import com.jetbrains.spark.submit.run.cluster.RemoteTargetId
import com.jetbrains.spark.submit.run.cluster.RemoteTargetType
import com.jetbrains.spark.submit.run.cluster.ui.ClusterSparkSubmitConfigurationEditor
import software.amazon.awssdk.services.emr.model.ClusterSummary
import javax.swing.Icon

class EmrRemoteTarget(
  val project: Project,
  private val dataManager: EmrDataManager,
  private val clusterDetails: EmrClusterDetails
) : RemoteTarget(clusterDetails.summary.name, createId(dataManager.driver, clusterDetails.summary)) {
  override val type: RemoteTargetType = RemoteTargetType.EMR

  private val sshManager = dataManager.sshManager

  override val icon: Icon = BigdatatoolsHdfsIcons.StorageIcons.AmazonEmr

  override fun setupItem(item: SimpleColoredComponent) {
    item.icon = this@EmrRemoteTarget.icon
    item.append(this@EmrRemoteTarget.name)
    item.append("  ")
    item.append(EmrMessagesBundle.message("remote.target.emr.cluster.remark"), SimpleTextAttributes.GRAYED_ITALIC_ATTRIBUTES)
  }

  fun hasSpark() = clusterDetails.cluster.applications()?.any { it.name() == "Spark" } ?: false

  override fun updateEditorFields(editor: ClusterSparkSubmitConfigurationEditor) {
    super.updateEditorFields(editor)

    editor.setTargetDependentSettings {
      editor.deployModeField.item = DeployModeType.CLUSTER
      editor.clusterManagerField.item = ClusterManagerType.YARN
    }
  }

  override val sshDetailsLinkCaption: String
    get() = sshManager.getOrCreateSshConfig(project, clusterDetails).name

  override suspend fun openSshConfigDialog(editor: ClusterSparkSubmitConfigurationEditor): SshConfig? {
    return sshManager.chooseFromUiConnectionData(project, clusterDetails)
  }

  override suspend fun getOrDefaultInitSshConfig(project: Project): SshConfig = sshManager.getOrCreateSshConfig(project, clusterDetails)

  override fun getOrCreateSparkConnection(): ConnectionData? {
    return dataManager.createSparkConnection(project, clusterDetails) {}
  }

  override fun addApplicationToMonitoring(processHandler: ProcessHandler, name: String, focusOnApp: Boolean) {
    val sparkService = MonitoringServiceProvider.getSparkMonitoringService() ?: return

    dataManager.createSparkConnection(project, clusterDetails) {
      executeOnPooledThread {
        sparkService.startApp(project, it, name, processHandler, focusOnApp)
      }
    }
  }

  companion object {
    fun createId(driver: Driver, clusterSummary: EmrClusterSummary) =
      RemoteTargetId(driver.getExternalId(), clusterSummary.id, clusterSummary.name)
    fun createId(driver: Driver, clusterSummary: ClusterSummary) =
      RemoteTargetId(driver.getExternalId(), clusterSummary.id(), clusterSummary.name())
  }
}