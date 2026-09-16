package com.intellij.bigdatatools.visualization.inlays

import com.intellij.openapi.Disposable
import com.intellij.ui.scale.JBUIScale
import java.awt.Point
import javax.swing.BorderFactory

/**
 *  Cell toolbar controller installs as global AWTEventListener and shows toolbar near cells.
 *  Toolbar will be shown
 *  - when the cell is selected
 *  - or mouse is over cell area.
 *  Maximum 2 toolbars is visible.
 */
class NotebookCellToolbarController : Disposable, InlaysMouseListener {

  private var selectedCellToolbar: NotebookCellToolbar? = null
  private var hoverCellToolbar: NotebookCellToolbar? = null
  private var creationCellToolbar: NotebookCellToolbar? = null

  private fun toolbarUnderMouse(point: Point): Boolean {
    return selectedCellToolbar?.toolbar?.bounds?.contains(point) == true ||
           hoverCellToolbar?.toolbar?.bounds?.contains(point) == true ||
           creationCellToolbar?.toolbar?.bounds?.contains(point) == true
  }

  override fun mouseMoved(inlay: NotebookInlayComponent, p: Point) {

    if (toolbarUnderMouse(p)) return

    // Toolbar for new cell creation and cell merging.
    if (p.y in inlay.y + inlay.height - InlayDimensions.bottomBorder..inlay.y + inlay.height) {
      if (creationCellToolbar?.inlay != inlay) {
        hideCreationCellToolbar()
        creationCellToolbar = NotebookCellToolbar(inlay) { NotebookCellToolbar.createNewCellBelowActions(it) }.apply {
          position = NotebookCellToolbar.CellToolbarPosition.BOTTOM_CENTER
          border = BorderFactory.createEmptyBorder(0, JBUIScale.scale(1), 0, JBUIScale.scale(1))
          show()
        }
      }
    }
    else if (inlay.cell.indexInNote == 0 && inlay.editor.offsetToXY(
        inlay.cellTextRange.startOffset).y - (if (inlay.cell.titleVisible) InlayDimensions.lineHeight else 0)
             in p.y..p.y + InlayDimensions.bottomBorder) {

      if (creationCellToolbar?.inlay != inlay) {
        hideCreationCellToolbar()
        creationCellToolbar = NotebookCellToolbar(inlay) { NotebookCellToolbar.createNewCellAboveActions(it) }.apply {
          position = NotebookCellToolbar.CellToolbarPosition.TOP_CENTER
          border = BorderFactory.createEmptyBorder(0, JBUIScale.scale(1), 0, JBUIScale.scale(1))
          show()
        }
      }
    }
    else {
      hideCreationCellToolbar()
    }

    if (inlay == selectedCellToolbar?.inlay) {
      hideHoverCellToolbar()
      return
    }

    if (inlay != hoverCellToolbar?.inlay) {
      hideHoverCellToolbar()
      hoverCellToolbar = NotebookCellToolbar(inlay) { inl -> NotebookCellToolbar.createCellActions(inl) }.apply {
        show()
      }
    }
  }

  override fun mouseExited(inlay: NotebookInlayComponent, p: Point) {
    if (toolbarUnderMouse(p)) return
    hideHoverCellToolbar()
    hideCreationCellToolbar()
    inlay.mouseOverInlay = false
  }

  override fun mouseEntered(inlay: NotebookInlayComponent, p: Point) {
    inlay.mouseOverInlay = true
  }

  private fun hideHoverCellToolbar(remove: Boolean = false) {
    hoverCellToolbar?.let {
      if (remove) it.dispose() else it.hide()
      hoverCellToolbar = null
    }
  }

  private fun hideSelectedCellToolbar(remove: Boolean = false) {
    selectedCellToolbar?.let {
      if (remove) it.dispose() else it.hide()
      selectedCellToolbar = null
    }
  }

  private fun hideCreationCellToolbar(remove: Boolean = false) {
    creationCellToolbar?.let {
      if (remove) it.dispose() else it.hide()
      creationCellToolbar = null
    }
  }

  fun cellSelected(inlay: NotebookInlayComponent) {

    // Nothing to do if cell already selected
    if (inlay == selectedCellToolbar?.inlay) {
      return
    }

    selectedCellToolbar?.hide()

    // Remove mouse hover toolbar if exist
    if (inlay == hoverCellToolbar?.inlay) {
      selectedCellToolbar = hoverCellToolbar
      hoverCellToolbar = null
    }
    else {
      selectedCellToolbar = NotebookCellToolbar(inlay) { NotebookCellToolbar.createCellActions(it) }.apply { show() }
    }
  }

  fun cellRemoved(inlay: NotebookInlayComponent) {
    if (inlay == hoverCellToolbar?.inlay) {
      hideHoverCellToolbar(true)
    }

    if (inlay == selectedCellToolbar?.inlay) {
      hideSelectedCellToolbar(true)
    }

    if (inlay == creationCellToolbar?.inlay) {
      hideCreationCellToolbar(true)
    }
  }

  override fun dispose() {
    hideHoverCellToolbar(true)
    hideSelectedCellToolbar(true)
    hideCreationCellToolbar(true)
  }
}