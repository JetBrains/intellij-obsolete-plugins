package com.intellij.bigdatatools.visualization.inlays

import com.intellij.bigdatatools.visualization.inlays.utils.use
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.impl.EditorImpl
import com.intellij.openapi.editor.markup.CustomHighlighterRenderer
import com.intellij.openapi.editor.markup.RangeHighlighter
import java.awt.Graphics

class NotebookCellHighlighterRenderer : CustomHighlighterRenderer {
  override fun paint(editor: Editor, highlighter: RangeHighlighter, g: Graphics) {
    editor as EditorImpl
    @Suppress("NAME_SHADOWING") g.create().use { g ->
      val scrollbarWidth = editor.scrollPane.verticalScrollBar.width
      val oldBounds = g.clipBounds
      val visibleArea = editor.scrollingModel.visibleArea
      g.clipRect(visibleArea.x + visibleArea.width - scrollbarWidth,
        oldBounds.y,
        scrollbarWidth,
        oldBounds.height)

      g.color = editor.colorsScheme.defaultBackground
      g.clipBounds.run {
        g.fillRect(x, y, width, height)
      }
    }
  }
}