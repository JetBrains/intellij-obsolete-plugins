package com.intellij.bigdatatools.zeppelin.structureview

import com.intellij.bigdatatools.zeppelin.psi.ZeppelinPsiFile
import com.intellij.ide.structureView.StructureViewModel
import com.intellij.ide.structureView.StructureViewTreeElement
import com.intellij.ide.structureView.TextEditorBasedStructureViewModel
import com.intellij.openapi.editor.Editor

class ZeppelinStructureViewModel(psiFile: ZeppelinPsiFile, editor: Editor?) : TextEditorBasedStructureViewModel(editor,
                                                                                                                psiFile), StructureViewModel.ElementInfoProvider {
  override fun getRoot(): StructureViewTreeElement = ZeppelinStructureViewRoot(psiFile as ZeppelinPsiFile)
  override fun isAlwaysShowsPlus(element: StructureViewTreeElement?): Boolean = false
  override fun isAlwaysLeaf(element: StructureViewTreeElement?): Boolean = false
}