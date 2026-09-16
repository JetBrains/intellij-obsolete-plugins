package com.intellij.bigdatatools.notebooks.core.impl.controllers

import com.intellij.bigdatatools.notebooks.core.api.editor.NotebookEditor
import com.intellij.bigdatatools.notebooks.core.api.nbformat.BasicNotebook
import com.intellij.bigdatatools.notebooks.core.api.nbformat.CellAdded
import com.intellij.bigdatatools.notebooks.core.api.nbformat.CellChanged
import com.intellij.bigdatatools.notebooks.core.api.nbformat.NotebookChangeListener
import com.intellij.bigdatatools.notebooks.core.api.nbformat.NotebookEvent
import com.intellij.bigdatatools.notebooks.core.api.nbformat.NotebookSchema
import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Key
import com.jetbrains.bigdatatools.common.delegate.Delegate

class NoteUsedInterpreterCollector(private val note: BasicNotebook) : Disposable {
  private var usedInterpreters = setOf<String>()
  var usedSubInterpreters = setOf<String>()

  @Suppress("MemberVisibilityCanBePrivate")
  val interpreterDelegate = Delegate<String, Unit>()
  val subInterpreterDelegate = Delegate<String, Unit>()

  private val noteListener = object : NotebookChangeListener {
    override fun onEvent(notebookEvent: NotebookEvent) {
      val interpreterCode = if (notebookEvent is CellAdded) {
        notebookEvent.cell.interpreterCode
      }
      else if (notebookEvent is CellChanged && NotebookSchema.cellText in notebookEvent.changedFields) {
        notebookEvent.cell.interpreterCode
      }
      else {
        null
      }
      processInterpreterCode(interpreterCode)
    }
  }

  init {
    note.addNotebookChangeListener(noteListener)

    note.cells.map {
      processInterpreterCode(interpreterCode = it.interpreterCode)
    }
  }

  override fun dispose() {
    note.removeNotebookChangeListener(noteListener)
  }

  private fun processInterpreterCode(interpreterCode: String?) {
    if (interpreterCode == null || interpreterCode in usedInterpreters)
      return

    val subInterpreter = interpreterCode.takeLastWhile { it != '.' }
    usedInterpreters = usedInterpreters + interpreterCode

    interpreterDelegate.notify(interpreterCode)

    if (subInterpreter in usedSubInterpreters)
      return
    usedSubInterpreters = usedSubInterpreters + subInterpreter
    subInterpreterDelegate.notify(subInterpreter)
  }

  companion object {
    val KEY = Key<NoteUsedInterpreterCollector>("NoteUsedInterpreterCollector")

    fun getOrSetup(notebookEditor: NotebookEditor) = getInstance(notebookEditor) ?: let {
      val controller = NoteUsedInterpreterCollector(notebookEditor.note)
      notebookEditor.putUserData(KEY, controller)
      controller
    }

    fun getInstance(notebookEditor: NotebookEditor) =
      notebookEditor.getUserData(KEY)
  }
}