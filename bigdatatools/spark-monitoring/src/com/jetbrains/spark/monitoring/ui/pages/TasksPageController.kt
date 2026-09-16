package com.jetbrains.spark.monitoring.ui.pages

import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.RightAlignedToolbarAction
import com.intellij.openapi.actionSystem.ex.ComboBoxAction
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.ui.IdeBorderFactory
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.SideBorder
import com.intellij.ui.TableSpeedSearch
import com.jetbrains.bigdatatools.common.monitoring.table.DataTable
import com.jetbrains.bigdatatools.common.monitoring.table.DataTableCreator
import com.jetbrains.bigdatatools.common.monitoring.table.extension.TableColumnsFitter
import com.jetbrains.bigdatatools.common.monitoring.table.extension.TableExtensionType
import com.jetbrains.bigdatatools.common.monitoring.table.extension.TableHeightFitter
import com.jetbrains.bigdatatools.common.monitoring.table.extension.TableLoadingDecorator
import com.jetbrains.bigdatatools.common.monitoring.table.model.DataTableColumnModel
import com.jetbrains.bigdatatools.common.monitoring.table.model.DataTableModel
import com.jetbrains.bigdatatools.common.settings.ColumnVisibilitySettings
import com.jetbrains.bigdatatools.common.ui.CollapsiblePanel
import com.jetbrains.spark.monitoring.data.Metric
import com.jetbrains.spark.monitoring.data.TaskData
import com.jetbrains.spark.monitoring.models.SparkDataManager
import com.jetbrains.spark.monitoring.models.TasksDataId
import com.jetbrains.spark.monitoring.settings.SparkToolwindowSettings
import com.jetbrains.spark.monitoring.statistics.SparkMonitoringUsagesCollector
import com.jetbrains.spark.monitoring.statistics.UIPanelType
import com.jetbrains.spark.monitoring.ui.table.LabelCountUpdater
import com.jetbrains.spark.monitoring.ui.table.renderers.SummaryColumnRenderer
import com.jetbrains.spark.monitoring.ui.utils.OpenUrlAction
import com.jetbrains.spark.monitoring.util.SMMessagesBundle
import java.awt.BorderLayout
import java.util.EnumSet
import javax.swing.BoxLayout
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.RowFilter
import javax.swing.table.TableRowSorter

class TasksPageController(private val project: Project, connectionId: String) : Disposable {
  private val dataManager = SparkDataManager.getInstance(connectionId, project) ?: error("Data Manager is not initialized")

  private val scrollPane: JScrollPane

  private var taskId: TasksDataId? = null

  private lateinit var tasksTable: DataTable<TaskData>
  private lateinit var tasksTableScrollPane: JScrollPane

  private lateinit var tasksSummaryTable: DataTable<Metric>
  private lateinit var tasksSummaryTableScrollPane: JScrollPane

  init {
    val panel = JPanel()
    panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)
    panel.add(createTasksListForStage(project))
    panel.add(createTasksSummaryListForStage())

    val parentPanel = JPanel(BorderLayout(0, 10))
    parentPanel.add(panel, BorderLayout.NORTH)

