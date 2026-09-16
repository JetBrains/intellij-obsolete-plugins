package com.jetbrains.spark.monitoring.ui.pages

import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.Separator
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.util.Disposer
import com.intellij.ui.awt.RelativePoint
import com.jetbrains.bigdatatools.common.monitoring.data.model.ObjectDataModel
import com.jetbrains.bigdatatools.common.monitoring.table.DataTable
import com.jetbrains.bigdatatools.common.monitoring.table.extension.TableColumnsFitter
import com.jetbrains.bigdatatools.common.monitoring.table.extension.TableExtensionType
import com.jetbrains.bigdatatools.common.monitoring.table.extension.TableLoadingDecorator
import com.jetbrains.bigdatatools.common.monitoring.toolwindow.DetailsMonitoringController
import com.jetbrains.bigdatatools.common.monitoring.toolwindow.TableWithDetailsMonitoringController
import com.jetbrains.bigdatatools.common.table.getColumnByIdentifier
import com.jetbrains.bigdatatools.common.table.renderers.LinkRenderer
import com.jetbrains.spark.monitoring.data.AppAttemptId
import com.jetbrains.spark.monitoring.data.ExecutorSummary
import com.jetbrains.spark.monitoring.models.SparkDataManager
import com.jetbrains.spark.monitoring.settings.SparkToolwindowSettings
import com.jetbrains.spark.monitoring.ui.utils.OpenUrlAction
import com.jetbrains.spark.monitoring.util.SMMessagesBundle
import com.jetbrains.spark.monitoring.util.SparkUrlOpener
import java.awt.Rectangle
import java.util.EnumSet

class SparkExecutorsController(private val project: Project,
                               connectionId: String) : TableWithDetailsMonitoringController<ExecutorSummary, AppAttemptId>(),
                                                       DetailsMonitoringController<AppAttemptId> {
  private val dataManager = SparkDataManager.getInstance(connectionId, project) ?: error("Data Manager is not initialized")
  private var selectedId: AppAttemptId? = null

  override val detailsController = SparkAggregateExecutorsController(dataManager)

  init {
    init()
  }

  override fun setDetailsId(id: AppAttemptId) {
    selectedId = id

    val model = getDataModel() ?: return
    dataTable.tableModel.setDataModel(model)

    TableColumnsFitter.get(dataTable)?.reset()
    TableLoadingDecorator.installOn(dataTable)

    decoratedTableComponent.revalidate()
    decoratedTableComponent.repaint()
  }

  override fun getTableExtensions(): EnumSet<TableExtensionType> = EnumSet.copyOf(
    super.getTableExtensions() - TableExtensionType.LOADING_INDICATOR)

  override fun getColumnSettings() = SparkToolwindowSettings.getInstance().executorsColumnSettings

  override fun getRenderableColumns() = ExecutorSummary.renderableColumns
  override fun getDataModel(): ObjectDataModel<ExecutorSummary>? {
    val applicationId = selectedId ?: return null
    return dataManager.getExecutorsModel(applicationId)
  }

  override fun customTableInit(table: DataTable<ExecutorSummary>) {
    val listener: (String, Boolean) -> Unit = { s, b ->
      initLogsLinkRender(table)
    }
    Disposer.register(this) {
      SparkToolwindowSettings.getInstance().executorsColumnSettings.onColumnVisibilityChanged.minusAssign(listener)
    }
    SparkToolwindowSettings.getInstance().executorsColumnSettings.onColumnVisibilityChanged.plusAssign(listener)
    initLogsLinkRender(table)
  }

  override fun indexToDetailId(row: Int) = selectedId

  override fun saveSelectedItem() {}

  override fun showColumnFilter(): Boolean = true

  override fun getToolbarTitle() = SMMessagesBundle.message("applications.tab.executors")

  override fun getAdditionalActions(): List<AnAction> = listOf(
    Separator(),
    OpenUrlAction({ "executors" }, project, dataManager) { selectedId?.toString() }
  )

  private fun initLogsLinkRender(table: DataTable<ExecutorSummary>) {
    val column = table.getColumnByIdentifier(ExecutorSummary::logs.name) ?: return
    LinkRenderer.installOnColumn(table, column).apply {
      onClick = { row, column ->
        val executorSummary = table.getDataAt(row)
        val cellEditor = table.getCellRect(row, column, true)
        openLog(executorSummary, cellEditor)
      }
    }
  }

  private fun openLog(executorSummary: ExecutorSummary?, rectangle: Rectangle) {
    val logMap = executorSummary?.executorLogs ?: emptyMap()
    val actions = logMap.entries.map { (logName, url) ->
      DumbAwareAction.create(logName) {
        SparkUrlOpener.openUrl(url, dataManager)
      }
    }
    val popupMenu = JBPopupFactory.getInstance().createActionGroupPopup(null, DefaultActionGroup(actions),
                                                                        DataManager.getInstance().getDataContext(dataTable),
                                                                        JBPopupFactory.ActionSelectionAid.SPEEDSEARCH, false)
    popupMenu.show(RelativePoint(dataTable, rectangle.location))
  }
}