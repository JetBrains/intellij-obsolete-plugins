package com.intellij.bigdatatools.emr.toolwindow.controllers

import com.intellij.bigdatatools.emr.data.EmrDataManager
import com.intellij.bigdatatools.emr.model.EmrClusterStepInfo
import com.intellij.bigdatatools.emr.model.isRunning
import com.intellij.bigdatatools.emr.settings.EmrToolWindowSettings
import com.intellij.bigdatatools.emr.submit.AddCustomJarStepDialog
import com.intellij.bigdatatools.emr.submit.AddHiveStepDialog
import com.intellij.bigdatatools.emr.submit.AddPigStepDialog
import com.intellij.bigdatatools.emr.submit.AddSparkStepDialog
import com.intellij.bigdatatools.emr.submit.AddStreamingStepDialog
import com.intellij.bigdatatools.emr.util.EmrMessagesBundle
import com.intellij.icons.AllIcons
import com.intellij.ide.actions.SmartPopupActionGroup
import com.intellij.openapi.actionSystem.ActionGroup
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
import com.jetbrains.bigdatatools.common.monitoring.table.extension.TableColumnsFitter
import com.jetbrains.bigdatatools.common.monitoring.table.extension.TableExtensionType
import com.jetbrains.bigdatatools.common.monitoring.table.extension.TableLoadingDecorator
import com.jetbrains.bigdatatools.common.monitoring.toolwindow.DetailsMonitoringController
import com.jetbrains.bigdatatools.common.monitoring.toolwindow.TableWithDetailsMonitoringController
import com.jetbrains.bigdatatools.common.ui.BdtJsonInfoDialog
import com.jetbrains.bigdatatools.common.ui.CustomComponentActionImpl
import com.jetbrains.bigdatatools.common.ui.ToolbarLabelActionImpl
import com.jetbrains.bigdatatools.common.ui.filter.CountFilterPopupComponent
import org.com.jetbrains.bigdatatools.utils.HdfsMessagesBundle
import org.jetbrains.annotations.Nls
import software.amazon.awssdk.services.emr.model.StepState
import java.util.EnumSet

