package com.intellij.bigdatatools.visualization.inlays.components

import com.intellij.bigdatatools.coreUi.ui.doOnChange
import com.intellij.bigdatatools.visualization.utils.VisMessagesBundle
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.fields.ExtendableTextField
import com.jetbrains.bigdatatools.common.table.models.DataFrameColumnModel
import com.jetbrains.bigdatatools.common.ui.CustomComponentActionImpl
import com.jetbrains.bigdatatools.common.ui.addSearchExtension
import com.jetbrains.bigdatatools.common.util.ToolbarUtils
import java.awt.Dimension
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JComponent

class ColumnsVisibilityDialog(private val columnModel: DataFrameColumnModel, private val columns: List<ColumnVisibility>) : DialogWrapper(
  null, false) {

  class ColumnVisibility(val name: String, val modelIndex: Int, var visible: Boolean)

  private val columnsListModel = JBList.createDefaultListModel(columns)
  private val columnsList = JBList(columnsListModel)

  init {
    title = VisMessagesBundle.message("columns.visibility.dialog.title")
    columnsList.cellRenderer = CheckboxListCellRenderer()
    columnsList.addMouseListener(object : MouseAdapter() {
      override fun mousePressed(e: MouseEvent) {
        val component = columnsList.cellRenderer.getListCellRendererComponent(columnsList, null, 0, false, false)
        val index = columnsList.locationToIndex(e.point)
        if (index != -1 && e.point.x < component.preferredSize.width) {
          val checkbox = columnsListModel.getElementAt(index)
          checkbox.visible = !checkbox.visible
          columnsList.repaint()
        }
      }
    })

    init()
    dimensionServiceKey
  }

  override fun createCenterPanel() = JBScrollPane(columnsList).apply { minimumSize = Dimension(310, 150) }

  override fun doOKAction() {
    while (columnModel.columnCount != 0) {
      columnModel.removeColumn(columnModel.getColumn(0))
    }

    columnsListModel.elements().toList().forEach { s ->
      if (s.visible) {
        columnModel.addColumn(s.modelIndex, s.name)
      }
    }

    super.doOKAction()
  }

  private fun swap(a: Int, b: Int) {
    val aObject = columnsListModel.getElementAt(a)
    val bObject = columnsListModel.getElementAt(b)
    columnsListModel.set(a, bObject)
    columnsListModel.set(b, aObject)
  }

  override fun createNorthPanel(): JComponent {
    val searchField = ExtendableTextField(15)
    searchField.addSearchExtension()

    searchField.doOnChange {
      columnsListModel.removeAllElements()
      columnsListModel.addAll(columns.filter { searchField.text.isNullOrEmpty() || it.name.contains(searchField.text) })
    }

    val showAll = DumbAwareAction.create(VisMessagesBundle.message("columns.action.showall"), AllIcons.Actions.Selectall) {
      columnsListModel.elements().iterator().forEach { it.visible = true }
      columnsList.repaint()
    }

    val hideAll = DumbAwareAction.create(VisMessagesBundle.message("columns.action.hideall"), AllIcons.Actions.Unselectall) {
      columnsListModel.elements().iterator().forEach { it.visible = false }
      columnsList.repaint()
    }

    val moveUp = object : DumbAwareAction(VisMessagesBundle.message("columns.action.moveup"), null, AllIcons.Actions.MoveUp) {
      override fun actionPerformed(e: AnActionEvent) {
        if (columnsList.selectedIndex <= 0) {
          return
        }
        val selectedIndex = columnsList.selectedIndex
        swap(selectedIndex, selectedIndex - 1)
        columnsList.selectedIndex = selectedIndex - 1
        columnsList.ensureIndexIsVisible(selectedIndex - 1)
      }

      override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = columnsList.selectedIndex > 0
      }

      override fun getActionUpdateThread() = ActionUpdateThread.BGT
    }

    val moveDown = object : DumbAwareAction(VisMessagesBundle.message("columns.action.movedown"), null, AllIcons.Actions.MoveDown) {
      override fun actionPerformed(e: AnActionEvent) {
        if (columnsList.selectedIndex == -1 || columnsList.selectedIndex == columnsListModel.size() - 1) {
          return
        }
        val selectedIndex = columnsList.selectedIndex
        swap(selectedIndex, selectedIndex + 1)
        columnsList.selectedIndex = selectedIndex + 1
        columnsList.ensureIndexIsVisible(selectedIndex + 1)
      }

      override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = columnsList.selectedIndex != -1 && columnsList.selectedIndex != columnsListModel.size() - 1
      }

      override fun getActionUpdateThread() = ActionUpdateThread.BGT
    }

    val toolbar = ToolbarUtils.createActionToolbar("BDTZeppelinVariableView",
                                                   DefaultActionGroup(CustomComponentActionImpl(searchField), showAll, hideAll,
                                                                      moveUp, moveDown), true).apply {
      targetComponent = columnsList
    }

    return toolbar.component
  }

  override fun getDimensionServiceKey() = "com.intellij.bigdatatools.visualization.inlays.components.columns.visibility.bounds"
}