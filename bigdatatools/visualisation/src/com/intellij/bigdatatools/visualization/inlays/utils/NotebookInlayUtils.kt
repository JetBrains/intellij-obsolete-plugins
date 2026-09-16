package com.intellij.bigdatatools.visualization.inlays.utils

import com.intellij.bigdatatools.notebooks.core.api.NotebookDataKeys
import com.intellij.bigdatatools.notebooks.core.api.nbformat.NotebookCell
import com.intellij.bigdatatools.notebooks.core.impl.editor.getPsiFile
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.impl.SimpleDataContext
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.impl.EditorImpl
import java.awt.Component
import java.awt.Container
import java.awt.Graphics
import java.beans.PropertyChangeEvent
import java.beans.PropertyChangeListener

internal object NotebookInlayUtils {

  /** Helper function for "Run" and "Create paragraph below" actions. */
  fun createActionEvent(editor: Editor, cell: NotebookCell): AnActionEvent {
    val dataContext = SimpleDataContext.builder()
      .add(NotebookDataKeys.NOTE_EDITOR, editor as? EditorImpl)
      .add(NotebookDataKeys.NOTE, cell.note)
      .add(NotebookDataKeys.NOTE_CELL, cell)
      .add(CommonDataKeys.EDITOR, editor)
      .add(CommonDataKeys.PROJECT, editor.project)
      .add(CommonDataKeys.PSI_FILE, editor.getPsiFile())
      .build()

    return AnActionEvent.createFromDataContext("", null, dataContext)
  }

  fun doWhenUserSizeSet(component: Component, once: Boolean, callback: (PropertyChangeEvent) -> Unit) {
    component.addPropertyChangeListener(object : PropertyChangeListener {
      override fun propertyChange(evt: PropertyChangeEvent) {
        if (evt.propertyName != "userSize") return
        callback(evt)
        if (once) component.removePropertyChangeListener(this)
      }
    })
  }

  fun doWhenPreferredSizeSet(component: Container, once: Boolean, callback: () -> Unit) {

    if (component.isPreferredSizeSet) {
      callback()
      if (once) {
        return
      }
    }

    component.addPropertyChangeListener(object : PropertyChangeListener {
      override fun propertyChange(evt: PropertyChangeEvent) {
        if (evt.propertyName != "preferredSize") return
        callback()
        if (once) component.removePropertyChangeListener(this)
      }
    })
  }
}

inline fun <T, G : Graphics> G.use(handler: (g: G) -> T): T =
  try {
    handler(this)
  }
  finally {
    @Suppress("SSBasedInspection")
    dispose()
  }