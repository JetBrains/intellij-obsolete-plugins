package com.intellij.bigdatatools.zeppelin.rfs.path

import com.intellij.bigdatatools.zeppelin.constants.ZeppelinConstants
import com.intellij.bigdatatools.zeppelin.models.notebook.NotebookInfo
import com.jetbrains.bigdatatools.common.rfs.driver.RfsPath
import org.jetbrains.annotations.TestOnly

class ZeppelinRfsPath private constructor(val id: String, elements: List<String>, isDirectory: Boolean) : RfsPath(elements, isDirectory) {

  val serverPath = "/" + stringRepresentation()

  val canonicalPath = serverPath + if (id.isNotEmpty()) "@$id" else ""

  val isInTrash: Boolean = elements.firstOrNull() == ZeppelinConstants.TRASH_NAME && nameCount > 1
  val isTrash: Boolean = elements.firstOrNull() == ZeppelinConstants.TRASH_NAME && nameCount == 1 && isDirectory

  override val parent: ZeppelinRfsPath? = super.parent?.let { fromRfsPath(it) }

  override fun prefixPath(endIndex: Int): ZeppelinRfsPath = fromRfsPath(super.prefixPath(endIndex))

  override fun child(subPath: String, isDirectory: Boolean): ZeppelinRfsPath = fromRfsPath(super.child(subPath, isDirectory))


  override fun replacePrefix(prefixElementsCount: Int, newPrefix: RfsPath): ZeppelinRfsPath =
    fromRfsPath(super.replacePrefix(prefixElementsCount, newPrefix))

  override fun toString() = canonicalPath

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as ZeppelinRfsPath

    if (isDirectory != other.isDirectory) return false
    if (id != other.id) return false
    if (elements != other.elements) return false

    return true
  }

  override fun hashCode(): Int {
    var result = id.hashCode()
    result = 31 * result + super.hashCode()
    return result
  }

  companion object {
    fun createRoot() = ZeppelinRfsPath("", emptyList(), true)

    fun createFromNoteInfo(noteInfo: NotebookInfo): ZeppelinRfsPath = if (noteInfo.isFile)
      createRfsPath(noteInfo.id, noteInfo.name)
    else
      createDir(noteInfo.name)

    fun createRfsPath(id: String, path: String): ZeppelinRfsPath = ZeppelinRfsPath(id, prepareAbsolutePath(path), false)

    fun createFromPath(path: String): ZeppelinRfsPath = ZeppelinRfsPath("", prepareAbsolutePath(path), path.endsWith("/") || path.isBlank())
    fun createDir(path: String): ZeppelinRfsPath = ZeppelinRfsPath("", prepareAbsolutePath(path), true)

    fun fromRfsPath(path: RfsPath) =
      if (path is ZeppelinRfsPath)
        path
      else
        ZeppelinRfsPath("", path.elements, path.isDirectory)

    private fun prepareAbsolutePath(path: String): List<String> {
      return path.split("/").filter { it.isNotBlank() }
    }

    @TestOnly
    fun createFromList(id: String, elements: List<String>, isDirectory: Boolean) = ZeppelinRfsPath(id, elements, isDirectory)
  }
}

fun RfsPath.zeppelinAbsolutePath(): String = ZeppelinRfsPath.fromRfsPath(this).serverPath