package com.intellij.bigdatatools.zeppelin.api.remote

import com.intellij.bigdatatools.zeppelin.api.remote.websocket.CreateParagraphData
import com.intellij.bigdatatools.zeppelin.api.remote.websocket.ParagraphData
import com.intellij.bigdatatools.zeppelin.api.remote.websocket.WebSocketClient
import com.intellij.bigdatatools.zeppelin.api.remote.websocket.WsRequestMessage
import com.intellij.bigdatatools.zeppelin.models.connection.WsMessages
import com.intellij.bigdatatools.zeppelin.models.connection.ZeppelinCredentials
import com.intellij.bigdatatools.zeppelin.models.notebook.ZeppelinCell
import com.intellij.bigdatatools.zeppelin.utils.JsonParser
import com.intellij.openapi.diagnostic.Logger

/**
 * The service for sending message to the Zeppelin server by WebSockets
 *
 * @param webSocketClient - web socket client
 */
class ZeppelinWebSocketAPI(private val webSocketClient: WebSocketClient) {
  /**
   * Close the connection
   */
  fun close(statusCode: Int?, reason: String?) = webSocketClient.close(statusCode, reason)

  /**
   * Connect to the application
   */
  fun connect() = webSocketClient.connect()

  /**
   * Send msg to get list of interpreters that are available for the notebook
   *
   * @param noteId      - an id of the notebook
   * @param credentials - an user credentials
   */
  fun getInterpreterBindings(noteId: String, credentials: ZeppelinCredentials) {
    val data = mapOf("noteId" to noteId)
    val opRequest = WsMessages.GET_INTERPRETER_BINDINGS.toString()
    val requestMessage = WsRequestMessage.create(opRequest, data, credentials)
    webSocketClient.sendMessage(requestMessage)
  }

  /**
   * Load a model of the notebook
   *
   * @param noteId      - the id of the notebook
   * @param credentials - the credentials of the user
   * @return the model of the notebook
   */
  fun getNote(noteId: String, credentials: ZeppelinCredentials) {
    val data = mapOf("id" to noteId)
    val opRequest = WsMessages.GET_NOTE.toString()
    val requestMessage = WsRequestMessage.create(opRequest, data, credentials)
    webSocketClient.sendMessage(requestMessage)
  }

  fun createNote(name: String, defaultInterpreterGroup: String, credentials: ZeppelinCredentials) {
    val data = mapOf("name" to name, "defaultInterpreterGroup" to defaultInterpreterGroup)
    val opRequest = WsMessages.NEW_NOTE.toString()
    val requestMessage = WsRequestMessage.create(opRequest, data, credentials)
    webSocketClient.sendMessage(requestMessage)
  }

  /**
   * Add cell to the notebook
   *
   * @param index      - an index of new cell
   * @param cell - a model of a cell
   * @param credentials - the credentials of the user
   */
  fun createParagraph(index: Int, cell: ZeppelinCell, credentials: ZeppelinCredentials) {
    val data = CreateParagraphData.create(index = index,
                                          title = cell.title,
                                          paragraph = cell.text,
                                          params = cell.settings.params,
                                          config = cell.config)

    val opRequest = WsMessages.COPY_PARAGRAPH.toString()
    val requestMessage = WsRequestMessage.create(
      opRequest, data, credentials)
    webSocketClient.sendMessage(requestMessage)
  }

  /**
   * Remove a paragraph in the notebook
   *
   * @param id      - an id of the paragraph
   * @param credentials - the credentials of the user
   */
  fun removeParagraph(id: String, credentials: ZeppelinCredentials) {
    val data = mapOf("id" to id)
    val opRequest = WsMessages.PARAGRAPH_REMOVE.toString()
    val requestMessage = WsRequestMessage.create(
      opRequest, data, credentials)
    webSocketClient.sendMessage(requestMessage)
  }

  /**
   * Run the cell in the Zeppelin application
   *
   * @param cell   - the cell, which must be run
   * @param credentials - the credentials of the user
   */
  fun runParagraph(cell: ZeppelinCell, credentials: ZeppelinCredentials) {
    val data = ParagraphData.create(id = cell.id,
                                    paragraph = cell.text,
                                    title = cell.title,
                                    params = cell.settings.params,
                                    config = cell.config)

    val opRequest = WsMessages.RUN_PARAGRAPH.toString()
    val requestMessage = WsRequestMessage.create(opRequest, data, credentials)
    webSocketClient.sendMessage(requestMessage)
  }

  fun clearAllOutputs(notebookId: String, credentials: ZeppelinCredentials) {
    val opRequest = WsMessages.PARAGRAPH_CLEAR_ALL_OUTPUT.toString()
    val requestMessage = WsRequestMessage.create(
      opRequest, mapOf("id" to notebookId), credentials)
    webSocketClient.sendMessage(requestMessage)
  }

