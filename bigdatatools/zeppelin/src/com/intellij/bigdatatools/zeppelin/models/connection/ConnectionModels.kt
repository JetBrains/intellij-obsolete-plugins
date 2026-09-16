package com.intellij.bigdatatools.zeppelin.models.connection


data class ZeppelinCredentials(val principal: String = "anonymous", val ticket: String = "anonymous", val roles: String = "")

data class Progress(val id: String, val progress: Int)

data class AngularUpdateResponse(val angularObject: Map<String, Any>, val noteId: String, val interpreterGroupId: String)
data class AngularRemoveResponse(val name: String, val noteId: String)

data class ZeppelinInfo(val version: String) {
  val versionInt
    get() = try {
      version.split(".")[1].toInt()
    }
    catch (e: Exception) {
      -1
    }
}