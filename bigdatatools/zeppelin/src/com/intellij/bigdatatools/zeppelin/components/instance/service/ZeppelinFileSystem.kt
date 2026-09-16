package com.intellij.bigdatatools.zeppelin.components.instance.service

import com.intellij.bigdatatools.notebooks.core.api.nbformat.NotebookSchema
import com.intellij.bigdatatools.zeppelin.components.ZeppelinInstanceCachedConnection
import com.intellij.bigdatatools.zeppelin.components.connections.ZeppelinConnectionListener
import com.intellij.bigdatatools.zeppelin.drivers.ZeppelinDriverManager
import com.intellij.bigdatatools.zeppelin.drivers.ZeppelinFileTypeViewer
import com.intellij.bigdatatools.zeppelin.drivers.fileinfo.ZeppelinFileInfo
import com.intellij.bigdatatools.zeppelin.models.notebook.NotebookInfo
import com.intellij.bigdatatools.zeppelin.models.notebook.ZeppelinFullTextSearchElement
import com.intellij.bigdatatools.zeppelin.models.notebook.ZeppelinNotebook
import com.intellij.bigdatatools.zeppelin.models.notebook.ZeppelinNotebookBuilder
import com.intellij.bigdatatools.zeppelin.rfs.path.ZeppelinRfsPath
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.jetbrains.bigdatatools.common.rfs.driver.ConnectedConnectionStatus
import com.jetbrains.bigdatatools.common.rfs.driver.ConnectingConnectionStatus
import com.jetbrains.bigdatatools.common.rfs.driver.RfsPath
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

class ZeppelinFileSystem(val project: Project?, private val connection: ZeppelinInstanceCachedConnection) : Disposable {
  private val shouldOpenNotes = mutableListOf<Pair<Project, ZeppelinRfsPath>>()

  private val config = connection.config
  internal val api get() = connection.api
  var notePaths: List<ZeppelinRfsPath>? = connection.cachedNotesInfo?.map { ZeppelinRfsPath.createFromNoteInfo(it) }
  private val isConnecting = AtomicBoolean(false)

  private val connectionListener = object : ZeppelinConnectionListener {
    override fun onConnected() {
      isConnecting.set(true)
      notePaths = connection.cachedNotesInfo?.map { ZeppelinRfsPath.createFromNoteInfo(it) }
      if (notePaths != null) {
        isConnecting.set(false)
      }
    }

    override fun onDisconnected(statusCode: Int?, reason: String?) {
      isConnecting.set(false)
      notePaths = emptyList()
    }

    override fun updateNotebookList(notebooks: List<NotebookInfo>) {
      isConnecting.set(false)
      val oldNotes = notePaths
      notePaths = notebooks.map { ZeppelinRfsPath.createFromNoteInfo(it) }

      if (notePaths == oldNotes) return

      openNewFiles(oldNotes ?: emptyList(), notePaths ?: emptyList())
    }
  }

  init {
    connection.addListener(connectionListener)
  }

  override fun dispose() {
    connection.removeListener(connectionListener)
  }

  fun getConnectionStatus() = when {
    !connection.isConnected() -> connection.getConnectionStatus()
    isConnecting.get() -> ConnectingConnectionStatus
    else -> ConnectedConnectionStatus
  }

  fun listStatus(origPath: RfsPath): List<ZeppelinRfsPath> {
    val children = notePaths
      ?.filter { it.startsWith(origPath) }
      ?.map { it.prefixPath(origPath.nameCount + 1) }
      ?.distinct()

    return children?.sortedWith(compareBy({ it.isInTrash }, { !it.isDirectory }, { it.name.lowercase(Locale.getDefault()) })) ?: emptyList()
  }

  fun getNotePathById(noteId: String) = notePaths?.firstOrNull { it.id == noteId }

  fun export(rfsPath: ZeppelinRfsPath): NoteWithPath? {
    if (rfsPath.isDirectory)
      error("Can export just notes")
    val exportNoteInfo = getNotesInPath(rfsPath)?.firstOrNull() ?: return null

    return NoteWithPath(exportNote(exportNoteInfo), exportNoteInfo)
  }

  fun findNoteByPath(path: String) = notePaths?.find { it.serverPath.removePrefix("/") == path.removePrefix("/") }

  fun getNoteInfoByPath(path: ZeppelinRfsPath) = when {
    !connection.isConnected() -> null
    path.isRoot -> path
    path.isFile -> notePaths?.find { it.serverPath == path.serverPath }
    notePaths?.any { it.startsWith(path) } == true -> path
    else -> null
  }

  private fun getNotesInPath(path: ZeppelinRfsPath) = if (path.isFile)
    listOf(path)
  else
    notePaths?.filter {
      it.startsWith(path)
    }

  fun noteExists(path: RfsPath) = notePaths?.any { it.elements == path.elements } == true

  fun addPromiseOpenFile(project: Project, path: ZeppelinRfsPath) = withCheckConnection {
    shouldOpenNotes.add(project to path)
  }

  fun createNote(newName: String) = withCheckConnection {
    api.createNoteAsync(notebookName = newName)
    refreshNotes()
  }

  fun deleteFromTrash(path: ZeppelinRfsPath) = when {
    !path.isInTrash -> false
    path.isDirectory -> deleteFolder(path)
    else -> deleteNotebook(path)
  }

  fun restoreFromTrash(path: ZeppelinRfsPath) = if (path.isDirectory)
    restoreFolder(path)
  else
    restoreNote(path)

  fun moveToTrash(path: ZeppelinRfsPath) {
    when {
      path.isInTrash -> return
      path.isDirectory -> moveFolderToTrash(path)
      else -> moveNoteToTrash(path)
    }
  }

