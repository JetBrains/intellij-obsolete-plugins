// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.python.huggingFace.modelChoice.ui

import com.intellij.python.community.impl.huggingFace.api.HuggingFaceEntityBasicApiData
import com.intellij.python.community.impl.huggingFace.service.HuggingFaceCoroutine
import com.intellij.python.huggingFace.modelChoice.modelHandling.HfModelChoiceHtmlContentGenerator
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBList
import kotlinx.coroutines.launch
import org.jetbrains.annotations.ApiStatus
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.DefaultListModel
import javax.swing.ListSelectionModel
import javax.swing.SwingUtilities


@ApiStatus.Internal
class HfModelList(listModel: DefaultListModel<HuggingFaceEntityBasicApiData>,
                  private val contentGenerator: HfModelChoiceHtmlContentGenerator, // Inject the generator
                  private val onUpdateSelectedModel: (HuggingFaceEntityBasicApiData) -> Unit,
                  private val onUpdateModelCardPanel: (String) -> Unit,
                  private val onDoubleClick: (HuggingFaceEntityBasicApiData) -> Unit
) : JBList<HuggingFaceEntityBasicApiData>(listModel) {

  init {
    cellRenderer = HfModelSearchCellRenderer()
    selectionMode = ListSelectionModel.SINGLE_SELECTION
    background = JBColor.WHITE

    addListSelectionListener { event ->
      if (!event.valueIsAdjusting) {
        val selectedData = selectedValue ?: return@addListSelectionListener

        onUpdateSelectedModel(selectedData)

        HuggingFaceCoroutine.Utils.ioScope.launch {
          val htmlContent = contentGenerator.generateHtmlContent(selectedData)
          SwingUtilities.invokeLater { onUpdateModelCardPanel(htmlContent) }
        }
      }
    }

    addMouseListener(object : MouseAdapter() {
      override fun mouseClicked(e: MouseEvent) {
        if (e.clickCount == 2) {
          val index = locationToIndex(e.point)
          onDoubleClick(model.getElementAt(index))
        }
      }
    })
  }
}
