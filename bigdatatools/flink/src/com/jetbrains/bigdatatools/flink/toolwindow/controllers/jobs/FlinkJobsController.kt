package com.jetbrains.bigdatatools.flink.toolwindow.controllers.jobs

import com.intellij.bigdatatools.coreUi.util.executeOnPooledThread
import com.intellij.icons.AllIcons
import com.intellij.ide.actions.SmartPopupActionGroup
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.Separator
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.DumbAwareToggleAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.ui.components.JBTextField
import com.jetbrains.bigdatatools.common.monitoring.actions.OpenUrlAction
import com.jetbrains.bigdatatools.common.monitoring.data.model.DataModelFilter
import com.jetbrains.bigdatatools.common.monitoring.data.model.FilterAdapter
import com.jetbrains.bigdatatools.common.monitoring.table.DataTable
import com.jetbrains.bigdatatools.common.monitoring.table.getSelectedData
import com.jetbrains.bigdatatools.common.monitoring.toolwindow.TableWithDetailsMonitoringController
import com.jetbrains.bigdatatools.common.table.renderers.LinkRenderer
import com.jetbrains.bigdatatools.common.ui.CustomComponentActionImpl
import com.jetbrains.bigdatatools.common.ui.ToolbarLabelActionImpl
import com.jetbrains.bigdatatools.common.ui.filter.CountFilterPopupComponent
import com.jetbrains.bigdatatools.common.util.invokeLater
import com.jetbrains.bigdatatools.flink.data.FlinkDataManager
import com.jetbrains.bigdatatools.flink.graph.file.JobVirtualFile
import com.jetbrains.bigdatatools.flink.graph.util.JobDetailsInfoParser
import com.jetbrains.bigdatatools.flink.model.JobExecutionStatus
import com.jetbrains.bigdatatools.flink.model.JobInfo
import com.jetbrains.bigdatatools.flink.toolwindow.config.FlinkToolWindowSettings
import com.jetbrains.bigdatatools.flink.util.FlinkMessagesBundle

class FlinkJobsController(val project: Project,
                          private val dataManager: FlinkDataManager) : TableWithDetailsMonitoringController<JobInfo, String>() {
  private val cancelJob = object : DumbAwareAction(FlinkMessagesBundle.message("job.action.cancel"), null,
                                                   AllIcons.Actions.Suspend) {
    override fun actionPerformed(e: AnActionEvent) {
      val job = getSelectedItem() ?: return
      val res = Messages.showYesNoDialog(project,
                                         FlinkMessagesBundle.message("dialog.job.cancel.message", job.jobName),
                                         FlinkMessagesBundle.message("dialog.job.cancel.title"),
                                         Messages.getQuestionIcon())
      if (res != Messages.OK)
        return
      dataManager.cancelJob(job.jid)
    }

    override fun update(e: AnActionEvent) {
      e.presentation.isEnabledAndVisible = getSelectedItem()?.status == JobExecutionStatus.RUNNING
    }

    override fun getActionUpdateThread() = ActionUpdateThread.BGT
  }

  private val actions: List<AnAction> = listOf(
    cancelJob,
    Separator.create(),
    OpenUrlAction(dataManager) { "/#/overview" }
  )

  private val connectionId = dataManager.connectionData.innerId

  override val detailsController = FlinkJobTabbedDetailsController(project, dataManager)

  init {
    init()
  }

  override fun customTableInit(table: DataTable<JobInfo>) {
    LinkRenderer.installOnColumn(table, columnModel.getColumn(table.columnCount - 1)).apply {
      onClick = { row, _ ->
        executeOnPooledThread {
          val jobId = indexToDetailId(row)
          val detailsOfJob = dataManager.client.getDetailsOfJob(jobId)

          invokeLater {
            JobDetailsInfoParser.plan = detailsOfJob.plan

            val fileEditorManager = FileEditorManager.getInstance(project)
            val virtualFile = JobVirtualFile(connectionId, jobId)
            fileEditorManager.openFile(virtualFile, true)
          }
        }
      }
    }
  }

  override fun createTopLeftToolbarActions(): List<AnAction> {
    val settings = FlinkToolWindowSettings.getInstance()
    val config = settings.getOrCreateConfig(connectionId)
    val userText = JBTextField(config.jobFilter, 8)

    FilterAdapter.install(dataTable.tableModel, userText, JobInfo.TEXT_FILTER) { userQuery ->
      config.jobFilter = userQuery
      dataManager.updater.invokeRefreshModel(dataManager.jobsModel)
    }

    val countFilter = CountFilterPopupComponent(FlinkMessagesBundle.message("flink.filter.limit"), config.jobLimit)
    FilterAdapter.install(dataTable.tableModel, countFilter, JobInfo.LIMIT_FILTER) { limit ->
      config.jobLimit = limit
      dataManager.updater.invokeRefreshModel(dataManager.jobsModel)
    }

    return listOf(ToolbarLabelActionImpl(FlinkMessagesBundle.message("flink.filter.text")),
                  CustomComponentActionImpl(userText),
                  createStatusFilter(settings),
                  CustomComponentActionImpl(countFilter))
  }

  private fun createStatusFilter(settings: FlinkToolWindowSettings): SmartPopupActionGroup {
    val statusFilter = SmartPopupActionGroup()

    statusFilter.templatePresentation.text = FlinkMessagesBundle.message("flink.filter.jobs")
    statusFilter.templatePresentation.icon = AllIcons.General.Filter

    for (status in JobExecutionStatus.entries) {
      val toggleState = object : DumbAwareToggleAction(status.title, null, null) {
        override fun isSelected(e: AnActionEvent) = settings.jobStatus.contains(status)
        override fun getActionUpdateThread() = ActionUpdateThread.BGT
        override fun setSelected(e: AnActionEvent, state: Boolean) {
          val filters = dataTable.tableModel.getDataModel()?.filters

          if (state) {
            settings.jobStatus.add(status)
            filters?.setFilter(DataModelFilter(JobInfo.STATUS_FILTER, settings.jobStatus.joinToString(",")))
          }
          else {
            settings.jobStatus.remove(status)
            if (settings.jobStatus.isEmpty()) {
              filters?.removeFilter(JobInfo.STATUS_FILTER)
            }
            else {
              filters?.setFilter(DataModelFilter(JobInfo.STATUS_FILTER, settings.jobStatus.joinToString(",")))
            }
          }
          dataManager.updater.invokeRefreshModel(dataManager.jobsModel)
        }
      }
      statusFilter.add(toggleState)
    }
    return statusFilter
  }

  override fun indexToDetailId(row: Int) = dataTable.getDataAt(row)?.jid ?: ""

  override fun saveSelectedItem() {
    FlinkToolWindowSettings.getInstance().saveSelectedJob(connectionId, dataTable.getSelectedData()?.jid ?: "")
  }

  override fun showColumnFilter(): Boolean = false

  override fun getColumnSettings() = FlinkToolWindowSettings.getInstance().jobsColumnSettings

  override fun getRenderableColumns() = JobInfo.renderableColumns

  override fun getDataModel() = dataManager.jobsModel

  override fun getAdditionalActions() = if (dataManager.connectionData.isFlinkHistory) actions.drop(1) else actions
}