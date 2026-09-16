package com.intellij.bigdatatools.zeppelin.interpreter

import com.intellij.bigdatatools.coreUi.ui.doOnChange
import com.intellij.bigdatatools.coreUi.fields.CustomListCellRenderer
import com.intellij.bigdatatools.zeppelin.components.containers.controller.ZeppelinNoteController
import com.intellij.bigdatatools.zeppelin.components.service.ZeppelinInterpreterSettingsListener
import com.intellij.bigdatatools.zeppelin.components.service.ZeppelinInterpreterSettingsManager
import com.intellij.bigdatatools.zeppelin.models.interpreter.InterpreterSettings
import com.intellij.bigdatatools.zeppelin.utils.ZepMessagesBundle
import com.intellij.icons.AllIcons
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.DataSink
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.actionSystem.UiDataProvider
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.ui.MessageDialogBuilder
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.Splitter
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.Version
import com.intellij.sql.indexOf
import com.intellij.ui.OnePixelSplitter
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBTextField
import com.intellij.ui.components.fields.ExtendableTextField
import com.intellij.ui.scale.JBUIScale
import com.jetbrains.bigdatatools.common.ui.addSearchExtension
import com.jetbrains.bigdatatools.common.util.ToolbarUtils
import com.intellij.bigdatatools.coreUi.util.executeOnPooledThread
import com.jetbrains.bigdatatools.common.util.invokeLater
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.BoxLayout
import javax.swing.DefaultListSelectionModel
import javax.swing.JComponent
import javax.swing.JPanel

