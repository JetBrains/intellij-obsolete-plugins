package com.jetbrains.hadoop.monitoring.toolwindow

import com.intellij.CommonBundle
import com.intellij.bigdatatools.coreUi.util.MessagesBundle
import com.intellij.icons.AllIcons
import com.intellij.ide.BrowserUtil
import com.intellij.ide.actions.SmartPopupActionGroup
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionToolbar
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.Separator
import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.DumbAwareToggleAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.MessageDialogBuilder
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.openapi.ui.Splitter
import com.intellij.openapi.util.Disposer
import com.intellij.ui.IdeBorderFactory
import com.intellij.ui.OnePixelSplitter
import com.intellij.ui.SideBorder
import com.intellij.ui.components.ActionLink
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.JBUI
import com.jetbrains.bigdatatools.common.monitoring.data.model.DataModelFilter
import com.jetbrains.bigdatatools.common.monitoring.data.model.FilterAdapter
import com.jetbrains.bigdatatools.common.monitoring.table.DataTable
import com.jetbrains.bigdatatools.common.monitoring.table.DataTableCreator
import com.jetbrains.bigdatatools.common.monitoring.table.TableClickHelper
import com.jetbrains.bigdatatools.common.monitoring.table.extension.TableExtensionType
import com.jetbrains.bigdatatools.common.monitoring.table.getSelectedData
import com.jetbrains.bigdatatools.common.monitoring.table.model.DataTableColumnModel
import com.jetbrains.bigdatatools.common.monitoring.table.model.DataTableModel
import com.jetbrains.bigdatatools.common.settings.ColumnVisibilitySettings
import com.jetbrains.bigdatatools.common.table.MaterialJBScrollPane
import com.jetbrains.bigdatatools.common.ui.CustomComponentActionImpl
import com.jetbrains.bigdatatools.common.ui.EditorPanel
import com.jetbrains.bigdatatools.common.ui.ToolbarLabelActionImpl
import com.jetbrains.bigdatatools.common.ui.filter.CountFilterPopupComponent
import com.jetbrains.bigdatatools.common.ui.filter.DateFilterPopupComponent
import com.jetbrains.bigdatatools.common.util.ToolbarUtils
import com.jetbrains.hadoop.monitoring.data.HadoopDataManager
import com.jetbrains.hadoop.monitoring.rest.resourcemanager.dao.AppInfo
import com.jetbrains.hadoop.monitoring.rest.resourcemanager.dao.YarnApplicationState
import com.jetbrains.hadoop.monitoring.settings.HadoopConnectionData
import com.jetbrains.hadoop.monitoring.settings.HadoopSettings
import com.jetbrains.hadoop.monitoring.statistics.HadoopMonitoringUsagesCollector
import com.jetbrains.hadoop.monitoring.util.HadoopMessagesBundle
import com.jetbrains.hadoop.monitoring.util.HadoopUtils
import java.awt.BorderLayout
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.awt.event.KeyListener
import java.util.EnumSet
import java.util.Locale
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel

class Applications(val project: Project, private val connectionData: HadoopConnectionData) : Disposable {

  companion object {
    fun killApplicationWithConfirmation(parent: JComponent, project: Project, connectionData: HadoopConnectionData, applicationId: String) {

      val dialog = MessageDialogBuilder.yesNo(HadoopMessagesBundle.message("kill.dialog.title"),
                                              HadoopMessagesBundle.message("kill.dialog.text", applicationId))
      dialog.yesText(HadoopMessagesBundle.message("kill.dialog.yes"))
      dialog.noText(CommonBundle.getCancelButtonText())
      if (dialog.ask(parent)) {
        val dataManager = HadoopDataManager.getInstance(connectionData.innerId, project) ?: error("Data Manager is not inited")
        dataManager.killApp(applicationId)
      }
    }
  }

  private val dataManager = HadoopDataManager.getInstance(connectionData.innerId, project) ?: error("Data Manager is not inited")
  private val applicationDetails = ApplicationDetails(project, connectionData, showStacktrace = false)

  private val applicationAttempts = ApplicationAttempts()
  private val splitter = OnePixelSplitter(false, HadoopSettings.getInstance().getApplicationDetailsProportion(connectionData.innerId))

