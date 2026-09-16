package com.jetbrains.spark.monitoring.ui.pages

import com.intellij.CommonBundle
import com.intellij.bigdatatools.coreUi.table.renderers.LinkArrayRendering
import com.intellij.execution.impl.ConsoleViewImpl
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionToolbar
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.project.DumbAwareToggleAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.openapi.ui.Splitter
import com.intellij.openapi.util.Disposer
import com.intellij.ui.OnePixelSplitter
import com.jetbrains.bigdatatools.common.monitoring.table.DataTable
import com.jetbrains.bigdatatools.common.monitoring.table.DataTableCreator
import com.jetbrains.bigdatatools.common.monitoring.table.extension.TableColumnsFitter
import com.jetbrains.bigdatatools.common.monitoring.table.extension.TableExtensionType
import com.jetbrains.bigdatatools.common.monitoring.table.extension.TableLoadingDecorator
import com.jetbrains.bigdatatools.common.monitoring.table.extension.TableRenderersSetter
import com.jetbrains.bigdatatools.common.monitoring.table.getSelectedData
import com.jetbrains.bigdatatools.common.monitoring.table.model.DataTableColumnModel
import com.jetbrains.bigdatatools.common.monitoring.table.model.DataTableModel
import com.jetbrains.bigdatatools.common.monitoring.toolwindow.DetailsMonitoringController
import com.jetbrains.bigdatatools.common.settings.ColumnVisibilitySettings
import com.jetbrains.bigdatatools.common.table.DecoratableDataTableModel
import com.jetbrains.bigdatatools.common.table.MaterialJBScrollPane
import com.jetbrains.bigdatatools.common.table.renderers.LinkArrayRenderer
import com.jetbrains.bigdatatools.common.ui.ToolbarVerticalLabelAction
import com.jetbrains.bigdatatools.common.ui.setCenterComponent
import com.jetbrains.bigdatatools.common.ui.setLineStartComponent
import com.jetbrains.bigdatatools.common.util.ToolbarUtils
import com.jetbrains.spark.monitoring.data.AppAttemptId
import com.jetbrains.spark.monitoring.data.SqlInfo
import com.jetbrains.spark.monitoring.data.SqlInfoStatus
import com.jetbrains.spark.monitoring.models.SparkDataManager
import com.jetbrains.spark.monitoring.models.SqlDataId
import com.jetbrains.spark.monitoring.settings.SparkToolwindowSettings
import com.jetbrains.spark.monitoring.statistics.SparkMonitoringUsagesCollector
import com.jetbrains.spark.monitoring.statistics.StateFilerType
import com.jetbrains.spark.monitoring.ui.utils.IconUtils
import com.jetbrains.spark.monitoring.ui.utils.OpenUrlAction
import com.jetbrains.spark.monitoring.util.SMMessagesBundle
import java.awt.BorderLayout
import java.util.EnumSet
import javax.swing.BorderFactory
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JTable
import javax.swing.RowFilter
import javax.swing.event.ListSelectionEvent
import javax.swing.table.TableCellRenderer
import javax.swing.table.TableRowSorter

