// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.python.huggingFace.modelChoice.ui

import com.intellij.python.huggingFace.tags.HfPipelineTag
import com.intellij.python.huggingFace.tags.HfPipelineTagMappings
import com.intellij.python.huggingFace.tags.HfTag
import com.intellij.python.huggingFace.tags.HfTagLoader
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.scale.JBUIScale
import org.jetbrains.annotations.ApiStatus
import java.awt.Component
import java.awt.Font
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.BorderFactory
import javax.swing.BoxLayout
import javax.swing.DefaultListCellRenderer
import javax.swing.DefaultListModel
import javax.swing.DefaultListSelectionModel
import javax.swing.JList
import javax.swing.JPanel
import javax.swing.event.ListSelectionListener

@ApiStatus.Internal
internal class HfPipelineTagPanel(private val model: HfModelSearchWindowModel,
                         private val onSelectionChanged: () -> Unit
) : JPanel() {
  private lateinit var list: JBList<Any>
  private var lastSelected: HfPipelineTag? = null

  init {
    layout = BoxLayout(this, BoxLayout.Y_AXIS)
    background = JBColor.WHITE
    border = null
    renderPipelineTagsPanel()
  }

  private fun renderPipelineTagsPanel() {
    val listModel = DefaultListModel<Any>().apply {
      HfTagLoader().loadPipelineTags().forEach { (subType, tags) ->
        addElement(HfPipelineTagMappings.getSubtypeDisplayNameMap(subType))
        tags.forEach{ tag -> addElement(tag) }
      }
    }

    list = JBList(listModel).apply {
      background = JBColor.WHITE
      border = BorderFactory.createEmptyBorder(PADDING, PADDING, PADDING, PADDING)
      addMouseListener(createMouseListener())
      addListSelectionListener(createSelectionListener())
      cellRenderer = createCellRenderer()
      selectionModel = createSelectionModel(listModel)
    }

    val scrollPane = JBScrollPane(list).apply {
      horizontalScrollBar.isOpaque = false
      horizontalScrollBar.background = JBColor.WHITE
      verticalScrollBar.isOpaque = false
      verticalScrollBar.background = JBColor.WHITE
      border = null
      background = JBColor.WHITE
    }

    removeAll()
    add(scrollPane)
    revalidate()
    repaint()
  }

  private fun createSelectionListener() = ListSelectionListener { e ->
    if (!e.valueIsAdjusting) {
      val selectedValue = list.selectedValue

      if (selectedValue == lastSelected) {
        deselect()
      } else {
        if (selectedValue is HfPipelineTag) {
          model.pipelineTag = selectedValue
        } else {
          // clicked on header, for example
          model.pipelineTag = null
          list.clearSelection()
        }
        lastSelected = model.pipelineTag
        onSelectionChanged()
      }
    }
  }

  private fun createMouseListener() = object : MouseAdapter() {
    var lastIndex = -1

    override fun mouseClicked(e: MouseEvent) {
      val index = list.locationToIndex(e.point)

      if (index == lastIndex) {
        deselect()
        onSelectionChanged()
      }

      lastIndex = list.selectedIndex
    }
  }


  private fun createSelectionModel(listModel: DefaultListModel<Any>) = object : DefaultListSelectionModel() {
    override fun setSelectionInterval(index0: Int, index1: Int) {
      if (listModel.getElementAt(index0) is HfTag) {
        super.setSelectionInterval(index0, index1)
      } else {
        clearSelection()
      }
    }
  }

  private fun createCellRenderer() = object : DefaultListCellRenderer() {
    override fun getListCellRendererComponent(list: JList<*>?, value: Any?, index: Int,
                                              isSelected: Boolean, cellHasFocus: Boolean): Component {
      super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)

      when (value) {
        is HfTag -> {  // pipeline tag itself
          text = value.label
          font = font.deriveFont(Font.PLAIN)
          icon = HfPipelineTagMappings.getPipelineTagIcon(value.id)
        }
        is String -> {  // category of tags
          text = value
          font = font.deriveFont(Font.BOLD)
          foreground = JBColor.GRAY
          icon = null
          border = BorderFactory.createEmptyBorder(0, CATEGORY_PADDING, 0, 0)
        }
      }
      return this
    }
  }

  private fun deselect() {
    model.pipelineTag = null
    list.clearSelection()
  }

  companion object {
    private val PADDING = JBUIScale.scale(14)
    private val CATEGORY_PADDING = JBUIScale.scale(8)
  }
}
