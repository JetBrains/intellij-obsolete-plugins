package com.intellij.bigdatatools.zeppelin.file

import com.intellij.bigdatatools.zeppelin.inote.file.INoteFileType
import com.intellij.bigdatatools.notebooks.core.impl.file.NotebookVirtualFile
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.newvfs.FileAttribute
import com.jetbrains.bigdatatools.common.rfs.util.RfsFileUtil

object NotebookFileUtil {
  private val FILE_IS_REMOTE = Key<Boolean>("isRemoteNotebookFlag")
  private val CONFIG_ID = Key<String?>("connectionId")
  private val CONFIG_SPECIFICATION_ID = Key<String?>("config_specification")
  private val NOTEBOOK_ID = Key<String?>("notebookId")
  private val CONTAINER_ID = Key<String?>("zeppelinContainerId")

  fun setRemoteFlag(file: VirtualFile) = getOriginFile(file).putCopyableUserData(FILE_IS_REMOTE, true)
  fun isRemote(file: VirtualFile) = getOriginFile(file).getCopyableUserData(FILE_IS_REMOTE) ?: false

  fun setConfigId(file: VirtualFile, configId: String) {
    setInMemoryConfigId(file, configId)
    setPersistentConfigId(file, configId)
  }

  fun setConfigSpecification(file: VirtualFile, specification: String) {
    setInMemoryConfigSpecification(file, specification)
    setPersistentConfigSpecification(file, specification)
  }

  fun getConfigSpecification(file: VirtualFile): String? {
    val inMemoryConfigId = getInMemoryConfigSpecification(file)
    if (inMemoryConfigId != null) return inMemoryConfigId
    val storedConfigId = getPersistentConfigSpecification(file) ?: return null
    setInMemoryConfigSpecification(file, storedConfigId)
    return storedConfigId
  }


  fun getConfigId(file: VirtualFile): String? {
    val inMemoryConfigId = getInMemoryConfigId(file)
    if (inMemoryConfigId != null) return inMemoryConfigId
    val storedConfigId = getPersistentConfigId(file) ?: return null
    setInMemoryConfigId(file, storedConfigId)
    return storedConfigId
  }

  fun setNotebookId(file: VirtualFile, configId: String) = getOriginFile(file)
    .putCopyableUserData(NOTEBOOK_ID, configId)

  fun getNotebookId(file: VirtualFile): String? = getOriginFile(file)
    .getCopyableUserData(NOTEBOOK_ID)

  fun setContainerId(file: VirtualFile, id: String) = getOriginFile(file).putCopyableUserData(CONTAINER_ID, id)
  fun getContainerId(file: VirtualFile): String? = getOriginFile(file).getCopyableUserData(CONTAINER_ID) ?: getNotebookId(file)

  private fun getInMemoryConfigId(file: VirtualFile): String? = getOriginFile(file)
    .getCopyableUserData(CONFIG_ID)

  private fun getInMemoryConfigSpecification(file: VirtualFile): String? = getOriginFile(file)
    .getCopyableUserData(CONFIG_SPECIFICATION_ID)


  private fun setInMemoryConfigId(file: VirtualFile, configId: String) = getOriginFile(file)
    .putCopyableUserData(CONFIG_ID, configId)

  private fun setInMemoryConfigSpecification(file: VirtualFile, configId: String) = getOriginFile(file)
    .putCopyableUserData(CONFIG_SPECIFICATION_ID, configId)


  private fun getPersistentConfigId(file: VirtualFile): String? {
    val originFile = getOriginFile(file)
    if (!isPersistentZeppelinFile(originFile))
      return null

    return RfsFileUtil.readFromAttribute(REMOTE_CONFIG_ID_ATTRIBUTE, originFile)
  }

  private fun getPersistentConfigSpecification(file: VirtualFile): String? {
    val originFile = getOriginFile(file)
    if (!isPersistentZeppelinFile(originFile))
      return null
    return RfsFileUtil.readFromAttribute(CONFIG_SPECIFICATION_ATTRIBUTE, file)
  }

  private fun setPersistentConfigId(file: VirtualFile, configId: String) {
    val originFile = getOriginFile(file)
    if (!isPersistentZeppelinFile(originFile))
      return
    if (getPersistentConfigId(originFile) == configId)
      return

    RfsFileUtil.setPersistentAttribute(REMOTE_CONFIG_ID_ATTRIBUTE, originFile, configId)
  }

  private fun setPersistentConfigSpecification(file: VirtualFile, configSpecificationId: String) {
    val originFile = getOriginFile(file)

    if (!isPersistentZeppelinFile(originFile))
      return
    if (getPersistentConfigId(originFile) == configSpecificationId)
      return

    RfsFileUtil.setPersistentAttribute(CONFIG_SPECIFICATION_ATTRIBUTE, originFile, configSpecificationId)
  }


  fun getOriginFile(file: VirtualFile) = if (file is NotebookVirtualFile)
    file.originFile
  else
    file

  private fun isPersistentZeppelinFile(file: VirtualFile) = file.isInLocalFileSystem


  private val REMOTE_CONFIG_ID_ATTRIBUTE = FileAttribute("remoteConfigAttribute", 1, true)
  private val CONFIG_SPECIFICATION_ATTRIBUTE = FileAttribute("configSpecificationAttribute", 1, true)
}

internal fun VirtualFile.getOriginalFile(): VirtualFile = NotebookFileUtil.getOriginFile(this)

val VirtualFile.isNoteFile: Boolean
  get() = fileType == ZeppelinFileType || extension == ZeppelinFileType.defaultExtension ||
          fileType == INoteFileType || extension == INoteFileType.defaultExtension