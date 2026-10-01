// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.python.huggingFace.modelChoice.ui

import com.intellij.icons.AllIcons
import com.intellij.python.community.impl.huggingFace.api.HuggingFaceEntityBasicApiData
import com.intellij.python.huggingFace.HuggingFaceProBundle
import com.intellij.ui.Gray
import com.intellij.ui.JBColor
import com.intellij.ui.scale.JBUIScale
import org.jetbrains.annotations.ApiStatus
import java.awt.Color
import java.awt.Component
import java.awt.Container
import java.awt.FlowLayout
import java.awt.Font
import javax.swing.BorderFactory
import javax.swing.BoxLayout
import javax.swing.JLabel
import javax.swing.JList
import javax.swing.JPanel
import javax.swing.ListCellRenderer
import javax.swing.SwingConstants

@ApiStatus.Internal
class HfModelSearchCellRenderer : JPanel(), ListCellRenderer<HuggingFaceEntityBasicApiData> {
  // todo: adaptiveness (?) + adjust styles
  // todo: selected cell does not fall outside the scroll pane
  // todo: right click actions (like goto HF website)
  // todo: proper headings for subtypes
  // todo: rounded rectangle selection
  private val itemIdLabel = JLabel()
  private val detailsLabel = JLabel()
  private val downloadsLabel = JLabel()
  private val likesLabel = JLabel()
  private val downloadsIconLabel = JLabel()
  private val likesIconLabel = JLabel()

  init {
    layout = BoxLayout(this, BoxLayout.Y_AXIS)

    itemIdLabel.font = itemIdLabel.font.deriveFont(Font.BOLD)
    itemIdLabel.horizontalAlignment = SwingConstants.LEFT

    val itemPanel = JPanel(FlowLayout(FlowLayout.LEFT)).apply {
      background = JBColor.WHITE
      add(itemIdLabel)
    }

    val infoPanel = JPanel(FlowLayout(FlowLayout.LEFT)).apply {
      background = JBColor.WHITE
      add(detailsLabel)
      add(downloadsIconLabel)
      add(downloadsLabel)
      add(likesIconLabel)
      add(likesLabel)
    }

    add(itemPanel)
    add(infoPanel)
    border = BorderFactory.createEmptyBorder(INNER_PADDING, INNER_PADDING, INNER_PADDING, INNER_PADDING)
  }

  override fun getListCellRendererComponent(
    list: JList<out HuggingFaceEntityBasicApiData>?,
    value: HuggingFaceEntityBasicApiData?,
    index: Int,
    isSelected: Boolean,
    cellHasFocus: Boolean
  ): Component {
    itemIdLabel.text = value?.itemId
    detailsLabel.text = HuggingFaceProBundle.message("table.cell.details", value?.pipelineTag ?: "", value?.humanReadableLastUpdated ?: "")
    downloadsLabel.text = value?.humanReadableDownloads
    likesLabel.text = value?.humanReadableLikes

    val bgColor = if (isSelected) SELECTION_COLOR else JBColor.WHITE

    background = bgColor
    itemIdLabel.foreground = JBColor.BLACK
    detailsLabel.foreground = GRAY_COLOR
    downloadsLabel.foreground = GRAY_COLOR
    likesLabel.foreground = GRAY_COLOR

    itemIdLabel.background = bgColor
    detailsLabel.background = bgColor
    downloadsLabel.background = bgColor
    likesLabel.background = bgColor

    downloadsIconLabel.icon = AllIcons.Plugins.Downloads
    likesIconLabel.icon = AllIcons.Plugins.Rating

    setComponentTransparency(this)
    return this
  }

  private fun setComponentTransparency(container: Container) {
    container.components.forEach {
      it.background = container.background
      if (it is Container) {
        setComponentTransparency(it)
      }
    }
  }

  companion object {
    val GRAY_COLOR: Color = JBColor.namedColor("Label.infoForeground", JBColor(Gray._120, Gray._135))
    val SELECTION_COLOR: Color = JBColor.namedColor("Plugins.lightSelectionBackground", JBColor(0xEDF6FE, 0x464A4D))
    val INNER_PADDING: Int = JBUIScale.scale(2)
  }
}