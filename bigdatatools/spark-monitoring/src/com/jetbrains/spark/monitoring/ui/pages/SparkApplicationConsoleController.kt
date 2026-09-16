package com.jetbrains.spark.monitoring.ui.pages

import com.intellij.execution.impl.ConsoleViewImpl
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.runners.RunContentBuilder
import com.intellij.execution.ui.RunContentDescriptor
import com.intellij.execution.ui.UIExperiment
import com.intellij.icons.AllIcons
import com.intellij.ide.BrowserUtil
import com.intellij.ide.ui.customization.CustomActionsSchema
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DataSink
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.openapi.actionSystem.UiDataProvider
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.getUserData
import com.intellij.openapi.util.Key
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.panel
import com.jetbrains.bigdatatools.common.monitoring.toolwindow.DetailsMonitoringController
import com.jetbrains.bigdatatools.common.table.ClipboardUtils
import com.jetbrains.bigdatatools.common.util.ToolbarUtils
import com.jetbrains.spark.monitoring.data.AppAttemptId
import com.jetbrains.spark.monitoring.models.SparkDataManager
import com.jetbrains.spark.monitoring.util.SMMessagesBundle
import java.awt.BorderLayout
import javax.swing.JComponent
import javax.swing.JPanel

class SparkApplicationConsoleController(project: Project, connectionId: String) : DetailsMonitoringController<AppAttemptId> {

  val dataManager = SparkDataManager.getInstance(connectionId, project) ?: error("Data Manager is not initialized")
  private var attemptId: AppAttemptId? = null

  private val panel: JPanel = object : JPanel(BorderLayout()), UiDataProvider {
    override fun uiDataSnapshot(sink: DataSink) {
      val id = attemptId ?: return
      val application = dataManager.getApplication(id) ?: return
      val consoleView = application.console as? ConsoleViewImpl ?: return

      sink[LangDataKeys.RUN_CONTENT_DESCRIPTOR] = RunContentDescriptor(
        consoleView, consoleView.getUserData(PROCESS_KEY), consoleView.component, id.appId)
    }
  }

  override fun dispose() {}

  override fun getComponent(): JComponent = panel

  override fun setDetailsId(id: AppAttemptId) {
    attemptId = id
    panel.removeAll()

    val application = dataManager.getApplication(id) ?: return
    val consoleView = application.console

    if (consoleView != null) {
      val isNewLayout = UIExperiment.isNewDebuggerUIEnabled()

      val mainGroupId = if (isNewLayout) RunContentBuilder.RUN_TOOL_WINDOW_TOP_TOOLBAR_GROUP else RunContentBuilder.RUN_TOOL_WINDOW_TOP_TOOLBAR_OLD_GROUP
      val toolbarGroup = CustomActionsSchema.getInstance().getCorrectedAction(mainGroupId) as ActionGroup
      val mainChildren = toolbarGroup.getChildren(null)

      val copyLogsAction = object : DumbAwareAction(SMMessagesBundle.message("action.titile.copy.logs"), null, AllIcons.Actions.Copy) {
        override fun actionPerformed(e: AnActionEvent) {
          val text = (consoleView as? ConsoleViewImpl)?.text ?: return
          ClipboardUtils.setStringContent(text)
        }

        override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT
      }

      val toolbar = ToolbarUtils.createActionToolbar("SPARK_APPLICATION_CONSOLE",
                                                     DefaultActionGroup(mainChildren.toList() + copyLogsAction),
                                                     horizontal = false)
      toolbar.targetComponent = consoleView.component

      panel.add(toolbar.component, BorderLayout.LINE_START)
      panel.add(consoleView.component, BorderLayout.CENTER)
    }
    else {
      panel.add(panel {
        row {
          link(SMMessagesBundle.message("link.to.download.logs")) {
            BrowserUtil.open(application.logsUrl)
          }.align(Align.CENTER)
        }.resizableRow()
      }, BorderLayout.CENTER)
    }
  }

  companion object {
    val PROCESS_KEY = Key<ProcessHandler>("PROCESS_KEY")
  }
}