class SqlPageController(project: Project,
                        private val connectionId: String,
                        private val sparkAppInfoController: SparkAppInfoController) : DetailsMonitoringController<AppAttemptId> {
  private val dataManager = SparkDataManager.getInstance(connectionId, project) ?: error("Data Manager is not initialized")

  private val panel = SimpleToolWindowPanel(false, true)
  private val splitter = OnePixelSplitter(false, SparkToolwindowSettings.getInstance().getSqlSplitterProportion(connectionId))
  private val detailsConsole = ConsoleViewImpl(project, false)

  val table: DataTable<SqlInfo>

  private var sqlId: SqlDataId? = null

  init {
    val sqlColumnSettings = SparkToolwindowSettings.getInstance().sqlColumnSettings

    val columnModel = DataTableColumnModel(SqlInfo.renderableColumns, columnSettings = sqlColumnSettings)
    val tableModel = DataTableModel(null, columnModel)

    table = DataTableCreator.create(tableModel, EnumSet.of(TableExtensionType.SPEED_SEARCH,
                                                           TableExtensionType.COLUMNS_FITTER,
                                                           TableExtensionType.LOADING_INDICATOR,
                                                           TableExtensionType.ERROR_HANDLER,
                                                           TableExtensionType.SELECTION_PRESERVER,
                                                           TableExtensionType.SMART_RESIZER))

    TableRenderersSetter.installOn(table, sqlColumnSettings, ::jobsRendererSupplier)

    Disposer.register(this, table)

    table.tableHeader.border = BorderFactory.createEmptyBorder()
    setupSelectionListener(table)
    setupRowFilter(table, tableModel, columnModel)

    Disposer.register(this, detailsConsole)

    splitter.firstComponent = MaterialJBScrollPane(table)
    splitter.secondComponent = createDetailsPanel()

    val toolbar = createToolbar(table, project, columnModel).apply { targetComponent = panel }

    panel.setContent(splitter)
    panel.toolbar = toolbar.component

    splitter.addPropertyChangeListener(Splitter.PROP_PROPORTION) {
      SparkToolwindowSettings.getInstance().setSqlSplitterProportion(connectionId, splitter.proportion)
    }
  }

  private fun createDetailsPanel(): JPanel {
    val actions = DefaultActionGroup(ToolbarVerticalLabelAction.create(SMMessagesBundle.message("applications.tab.details")))
    val toolbar = ToolbarUtils.createActionToolbar(detailsConsole.component, "BDTSparkSqlDetails", actions, horizontal = false)

    return JPanel(BorderLayout()).apply {
      setCenterComponent(detailsConsole.component)
      setLineStartComponent(toolbar.component)
    }
  }

  private fun jobsRendererSupplier(annotations: Array<out Annotation>,
                                   @Suppress("UNUSED_PARAMETER") tableModel: DecoratableDataTableModel?): TableCellRenderer? {
    return if (annotations.find { it is LinkArrayRendering } != null) {
      LinkArrayRenderer().apply {
        onClick = ::onJobsClick
      }
    }
    else {
      null
    }
  }

  // Expected that jobs list will be a string like "[2]" or "[2,3,4]"
  private fun onJobsClick(jobId: String) {
    val id = jobId.toIntOrNull() ?: return
    sparkAppInfoController.selectByLabel(SparkAppInfoController.JOBS_LABEL)
    sparkAppInfoController.jobsPageController.focusOnJob(id)
  }

  override fun setDetailsId(id: AppAttemptId) = setSqlId(SqlDataId(id))

  private fun setSqlId(sqlId: SqlDataId) {
    this.sqlId = sqlId
    val sqlsModel = dataManager.getSqlModel(sqlId.applicationId)
    table.tableModel.setDataModel(sqlsModel)

    TableColumnsFitter.get(table)?.reset()
    TableLoadingDecorator.installOn(table)
  }

  private fun setupRowFilter(table: JTable, tableModel: DataTableModel<SqlInfo>, columnModel: DataTableColumnModel<SqlInfo>) {
    val sorter = TableRowSorter(tableModel)

    val filter = object : RowFilter<DataTableModel<SqlInfo>, Int>() {
      private val statusColumnIndex = columnModel.getModelIndex("status")
      override fun include(entry: Entry<out DataTableModel<SqlInfo>, out Int>) =
        SparkToolwindowSettings.getInstance().getSparkConfigOrDefault(connectionId).sqlStatuses.contains(
          entry.getValue(statusColumnIndex) as SqlInfoStatus)
    }

    sorter.rowFilter = filter
    table.rowSorter = sorter
  }

  private fun createToolbar(table: JTable, project: Project, columnModel: DataTableColumnModel<SqlInfo>): ActionToolbar {
    val settings = SparkToolwindowSettings.getInstance()

    val actions = DefaultActionGroup()

    actions.add(ToolbarVerticalLabelAction.create(SMMessagesBundle.message("applications.tab.sql")))

    val filterAction = DefaultActionGroup(CommonBundle.message("action.text.filter"), null, AllIcons.General.Filter).apply {
      isPopup = true
    }

    for (status in SqlInfoStatus.entries) {
      val toggleState = object : DumbAwareToggleAction(status.text, null, IconUtils.getIconForSqlStatus(status)) {
        override fun isSelected(e: AnActionEvent) = settings.getSparkConfigOrDefault(connectionId).sqlStatuses.contains(status)
        override fun getActionUpdateThread() = ActionUpdateThread.BGT
        override fun setSelected(e: AnActionEvent, state: Boolean) {
          if (state) {
            settings.getSparkConfigOrDefault(connectionId).sqlStatuses.add(status)
          }
          else {
            settings.getSparkConfigOrDefault(connectionId).sqlStatuses.remove(status)
          }
          table.rowSorter?.allRowsChanged()
          SparkMonitoringUsagesCollector.stateFilterChangedEvent.log(project, StateFilerType.Sql, status.text, state)
          val id = sqlId ?: return
          dataManager.updater.invokeRefreshModel(dataManager.getSqlModel(id.applicationId))
        }
      }

      filterAction.add(toggleState)
    }

    actions.add(filterAction)

    actions.addSeparator()

    val configSqlColumnsAction = ColumnVisibilitySettings.createAction(columnModel.allColumns, settings.sqlColumnSettings)

    actions.add(configSqlColumnsAction)
    actions.addSeparator()
    actions.add(OpenUrlAction({ "SQL" }, project, dataManager) { sqlId?.applicationId?.toString() })

    return ToolbarUtils.createActionToolbar("BDTSparkSQL", actions, false)
  }

  private fun setupSelectionListener(table: DataTable<SqlInfo>) {
    table.selectionModel.addListSelectionListener { e: ListSelectionEvent ->

      if (e.valueIsAdjusting) return@addListSelectionListener

      detailsConsole.clear()

      val sqlInfo = table.getSelectedData() ?: return@addListSelectionListener
      detailsConsole.print(sqlInfo.descriptionFull, ConsoleViewContentType.NORMAL_OUTPUT)
      detailsConsole.scrollTo(0)
    }
  }

  override fun getComponent(): JComponent {
    return panel
  }

  override fun dispose() {}
}