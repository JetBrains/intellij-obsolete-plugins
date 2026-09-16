package com.intellij.bigdatatools.zeppelin.drivers.support

import com.intellij.bigdatatools.zeppelin.controllers.editor.ZeppelinNoteLoadPromise
import com.intellij.bigdatatools.zeppelin.drivers.ZeppelinDriver
import com.intellij.bigdatatools.zeppelin.editor.ZeppelinEditor
import com.intellij.bigdatatools.zeppelin.utils.ZepMessagesBundle
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.Disposer
import com.intellij.util.Alarm
import com.jetbrains.bigdatatools.common.util.invokeLater
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean

class NotLoadedEditorDecisionService(val driver: ZeppelinDriver) : Disposable {
  private val editors = CopyOnWriteArrayList<ZeppelinEditor>()
  private val isWaiting = AtomicBoolean(false)
  private val alarm = Alarm(Alarm.ThreadToUse.POOLED_THREAD, this)

  override fun dispose() {
    editors.clear()
  }

  fun addEditor(zeppelinEditor: ZeppelinEditor) {
    if (Disposer.isDisposed(this))
      return

    editors.add(zeppelinEditor)
    if (isWaiting.compareAndSet(false, true)) {
      alarm.addRequest(::afterWait, WAIT_MORE_EDITORS_TIME, true)
    }
  }

  private fun afterWait() {
    val grouped = editors.filter { !Disposer.isDisposed(it) }.groupBy { it.project }
    editors.clear()
    isWaiting.set(false)

    grouped.forEach { (project, projectEditors) ->
      askDecision(project, projectEditors)
    }

  }

  private fun askDecision(project: Project, projectEditors: List<ZeppelinEditor>) = invokeLater {
    if (Disposer.isDisposed(this@NotLoadedEditorDecisionService))
      return@invokeLater

    val names = projectEditors.joinToString { it.file.name }
    val result = Messages.showDialog(project,
                                     ZepMessagesBundle.message("note.load.timeout.message", names),
                                     ZepMessagesBundle.message("note.load.timeout.title"),
                                     arrayOf(ZepMessagesBundle.message("note.load.timeout.wait"), Messages.getCancelButton()),
                                     0,
                                     Messages.getQuestionIcon())
    if (result == 0) {
      return@invokeLater
    }
    else {
      projectEditors.forEach {
        ZeppelinNoteLoadPromise.setLoadError(it)
      }
    }
  }

  companion object {
    const val WAIT_MORE_EDITORS_TIME = 5000
  }
}