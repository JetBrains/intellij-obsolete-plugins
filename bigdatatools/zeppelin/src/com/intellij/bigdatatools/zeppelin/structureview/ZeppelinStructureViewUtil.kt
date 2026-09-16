package com.intellij.bigdatatools.zeppelin.structureview

import com.intellij.bigdatatools.notebooks.core.impl.file.NotebookVirtualFile
import com.intellij.bigdatatools.zeppelin.file.NotebookFileUtil
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiUtilCore
import com.jetbrains.bigdatatools.common.rfs.driver.manager.DriverManager

object ZeppelinStructureViewUtil {
  fun navigateImpl(anchor: PsiElement, requestFocus: Boolean) {
    (PsiUtilCore.getVirtualFile(anchor) as? NotebookVirtualFile)?.let { virtualFile ->
      val driver = NotebookFileUtil.getConfigId(virtualFile)?.let { configId ->
        DriverManager.getDriverById(anchor.project, configId)
      }

      if (driver != null)
        OpenFileDescriptor(anchor.project, virtualFile, if (anchor is PsiFile) -1 else anchor.textOffset).navigate(requestFocus)
    }
  }
}