package com.jetbrains.bigdatatools.flink.toolwindow.controllers.jars

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.Separator
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.components.JBTextField
import com.jetbrains.bigdatatools.common.monitoring.actions.OpenUrlAction
import com.jetbrains.bigdatatools.common.monitoring.data.model.FilterAdapter
import com.jetbrains.bigdatatools.common.monitoring.toolwindow.AbstractTableController
import com.jetbrains.bigdatatools.common.ui.CustomComponentActionImpl
import com.jetbrains.bigdatatools.common.ui.ToolbarLabelActionImpl
import com.jetbrains.bigdatatools.common.ui.filter.CountFilterPopupComponent
import com.jetbrains.bigdatatools.flink.data.FlinkDataManager
import com.jetbrains.bigdatatools.flink.model.JarInfo
import com.jetbrains.bigdatatools.flink.submit.RunJarStepDialog
import com.jetbrains.bigdatatools.flink.toolwindow.config.FlinkToolWindowSettings
import com.jetbrains.bigdatatools.flink.util.FlinkMessagesBundle

class FlinkJarsController(val project: Project, private val dataManager: FlinkDataManager) : AbstractTableController<JarInfo>() {
  private val addJarAction = DumbAwareAction.create(FlinkMessagesBundle.message("jar.action.add"), AllIcons.General.Add) {
      val chooser = FileChooserDescriptorFactory.createSingleFileDescriptor("jar")
        .withTitle(FlinkMessagesBundle.message("jar.action.upload"))
        .withHideIgnored(false)
        .withShowHiddenFiles(true)

      var filesToUpload: VirtualFile? = null
      FileChooser.chooseFile(chooser, project, null) { uploadingFile ->
        filesToUpload = uploadingFile
      }

      val path = filesToUpload?.path
      if (path != null) {
        dataManager.uploadJar(path)
      }
  }

  private val runJarAction = object : DumbAwareAction(FlinkMessagesBundle.message("jar.action.execute"), null, AllIcons.Actions.Execute) {
    override fun actionPerformed(e: AnActionEvent) {
      val jar: JarInfo = getSelectedItem() ?: return

      val jarId = jar.id
      val entryClass = jar.entry.firstOrNull()?.name ?: ""
      val dialog = RunJarStepDialog(project, entryClass, FlinkMessagesBundle.message("dialog.jar.run.title"))
      if (!dialog.showAndGet())
        return

      dataManager.runJar(jarId, dialog.getResult())
    }

    override fun update(e: AnActionEvent) {
      e.presentation.isEnabledAndVisible = getSelectedItem() != null
    }

    override fun getActionUpdateThread() = ActionUpdateThread.BGT
  }

  private val deleteJarAction = object : DumbAwareAction(FlinkMessagesBundle.message("jar.action.delete"), null, AllIcons.General.Remove) {
    override fun actionPerformed(e: AnActionEvent) {
      val jar: JarInfo = getSelectedItem() ?: return

      val res = Messages.showYesNoDialog(project,
                                         FlinkMessagesBundle.message("dialog.jar.delete.message", jar.name),
                                         FlinkMessagesBundle.message("dialog.jar.delete.title"),
                                         Messages.getQuestionIcon())
      if (res != Messages.OK)
        return
      dataManager.removeJar(jar)
    }

    override fun update(e: AnActionEvent) {
      e.presentation.isEnabledAndVisible = getSelectedItem() != null
    }

    override fun getActionUpdateThread() = ActionUpdateThread.BGT
  }

  private val connectionId = dataManager.connectionData.innerId

  init {
    init()
  }

  override fun createTopLeftToolbarActions(): List<AnAction> {
    val settings = FlinkToolWindowSettings.getInstance()
    val config = settings.getOrCreateConfig(connectionId)
    val userText = JBTextField(config.jarFilter, 8)

    FilterAdapter.install(dataTable.tableModel, userText, JarInfo.TEXT_FILTER) { userQuery ->
      config.jarFilter = userQuery
      dataManager.updater.invokeRefreshModel(dataManager.jarsModel)
    }

    val countFilter = CountFilterPopupComponent(FlinkMessagesBundle.message("flink.filter.limit"), config.jarLimit)
    FilterAdapter.install(dataTable.tableModel, countFilter, JarInfo.LIMIT_FILTER) { limit ->
      config.jarLimit = limit
      dataManager.updater.invokeRefreshModel(dataManager.jarsModel)
    }

    return listOf(ToolbarLabelActionImpl(FlinkMessagesBundle.message("flink.filter.text")),
                  CustomComponentActionImpl(userText),
                  CustomComponentActionImpl(countFilter))
  }

  override fun showColumnFilter(): Boolean = false

  override fun getColumnSettings() = FlinkToolWindowSettings.getInstance().jarsColumnSettings

  override fun getRenderableColumns() = JarInfo.renderableColumns

  override fun getDataModel() = dataManager.jarsModel

  override fun getAdditionalActions(): List<AnAction> = listOf(
    runJarAction,
    addJarAction,
    deleteJarAction,
    Separator.create(),
    OpenUrlAction(dataManager) { "/#/submit" }
  )
}
