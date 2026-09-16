package com.jetbrains.spark.monitoring.ui.pages

import com.intellij.bigdatatools.coreUi.util.MessagesBundle
import com.intellij.icons.AllIcons
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.Separator
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.DumbAwareToggleAction
import com.intellij.openapi.project.Project
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.SearchTextField
import com.jetbrains.bigdatatools.common.monitoring.data.listener.DataModelListener
import com.jetbrains.bigdatatools.common.monitoring.data.model.FilterAdapter
import com.jetbrains.bigdatatools.common.monitoring.table.DataTable
import com.jetbrains.bigdatatools.common.monitoring.toolwindow.AbstractTableController
import com.jetbrains.bigdatatools.common.settings.ColumnVisibilitySettings
import com.jetbrains.bigdatatools.common.table.getColumnByIdentifier
import com.jetbrains.bigdatatools.common.table.renderers.LinkRenderer
import com.jetbrains.bigdatatools.common.ui.CustomComponentActionImpl
import com.jetbrains.bigdatatools.common.ui.filter.CountFilterPopupComponent
import com.jetbrains.bigdatatools.common.ui.filter.DateFilterPopupComponent
import com.jetbrains.spark.monitoring.data.ApplicationStatus
import com.jetbrains.spark.monitoring.data.PresentableApplicationInfo
import com.jetbrains.spark.monitoring.models.SparkDataManager
import com.jetbrains.spark.monitoring.settings.SparkToolwindowSettings
import com.jetbrains.spark.monitoring.statistics.SparkMonitoringUsagesCollector
import com.jetbrains.spark.monitoring.statistics.StateFilerType
import com.jetbrains.spark.monitoring.statistics.ToolbarActionType
import com.jetbrains.spark.monitoring.ui.utils.IconUtils
import com.jetbrains.spark.monitoring.util.SMMessagesBundle
import javax.swing.event.DocumentEvent

