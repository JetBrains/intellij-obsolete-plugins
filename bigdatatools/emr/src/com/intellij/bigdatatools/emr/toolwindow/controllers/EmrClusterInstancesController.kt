package com.intellij.bigdatatools.emr.toolwindow.controllers

import com.intellij.bigdatatools.emr.data.EmrDataManager
import com.intellij.bigdatatools.emr.model.EmrClusterInstanceInfo
import com.intellij.bigdatatools.emr.settings.EmrToolWindowSettings
import com.intellij.bigdatatools.emr.util.EmrMessagesBundle
import com.intellij.bigdatatools.sftp.icons.BigdatatoolsSftpIcons
import com.intellij.icons.AllIcons
import com.intellij.ide.actions.SmartPopupActionGroup
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.Separator
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.DumbAwareToggleAction
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBTextField
import com.jetbrains.bigdatatools.common.monitoring.actions.OpenUrlAction
import com.jetbrains.bigdatatools.common.monitoring.data.model.DataModelFilter
import com.jetbrains.bigdatatools.common.monitoring.data.model.FilterAdapter
import com.jetbrains.bigdatatools.common.monitoring.toolwindow.DetailsTableMonitoringController
import com.jetbrains.bigdatatools.common.ui.BdtJsonInfoDialog
import com.jetbrains.bigdatatools.common.ui.CustomComponentActionImpl
import com.jetbrains.bigdatatools.common.ui.ToolbarLabelActionImpl
import com.jetbrains.bigdatatools.common.ui.filter.CountFilterPopupComponent
import com.intellij.bigdatatools.coreUi.util.executeOnPooledThread
import org.com.jetbrains.bigdatatools.utils.HdfsMessagesBundle
import org.jetbrains.annotations.Nls
import software.amazon.awssdk.services.emr.model.ClusterState
import software.amazon.awssdk.services.emr.model.InstanceState

