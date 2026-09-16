package com.intellij.bigdatatools.zeppelin.models.notebook

import com.intellij.bigdatatools.zeppelin.constants.ZeppelinConstants
import com.jetbrains.bigdatatools.common.rfs.util.withSlash

class NotebookInfo(val id: String, name: String) {
  val isDirectory: Boolean = id.isEmpty()

  val name = if (isDirectory)
    name.removePrefix("/").withSlash()
  else
    name.removePrefix("/").removeSuffix("/")

  val isFile: Boolean = id.isNotEmpty()
  val isInTrash: Boolean = this.name == ZeppelinConstants.TRASH_NAME || this.name.startsWith("${ZeppelinConstants.TRASH_NAME}/")
  val shortName: String = this.name.removeSuffix("/").split("/").last()

  override fun equals(other: Any?): Boolean = when {
    this === other -> true
    other !is NotebookInfo -> false
    id != other.id -> false
    name != other.name -> false
    else -> true
  }

  override fun hashCode(): Int {
    var result = id.hashCode()
    result = 31 * result + name.hashCode()
    return result
  }
}