  fun clearOutput(notebookInfo: ZeppelinRfsPath) = withCheckConnection {
    assert(notebookInfo.id.isNotEmpty())
    api.clearAllOutputs(notebookInfo.id)
  }

  fun emptyTrash() = withCheckConnection { api.emptyTrash() }
  fun restoreAll() = withCheckConnection { api.restoreAll() }

  fun fullTextSearch(query: String): List<ZeppelinFullTextSearchElement> {
    if (!connection.isConnected())
      return emptyList()
    val originalResult = if (query.isNotBlank())
      api.fullTextSearch(query)
    else
      emptyList()

    val nameSearchResults = findInNames(query).map {
      val fileName = it.stringRepresentation()
      ZeppelinFullTextSearchElement(it.id,
                                    name = fileName,
                                    fileName = fileName, query = query, connId = config.innerId)
    }
    return originalResult.map {
      val noteId = it.id.split("/").firstOrNull() ?: ""
      val fileName = getNotePathById(noteId)?.serverPath ?: ""
      it.copy(connId = config.innerId,
              id = noteId,
              name = fileName,
              fileName = fileName)
    } + nameSearchResults
  }

  private fun findInNames(query: String): List<ZeppelinRfsPath> {
    val noteUrlIndicator = "/notebook/"
    val realQuery = if (query.contains(noteUrlIndicator))
      query.split(noteUrlIndicator).last()
    else
      query
    return notePaths?.filter { it.canonicalPath.contains(realQuery) } ?: emptyList()
  }

  fun refreshNotes() = connection.api.getNotesInfoAsync()

  internal fun cloneNote(copyNote: ZeppelinRfsPath,
                         newName: ZeppelinRfsPath) = withCheckConnection {
    api.cloneNotebookSync(copyNote.id, newName.serverPath)
  }

  internal fun deleteSync(path: ZeppelinRfsPath) {
    val notesForRemove = getNotesInPath(path)
    notesForRemove?.forEach {
      api.deleteNoteSync(it.id)
    }
  }

  private fun deleteFolder(path: ZeppelinRfsPath) = withCheckConnection {
    api.deleteFolderAsync(path.serverPath)
  }

  private fun deleteNotebook(path: ZeppelinRfsPath) = withCheckConnection {
    api.deleteNotebookAsync(path.id)
  }

  fun importNote(note: ZeppelinNotebook): String {
    try {
      val jsonObject = note.json.deepCopy()
      val cells = jsonObject.get(NotebookSchema.cellFieldName).asJsonArray
      cells.forEach {
        it.asJsonObject.remove(NotebookSchema.cellId)
        it.asJsonObject.remove("runtimeInfos")
      }
      val json = jsonObject.toString()
      return connection.api.importNotebookSync(json)
    }
    catch (t: Throwable) {
      throw Exception("Cannot import ${note.path ?: note.clearName}", t)
    }
  }

  private fun exportNote(path: ZeppelinRfsPath): ZeppelinNotebook {
    val jsonString = exportNoteJson(path)
    return ZeppelinNotebookBuilder.createFromJson(jsonString)
  }

  fun exportNoteJson(path: ZeppelinRfsPath) = try {
    connection.api.exportNotebook(path.id)
  }
  catch (t: Throwable) {
    throw Exception("Cannot export ${path.canonicalPath}", t)
  }

  private fun restoreNote(path: ZeppelinRfsPath) = withCheckConnection { api.restoreNote(path.id) }
  private fun restoreFolder(path: ZeppelinRfsPath) = withCheckConnection { api.restoreFolder(path.serverPath) }

  private fun openNewFiles(oldNotes: List<ZeppelinRfsPath>, newNotes: List<ZeppelinRfsPath>) {
    if (shouldOpenNotes.isEmpty()) return
    val addedNotes = newNotes - oldNotes.toSet()

    val addedNoteNames = addedNotes.map { it.serverPath }
    val readyToOpen = shouldOpenNotes.filter { it.second.serverPath in addedNoteNames }
    readyToOpen.forEach { (project, notePath) ->
      val noteRfsPath = addedNotes.find { it.serverPath == notePath.serverPath } ?: return@forEach
      val driver = ZeppelinDriverManager.getDriver(project, config.innerId) ?: error("Driver is not found")
      ZeppelinFileTypeViewer().openFile(driver = driver, project = project, notePath = noteRfsPath, requestFocus = false)
    }

    shouldOpenNotes -= readyToOpen.toSet()
  }

  private fun moveNoteToTrash(path: ZeppelinRfsPath): Boolean = withCheckConnection {
    api.moveNoteToTrash(path.id)
  }

  private fun moveFolderToTrash(path: ZeppelinRfsPath) = withCheckConnection {
    api.moveFolderToTrash(path.serverPath)
  }

  internal fun <T> withCheckConnection(body: () -> T) = if (!connection.isConnected()) {
    false
  }
  else {
    body()
    true
  }

  internal fun getSourcesNotesWithTargetPaths(fromFileInfo: ZeppelinFileInfo,
                                              toPath: ZeppelinRfsPath): List<Pair<ZeppelinRfsPath, ZeppelinRfsPath>> {
    val fromPath = fromFileInfo.path

    val prevPaths = getNotesInPath(fromFileInfo.path) ?: emptyList()
    val newPaths = prevPaths.map { it.replacePrefix(fromPath.nameCount, toPath) }

    return prevPaths.zip(newPaths)
  }

  data class NoteWithPath(val note: ZeppelinNotebook, val path: ZeppelinRfsPath)
}