class EmrClusterInstancesController(val project: Project,
                                    private val dataManager: EmrDataManager) : DetailsTableMonitoringController<EmrClusterInstanceInfo, String>() {

  fun InstanceState.title(): @Nls String {
    return when (this) {
      InstanceState.AWAITING_FULFILLMENT -> EmrMessagesBundle.message("instance.state.AWAITING_FULFILLMENT")
      InstanceState.PROVISIONING -> EmrMessagesBundle.message("instance.state.PROVISIONING")
      InstanceState.BOOTSTRAPPING -> EmrMessagesBundle.message("instance.state.BOOTSTRAPPING")
      InstanceState.RUNNING -> EmrMessagesBundle.message("instance.state.RUNNING")
      InstanceState.TERMINATED -> EmrMessagesBundle.message("instance.state.TERMINATED")
      InstanceState.UNKNOWN_TO_SDK_VERSION -> ""
    }
  }

  private val connectionId = dataManager.connectionData.innerId

  init {
    init()
  }

  override fun showColumnFilter(): Boolean = true

  override fun getColumnSettings() = EmrToolWindowSettings.getInstance().clustersInstanceColumnsSettings

  override fun getRenderableColumns() = EmrClusterInstanceInfo.renderableColumns

  override fun getDataModel() = selectedId?.let { dataManager.getClusterInstancesModel(it) }

  override fun getAdditionalActions(): List<AnAction> {

    val showDetailsAction = DumbAwareAction.create(EmrMessagesBundle.message("emr.instance.details"),
                                                   AllIcons.FileTypes.Json) {
      val instance = getSelectedItem() ?: return@create
        BdtJsonInfoDialog(project, instance.instance.id(), instance.instance).show()
    }

    val openInSftpAction = object : DumbAwareAction(EmrMessagesBundle.message("emr.instance.open.in.sftp"), null,
                                                    BigdatatoolsSftpIcons.Sftp) {
      override fun actionPerformed(e: AnActionEvent) {
        showConnection(dataTable.selectedRow)
      }

      override fun update(e: AnActionEvent) {
        val instance = getSelectedItem()
        e.presentation.isEnabled = instance != null && ClusterState.knownValues().firstOrNull { it.name == instance.state }?.isRunning() == true

        e.presentation.text = if (e.presentation.isEnabled || e.place != TABLE_TOOLBAR_PLACE)
          EmrMessagesBundle.message("emr.instance.open.in.sftp")
        else {
          @Suppress("DialogTitleCapitalization") // Here we have complex sentence with <br>.
          EmrMessagesBundle.message("emr.instance.open.in.sftp.unavailable")
        }
      }

      override fun getActionUpdateThread() = ActionUpdateThread.BGT
    }

    return listOf(openInSftpAction,
                  showDetailsAction,
                  Separator.create(),
                  OpenUrlAction(dataManager) {
                    val clusterId = selectedId ?: return@OpenUrlAction null
                    "#cluster-details:$clusterId"
                  })
  }

  override fun createTopLeftToolbarActions(): List<AnAction> {
    val settings = EmrToolWindowSettings.getInstance()
    val config = settings.getOrCreateConfig(connectionId)
    val userText = JBTextField(config.instanceFilter, 8)

    FilterAdapter.install(dataTable.tableModel, userText, EmrClusterInstanceInfo.TEXT_FILTER) { userQuery ->
      config.instanceFilter = userQuery
      val id = selectedId ?: return@install
      dataManager.updater.invokeRefreshModel(dataManager.getClusterInstancesModel(id))
    }

    val countFilter = CountFilterPopupComponent(HdfsMessagesBundle.message("emr.cluster.filter.limit"), config.instanceLimit)
    FilterAdapter.install(dataTable.tableModel, countFilter, EmrClusterInstanceInfo.LIMIT_FILTER) { limit ->
      config.instanceLimit = limit
      val id = selectedId ?: return@install
      dataManager.updater.invokeRefreshModel(dataManager.getClusterInstancesModel(id))
    }

    return listOf(ToolbarLabelActionImpl(HdfsMessagesBundle.message("emr.filter.text")),
                                     CustomComponentActionImpl(userText),
                                     createStatusFilter(),
                                     CustomComponentActionImpl(countFilter))
  }

  private fun createStatusFilter(): SmartPopupActionGroup {
    val statusFilter = SmartPopupActionGroup()
    statusFilter.templatePresentation.text = HdfsMessagesBundle.message("emr.cluster.filter")
    statusFilter.templatePresentation.icon = AllIcons.General.Filter

    val settings = EmrToolWindowSettings.getInstance()

    for (status in InstanceState.knownValues()) {
      val toggleState = object : DumbAwareToggleAction(status.title(), null, null) {
        override fun isSelected(e: AnActionEvent) = settings.instanceStates.contains(status)
        override fun getActionUpdateThread() = ActionUpdateThread.BGT
        override fun setSelected(e: AnActionEvent, state: Boolean) {
          val filters = dataTable.tableModel.getDataModel()?.filters

          if (state) {
            settings.instanceStates.add(status)
            filters?.setFilter(DataModelFilter(EmrClusterInstanceInfo.STATES_FILTER, settings.instanceStates.joinToString(",")))
          }
          else {
            settings.instanceStates.remove(status)
            if (settings.instanceStates.isEmpty()) {
              filters?.removeFilter(EmrClusterInstanceInfo.STATES_FILTER)
            }
            else {
              filters?.setFilter(DataModelFilter(EmrClusterInstanceInfo.STATES_FILTER, settings.instanceStates.joinToString(",")))
            }
          }
          val id = selectedId ?: return
          dataManager.updater.invokeRefreshModel(dataManager.getClusterInstancesModel(id))
        }
      }

      statusFilter.add(toggleState)
    }

    return statusFilter
  }

  @Suppress("DuplicatedCode")
  private fun showConnection(row: Int) = executeOnPooledThread {
    val instanceInfo = dataTable.getDataAt(row)
    if (instanceInfo?.type == null)
      return@executeOnPooledThread

    val clusterId = selectedId ?: return@executeOnPooledThread

    dataManager.actionWrapper {
      dataManager.openSftpConnection(project, clusterId, instanceInfo)
    }
  }
}