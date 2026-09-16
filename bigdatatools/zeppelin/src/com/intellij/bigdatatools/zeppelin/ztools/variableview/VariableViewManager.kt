package com.intellij.bigdatatools.zeppelin.ztools.variableview

import com.intellij.bigdatatools.notebooks.core.api.NotebookKeys
import com.intellij.bigdatatools.notebooks.core.impl.file.NotebookVirtualFile
import com.intellij.bigdatatools.zeppelin.idea.toolwindow.ZtoolsToolWindowUtils
import com.intellij.bigdatatools.zeppelin.ztools.dataframe.ZtoolsDataFrameUtils
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile

@Service(Service.Level.PROJECT)
class VariableViewManager(private val project: Project) : Disposable {
  init {
    project.messageBus.connect(this).subscribe(
      FileEditorManagerListener.FILE_EDITOR_MANAGER,
      object : FileEditorManagerListener {

        private fun getVariableView(file: VirtualFile): VariableView? {
          val notebookVirtualFile = file.getUserData(NotebookKeys.NOTEBOOK_VIRTUAL_FILE) ?: file as? NotebookVirtualFile ?: return null
          return notebookVirtualFile.getUserData(VARIABLE_VIEW_KEY)
        }

        override fun selectionChanged(event: FileEditorManagerEvent) {
          val file = event.newFile ?: return
          val variableView = getVariableView(file) ?: return
          ZtoolsToolWindowUtils.setVariableView(project, variableView)
        }

        override fun fileOpened(source: FileEditorManager, file: VirtualFile) {
          val variableView = getVariableView(file) ?: return
          ZtoolsToolWindowUtils.setVariableView(project, variableView)
        }

        override fun fileClosed(source: FileEditorManager, file: VirtualFile) {
          val variableView = getVariableView(file) ?: return
          ZtoolsToolWindowUtils.removeVariableView(project, variableView)
        }
      })
  }

  override fun dispose() = Unit

  fun setVariables(file: NotebookVirtualFile, variables: Map<String, Any>, interpreterCode: String, isAppend: Boolean) {
    val variableView = file.getUserData(VARIABLE_VIEW_KEY) ?: error("Variable view is not initialized")
    variableView.setVariables(variables, interpreterCode, isAppend)
    ZtoolsDataFrameUtils.updateDataFramesInfo(variableView.root, file)

    ZtoolsToolWindowUtils.showNotificationOpenZtools(project)
  }

  fun isEmptyVariables(file: NotebookVirtualFile, interpreterCode: String): Boolean {
    val variableView = file.getUserData(VARIABLE_VIEW_KEY) ?: return true
    return variableView.isEmpty(interpreterCode)
  }

  fun addErrors(file: NotebookVirtualFile, interpreterCode: String, errors: List<String>?) {
    val variableView = file.getUserData(VARIABLE_VIEW_KEY) ?: return
    if (!errors.isNullOrEmpty())
      variableView.addErrors(interpreterCode, errors)
  }

  fun clearErrors(file: NotebookVirtualFile, interpreterCode: String) {
    val variableView = file.getUserData(VARIABLE_VIEW_KEY) ?: return
    variableView.clearErrors(interpreterCode)
  }

  fun initVariableView(file: NotebookVirtualFile, disposable: Disposable): VariableView {
    val variableView = VariableView(project, file)
    Disposer.register(disposable, variableView)
    Disposer.register(disposable) {
      ZtoolsToolWindowUtils.removeVariableView(project, variableView)
    }
    file.putUserData(VARIABLE_VIEW_KEY, variableView)
    return variableView
  }

  fun refreshUpdateInfo(file: NotebookVirtualFile) {
    file.getUserData(VARIABLE_VIEW_KEY)?.refreshInfo()
  }

  companion object {
    fun getInstance(project: Project): VariableViewManager = project.getService(VariableViewManager::class.java)

    val VARIABLE_VIEW_KEY = Key.create<VariableView>("VARIABLE_VIEW_KEY")
  }
}