  private val splitterDetailsAttempts = OnePixelSplitter(false,
                                                         HadoopSettings.getInstance().getDetailsAttemptsProportion(connectionData.innerId))

  val table: DataTable<AppInfo>

  private val openTabLabel = ActionLink(HadoopMessagesBundle.message("application.open.button")) {
    HadoopMonitoringUsagesCollector.openedInSeparateTabEvent.log(project)
    showSelectedApplication()
  }

  init {
    Disposer.register(this, applicationDetails)
    Disposer.register(this, applicationAttempts)

    val applicationsModel = dataManager.getApplicationsModel()

    val applicationsColumnSettings = HadoopSettings.getInstance().applicationColumnSettings

    val columnModel = DataTableColumnModel(AppInfo.renderableColumns, applicationsColumnSettings)
    val tableModel = DataTableModel(applicationsModel, columnModel)

    table = DataTableCreator.create(tableModel, EnumSet.of(TableExtensionType.SPEED_SEARCH,
                                                           TableExtensionType.RENDERERS_SETTER,
                                                           TableExtensionType.COLUMNS_FITTER,
                                                           TableExtensionType.ERROR_HANDLER,
                                                           TableExtensionType.SELECTION_PRESERVER,
                                                           TableExtensionType.LOADING_INDICATOR,
                                                           TableExtensionType.SMART_RESIZER))

    table.addKeyListener(keyHandler())
    Disposer.register(this, table)
    val scrollPane = MaterialJBScrollPane(table)
    table.selectionModel.addListSelectionListener { e ->
      if (table.selectedRow == -1 || e.valueIsAdjusting) {
        return@addListSelectionListener
      }

      val application = table.getSelectedData() ?: return@addListSelectionListener

      applicationDetails.setApplication(application)

      val attemptsModel = dataManager.getApplicationAttemptsModel(application.id)
      applicationAttempts.setDataModel(attemptsModel)
    }

    TableClickHelper.installOn(table, listOf("trackingUrl")) { appInfo, _ ->
      appInfo.trackingUrl?.let { trackingUrl ->
        if (appInfo.applicationType?.lowercase(Locale.getDefault())?.contains("spark") == true) {
          HadoopUtils.openSparkUrl(project, trackingUrl, connectionData)
        }
        else {
          BrowserUtil.open(trackingUrl)
        }
      }
    }

    TableClickHelper.installBrowseOn(table, AppInfo.renderableColumns.map { it.field })

    openTabLabel.border = JBUI.Borders.empty(10)
    openTabLabel.horizontalAlignment = JLabel.CENTER

    val panel = JPanel(BorderLayout()).apply {
      val actionToolbar = createToolBar(table, columnModel)
      actionToolbar.targetComponent = this
      add(scrollPane, BorderLayout.CENTER)
      add(actionToolbar.component.apply { border = IdeBorderFactory.createBorder(SideBorder.BOTTOM) }, BorderLayout.NORTH)
    }
    splitter.firstComponent = panel
    splitter.secondComponent = splitterDetailsAttempts
    splitterDetailsAttempts.firstComponent = createDetails()
    splitterDetailsAttempts.secondComponent = createAttempts()

    val connectionId = connectionData.innerId
    splitterDetailsAttempts.firstComponent.isVisible = HadoopSettings.getInstance().isAppDetailsShown(connectionId)
    splitterDetailsAttempts.secondComponent.isVisible = HadoopSettings.getInstance().isAppAttemptsShown(connectionId)

    splitter.addPropertyChangeListener(Splitter.PROP_PROPORTION) {
      HadoopSettings.getInstance().setApplicationDetailsProportion(connectionId, splitter.proportion)
    }

    splitterDetailsAttempts.addPropertyChangeListener(Splitter.PROP_PROPORTION) {
      HadoopSettings.getInstance().setDetailsAttemptsProportion(connectionId, splitterDetailsAttempts.proportion)
    }
  }

  fun getComponent(): JComponent = splitter

  override fun dispose() {}

  private fun createAttempts(): JComponent = applicationAttempts.getComponent()

