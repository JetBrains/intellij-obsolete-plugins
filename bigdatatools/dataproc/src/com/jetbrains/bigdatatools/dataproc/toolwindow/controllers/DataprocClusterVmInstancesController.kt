package com.jetbrains.bigdatatools.dataproc.toolwindow.controllers

import com.intellij.bigdatatools.sftp.icons.BigdatatoolsSftpIcons
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.jetbrains.bigdatatools.common.monitoring.toolwindow.DetailsTableMonitoringController
import com.jetbrains.bigdatatools.dataproc.data.DataprocDataManager
import com.jetbrains.bigdatatools.dataproc.data.DataprocDataManagerUtils
import com.jetbrains.bigdatatools.dataproc.model.DataprocVmInstanceInfo
import com.jetbrains.bigdatatools.dataproc.model.DataprocWebInterfaceInfo
import com.jetbrains.bigdatatools.dataproc.settings.DataprocToolWindowSettings
import com.jetbrains.bigdatatools.dataproc.util.DataprocMessagesBundle
import com.jetbrains.bigdatatools.sftp.util.SshUtils

class DataprocClusterVmInstancesController(val project: Project,
                                           private val dataManager: DataprocDataManager) : DetailsTableMonitoringController<DataprocVmInstanceInfo, String>() {
  init {
    init()
  }

  override fun getAdditionalActions(): List<AnAction> {
    val createSshConnectionAction = object : DumbAwareAction(DataprocMessagesBundle.message("action.ssh"),
                                                             null,
                                                             AllIcons.Debugger.Console) {
      override fun actionPerformed(e: AnActionEvent) {
        val instance = getSelectedItem() ?: return
        val cluster = dataManager.getClusterInfoModel(instance.clusterInfo.name).originObject ?: return
        if (!DataprocDataManagerUtils.checkIsCliOperationsAllowed(project, dataManager.connectionData))
          return

        val instanceName = instance.name

        dataManager.actionWrapper(cluster.name) {
          dataManager.driverCreator.createSshConsole(project, cluster, instanceName)
        }
      }

      override fun update(e: AnActionEvent) {
        e.presentation.isVisible = SshUtils.isSshConsoleAvailable()
        e.presentation.isEnabled = dataManager.isClusterRun(getSelectedItem()?.clusterInfo?.name)
      }

      override fun getActionUpdateThread() = ActionUpdateThread.BGT
    }


    val createSftpConnectionAction = object : DumbAwareAction(DataprocMessagesBundle.message("action.sftp"),
                                                              null,
                                                              BigdatatoolsSftpIcons.Sftp) {
      override fun actionPerformed(e: AnActionEvent) {
        val instance = getSelectedItem() ?: return
        val cluster = dataManager.getClusterInfoModel(instance.clusterInfo.name).originObject ?: return

        val instanceName = instance.name
        val appInfo = DataprocWebInterfaceInfo(name = instanceName, url = "", componentGateway = false,
                                               instanceName = instanceName)

        dataManager.actionWrapper(cluster.name) {
          dataManager.dependsManager.browseOrOpenConnection(project, cluster, appInfo)
        }
      }

      override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = dataManager.isClusterRun(getSelectedItem()?.clusterInfo?.name)
      }

      override fun getActionUpdateThread() = ActionUpdateThread.BGT

    }

    return listOf(createSshConnectionAction, createSftpConnectionAction)
  }

  override fun showColumnFilter(): Boolean = false
  override fun getColumnSettings() = DataprocToolWindowSettings.getInstance().vmInstanceColumnsSettings
  override fun getRenderableColumns() = DataprocVmInstanceInfo.renderableColumns
  override fun getDataModel() = selectedId?.let { dataManager.getClusterVmInstancesDataModel(it) }
}

