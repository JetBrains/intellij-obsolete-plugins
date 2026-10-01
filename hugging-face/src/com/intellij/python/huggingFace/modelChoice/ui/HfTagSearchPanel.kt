// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.python.huggingFace.modelChoice.ui

import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.OnePixelDivider
import com.intellij.openapi.util.NlsSafe
import com.intellij.python.community.impl.huggingFace.api.HuggingFaceModelSortKey
import com.intellij.python.huggingFace.HuggingFaceProBundle
import com.intellij.python.huggingFace.tags.HfTag
import com.intellij.python.huggingFace.tags.HfTagLoader
import com.intellij.ui.JBColor
import com.intellij.ui.border.CustomLineBorder
import com.intellij.ui.dsl.builder.panel
import java.awt.Component
import java.awt.event.ActionListener
import javax.swing.BorderFactory
import javax.swing.DefaultListCellRenderer
import javax.swing.JList

/**
 * Panel 3 - combo boxes with tags selection.
 * @see com.intellij.python.huggingFace.modelChoice.ui.HfModelSelectionDialog
 */
internal class HfTagSearchPanel(
  private var model: HfModelSearchWindowModel,
  private val onSelectionChanged: () -> Unit
) {
  // todo: multiple choice
  private val tagLoader = HfTagLoader()
  // null + is a workaround to add a "deselect" option, better solutions are welcome
  private val licenseTags = listOf(null) + tagLoader.loadLicenseTags().sortedBy { it.label }
  private val otherTags = listOf(null) + tagLoader.loadOtherTags().sortedBy { it.label }

  fun getPanel(): DialogPanel {
    return panel {
      indent {
        row(HuggingFaceProBundle.message("python.hugging.face.model.choice.sort.by.label")) {

          comboBox(HuggingFaceModelSortKey.entries).apply {
            setupComboBoxDefaults(component)
            component.addActionListener(createComboBoxActionListener({ it as HuggingFaceModelSortKey? }, { model.sortKey = it }))
            component.renderer = defaultCellRenderer { value ->
              if (value is HuggingFaceModelSortKey) {
                value.displayName
              } else ""
            }
          }  // sort by

          comboBox(licenseTags).apply {
            setupComboBoxDefaults(component)
            component.prototypeDisplayValue = PROTOTYPE_LICENSE_TAG
            component.addActionListener(createComboBoxActionListener({ it as HfTag? }, { model.license = it }))

            component.renderer = defaultCellRenderer { value ->
              when {
                value == null && component.selectedItem == null -> HuggingFaceProBundle.message("python.hugging.face.model.choice.license.placeholder")
                value == null -> HuggingFaceProBundle.message("python.hugging.face.model.choice.skip.selection")
                value is HfTag -> value.label
                else -> ""
              }
            }
          }  // license

          comboBox(otherTags).apply {
            setupComboBoxDefaults(component)
            component.prototypeDisplayValue = PROTOTYPE_OTHER_TAG
            component.addActionListener(createComboBoxActionListener({ it as HfTag? }, { model.otherTags = it }))

            component.renderer = defaultCellRenderer { value ->
              when {
                value == null && component.selectedItem == null -> HuggingFaceProBundle.message("python.hugging.face.model.choice.tags.placeholder")
                value == null -> HuggingFaceProBundle.message("python.hugging.face.model.choice.skip.selection")
                value is HfTag -> value.label
                else -> ""
              }
            }
          }  // other tags

        }  // <<< row ends
      }  // indent scope
    }.apply {
      background = JBColor.WHITE
      border = CustomLineBorder(OnePixelDivider.BACKGROUND, 0, 0, 1, 0)
    }
  }

  private fun <T> createComboBoxActionListener(getSelected: (Any?) -> T?, setValue: (T?) -> Unit) : ActionListener {
    var lastSelected: T? = null
    return ActionListener {
      val newSelected = getSelected((it.source as ComboBox<*>).selectedItem)
      if (newSelected != lastSelected) {
        setValue(newSelected)
        lastSelected = newSelected
        onSelectionChanged()
      }
    }
  }

  private fun <T> setupComboBoxDefaults(component: ComboBox<T>) {
    component.apply {
      border = BorderFactory.createEmptyBorder()
      background = JBColor.WHITE
      isSwingPopup = false
    }
  }

  private fun defaultCellRenderer(render: (Any?) -> String): DefaultListCellRenderer {
    return object : DefaultListCellRenderer() {
      override fun getListCellRendererComponent(list: JList<*>?, value: Any?, index: Int, isSelected: Boolean, cellHasFocus: Boolean): Component {
        super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
        @NlsSafe val renderedText = render(value)
        text = renderedText
        return this
      }
    }
  }

  companion object {
    // prototypes are needed to limit the comboBox width
    private val PROTOTYPE_LICENSE_TAG = HfTag("-", HuggingFaceProBundle.message("python.hugging.face.model.combobox.license.tag"), "dummy")
    private val PROTOTYPE_OTHER_TAG = HfTag("-", HuggingFaceProBundle.message("python.hugging.face.model.combobox.other.tag"), "dummy")
  }
}
