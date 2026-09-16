package com.jetbrains.bigdatatools.dataproc.toolwindow.controllers

import com.google.protobuf.util.JsonFormat
import com.intellij.bigdatatools.sftp.icons.BigdatatoolsSftpIcons
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.Separator
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.jetbrains.bigdatatools.common.monitoring.actions.OpenUrlAction
import com.jetbrains.bigdatatools.common.monitoring.data.model.FieldsGroupModel
import com.jetbrains.bigdatatools.common.monitoring.toolwindow.AbstractGroupFieldsModelsController
import com.jetbrains.bigdatatools.common.ui.BdtJsonInfoDialog
import com.jetbrains.bigdatatools.dataproc.data.DataprocDataManager
import com.jetbrains.bigdatatools.dataproc.data.DataprocDataManagerUtils
import com.jetbrains.bigdatatools.dataproc.model.DataprocClusterInfo
import com.jetbrains.bigdatatools.dataproc.model.DataprocWebInterfaceInfo
import com.jetbrains.bigdatatools.dataproc.settings.DataprocToolWindowSettings
import com.jetbrains.bigdatatools.dataproc.util.DataprocMessagesBundle
import com.jetbrains.bigdatatools.sftp.util.SshUtils
import org.com.jetbrains.bigdatatools.icons.Icons
import org.com.jetbrains.bigdatatools.utils.HdfsMessagesBundle

class DataprocClusterInfoController(project: Project, override val dataManager: DataprocDataManager) :
  AbstractGroupFieldsModelsController<String>(project, dataManager.connectionData.innerId) {

  override val toolWindowSettings = DataprocToolWindowSettings.getInstance()

  init {
    init()
  }

  override fun createActions(): List<AnAction> {
    val openStageBucketGcs = object : DumbAwareAction(DataprocMessagesBundle.message("action.open.stage.bucket"),
                                                      null, Icons.GCS_ICON) {
      override fun actionPerformed(e: AnActionEvent) {
        val id = id ?: return
        val cluster = dataManager.getClusterInfoModel(id).originObject ?: return


        dataManager.dependsManager.browseGcsStageBucket(project, cluster, null)
      }

      override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = id != null
      }

      override fun getActionUpdateThread() = ActionUpdateThread.BGT
    }

    val showClusterDetailsAction = object : DumbAwareAction(HdfsMessagesBundle.message("emr.cluster.info.details"), null,
                                                            AllIcons.FileTypes.Json) {
      override fun actionPerformed(e: AnActionEvent) {
        val id = id ?: return
        val cluster = dataManager.getClusterInfoModel(id).originObject?.cluster ?: return

        BdtJsonInfoDialog(project, cluster.clusterName, JsonFormat.printer().print(cluster)).show()
      }

      override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = id != null
      }

      override fun getActionUpdateThread() = ActionUpdateThread.BGT
    }

    val createSshConnectionAction = object : DumbAwareAction(DataprocMessagesBundle.message("action.ssh.master.node"), null,
                                                             AllIcons.Debugger.Console) {
      override fun actionPerformed(e: AnActionEvent) {
        val id = id ?: return
        val cluster = dataManager.getClusterInfoModel(id).originObject ?: return

        if (!DataprocDataManagerUtils.checkIsCliOperationsAllowed(project, dataManager.connectionData))
          return
        val instanceName = "${cluster.name}-m"

        dataManager.actionWrapper(cluster.name) {
          dataManager.driverCreator.createSshConsole(project, cluster, instanceName)

        }
      }

      override fun update(e: AnActionEvent) {
        e.presentation.isVisible = SshUtils.isSshConsoleAvailable()
        e.presentation.isEnabled = dataManager.isClusterRun(id)
      }

      override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT
    }

    val createSftpConnectionAction = object : DumbAwareAction(DataprocMessagesBundle.message("action.sftp.master.node"),
                                                              null,
                                                              BigdatatoolsSftpIcons.Sftp) {
      override fun actionPerformed(e: AnActionEvent) {
        val id = id ?: return
        val cluster = dataManager.getClusterInfoModel(id).originObject ?: return
        if (!DataprocDataManagerUtils.checkIsCliOperationsAllowed(project, dataManager.connectionData))
          return

        val instanceName = "${cluster.name}-m"
        val appInfo = DataprocWebInterfaceInfo(name = instanceName, url = "", componentGateway = false,
                                               instanceName = instanceName)

        dataManager.actionWrapper(cluster.name) {
          dataManager.dependsManager.browseOrOpenConnection(project, cluster, appInfo)
        }
      }

      override fun getActionUpdateThread() = ActionUpdateThread.BGT

      override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = dataManager.isClusterRun(id)
      }
    }

    val openUrlAction = OpenUrlAction(dataManager) {
      val connectionData = dataManager.connectionData
      "/clusters/${id}/monitoring?region=${connectionData.region}&project=${connectionData.projectId}"
    }

    return super.createActions() + listOf(openStageBucketGcs, createSshConnectionAction, createSftpConnectionAction,
                                          Separator.create(),
                                          showClusterDetailsAction,
                                          openUrlAction)
  }

  override fun getFieldsGroupModel(id: String): FieldsGroupModel<DataprocClusterInfo> = dataManager.getClusterInfoModel(id)
}