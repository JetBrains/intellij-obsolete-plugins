package com.intellij.bigdatatools.visualization.inlays

import com.intellij.bigdatatools.notebooks.core.api.NotebookDataProvider
import com.intellij.bigdatatools.notebooks.core.api.nbformat.NotebookCell
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonShortcuts
import com.intellij.openapi.actionSystem.CompositeShortcutSet
import com.intellij.openapi.actionSystem.DataSink
import com.intellij.openapi.actionSystem.UiDataProvider
import com.intellij.openapi.editor.ScrollType
import com.intellij.openapi.editor.impl.EditorImpl
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.JBInsets
import java.awt.Font
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.event.FocusEvent
import java.awt.event.FocusListener
import javax.swing.BorderFactory
import javax.swing.JComponent
import javax.swing.JTextField

class NotebookInlayTitle(private val editor: EditorImpl, val cell: NotebookCell) : InlayComponent(), UiDataProvider {

  val title = JBTextField(cell.title)

  // Gutter component with synchronized position to fill vertical line and selected line on the left of title to make title the same look
  // with cell.
  private val gutterComponent = NotebookInlayTitleGutter(editor)

  var selected
    get() = gutterComponent.selected
    set(value) {
      gutterComponent.selected = value
    }

  var synced
    get() = gutterComponent.synced
    set(value) {
      gutterComponent.synced = value
    }

  init {
    title.apply {
      font = editor.getFontMetrics(Font.BOLD).font
      emptyText.text = "Untitled"
      isOpaque = false
      background = null
      border = BorderFactory.createEmptyBorder(0, -6, 0, 0)
      horizontalAlignment = JTextField.LEFT
      margin = JBInsets(title.margin.top, 0, title.margin.bottom, 0)
    }

    title.addFocusListener(object : FocusListener {
      override fun focusGained(e: FocusEvent?) = Unit
      override fun focusLost(e: FocusEvent?) {
        if (cell.title == title.text) {
          return
        }

        cell.note?.performModification {
          cell.title = title.text
        }
      }
    })

    val upAction = object : DumbAwareAction() {
      override fun actionPerformed(e: AnActionEvent) {
        if (cell.indexInNote != 0) {
          editor.contentComponent.requestFocus()
          editor.caretModel.moveToOffset(cell.textRange.startOffset - 1)
          editor.scrollingModel.scrollToCaret(ScrollType.MAKE_VISIBLE)
        }
      }
    }

    val downAction = object : DumbAwareAction() {
      override fun actionPerformed(e: AnActionEvent) {
        editor.contentComponent.requestFocus()
        editor.caretModel.moveToOffset(cell.textRange.startOffset)
        editor.scrollingModel.scrollToCaret(ScrollType.MAKE_VISIBLE)
      }
    }

    upAction.registerCustomShortcutSet(CommonShortcuts.getMoveUp(), this)
    downAction.registerCustomShortcutSet(CompositeShortcutSet(CommonShortcuts.getMoveDown(), CommonShortcuts.ENTER), this)

    add(title)
    setLayer(title, DEFAULT_LAYER)

    (editor.gutter as JComponent).apply { add(gutterComponent) }

    updateGutterComponentPosition()
  }

  override fun uiDataSnapshot(sink: DataSink) {
    NotebookDataProvider.uiDataSnapshot(sink, editor.project, editor, cell.note, cell)
  }

  fun updateGutterComponentPosition() {
    val gutterWidth = (editor.gutter as JComponent).width
    val width = NotebookInlayComponent.stripeWidth + NotebookInlayComponent.stripeOffset
    val x = gutterWidth - width

    gutterComponent.apply {
      if (this@apply.x != x ||
          this@apply.y != this@NotebookInlayTitle.y ||
          this@apply.width != width ||
          this@apply.height != this@NotebookInlayTitle.height) {
        setBounds(x, this@NotebookInlayTitle.y, width, this@NotebookInlayTitle.height)
        repaint()
      }
    }
  }

  override fun setBounds(x: Int, y: Int, width: Int, height: Int) {
    if (this.y != y || this.height != height) {
      super.setBounds(x, y, width, height)
      updateGutterComponentPosition()
    }
    else {
      super.setBounds(x, y, width, height)
    }
  }

  override fun doLayout() {
    components.forEach { it.setBounds(0, 0, width, height) }
  }

  override fun paintComponent(g: Graphics) {
    super.paintComponent(g)
    val g2d = g.create() as Graphics2D
    try {
      g2d.color = getCodeCellBackground(editor.colorsScheme)
      g2d.fillRect(0, 0, width, height)
    }
    finally {
      g2d.dispose()
    }
  }

  override fun dispose() {
    (editor.gutter as JComponent).apply {
      remove(gutterComponent)
    }
    super.dispose()
  }
}