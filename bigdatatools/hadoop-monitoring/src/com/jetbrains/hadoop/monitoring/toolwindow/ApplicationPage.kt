package com.jetbrains.hadoop.monitoring.toolwindow

import com.intellij.execution.impl.ConsoleViewImpl
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.icons.AllIcons
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionToolbar
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.Separator
import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.DumbAwareToggleAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.openapi.ui.Splitter
import com.intellij.openapi.util.Disposer
import com.intellij.ui.OnePixelSplitter
import com.intellij.ui.components.JBScrollPane
import com.jetbrains.bigdatatools.common.monitoring.data.model.FieldsDataModel
import com.jetbrains.bigdatatools.common.monitoring.list.DataListCreator
import com.jetbrains.bigdatatools.common.monitoring.list.ListClickHelper
import com.jetbrains.bigdatatools.common.monitoring.list.extension.ListExtensionType
import com.jetbrains.bigdatatools.common.monitoring.list.getValueForRow
import com.jetbrains.bigdatatools.common.monitoring.list.model.ListTableModel
import com.jetbrains.bigdatatools.common.monitoring.table.extension.TableSelectionPreserver
import com.jetbrains.bigdatatools.common.monitoring.table.getSelectedData
import com.jetbrains.bigdatatools.common.settings.ColumnVisibilitySettings
import com.jetbrains.bigdatatools.common.ui.ToolbarLabelActionImpl
import com.jetbrains.bigdatatools.common.util.ToolbarUtils
import com.jetbrains.hadoop.monitoring.action.OpenUrlAction
import com.jetbrains.hadoop.monitoring.data.HadoopDataManager
import com.jetbrains.hadoop.monitoring.rest.resourcemanager.dao.AppInfo
import com.jetbrains.hadoop.monitoring.settings.HadoopConnectionData
import com.jetbrains.hadoop.monitoring.settings.HadoopSettings
import com.jetbrains.hadoop.monitoring.util.HadoopMessagesBundle
import com.jetbrains.hadoop.monitoring.util.HadoopUtils
import java.util.EnumSet
import java.util.Locale

/**
 * Single application page. Contains:
 * - List of selected application fields
 * - Application diagnostics console
 * - Application attempts
 * - Container info for selected attempt
 */
class ApplicationPage(project: Project, connectionData: HadoopConnectionData, appInfo: AppInfo) : Disposable {
  private val dataManager = HadoopDataManager.getInstance(connectionData.innerId, project) ?: error("Data Manager is not inited")

  private val applicationSplitter = OnePixelSplitter(false, HadoopSettings.getInstance().getStandaloneApplicationProportion(
    connectionData.innerId))
  private val diagnosticsSplitter = OnePixelSplitter(false, HadoopSettings.getInstance().getStandaloneDiagnosticsProportion(
    connectionData.innerId))
  private val attemptsSplitter = OnePixelSplitter(false,
                                                  HadoopSettings.getInstance().getStandaloneAttemptsProportion(connectionData.innerId))

