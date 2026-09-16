package com.intellij.bigdatatools.visualization.inlays.layouts

import com.intellij.bigdatatools.visualization.inlays.NotebookInlayComponentGutter
import com.intellij.bigdatatools.visualization.inlays.components.ToolbarVisibility
import com.intellij.bigdatatools.visualization.inlays.pages.InlayPage
import com.intellij.bigdatatools.visualization.inlays.pages.InlayTablePage
import com.intellij.openapi.util.Disposer

abstract class NotebookInlayLayout : InlayPage, ToolbarVisibility {

  override var indexInResults = -1

  // List of displayed outputs.
  protected val _pages = mutableListOf<InlayPage>()

  fun getPages(): List<InlayPage> = _pages

  open fun setPages(inlayPages: List<InlayPage>, clear: Boolean) {

    if (clear) {
      clear()
    }

    if (inlayPages.isEmpty()) {
      return
    }

    _pages.addAll(inlayPages)
    _pages.forEach {
      Disposer.register(this, it)
    }
  }

  open fun addContentChangeListener(listener: () -> Unit) = Unit

  // Only for statistics, rows and columns in table in inlay, if present.
  fun getTableDimensions(): Pair<Int, Int>? {
    return (_pages.find { it is InlayTablePage } as? InlayTablePage)?.getTableDimensions()
  }

  override fun getCollapsedDescription(): String {
    for (page in _pages) {
      val description = page.getCollapsedDescription()
      if (description != null) {
        return description
      }
    }
    return ""
  }

  open fun clear() {
    _pages.forEach { Disposer.dispose(it) }
    _pages.clear()
    component.removeAll()
  }

  abstract fun updateGutter(gutter: NotebookInlayComponentGutter)
}