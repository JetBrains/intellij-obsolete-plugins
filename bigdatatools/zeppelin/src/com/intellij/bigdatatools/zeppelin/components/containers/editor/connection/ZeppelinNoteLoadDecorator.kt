package com.intellij.bigdatatools.zeppelin.components.containers.editor.connection

import com.intellij.bigdatatools.zeppelin.constants.ZeppelinIcons
import com.intellij.bigdatatools.zeppelin.controllers.editor.NoteLoadSilentError
import com.intellij.bigdatatools.zeppelin.controllers.editor.ZeppelinNoteLoadPromise.isNoteLoaded
import com.intellij.bigdatatools.zeppelin.controllers.editor.ZeppelinNoteLoadPromise.onNoteLoaded
import com.intellij.bigdatatools.zeppelin.editor.ZeppelinEditor
import com.intellij.bigdatatools.zeppelin.utils.ZepMessagesBundle
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.impl.EditorImpl
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.impl.FileEditorManagerImpl
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.IdeFocusManager
import com.intellij.ui.AnimatedIcon
import com.intellij.ui.EditorNotifications
import com.intellij.ui.tabs.TabInfo
import com.intellij.util.concurrency.EdtExecutorService
import com.jetbrains.bigdatatools.common.util.invokeLater
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import kotlin.reflect.full.functions
import kotlin.reflect.jvm.isAccessible

internal class ZeppelinNoteLoadDecorator(private val editor: ZeppelinEditor) : Disposable {
  private val project = editor.project

  private var loadingIcon: ScheduledFuture<*>? = null

  init {
    callTextEditorComponentStartLoading()
    // This will hide editor until the loading is not finished.
    editor.getSourceEditor().editor.component.isVisible = false
    setLoadingInTab()
    editor.onNoteLoaded { onNoteLoaded(it) }
  }

  // ToDo Here we have a huge hack to deal with the new code in platform (BDIDE-3584).
  //    Probably the better idea was to override "loadEditorInBackground"
  private fun callFunctionOfTextEditorComponent(functionName: String) {
    editor.getSourceEditor().component::class.functions.firstOrNull { it.name == functionName }?.apply {
      isAccessible = true
    }?.call(editor.getSourceEditor().component)
  }

  private fun callTextEditorComponentStartLoading() = callFunctionOfTextEditorComponent("startLoading")

  private fun callTextEditorComponentStopLoading() = callFunctionOfTextEditorComponent("loadingFinished")

  override fun dispose() {
    loadingIcon?.cancel(true)
  }

  private fun onNoteLoaded(error: Throwable?) = invokeLater {
    if (Disposer.isDisposed(editor))
      return@invokeLater

    if (error != null) {
      closeWithError(editor.project, editor.file.originFile, error)
      return@invokeLater
    }

    callTextEditorComponentStopLoading()
    editor.getSourceEditor().editor.component.isVisible = true

    loadingIcon?.cancel(true)
    getTab()?.setIcon(ZeppelinIcons.ZEPPELIN_FILE)

    project.service<EditorNotifications>().updateNotifications(editor.file)

    (editor.getSourceEditor().editor as? EditorImpl)?.contentComponent?.let {
      if (it.isShowing) {
        val focusManager = IdeFocusManager.getInstance(editor.project)
        if (focusManager.focusOwner !== it) {
          focusManager.requestFocus(it, true)
        }
      }
    }
  }

  private fun setLoadingInTab() = invokeLater {
    if (editor.isNoteLoaded) return@invokeLater
    val tab = getTab() ?: return@invokeLater
    loadingIcon = setLoadingIcon(tab)
  }

  private fun setLoadingIcon(tab: TabInfo): ScheduledFuture<*> {
    var index = 0

    return EdtExecutorService.getScheduledExecutorInstance()
      .scheduleWithFixedDelay({
                                tab.setIcon(AnimatedIcon.Default.ICONS[index])
                                index++
                                if (index >= AnimatedIcon.Default.ICONS.size) {
                                  index = 0
                                }
                              }, 0, 300, TimeUnit.MILLISECONDS)
  }

  private fun getTab(): TabInfo? {
    val fileEditorManager = FileEditorManager.getInstance(project) as FileEditorManagerImpl
    val currentWindow = fileEditorManager.splitters.currentWindow

    return currentWindow?.tabbedPane?.tabs?.tabs?.find { it.`object` == editor.file.originalFile }
  }

  companion object {
    private val logger = Logger.getInstance(this::class.java)

    fun closeWithError(project: Project, file: VirtualFile, error: Throwable) {
      if (error !is NoteLoadSilentError)
        Messages.showErrorDialog(error.message ?: (error::class.java.name + "\n" + ZepMessagesBundle.message("note.close.info")),
                                 ZepMessagesBundle.message("note.open.error.title"))
      try {
        FileEditorManager.getInstance(project).closeFile(file)
      }
      catch (t: Throwable) {
        logger.warn("Cannot close file ${file.name}", t)
      }
    }
  }
}