  /**
   * Commit a cell changes to Zeppelin
   *
   * @param cell - a model of a cell to commit
   * @param credentials - the credentials of the user
   */
  fun commitParagraph(cell: ZeppelinCell, credentials: ZeppelinCredentials) {
    val data = ParagraphData.create(cell.id, cell.text,
                                    cell.title, config = cell.config,
                                    params = cell.settings.params)

    val opRequest = WsMessages.COMMIT_PARAGRAPH.toString()
    val requestMessage = WsRequestMessage.create(opRequest, data, credentials)
    webSocketClient.sendMessage(requestMessage)
  }

  fun patchParagraph(noteId: String, cellId: String, patch: String, credentials: ZeppelinCredentials) {
    val data = mapOf(
      "noteId" to noteId,
      "id" to cellId,
      "patch" to patch)

    val opRequest = WsMessages.PATCH_PARAGRAPH.toString()
    val requestMessage = WsRequestMessage.create(opRequest, data, credentials)
    webSocketClient.sendMessage(requestMessage)
  }

  /**
   * Ping a Zeppelin server to keep websocket connection alive
   *
   * @param credentials - the credentials of the user
   */
  fun pingZeppelin(credentials: ZeppelinCredentials) {
    val opRequest = WsMessages.PING.toString()
    val requestMessage = WsRequestMessage.create(
      opRequest, mapOf<String, Any>(), credentials)
    webSocketClient.sendMessage(requestMessage)
  }

  /**
   * Get configurations for the notebook
   */
  @Suppress("unused")
  fun getListConfigurations(credentials: ZeppelinCredentials) {
    val opRequest = WsMessages.LIST_CONFIGURATIONS.toString()
    val requestMessage = WsRequestMessage.create(
      opRequest, mapOf<String, Any>(), credentials)
    webSocketClient.sendMessage(requestMessage)
  }

  fun getInterpreterSettings(credentials: ZeppelinCredentials) {
    val opRequest = WsMessages.GET_INTERPRETER_SETTINGS.toString()
    val requestMessage = WsRequestMessage.create(
      opRequest, mapOf<String, Any>(), credentials)
    webSocketClient.sendMessage(requestMessage)
  }

  /**
   * Clear an output of the cell
   *
   * @param cell   - a model of the cell
   * @param credentials - the credentials of the user
   */
  fun clearParagraphOutput(cell: ZeppelinCell, credentials: ZeppelinCredentials) {
    val opRequest = WsMessages.PARAGRAPH_CLEAR_OUTPUT.toString()
    val requestMessage = WsRequestMessage.create(
      opRequest, mapOf("id" to cell.id), credentials)
    webSocketClient.sendMessage(requestMessage)
  }

  /**
   * Save new interpreters bindings settings for the notebook
   *
   * @param noteId                  - id of the notebook
   * @param interpreterIds - new interpreters bindings
   * @param credentials             - an user credentials
   */
  fun saveListOfBindingInterpreters(noteId: String,
                                    interpreterIds: List<String>,
                                    credentials: ZeppelinCredentials) {
    val data = mapOf("noteId" to noteId, "selectedSettingIds" to interpreterIds)
    val opRequest = WsMessages.SAVE_INTERPRETER_BINDINGS.toString()
    val requestMessage = WsRequestMessage.create(
      opRequest, data, credentials)

    logger.trace("Start request 'Save list of binding interpreters'. Data : $data, credentials: $credentials.")
    webSocketClient.sendMessage(requestMessage)
  }

  /**
   * Get list of notebooks on server
   *
   * @param credentials             - an user credentials
   */
  fun getNotebookList(credentials: ZeppelinCredentials) {
    val opRequest = WsMessages.LIST_NOTES.toString()
    val requestMessage = WsRequestMessage.create(
      opRequest, mapOf<String, Any>(), credentials)

    webSocketClient.sendMessage(requestMessage)
    logger.trace("Send request 'Get notebook list'. Credentials: $credentials.")
  }

  /**
   * Fully note remove
   *
   * @param noteId - the id of a notebook
   * @param credentials - an user credentials
   */
  fun deleteNote(noteId: String, credentials: ZeppelinCredentials) {
    val opRequest = WsMessages.DEL_NOTE.toString()
    val requestMessage = WsRequestMessage.create(
      opRequest, mapOf("id" to noteId), credentials)
    webSocketClient.sendMessage(requestMessage)
  }

  /**
   * Delete a folder
   *
   * @param folderId - the id of a notebook
   * @param credentials - an user credentials
   */
  fun deleteFolder(folderId: String, credentials: ZeppelinCredentials) {
    val opRequest = WsMessages.REMOVE_FOLDER.toString()
    val requestMessage = WsRequestMessage.create(
      opRequest, mapOf("id" to folderId), credentials)
    webSocketClient.sendMessage(requestMessage)
  }