  init {
    val data = FieldsDataModel.createForObject(appInfo)

    val attempts = ApplicationAttempts()
    Disposer.register(this, attempts)

    val attemptsModel = dataManager.getApplicationAttemptsModel(appInfo.id)
    attempts.setDataModel(attemptsModel)

    val containersInfo = Containers()
    Disposer.register(this, containersInfo)

    val table = attempts.table
    TableSelectionPreserver.installOn(table, null)
    table.selectionModel.addListSelectionListener { e ->
      if (table.selectedRow == -1 || e.valueIsAdjusting) {
        return@addListSelectionListener
      }

      val attempt = table.getSelectedData() ?: return@addListSelectionListener

      val containersModel = dataManager.getContainersInfo(appInfo.id, attempt.id)
      containersInfo.setDataModel(containersModel)
    }

    applicationSplitter.firstComponent = createApplicationComponent(project, data, connectionData, appInfo.id)
    applicationSplitter.secondComponent = diagnosticsSplitter
    diagnosticsSplitter.firstComponent = createApplicationDiagnostics(project, connectionData.innerId, data["diagnostics"] as String?)
    diagnosticsSplitter.secondComponent = attemptsSplitter
    attemptsSplitter.firstComponent = attempts.getComponent()
    attemptsSplitter.secondComponent = containersInfo.getComponent()

    applicationSplitter.addPropertyChangeListener(Splitter.PROP_PROPORTION) {
      HadoopSettings.getInstance().setStandaloneApplicationProportion(connectionData.innerId, applicationSplitter.proportion)
    }

    diagnosticsSplitter.addPropertyChangeListener(Splitter.PROP_PROPORTION) {
      HadoopSettings.getInstance().setStandaloneDiagnosticsProportion(connectionData.innerId, diagnosticsSplitter.proportion)
    }

    attemptsSplitter.addPropertyChangeListener(Splitter.PROP_PROPORTION) {
      HadoopSettings.getInstance().setStandaloneAttemptsProportion(connectionData.innerId, attemptsSplitter.proportion)
    }
  }

  private fun createApplicationDiagnostics(project: Project, connectionId: String, diagnostics: String?): SimpleToolWindowPanel {

    val diagnosticsConsole = ConsoleViewImpl(project, true)

    val scrollToBottomAction = object : DumbAwareToggleAction(HadoopMessagesBundle.message("tools.logs.scrollToBottom.text"),
                                                              HadoopMessagesBundle.message("tools.logs.scrollToBottom.hint"),
                                                              AllIcons.RunConfigurations.Scroll_down) {
      override fun isSelected(e: AnActionEvent) = HadoopSettings.getInstance().getScrollDiagnosticsToBottom(connectionId)
      override fun getActionUpdateThread() = ActionUpdateThread.BGT
      override fun setSelected(e: AnActionEvent, state: Boolean) {
        HadoopSettings.getInstance().setScrollDiagnosticsToBottom(connectionId, state)
        if (state) {
          diagnosticsConsole.scrollToEnd()
        }
      }
    }

    val actionGroup = DefaultActionGroup(ToolbarLabelActionImpl(HadoopMessagesBundle.message("application.diagnostics.header")),
                                         scrollToBottomAction)

    val consolePanel = SimpleToolWindowPanel(true, true).apply {
      toolbar = ToolbarUtils.createActionToolbar(this, "BDTHadoopApplicationDiagnostics", actionGroup, true).component
      setContent(diagnosticsConsole.component)
    }

    Disposer.register(this, diagnosticsConsole)

    try {
      diagnosticsConsole.clear()
      diagnosticsConsole.print(if (diagnostics.isNullOrBlank()) HadoopMessagesBundle.message("application.diagnostics.empty")
                               else diagnostics,
                               ConsoleViewContentType.ERROR_OUTPUT)

      if (HadoopSettings.getInstance().getScrollDiagnosticsToBottom(connectionId))
        diagnosticsConsole.scrollToEnd()
      else
        diagnosticsConsole.scrollTo(0)
    }
    catch (e: Exception) {
      logger.error(e)
    }

    return consolePanel
  }

