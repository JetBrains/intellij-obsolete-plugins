package com.intellij.dataWrangler.jupyterPython.actions

import com.intellij.dataWrangler.DW_SESSION
import com.intellij.dataWrangler.DataWranglerSession
import com.intellij.dataWrangler.asSafely
import com.intellij.dataWrangler.impl.action.setDataWranglerActionState
import com.intellij.dataWrangler.jupyterPython.engine.PythonDataWranglerContext
import com.intellij.dataWrangler.jupyterPython.engine.console.PyDataWranglerLocalTableContext
import com.intellij.lang.Language
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.edtWriteAction
import com.intellij.openapi.fileChooser.FileChooserFactory
import com.intellij.openapi.fileChooser.FileSaverDescriptor
import com.intellij.openapi.fileChooser.FileSaverDialog
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.concurrency.annotations.RequiresWriteLock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val FILE_NAME_SUFFIX = "_dw_transformations"

internal abstract class DataWranglerExportCodeAction : AnAction() {

  abstract fun getTargetLanguage(): Language

  abstract suspend fun <C : PythonDataWranglerContext> getFileContent(session: DataWranglerSession<C>): String?

  @RequiresWriteLock
  abstract fun writeFile(outputFile: VirtualFile, project: Project, content: String)

  override fun update(e: AnActionEvent) {
    setDataWranglerActionState(e)
    val session = e.getData(DW_SESSION)
    e.presentation.isEnabledAndVisible = session?.getContext() is PyDataWranglerLocalTableContext
  }

  override fun getActionUpdateThread(): ActionUpdateThread {
    return ActionUpdateThread.BGT
  }

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    val session = e.getData(DW_SESSION)?.asSafely<PyDataWranglerLocalTableContext>() ?: return
    e.coroutineScope.launch(Dispatchers.Default) {
      openFileSaverDialog(session, project)
    }
  }

  suspend fun <C : PythonDataWranglerContext> openFileSaverDialog(session: DataWranglerSession<C>, project: Project) {
    val parentDir = session.getContext().getFile()?.parent ?: return
    val targetExtension = getTargetLanguage().associatedFileType?.defaultExtension ?: return
    val fileWrapper = withContext(Dispatchers.EDT) {
      val descriptor = FileSaverDescriptor(templateText, "", targetExtension)
      val chooser: FileSaverDialog = FileChooserFactory.getInstance().createSaveFileDialog(descriptor, project)
      val tableName = session.getContext().getTableName().subSequence(2, session.getContext().getTableName().length - 2)
      chooser.save(parentDir, "${tableName}$FILE_NAME_SUFFIX.$targetExtension")
    } ?: return
    withContext(Dispatchers.Default) {
      val outputFile = fileWrapper.getVirtualFile(true) ?: return@withContext
      val content = getFileContent(session) ?: return@withContext
      val fileEditorManager = FileEditorManager.getInstance(project)
      edtWriteAction {
        writeFile(outputFile, project, content)
      }
      withContext(Dispatchers.EDT) {
        fileEditorManager.openFile(outputFile, true)
      }
    }
  }
}