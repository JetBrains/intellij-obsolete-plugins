package com.intellij.bigdatatools.emr.toolwindow.controllers

import com.intellij.bigdatatools.coreUi.util.NotificationUtils
import com.intellij.bigdatatools.emr.data.EmrDataManager
import com.intellij.bigdatatools.emr.model.EmrClusterInfo
import com.intellij.bigdatatools.emr.model.EmrClusterState
import com.intellij.bigdatatools.emr.settings.EmrToolWindowSettings
import com.intellij.bigdatatools.emr.util.EmrMessagesBundle
import com.intellij.icons.AllIcons
import com.intellij.ide.actions.SmartPopupActionGroup
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
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
import com.jetbrains.bigdatatools.common.ui.ToolbarGreyLabelActionImpl
import com.jetbrains.bigdatatools.common.ui.ToolbarLabelActionImpl
import com.jetbrains.bigdatatools.common.ui.filter.CountFilterPopupComponent
import org.com.jetbrains.bigdatatools.utils.HdfsMessagesBundle
import software.amazon.awssdk.services.emr.model.ClusterState
import java.util.EnumSet
import javax.swing.JComponent

class EmrClusterController(private val project: Project,
                           val dataManager: EmrDataManager,
                           val showDetails: Boolean = true) : TableWithDetailsMonitoringController<EmrClusterInfo, String>() {
  private val model: ObjectDataModel<EmrClusterInfo> = dataManager.clusterModel

  private val stopClusterAction = object : DumbAwareAction(EmrMessagesBundle.message("cluster.action.stop"), null,
                                                           AllIcons.Actions.Suspend) {

    override fun actionPerformed(e: AnActionEvent) {
      try {
        val selectedValue = getSelectedItem()
        val cluster: EmrClusterInfo = selectedValue ?: return
        val res = Messages.showYesNoDialog(project,
                                           HdfsMessagesBundle.message("emr.cluster.terminate.cluster.message", cluster.name),
                                           HdfsMessagesBundle.message("emr.cluster.terminate.cluster.title"),
                                           Messages.getQuestionIcon())
        if (res != Messages.OK)
          return
        dataManager.terminateCluster(cluster.id)
      }
      catch (t: Throwable) {
        NotificationUtils.showExceptionMessage(project, t, HdfsMessagesBundle.message("emr.error.stop.cluster"))
      }
    }

    override fun update(e: AnActionEvent) {
      e.presentation.isEnabled = getSelectedItem()?.state?.isRunning() == true
    }

    override fun getActionUpdateThread() = ActionUpdateThread.BGT
  }

  override val detailsController = EmrClusterTabsController(dataManager, project)

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

  override fun getColumnSettings(): ColumnVisibilitySettings = EmrToolWindowSettings.getInstance().clustersColumnSettings
  override fun getRenderableColumns() = EmrClusterInfo.renderableColumns

  override fun getDataModel() = model

  override fun showColumnFilter(): Boolean = false

  override fun getTableExtensions(): EnumSet<TableExtensionType> {
    val tableExtensions = super.getTableExtensions()
    tableExtensions.remove(TableExtensionType.ERROR_HANDLER)
    return tableExtensions
  }

  override fun createTopRightToolbarActions(): List<AnAction> = listOf(
    ToolbarGreyLabelActionImpl(EmrMessagesBundle.message("tab.name.clusters")),
  )

  override fun indexToDetailId(row: Int) = dataTable.getDataAt(row)?.id ?: ""

  override fun saveSelectedItem() {
    EmrToolWindowSettings.getInstance().saveSelectedCluster(connectionId,
                                                            dataTable.getSelectedData()?.id ?: "")
  }

  override fun createTopLeftToolbarActions(): List<AnAction> {
    val statusFilter = object : SmartPopupActionGroup() {
      override fun isDumbAware(): Boolean = true
    }

    statusFilter.templatePresentation.text = HdfsMessagesBundle.message("emr.cluster.filter")
    statusFilter.templatePresentation.icon = AllIcons.General.Filter

    val settings = EmrToolWindowSettings.getInstance()

    for (status in EmrClusterState.supportedValues) {
      @Suppress("HardCodedStringLiteral")
      val text = status.title

      val toggleState = object : DumbAwareToggleAction(text, null, null) {
        override fun isSelected(e: AnActionEvent) = settings.customClusterStates.contains(status)
        override fun getActionUpdateThread() = ActionUpdateThread.BGT
        override fun setSelected(e: AnActionEvent, state: Boolean) {
          val filters = dataTable.tableModel.getDataModel()?.filters

          if (state) {
            settings.customClusterStates.add(status)
            filters?.setFilter(DataModelFilter(EmrClusterInfo.STATES_FILTER, settings.customClusterStates.joinToString(",")))
          }
          else {
            settings.customClusterStates.remove(status)
            if (settings.customClusterStates.isEmpty()) {
              filters?.removeFilter(EmrClusterInfo.STATES_FILTER)
            }
            else {
              filters?.setFilter(DataModelFilter(EmrClusterInfo.STATES_FILTER, settings.customClusterStates.joinToString(",")))
            }
          }
          dataManager.updater.invokeRefreshModel(dataManager.clusterModel)
        }
      }

      statusFilter.add(toggleState)
    }

    val config = settings.getOrCreateConfig(connectionId)

    val userText = JBTextField(config.clusterFilter, 8)

    FilterAdapter.install(dataTable.tableModel, userText, EmrClusterInfo.TEXT_FILTER) { userQuery ->
      config.clusterFilter = userQuery
      dataManager.updater.invokeRefreshModel(dataManager.clusterModel)
    }

    val countFilter = CountFilterPopupComponent(HdfsMessagesBundle.message("emr.cluster.filter.limit"), config.clusterLimit)
    FilterAdapter.install(dataTable.tableModel, countFilter, EmrClusterInfo.LIMIT_FILTER) { limit ->
      config.clusterLimit = limit
      dataManager.updater.invokeRefreshModel(dataManager.clusterModel)
    }

    return listOf(ToolbarLabelActionImpl(HdfsMessagesBundle.message("emr.filter.text")),
                  CustomComponentActionImpl(userText),
                  statusFilter,
                  CustomComponentActionImpl(countFilter),
                  stopClusterAction,
                  OpenUrlAction(dataManager) { "#cluster-list:" })
  }
}

fun ClusterState?.isRunning() =
  this in setOf(ClusterState.STARTING, ClusterState.RUNNING, ClusterState.BOOTSTRAPPING, ClusterState.WAITING)