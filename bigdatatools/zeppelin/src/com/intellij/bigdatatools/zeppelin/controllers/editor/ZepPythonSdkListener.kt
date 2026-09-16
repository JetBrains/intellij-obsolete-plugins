package com.intellij.bigdatatools.zeppelin.controllers.editor

import com.intellij.bigdatatools.notebooks.core.impl.controllers.NoteUsedInterpreterCollector
import com.intellij.bigdatatools.zeppelin.editor.ZeppelinEditor
import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Key
import com.intellij.ui.EditorNotifications

class ZepPythonSdkListener(private val zepEditor: ZeppelinEditor) : Disposable {
  private val interpreterCollector = NoteUsedInterpreterCollector.getInstance(zepEditor) ?: error("Controller is not found")
  val note = zepEditor.note

  init {
    interpreterCollector.subInterpreterDelegate.plusAssign(::listener)
    init()
  }

  override fun dispose() {
    interpreterCollector.subInterpreterDelegate.minusAssign(::listener)
  }

  private fun listener(subInterpreter: String) {
    if (subInterpreter in supportedSubInterpreters)
      updatePanel()
  }

  private fun init() {
    val supported = interpreterCollector.usedSubInterpreters.any { it in supportedSubInterpreters }
    if (supported)
      updatePanel()
  }

  private fun updatePanel() {
    EditorNotifications.getInstance(zepEditor.project).updateNotifications(zepEditor.file.originFile)
  }

  companion object {
    val KEY = Key<ZepPythonSdkListener>("ZEP_EDITOR_LANG_CONTROLLER")

    val supportedSubInterpreters = setOf("python", "ipython", "pyspark", "pyflink")
  }
}