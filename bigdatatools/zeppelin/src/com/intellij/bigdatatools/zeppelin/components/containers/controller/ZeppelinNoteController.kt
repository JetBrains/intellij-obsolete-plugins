package com.intellij.bigdatatools.zeppelin.components.containers.controller

import com.intellij.bigdatatools.coreUi.util.NotificationUtils
import com.intellij.bigdatatools.zeppelin.api.remote.ZeppelinApi
import com.intellij.bigdatatools.zeppelin.api.remote.ZeppelinNoRightsException
import com.intellij.bigdatatools.zeppelin.components.containers.service.ZeppelinNoteCacheConnection
import com.intellij.bigdatatools.zeppelin.editor.ZeppelinEditor
import com.intellij.bigdatatools.zeppelin.models.interpreter.Interpreter
import com.intellij.bigdatatools.zeppelin.utils.ZepMessagesBundle
import com.intellij.codeInsight.hint.HintManager
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.util.PairFunction
import com.jetbrains.bigdatatools.common.util.invokeLater
import com.jetbrains.bigdatatools.common.util.launchBackgroundTask
import javax.swing.JCheckBox

class ZeppelinNoteController(val project: Project,
                             val cachedConnection: ZeppelinNoteCacheConnection,
                             val zeppelinEditor: ZeppelinEditor) {
  private val api get() = cachedConnection.api
  private val editor = zeppelinEditor.editor

  val bindings: List<Interpreter>
    get() = cachedConnection.interpreterBindings

  val interpreterSettings
    get() = cachedConnection.interpreterSettings

  val noteId: String
    get() = cachedConnection.noteId

  val config = cachedConnection.config

  val file = zeppelinEditor.file

  val originFile = zeppelinEditor.file.originFile

  val note = zeppelinEditor.note

  fun saveNewInterpreterSettings(newBindings: List<Interpreter>) {
    if (bindings == newBindings) return
    if (!checkConnectionWithNotification()) return

    api.saveInterpreterBindings(newBindings, noteId)
    cachedConnection.refreshInterpreterBindingsAsync()
  }

  fun restartInterpreterWithConfirmation(interpreterId: String, interpreterName: String) {
    Messages.showCheckboxMessageDialog(ZepMessagesBundle.message("interpreter.settings.restart.message", interpreterName),
                                       ZepMessagesBundle.message("action.ZeppelinRestartCellAction.text"),
                                       arrayOf(Messages.getOkButton(), Messages.getCancelButton()),
                                       ZepMessagesBundle.message("interpreter.settings.restart.checkbox"),
                                       PropertiesComponent.getInstance().getBoolean(RESTART_FOR_NOTE_KEY, false), 0, 0,
                                       Messages.getQuestionIcon(),
                                       PairFunction { exitCode: Int, cb: JCheckBox ->
                                         if (exitCode == Messages.OK) {
                                           restartInterpreter(interpreterId, interpreterName, cb.isSelected)
                                           PropertiesComponent.getInstance().setValue(RESTART_FOR_NOTE_KEY, cb.isSelected)
                                         }
                                         exitCode
                                       })
  }

  fun restartInterpreterWithConfirmation(interpreter: Interpreter) = restartInterpreterWithConfirmation(interpreter.id, interpreter.name)

  private fun restartInterpreter(interpreterId: String, interpreterName: String, forNote: Boolean) = launchBackgroundTask(project,
                                                                                                                          ZepMessagesBundle.message(
                                                                                                                            "interpreter.restart.desc"),
                                                                                                                          cancelable = false) {
    it.text2 = ZepMessagesBundle.message("interpreter.restart.text2", interpreterName, config.getNameWithAddress())
    if (!checkConnectionWithNotification())
      return@launchBackgroundTask

    restartInterpreterAndNotify(api, if (forNote) noteId else null, interpreterId, interpreterName)

    cachedConnection.refreshInterpretersAsync()
    cachedConnection.refreshInterpreterBindingsAsync()
  }

  fun checkConnectionWithNotification(): Boolean {
    val isConnected = cachedConnection.isConnected()
    if (!isConnected) invokeLater {
      HintManager.getInstance().showInformationHint(editor, ZepMessagesBundle.message(
        "hint.text.html.notebook.disconnected.please.refresh.connection.html"))
    }

    return isConnected
  }

  companion object {

    private const val RESTART_FOR_NOTE_KEY = "zeppelin.interpreter.settings.restart.for.note"

    fun restartInterpreterAndNotify(api: ZeppelinApi, noteId: String?, interpreterId: String, interpreterName: String) {
      try {
        api.restartInterpreter(interpreterId, noteId)
        NotificationUtils.notifySuccess(ZepMessagesBundle.message("restart.success", interpreterName),
                                           title = ZepMessagesBundle.message("restart.interpreter"))
      }
      catch (e: ZeppelinNoRightsException) {
        NotificationUtils.notifyText(ZepMessagesBundle.message("no.rights.restart", interpreterName),
                                        title = ZepMessagesBundle.message("restart.interpreter"))
      }
      catch (e: Exception) {
        NotificationUtils.notifyException(e, title = ZepMessagesBundle.message("restart.interpreter"))
      }
    }
  }
}