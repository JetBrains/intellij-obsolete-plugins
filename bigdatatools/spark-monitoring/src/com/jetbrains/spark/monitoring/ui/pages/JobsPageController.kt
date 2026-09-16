package com.jetbrains.spark.monitoring.ui.pages

import com.intellij.CommonBundle
import com.intellij.bigdatatools.coreUi.util.executeOnPooledThread
import com.intellij.icons.AllIcons
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.DumbAwareToggleAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.openapi.ui.Splitter
import com.intellij.openapi.util.Disposer
import com.intellij.ui.OnePixelSplitter
import com.intellij.ui.TableSpeedSearch
import com.intellij.ui.components.JBPanelWithEmptyText
import com.jetbrains.bigdatatools.common.integration.MonitoringServiceProvider
import com.jetbrains.bigdatatools.common.monitoring.table.DataTable
import com.jetbrains.bigdatatools.common.monitoring.table.extension.TableColumnsFitter
import com.jetbrains.bigdatatools.common.monitoring.table.extension.TableErrorHandler
import com.jetbrains.bigdatatools.common.monitoring.table.extension.TableLoadingDecorator
import com.jetbrains.bigdatatools.common.monitoring.table.extension.TableRenderersSetter
import com.jetbrains.bigdatatools.common.monitoring.table.extension.TableSelectionPreserver
import com.jetbrains.bigdatatools.common.monitoring.table.getSelectedData
import com.jetbrains.bigdatatools.common.monitoring.table.model.DataTableColumnModel
import com.jetbrains.bigdatatools.common.monitoring.table.model.DataTableModel
import com.jetbrains.bigdatatools.common.monitoring.toolwindow.DetailsMonitoringController
import com.jetbrains.bigdatatools.common.settings.ColumnVisibilitySettings
import com.jetbrains.bigdatatools.common.table.MaterialJBScrollPane
import com.jetbrains.bigdatatools.common.table.extension.TableResizeController
import com.jetbrains.bigdatatools.common.table.getColumnByIdentifier
import com.jetbrains.bigdatatools.common.table.renderers.LinkRenderer
import com.jetbrains.bigdatatools.common.ui.ToolbarVerticalLabelAction
import com.jetbrains.bigdatatools.common.util.ToolbarUtils
import com.jetbrains.bigdatatools.common.util.invokeLater
import com.jetbrains.spark.monitoring.data.AppAttemptId
import com.jetbrains.spark.monitoring.data.JobData
import com.jetbrains.spark.monitoring.data.JobExecutionStatus
import com.jetbrains.spark.monitoring.graph.file.DotVirtualFile
import com.jetbrains.spark.monitoring.graph.util.DotFormatParser
import com.jetbrains.spark.monitoring.models.JobsDataId
import com.jetbrains.spark.monitoring.models.SparkDataManager
import com.jetbrains.spark.monitoring.models.StagesDataId
import com.jetbrains.spark.monitoring.settings.SparkToolwindowSettings
import com.jetbrains.spark.monitoring.statistics.SparkMonitoringUsagesCollector
import com.jetbrains.spark.monitoring.statistics.StateFilerType
import com.jetbrains.spark.monitoring.ui.utils.IconUtils
import com.jetbrains.spark.monitoring.ui.utils.OpenUrlAction
import com.jetbrains.spark.monitoring.util.SMMessagesBundle
import javax.swing.BorderFactory
import javax.swing.BoxLayout
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JTable
import javax.swing.RowFilter
import javax.swing.event.ListSelectionEvent
import javax.swing.event.ListSelectionListener
import javax.swing.table.TableRowSorter

/**
 * Controller for spark jobs list view.
 */
class JobsPageController(val project: Project, private val connectionId: String) : DetailsMonitoringController<AppAttemptId> {
  private val dataManager = SparkDataManager.getInstance(connectionId, project) ?: error("Data Manager is not initialized")

  private val listContainer = JPanel()

  private val splitter = OnePixelSplitter(false, SparkToolwindowSettings.getInstance().getJobsSplitterProportion(connectionId))

  private val centralPanel = SimpleToolWindowPanel(false, true)

  private lateinit var jobsTable: DataTable<JobData>

  private lateinit var tableColumnsFitter: TableColumnsFitter<JobData>

  private var jobsId: JobsDataId? = null

