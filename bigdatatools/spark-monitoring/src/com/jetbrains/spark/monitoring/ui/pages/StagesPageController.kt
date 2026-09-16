package com.jetbrains.spark.monitoring.ui.pages

import com.intellij.CommonBundle
import com.intellij.bigdatatools.sparkMonitoring.icons.BigdatatoolsSparkMonitoringIcons
import com.intellij.execution.impl.ConsoleViewImpl
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionToolbar
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.Separator
import com.intellij.openapi.actionSystem.ToggleAction
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.DumbAwareToggleAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.openapi.ui.Splitter
import com.intellij.openapi.util.Disposer
import com.intellij.ui.OnePixelSplitter
import com.intellij.ui.PopupHandler
import com.jetbrains.bigdatatools.common.monitoring.table.DataTable
import com.jetbrains.bigdatatools.common.monitoring.table.DataTableCreator
import com.jetbrains.bigdatatools.common.monitoring.table.extension.TableColumnsFitter
import com.jetbrains.bigdatatools.common.monitoring.table.extension.TableExtensionType
import com.jetbrains.bigdatatools.common.monitoring.table.extension.TableLoadingDecorator
import com.jetbrains.bigdatatools.common.monitoring.table.extension.TableRowAutoselector
import com.jetbrains.bigdatatools.common.monitoring.table.getSelectedData
import com.jetbrains.bigdatatools.common.monitoring.table.model.DataTableColumnModel
import com.jetbrains.bigdatatools.common.monitoring.table.model.DataTableModel
import com.jetbrains.bigdatatools.common.monitoring.toolwindow.DetailsMonitoringController
import com.jetbrains.bigdatatools.common.settings.ColumnVisibilitySettings
import com.jetbrains.bigdatatools.common.table.MaterialJBScrollPane
import com.jetbrains.bigdatatools.common.ui.ToolbarVerticalLabelAction
import com.jetbrains.bigdatatools.common.util.ToolbarUtils
import com.jetbrains.bigdatatools.common.util.invokeLater
import com.jetbrains.spark.monitoring.data.AppAttemptId
import com.jetbrains.spark.monitoring.data.StageData
import com.jetbrains.spark.monitoring.data.StageStatus
import com.jetbrains.spark.monitoring.models.SparkDataManager
import com.jetbrains.spark.monitoring.models.StagesDataId
import com.jetbrains.spark.monitoring.models.TasksDataId
import com.jetbrains.spark.monitoring.settings.SparkToolwindowSettings
import com.jetbrains.spark.monitoring.statistics.SparkMonitoringUsagesCollector
import com.jetbrains.spark.monitoring.statistics.StageUIBlockType
import com.jetbrains.spark.monitoring.statistics.StateFilerType
import com.jetbrains.spark.monitoring.ui.utils.IconUtils
import com.jetbrains.spark.monitoring.ui.utils.OpenUrlAction
import com.jetbrains.spark.monitoring.util.SMMessagesBundle
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.util.EnumSet
import javax.swing.BorderFactory
import javax.swing.JComponent
import javax.swing.JTable
import javax.swing.RowFilter
import javax.swing.event.ListSelectionEvent
import javax.swing.event.ListSelectionListener
import javax.swing.table.TableRowSorter

/**
 * Stages list and details console in viewType.SHORT
 * Additionally tasks list in viewType.FULL
 */
