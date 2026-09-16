package com.intellij.bigdatatools.visualization.inlays.components

import com.intellij.ui.SpeedSearchBase
import java.util.AbstractList
import javax.swing.text.DefaultHighlighter
import javax.swing.text.JTextComponent

/**
 * SpeedSearch component for JTextPane.
 * Simply focus the component and start typing to appear search bar.
 * Use up/down arrows to navigate to previous/next search results. Esc key or defocus component to hide search.
 */
// TODO we can boost performance slightly by remove "component.text.split('\n')"
class JTextComponentSpeedSearch private constructor(component: JTextComponent) : SpeedSearchBase<JTextComponent>(component, null) {

  init {
    // This will remove highlighting when we are closing search popup.
    addChangeListener {
      if (!isPopupActive && component.highlighter != null) {
        component.highlighter.removeAllHighlights()
      }
    }
  }

  override fun getElementCount(): Int {
    return component.text.count { it == '\n' }
  }

  override fun getElementIterator(startingIndex: Int): ListIterator<Any> {
    return MyRowsList().listIterator(startingIndex)
  }

  override fun getSelectedIndex(): Int {
    return selectedIndex
  }

  override fun getElementText(row: Any): String {
    val rows = component.text.split('\n')
    return rows[row as Int]
  }

  private var selectedIndex = -1

  override fun selectElement(row: Any?, selectedText: String) {
    if (row == null || row as Int? == null) {
      selectedIndex = -1
      return
    }

    val rows = component.text.split('\n')

    var rowStartOffset = 0
    for (i in 0 until row) {
      rowStartOffset += rows[i].length + 1
    }

    val found = comparator.matchingFragments(selectedText, rows[row])

    val highlighter = component.highlighter
    highlighter.removeAllHighlights()
    for (f in found!!) {
      highlighter.addHighlight(rowStartOffset + f.startOffset, rowStartOffset + f.endOffset, DefaultHighlighter.DefaultPainter)
    }

    // For scrolling to selected row.
    component.caretPosition = rowStartOffset

    selectedIndex = row
  }

  // ToDo should be changed to ListIterator
  private inner class MyRowsList : AbstractList<Any>() {
    override val size: Int
      get() = component.text.count { it == '\n' }

    override fun get(index: Int): Any {
      return index
    }
  }

  companion object {
    fun installOn(component: JTextComponent) = JTextComponentSpeedSearch(component).apply {
      setupListeners()
    }
  }
}