  private val jobsSelectionListener = object : ListSelectionListener {

    override fun valueChanged(e: ListSelectionEvent) {
      if (e.valueIsAdjusting) {
        return
      }
      valueChanged()
    }

    fun valueChanged() {
      val settings = SparkToolwindowSettings.getInstance()

      if (jobsTable.selectedRow == -1) {
        splitter.secondComponent = JBPanelWithEmptyText().withEmptyText(SMMessagesBundle.message("jobs.noSelection"))
        settings.setSelectedJobId(connectionId, null)
      }
      else {
        val job = jobsTable.getSelectedData() ?: return
        showStagesForJob(job)
        settings.setSelectedJobId(connectionId, job.id.toString())
      }
    }
  }

  init {
    listContainer.layout = BoxLayout(listContainer, BoxLayout.Y_AXIS)
    centralPanel.setContent(splitter)

    val settings = SparkToolwindowSettings.getInstance()

    createJobsTableView()

    splitter.secondComponent = JBPanelWithEmptyText().withEmptyText(SMMessagesBundle.message("jobs.noSelection"))

    splitter.addPropertyChangeListener(Splitter.PROP_PROPORTION) {
      settings.setJobsSplitterProportion(connectionId, splitter.proportion)
    }
  }

  override fun setDetailsId(id: AppAttemptId) = setJobsId(JobsDataId(id))

  private fun setJobsId(jobsId: JobsDataId) {
    this.jobsId = jobsId

    val jobsModel = dataManager.getJobsModel(jobsId.applicationId)

    tableColumnsFitter.reset()

    jobsTable.selectionModel.removeListSelectionListener(jobsSelectionListener)
    jobsTable.tableModel.setDataModel(jobsModel)
    jobsTable.selectionModel.addListSelectionListener(jobsSelectionListener)

    TableLoadingDecorator.installOn(jobsTable)
    jobsSelectionListener.valueChanged()
  }

  private fun createJobsTableView() {

    val settings = SparkToolwindowSettings.getInstance()

    val jobsColumnSettings = settings.jobsColumnSettings

    val jobsModel = jobsId?.let { dataManager.getJobsModel(it.applicationId) }

    val columnModel = DataTableColumnModel(JobData.renderableColumns, jobsColumnSettings)
    val tableModel = DataTableModel(jobsModel, columnModel)

    jobsTable = DataTable(tableModel, tableModel.columnModel)
    jobsTable.border = BorderFactory.createEmptyBorder()

    Disposer.register(this, jobsTable)
    Disposer.register(jobsTable, columnModel)
    Disposer.register(jobsTable, tableModel)

    jobsTable.tableHeader.border = BorderFactory.createEmptyBorder()
    TableSpeedSearch.installOn(jobsTable)

    jobsTable.selectionModel.addListSelectionListener(jobsSelectionListener)

    setupTableFilter(jobsTable, tableModel, columnModel)

    TableRenderersSetter.installOn(jobsTable, jobsColumnSettings)
    TableResizeController.installOn(jobsTable)
    tableColumnsFitter = TableColumnsFitter.installOn(jobsTable, jobsColumnSettings)

    val column = jobsTable.getColumnByIdentifier(JobData::visualization.name) ?: return
    LinkRenderer.installOnColumn(jobsTable, column).apply {
      onClick = { _, _ ->
        val appId = jobsId?.applicationId
        val jobId = SparkToolwindowSettings.getInstance().getSelectedJobId(connectionId)
        if (appId != null && jobId != null) {
          executeOnPooledThread {
            val dot = dataManager.client.getDAGForJob(appId.toString(), jobId)

            invokeLater {
              DotFormatParser.graphs = dot.first
              DotFormatParser.externalEdges = dot.second

              val fileEditorManager = FileEditorManager.getInstance(project)
              val virtualFile = DotVirtualFile(connectionId, appId.toString(), jobId)
              fileEditorManager.openFile(virtualFile, true)
            }
          }
        }
      }
    }

    TableSelectionPreserver.installOn(jobsTable, settings.getSelectedJobId(connectionId))
    TableErrorHandler.installOn(jobsTable)

    if (jobsId != null) {
      // TableRowAutoselector.installOn(jobsTable, settings.getSelectedJobId(connectionId))
      TableLoadingDecorator.installOn(jobsTable)
    }

    splitter.firstComponent = MaterialJBScrollPane(jobsTable)
    splitter.firstComponent.border = BorderFactory.createEmptyBorder()

    createToolbar(columnModel)
  }

