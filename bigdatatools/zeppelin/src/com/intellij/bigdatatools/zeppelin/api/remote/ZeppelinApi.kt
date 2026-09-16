package com.intellij.bigdatatools.zeppelin.api.remote

import com.google.gson.Gson
import com.intellij.bigdatatools.coreUi.connection.exception.BdtConnectionException
import com.intellij.bigdatatools.coreUi.connection.exception.BdtUnexpectedConnectionException
import com.intellij.bigdatatools.notebooks.core.api.nbformat.CellStatus
import com.intellij.bigdatatools.zeppelin.api.remote.rest.ZeppelinRestApi
import com.intellij.bigdatatools.zeppelin.components.connections.parser.ZeppelinVersionAdapter
import com.intellij.bigdatatools.zeppelin.constants.ZeppelinConstants
import com.intellij.bigdatatools.zeppelin.dependency.collector.builtin.Repository
import com.intellij.bigdatatools.zeppelin.models.connection.ZeppelinCredentials
import com.intellij.bigdatatools.zeppelin.models.connection.ZeppelinInfo
import com.intellij.bigdatatools.zeppelin.models.interpreter.Interpreter
import com.intellij.bigdatatools.zeppelin.models.interpreter.InterpreterSettings
import com.intellij.bigdatatools.zeppelin.models.notebook.NotebookInfo
import com.intellij.bigdatatools.zeppelin.models.notebook.ZeppelinCell
import com.intellij.bigdatatools.zeppelin.models.notebook.ZeppelinNotebook
import com.intellij.bigdatatools.zeppelin.models.notebook.ZeppelinNotebookBuilder
import com.intellij.bigdatatools.zeppelin.settings.ZeppelinConnectionData
import com.intellij.bigdatatools.zeppelin.utils.ZepMessagesBundle
import com.intellij.credentialStore.isEmpty
import com.intellij.openapi.diagnostic.Logger

/**
 * Class, which implements the logic of communication with Zeppelin API
 *
 * @param zeppelinWebSocketAPI — service for communication with Zeppelin by WebSockets
 * @param zeppelinRestApi — service for communication with Zeppelin by REST API
 * @param zeppelinConnectionData — a Zeppelin config
 */
