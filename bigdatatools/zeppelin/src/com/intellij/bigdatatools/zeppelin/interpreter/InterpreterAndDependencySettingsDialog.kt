package com.intellij.bigdatatools.zeppelin.interpreter

import com.intellij.bigdatatools.zeppelin.components.containers.controller.ZeppelinNoteController
import com.intellij.bigdatatools.zeppelin.components.instance.ZeppelinConnectionManager
import com.intellij.bigdatatools.zeppelin.dependency.ui.DependencySettingsDialog
import com.intellij.bigdatatools.zeppelin.drivers.ZeppelinDriverManager
import com.intellij.bigdatatools.zeppelin.editor.ZeppelinEditor
import com.intellij.bigdatatools.zeppelin.idea.settings.notebook.bindings.BindingInterpretersView
import com.intellij.bigdatatools.zeppelin.utils.ZepMessagesBundle
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogBuilder
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.util.Disposer
import com.intellij.ui.components.JBTabbedPane

/* Combined 4 in 1, InterpreterSettings InterpreterBindingsSettings DependencySettings and RepositorySettings. */
object InterpreterAndDependencySettingsDialog {
  private const val DIALOG_BOUNDS_KEY = "zeppelin.notebook.interpreter.settings.bounds"

  fun show(project: Project, zeppelinEditor: ZeppelinEditor) {
    val cacheConnection = ZeppelinConnectionManager.getNoteConnectionByEditor(zeppelinEditor) ?: return
    val noteController = ZeppelinNoteController(project, cacheConnection, zeppelinEditor)
    val driver = ZeppelinDriverManager.getDriver(zeppelinEditor.project, noteController.config.innerId)
    val dependencyManager = driver?.dependencyManager
    val interpreterSettingsManager = driver?.interpreterSettingsManager

    val viewsList = mutableListOf<BaseSettingsDialog>()

    val tabs = JBTabbedPane()
    val bindingInterpretersView = BindingInterpretersView(noteController)
    tabs.addTab(ZepMessagesBundle.message("bindings.title"), bindingInterpretersView.getComponent())
    viewsList += bindingInterpretersView

    if (dependencyManager != null) {
      val dependencySettingsDialog = DependencySettingsDialog(dependencyManager, project)
      tabs.addTab(ZepMessagesBundle.message("dependency.settings.title"), dependencySettingsDialog.getComponent())
      viewsList += dependencySettingsDialog
    }

    if (interpreterSettingsManager != null) {
      val interpreterSettingsDialog = InterpreterSettingsDialog(noteController, interpreterSettingsManager)
      tabs.addTab(ZepMessagesBundle.message("interpreter.settings.dialog.title"), interpreterSettingsDialog.component)
      viewsList += interpreterSettingsDialog

      val repositorySettingsDialog = RepositorySettingsDialog(interpreterSettingsManager)
      tabs.addTab(ZepMessagesBundle.message("repository.settings.dialog.title"), repositorySettingsDialog.component)
      viewsList += repositorySettingsDialog
    }

    DialogBuilder().apply {
      title(ZepMessagesBundle.message("note.settings.dialog.title"))
      addOkAction()
      addCancelAction()
      setOkOperation {
        viewsList.forEach { if (it.isModified()) it.apply() }
        dialogWrapper.close(DialogWrapper.OK_EXIT_CODE)
      }
      setCenterPanel(tabs)
      setDimensionServiceKey(DIALOG_BOUNDS_KEY)

      viewsList.forEach {
        if (it is Disposable) {
          Disposer.register(this, it)
        }
      }
    }.show()
  }
}