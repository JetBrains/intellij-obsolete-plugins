package com.intellij.bigdatatools.zeppelin.structureview

import com.intellij.bigdatatools.zeppelin.psi.ZeppelinPsiFile
import com.intellij.ide.structureView.StructureViewModel
import com.intellij.ide.structureView.TreeBasedStructureViewBuilder
import com.intellij.openapi.editor.Editor

class ZeppelinStructureViewBuilder(private val psiFile: ZeppelinPsiFile) : TreeBasedStructureViewBuilder() {
  override fun createStructureViewModel(editor: Editor?): StructureViewModel = ZeppelinStructureViewModel(psiFile, editor)
}