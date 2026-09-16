package com.intellij.bigdatatools.zeppelin.models

import com.intellij.bigdatatools.zeppelin.file.ZeppelinRemoteFile
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.impl.EditorTabTitleProvider
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsSafe
import com.intellij.openapi.vfs.VirtualFile

class ZeppelinTabTitleProvider : EditorTabTitleProvider, DumbAware {
  override fun getEditorTabTitle(project: Project, file: VirtualFile): String? {
    if (file !is ZeppelinRemoteFile) return null

    val hasSameNames = filesWithSameName(file, project).isNotEmpty()
    @Suppress("UnnecessaryVariable")
    @NlsSafe
    val filename: String = if (hasSameNames) file.fileSystem.extractPresentableUrl(file.path) else ZeppelinVirtualFileSystem.findDisplayName(file)
    return filename
  }

  private fun filesWithSameName(file: VirtualFile, project: Project): HashSet<VirtualFile> {
    val setOfFilesWithTheSameName = hashSetOf<VirtualFile>()

    for (openFile in FileEditorManager.getInstance(project).openFiles)
      if (openFile.name == file.name && openFile != file) setOfFilesWithTheSameName.add(openFile)

    return setOfFilesWithTheSameName
  }
}