  private fun setupTableFilter(table: JTable, tableModel: DataTableModel<JobData>, columnModel: DataTableColumnModel<JobData>) {
    val sorter = TableRowSorter(tableModel)

    val filter = object : RowFilter<DataTableModel<JobData>, Int>() {
      private val statusColumnIndex = columnModel.getModelIndex("status")
      override fun include(entry: Entry<out DataTableModel<JobData>, out Int>): Boolean {
        return SparkToolwindowSettings.getInstance().getSparkConfigOrDefault(connectionId).jobStatuses.contains(
          entry.getValue(statusColumnIndex) as JobExecutionStatus)
      }
    }

    sorter.rowFilter = filter
    table.rowSorter = sorter
  }

  private fun createToolbar(columnModel: DataTableColumnModel<JobData>? = null) {

    val actions = DefaultActionGroup()

    actions.addSeparator()

    val settings = SparkToolwindowSettings.getInstance()

    actions.add(ToolbarVerticalLabelAction.create(SMMessagesBundle.message("applications.tab.jobs")))

    val filterAction = DefaultActionGroup(CommonBundle.message("action.text.filter"), null, AllIcons.General.Filter).apply {
      isPopup = true
    }

    for (status in JobExecutionStatus.entries) {
      val toggleState = object : DumbAwareToggleAction(status.text, null, IconUtils.getIconForJobStatus(status)) {
        override fun isSelected(e: AnActionEvent) = settings.getSparkConfigOrDefault(connectionId).jobStatuses.contains(status)
        override fun getActionUpdateThread() = ActionUpdateThread.BGT
        override fun setSelected(e: AnActionEvent, state: Boolean) {
          if (state) {
            settings.getSparkConfigOrDefault(connectionId).jobStatuses.add(status)
          }
          else {
            settings.getSparkConfigOrDefault(connectionId).jobStatuses.remove(status)
          }
          jobsTable.rowSorter?.allRowsChanged()
          SparkMonitoringUsagesCollector.stateFilterChangedEvent.log(project, StateFilerType.Job, status.text, state)
          val applicationId = jobsId?.applicationId ?: return
          dataManager.updater.invokeRefreshModel(dataManager.getJobsModel(applicationId))
        }
      }

      filterAction.add(toggleState)
    }

    actions.add(filterAction)

    if (columnModel != null) {
      val jobsColumnSettings = SparkToolwindowSettings.getInstance().jobsColumnSettings
      val configJobsColumnsAction = ColumnVisibilitySettings.createAction(columnModel.allColumns, jobsColumnSettings)
      actions.addSeparator()
      actions.add(configJobsColumnsAction)
    }

    actions.addSeparator()
    actions.add(OpenUrlAction({ "jobs" }, project, dataManager) { jobsId?.applicationId?.toString() })

    if (centralPanel.toolbar != null) {
      centralPanel.toolbar = null
    }

    centralPanel.toolbar = ToolbarUtils.createActionToolbar(centralPanel, "BDTSparkJobs", actions, false).component
  }

  private var currentStagesPageController: StagesPageController? = null

  /** Shows list of stages */
  private fun showStagesForJob(jobInfo: JobData) {

    jobsId ?: return

    currentTasksPageController?.let { Disposer.dispose(it) }

    if (currentStagesPageController == null) {
      currentStagesPageController = StagesPageController(project, StagesPageController.Type.SHORT, connectionId)
      Disposer.register(this, currentStagesPageController!!)
    }

    currentStagesPageController!!.setStageId(StagesDataId(jobsId!!.applicationId, jobInfo.stageIds.map { it.toString() }),
                                             jobInfo.id)
    splitter.secondComponent = currentStagesPageController!!.getComponent()
  }

  private var currentTasksPageController: TasksPageController? = null

  override fun getComponent(): JComponent {
    return centralPanel
  }

  fun focusOnJob(jobId: Int) {

    if (jobsTable.columnCount < 1) {
      createCantFindJobPopup(jobId)
      return
    }


    try {
      for (i in 0 until jobsTable.rowCount) {
        if (jobsTable.getDataAt(i)?.id == jobId) {
          jobsTable.setRowSelectionInterval(i, i)

          val rect = jobsTable.getCellRect(i, 0, true)
          jobsTable.scrollRectToVisible(rect)
          return
        }
      }
    }
    catch (e: Exception) {
      // Could be that model and table not synchronized yet because model updated from network thread, but informs table in ui thread.
    }

    createCantFindJobPopup(jobId)
  }

  private fun createCantFindJobPopup(jobId: Int) {
    val notificationGroup = MonitoringServiceProvider.getNotificationGroup()
    val notification = notificationGroup.createNotification(SMMessagesBundle.message("jobs.cannotFind.title"),
                                                            SMMessagesBundle.message("jobs.cannotFind.text", jobId),
                                                            NotificationType.INFORMATION)
    notification.notify(project)
  }

  override fun dispose() {}
}