package com.intellij.bigdatatools.zeppelin.components.connections.parser

import com.intellij.bigdatatools.zeppelin.api.remote.ZeppelinVersionIsNotSupportedException
import com.intellij.bigdatatools.zeppelin.dependency.collector.builtin.Repository
import com.intellij.bigdatatools.zeppelin.models.connection.ZeppelinInfo
import com.intellij.bigdatatools.zeppelin.models.interpreter.InterpreterSettings
import com.intellij.bigdatatools.zeppelin.models.notebook.NotebookInfo
import com.intellij.bigdatatools.zeppelin.utils.JsonParser

interface ZeppelinVersionAdapter {
  val isAllowedRemoveLastCell: Boolean
  fun isSupport(info: ZeppelinInfo): Boolean
  fun parseNotebookInfos(data: Any): List<NotebookInfo>
  fun parseInterpretersSettings(data: Any): List<InterpreterSettings>
  fun parseInterpreters(data: Any): List<InterpreterSettings>
  fun parseInterpreterSettings(data: Any): InterpreterSettings
  fun encodeInterpreterSettings(interpreterSettings: InterpreterSettings): Any
  fun encodeProperties(interpreterSettings: InterpreterSettings): Map<String, Any>
  fun parseRepositories(repos: Any): List<Repository>

  companion object {
    private val supportedParsers = setOf(ZeppelinVersionAdapter7, ZeppelinVersionAdapter8, ZeppelinVersionAdapter9(),
                                         ZeppelinVersionAdapter10)

    fun parseVersion(versionObject: Any): ZeppelinInfo? {
      val info = try {
        JsonParser.fromValueObject(versionObject, ZeppelinInfo::class.java)
      }
      catch (e: Exception) {
        val version = JsonParser.fromValueObject(versionObject, String::class.java)
        ZeppelinInfo(version)
      }

      return if (!info.version.isBlank()) {
        info
      }
      else {
        null
      }
    }

    fun isVersionSupport(info: ZeppelinInfo) = supportedParsers.any { it.isSupport(info) }

    fun getInstance(zeppelinInfo: ZeppelinInfo): ZeppelinVersionAdapter {
      return supportedParsers.firstOrNull {
        it.isSupport(zeppelinInfo)
      } ?: throw ZeppelinVersionIsNotSupportedException(zeppelinInfo.version, "")
    }
  }
}