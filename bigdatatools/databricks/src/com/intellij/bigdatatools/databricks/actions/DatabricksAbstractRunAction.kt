package com.intellij.bigdatatools.databricks.actions

import com.intellij.bigdatatools.databricks.client.DatabricksDataManager
import com.intellij.bigdatatools.databricks.rfs.DatabricksDriver
import com.intellij.bigdatatools.databricks.run.common.DatabricksAbstractRunner
import com.intellij.bigdatatools.databricks.settings.DatabricksConnectionGroup
import com.intellij.bigdatatools.databricks.toolwindow.DatabricksMonitoringToolWindowController
import com.intellij.bigdatatools.databricks.util.DatabricksBundle
import com.intellij.ide.scratch.ScratchUtil
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.edtWriteAction
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.NlsActions
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.platform.ide.progress.withBackgroundProgress
import com.jetbrains.bigdatatools.common.monitoring.toolwindow.MonitoringToolWindowController
import com.jetbrains.bigdatatools.common.rfs.driver.manager.DriverManager
import com.jetbrains.bigdatatools.common.settings.ConnectionSettings

internal abstract class DatabricksAbstractRunAction : DumbAwareAction() {
  protected abstract val supportedExtensions: Set<String>

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    val editor = FileEditorManager.getInstance(project).selectedEditor as? TextEditor ?: return
    val file = editor.file ?: return
    if (file.extension?.lowercase() !in supportedExtensions)
      return

    val dataManager = getDataManager(project) ?: return
    run(project, editor, file, dataManager, createRunner(project, dataManager))
  }

  override fun getActionUpdateThread() = ActionUpdateThread.BGT

  override fun update(e: AnActionEvent) {
    val project = e.project ?: return
    val virtualFile = FileEditorManager.getInstance(project).selectedEditor?.file
    e.presentation.isEnabledAndVisible = virtualFile?.extension in supportedExtensions
    virtualFile ?: return
    val isScratchFile = ScratchUtil.isScratch(virtualFile)
    e.presentation.isEnabledAndVisible = e.presentation.isEnabledAndVisible && !isScratchFile
    // Some python filenames have "_" which could be interpreted as a mnemonic, so we should explicitly disable mnemonics.
    e.presentation.setText(getActionName(virtualFile), false)
  }

  @NlsActions.ActionText
  protected abstract fun getActionName(virtualFile: VirtualFile): String

  protected abstract fun createRunner(project: Project, dataManager: DatabricksDataManager): DatabricksAbstractRunner

  companion object {
    fun getDataManager(project: Project): DatabricksDataManager? {
      // We will use currently selected driver in Databricks toolwindow, or the first one, if nothing is selected.
      val activeConnectionId = DatabricksMonitoringToolWindowController.getInstance(project)?.getActiveTabConnectionId()
      var driver = if (activeConnectionId == null) null else DriverManager.getDriverById(project, activeConnectionId) as? DatabricksDriver
      if (driver == null) {
        driver = DriverManager.getDrivers(project).firstOrNull { it is DatabricksDriver } as? DatabricksDriver
      }
      if (driver == null) {
        val result = Messages.showOkCancelDialog(project,
                                                 DatabricksBundle.message("dialog.no.connection.created"),
                                                 DatabricksBundle.message("dialog.databricks.title"),
                                                 DatabricksBundle.message("dialog.no.connection.created.button"),
                                                 Messages.getCancelButton(), null)
        if (result == Messages.OK) {
          ConnectionSettings.create(project, DatabricksConnectionGroup())
        }
        return null
      }

      return driver.dataManager
    }

    fun run(project: Project, editor: TextEditor, virtualFile: VirtualFile, dataManager: DatabricksDataManager, runner: DatabricksAbstractRunner) {
      if (!dataManager.client.isConnected()) {
        Messages.showInfoMessage(project, DatabricksBundle.message("dialog.no.connection.established"),
                                 DatabricksBundle.message("dialog.databricks.title"))
        return
      }

      val cluster = dataManager.getCurrentCluster()
      if (cluster == null) {
        val result = Messages.showOkCancelDialog(project,
                                                 DatabricksBundle.message("dialog.no.cluster.selected.text"),
                                                 DatabricksBundle.message("dialog.databricks.title"),
                                                 DatabricksBundle.message("dialog.no.cluster.selected.button"),
                                                 Messages.getCancelButton(), null)
        if (result == Messages.OK) {
          val toolwindow = ToolWindowManager.getInstance(project).getToolWindow(DatabricksMonitoringToolWindowController.TOOL_WINDOW_ID)
                           ?: return
          toolwindow.show()
          val connectionId = toolwindow.contentManager.selectedContent?.getUserData(MonitoringToolWindowController.CONNECTION_ID) ?: return
          DatabricksMonitoringToolWindowController.getInstance(project)?.focusOn(connectionId, DatabricksDriver.confPath)
        }
        return
      }

      if (!cluster.isClusterStarted()) {
        val result = Messages.showOkCancelDialog(project,
                                                 DatabricksBundle.message("dialog.cluster.not.started.text"),
                                                 DatabricksBundle.message("dialog.databricks.title"),
                                                 DatabricksBundle.message("dialog.cluster.not.started.button"),
                                                 Messages.getCancelButton(), null)
        if (result == Messages.OK) {
          dataManager.startCluster(cluster)
        }
        else {
          return
        }
      }

      //if (cluster.state == State.PENDING) {
      //  val result = Messages.showOkCancelDialog(project,
      //                              DatabricksBundle.message("dialog.cluster.pending.text"),
      //                              DatabricksBundle.message("dialog.databricks.title"),
      //                              DatabricksBundle.message("dialog.cluster.pending.button"),
      //                              Messages.getCancelButton(), null)
      //  if (result == Messages.OK) {
      //    dataManager.
      //
      //  }
      //  else {
      //    return
      //  }
      //
      //  return
      //}

      dataManager.actionWrapperSuspend {
        edtWriteAction {
          val document = editor.editor.getDocument()
          document.let { FileDocumentManager.getInstance().saveDocument(it) }
        }
        withBackgroundProgress(project, DatabricksBundle.message("task.run.task.as.workflow"), true) {
          runner.run(virtualFile)
        }
      }
    }
  }
}