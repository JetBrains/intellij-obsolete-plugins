package com.intellij.bigdatatools.zeppelin.components.connections.parser

import com.intellij.bigdatatools.zeppelin.models.connection.ZeppelinInfo
import com.intellij.bigdatatools.zeppelin.models.interpreter.InterpreterInfo
import com.intellij.bigdatatools.zeppelin.models.interpreter.InterpreterOption
import com.intellij.bigdatatools.zeppelin.models.interpreter.InterpreterProperty
import com.intellij.bigdatatools.zeppelin.models.interpreter.InterpreterSettings
import com.intellij.bigdatatools.zeppelin.models.interpreter.InterpreterStatus
import com.intellij.bigdatatools.zeppelin.models.interpreter.ZepDependency
import com.intellij.bigdatatools.zeppelin.utils.JsonParser

internal object ZeppelinVersionAdapter7 : DefaultZeppelinVersionAdapter() {
  override fun isSupport(info: ZeppelinInfo): Boolean = info.versionInt == 7

  override val isAllowedRemoveLastCell: Boolean = false

  override fun parseInterpretersSettings(data: Any): List<InterpreterSettings> {
    val interpreters7 = JsonParser.fromValueList(data, InterpreterSettings7::class.java)
    return interpreters7.map { it.toNormal() }
  }

  override fun parseInterpreterSettings(data: Any): InterpreterSettings {
    val interpreters7 = JsonParser.fromValueObject(data, InterpreterSettings7::class.java)
    return interpreters7.toNormal()
  }

  override fun encodeInterpreterSettings(interpreterSettings: InterpreterSettings) =
    InterpreterSettings7.fromNormal(interpreterSettings)

  override fun encodeProperties(interpreterSettings: InterpreterSettings) =
    interpreterSettings.properties.map { (key, prop) ->
      val name = prop.name.ifBlank { key }
      name to prop.value.toString()
    }.toMap()

  data class InterpreterSettings7(val id: String,
                                  val name: String,
                                  val group: String,
                                  val dependencies: List<ZepDependency> = emptyList(),
                                  val status: InterpreterStatus,
                                  val option: InterpreterOption = InterpreterOption(),
                                  val properties: Map<String, String> = emptyMap(),
                                  val interpreterGroup: List<InterpreterInfo>,
                                  val errorReason: String?) {
    fun toNormal() = InterpreterSettings(id = id,
                                         name = name,
                                         group = group,
                                         dependencies = dependencies,
                                         status = status,
                                         properties = properties.map { it.key to InterpreterProperty(it.key, it.value) }.toMap(),
                                         option = option,
                                         interpreterGroup = interpreterGroup,
                                         errorReason = errorReason)

    companion object {
      fun fromNormal(interpreterSettings: InterpreterSettings): InterpreterSettings7 =
        InterpreterSettings7(id = interpreterSettings.id,
                             name = interpreterSettings.name,
                             group = interpreterSettings.group,
                             dependencies = interpreterSettings.dependencies,
                             status = interpreterSettings.status,
                             option = interpreterSettings.option,
                             properties = interpreterSettings.properties.map { (key, prop) ->
                               val name = prop.name.ifBlank { key }
                               name to prop.value.toString()
                             }.toMap(),
                             interpreterGroup = interpreterSettings.interpreterGroup,
                             errorReason = interpreterSettings.errorReason)
    }
  }
}