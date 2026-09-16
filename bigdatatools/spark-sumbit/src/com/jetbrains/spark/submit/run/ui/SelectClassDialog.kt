package com.jetbrains.spark.submit.run.ui

import com.intellij.bigdatatools.coreUi.ui.doOnChange
import com.intellij.bigdatatools.coreUi.ui.onDoubleClick
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.fields.ExtendableTextField
import com.intellij.ui.speedSearch.FilteringListModel
import com.jetbrains.bigdatatools.common.ui.addSearchExtension
import com.jetbrains.spark.submit.util.SparkMessagesBundle
import java.awt.BorderLayout
import java.util.regex.Pattern
import javax.swing.JComponent
import javax.swing.JPanel

class SelectClassDialog(val project: Project,
                        var selectedClass: String?,
                        private var allClasses: List<String>) {
  fun show(): Boolean {

    val dialog = object : DialogWrapper(project, true) {
      private val panel = JPanel(BorderLayout())
      private val textField = ExtendableTextField()
      private val listFilterModel = FilteringListModel(JBList.createDefaultListModel(allClasses))
      private val list = JBList(listFilterModel)

      init {
        title = SparkMessagesBundle.message("dialog.select.class.title")

        list.emptyText.clear()
        list.emptyText.appendText(SparkMessagesBundle.message("dialog.select.class.empty"))

        textField.focusTraversalKeysEnabled = false
        textField.addSearchExtension()
        textField.doOnChange {
          val pattern = Pattern.compile(Pattern.quote(textField.text), Pattern.CASE_INSENSITIVE)
          listFilterModel.setFilter { pattern.matcher(it).find() }
          if (!list.isEmpty) {
            list.selectedIndex = 0
          }
        }

        listFilterModel.setFilter { true }

        panel.add(textField, BorderLayout.NORTH)
        panel.add(JBScrollPane(list), BorderLayout.CENTER)

        if (!list.isEmpty) {
          if (!selectedClass.isNullOrBlank())
            @Suppress("HardCodedStringLiteral")
            list.setSelectedValue(selectedClass, true)
          else
            list.selectedIndex = 0
        }

        list.onDoubleClick { e ->
          val index = list.locationToIndex(e.point)
          if (index != -1) {
            doOKAction()
          }
        }

        init()
      }

      override fun getPreferredFocusedComponent(): JComponent = textField

      override fun createCenterPanel(): JComponent = panel

      fun selectedClass(): String? = list.selectedValue
    }

    if (dialog.showAndGet()) {
      selectedClass = dialog.selectedClass()
      return true
    }

    return false
  }
}