package com.intellij.bigdatatools.visualization.inlays.components

import com.intellij.execution.impl.ConsoleViewImpl
import com.intellij.execution.process.AnsiEscapeDecoder
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.editor.impl.EditorImpl
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.ui.components.JBScrollPane
import com.jetbrains.bigdatatools.common.table.JBScrollPaneUtils
import javax.swing.BorderFactory

class ColoredTextConsole(project: Project, viewer: Boolean = false) :
  ConsoleViewImpl(project, viewer), AnsiEscapeDecoder.ColoredTextAcceptor {

  private val ansiEscapeDecoder = AnsiEscapeDecoder()

  init {
    // Called to init editor.
    component

    val editorImpl = editor as? EditorImpl
    if (editorImpl != null) {
      JBScrollPaneUtils.disableHorizontalWheelRedispatch(editorImpl.scrollPane as? JBScrollPane)
      editorImpl.scrollPane.border = BorderFactory.createEmptyBorder()
    }
  }

  fun addData(message: String, outputType: Key<*>) {
    ansiEscapeDecoder.escapeText(message, outputType, this)
  }

  override fun coloredTextAvailable(text: String, attributes: Key<*>) {
    print(text, ConsoleViewContentType.getConsoleViewType(attributes))
  }

  override fun getActionUpdateThread() = ActionUpdateThread.BGT
}