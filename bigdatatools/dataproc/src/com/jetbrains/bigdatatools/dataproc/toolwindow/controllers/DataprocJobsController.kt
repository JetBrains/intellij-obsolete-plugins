package com.jetbrains.bigdatatools.dataproc.toolwindow.controllers

import com.google.cloud.dataproc.v1.JobStatus
import com.intellij.bigdatatools.coreUi.util.NotificationUtils
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
import com.jetbrains.bigdatatools.common.monitoring.data.model.DataModelFilter
import com.jetbrains.bigdatatools.common.monitoring.data.model.FilterAdapter
import com.jetbrains.bigdatatools.common.monitoring.table.extension.TableColumnsFitter
import com.jetbrains.bigdatatools.common.monitoring.table.extension.TableExtensionType
import com.jetbrains.bigdatatools.common.monitoring.table.extension.TableLoadingDecorator
import com.jetbrains.bigdatatools.common.monitoring.toolwindow.DetailsMonitoringController
import com.jetbrains.bigdatatools.common.monitoring.toolwindow.TableWithDetailsMonitoringController
import com.jetbrains.bigdatatools.common.ui.CustomComponentActionImpl
import com.jetbrains.bigdatatools.common.ui.ToolbarLabelActionImpl
import com.jetbrains.bigdatatools.common.ui.filter.CountFilterPopupComponent
import com.jetbrains.bigdatatools.dataproc.data.DataprocDataManager
import com.jetbrains.bigdatatools.dataproc.model.DataprocClusterInfo
import com.jetbrains.bigdatatools.dataproc.model.DataprocJobInfo
import com.jetbrains.bigdatatools.dataproc.model.DataprocJobState
import com.jetbrains.bigdatatools.dataproc.settings.DataprocToolWindowSettings
import com.jetbrains.bigdatatools.dataproc.submit.DataprocAddJobDialog
import com.jetbrains.bigdatatools.dataproc.util.DataprocMessagesBundle
import org.com.jetbrains.bigdatatools.utils.HdfsMessagesBundle
import java.util.EnumSet

