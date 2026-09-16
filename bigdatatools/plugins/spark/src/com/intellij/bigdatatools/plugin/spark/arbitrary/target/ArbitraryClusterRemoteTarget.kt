package com.intellij.bigdatatools.plugin.spark.arbitrary.target

import com.intellij.bigdatatools.plugin.spark.BigdatatoolsPluginSparkIcons
import com.intellij.bigdatatools.plugin.spark.arbitrary.ArbitraryClusterDataManager
import com.intellij.execution.process.ProcessHandler
import com.intellij.openapi.project.Project
import com.intellij.ssh.config.unified.SshConfig
import com.intellij.ui.SimpleColoredComponent
import com.intellij.ui.SimpleTextAttributes
import com.intellij.bigdatatools.coreUi.settings.connections.ConnectionData
import com.jetbrains.bigdatatools.common.integration.MonitoringServiceProvider
import com.intellij.bigdatatools.coreUi.util.executeOnPooledThread
import com.jetbrains.spark.submit.model.ClusterManagerType
import com.jetbrains.spark.submit.model.DeployModeType
import com.jetbrains.spark.submit.run.cluster.RemoteTarget
import com.jetbrains.spark.submit.run.cluster.RemoteTargetId
import com.jetbrains.spark.submit.run.cluster.RemoteTargetType
import com.jetbrains.spark.submit.run.cluster.ui.ClusterSparkSubmitConfigurationEditor
import com.jetbrains.spark.submit.util.SparkMessagesBundle
import javax.swing.Icon

class ArbitraryClusterRemoteTarget(
  val project: Project,
  private val dataManager: ArbitraryClusterDataManager
) : RemoteTarget(dataManager.connectionData.name, RemoteTargetId(dataManager.driver.getExternalId(), null, dataManager.connectionData.name)) {
  override val type: RemoteTargetType = RemoteTargetType.ARBITRARY_CLUSTER

  override val icon: Icon = BigdatatoolsPluginSparkIcons.ArbitraryCluster

  override fun setupItem(item: SimpleColoredComponent) {
    item.icon = this@ArbitraryClusterRemoteTarget.icon
    item.append(this@ArbitraryClusterRemoteTarget.name)
    item.append("  ")
    item.append(SparkMessagesBundle.message("remote.target.arbitrary.cluster.remark"), SimpleTextAttributes.GRAYED_ITALIC_ATTRIBUTES)
  }

  override fun updateEditorFields(editor: ClusterSparkSubmitConfigurationEditor) {
    super.updateEditorFields(editor)

    editor.setTargetDependentSettings {
      editor.deployModeField.item = DeployModeType.CLUSTER
      editor.clusterManagerField.item = ClusterManagerType.YARN
    }
  }

  override val sshDetailsLinkCaption: String
    get() = dataManager.getSshConfig(project)?.name ?: "<ERROR>"

  override suspend fun openSshConfigDialog(editor: ClusterSparkSubmitConfigurationEditor): SshConfig? {
    return dataManager.chooseFromUiConnectionData(project)
  }

  override suspend fun getOrDefaultInitSshConfig(project: Project): SshConfig = dataManager.getSshConfig(project) ?: error("Not setup")

  override fun getOrCreateSparkConnection(): ConnectionData? {
    return dataManager.createSparkConnection(project) {}
  }

  override fun addApplicationToMonitoring(processHandler: ProcessHandler, name: String, focusOnApp: Boolean) {
    val sparkService = MonitoringServiceProvider.getSparkMonitoringService() ?: return

    dataManager.createSparkConnection(project) {
      executeOnPooledThread {
        sparkService.startApp(project, it, name, processHandler, focusOnApp)
      }
    }
  }
}