@Suppress("unused")
open class ZeppelinApi(private val zeppelinWebSocketAPI: ZeppelinWebSocketAPI,
                       private val zeppelinRestApi: ZeppelinRestApi,
                       private val zeppelinConnectionData: ZeppelinConnectionData) {
  var innerZeppelinInfo: ZeppelinInfo? = null
    protected set
  protected var credentials = ZeppelinCredentials()

  fun connect(): ZeppelinInfo {
    try {
      innerZeppelinInfo = try {
        checkConnectionAndGetInfo()
      }
      catch (e: ZeppelinMissingCredentialsException) {
        null
      }

      zeppelinConnectionData.zeppelinUser.takeIf { !zeppelinConnectionData.anonymous && !it.isEmpty() }?.let { zeppelinUser ->
        credentials = zeppelinRestApi.login(zeppelinUser)
      }
      if (innerZeppelinInfo == null) {
        innerZeppelinInfo = checkConnectionAndGetInfo()
      }
      getAllNotebooksInfoSync()
    }
    catch (e: ZeppelinNoRightsException) {
      throw ZeppelinBadCredentialsException(e)
    }
    catch (e: BdtConnectionException) {
      throw e
    }
    catch (e: Throwable) {
      throw BdtUnexpectedConnectionException(e)
    }

    try {
      zeppelinWebSocketAPI.connect()
    }
    catch (t: Throwable) {
      throw ZeppelinWebSocketConnectionException(zeppelinConnectionData.getFullHttpUrl(), t)
    }

    return innerZeppelinInfo ?: error("No inner Zeppelin info")
  }

  /**
   * Close the Zeppelin connection if it is opened
   */
  fun close(statusCode: Int?, reason: String?) {
    zeppelinWebSocketAPI.close(statusCode, reason)
  }

  /**
   * Request interpreter settings for Notebook by WS if available
   */
  fun getInterpreterSettingsAsync() = zeppelinWebSocketAPI.getInterpreterSettings(credentials)

  fun getTempNotebook() = getOrCreateNotebook(zeppelinConnectionData.defaultNotebookName)

  private fun getNotebookSync(notebookId: String): ZeppelinNotebook {
    assert(notebookId.isNotEmpty()) { "An id of notebook is empty, cannot get the note from the server" }
    val jsonNote = zeppelinRestApi.getNotebook(notebookId)
    val jsonString = Gson().toJson(jsonNote)
    return ZeppelinNotebookBuilder.createFromJson(jsonString)
  }

  /**
   * Create a notebook
   *
   * @param notebookName - a name of a notebook
   * @return a model of the created notebook
   */
  private fun createNotebookSync(notebookName: String, cells: List<ZeppelinCell> = emptyList()): ZeppelinNotebook {
    val notebook = ZeppelinNotebookBuilder.createNotebook(name = notebookName, cells = cells)
    val id = zeppelinRestApi.createNotebook(notebook)
    return getNotebookSync(id)
  }

  fun createNoteAsync(notebookName: String) {
    zeppelinWebSocketAPI.createNote(notebookName, "spark", credentials)
  }

  /**
   * Delete a folder asynchronous
   *
   * @param id - a folder path
   */
  fun moveFolderToTrash(id: String) {
    zeppelinWebSocketAPI.folderToTrash(id.removeSuffix("/").removePrefix("/"), credentials)
  }

  /**
   * Get interpreter bindings for notebook
   *
   * @param notebookId - an id of the notebook
   */
  fun getInterpreterBindings(notebookId: String) = zeppelinWebSocketAPI.getInterpreterBindings(notebookId, credentials)

  /**
   * Delete a notebook asynchronous
   *
   * @param notebookId - a notebook model
   */
  fun moveNoteToTrash(notebookId: String) = zeppelinWebSocketAPI.noteToTrash(notebookId, credentials)

  /**
   * Permanently Delete notebook asynchronous
   */
  fun deleteNotebookAsync(notebookId: String) = zeppelinWebSocketAPI.deleteNote(notebookId, credentials)

  /**
   * Permanently delete folder asynchronous
   *
   * @param id - a folder path
   */
  fun deleteFolderAsync(id: String) = zeppelinWebSocketAPI.deleteFolder(id.removeSuffix("/").removePrefix("/"), credentials)

  fun deleteNoteSync(notebookId: String) = zeppelinRestApi.deleteNotebook(notebookId)

  fun deleteNoteSync(notebook: NotebookInfo) = zeppelinRestApi.deleteNotebook(notebook.id)

  /**
   * Get from Zeppelin the notebook by the name. If the notebook does not exist the notebook will be created
   *
   * @param notebookName - the name of the notebook
   * @return notebook
   */
  private fun getOrCreateNotebook(notebookName: String): ZeppelinNotebook? {
    val zeppelinInfo = innerZeppelinInfo ?: return null

    val notebooks = zeppelinRestApi.getNotebooks(zeppelinInfo)
    val noteId = notebooks.firstOrNull { it.name == notebookName }?.id
                 ?: createNotebookSync(notebookName).id
    return getNotebookSync(noteId)
  }

  fun importNotebookSync(note: String) = zeppelinRestApi.importNotebook(note)

  /**
   * Export the notebook from the server
   *
   * @param noteId - a note info
   *
   * @return notebook json
   */
  fun exportNotebook(noteId: String): String = zeppelinRestApi.exportNotebook(noteId)


  fun pingZeppelin() = zeppelinWebSocketAPI.pingZeppelin(credentials)

  /**
   * Send request to get a list of available notebooks
   */
  fun getNotesInfoAsync() = zeppelinWebSocketAPI.getNotebookList(credentials)

  fun emptyTrash() = zeppelinWebSocketAPI.emptyTrash(credentials)

  fun restoreAll() = zeppelinWebSocketAPI.restoreAll(credentials)

  fun restoreNote(id: String) = zeppelinWebSocketAPI.restoreNote(id, credentials)

  fun restoreFolder(id: String) = zeppelinWebSocketAPI.restoreFolder(id.removeSuffix("/").removePrefix("/"), credentials)

  @Suppress("unused")
  fun renameNotebookSync(noteId: String, newNotebookName: String) =
    zeppelinRestApi.renameNote(noteId, newNotebookName)

  @Suppress("unused")
  fun renameNotebookAsync(noteId: String, newNotebookName: String) =
    zeppelinWebSocketAPI.renameNote(noteId, newNotebookName, false, credentials)

  @Suppress("unused")
  fun renameFolder(oldDirFullName: String, newDirFullName: String) =
    zeppelinWebSocketAPI.renameFolder(oldDirFullName.removeSuffix("/").removePrefix("/"),
                                      newDirFullName.removeSuffix("/").removePrefix("/"),
                                      credentials)

  fun cloneNotebookSync(sourceNoteId: String, toPath: String): NotebookInfo {
    val id = zeppelinRestApi.cloneNote(sourceNoteId, toPath)
    return NotebookInfo(id = id, name = toPath)
  }

  fun getRepositories(): List<Repository> = innerZeppelinInfo?.let { zeppelinRestApi.getRepositories(it) }
                                            ?: error("Cannot receive Zeppelin Info to parse repos")

  fun addRepository(repository: Repository) = zeppelinRestApi.addRepository(repository)

  fun removeRepository(repository: Repository) = zeppelinRestApi.removeRepository(repository)

  private fun checkConnectionAndGetInfo(): ZeppelinInfo {
    val serverInfo: ZeppelinInfo = try {
      serverInfo()
    }
    catch (e: BdtConnectionException) {
      throw e
    }
    catch (e: Throwable) {
      throw BdtUnexpectedConnectionException(e)
    }

    if (!ZeppelinVersionAdapter.isVersionSupport(serverInfo)) {
      throw ZeppelinVersionIsNotSupportedException(serverInfo.version, zeppelinConnectionData.getFullHttpUrl())
    }

    return serverInfo
  }

  /**
   * Get notebook by WS from the server
   * IMPORTANT NOTICE: after performing of this operation, connection starts listen changes of this Notebook

   * @param notebookId - a note id
   */
  fun getNotebookAsync(notebookId: String) = zeppelinWebSocketAPI.getNote(notebookId, credentials)

  /**
   * Create a cell in Zeppelin
   *
   * @param index - an index of insert cell
   * @param cell - a saved cell
   */
  fun createParagraph(index: Int, cell: ZeppelinCell) {
    zeppelinWebSocketAPI.createParagraph(index, cell, credentials)
  }

  /**
   * Remove a paragraph in Zeppelin
   *
   * @param id - an id of a paragraph
   */
  fun removeParagraph(id: String) {
    zeppelinWebSocketAPI.removeParagraph(id, credentials)
  }

  /**
   * Run the code on the zeppelin application
   *
   * @param cell - a model of a cell
   */
  fun runCell(cell: ZeppelinCell) = zeppelinWebSocketAPI.runParagraph(cell, credentials)

  /**
   * Clear an output of the paragraph
   *
   * @param - a model of the paragraph
   */
  fun clearParagraphOutput(paragraph: ZeppelinCell) {
    zeppelinWebSocketAPI.clearParagraphOutput(paragraph, credentials)
  }

  /**
   * Run all cells of the notebook
   */
  fun runAll(notebookId: String, cells: List<ZeppelinCell>) {
    zeppelinWebSocketAPI.runAllParagraphs(notebookId, cells, credentials)
  }

  /**
   * Stop executing paragraphs in a notebook
   */
  fun stopAllParagraphs(notebookId: String) = zeppelinRestApi.stopAllParagraphs(notebookId)

  /**
   * Stop executing or cancel pending paragraph in a notebook
   */
  fun stopParagraph(cellId: String) = zeppelinWebSocketAPI.cancelParagraph(cellId, credentials)

  fun moveParagraph(cellId: String, toIndex: Int) = zeppelinWebSocketAPI.moveParagraph(cellId, toIndex, credentials)

  /**
   * Clear output of all paragraphs
   */
  fun clearAllOutputs(notebookId: String) {
    zeppelinWebSocketAPI.clearAllOutputs(notebookId, credentials)
  }

  /**
   * Commit paragraph changes
   *
   * @param paragraph - a model of paragraph
   */
  fun commitParagraph(paragraph: ZeppelinCell) {
    if (paragraph.id.isEmpty())
      logger.error("Commit paragraph with empty id!.\nParagraph:\n$paragraph")
    else
      zeppelinWebSocketAPI.commitParagraph(paragraph, credentials)
  }

  fun patchParagraph(noteId: String, cellId: String, patch: String) {
    if (cellId.isEmpty())
      logger.error("Path cell with empty id. note: $noteId, patch: $patch")
    else
      zeppelinWebSocketAPI.patchParagraph(noteId, cellId, patch, credentials)
  }

  /**
   * Restart an interpreter
   *
   * @param interpreterId - an interpreter
   * @param notebookId - a notebook id
   */
  fun restartInterpreter(interpreterId: String, notebookId: String?) {
    zeppelinRestApi.restartInterpreter(interpreterId, notebookId)
  }

  /**
   * Full text search in all notes
   * @param query - a search query
   *
   * @return list with found results
   */
  fun fullTextSearch(query: String) = zeppelinRestApi.fullTextSearch(query)

  /**
   * Save an interpreter bindings for the notebook
   * @param bindings - a model of new bindings
   * @param notebookId
   */
  fun saveInterpreterBindings(bindings: List<Interpreter>, notebookId: String) {
    assert(notebookId.isNotBlank())
    val bindingsIds = bindings.asSequence().filter { it.selected }.map { it.id }.toList()
    zeppelinWebSocketAPI.saveListOfBindingInterpreters(notebookId, bindingsIds, credentials)
  }

  fun getInterpreterSettingsSync() = innerZeppelinInfo?.let {
    zeppelinRestApi.getInterpreterSettings(ZeppelinVersionAdapter.getInstance(it))
  }

  fun getAvailableInterpretersSync() = innerZeppelinInfo?.let {
    zeppelinRestApi.getInterpreters(ZeppelinVersionAdapter.getInstance(it))
  }


  /**
   * Update an interpreter bindings for the notebook
   * @param interpreterSettings - an interpreter settings
   */
  fun updateInterpreterSetting(interpreterSettings: InterpreterSettings) = innerZeppelinInfo?.let {
    zeppelinRestApi.updateInterpreterSetting(interpreterSettings, ZeppelinVersionAdapter.getInstance(it))
  }

  /**
   * Update an interpreter bindings for the notebook
   * @param interpreterSettings - an interpreter settings
   */
  fun addInterpreterSettings(interpreterSettings: InterpreterSettings) = innerZeppelinInfo?.let {
    zeppelinRestApi.addInterpreterSetting(interpreterSettings, ZeppelinVersionAdapter.getInstance(it))
  }

  /**
   * Update an interpreter bindings for the notebook
   * @param interpreterSettings - an interpreter settings
   */
  fun removeInterpreterSettings(interpreterSettings: InterpreterSettings) {
    innerZeppelinInfo?.let {
      zeppelinRestApi.removeInterpreterSetting(interpreterSettings)
    }
  }


  /**
   * Clear temporary notebook
   */
  fun clearTempNotebook() {
    val tempNote = try {
      getTempNotebook() ?: return
    }
    catch (t: Throwable) {
      return
    }

    val paragraphs = tempNote.cells
    val unusedParagraphs = paragraphs.filter {
      it.status in setOf(CellStatus.ERROR, CellStatus.ABORT, CellStatus.FINISHED) ||
      it.text == "" && it.status == CellStatus.READY
    }
    if (unusedParagraphs.size < 10) return
    unusedParagraphs.forEach {
      removeParagraph(it.id)
    }
  }

  /**
   * Get Zeppelin Info
   */
  fun serverInfo(): ZeppelinInfo {
    val zeppelinInfo = zeppelinRestApi.getZeppelinInfo()
    if (zeppelinConnectionData.zeppelinVersion.isNotEmpty()) {
      return ZeppelinInfo(zeppelinConnectionData.zeppelinVersion)
    }
    else {
      return zeppelinInfo ?: throw BdtConnectionException(
        ZepMessagesBundle.message("cannot.detect.zeppelin.version.please.specify.in.connection.settings"))
    }
  }

  /**
   * Get a list of the available notebooks zeppelinInfo
   *
   * @return the list of notebooks
   */
  private fun getAllNotebooksInfoSync(): List<NotebookInfo> {
    val zeppelinInfo = innerZeppelinInfo ?: return emptyList()
    val notebooksWithoutParagraphs = zeppelinRestApi.getNotebooks(zeppelinInfo)
    return notebooksWithoutParagraphs.filter {
      !it.name.contains(ZeppelinConstants.TRASH_NAME)
    }
  }

  companion object {
    private val logger = Logger.getInstance(this::class.java)
  }
}