class SparkApplicationController(
  private val project: Project,
  val dataManager: SparkDataManager,
  val source: SparkAppControllerSource) : AbstractTableController<PresentableApplicationInfo>() {

  val connectionId = dataManager.connectionData.innerId

  /** Flag that shows that statistics about apps count already sent. */
  private var countOnConnectionGot = false

  private val searchTextField = SearchTextField(false).apply {
    addDocumentListener(object : DocumentAdapter() {
      override fun textChanged(e: DocumentEvent) {
        val config = SparkToolwindowSettings.getInstance().getSparkConfigOrDefault(dataManager.connectionData.innerId)
        config.appFilter = this@apply.text
        dataManager.updater.invokeRefreshModel(dataManager.applications)
      }
    })

    text = SparkToolwindowSettings.getInstance().getSparkConfigOrDefault(dataManager.connectionData.innerId).appFilter
  }


  init {
    init()

    val applicationsModel = dataManager.applications

    if (!countOnConnectionGot) {
      // Statistics listener
      applicationsModel.addListener(object : DataModelListener {
        override fun onChanged() {
          val data = applicationsModel.data ?: emptyList()
          SparkMonitoringUsagesCollector.logCollectedEvent(project,
                                                           data.size,
                                                           data.count { it.status == ApplicationStatus.RUNNING })
          applicationsModel.removeListener(this)
          countOnConnectionGot = true
        }
      })
    }
  }

  override fun getColumnSettings(): ColumnVisibilitySettings = SparkToolwindowSettings.getInstance().applicationsColumnSettings
  override fun getRenderableColumns() = PresentableApplicationInfo.renderableColumns
  override fun getDataModel() = dataManager.applications
  override fun showColumnFilter(): Boolean = false

  override fun customTableInit(table: DataTable<PresentableApplicationInfo>) {
    val column = table.getColumnByIdentifier(PresentableApplicationInfo::appId.name) ?: return
    LinkRenderer.installOnColumn(table, column).apply {
      onClick = { row, _ ->
        val appAttemptId = table.getDataAt(row)
        appAttemptId?.let {
          source.open(project, it)

        }
      }
    }
  }

  override fun createTopLeftToolbarActions(): List<AnAction> {
    val settings = SparkToolwindowSettings.getInstance()
    val config = settings.getSparkConfigOrDefault(connectionId)

    val countFilter = CountFilterPopupComponent(SMMessagesBundle.message("applications.filter.limit"), config.applicationsLimit)
    FilterAdapter.install(dataTable.tableModel, countFilter, PresentableApplicationInfo.LIMIT_FILTER) { limit ->
      SparkToolwindowSettings.getInstance().getSparkConfigOrDefault(connectionId).applicationsLimit = limit
      dataManager.updater.invokeRefreshModel(dataManager.applications)
    }

    val startedFilter = DateFilterPopupComponent(SMMessagesBundle.message("applications.filter.started"),
                                                 config.applicationStartedPeriodType,
                                                 config.applicationsStartedBegin,
                                                 config.applicationsStartedEnd)
    FilterAdapter.install(dataTable.tableModel, startedFilter, PresentableApplicationInfo.STARTED_BEGIN_FILTER,
                          PresentableApplicationInfo.STARTED_END_FILTER) { periodType, from, to ->
      val sparkConfig = SparkToolwindowSettings.getInstance().getSparkConfigOrDefault(connectionId)
      sparkConfig.applicationStartedPeriodType = periodType
      sparkConfig.applicationsStartedBegin = from
      sparkConfig.applicationsStartedEnd = to
      dataManager.updater.invokeRefreshModel(dataManager.applications)
    }

    val finishedFilter = DateFilterPopupComponent(SMMessagesBundle.message("applications.filter.finished"),
                                                  config.applicationFinishedPeriodType,
                                                  config.applicationsFinishedBegin,
                                                  config.applicationsFinishedEnd)
    FilterAdapter.install(dataTable.tableModel, finishedFilter, PresentableApplicationInfo.FINISHED_BEGIN_FILTER,
                          PresentableApplicationInfo.FINISHED_END_FILTER) { periodType, from, to ->
      val sparkConfig = SparkToolwindowSettings.getInstance().getSparkConfigOrDefault(connectionId)
      sparkConfig.applicationFinishedPeriodType = periodType
      sparkConfig.applicationsFinishedBegin = from
      sparkConfig.applicationsFinishedEnd = to
      dataManager.updater.invokeRefreshModel(dataManager.applications)
    }


    val stateFilterAction = DefaultActionGroup(SMMessagesBundle.message("action.text.filter.state"), null, AllIcons.General.Filter).apply {
      isPopup = true
    }

    for (status in ApplicationStatus.entries) {
      val toggleState = object : DumbAwareToggleAction(status.text, null, IconUtils.getIconForApplicationStatus(status)) {
        override fun isSelected(e: AnActionEvent) = settings.getSparkConfigOrDefault(connectionId).applicationStatuses.contains(status)
        override fun getActionUpdateThread() = ActionUpdateThread.BGT
        override fun setSelected(e: AnActionEvent, state: Boolean) {
          if (state) {
            settings.getSparkConfigOrDefault(connectionId).applicationStatuses.add(status)
          }
          else {
            settings.getSparkConfigOrDefault(connectionId).applicationStatuses.remove(status)
          }

          // For application is only 2 states - COMPLETE and RUNNING and if we disabling both, than all records will be selected.
          // To prevent unselecting of all filter buttons (which is unclean for user) we will restore selection.
          if (settings.getSparkConfigOrDefault(connectionId).applicationStatuses.isEmpty()) {
            settings.getSparkConfigOrDefault(connectionId).applicationStatuses.add(
              if (status == ApplicationStatus.COMPLETE) ApplicationStatus.RUNNING else ApplicationStatus.COMPLETE)
          }

          SparkMonitoringUsagesCollector.stateFilterChangedEvent.log(dataManager.project, StateFilerType.Application, status.text, state)
          settings.refreshAllApplications()
        }
      }

      //   actions.add(toggleState)
      stateFilterAction.add(toggleState)
    }

    stateFilterAction.addSeparator()
    stateFilterAction.add(object : DumbAwareToggleAction(SMMessagesBundle.message("filter.app.run.by.me"), null, null) {
      override fun isSelected(e: AnActionEvent) = settings.getSparkConfigOrDefault(connectionId).showOnlyMyTasks
      override fun getActionUpdateThread() = ActionUpdateThread.BGT
      override fun setSelected(e: AnActionEvent, state: Boolean) {
        settings.getSparkConfigOrDefault(connectionId).showOnlyMyTasks = state
        settings.refreshAllApplications()
      }
    })

    val openInBrowserAction = DumbAwareAction.create(MessagesBundle.message("open.in.browser.title"), AllIcons.General.Web) {
      BrowserUtil.browse("${dataManager.getRealUrl()}/applications")
      SparkMonitoringUsagesCollector.toolbarActionInvokedEvent.log(dataManager.project, ToolbarActionType.OpenUrl)
    }


    return listOf(CustomComponentActionImpl(searchTextField),
                  CustomComponentActionImpl(countFilter),
                  CustomComponentActionImpl(startedFilter),
                  CustomComponentActionImpl(finishedFilter),
                  stateFilterAction,
                  createColumnFilterAction(),
                  Separator.create(),
                  openInBrowserAction)
  }

  override fun createTopRightToolbarActions() = source.rightToolbarActions
}
