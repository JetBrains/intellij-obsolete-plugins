package com.intellij.bigdatatools.visualization.inlays.pages

import com.intellij.bigdatatools.visualization.utils.VisMessagesBundle
import com.intellij.icons.AllIcons
import com.intellij.util.ui.JBUI
import java.awt.Component
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JLabel
import javax.swing.JPanel

/** Page in shown when JCEF is unavailable. */
class InlayHtmlPageStub : InlayPage {

  override var indexInResults = -1

  override val title
    get() = VisMessagesBundle.message("page.title.html")

  override val contentType = InlayPageContentType.TEXT

  override val component = JPanel()

  init {
    component.layout = BoxLayout(component, BoxLayout.Y_AXIS)
    component.isOpaque = false
    component.isOpaque = false

    val errorLabel = JLabel("<html>${VisMessagesBundle.message("jcef.missing")}</html>", AllIcons.General.Warning, JLabel.CENTER)
    errorLabel.alignmentX = Component.CENTER_ALIGNMENT

    component.apply {
      add(Box.createVerticalStrut(JBUI.scale(5)))
      add(errorLabel)
      add(Box.createVerticalStrut(JBUI.scale(5)))
    }
  }
}