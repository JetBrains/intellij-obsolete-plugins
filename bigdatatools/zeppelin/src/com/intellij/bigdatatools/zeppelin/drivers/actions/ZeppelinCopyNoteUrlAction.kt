package com.intellij.bigdatatools.zeppelin.drivers.actions

import com.intellij.bigdatatools.zeppelin.drivers.fileinfo.ZeppelinFileInfo
import com.intellij.openapi.project.Project
import com.jetbrains.bigdatatools.common.rfs.driver.FileInfo
import com.jetbrains.bigdatatools.common.rfs.projectview.actions.CopyFilePathActionBase

class ZeppelinCopyNoteUrlAction : CopyFilePathActionBase() {
  override fun getQualifiedName(fileInfo: FileInfo, preview: Boolean, project: Project): String? {
    return (fileInfo as? ZeppelinFileInfo)?.noteUrl.takeIf { !fileInfo.isDirectory }
  }
}