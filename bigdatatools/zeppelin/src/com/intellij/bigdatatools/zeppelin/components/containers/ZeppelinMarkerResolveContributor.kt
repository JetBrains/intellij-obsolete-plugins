package com.intellij.bigdatatools.zeppelin.components.containers

import com.intellij.bigdatatools.notebooks.core.impl.editor.NotebookEditorUtils
import com.intellij.bigdatatools.zeppelin.components.connections.ZeppelinConnectionListener
import com.intellij.bigdatatools.zeppelin.components.containers.service.ZeppelinNoteCacheConnection
import com.intellij.bigdatatools.zeppelin.editor.ZeppelinEditor
import com.intellij.bigdatatools.zeppelin.file.NotebookFileUtil
import com.intellij.bigdatatools.zeppelin.models.interpreter.Interpreter
import com.intellij.bigdatatools.zeppelin.models.interpreter.InterpreterSettings
import com.intellij.bigdatatools.zeppelin.notebook.interpreter.ZeppelinMarkerResolver
import com.intellij.openapi.Disposable

class ZeppelinMarkerResolveContributor(private val cacheConnection: ZeppelinNoteCacheConnection, val editor: ZeppelinEditor) : Disposable {
  private val virtualFile = editor.file

  private val interpreters
    get() = cacheConnection.interpreterSettings
  private val bindings
    get() = cacheConnection.interpreterBindings

  private val config = cacheConnection.config

  private val connectionListener = object : ZeppelinConnectionListener {
    override fun updateInterpreterSettings(interpreterSettings: List<InterpreterSettings>) = updateMarkers()
    override fun updateInterpreterBindings(bindings: List<Interpreter>): Unit = updateMarkers()
  }

  init {
    cacheConnection.addListener(connectionListener)

    NotebookEditorUtils.refreshHighlight(editor)
    updateMarkers()
  }

  override fun dispose() {
    cacheConnection.removeListener(connectionListener)
    val noteId = virtualFile.let { NotebookFileUtil.getNotebookId(it) }
    ZeppelinMarkerResolver.clearInterpreters(config.innerId, noteId)
  }

  @Suppress("UNCHECKED_CAST")
  private fun interpreterToLanguages(interpreters: List<InterpreterSettings>, bindings: List<Interpreter>): List<Pair<String, String>> {
    val interpreterToLanguage = convertInterpreters(interpreters)

    val defaultBindingInterpreter = getDefaultBindingInterpreter(bindings)?.let { (id, group) ->
      interpreterToLanguage.firstOrNull { it.group == group && it.interpreterId == id }
    }?.copy(isDefault = true)

    val instanceDefaultInterpreter = interpreterToLanguage.firstOrNull { it.isDefault }

    val defaultInterpreter = defaultBindingInterpreter ?: instanceDefaultInterpreter

    val default = defaultInterpreter?.let { "" to it.language }?.let { listOf(it) } ?: listOf()

    val shortGroups = interpreterToLanguage
      .filter { it.interpreterId == defaultInterpreter?.interpreterId }
      .map { it.group to it.language }

    val fullGroups = interpreterToLanguage.map { "${it.interpreterId}.${it.group}" to it.language }

    val defaultInterpreterLanguages = interpreterToLanguage
      .groupBy { it.interpreterId }
      .map { it.key to (it.value.firstOrNull()?.language ?: it.key) }

    return default + shortGroups + fullGroups + defaultInterpreterLanguages
  }

  @Suppress("UNCHECKED_CAST")
  private fun convertInterpreters(interpreterSettings: List<InterpreterSettings>): List<InterpreterToLanguage> {
    return interpreterSettings.flatMap { interpreter ->
      val id = interpreter.id

      interpreter.interpreterGroup.map { group ->
        val language = group.editor["language"] as? String ?: return@map null
        InterpreterToLanguage(id, group.name, language, group.defaultInterpreter)
      }.filterNotNull()
    }
  }

  private fun updateMarkers() = synchronized(this) {
    val result = interpreterToLanguages(interpreters, bindings).toMap()
    if (result.isEmpty()) {
      return@synchronized
    }

    val noteId = virtualFile.let { NotebookFileUtil.getNotebookId(it) }
    val isChanged = ZeppelinMarkerResolver.addExistedInterpreters(result, config.innerId, noteId)

    if (isChanged)
      NotebookEditorUtils.refreshHighlight(editor)
  }

  private fun getDefaultBindingInterpreter(bindings: List<Interpreter>): Pair<String, String>? {
    val binding = bindings.firstOrNull()

    val id = binding?.id ?: return null
    val groupName = bindings.firstOrNull()?.interpreters?.firstOrNull()?.name ?: return null

    return id to groupName
  }

  private data class InterpreterToLanguage(val interpreterId: String, val group: String, val language: String, val isDefault: Boolean)
}