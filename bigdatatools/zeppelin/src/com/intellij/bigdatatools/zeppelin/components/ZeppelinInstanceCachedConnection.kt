package com.intellij.bigdatatools.zeppelin.components

import com.google.gson.JsonObject
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
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.bigdatatools.coreUi.util.executeOnPooledThread

class ZeppelinInstanceCachedConnection(
  val config: ZeppelinConnectionData
) : Disposable, ZeppelinConnectionProvider {
  private val connection: ZeppelinConnection = ZeppelinConnection(config)
  private var listeners: List<ZeppelinConnectionListener> = listOf()

  val api get() = connection.api

  val tunnelUri: String?
    get() = connection.tunnelUri

  var isInterpreterSettingsAvailable = false
    private set
  var isRepositoriesAvailable = false
    private set
  var cachedNotesInfo: List<NotebookInfo>? = null
    private set
  var interpreterSettings: List<InterpreterSettings> = emptyList()
    private set
  var availableInterpreters: List<InterpreterSettings> = emptyList()
    private set
  var availableInterpretersException: Throwable? = null
    private set

  var repositories: List<Repository> = emptyList()
    private set
  var repositoriesException: Throwable? = null
    private set


  private val connectionListener by lazy {
    object : ZeppelinConnectionListener {
      override fun onConnected() {
        updateRepositoriesAsync()
        refreshInterpretersSettingsAsync()
        refreshAvailableInterpretersTemplatesAsync()
        updateNotesInfoAsync()

        notifyListeners {
          it.onConnected()
        }
      }

      override fun updateNotebookList(notebooks: List<NotebookInfo>) {
        val preparedNotes = notebooks.map { NotebookInfo(id = it.id, name = it.name.replace("//", "/")) }
        cachedNotesInfo = preparedNotes

        notifyListeners {
          it.updateNotebookList(preparedNotes)
        }
      }

      override fun updateInterpreterSettings(interpreterSettings: List<InterpreterSettings>) {
        if (this@ZeppelinInstanceCachedConnection.interpreterSettings == interpreterSettings)
          return
        this@ZeppelinInstanceCachedConnection.interpreterSettings = interpreterSettings

        notifyListeners {
          it.updateInterpreterSettings(interpreterSettings)
        }
      }

      override fun onDisconnected(statusCode: Int?, reason: String?) = notifyListeners {
        it.onDisconnected(statusCode, reason)
      }

      override fun updateRepositories(repositories: List<Repository>, requestException: Throwable?) = notifyListeners {
        it.updateRepositories(repositories, requestException)
      }

      override fun updateAngularObject(newAngularObject: AngularUpdateResponse) = notifyListeners {
        it.updateAngularObject(newAngularObject)
      }

      override fun removeAngularObject(angularObject: AngularRemoveResponse) = notifyListeners {
        it.removeAngularObject(angularObject)
      }

      override fun onServerError(info: String) = notifyListeners {
        it.onServerError(info)
      }

      override fun onConnectionError(throwable: Throwable) = notifyListeners {
        it.onConnectionError(throwable)
      }

      override fun onZeppelinInfoChange(newInfo: ZeppelinInfo?) = notifyListeners {
        it.onZeppelinInfoChange(newInfo)
      }

      override fun updateNotebook(notebook: ZeppelinNotebook) = notifyListeners {
        it.updateNotebook(notebook)
      }

      override fun addCell(jsonCell: JsonObject, index: Int) = notifyListeners {
        it.addCell(jsonCell, index)
      }

      override fun removeCell(jsonCell: JsonObject) = notifyListeners {
        it.removeCell(jsonCell)
      }

      override fun updateCell(paragraphJson: JsonObject) = notifyListeners {
        it.updateCell(paragraphJson)
      }

      override fun pathParagraph(paragraphId: String, patch: String) = notifyListeners {
        it.pathParagraph(paragraphId, patch)
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

      override fun updateInterpreterBindings(bindings: List<Interpreter>) = notifyListeners {
        it.updateInterpreterBindings(bindings)
      }

      override fun updateDefaultInterpreter(defaultInterpreterSettings: InterpreterSettings) = notifyListeners {
        it.updateDefaultInterpreter(defaultInterpreterSettings)
      }

      override fun updateBindingsInterpretersSettings(bindingInterpreterSettings: List<InterpreterSettings>) = notifyListeners {
        it.updateBindingsInterpretersSettings(bindingInterpreterSettings)
      }

      override fun updateCollaborativeModeStatus(status: Boolean) = notifyListeners {
        it.updateCollaborativeModeStatus(status)
      }
    }
  }

  init {
    Disposer.register(this, connection)
    connection.addListener(connectionListener)
  }

  override fun dispose() {
    connection.removeListener(connectionListener)
  }

  override fun isConnected(): Boolean = connection.isConnected()

  val zeppelinInfo
    get() = connection.zeppelinInfo

  fun refreshAvailableInterpretersTemplatesAsync() = executeOnPooledThread {
    val newSettings = try {
      api.getAvailableInterpretersSync() ?: error("No Zeppelin info")
    }
    catch (t: Throwable) {
      val prevException = availableInterpretersException
      availableInterpretersException = t
      availableInterpreters = emptyList()
      if (prevException == null) {
        notifyListeners {
          it.updateAvailableInterpreters(emptyList(), t)
        }
      }
      return@executeOnPooledThread
    }

    val prevException = availableInterpretersException
    availableInterpretersException = null
    val oldSettings = availableInterpreters
    if (prevException == null && newSettings == oldSettings)
      return@executeOnPooledThread

    availableInterpreters = newSettings
    notifyListeners {
      it.updateAvailableInterpreters(newSettings, null)
    }
  }

  fun refreshInterpretersSettingsAsync() = executeOnPooledThread {
    val api = connection.apiOrNull ?: return@executeOnPooledThread
    val newSettings = try {
      api.getInterpreterSettingsSync() ?: error("No Zeppelin info")
    }
    catch (t: Throwable) {
      //Settings can be not available by Rest (if no right) but always available by WS
      //However, WS sometimes sent wrong result (interpreter removed but in WS it is ok
      //So we make this hack
      api.getInterpreterSettingsAsync()

      logger.info("Cannot request interpreter settings by REST", t)
      return@executeOnPooledThread
    }

    isInterpreterSettingsAvailable = true

    val oldSettings = interpreterSettings
    if (newSettings == oldSettings)
      return@executeOnPooledThread

    interpreterSettings = newSettings
    notifyListeners {
      it.updateInterpreterSettings(newSettings)
    }
  }

  fun updateRepositoriesAsync() = executeOnPooledThread {
    val newRepositories = try {
      connection.api.getRepositories()
    }
    catch (t: Throwable) {
      logger.info("Cannot request Repositories by REST", t)
      isRepositoriesAvailable = false
      repositories = emptyList()
      val prevException = repositoriesException
      repositoriesException = t
      if (prevException == null)
        notifyListeners {
          it.updateRepositories(emptyList(), t)
        }
      return@executeOnPooledThread
    }
    val prevException = repositoriesException
    repositoriesException = null
    isRepositoriesAvailable = true

    val oldRepos = repositories
    if (prevException == null && newRepositories == oldRepos)
      return@executeOnPooledThread

    repositories = newRepositories
    notifyListeners {
      it.updateRepositories(newRepositories, null)
    }
  }

  fun refreshConnectionSync(force: Boolean, project: Project?) =
    connection.refreshConnectionSync(force, project)

  fun disconnect(reason: String) = connection.disconnect(reason = reason)

  fun updateNotesInfoAsync() = try {
    api.getNotesInfoAsync()
  }
  catch (t: Throwable) {
    logger.warn(t)
  }

  fun getConnectionStatus() = connection.getConnectionStatus()

  private fun notifyListeners(op: (ZeppelinConnectionListener) -> Unit) = listeners.forEach {
    try {
      op(it)
    }
    catch (t: Throwable) {
      logger.warn(t)
    }
  }

  override fun addListener(listener: ZeppelinConnectionListener) {
    listeners = listeners + listener
  }

  override fun removeListener(listener: ZeppelinConnectionListener) {
    listeners = listeners - listener
  }

  companion object {
    private val logger = Logger.getInstance(this::class.java)
  }
}