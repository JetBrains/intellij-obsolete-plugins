package com.intellij.bigdatatools.zeppelin.structureview

import com.intellij.bigdatatools.notebooks.core.impl.editor.NotebookEditorUtils
import com.intellij.bigdatatools.notebooks.core.impl.file.NotebookVirtualFile
import com.intellij.bigdatatools.zeppelin.constants.ZeppelinIcons
import com.intellij.bigdatatools.zeppelin.controllers.editor.ZeppelinParagraphTitleController
import com.intellij.bigdatatools.zeppelin.models.notebook.ZeppelinNotebook
import com.intellij.bigdatatools.zeppelin.psi.ZeppelinPsiFile
import com.intellij.ide.structureView.StructureViewTreeElement
import com.intellij.ide.util.treeView.smartTree.TreeElement
import com.intellij.navigation.ItemPresentation
import com.intellij.util.containers.toArray

class ZeppelinStructureViewRoot(private val psiFile: ZeppelinPsiFile) : StructureViewTreeElement {
  private val rootPresentation = SimpleItemPresentation(psiFile.name, null, ZeppelinIcons.ZEPPELIN_FILE, ZeppelinIcons.ZEPPELIN_FILE)

  override fun getPresentation(): ItemPresentation = rootPresentation

  override fun getChildren(): Array<TreeElement> {
    val notebookVirtualFile = psiFile.virtualFile as? NotebookVirtualFile ?: return emptyArray()

    return NotebookEditorUtils.getPsiCells(psiFile.project, notebookVirtualFile).let { psiCells ->
      (notebookVirtualFile.notebook as? ZeppelinNotebook)?.cells?.zip(psiCells)
    }?.map { (cell, psiCell) ->
      val title = "Paragraph ${ZeppelinParagraphTitleController.generateTitle(cell)}"
      ZeppelinStructureViewCellElement(psiCell, title)
    }?.toArray(emptyArray<TreeElement>()) ?: emptyArray()
  }

  override fun getValue(): Any = psiFile.name
}