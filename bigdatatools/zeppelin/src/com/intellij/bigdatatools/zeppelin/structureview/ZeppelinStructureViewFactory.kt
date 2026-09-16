package com.intellij.bigdatatools.zeppelin.structureview

import com.intellij.bigdatatools.zeppelin.psi.ZeppelinPsiFile
import com.intellij.ide.structureView.StructureViewBuilder
import com.intellij.lang.PsiStructureViewFactory
import com.intellij.psi.PsiFile

class ZeppelinStructureViewFactory : PsiStructureViewFactory {
  //((psiFile as ZeppelinPsiFile).virtualFile as NotebookVirtualFile).notebook.cells
  override fun getStructureViewBuilder(psiFile: PsiFile): StructureViewBuilder? =
    (psiFile as? ZeppelinPsiFile)?.let { ZeppelinStructureViewBuilder(it) }
}