  private fun createDetails(): JComponent {
    return SimpleToolWindowPanel(true, true).apply {
      setContent(EditorPanel(BorderLayout()).apply {
        add(applicationDetails.getComponent().apply { border = IdeBorderFactory.createBorder(SideBorder.BOTTOM) }, BorderLayout.CENTER)
        add(openTabLabel, BorderLayout.PAGE_END)
      })
    }
  }

  private fun keyHandler(): KeyListener = object : KeyAdapter() {
    override fun keyPressed(e: KeyEvent) {
      super.keyPressed(e)
      if (e.keyCode == KeyEvent.VK_ENTER) {
        showSelectedApplication()
      }
    }
  }

  private fun showSelectedApplication() {
    val application = table.getSelectedData() ?: return
    HadoopToolWindowController.getInstance(project).showApplication(connectionData, application)
  }

  private fun killSelectedApplication() {
    if (table.selectedRow < 0) {
      Messages.showErrorDialog(HadoopMessagesBundle.message("application.kill.no.selection.text"),
                               HadoopMessagesBundle.message("application.kill.no.selection.title"))
      return
    }

    val application = table.getSelectedData() ?: return
    killApplicationWithConfirmation(getComponent(), project, connectionData, application.id)
  }

  private fun createToolBar(table: DataTable<AppInfo>, columnModel: DataTableColumnModel<AppInfo>): ActionToolbar {
    val connectionId = connectionData.innerId

    val toggleDetailsAction = object : DumbAwareToggleAction(HadoopMessagesBundle.message("applications.toggleDetails"),
                                                             HadoopMessagesBundle.message("applications.toggleDetails.hint"),
                                                             AllIcons.Actions.ListFiles) {
      init {
        templatePresentation.putClientProperty(ActionUtil.SHOW_TEXT_IN_TOOLBAR, true)
      }
      override fun isSelected(e: AnActionEvent) = HadoopSettings.getInstance().isAppDetailsShown(connectionId)
      override fun getActionUpdateThread() = ActionUpdateThread.BGT
      override fun setSelected(e: AnActionEvent, state: Boolean) {
        splitterDetailsAttempts.firstComponent.isVisible = state
        HadoopSettings.getInstance().setAppDetailsShown(connectionId, state)
      }
    }

    val toggleAttemptsAction = object : DumbAwareToggleAction(HadoopMessagesBundle.message("applications.toggleAttempts"),
                                                              HadoopMessagesBundle.message("applications.toggleAttempts.hint"),
                                                              AllIcons.Actions.PreviewDetails) {
      init {
        templatePresentation.putClientProperty(ActionUtil.SHOW_TEXT_IN_TOOLBAR, true)
      }
      override fun isSelected(e: AnActionEvent) = HadoopSettings.getInstance().isAppAttemptsShown(connectionId)
      override fun getActionUpdateThread() = ActionUpdateThread.BGT
      override fun setSelected(e: AnActionEvent, state: Boolean) {
        splitterDetailsAttempts.secondComponent.isVisible = state
        HadoopSettings.getInstance().setAppAttemptsShown(connectionId, state)
      }
    }

    val statusFilter = SmartPopupActionGroup()
    statusFilter.templatePresentation.text = HadoopMessagesBundle.message("application.filter.state")
    statusFilter.templatePresentation.icon = AllIcons.General.Filter

    val settings = HadoopSettings.getInstance()

    for (status in YarnApplicationState.entries) {
      val toggleState = object : DumbAwareToggleAction(status.text, null, IconUtils.getIconForApplicationStatus(status)) {
        override fun isSelected(e: AnActionEvent) = settings.applicationStates.contains(status)
        override fun getActionUpdateThread() = ActionUpdateThread.BGT
        override fun setSelected(e: AnActionEvent, state: Boolean) {
          val filters = table.tableModel.getDataModel()?.filters

          if (state) {
            settings.applicationStates.add(status)
            filters?.setFilter(DataModelFilter(AppInfo.STATES_FILTER, settings.applicationStates.joinToString(",")))
          }
          else {
            settings.applicationStates.remove(status)
            if (settings.applicationStates.isEmpty()) {
              filters?.removeFilter(AppInfo.STATES_FILTER)
            }
            else {
              filters?.setFilter(DataModelFilter(AppInfo.STATES_FILTER, settings.applicationStates.joinToString(",")))
            }
          }
          dataManager.updater.invokeRefreshModel(dataManager.getApplicationsModel())
        }
      }

      statusFilter.add(toggleState)
    }

    val configStoragesColumnsAction = ColumnVisibilitySettings.createAction(columnModel.allColumns, settings.applicationColumnSettings)

    val openInBrowserAction = DumbAwareAction.create(MessagesBundle.message("open.in.browser.title"), AllIcons.General.Web) {
      HadoopMonitoringUsagesCollector.openedInBrowserEvent.log(project)
      BrowserUtil.browse("${dataManager.getRealUrl()}/cluster/apps")
    }

    val killAction = DumbAwareAction.create(HadoopMessagesBundle.message("applications.kill.title"), AllIcons.Debugger.KillProcess) {
      killSelectedApplication()
    }

    val config = settings.getHadoopConfigOrDefault(connectionData.innerId)

    val userText = JBTextField(config.applicationsUser, 8)

    FilterAdapter.install(table.tableModel, userText, AppInfo.USER_FILTER) { userQuery ->
      HadoopSettings.getInstance().getOrCreateHadoopConfig(connectionData.innerId).applicationsUser = userQuery
      dataManager.updater.invokeRefreshModel(dataManager.getApplicationsModel())
    }

    val countFilter = CountFilterPopupComponent(HadoopMessagesBundle.message("applications.filter.limit"), config.applicationsLimit)
    FilterAdapter.install(table.tableModel, countFilter, AppInfo.LIMIT_FILTER) { limit ->
      HadoopSettings.getInstance().getOrCreateHadoopConfig(connectionData.innerId).applicationsLimit = limit
      dataManager.updater.invokeRefreshModel(dataManager.getApplicationsModel())
    }

    val startedFilter = DateFilterPopupComponent(HadoopMessagesBundle.message("applications.filter.started"),
                                                 config.applicationStartedPeriodType,
                                                 config.applicationsStartedBegin,
                                                 config.applicationsStartedEnd)

    FilterAdapter.install(table.tableModel, startedFilter,
                          AppInfo.STARTED_BEGIN_FILTER,
                          AppInfo.STARTED_END_FILTER) { periodType, from, to ->
      val hadoopSettings = HadoopSettings.getInstance().getOrCreateHadoopConfig(connectionData.innerId)
      hadoopSettings.applicationStartedPeriodType = periodType
      hadoopSettings.applicationsStartedBegin = from
      hadoopSettings.applicationsStartedEnd = to
      dataManager.updater.invokeRefreshModel(dataManager.getApplicationsModel())
    }

    val finishedFilter = DateFilterPopupComponent(HadoopMessagesBundle.message("applications.filter.finished"),
                                                  config.applicationFinishedPeriodType,
                                                  config.applicationsFinishedBegin,
                                                  config.applicationsFinishedEnd)

    FilterAdapter.install(table.tableModel, finishedFilter,
                          AppInfo.FINISHED_BEGIN_FILTER,
                          AppInfo.FINISHED_END_FILTER) { periodType, from, to ->
      val hadoopSettings = HadoopSettings.getInstance().getOrCreateHadoopConfig(connectionData.innerId)
      hadoopSettings.applicationFinishedPeriodType = periodType
      hadoopSettings.applicationsFinishedBegin = from
      hadoopSettings.applicationsFinishedEnd = to
      dataManager.updater.invokeRefreshModel(dataManager.getApplicationsModel())
    }

    val toolbar = DefaultActionGroup(ToolbarLabelActionImpl(HadoopMessagesBundle.message("application.filter.name")),
                                     CustomComponentActionImpl(userText),
                                     statusFilter,
                                     CustomComponentActionImpl(countFilter),
                                     CustomComponentActionImpl(startedFilter),
                                     CustomComponentActionImpl(finishedFilter),
                                     Separator(),
                                     configStoragesColumnsAction,
                                     openInBrowserAction,
                                     killAction,
                                     Separator(),
                                     toggleDetailsAction,
                                     toggleAttemptsAction)

    return ToolbarUtils.createActionToolbar("BDTHadoopApplicationFiltering", toolbar, true)
  }
}