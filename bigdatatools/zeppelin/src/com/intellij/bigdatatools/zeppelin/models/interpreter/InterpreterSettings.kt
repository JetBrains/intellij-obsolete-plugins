package com.intellij.bigdatatools.zeppelin.models.interpreter

import com.intellij.openapi.util.NlsSafe
import com.squareup.moshi.Json

data class InterpreterSettings(val id: String = "",
                               @NlsSafe val name: String,
                               val group: String = name,
                               val dependencies: List<ZepDependency> = emptyList(),
                               val status: InterpreterStatus = InterpreterStatus.NOT_CREATED,
                               val properties: Map<String, InterpreterProperty> = emptyMap(),
                               val option: InterpreterOption = InterpreterOption(),
                               val interpreterGroup: List<InterpreterInfo> = emptyList(),
                               @NlsSafe val errorReason: String? = "")

data class InterpreterInfo(@NlsSafe val name: String = "",
                           @field:Json(name = "class") @Json(name = "class") val className: String = "",
                           val defaultInterpreter: Boolean = false,
                           val editor: Map<String, Any?> = mapOf(),
                           val config: Map<String, Any?> = mapOf()
)

data class InterpreterOption(
  var perNote: String? = InstantiationType.SHARED,
  var perUser: String? = InstantiationType.SHARED,

  var setPermission: Boolean = false,
  var owners: List<String> = listOf(),

  var isExistingProcess: Boolean = false,
  var host: String? = null,
  var port: Int = -1,

  @Deprecated("We do not need it")
  var remote: Boolean = true,
  @Deprecated("We do not need it")
  var isUserImpersonate: Boolean = true
)

@Suppress("unused")
enum class InterpreterPropertyType(val value: String) {
  @field:Json(name = "textarea")
  textarea("textarea"),

  @field:Json(name = "boolean")
  boolean("boolean"),

  @field:Json(name = "text")
  text("text"),

  @field:Json(name = "string")
  string("string"),

  @field:Json(name = "number")
  number("number"),

  @field:Json(name = "url")
  url("url"),

  @field:Json(name = "password")
  password("password"),

  @field:Json(name = "checkbox")
  checkbox("checkbox");
}

data class InterpreterProperty(val name: String = "",
                               var value: Any = "",
                               var defaultValue: Any = "",
                               val type: InterpreterPropertyType = InterpreterPropertyType.string,
                               val description: String = "")

@Suppress("unused")
enum class InterpreterStatus {
  READY, DOWNLOADING_DEPENDENCIES, ERROR, PENDING, NOT_CREATED
}

data class Interpreter(val id: String,
                       val name: String,
                       val interpreters: List<InterpreterInfo>,
                       val selected: Boolean = true) {
  val nameWithSubs: String
    get() = "$name ${interpreters.joinToString(", ") { "%${it.name}" }}"
}

object InstantiationType {
  @NlsSafe
  const val SHARED = "shared"
  const val SCOPED = "scoped"
  const val ISOLATED = "isolated"
  const val NONE = ""

  val values = listOf(SHARED, SCOPED, ISOLATED, NONE)
  fun parse(value: String): String = try {
    values.firstOrNull { it.lowercase() == value.lowercase() } ?: SHARED
  }
  catch (e: Exception) {
    SHARED
  }

}

data class ZepDependency(var groupArtifactVersion: String = "",
                         var exclusions: List<String> = emptyList(),
                         var local: Boolean = false,
                         var transitive: Boolean = true) {
  private val parts: List<String>
    get() = groupArtifactVersion.split(":")
  val group: String
    get() = if (isMaven()) parts[0] else ""
  val id: String
    get() = if (isMaven()) parts[1] else ""
  val version: String
    get() = if (isMaven()) parts.last() else ""

  fun getType() = when {
    groupArtifactVersion.startsWith(MODULE_NAME_PREFIX) -> DepType.MODULE
    groupArtifactVersion.split(":").size >= 3 -> DepType.MAVEN
    else -> DepType.FILE
  }

  fun isMaven() = getType() == DepType.MAVEN
  fun isFile() = getType() == DepType.FILE
  fun isModule() = getType() == DepType.MODULE

  val moduleName get() = groupArtifactVersion.removePrefix(MODULE_NAME_PREFIX).trim()

  companion object {
    const val MODULE_NAME_PREFIX = "Module:"
  }
}

enum class DepType {
  MAVEN, FILE, MODULE
}