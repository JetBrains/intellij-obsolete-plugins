package com.intellij.bigdatatools.zeppelin.file

import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.bigdatatools.common.notebooks.NotebookFileService

class ZeppelinNotebookFileService : NotebookFileService {
  override fun getConfigId(file: VirtualFile) = NotebookFileUtil.getConfigId(file)
}