class DataprocJobsController(val project: Project,
                             private val dataManager: DataprocDataManager) : TableWithDetailsMonitoringController<DataprocJobInfo, String>(),
                                                                             DetailsMonitoringController<String> {
  private val connectionId = dataManager.connectionData.innerId
  private var selectedClusterName: String? = null
  override val detailsController = DataprocJobInfoController(project, dataManager)

  private val addJobAction = DumbAwareAction.create(DataprocMessagesBundle.message("action.add.job.title"), AllIcons.General.Add) {
    if (!dataManager.isClusterRun(selectedClusterName)) {
      NotificationUtils.showInfoMessage(project,
                                           DataprocMessagesBundle.message("dataproc.error.cluster.must.be.started"),
                                           DataprocMessagesBundle.message("dataproc.error"))
      return@create
    }

    val dialog = DataprocAddJobDialog(project, dataManager, selectedClusterName)
    if (!dialog.showAndGet())
      return@create

    dataManager.addJob(dialog.getResult())
  }

  private val cloneJobAction = object : DumbAwareAction(DataprocMessagesBundle.message("action.clone.job.title"),
                                                        null,
                                                        AllIcons.Actions.Copy) {
    override fun actionPerformed(e: AnActionEvent) {
      val job = getSelectedItem() ?: return
      val dialog = DataprocAddJobDialog(project, dataManager, selectedClusterName)
      dialog.initByConfig(job)
      if (!dialog.showAndGet())
        return

      dataManager.addJob(dialog.getResult())
    }

    override fun update(e: AnActionEvent) {
      e.presentation.isEnabled = dataManager.isClusterRun(getSelectedItem()?.cluster)
      e.presentation.isVisible = getSelectedItem() != null
    }

    override fun getActionUpdateThread() = ActionUpdateThread.BGT
  }

  private val cancelJobAction = object : DumbAwareAction(DataprocMessagesBundle.message("action.cancel.job.title"),
                                                         null,
                                                         AllIcons.Actions.Suspend) {
    override fun actionPerformed(e: AnActionEvent) {
      val job = getSelectedItem()?.job ?: return
      val isOk = Messages.showOkCancelDialog(project, DataprocMessagesBundle.message("action.cancel.job.confirm.msg", job.jobUuid),
                                             DataprocMessagesBundle.message("action.confirm.title"), Messages.getOkButton(),
                                             Messages.getCancelButton(),
                                             Messages.getQuestionIcon()) == Messages.OK
      if (!isOk)
        return
      dataManager.cancelJob(job)
    }

    override fun update(e: AnActionEvent) {
      e.presentation.isVisible = getSelectedItem()?.job?.status?.state in setOf(JobStatus.State.PENDING, JobStatus.State.RUNNING)
      e.presentation.isEnabled = dataManager.isClusterRun(getSelectedItem()?.cluster)
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT
  }

  private val deleteJobAction = object : DumbAwareAction(DataprocMessagesBundle.message("action.delete.job.title"),
                                                         null,
                                                         AllIcons.Actions.GC) {
    override fun actionPerformed(e: AnActionEvent) {
      val job = getSelectedItem()?.job ?: return
      val isOk = Messages.showOkCancelDialog(project, DataprocMessagesBundle.message("action.delete.job.confirm.msg", job.jobUuid),
                                             DataprocMessagesBundle.message("action.confirm.title"), Messages.getOkButton(),
                                             Messages.getCancelButton(),
                                             Messages.getQuestionIcon()) == Messages.OK
      if (!isOk)
        return
      dataManager.deleteJob(job)
    }

    override fun update(e: AnActionEvent) {
      e.presentation.isEnabledAndVisible = getSelectedItem()?.job?.status?.state !in setOf(JobStatus.State.PENDING,
                                                                                           JobStatus.State.CANCEL_STARTED,
                                                                                           JobStatus.State.CANCEL_PENDING,
                                                                                           JobStatus.State.RUNNING)

    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT
  }

  init {
    init()
  }

  override fun indexToDetailId(row: Int): String = dataTable.getDataAt(row)?.id ?: ""

  override fun saveSelectedItem() = Unit

  override fun getTableExtensions(): EnumSet<TableExtensionType> =
    EnumSet.copyOf(super.getTableExtensions() - TableExtensionType.LOADING_INDICATOR)

  override fun setDetailsId(id: String) {
    selectedClusterName = id
    detailsController.clusterId = id

    val model = getDataModel() ?: return
    dataTable.tableModel.setDataModel(model)

    TableColumnsFitter.get(dataTable)?.reset()
    TableLoadingDecorator.installOn(dataTable)

    decoratedTableComponent.revalidate()
    decoratedTableComponent.repaint()
  }

  override fun createTopLeftToolbarActions(): List<AnAction> {
    val statusFilter = object : SmartPopupActionGroup() {
      override fun isDumbAware(): Boolean = true
    }

    statusFilter.templatePresentation.text = HdfsMessagesBundle.message("emr.cluster.filter")
    statusFilter.templatePresentation.icon = AllIcons.General.Filter

    val settings = DataprocToolWindowSettings.getInstance()

    for (status in DataprocJobState.entries) {
      @Suppress("HardCodedStringLiteral")
      val text = status.title

      val toggleState = object : DumbAwareToggleAction(text, null, null) {
        override fun isSelected(e: AnActionEvent) = settings.customJobStates.contains(status)
        override fun getActionUpdateThread() = ActionUpdateThread.BGT
        override fun setSelected(e: AnActionEvent, state: Boolean) {
          val filters = dataTable.tableModel.getDataModel()?.filters

          if (state) {
            settings.customJobStates.add(status)
            filters?.setFilter(DataModelFilter(DataprocClusterInfo.STATES_FILTER, settings.customJobStates.joinToString(",")))
          }
          else {
            settings.customJobStates.remove(status)
            if (settings.customJobStates.isEmpty()) {
              filters?.removeFilter(DataprocClusterInfo.STATES_FILTER)
            }
            else {
              filters?.setFilter(DataModelFilter(DataprocClusterInfo.STATES_FILTER, settings.customJobStates.joinToString(",")))
            }
          }
          dataManager.updater.invokeRefreshModel(dataManager.getClusterJobsDataModel(selectedClusterName))
        }
      }

      statusFilter.add(toggleState)
    }

    val config = settings.getOrCreateConfig(connectionId)

    val userText = JBTextField(config.jobFilter, 8)

    FilterAdapter.install(dataTable.tableModel, userText, DataprocJobInfo.TEXT_FILTER) { userQuery ->
      config.jobFilter = userQuery
      val id = selectedClusterName ?: return@install
      dataManager.updater.invokeRefreshModel(dataManager.getClusterJobsDataModel(id))
    }

    val countFilter = CountFilterPopupComponent(HdfsMessagesBundle.message("emr.cluster.filter.limit"), config.jobLimit)
    FilterAdapter.install(dataTable.tableModel, countFilter, DataprocJobInfo.LIMIT_FILTER) { limit ->
      config.jobLimit = limit
      val id = selectedClusterName ?: return@install
      dataManager.updater.invokeRefreshModel(dataManager.getClusterJobsDataModel(id))
    }

    return listOf(ToolbarLabelActionImpl(HdfsMessagesBundle.message("emr.filter.text")),
                  CustomComponentActionImpl(userText),
                  statusFilter,
                  CustomComponentActionImpl(countFilter))
  }

  override fun showColumnFilter(): Boolean = false

  override fun getColumnSettings() = DataprocToolWindowSettings.getInstance().jobsColumnsSettings

  override fun getRenderableColumns() = DataprocJobInfo.renderableColumns

  override fun getDataModel() = selectedClusterName?.let { dataManager.getClusterJobsDataModel(it) }

  override fun getAdditionalActions(): List<AnAction> {
    return listOf(addJobAction, cloneJobAction, cancelJobAction, deleteJobAction)
  }
}

