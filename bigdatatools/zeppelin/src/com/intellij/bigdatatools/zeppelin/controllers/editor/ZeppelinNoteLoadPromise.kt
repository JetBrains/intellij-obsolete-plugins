package com.intellij.bigdatatools.zeppelin.controllers.editor

import com.intellij.bigdatatools.notebooks.core.api.editor.NotebookEditor
import com.intellij.bigdatatools.zeppelin.constants.ZeppelinTimeouts
import com.intellij.bigdatatools.zeppelin.drivers.ZeppelinDriver
import com.intellij.bigdatatools.zeppelin.editor.ZeppelinEditor
import com.intellij.bigdatatools.zeppelin.file.NotebookFileUtil
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.Key
import com.intellij.util.Alarm
import com.jetbrains.bigdatatools.common.rfs.driver.Driver
import com.jetbrains.bigdatatools.common.rfs.driver.manager.DriverManager
import com.jetbrains.bigdatatools.common.util.BdtAsyncPromise
import org.jetbrains.concurrency.AsyncPromise

object ZeppelinNoteLoadPromise {

  val ZeppelinEditor.isNoteLoaded: Boolean
    get() {
      val loadPromise = getUserData(KEY) ?: error("Load promise is not found")
      return loadPromise.isDone
    }

  fun ZeppelinEditor.onNoteLoaded(body: (Throwable?) -> Unit) {
    val loadPromise = getUserData(KEY) ?: return
    loadPromise.onSuccess {
      if (editor.isDisposed)
        return@onSuccess
      try {
        body(null)
      }
      catch (t: Throwable) {
        logger.error(t)
      }
    }
    loadPromise.onError {
      if (editor.isDisposed)
        return@onError
      try {
        body(it)
      }
      catch (t: Throwable) {
        logger.error(t)
      }
    }
  }

  fun init(zeppelinEditor: ZeppelinEditor) {
    zeppelinEditor.putUserData(KEY, BdtAsyncPromise())

    if (!NotebookFileUtil.isRemote(zeppelinEditor.file)) {
      makeLoaded(zeppelinEditor, null)
      return
    }


    val driver = getDriverForEditor(zeppelinEditor) as? ZeppelinDriver ?: error("Driver is not found for editor")
    val alarm = Alarm(Alarm.ThreadToUse.POOLED_THREAD, zeppelinEditor)
    alarm.addRequest(
      {
        if (Disposer.isDisposed(zeppelinEditor))
          return@addRequest
        val promise = zeppelinEditor.getUserData(KEY) ?: error("No promise")

        if (!promise.isDone) {
          driver.addIsNotLoadedNoteEditor(zeppelinEditor)
        }
      }, ZeppelinTimeouts.GET_NOTE_TIMEOUT)
  }

  fun setLoadError(zeppelinEditor: ZeppelinEditor) {
    val promise = zeppelinEditor.getUserData(KEY)
    promise?.setError(NoteLoadSilentError())
  }

  fun makeLoaded(notebookEditor: NotebookEditor, error: String?) {
    val promise = notebookEditor.getUserData(KEY) ?: error("Promise is not found")
    if (error == null)
      promise.setResult(Unit)
    else
      promise.setError(error)
  }

  private fun getDriverForEditor(zeppelinEditor: ZeppelinEditor): Driver? {
    val project = zeppelinEditor.project
    val configId = NotebookFileUtil.getConfigId(zeppelinEditor.file) ?: return null
    return DriverManager.getDriverById(project, configId)
  }

  private val KEY = Key<AsyncPromise<Unit>>("NOTE_LOAD_PROMISE")

  private val logger = Logger.getInstance(this::class.java)
}

internal class NoteLoadSilentError : Throwable()