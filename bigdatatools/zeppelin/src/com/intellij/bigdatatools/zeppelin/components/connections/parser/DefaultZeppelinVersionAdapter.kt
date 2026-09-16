package com.intellij.bigdatatools.zeppelin.components.connections.parser

import com.intellij.bigdatatools.zeppelin.dependency.collector.builtin.Repository
import com.intellij.bigdatatools.zeppelin.models.interpreter.InterpreterSettings
import com.intellij.bigdatatools.zeppelin.models.notebook.NotebookInfo
import com.intellij.bigdatatools.zeppelin.utils.JsonParser

abstract class DefaultZeppelinVersionAdapter : ZeppelinVersionAdapter {
  override val isAllowedRemoveLastCell: Boolean = true

  override fun encodeInterpreterSettings(interpreterSettings: InterpreterSettings): Any = interpreterSettings

  override fun encodeProperties(interpreterSettings: InterpreterSettings): Map<String, Any> = interpreterSettings.properties

  override fun parseRepositories(repos: Any) = JsonParser.fromValueList(repos, Repository::class.java)

  override fun parseNotebookInfos(data: Any): List<NotebookInfo> =
    JsonParser.fromValueList(data, NotebookInfo::class.java)

  override fun parseInterpretersSettings(data: Any): List<InterpreterSettings> {
    val interpreters = JsonParser.fromValueList(data, InterpreterSettings::class.java)
    return interpreters.map { transformInterpreterSettings(it) }
  }

  override fun parseInterpreters(data: Any): List<InterpreterSettings> {
    val interpreters = JsonParser.fromValueList(data, InterpreterSettings::class.java)
    return interpreters.map { transformInterpreterSettings(it) }
  }

  override fun parseInterpreterSettings(data: Any): InterpreterSettings {
    val interpreterSettings = JsonParser.fromValueObject(data, InterpreterSettings::class.java)
    return transformInterpreterSettings(interpreterSettings)
  }

  private fun transformInterpreterSettings(interpreter: InterpreterSettings): InterpreterSettings {
    val transformedProperties = interpreter.properties.map {
      if (it.key.isNotBlank())
        it.key to it.value.copy(name = it.key)
      else
        it.value.name to it.value
    }.toMap()

    return interpreter.copy(properties = transformedProperties)
  }
}