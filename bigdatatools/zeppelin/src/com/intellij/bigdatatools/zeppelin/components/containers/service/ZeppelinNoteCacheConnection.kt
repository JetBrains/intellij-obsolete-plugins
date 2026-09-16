package com.intellij.bigdatatools.zeppelin.components.containers.service

import com.google.gson.JsonObject
import com.intellij.bigdatatools.zeppelin.components.ZeppelinInstanceCachedConnection
import com.intellij.bigdatatools.zeppelin.components.connections.ZeppelinConnection
import com.intellij.bigdatatools.zeppelin.components.connections.ZeppelinConnectionListener
import com.intellij.bigdatatools.zeppelin.components.connections.ZeppelinConnectionProvider
import com.intellij.bigdatatools.zeppelin.dependency.collector.builtin.Repository
import com.intellij.bigdatatools.zeppelin.models.connection.AngularRemoveResponse
import com.intellij.bigdatatools.zeppelin.models.connection.AngularUpdateResponse
import com.intellij.bigdatatools.zeppelin.models.connection.Progress
import com.intellij.bigdatatools.zeppelin.models.connection.ZeppelinInfo
import com.intellij.bigdatatools.zeppelin.models.interpreter.Interpreter
import com.intellij.bigdatatools.zeppelin.models.interpreter.InterpreterSettings
import com.intellij.bigdatatools.zeppelin.models.notebook.NotebookInfo
import com.intellij.bigdatatools.zeppelin.models.notebook.ParagraphOutput
import com.intellij.bigdatatools.zeppelin.models.notebook.ZeppelinNotebook
import com.intellij.bigdatatools.zeppelin.settings.ZeppelinConnectionData
import com.intellij.openapi.Disposable
import com.intellij.openapi.diagnostic.Logger
import com.intellij.diagnostic.rethrowControlFlowException
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import org.jetbrains.annotations.TestOnly