  /**
   * Move folder to the trash
   *
   * @param folderId - the id of a folder
   * @param credentials - an user credentials
   */
  fun folderToTrash(folderId: String, credentials: ZeppelinCredentials) {
    val opRequest = WsMessages.MOVE_FOLDER_TO_TRASH.toString()
    val requestMessage = WsRequestMessage.create(opRequest, mapOf("id" to folderId), credentials)
    webSocketClient.sendMessage(requestMessage)
  }

  /**
   * Move notebook to the trash
   *
   * @param noteId - the id of a notebook
   * @param credentials - an user credentials
   */
  fun noteToTrash(noteId: String, credentials: ZeppelinCredentials) {
    val opRequest = WsMessages.MOVE_NOTE_TO_TRASH.toString()
    val requestMessage = WsRequestMessage.create(opRequest, mapOf("id" to noteId), credentials)
    webSocketClient.sendMessage(requestMessage)
  }

  /**
   * Empty trash
   *
   * @param credentials - an user credentials
   */
  fun emptyTrash(credentials: ZeppelinCredentials) {
    val opRequest = WsMessages.EMPTY_TRASH.toString()
    val requestMessage = WsRequestMessage.create(opRequest, mapOf<String, String>(), credentials)
    webSocketClient.sendMessage(requestMessage)
  }

  /**
   * Empty trash
   *
   * @param credentials - an user credentials
   */
  fun restoreAll(credentials: ZeppelinCredentials) {
    val opRequest = WsMessages.RESTORE_ALL.toString()
    val requestMessage = WsRequestMessage.create(opRequest, mapOf<String, String>(), credentials)
    webSocketClient.sendMessage(requestMessage)
  }

  fun restoreNote(noteId: String, credentials: ZeppelinCredentials) {
    val opRequest = WsMessages.RESTORE_NOTE.toString()
    val requestMessage = WsRequestMessage.create(
      opRequest, mapOf("id" to noteId), credentials)
    webSocketClient.sendMessage(requestMessage)
  }

  fun restoreFolder(folderId: String, credentials: ZeppelinCredentials) {
    val opRequest = WsMessages.RESTORE_FOLDER.toString()
    val requestMessage = WsRequestMessage.create(
      opRequest, mapOf("id" to folderId), credentials)
    webSocketClient.sendMessage(requestMessage)
  }

  /**
   * Rename a notebook
   *
   * @param noteId - an id of a notebook
   * @param noteName - a new name of a notebook
   * @param relative - a relative path to notebook or not
   * @param credentials - an user credentials
   */
  fun renameNote(noteId: String, noteName: String, relative: Boolean, credentials: ZeppelinCredentials) {
    val opRequest = WsMessages.NOTE_RENAME.toString()
    val requestMessage = WsRequestMessage.create(
      opRequest, mapOf("id" to noteId, "name" to noteName, "relative" to relative), credentials)
    webSocketClient.sendMessage(requestMessage)
  }

  /**
   * Rename a folder
   *
   * @param oldFolder - an old folder path
   * @param newFolder - a new folder path
   * @param credentials - an user credentials
   */
  fun renameFolder(oldFolder: String, newFolder: String, credentials: ZeppelinCredentials) {
    val opRequest = WsMessages.FOLDER_RENAME.toString()
    val requestMessage = WsRequestMessage.create(
      opRequest, mapOf("id" to oldFolder, "name" to newFolder), credentials)
    webSocketClient.sendMessage(requestMessage)
  }

  fun runAllParagraphs(noteId: String, cells: List<ZeppelinCell>, credentials: ZeppelinCredentials) {
    val opRequest = WsMessages.RUN_ALL_PARAGRAPHS.toString()

    val sentParagraphs = cells.map { paragraph ->
      ParagraphData.create(paragraph.id, paragraph.text,
                           paragraph.title,
                           config = paragraph.config,
                           params = paragraph.settings.params)
    }
    val requestMessage = WsRequestMessage.create(
      opRequest, mapOf("noteId" to noteId, "paragraphs" to JsonParser.toJson(sentParagraphs)), credentials)
    webSocketClient.sendMessage(requestMessage)
  }

  fun cancelParagraph(paragraphId: String, credentials: ZeppelinCredentials) {
    val opRequest = WsMessages.CANCEL_PARAGRAPH.toString()
    val requestMessage = WsRequestMessage.create(opRequest, mapOf("id" to paragraphId), credentials)
    webSocketClient.sendMessage(requestMessage)
  }

  fun moveParagraph(paragraphId: String, toIndex: Int, credentials: ZeppelinCredentials) {
    val opRequest = WsMessages.MOVE_PARAGRAPH.toString()
    val requestMessage = WsRequestMessage.create(opRequest, mapOf("id" to paragraphId, "index" to toIndex), credentials)
    webSocketClient.sendMessage(requestMessage)
  }

  companion object {
    private val logger = Logger.getInstance(this::class.java)
  }
}