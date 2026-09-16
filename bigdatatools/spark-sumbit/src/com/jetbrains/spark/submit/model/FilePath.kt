package com.jetbrains.spark.submit.model

import com.intellij.openapi.util.NlsSafe
import com.intellij.util.xmlb.annotations.OptionTag
import com.intellij.util.xmlb.annotations.Transient

data class FilePath(@get:Transient var type: FileType = FileType.CUSTOM, @NlsSafe var path: String = "") {
  @get:OptionTag(value = "type")
  var typeSerialized: String
    get() = type.schemeNoSlash
    set(value) { type = FileType.migrateFromEnum(value) }

  fun isBlank(): Boolean = path.isBlank()

  fun getAsSelectedPath() = if (type in FileType.WEB_TYPES) type.scheme + path else path

  @NlsSafe
  override fun toString() = type.scheme + path

  companion object {
    fun fromPathWithScheme(path: String, defaultScheme: FileType = FileType.CUSTOM): FilePath {
      val split = path.split("://", limit = 2)
      return when (split.size) {
        2 -> FilePath(FileType(split[0]), split[1])
        else -> FilePath(defaultScheme, path)
      }
    }
  }
}