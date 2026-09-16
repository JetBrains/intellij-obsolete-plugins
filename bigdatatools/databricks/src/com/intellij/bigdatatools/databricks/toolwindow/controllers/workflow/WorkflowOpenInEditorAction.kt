package com.intellij.bigdatatools.databricks.toolwindow.controllers.workflow

import com.intellij.bigdatatools.databricks.util.DatabricksBundle
import com.intellij.execution.impl.ConsoleViewImpl
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DataSink
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.UiDataProvider
import com.intellij.openapi.fileEditor.impl.JComponentEditorProviderUtils
import com.intellij.openapi.fileEditor.impl.JComponentFileType
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.util.Disposer
import com.intellij.ui.PopupHandler
import com.intellij.ui.jcef.JBCefBrowser
import com.jetbrains.bigdatatools.common.util.ToolbarUtils
import java.awt.BorderLayout
import javax.swing.JPanel

class WorkflowOpenInEditorAction : DumbAwareAction() {

  override fun getActionUpdateThread() = ActionUpdateThread.BGT

  override fun update(e: AnActionEvent) {
    super.update(e)
    val workflowDetailsController = e.getData(WorkflowDetailsController.WORKFLOW_DETAILS_CONTROLLER_KEY) ?: return
    e.presentation.isEnabled = workflowDetailsController.content.isNotEmpty()
  }

  override fun actionPerformed(e: AnActionEvent) {
    val workflowDetailsController = e.getData(WorkflowDetailsController.WORKFLOW_DETAILS_CONTROLLER_KEY) ?: return
    val project = e.project ?: return

    val (component, disposable) = when (workflowDetailsController.contentType) {
      WorkflowDetailsController.ContentType.HTML -> {
        val browser = JBCefBrowser().apply {
          loadHTML(workflowDetailsController.content)
          PopupHandler.installPopupMenu(component,
                                        ActionManager.getInstance().getAction("Databricks.WorkflowToggleCodeGroup") as ActionGroup,
                                        "WorkflowResultBrowser")
        }

        // we also need to create toolbar.
        val panel = object : JPanel(BorderLayout()), UiDataProvider {
          override fun uiDataSnapshot(sink: DataSink) {
            sink[WorkflowDetailsController.WORKFLOW_DETAILS_CONTROLLER_KEY] = workflowDetailsController
            sink[WorkflowDetailsController.WORKFLOW_DETAILS_BROWSER_KEY] = browser
          }
        }.apply {
          val actions = DefaultActionGroup(ActionManager.getInstance().getAction("Databricks.WorkflowToggleCodeGroup"))
          val toolbar = ToolbarUtils.createActionToolbar(this, "DatabricksWorkflowDetails", actions, true)
          add(toolbar.component, BorderLayout.NORTH)
          add(browser.component, BorderLayout.CENTER)
        }

        panel to browser
      }
      WorkflowDetailsController.ContentType.CONSOLE -> {
        val console = ConsoleViewImpl(project, true).apply {
          print(workflowDetailsController.content, ConsoleViewContentType.NORMAL_OUTPUT)
        }
        console.component to console
      }
    }

    val editors = JComponentEditorProviderUtils.openEditor(project, workflowDetailsController.title.ifBlank { DatabricksBundle.message("open.in.editor.default.name") },
                                                           component, JComponentFileType.INSTANCE)
    editors.firstOrNull()?.apply {
      Disposer.register(this, disposable)
    }
  }
}