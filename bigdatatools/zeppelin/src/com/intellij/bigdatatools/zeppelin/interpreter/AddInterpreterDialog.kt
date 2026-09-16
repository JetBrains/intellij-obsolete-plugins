package com.intellij.bigdatatools.zeppelin.interpreter

import com.intellij.CommonBundle
import com.intellij.bigdatatools.zeppelin.components.service.ZeppelinInterpreterSettingsManager
import com.intellij.bigdatatools.zeppelin.models.interpreter.InterpreterSettings
import com.intellij.bigdatatools.zeppelin.utils.ZepMessagesBundle
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.util.Disposer
import java.awt.event.ActionEvent
import javax.swing.AbstractAction
import javax.swing.JComponent

class AddInterpreterDialog(private val manager: ZeppelinInterpreterSettingsManager, showPropertiesDetails: Boolean)
  : DialogWrapper(null, true) {

  companion object {
    private const val ADD_INTERPRETER_SETTINGS_BOUNDS = "zeppelin.notebook.add.interpreter.settings.bounds"
  }

  val panel = InterpreterSettingsPanel(InterpreterSettingsPanel.Mode.CREATION, manager, showPropertiesDetails).apply {
    val name = getSuitableNewInterpreterName()
    showDetails(InterpreterSettings(id = name, name = name, group = name))
  }

  init {
    Disposer.register(disposable, panel)
    title = ZepMessagesBundle.message("interpreter.settings.add.dialog.title")
    init()
  }

  private fun getSuitableNewInterpreterName(): String {
    var name = "Unnamed"
    for (i in 1..100) {
      if (manager.getInterpretersSettings().find { it.name == name } == null) {
        return name
      }

      name = "Unnamed_${i}"
    }

    return "Unnamed"
  }

  override fun createCenterPanel(): JComponent = panel
  override fun getStyle() = DialogStyle.COMPACT
  override fun getDimensionServiceKey() = ADD_INTERPRETER_SETTINGS_BOUNDS

  override fun createActions() = arrayOf(
    object : AbstractAction(CommonBundle.getAddButtonText(), null) {
      override fun actionPerformed(p0: ActionEvent?) {
        panel.getSettings() ?: return // There we have validation and user will get a warning.
        close(OK_EXIT_CODE)
      }
    },
    cancelAction
  )
}