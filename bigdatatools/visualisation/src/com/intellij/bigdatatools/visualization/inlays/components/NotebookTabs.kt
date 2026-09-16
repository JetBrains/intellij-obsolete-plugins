package com.intellij.bigdatatools.visualization.inlays.components

import com.intellij.bigdatatools.visualization.utils.VisMessagesBundle
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.util.ui.components.BorderLayoutPanel
import com.jetbrains.bigdatatools.common.ui.getCenterComponent
import com.jetbrains.bigdatatools.common.ui.getSouthComponent
import org.jetbrains.annotations.Nls
import java.awt.BorderLayout
import java.awt.Component
import javax.swing.JPanel
import javax.swing.JToggleButton

/**
 * This component "installs" into bottom area of given FileEditor and allows us to add tabs with custom components under editor.
 */
class NotebookTabs private constructor(private val editor: BorderLayoutPanel) : JPanel() {

  companion object {

    fun installOn(editor: FileEditor): NotebookTabs? {

      val component = editor.component as? BorderLayoutPanel ?: return null

      when (val bottomComponent = component.getSouthComponent()) {
        null -> return NotebookTabs(component)
        is NotebookTabs -> return bottomComponent
        else -> return null
      }
    }
  }

  private val tabs = HashMap<JToggleButton, Component>()

  init {
    editor.addToBottom(this)
    val center = editor.getCenterComponent()
    center?.let { addTab(VisMessagesBundle.message("note.tab.code"), it) }
  }

  private fun addTab(@Nls name: String, page: Component) {
    val tab = JToggleButton(name)

    val action = {
      val currentCenter = editor.getCenterComponent()
      if (currentCenter != page) {
        editor.remove(currentCenter)
        editor.add(page, BorderLayout.CENTER)
        editor.repaint()
      }
    }

    tab.addActionListener { action.invoke() }

    tabs[tab] = page

    action.invoke()

    add(tab)
  }
}