package com.intellij.bigdatatools.zeppelin.components.containers.editor.connection

import com.intellij.bigdatatools.zeppelin.components.connections.ZeppelinConnectionListener
import com.intellij.bigdatatools.zeppelin.components.containers.controller.ZeppelinNoteController
import com.intellij.bigdatatools.zeppelin.drivers.ZeppelinDriverManager
import com.intellij.bigdatatools.zeppelin.models.notebook.ZeppelinNotebook
import com.intellij.bigdatatools.zeppelin.rfs.path.ZeppelinRfsPath
import com.intellij.openapi.Disposable
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.bigdatatools.common.rfs.driver.FileInfo
import com.jetbrains.bigdatatools.common.rfs.util.RfsFileUtil

class ZeppelinNoteGoToTargetListener(val controller: ZeppelinNoteController) : Disposable {
  private val cacheConnection = controller.cachedConnection
  private val project = controller.project
  private val config = controller.config
  private val notebook = controller.note
  private val file = controller.originFile

  private val listener = object : ZeppelinConnectionListener {
    override fun updateNotebook(notebook: ZeppelinNotebook) {
      addFileInfoToVirtualFile(ZeppelinRfsPath.createRfsPath(notebook.id, notebook.path ?: notebook.name), file)
    }
  }

  init {
    cacheConnection.addListener(listener)
    addFileInfoToVirtualFile(ZeppelinRfsPath.createRfsPath(notebook.id, notebook.path ?: notebook.name), file)
  }

  override fun dispose() = cacheConnection.removeListener(listener)

  fun addFileInfoToVirtualFile(path: ZeppelinRfsPath, virtualFile: VirtualFile) {
    val fileInfo = getFileInfo(path) ?: return
    RfsFileUtil.setDriverIdAndPath(virtualFile, fileInfo)
  }

  private fun getFileInfo(path: ZeppelinRfsPath): FileInfo? {
    val driver = ZeppelinDriverManager.getDriver(project, config.innerId) ?: return null
    return driver.createFileInfoByPath(path)
  }
}