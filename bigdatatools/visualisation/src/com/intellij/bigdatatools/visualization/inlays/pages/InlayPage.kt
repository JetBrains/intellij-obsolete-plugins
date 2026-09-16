package com.intellij.bigdatatools.visualization.inlays.pages

import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.AnAction
import javax.swing.JComponent

interface InlayPage : Disposable {

  var indexInResults: Int

  val component: JComponent

  val title: String

  val contentType: InlayPageContentType

  override fun dispose() = Unit

  fun getCollapsedDescription(): String? = null

  fun createActions(): List<AnAction> = emptyList()
}