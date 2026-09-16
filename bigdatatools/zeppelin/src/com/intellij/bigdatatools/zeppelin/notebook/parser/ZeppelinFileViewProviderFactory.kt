package com.intellij.bigdatatools.zeppelin.notebook.parser

import com.intellij.bigdatatools.notebooks.core.api.NotebookConstants
import com.intellij.bigdatatools.notebooks.core.impl.file.NotebookVirtualFile
import com.intellij.lang.Language
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.FileViewProvider
import com.intellij.psi.FileViewProviderFactory
import com.intellij.psi.PsiManager
import com.intellij.psi.SingleRootFileViewProvider
import com.intellij.testFramework.LightVirtualFile

class ZeppelinFileViewProviderFactory : FileViewProviderFactory {
  override fun createFileViewProvider(file: VirtualFile,
                                      language: Language,
                                      manager: PsiManager,
                                      eventSystemEnabled: Boolean): FileViewProvider =
    if (file is NotebookVirtualFile || (file is LightVirtualFile && file.content.startsWith(NotebookConstants.PARAGRAPH_DELIMITER)))
      ZeppelinFileViewProvider(manager, file, true)
    else
      SingleRootFileViewProvider(manager, file)
}