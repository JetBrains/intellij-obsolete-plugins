package com.jetbrains.bigdatatools.dataproc.toolwindow.controllers

import com.google.protobuf.util.JsonFormat
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.ui.components.ActionLink
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.WrapLayout
import com.jetbrains.bigdatatools.common.monitoring.actions.OpenUrlAction
import com.jetbrains.bigdatatools.common.monitoring.toolwindow.AbstractGroupFieldsModelsController
import com.jetbrains.bigdatatools.common.ui.BdtJsonInfoDialog
import com.jetbrains.bigdatatools.common.ui.setNorthComponent
import com.jetbrains.bigdatatools.dataproc.data.DataprocDataManager
import com.jetbrains.bigdatatools.dataproc.settings.DataprocToolWindowSettings
import com.jetbrains.bigdatatools.dataproc.util.DataprocMessagesBundle
import org.com.jetbrains.bigdatatools.utils.HdfsMessagesBundle
import java.awt.FlowLayout
import javax.swing.JPanel

class DataprocJobInfoController(project: Project, override val dataManager: DataprocDataManager) :
  AbstractGroupFieldsModelsController<String>(project, dataManager.connectionData.innerId) {

  var clusterId: String = ""

  override val toolWindowSettings = DataprocToolWindowSettings.getInstance()

  init {
    init()
    intermediatePanel.setNorthComponent(createLinksPanel())
  }

  override fun getFieldsGroupModel(id: String) = dataManager.getJobInfoModel(clusterId, id)

  override fun createActions(): List<AnAction> {
    val showAsJson = object : DumbAwareAction(HdfsMessagesBundle.message("emr.cluster.info.details"), null,
                                              AllIcons.FileTypes.Json) {
      override fun actionPerformed(e: AnActionEvent) {
        val jobId = id ?: return
        val jobInfo = dataManager.getJobInfoModel(clusterId, jobId).originObject ?: return
        val job = jobInfo.job

        BdtJsonInfoDialog(project, jobInfo.id, JsonFormat.printer().print(job)).show()
      }

      override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = id != null
      }

      override fun getActionUpdateThread() = ActionUpdateThread.BGT
    }

    val openUrl = OpenUrlAction(dataManager) {
      val connectionData = dataManager.connectionData
      val projectId = connectionData.projectId
      val region = connectionData.region
      "/jobs/${id}/monitoring?region=${region}&project=${projectId}"
    }
    return super.createActions() + listOf(showAsJson, openUrl)
  }

  private fun createLinksPanel(): JPanel {
    val showJobFiles = ActionLink(DataprocMessagesBundle.message("job.info.open.job.files")) {
      val id = id ?: return@ActionLink
      val jobInfo = dataManager.getJobInfoModel(clusterId, id).originObject ?: return@ActionLink
      val clusterInfo = dataManager.getClusterInfoModel(jobInfo.cluster).originObject ?: return@ActionLink
      dataManager.actionWrapper(clusterInfo.name) {
        dataManager.dependsManager.browseGcsStageBucket(project, clusterInfo, jobInfo.job.driverControlFilesUri)
      }
    }

    return JPanel(WrapLayout(FlowLayout.LEADING, JBUI.scale(10), JBUI.scale(10))).apply {
      add(showJobFiles)
    }
  }
}