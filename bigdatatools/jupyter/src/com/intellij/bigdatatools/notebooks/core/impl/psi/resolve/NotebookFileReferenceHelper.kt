package com.intellij.bigdatatools.notebooks.core.impl.psi.resolve

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFileSystemItem
import com.intellij.psi.impl.source.resolve.reference.impl.providers.FileReferenceHelper
import com.intellij.psi.impl.source.resolve.reference.impl.providers.NullFileReferenceHelper
import com.intellij.bigdatatools.notebooks.core.impl.file.NotebookVirtualFile

class NotebookFileReferenceHelper : FileReferenceHelper() {
  override fun getContexts(project: Project, file: VirtualFile): Collection<PsiFileSystemItem> =
    (file as? NotebookVirtualFile)?.originalFile?.let {
      NullFileReferenceHelper.INSTANCE.getContexts(project, it)
    } ?: emptyList()

  override fun isMine(project: Project, file: VirtualFile): Boolean = file is NotebookVirtualFile
}
