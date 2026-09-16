package com.intellij.bigdatatools.zeppelin.editor.actions

import com.intellij.bigdatatools.notebooks.core.api.executor.NoteExecutable
import com.intellij.bigdatatools.notebooks.core.api.executor.NoteExecutorUtils
import com.intellij.bigdatatools.notebooks.core.impl.file.NotebookVirtualFile
import com.intellij.bigdatatools.zeppelin.components.containers.LocalNoteContainer
import com.intellij.bigdatatools.zeppelin.file.NotebookFileUtil
import com.intellij.bigdatatools.zeppelin.idea.toolwindow.actions.OpenGlobalSettingsFormAction
import com.intellij.bigdatatools.zeppelin.utils.ZepMessagesBundle
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.ex.ComboBoxAction
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.DumbAwareAction
import javax.swing.JComponent

class ExecutorManagementAction(private var noteFile: NotebookVirtualFile) : ComboBoxAction(), DumbAware {
  override fun createPopupActionGroup(button: JComponent, context: DataContext): DefaultActionGroup {
    val fileContainer = LocalNoteContainer.getContainer(noteFile) ?: return DefaultActionGroup()

    val allProviders = fileContainer.executorsProvider

    val selectedConfig = NotebookFileUtil.getConfigId(noteFile)
    val actions = allProviders.map { createActionForInstanceSpec(fileContainer, selectedConfig, it) }

    val globalSettingsFormAction = OpenGlobalSettingsFormAction(selectedConfig)
    val allActions = listOf(globalSettingsFormAction) + actions

    return DefaultActionGroup(allActions)
  }

  private fun createActionForInstanceSpec(container: LocalNoteContainer,
                                          selectedConfigId: String?,
                                          noteExecutable: NoteExecutable): DumbAwareAction {
    val isSelectedConfig = selectedConfigId == noteExecutable.getExternalId()
    return object : DumbAwareAction(noteExecutable.presentableName) {
      init {
        templatePresentation.description = ZepMessagesBundle.message("editor.action.select.connection", noteExecutable.presentableName)
        if (isSelectedConfig) templatePresentation.icon = AllIcons.Actions.Checked
      }

      override fun actionPerformed(e: AnActionEvent) {
        container.setNewExecutor(noteExecutable)
      }
    }
  }

  override fun update(e: AnActionEvent) {
    e.presentation.isVisible = !NotebookFileUtil.isRemote(noteFile)
    if (!e.presentation.isVisible) return
    val project = e.project ?: return

    val configId = NotebookFileUtil.getConfigId(noteFile)
    val driver = configId?.let { NoteExecutorUtils.getDriver(project, it) }

    if (driver != null) {
      e.presentation.text = driver.presentableName
      e.presentation.icon = driver.icon
    }
    else {
      e.presentation.text = ZepMessagesBundle.message("editor.action.select.connection.default")
      e.presentation.icon = null
    }
  }

  override fun getActionUpdateThread() = ActionUpdateThread.BGT
}