  private fun createToolbar(project: Project, connectionData: HadoopConnectionData, applicationId: String): ActionToolbar {

    val connectionId = connectionData.innerId

    val toggleDiagnostics = object : DumbAwareToggleAction(HadoopMessagesBundle.message("toggle.diagnostic.title"),
                                                           HadoopMessagesBundle.message("toggle.diagnostic.hint"),
                                                           AllIcons.Actions.PreviewDetails) {
      init {
        templatePresentation.putClientProperty(ActionUtil.SHOW_TEXT_IN_TOOLBAR, true)
      }
      override fun isSelected(e: AnActionEvent) = HadoopSettings.getInstance().isStandaloneDiagnosticsShown(connectionId)
      override fun getActionUpdateThread() = ActionUpdateThread.BGT
      override fun setSelected(e: AnActionEvent, state: Boolean) {
        diagnosticsSplitter.firstComponent.isVisible = state
        HadoopSettings.getInstance().setStandaloneDiagnosticsShown(connectionId, state)
      }
    }

    val toggleAttempts = object : DumbAwareToggleAction(HadoopMessagesBundle.message("toggle.attempts.title"),
                                                        HadoopMessagesBundle.message("toggle.attempts.hint"),
                                                        AllIcons.Actions.ListFiles) {
      init {
        templatePresentation.putClientProperty(ActionUtil.SHOW_TEXT_IN_TOOLBAR, true)
      }
      override fun isSelected(e: AnActionEvent) = HadoopSettings.getInstance().isStandaloneAttemptsShown(connectionId)
      override fun getActionUpdateThread() = ActionUpdateThread.BGT
      override fun setSelected(e: AnActionEvent, state: Boolean) {
        attemptsSplitter.firstComponent.isVisible = state
        HadoopSettings.getInstance().setStandaloneAttemptsShown(connectionId, state)
      }
    }

    val toggleContainers = object : DumbAwareToggleAction(HadoopMessagesBundle.message("toggle.containers.title"),
                                                          HadoopMessagesBundle.message("toggle.containers.hint"),
                                                          AllIcons.Actions.ListFiles) {
      init {
        templatePresentation.putClientProperty(ActionUtil.SHOW_TEXT_IN_TOOLBAR, true)
      }
      override fun isSelected(e: AnActionEvent) = HadoopSettings.getInstance().isStandaloneContainersShown(connectionId)
      override fun getActionUpdateThread() = ActionUpdateThread.BGT
      override fun setSelected(e: AnActionEvent, state: Boolean) {
        attemptsSplitter.secondComponent.isVisible = state
        HadoopSettings.getInstance().setStandaloneContainersShown(connectionId, state)
      }
    }

    val openUrl = OpenUrlAction({ "cluster/app/$applicationId" }, project, dataManager)

    val appDetailsColumnsVisibility = ColumnVisibilitySettings.createAction(AppInfo.renderableColumns,
                                                                            HadoopSettings.getInstance().applicationDetailsColumnSettings)

    val killAction = DumbAwareAction.create(HadoopMessagesBundle.message("applications.kill.title"), AllIcons.Debugger.KillProcess) {
      Applications.killApplicationWithConfirmation(applicationSplitter, project, connectionData, applicationId)
    }

    val actionGroup = DefaultActionGroup(openUrl,
                                         appDetailsColumnsVisibility,
                                         killAction,
                                         Separator(),
                                         toggleDiagnostics,
                                         toggleAttempts,
                                         toggleContainers)

    return ToolbarUtils.createActionToolbar("BDTHadoopApplications", actionGroup, true)
  }

  private fun createApplicationComponent(project: Project,
                                         data: FieldsDataModel,
                                         connectionData: HadoopConnectionData,
                                         applicationId: String): SimpleToolWindowPanel {
    val tableModel = ListTableModel(data, HadoopSettings.getInstance().applicationDetailsColumnSettings)
    val dataList = DataListCreator.create(tableModel, EnumSet.allOf(ListExtensionType::class.java))
    Disposer.register(this, dataList)

    ListClickHelper.installOn(dataList, "trackingUrl") { url ->
      if (url as? String == null) return@installOn
      if (dataList.getValueForRow("applicationType")?.toString()?.lowercase(Locale.getDefault())?.contains("spark") == true) {
        HadoopUtils.openSparkUrl(project, url, connectionData)
      }
      else {
        BrowserUtil.open(url)
      }
    }

    ListClickHelper.installBrowseOn(dataList, AppInfo.renderableColumns.map { it.field })

    return SimpleToolWindowPanel(true, true).apply {
      val actionToolbar = createToolbar(project, connectionData, applicationId)
      actionToolbar.targetComponent = this
      toolbar = actionToolbar.component
      setContent(JBScrollPane(dataList))
    }
  }

  fun getComponent() = applicationSplitter

  override fun dispose() {}

  companion object {
    private val logger = Logger.getInstance(this::class.java)
  }
}