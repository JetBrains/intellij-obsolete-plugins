package com.intellij.bigdatatools.zeppelin.structureview

import com.intellij.bigdatatools.notebooks.core.api.psi.PsiCell
import com.intellij.bigdatatools.zeppelin.notebook.interpreter.supported.InterpreterSupport
import com.intellij.ide.structureView.StructureViewTreeElement
import com.intellij.ide.util.treeView.smartTree.TreeElement
import com.intellij.navigation.ItemPresentation
import com.intellij.util.containers.toArray
import javax.swing.Icon

class ZeppelinStructureViewCellElement(private val psiCell: PsiCell, private val title: String) : StructureViewTreeElement {
  override fun navigate(requestFocus: Boolean) {
    if (!psiCell.isValid) return

    ZeppelinStructureViewUtil.navigateImpl(psiCell, requestFocus)
  }

  override fun getPresentation(): ItemPresentation =
    SimpleItemPresentation(title, null, getLanguageIcon(), getLanguageIcon())

  override fun getChildren(): Array<TreeElement> {
    if (!psiCell.isValid) return emptyArray()

    return InterpreterSupport.getStructureElements(psiCell).toArray(emptyArray())

  }

  override fun canNavigate(): Boolean = true
  override fun getValue(): Any = psiCell
  override fun canNavigateToSource(): Boolean = true

  private fun getLanguageIcon(): Icon? {
    if (!psiCell.isValid) return null
    val anchorEndElement = psiCell.containingFile.viewProvider.findElementAt(psiCell.textRange.endOffset - 1)
    val anchorFile = (anchorEndElement ?: psiCell).containingFile
    return anchorFile.fileType.icon
  }
}