class EmrClusterStepsController(val project: Project,
                                private val dataManager: EmrDataManager) : TableWithDetailsMonitoringController<EmrClusterStepInfo, String>(),
                                                                           DetailsMonitoringController<String> {

  fun StepState.title(): @Nls String {
    return when (this) {
      StepState.PENDING -> EmrMessagesBundle.message("step.state.PENDING")
      StepState.CANCEL_PENDING -> EmrMessagesBundle.message("step.state.CANCEL_PENDING")
      StepState.RUNNING -> EmrMessagesBundle.message("step.state.RUNNING")
      StepState.COMPLETED -> EmrMessagesBundle.message("step.state.COMPLETED")
      StepState.CANCELLED -> EmrMessagesBundle.message("step.state.CANCELLED")
      StepState.FAILED -> EmrMessagesBundle.message("step.state.FAILED")
      StepState.INTERRUPTED -> EmrMessagesBundle.message("step.state.INTERRUPTED")
      StepState.UNKNOWN_TO_SDK_VERSION -> ""
    }
  }

  private var selectedClusterId: String? = null

  private val connectionId = dataManager.connectionData.innerId

  override val detailsController = EmrStepInfoController(project, dataManager)

  private val addSparkStepAction = object : DumbAwareAction(EmrMessagesBundle.message("cluster.spark.step.add.action"), null,
                                                            null) {
    override fun actionPerformed(e: AnActionEvent) {
      val clusterId = selectedClusterId ?: return

      val dialog = AddSparkStepDialog(project, dataManager, clusterId)
      if (!dialog.showAndGet())
        return

      dataManager.addStep(clusterId, dialog.getResult())
    }

    override fun update(e: AnActionEvent) {
      val id = selectedClusterId ?: return
      val cluster = dataManager.getClusterInfoModel(id).originObject?.cluster ?: return

      e.presentation.isVisible = cluster.applications().any { it.name() == "Spark" }
    }

    override fun getActionUpdateThread() = ActionUpdateThread.BGT
  }

  private val addCustomJarStepAction = DumbAwareAction.create(EmrMessagesBundle.message("cluster.jar.step.add.action")) {
    val clusterId = selectedClusterId ?: return@create

    val dialog = AddCustomJarStepDialog(project, dataManager, clusterId, EmrMessagesBundle.message("cluster.jar.step.add.action"),
                                        EmrMessagesBundle.message("cluster.jar.step.default.name"))
    if (!dialog.showAndGet())
      return@create

    dataManager.addStep(clusterId, dialog.getResult())
  }

  private val addPigStepAction = object : DumbAwareAction(EmrMessagesBundle.message("cluster.pig.step.add.action"),
                                                          null, null) {
    override fun actionPerformed(e: AnActionEvent) {
      val clusterId = selectedClusterId ?: return

      val dialog = AddPigStepDialog(project, dataManager, clusterId,
                                    EmrMessagesBundle.message("cluster.pig.step.add.action"),
                                    EmrMessagesBundle.message("cluster.pig.step.default.name")
      )
      if (!dialog.showAndGet())
        return

      dataManager.addStep(clusterId, dialog.getResult())
    }

    override fun update(e: AnActionEvent) {
      e.presentation.isVisible = false
      val id = selectedClusterId ?: return
      val cluster = dataManager.getClusterInfoModel(id).originObject?.cluster ?: return

      e.presentation.isVisible = cluster.applications().any { it.name() == "Pig" }
    }

    override fun getActionUpdateThread() = ActionUpdateThread.BGT
  }

  private val addHiveStepAction = object : DumbAwareAction(EmrMessagesBundle.message("cluster.hive.step.add.action"),
                                                           null, null) {
    override fun actionPerformed(e: AnActionEvent) {
      val clusterId = selectedClusterId ?: return

      val dialog = AddHiveStepDialog(project, dataManager, clusterId, EmrMessagesBundle.message("cluster.hive.step.add.action"),
                                     EmrMessagesBundle.message("cluster.hive.step.default.name"))
      if (!dialog.showAndGet())
        return

      dataManager.addStep(clusterId, dialog.getResult())
    }

    override fun update(e: AnActionEvent) {
      e.presentation.isVisible = false
      val id = selectedClusterId ?: return
      val cluster = dataManager.getClusterInfoModel(id).originObject?.cluster ?: return

      e.presentation.isVisible = cluster.applications().any { it.name() == "Hive" }
    }

    override fun getActionUpdateThread() = ActionUpdateThread.BGT
  }

  private val addStreamingStepAction = DumbAwareAction.create(EmrMessagesBundle.message("cluster.streaming.step.add.action")) {
    val clusterId = selectedClusterId ?: return@create

    val dialog = AddStreamingStepDialog(project, dataManager, clusterId, EmrMessagesBundle.message("cluster.streaming.step.add.action"),
                                        EmrMessagesBundle.message("cluster.streaming.step.default.name"))
    if (!dialog.showAndGet())
      return@create

    dataManager.addStep(clusterId, dialog.getResult())
  }

  private val addStepActions = object : ActionGroup(EmrMessagesBundle.message("cluster.action.add.step"), null, AllIcons.General.Add) {
    init {
      isPopup = true
    }

    override fun getChildren(e: AnActionEvent?): Array<AnAction> = arrayOf(
      addSparkStepAction,
      addCustomJarStepAction,
      addStreamingStepAction,
      addHiveStepAction,
      addPigStepAction
    )

    override fun isDumbAware(): Boolean = true

    override fun update(e: AnActionEvent) {
      e.presentation.isEnabled = dataManager.getCachedClusterDetails(selectedClusterId)?.state.isRunning()
      e.presentation.text = if (e.presentation.isEnabled || e.place != TABLE_TOOLBAR_PLACE)
        EmrMessagesBundle.message("cluster.action.add.step")
      else {
        @Suppress("DialogTitleCapitalization") // After the properly capitalized title we have some useful comment on new line.
        EmrMessagesBundle.message("cluster.action.add.step.unavailable")
      }
    }

    override fun getActionUpdateThread() = ActionUpdateThread.BGT
  }

  private val cloneStepAction = object : DumbAwareAction(EmrMessagesBundle.message("cluster.action.clone.step"),
                                                         null,
                                                         AllIcons.Actions.Copy) {
    override fun actionPerformed(e: AnActionEvent) {
      val clusterId = selectedClusterId ?: return
      val stepConfig = getSelectedItem()?.config ?: return

      val dialog = AddCustomJarStepDialog(project, dataManager, clusterId, EmrMessagesBundle.message("cluster.jar.step.add.action"),
                                          EmrMessagesBundle.message("cluster.jar.step.default.name"))
      dialog.initByConfig(stepConfig)
      if (!dialog.showAndGet())
        return

      dataManager.addStep(clusterId, dialog.getResult())
    }

    override fun update(e: AnActionEvent) {
      e.presentation.isEnabled = dataManager.getCachedClusterDetails(selectedClusterId)?.state.isRunning()
      e.presentation.text = if (e.presentation.isEnabled || e.place != TABLE_TOOLBAR_PLACE)
        EmrMessagesBundle.message("cluster.action.clone.step")
      else {
        @Suppress("DialogTitleCapitalization") // After the properly capitalized title we have some useful comment on new line.
        EmrMessagesBundle.message("cluster.action.clone.step.unavailable")
      }
    }

    override fun getActionUpdateThread() = ActionUpdateThread.BGT
  }

  private val cancelStepAction = object : DumbAwareAction(EmrMessagesBundle.message("emr.step.cancel.action"), null,
                                                          AllIcons.Actions.Suspend) {

    override fun actionPerformed(e: AnActionEvent) {
      val step: EmrClusterStepInfo = getSelectedItem() ?: return
      val clusterId = selectedClusterId ?: return
      val res = Messages.showYesNoDialog(project,
                                         EmrMessagesBundle.message("emr.step.cancel.message", step.name),
                                         EmrMessagesBundle.message("emr.step.cancel.title"),
                                         Messages.getQuestionIcon())
      if (res != Messages.OK)
        return
      dataManager.cancelSteps(clusterId, listOf(step.id))
    }

    override fun update(e: AnActionEvent) {

      e.presentation.isEnabled = getSelectedItem()?.state?.isRunning == true
      e.presentation.text = if (e.presentation.isEnabled || e.place != TABLE_TOOLBAR_PLACE)
        EmrMessagesBundle.message("emr.step.cancel.action")
      else {
        @Suppress("DialogTitleCapitalization") // After the properly capitalized title we have some useful comment on new line.
        EmrMessagesBundle.message("emr.step.cancel.action.unavailable")
      }
    }

    override fun getActionUpdateThread() = ActionUpdateThread.BGT
  }

  private val showStepDetailsAction = object : DumbAwareAction(HdfsMessagesBundle.message("emr.step.details"), null,
                                                               AllIcons.FileTypes.Json) {

    override fun actionPerformed(e: AnActionEvent) {
      val step: EmrClusterStepInfo = getSelectedItem() ?: return
      BdtJsonInfoDialog(project, step.name, step.originalObject).show()
    }

    override fun update(e: AnActionEvent) {
      e.presentation.isEnabled = getSelectedItem() != null
    }

    override fun getActionUpdateThread() = ActionUpdateThread.BGT
  }

  init {
    init()
  }

  override fun indexToDetailId(row: Int): String = dataTable.getDataAt(row)?.id ?: ""

  override fun saveSelectedItem() = Unit

  override fun getTableExtensions(): EnumSet<TableExtensionType> =
    EnumSet.copyOf(super.getTableExtensions() - TableExtensionType.LOADING_INDICATOR)

  override fun setDetailsId(id: String) {
    selectedClusterId = id
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

    val settings = EmrToolWindowSettings.getInstance()

    for (status in StepState.knownValues()) {
      val toggleState = object : DumbAwareToggleAction(status.title(), null, null) {
        override fun isSelected(e: AnActionEvent) = settings.stepStates.contains(status)
        override fun getActionUpdateThread() = ActionUpdateThread.BGT
        override fun setSelected(e: AnActionEvent, state: Boolean) {
          val filters = dataTable.tableModel.getDataModel()?.filters

          if (state) {
            settings.stepStates.add(status)
            filters?.setFilter(DataModelFilter(EmrClusterStepInfo.STATES_FILTER, settings.stepStates.joinToString(",")))
          }
          else {
            settings.stepStates.remove(status)
            if (settings.stepStates.isEmpty()) {
              filters?.removeFilter(EmrClusterStepInfo.STATES_FILTER)
            }
            else {
              filters?.setFilter(DataModelFilter(EmrClusterStepInfo.STATES_FILTER, settings.stepStates.joinToString(",")))
            }
          }
          val id = selectedClusterId ?: return
          dataManager.updater.invokeRefreshModel(dataManager.getStepsDataModel(id))
        }
      }

      statusFilter.add(toggleState)
    }

    val config = settings.getOrCreateConfig(connectionId)

    val userText = JBTextField(config.stepFilter, 8)

    FilterAdapter.install(dataTable.tableModel, userText, EmrClusterStepInfo.TEXT_FILTER) { userQuery ->
      config.stepFilter = userQuery
      val id = selectedClusterId ?: return@install
      dataManager.updater.invokeRefreshModel(dataManager.getStepsDataModel(id))
    }

    val countFilter = CountFilterPopupComponent(HdfsMessagesBundle.message("emr.cluster.filter.limit"), config.stepLimit)
    FilterAdapter.install(dataTable.tableModel, countFilter, EmrClusterStepInfo.LIMIT_FILTER) { limit ->
      config.stepLimit = limit
      val id = selectedClusterId ?: return@install
      dataManager.updater.invokeRefreshModel(dataManager.getStepsDataModel(id))
    }

    return listOf(ToolbarLabelActionImpl(HdfsMessagesBundle.message("emr.filter.text")),
                  CustomComponentActionImpl(userText),
                  statusFilter,
                  CustomComponentActionImpl(countFilter))
  }

  override fun showColumnFilter(): Boolean = false

  override fun getColumnSettings() = EmrToolWindowSettings.getInstance().clustersStepsColumnsSettings

  override fun getRenderableColumns() = EmrClusterStepInfo.renderableColumns

  override fun getDataModel() = selectedClusterId?.let { dataManager.getStepsDataModel(it) }

  override fun getAdditionalActions(): List<AnAction> = listOf(addStepActions, cloneStepAction, cancelStepAction, showStepDetailsAction,
                                                               Separator.create(),
                                                               OpenUrlAction(dataManager) {
                                                                 val clusterId = selectedClusterId ?: return@OpenUrlAction null
                                                                 "#cluster-details:$clusterId"
                                                               }

  )
}

