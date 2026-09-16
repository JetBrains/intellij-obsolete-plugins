package com.jetbrains.bigdatatools.dataproc.toolwindow.controllers

import com.intellij.bigdatatools.coreUi.util.NotificationUtils
import com.intellij.icons.AllIcons
import com.intellij.ide.actions.SmartPopupActionGroup
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.Separator
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.DumbAwareToggleAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.ui.components.JBTextField
import com.jetbrains.bigdatatools.common.monitoring.actions.OpenUrlAction
import com.jetbrains.bigdatatools.common.monitoring.data.model.DataModelFilter
import com.jetbrains.bigdatatools.common.monitoring.data.model.FilterAdapter
import com.jetbrains.bigdatatools.common.monitoring.data.model.ObjectDataModel
import com.jetbrains.bigdatatools.common.monitoring.table.TableEventListener
import com.jetbrains.bigdatatools.common.monitoring.table.extension.TableErrorHandler
import com.jetbrains.bigdatatools.common.monitoring.table.extension.TableExtensionType
import com.jetbrains.bigdatatools.common.monitoring.table.getSelectedData
import com.jetbrains.bigdatatools.common.monitoring.toolwindow.TableWithDetailsMonitoringController
import com.jetbrains.bigdatatools.common.settings.ColumnVisibilitySettings
import com.jetbrains.bigdatatools.common.ui.CustomComponentActionImpl
import com.jetbrains.bigdatatools.common.ui.ToolbarLabelActionImpl
import com.jetbrains.bigdatatools.common.ui.filter.CountFilterPopupComponent
import com.jetbrains.bigdatatools.dataproc.data.DataprocDataManager
import com.jetbrains.bigdatatools.dataproc.model.DataprocClusterInfo
import com.jetbrains.bigdatatools.dataproc.model.DataprocClusterState
import com.jetbrains.bigdatatools.dataproc.model.DataprocClusterState.Companion.isActive
import com.jetbrains.bigdatatools.dataproc.settings.DataprocToolWindowSettings
import com.jetbrains.bigdatatools.dataproc.util.DataprocMessagesBundle
import org.com.jetbrains.bigdatatools.utils.HdfsMessagesBundle
import java.util.EnumSet
import javax.swing.JComponent

