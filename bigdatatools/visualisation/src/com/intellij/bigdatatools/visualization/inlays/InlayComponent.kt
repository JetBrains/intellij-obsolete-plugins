package com.intellij.bigdatatools.visualization.inlays

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.editor.EditorCustomElementRenderer
import com.intellij.openapi.editor.Inlay
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.util.Disposer
import java.awt.Graphics
import java.awt.Rectangle
import javax.swing.JLayeredPane
import kotlin.math.max

/** Inlay editor component displaying text output, table data and charts for notebook paragraphs. */
open class InlayComponent : JLayeredPane(), EditorCustomElementRenderer, Disposable {

  /** Inlay, associated with this component. Our swing component positioned and sized according inlay. */
  var inlay: Inlay<*>? = null

  //region EditorCustomElementRenderer
  override fun paint(inlay: Inlay<*>, g: Graphics, r: Rectangle, textAttributes: TextAttributes) {
    // Actually, bounds will be updated only when they are changed what happens relatively rarely.
    updateComponentBounds(inlay)
  }

  /** Updates position and size of linked component. */
  private fun updateComponentBounds(inlay: Inlay<*>) {
    inlay.bounds?.let { updateComponentBounds(it) }
  }

  /** Returns width of component. */
  override fun calcWidthInPixels(inlay: Inlay<*>): Int = size.width

  /** Returns height of component. */
  override fun calcHeightInPixels(inlay: Inlay<*>): Int = size.height
  //endregion EditorCustomElementRenderer

  override fun setBounds(x: Int, y: Int, width: Int, height: Int) {
    if (this.x == x && this.y == y && this.width == width && this.height == height) {
      return
    }

    super.setBounds(x, y, max(0, width), max(0, height))
    runReadAction {
      inlay?.update()
    }
  }

  /** Normally this should happens directly after getting inlay with editor.inlayModel.addBlockElement. */
  // And this should only happen once.
  open fun assignInlay(inlay: Inlay<*>) {
    this.inlay = inlay

    // This method force inlay to query the size from us.
    inlay.update()
  }

  /** Fits size and position of component to inlay's size and position. */
  private fun updateComponentBounds(targetRegion: Rectangle) {
    if (bounds == targetRegion) {
      return
    }

    bounds = targetRegion

    revalidate()
    repaint()
  }

  /** Deleted inlay. This component itself should be removed manually (like: comp.parent?.remove(comp)). */
  override fun dispose() {
    inlay?.let {
      Disposer.dispose(it)
      inlay = null
    }
  }
}