    scrollPane = ScrollPaneFactory.createScrollPane(parentPanel, true)
  }

  fun setTaskId(taskId: TasksDataId?) {
    this.taskId = taskId

    if (taskId == null) {
      tasksTable.tableModel.setDataModel(null)
      tasksSummaryTable.tableModel.setDataModel(null)
      return
    }

    val tasksModel = dataManager.getTasksModel(taskId.applicationId, taskId.stageId, taskId.attemptId)
    tasksTable.tableModel.setDataModel(tasksModel)
    if (tasksModel.size > 0) {
      TableHeightFitter.fitSize(tasksTableScrollPane, tasksTable)
    }
    TableColumnsFitter.get(tasksTable)?.reset()
    TableLoadingDecorator.installOn(tasksTable)

    val tasksSummaryModel = dataManager.getTasksSummaryModel(taskId.applicationId, taskId.stageId, taskId.attemptId)
    tasksSummaryTable.tableModel.setDataModel(tasksSummaryModel)
    if (tasksSummaryModel.size > 0) {
      TableHeightFitter.fitSize(tasksSummaryTableScrollPane, tasksSummaryTable)
    }
    TableColumnsFitter.get(tasksSummaryTable)?.reset()
    TableLoadingDecorator.installOn(tasksSummaryTable)

    scrollPane.revalidate()
    scrollPane.repaint()
  }

  fun getComponent(): JComponent = scrollPane

  private fun createTasksListForStage(project: Project): JComponent {

    val tasksColumnSettings = SparkToolwindowSettings.getInstance().tasksColumnSettings

    val columnModel = DataTableColumnModel(TaskData.renderableColumns, tasksColumnSettings)
    val tableModel = DataTableModel(null, columnModel)

    tasksTable = DataTableCreator.create(tableModel, EnumSet.of(TableExtensionType.SPEED_SEARCH,
                                                                TableExtensionType.RENDERERS_SETTER,
                                                                TableExtensionType.ERROR_HANDLER,
                                                                TableExtensionType.LOADING_INDICATOR,
                                                                TableExtensionType.COLUMNS_FITTER,
                                                                TableExtensionType.SMART_RESIZER))
    Disposer.register(this, tasksTable)

    tasksTableScrollPane = ScrollPaneFactory.createScrollPane(tasksTable, true)

    TableHeightFitter.installOn(tasksTableScrollPane, tasksTable)

    val openInBrowserAction = object : OpenUrlAction(
      { if (taskId == null) "" else "stages/stage/?id=${taskId!!.stageId}&attempt=${taskId!!.attemptId}" },
      project, dataManager, { taskId?.applicationId?.toString() }), RightAlignedToolbarAction {}

    val settings = SparkToolwindowSettings.getInstance()
    val configTasksColumnsAction = ColumnVisibilitySettings.createRightAlignedAction(TaskData.renderableColumns,
                                                                                     settings.tasksColumnSettings)

    val panelTable = CollapsiblePanel(SMMessagesBundle.message("tasks.title")).apply {
      addActions(openInBrowserAction, configTasksColumnsAction)
      component = tasksTableScrollPane
      collapsed = true
      addExpandListener { value -> SparkMonitoringUsagesCollector.panelExpandedEvent.log(project, UIPanelType.Tasks, value) }
    }

    LabelCountUpdater.installOn(tasksTable, SMMessagesBundle.message("tasks.title"), panelTable.titleLabel)

    return panelTable
  }

  private fun createTasksSummaryListForStage(): JComponent {
    val columnModel = DataTableColumnModel(Metric.renderableColumns, ColumnVisibilitySettings(
      mutableListOf("metric", "min", "percentile25", "median", "percentile75", "max"))
    )
    val tableModel = DataTableModel(null, columnModel)

    tasksSummaryTable = DataTable(tableModel, columnModel)
    tasksSummaryTable.tableHeader.reorderingAllowed = false
    tasksSummaryTable.oneAndHalfRowHeight = true

    TableSpeedSearch.installOn(tasksSummaryTable)

    Disposer.register(this, tasksSummaryTable)
    Disposer.register(tasksSummaryTable, columnModel)
    Disposer.register(tasksSummaryTable, tableModel)

    val filter = object : RowFilter<DataTableModel<Metric>, Int>() {
      override fun include(entry: Entry<out DataTableModel<Metric>, out Int>): Boolean {
        for (i in 1 until 6) {
          if (entry.getValue(i) != 0L) {
            return true
          }
        }
        return false
      }
    }

    val sorter = TableRowSorter(tableModel)
    sorter.rowFilter = filter
    tasksSummaryTable.rowSorter = sorter

    val summaryColumnRenderer = SummaryColumnRenderer()

    for (column in tasksSummaryTable.columnModel.columns) {
      column.cellRenderer = summaryColumnRenderer
    }

    tasksSummaryTableScrollPane = ScrollPaneFactory.createScrollPane(tasksSummaryTable, true)

    TableColumnsFitter.installOn(tasksSummaryTable, null)
    TableHeightFitter.installOn(tasksSummaryTableScrollPane, tasksSummaryTable)

    val filterModeAction: ComboBoxAction = object : ComboBoxAction(), RightAlignedToolbarAction {

      override fun isDumbAware(): Boolean = true

      override fun update(e: AnActionEvent) {
        super.update(e)
        e.presentation.text = if (sorter.rowFilter == null) SMMessagesBundle.message("tasks.showAllRow")
        else SMMessagesBundle.message("tasks.hideEmptyRows")
      }

      override fun getActionUpdateThread() = ActionUpdateThread.BGT

      override fun createPopupActionGroup(button: JComponent, context: DataContext): DefaultActionGroup {
        val group = DefaultActionGroup()

        group.add(DumbAwareAction.create(SMMessagesBundle.message("tasks.showAllRow")) {
          sorter.rowFilter = null
          templatePresentation.text = SMMessagesBundle.message("tasks.showAllRow")
          TableHeightFitter.fitSize(tasksSummaryTableScrollPane, tasksSummaryTable)
        })

        group.add(DumbAwareAction.create(SMMessagesBundle.message("tasks.hideEmptyRows")) {
          sorter.rowFilter = filter
          templatePresentation.text = SMMessagesBundle.message("tasks.hideEmptyRows")
          TableHeightFitter.fitSize(tasksSummaryTableScrollPane, tasksSummaryTable)
        })
        return group
      }
    }

    return CollapsiblePanel(SMMessagesBundle.message("tasks.summary")).apply {
      addActions(filterModeAction)
      component = tasksSummaryTableScrollPane
      border = IdeBorderFactory.createBorder(SideBorder.TOP or SideBorder.BOTTOM)
      addExpandListener { value -> SparkMonitoringUsagesCollector.panelExpandedEvent.log(project, UIPanelType.TasksSummary, value) }
    }
  }

  override fun dispose() = Unit
}