class DataprocClusterListController(private val project: Project,
                                    val dataManager: DataprocDataManager,
                                    val showDetails: Boolean = true) : TableWithDetailsMonitoringController<DataprocClusterInfo, String>() {
  private val model: ObjectDataModel<DataprocClusterInfo> = dataManager.clusterModel

  private val deleteClusterAction = object : DumbAwareAction(DataprocMessagesBundle.message("cluster.action.delete"), null,
                                                             AllIcons.Actions.GC) {

    override fun actionPerformed(e: AnActionEvent) {
      try {
        val selectedValue = getSelectedItem()
        val cluster: DataprocClusterInfo = selectedValue ?: return
        val res = Messages.showYesNoDialog(project,
                                           DataprocMessagesBundle.message("action.cluster.remove.confirm.msg", cluster.name),
                                           DataprocMessagesBundle.message("action.confirm.title"),
                                           Messages.getQuestionIcon())
        if (res != Messages.OK)
          return
        dataManager.removeCluster(cluster.name)
      }
      catch (t: Throwable) {
        NotificationUtils.showExceptionMessage(project, t, HdfsMessagesBundle.message("emr.error.remove.cluster"))
      }
    }

    override fun update(e: AnActionEvent) {
      e.presentation.isEnabledAndVisible = getSelectedItem() != null
    }

    override fun getActionUpdateThread() = ActionUpdateThread.BGT
  }

  private val runClusterAction = object : DumbAwareAction(DataprocMessagesBundle.message("cluster.action.start"), null,
                                                          AllIcons.RunConfigurations.TestState.Run) {

    override fun actionPerformed(e: AnActionEvent) {
      try {
        val selectedValue = getSelectedItem()
        val cluster: DataprocClusterInfo = selectedValue ?: return
        val res = Messages.showYesNoDialog(project,
                                           DataprocMessagesBundle.message("action.cluster.start.confirm.msg", cluster.name),
                                           DataprocMessagesBundle.message("action.confirm.title"),
                                           Messages.getQuestionIcon())
        if (res != Messages.OK)
          return
        dataManager.startCluster(cluster.name)
      }
      catch (t: Throwable) {
        NotificationUtils.showExceptionMessage(project, t, HdfsMessagesBundle.message("emr.error.start.cluster"))
      }
    }

    override fun update(e: AnActionEvent) {
      e.presentation.isEnabledAndVisible = getSelectedItem()?.state?.isActive == false
    }

    override fun getActionUpdateThread() = ActionUpdateThread.BGT
  }

  private val stopClusterAction = object : DumbAwareAction(DataprocMessagesBundle.message("cluster.action.stop"), null,
                                                           AllIcons.Actions.Suspend) {

    override fun actionPerformed(e: AnActionEvent) {
      try {
        val selectedValue = getSelectedItem()
        val cluster: DataprocClusterInfo = selectedValue ?: return
        val res = Messages.showYesNoDialog(project,
                                           DataprocMessagesBundle.message("action.cluster.terminate.confirm.msg", cluster.name),
                                           DataprocMessagesBundle.message("action.confirm.title"),
                                           Messages.getQuestionIcon())
        if (res != Messages.OK)
          return
        dataManager.terminateCluster(cluster.name)
      }
      catch (t: Throwable) {
        NotificationUtils.showExceptionMessage(project, t, HdfsMessagesBundle.message("emr.error.stop.cluster"))
      }
    }

    override fun update(e: AnActionEvent) {
      e.presentation.isEnabledAndVisible = getSelectedItem()?.state?.isActive == true
    }

    override fun getActionUpdateThread() = ActionUpdateThread.BGT
  }

  override val detailsController = DataprocClusterTabsController(dataManager, project)

  private val connectionData = dataManager.connectionData

  private val connectionId = connectionData.innerId

  init {
    detailsSplitter.proportion = 0.3f
    init()

    TableErrorHandler.installOn(dataTable, object : TableEventListener {
      override fun onChanged() {
        if (dataTable.rowCount == 0) {
          dataTable.emptyText.clear()
          dataTable.emptyText.appendText(
            HdfsMessagesBundle.message("emr.connection.warning.no.clusters.desc.window", connectionData.region))
        }
      }
    })
  }

  override fun getComponent(): JComponent = if (showDetails)
    super.getComponent()
  else
    detailsSplitter.firstComponent


  override fun getToolbarTitle() = DataprocMessagesBundle.message("cluster.tab.name")

  override fun getColumnSettings(): ColumnVisibilitySettings = DataprocToolWindowSettings.getInstance().clustersColumnSettings

  override fun getRenderableColumns() = DataprocClusterInfo.renderableColumns

  override fun getDataModel() = model

  override fun showColumnFilter(): Boolean = true

  override fun getTableExtensions(): EnumSet<TableExtensionType> {
    val tableExtensions = super.getTableExtensions()
    tableExtensions.remove(TableExtensionType.ERROR_HANDLER)
    return tableExtensions
  }

  override fun getAdditionalActions(): List<AnAction> = listOf(
    Separator.create(),
    runClusterAction,
    stopClusterAction,
    deleteClusterAction,
    Separator.create(),
    OpenUrlAction(dataManager) { "/clusters?project=${connectionData.projectId ?: ""}" },
  )

  override fun indexToDetailId(row: Int) = dataTable.getDataAt(row)?.name ?: ""

  override fun saveSelectedItem() {
    DataprocToolWindowSettings.getInstance().saveSelectedCluster(connectionId,
                                                                 dataTable.getSelectedData()?.id ?: "")
  }

  override fun createTopLeftToolbarActions(): List<AnAction> {
    val statusFilter = object : SmartPopupActionGroup() {
      override fun isDumbAware(): Boolean = true
    }

    statusFilter.templatePresentation.text = HdfsMessagesBundle.message("emr.cluster.filter")
    statusFilter.templatePresentation.icon = AllIcons.General.Filter

    val settings = DataprocToolWindowSettings.getInstance()

    for (status in DataprocClusterState.entries) {
      @Suppress("HardCodedStringLiteral")
      val text = status.title

      val toggleState = object : DumbAwareToggleAction(text, null, null) {
        override fun isSelected(e: AnActionEvent) = settings.customClusterStates.contains(status)
        override fun getActionUpdateThread() = ActionUpdateThread.BGT
        override fun setSelected(e: AnActionEvent, state: Boolean) {
          val filters = dataTable.tableModel.getDataModel()?.filters

          if (state) {
            settings.customClusterStates.add(status)
            filters?.setFilter(DataModelFilter(DataprocClusterInfo.STATES_FILTER, settings.customClusterStates.joinToString(",")))
          }
          else {
            settings.customClusterStates.remove(status)
            if (settings.customClusterStates.isEmpty()) {
              filters?.removeFilter(DataprocClusterInfo.STATES_FILTER)
            }
            else {
              filters?.setFilter(DataModelFilter(DataprocClusterInfo.STATES_FILTER, settings.customClusterStates.joinToString(",")))
            }
          }
          dataManager.updater.invokeRefreshModel(dataManager.clusterModel)
        }
      }

      statusFilter.add(toggleState)
    }

    val config = settings.getOrCreateConfig(connectionId)

    val userText = JBTextField(config.textFilter, 8)

    FilterAdapter.install(dataTable.tableModel, userText, DataprocClusterInfo.TEXT_FILTER) { userQuery ->
      config.textFilter = userQuery
      dataManager.updater.invokeRefreshModel(dataManager.clusterModel)
    }

    val countFilter = CountFilterPopupComponent(HdfsMessagesBundle.message("emr.cluster.filter.limit"), config.clusterLimit)
    FilterAdapter.install(dataTable.tableModel, countFilter, DataprocClusterInfo.LIMIT_FILTER) { limit ->
      config.clusterLimit = limit
      dataManager.updater.invokeRefreshModel(dataManager.clusterModel)
    }

    return listOf(ToolbarLabelActionImpl(HdfsMessagesBundle.message("emr.filter.text")),
                  CustomComponentActionImpl(userText),
                  statusFilter,
                  CustomComponentActionImpl(countFilter))
  }
}