class InterpreterSettingsDialog(private val noteController: ZeppelinNoteController,
                                val manager: ZeppelinInterpreterSettingsManager) : ZeppelinInterpreterSettingsListener, BaseSettingsDialog, Disposable {

  companion object {
    private const val HELP_TOPIC_ID = "big.data.tools.zeppelin.interpreter.settings"
    private const val LIST_SPLITTER_PROPORTION_KEY = "zeppelin.notebook.interpreter.settings.splitter.proportion"
  }

  private val listModel = FilteredListModel<InterpreterSettings> { it.name }

  private val list = JBList(listModel).apply {
    cellRenderer = CustomListCellRenderer<InterpreterSettings> { "   ${it.name}" }
    autoscrolls = true
    selectionMode = DefaultListSelectionModel.SINGLE_SELECTION
  }

  private val showPropertiesDetails = isDetailsVisible()

  private val details = InterpreterSettingsPanel(InterpreterSettingsPanel.Mode.EDIT, manager, showPropertiesDetails)
  private var selectedInterpreter: InterpreterSettings? = null

  val component: OnePixelSplitter = object : OnePixelSplitter(false, LIST_SPLITTER_PROPORTION_KEY, 0.3f), UiDataProvider {
    override fun uiDataSnapshot(sink: DataSink) {
      sink[PlatformDataKeys.HELP_ID] = HELP_TOPIC_ID
    }
  }.apply {
    dividerPositionStrategy = Splitter.DividerPositionStrategy.KEEP_FIRST_SIZE
    secondComponent = details
    firstComponent = createList(manager.getInterpretersSettings().map { it.copy() })
  }

  init {
    manager.addListener(this)
    Disposer.register(this, details)
  }

  private fun updateCurrentlySelectedElementInList() {
    val localSelectedInterpreter = selectedInterpreter
    if (localSelectedInterpreter != null) {
      val indexOfFound = listModel.data.indexOf { it.id == localSelectedInterpreter.id }
      if (indexOfFound != -1) {
        details.getSettings()?.let { listModel.replaceElement(it, indexOfFound) }
      }
    }
  }

  private fun createList(elements: List<InterpreterSettings>): JComponent {

    listModel.data = elements

    val scrollPane = ScrollPaneFactory.createScrollPane(list, true)

    list.addListSelectionListener { e ->
      if (e.valueIsAdjusting) return@addListSelectionListener
      updateCurrentlySelectedElementInList()
      selectedInterpreter = list.selectedValue
      details.showDetails(list.selectedValue)
    }

    if (listModel.size != 0) {
      list.selectedIndex = 0
    }

    val horizontalPanel = JPanel().apply {
      layout = BoxLayout(this, BoxLayout.Y_AXIS)
      add(ToolbarUtils.createActionToolbar(this, "BDTZeppelinInterpreterSettings", createActions(), true).component)
      add(createFilterText())
    }

    return JPanel(BorderLayout()).apply {
      add(horizontalPanel, BorderLayout.NORTH)
      add(scrollPane, BorderLayout.CENTER)
      minimumSize = Dimension(JBUIScale.scale(150), minimumSize.height)
    }
  }

  private fun isDetailsVisible(): Boolean {
    val zeppelinInfo = manager.connection.zeppelinInfo
    return if (zeppelinInfo == null) true else Version.parseVersion(zeppelinInfo.version)?.isOrGreaterThan(0, 9, 0) ?: true
  }

  override fun dispose() {
    manager.removeListener(this)
  }

  //region ZeppelinInterpreterSettingsListener
  override fun updateInterpreterSettings(interpreterSettings: List<InterpreterSettings>) {
    invokeLater {
      val selectedInterpreter = list.selectedValue

      listModel.data = interpreterSettings.map { it.copy() }

      if (selectedInterpreter != null) {
        val found = listModel.filteredData.find { it.name == selectedInterpreter.name }
        found?.let { list.setSelectedValue(found, true) }
      }

      if (list.selectedIndex == -1 && !list.isEmpty) {
        list.selectedIndex = 0
      }
    }
  }

  override fun updateAvailableInterpreterTemplates(interpreters: List<InterpreterTemplate>, exception: Throwable?) {
    invokeLater {
      details.updateAvailableInterpreterTemplates(interpreters)
    }
  } //endregion ZeppelinInterpreterSettingsListener

  private fun createActions(): List<AnAction> {

    val refreshAction = DumbAwareAction.create(ZepMessagesBundle.message("interpreter.settings.action.refresh"), AllIcons.Actions.Refresh) {
      executeOnPooledThread {
        try {
          manager.refreshAsync()
        }
        catch (e: Exception) {
          BaseSettingsDialog.showErrorMessage(ZepMessagesBundle.message("interpreter.settings.error.refresh"), e)
        }
      }
    }

    val addAction = DumbAwareAction.create(ZepMessagesBundle.message("interpreter.settings.action.add"), AllIcons.General.Add) {
      try {
        manager.getInterpreterTemplates()
      }
      catch (e: Exception) {
        BaseSettingsDialog.showErrorMessage(ZepMessagesBundle.message("interpreter.settings.error.getTemplates"), e)
        return@create
      }

      val dialog = AddInterpreterDialog(manager, showPropertiesDetails)
      if (dialog.showAndGet()) {
        dialog.panel.getSettings()?.let {
          listModel.addElement(it)
          list.setSelectedValue(it, true)
        }
      }
    }

    val removeAction = DumbAwareAction.create(ZepMessagesBundle.message("interpreter.settings.action.remove"), AllIcons.General.Remove) {
      val contextComponent = it.getData(PlatformDataKeys.CONTEXT_COMPONENT) as? JComponent ?: component

      val selectedInterpreter = list.selectedValue

      if (selectedInterpreter == null) {
        Messages.showInfoMessage(ZepMessagesBundle.message("interpreter.settings.remove.nothing.message"),
                                 ZepMessagesBundle.message("interpreter.settings.remove.title"))
      }
      else {
        val res = MessageDialogBuilder.yesNo(ZepMessagesBundle.message("interpreter.settings.remove.title"),
                                             ZepMessagesBundle.message("interpreter.settings.remove.message",
                                                                       selectedInterpreter.name)).ask(contextComponent)
        if (res) {
          listModel.removeElement(selectedInterpreter)
        }
      }
    }

    val restartAction = DumbAwareAction.create(ZepMessagesBundle.message("interpreter.settings.action.restart"), AllIcons.Actions.Restart) {
      val selectedInterpreter = list.selectedValue ?: return@create
      noteController.restartInterpreterWithConfirmation(selectedInterpreter.id, selectedInterpreter.name)
    }

    return listOf(refreshAction, addAction, removeAction, restartAction)
  }

  override fun isModified(): Boolean {
    updateCurrentlySelectedElementInList()
    return FilteredListModel<InterpreterSettings> { it.name }.apply { data = manager.getInterpretersSettings().map { it.copy() } }.data != listModel.data
  }

  override fun apply(): Boolean {
    executeOnPooledThread {
      var hasChanges = false
      listModel.data.forEach { interpreterSettings ->
        try {
          val found = manager.getInterpretersSettings().find { it.id == interpreterSettings.id }
          if (found == interpreterSettings) {
            return@forEach
          }

          hasChanges = if (found == null) {
            manager.addInterpreterSettings(interpreterSettings)
            true
          }
          else {
            manager.updateInterpreterSettings(interpreterSettings)
            true
          }
        }
        catch (e: Exception) {
          BaseSettingsDialog.showErrorMessage(ZepMessagesBundle.message("interpreter.settings.error.addOrUpdate", interpreterSettings.name),
                                              e)
        }
      }

      // Processing deleted.
      manager.getInterpretersSettings().filter { old -> listModel.data.find { it.id == old.id } == null }.forEach {
        manager.removeInterpreterSettings(it)
        hasChanges = true
      }

      if (hasChanges) {
        manager.refreshAsync()
      }
    }
    return true
  }

  private fun createFilterText(): JBTextField {
    return ExtendableTextField().apply {
      addSearchExtension()
      doOnChange {
        listModel.filter = { settings -> if (text.isBlank()) true else settings.name.lowercase().contains(text.lowercase()) }
      }
    }
  }
}