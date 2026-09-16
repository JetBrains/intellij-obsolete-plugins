package com.intellij.bigdatatools.plugin.spark.java.submit

import com.intellij.bigdatatools.coreUi.ui.onDoubleClick
import com.intellij.icons.AllIcons
import com.intellij.openapi.options.ex.SingleConfigurableEditor
import com.intellij.openapi.options.newEditor.SettingsDialog
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ui.configuration.ProjectStructureConfigurable
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.packaging.artifacts.ArtifactManager
import com.intellij.packaging.artifacts.ArtifactPointer
import com.intellij.packaging.artifacts.ArtifactPointerManager
import com.intellij.ui.SimpleListCellRenderer
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.jetbrains.spark.submit.util.SparkMessagesBundle
import java.awt.Dimension
import java.awt.event.ActionEvent
import javax.swing.Action
import javax.swing.DefaultListModel
import javax.swing.JComponent
import javax.swing.JList
import javax.swing.ListSelectionModel

class SelectArtifactDialog(val project: Project,
                           var selectedArtifact: ArtifactPointer?,
                           private var allArtifacts: List<ArtifactPointer>) {
  fun show(): Boolean {

    val list = JBList(allArtifacts)
    list.selectionMode = ListSelectionModel.SINGLE_SELECTION
    list.emptyText.clear()
    list.emptyText.appendText(SparkMessagesBundle.message("dialog.select.artifact.empty"))
    list.emptyText.appendSecondaryText(SparkMessagesBundle.message("dialog.select.artifact.link.open.artifact.settings"),
                                       SimpleTextAttributes.LINK_ATTRIBUTES) {
      openArtifactPanel()
      refresh(list)
    }
    list.cellRenderer = object : SimpleListCellRenderer<ArtifactPointer>() {
      override fun customize(list: JList<out ArtifactPointer>, value: ArtifactPointer, index: Int, selected: Boolean, hasFocus: Boolean) {
        text = value.artifactName
        icon = AllIcons.Nodes.Artifact
      }
    }

    val dialog = object : DialogWrapper(project, true) {

      init {
        title = SparkMessagesBundle.message("dialog.select.artifact.title")
        init()

        list.onDoubleClick { e ->
          val index = list.locationToIndex(e.point)
          if (index != -1) {
            doOKAction()
          }
        }
      }

      override fun getPreferredFocusedComponent(): JComponent = list

      override fun createCenterPanel(): JComponent = JBScrollPane(list).apply {
        preferredSize = Dimension(width, preferredSize.height.coerceAtLeast(200))
      }

      override fun createLeftSideActions(): Array<Action> {
        return arrayOf(object : DialogWrapperAction(SparkMessagesBundle.message("dialog.select.artifact.button.open.artifact.settings")) {
          override fun doAction(e: ActionEvent?) {
            openArtifactPanel()
            refresh(list)
          }
        })
      }
    }

    if (dialog.showAndGet()) {
      selectedArtifact = list.selectedValue
      return true
    }

    return false
  }

  private fun refresh(list: JBList<ArtifactPointer>) {
    val defaultListModel = list.model as? DefaultListModel ?: return

    defaultListModel.clear()
    val new = sortedArtifactPointers(project)
    defaultListModel.addAll(new)
  }

  private fun openArtifactPanel() {
    val configurable = ProjectStructureConfigurable.getInstance(project)
    val configurablePanel = object : SingleConfigurableEditor(project, configurable, SettingsDialog.DIMENSION_KEY) {
      override fun getStyle(): DialogStyle = DialogStyle.COMPACT
    }
    configurable.navigateTo(configurable.createArtifactPlace(null), true)
    configurablePanel.show()
  }

  companion object {
    fun sortedArtifactPointers(project: Project): List<ArtifactPointer> {
      val sortedArtifacts = ArtifactManager.getInstance(project).sortedArtifacts
      val pointerManager = ArtifactPointerManager.getInstance(project)
      val pointArtifacts = sortedArtifacts.map { pointerManager.createPointer(it) }
      return pointArtifacts
    }
  }
}