class StagesPageController(private val project: Project,
                           private val viewType: Type,
                           private val connectionId: String) : DetailsMonitoringController<AppAttemptId> {

  enum class Type {
    SHORT,
    FULL // With additional pane with tasks
  }

  private val dataManager = SparkDataManager.getInstance(connectionId, project) ?: error("Data Manager is not initialized")

  // Splitter between stage list and details
  private val stageDetailsSplitter: OnePixelSplitter
  private val detailsTasksSplitter: OnePixelSplitter

  private var detailsConsole: ConsoleViewImpl? = null
  private var tasksPageController: TasksPageController? = null

  private val stagesSelectionListener = object : ListSelectionListener {
    override fun valueChanged(e: ListSelectionEvent) {
      if (e.valueIsAdjusting) {
        return
      }
      updateDetails()
    }
  }

  private val table: DataTable<StageData>

  private var stageId: StagesDataId? = null

  private var jobId: Int? = null

  init {
    val settings = SparkToolwindowSettings.getInstance()

    val stagesColumnSettings = if (viewType == Type.FULL) settings.stagesFullColumnSettings else settings.stagesShortColumnSettings

    val columnModel = DataTableColumnModel(StageData.renderableColumns, stagesColumnSettings)
    val tableModel = DataTableModel(null, columnModel)

    table = DataTableCreator.create(tableModel, EnumSet.of(TableExtensionType.SPEED_SEARCH,
                                                           TableExtensionType.RENDERERS_SETTER,
                                                           TableExtensionType.COLUMNS_FITTER,
                                                           TableExtensionType.LOADING_INDICATOR,
                                                           TableExtensionType.ERROR_HANDLER,
                                                           TableExtensionType.SELECTION_PRESERVER,
                                                           TableExtensionType.SMART_RESIZER))

    Disposer.register(this, table)

    table.tableHeader.border = BorderFactory.createEmptyBorder()

    if (viewType == Type.FULL) {
      setupRowFilter(table, tableModel, columnModel)
    }

    table.addMouseListener(object : MouseAdapter() {
      override fun mouseClicked(e: MouseEvent) {
        if (e.clickCount == 2 && table.selectedRow != -1) {
          showTasksForSelectedStage(getSelectedStage())
        }
      }
    })

    setupStagesTablePopupMenu()

    detailsTasksSplitter = OnePixelSplitter(false, 0.5f)

    setupRowFilter(table, tableModel, columnModel)

    table.selectionModel.addListSelectionListener(stagesSelectionListener)

    val scrollPane = MaterialJBScrollPane(table)
    scrollPane.border = BorderFactory.createEmptyBorder()

    stageDetailsSplitter = OnePixelSplitter(false, settings.getStagesSplitterProportion(connectionId))

    stageDetailsSplitter.addPropertyChangeListener(Splitter.PROP_PROPORTION) {
      if (stageId != null) {
        settings.setStagesSplitterProportion(connectionId, stageDetailsSplitter.proportion)
      }
    }

    stageDetailsSplitter.firstComponent =
      SimpleToolWindowPanel(false, true).apply {
        setContent(scrollPane)
        val actionToolbar = createToolbar(table, columnModel)
        actionToolbar.targetComponent = this
        toolbar = actionToolbar.component
      }

    stageDetailsSplitter.secondComponent = detailsTasksSplitter
  }

  override fun setDetailsId(id: AppAttemptId) = setStageId(StagesDataId(id, null))

  fun setStageId(stageId: StagesDataId, jobId: Int? = null) {
    this.stageId = stageId
    this.jobId = jobId

    val stagesModel = dataManager.getStagesModel(stageId)
    table.tableModel.setDataModel(stagesModel)

    TableColumnsFitter.get(table)?.reset()
    TableLoadingDecorator.installOn(table)
    // Auto-select first row.
    TableRowAutoselector.installOn(table)
  }

  private fun updateDetails() {
    val settings = SparkToolwindowSettings.getInstance()

    if (settings.isDetailsShown(connectionId)) {
      showDetailsForSelectedStage(getSelectedStage())
    }

    if (settings.isTasksShown(connectionId)) {
      showTasksForSelectedStage(getSelectedStage())
    }
  }

  override fun getComponent(): JComponent {
    return stageDetailsSplitter
  }

  private fun setupStagesTablePopupMenu() {
    val openAction = DumbAwareAction.create(SMMessagesBundle.message("stages.openTasks")) { showTasksForSelectedStage(getSelectedStage()) }
    PopupHandler.installPopupMenu(table, DefaultActionGroup(
      ActionManager.getInstance().getAction("BdIde.TableEditor.PopupActionGroup") as ActionGroup, Separator(), openAction),
                                  "StagesPageController")
  }

  private fun createToolbar(table: DataTable<StageData>, columnModel: DataTableColumnModel<StageData>): ActionToolbar {

    val settings = SparkToolwindowSettings.getInstance()

    val actions = DefaultActionGroup()

    actions.add(ToolbarVerticalLabelAction.create(SMMessagesBundle.message("applications.tab.stages")))

    val filterAction = DefaultActionGroup(CommonBundle.message("action.text.filter"), null, AllIcons.General.Filter).apply {
      isPopup = true
    }

    for (status in StageStatus.entries) {
      val toggleState = object : DumbAwareToggleAction(status.text, null, IconUtils.getIconForStageStatus(status)) {
        override fun isSelected(e: AnActionEvent) = settings.getSparkConfigOrDefault(connectionId).stageStatuses.contains(status)
        override fun getActionUpdateThread() = ActionUpdateThread.BGT
        override fun setSelected(e: AnActionEvent, state: Boolean) {
          if (state) {
            settings.getSparkConfigOrDefault(connectionId).stageStatuses.add(status)
          }
          else {
            settings.getSparkConfigOrDefault(connectionId).stageStatuses.remove(status)
          }
          table.rowSorter?.allRowsChanged()
          SparkMonitoringUsagesCollector.stateFilterChangedEvent.log(project, StateFilerType.Stage, status.text, state)

          val id = stageId ?: return
          dataManager.updater.invokeRefreshModel(dataManager.getStagesModel(id))
        }
      }

      filterAction.add(toggleState)
    }

    actions.add(filterAction)

    actions.addSeparator()

    val configStoragesColumnsAction = ColumnVisibilitySettings.createAction(
      columnModel.allColumns, if (viewType == Type.FULL) settings.stagesFullColumnSettings else settings.stagesShortColumnSettings
    )
    actions.add(configStoragesColumnsAction)

    actions.addSeparator()

    val toggleDetailsAction = object : ToggleAction(SMMessagesBundle.message("stages.showDetails.title"),
                                                    SMMessagesBundle.message("stages.showDetails.hint"),
                                                    AllIcons.Actions.PreviewDetails), DumbAware {
      override fun isSelected(e: AnActionEvent) = detailsConsole != null
      override fun getActionUpdateThread() = ActionUpdateThread.BGT
      override fun setSelected(e: AnActionEvent, state: Boolean) {

        if (detailsConsole == null) {
          showDetailsForSelectedStage(getSelectedStage())
        }
        else {
          detailsTasksSplitter.firstComponent = null
          Disposer.dispose(detailsConsole!!)
          detailsConsole = null

          stageId ?: return

          settings.setDetailsShown(connectionId, false)
        }

        SparkMonitoringUsagesCollector.blockShownEvent.log(project, StageUIBlockType.StageDetails, isSelected(e))
      }
    }
    actions.add(toggleDetailsAction)

    val toggleTasksAction = object : ToggleAction(SMMessagesBundle.message("stages.showTasks.title"),
                                                  SMMessagesBundle.message("stages.showTasks.hint"),
                                                  BigdatatoolsSparkMonitoringIcons.TasksTable), DumbAware {
      override fun isSelected(e: AnActionEvent) = tasksPageController != null
      override fun getActionUpdateThread() = ActionUpdateThread.BGT
      override fun setSelected(e: AnActionEvent, state: Boolean) {

        if (tasksPageController == null) {
          showTasksForSelectedStage(getSelectedStage())
        }
        else {
          detailsTasksSplitter.secondComponent = null
          Disposer.dispose(tasksPageController!!)
          tasksPageController = null

          stageId ?: return

          settings.setTasksShown(connectionId, false)
        }

        SparkMonitoringUsagesCollector.blockShownEvent.log(project, StageUIBlockType.StageTasks, isSelected(e))
      }
    }
    actions.add(toggleTasksAction)
    actions.addSeparator()
    actions.add(OpenUrlAction({ if (jobId != null) "jobs/job/?id=$jobId" else "stages" }, project,
                              dataManager) { stageId?.applicationId?.toString() })

    return ToolbarUtils.createActionToolbar("BDTSparkStages", actions, false)
  }

  private fun getSelectedStage(): StageData? = table.getSelectedData()

  private fun showTasksForSelectedStage(stage: StageData?) {

    stageId ?: return

    var tasksPageController = this.tasksPageController
    if (tasksPageController == null) {
      tasksPageController = TasksPageController(project, connectionId)
      this.tasksPageController = tasksPageController
      Disposer.register(this, tasksPageController)
      detailsTasksSplitter.secondComponent = tasksPageController.getComponent()
    }

    tasksPageController.setTaskId(if (stage == null) null else TasksDataId(stageId!!.applicationId, stage.id, stage.attemptId))

    val settings = SparkToolwindowSettings.getInstance()
    if (stage != null) {
      settings.setSelectedStageId(connectionId, stage.id.toString())
    }
    settings.setTasksShown(connectionId, true)
  }

  private fun setupRowFilter(table: JTable, tableModel: DataTableModel<StageData>, columnModel: DataTableColumnModel<StageData>) {
    val sorter = TableRowSorter(tableModel)

    val filter = object : RowFilter<DataTableModel<StageData>, Int>() {
      private val statusColumnIndex = columnModel.getModelIndex("status")
      override fun include(entry: Entry<out DataTableModel<StageData>, out Int>): Boolean {
        return SparkToolwindowSettings.getInstance().getSparkConfigOrDefault(connectionId).stageStatuses.contains(
          entry.getValue(statusColumnIndex) as StageStatus)
      }
    }

    sorter.rowFilter = filter
    table.rowSorter = sorter
  }

  private fun showDetailsForSelectedStage(stage: StageData?) {
    var detailsConsole = this.detailsConsole
    if (detailsConsole == null) {
      detailsConsole = ConsoleViewImpl(project, false)
      this.detailsConsole = detailsConsole
      Disposer.register(this, detailsConsole)
      detailsTasksSplitter.firstComponent = detailsConsole.component
    }

    invokeLater {
      detailsConsole.clear()
      if (stage != null) {
        detailsConsole.print(stage.details, ConsoleViewContentType.NORMAL_OUTPUT)
      }
      detailsConsole.scrollTo(0)
    }

    SparkToolwindowSettings.getInstance().setDetailsShown(connectionId, true)
  }

  override fun dispose() {}
}