class ZeppelinNoteCacheConnection(
  private val instanceConnection: ZeppelinInstanceCachedConnection,
  noteId: String,
  val project: Project?) : Disposable, ZeppelinConnectionProvider {
  val config: ZeppelinConnectionData = instanceConnection.config
  var noteId: String = noteId
    private set
  private val connection = ZeppelinConnection(config)

  val api get() = connection.api
  private val apiOrNull get() = connection.apiOrNull

  var isCollaborative: Boolean = false
    private set
  var interpreterBindings: List<Interpreter> = listOf()
    private set
  val interpreterSettings: List<InterpreterSettings>
    get() = instanceConnection.interpreterSettings

  var defaultBindingInterpreterSettings: InterpreterSettings? = null
    private set
  var bindingInterpreterSettings: List<InterpreterSettings> = calcBindingInterpretersSettings()
    private set

  override fun isConnected(): Boolean = connection.isConnected()

  val zeppelinInfo
    get() = connection.zeppelinInfo

  val tunnelUri: String?
    get() = connection.tunnelUri

  private val instanceConnectionListener = object : ZeppelinConnectionListener {
    override fun updateInterpreterSettings(interpreterSettings: List<InterpreterSettings>) {
      connectionListener.updateInterpreterSettings(interpreterSettings)

      updateCachedDefaultInterpreter()
      updateCachedBindingsInterpretersState()
      refreshInterpreterBindingsAsync()

    }

    override fun onDisconnected(statusCode: Int?, reason: String?) {
      //Note connection sometime can do not response on messages, so they have infinity connection lost timeout
      //So we need say them if connection have been really lost
      disconnect(null, reason)
    }
  }

  private val connectionListener = object : ZeppelinConnectionListener {
    override fun onConnected() {
      initRefreshState()

      notifyListeners {
        it.onConnected()
      }
    }

    override fun onDisconnected(statusCode: Int?, reason: String?) {
      notifyListeners {
        it.onDisconnected(statusCode, reason)
      }
    }

    override fun updateInterpreterBindings(bindings: List<Interpreter>) {
      if (interpreterBindings == bindings)
        return
      interpreterBindings = bindings

      notifyListeners {
        it.updateInterpreterBindings(bindings)
      }

      updateCachedDefaultInterpreter()
      updateCachedBindingsInterpretersState()
    }

    override fun updateInterpreterSettings(interpreterSettings: List<InterpreterSettings>) {
      notifyListeners {
        it.updateInterpreterSettings(this@ZeppelinNoteCacheConnection.interpreterSettings)
      }
    }

    override fun updateCollaborativeModeStatus(status: Boolean) {
      if (isCollaborative == status)
        return

      isCollaborative = status
      notifyListeners {
        it.updateCollaborativeModeStatus(status)
      }
    }

    override fun updateAngularObject(newAngularObject: AngularUpdateResponse) = notifyListeners {
      it.updateAngularObject(newAngularObject)
    }

    override fun removeAngularObject(angularObject: AngularRemoveResponse) = notifyListeners {
      it.removeAngularObject(angularObject)
    }

    override fun updateNotebook(notebook: ZeppelinNotebook) = notifyListeners {
      it.updateNotebook(notebook)
    }

    override fun addCell(jsonCell: JsonObject, index: Int) = notifyListeners {
      it.addCell(jsonCell, index)
    }

    override fun moveCell(sourceId: String, toIndex: Int) = notifyListeners {
      it.moveCell(sourceId, toIndex)
    }

    override fun removeCell(jsonCell: JsonObject) = notifyListeners {
      it.removeCell(jsonCell)
    }

    override fun updateCell(paragraphJson: JsonObject) = notifyListeners {
      it.updateCell(paragraphJson)
    }

    override fun onConnectionError(throwable: Throwable) = notifyListeners {
      it.onConnectionError(throwable)
    }

    override fun onZeppelinInfoChange(newInfo: ZeppelinInfo?) = notifyListeners {
      it.onZeppelinInfoChange(newInfo)
    }

    override fun updateNotebookList(notebooks: List<NotebookInfo>) = notifyListeners {
      it.updateNotebookList(notebooks)
    }

    override fun pathParagraph(paragraphId: String, patch: String) {
      isCollaborative = true
      notifyListeners {
        it.pathParagraph(paragraphId, patch)
      }
    }

    override fun updateProgress(progress: Progress) = notifyListeners {
      it.updateProgress(progress)
    }

    override fun onParagraphInfo(info: Map<String, Any>) = notifyListeners {
      it.onParagraphInfo(info)
    }

    override fun updateOutput(paragraphOutput: ParagraphOutput, isUpdate: Boolean) = notifyListeners {
      it.updateOutput(paragraphOutput, isUpdate)
    }

    override fun updateDefaultInterpreter(defaultInterpreterSettings: InterpreterSettings) = notifyListeners {
      it.updateDefaultInterpreter(defaultInterpreterSettings)
    }

    override fun updateBindingsInterpretersSettings(bindingInterpreterSettings: List<InterpreterSettings>) = notifyListeners {
      it.updateBindingsInterpretersSettings(bindingInterpreterSettings)
    }

    override fun updateRepositories(repositories: List<Repository>, requestException: Throwable?) = notifyListeners {
      it.updateRepositories(repositories, requestException)
    }

    override fun onServerError(info: String) = notifyListeners {
      it.onServerError(info)
    }
  }

  init {
    Disposer.register(this, connection)

    instanceConnection.addListener(instanceConnectionListener)
    connection.addListener(connectionListener)

    initRefreshState()
  }

  override fun dispose() {
    instanceConnection.removeListener(instanceConnectionListener)
    connection.removeListener(connectionListener)
  }

  fun refreshConnectionAsync(force: Boolean = false, project: Project?) =
    connection.refreshConnectionAsync(force, project)

  fun refreshInterpretersAsync() = instanceConnection.refreshInterpretersSettingsAsync()

  fun refreshInterpreterBindingsAsync() {
    if (noteId.isNotEmpty())
      apiOrNull?.getInterpreterBindings(noteId)
  }

  fun refreshNotebook() {
    if (noteId.isNotEmpty())
      api.getNotebookAsync(noteId)
  }

  fun updateNoteId(tempNoteId: String) {
    noteId = tempNoteId
    refreshNotebook()
    refreshInterpreterBindingsAsync()
  }

  @TestOnly
  fun refreshConnectionSync(project: Project, force: Boolean = false) = connection.refreshConnectionSync(force, project)
  fun disconnect(code: Int? = null, reason: String? = null) = connection.disconnect(code, reason)

  private var listeners = listOf<ZeppelinConnectionListener>()

  override fun addListener(listener: ZeppelinConnectionListener) {
    listeners = listeners + listener
  }

  override fun removeListener(listener: ZeppelinConnectionListener) {
    listeners = listeners - listener
  }

  private fun initRefreshState() {
    if (!isConnected()) return
    try {
      refreshInterpreterBindingsAsync()
      refreshNotebook()
    }
    catch (t: Throwable) {
      logger.error(t)
    }
  }

  private fun calcBindingInterpretersSettings(): List<InterpreterSettings> {
    val bindings = interpreterBindings
    val allInterpreters = interpreterSettings
    if (bindings.isEmpty() || allInterpreters.isEmpty()) return emptyList()
    return bindings.mapNotNull { bind -> allInterpreters.firstOrNull { bind.id == it.id } }
  }

  private fun updateCachedDefaultInterpreter() {
    if (interpreterSettings.isEmpty() || interpreterBindings.isEmpty())
      defaultBindingInterpreterSettings = null
    val defaultBinding = interpreterBindings.firstOrNull() ?: return
    val defaultBindingId = defaultBinding.id
    val newDefaultInterpreter = interpreterSettings.firstOrNull { it.id == defaultBindingId } ?: return

    if (defaultBindingInterpreterSettings == newDefaultInterpreter) return

    defaultBindingInterpreterSettings = newDefaultInterpreter

    notifyListeners {
      it.updateDefaultInterpreter(newDefaultInterpreter)
    }
  }

  private fun updateCachedBindingsInterpretersState() {
    val newBindingInterpreters = calcBindingInterpretersSettings()
    if (newBindingInterpreters == bindingInterpreterSettings) return

    bindingInterpreterSettings = newBindingInterpreters
    notifyListeners {
      it.updateBindingsInterpretersSettings(newBindingInterpreters)
    }
  }

  private fun notifyListeners(op: (ZeppelinConnectionListener) -> Unit) = listeners.forEach {
    try {
      op(it)
    }
    catch (t: Throwable) {
      rethrowControlFlowException(t)
      logger.error(t)
    }
  }


  companion object {
    private val logger = Logger.getInstance(this::class.java)
  }
}