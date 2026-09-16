package com.intellij.bigdatatools.zeppelin.editor.actions

import com.intellij.bigdatatools.notebooks.core.impl.file.NotebookVirtualFile
import com.intellij.openapi.actionSystem.ActionPromoter
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.bigdatatools.zeppelin.file.ZeppelinFileType

class ZeppelinEditorActionPromoter : ActionPromoter {
  override fun promote(actions: List<AnAction>, context: DataContext): List<AnAction> {
    val virtualFile = context.getData(PlatformDataKeys.VIRTUAL_FILE) ?: return emptyList()
    if (virtualFile !is NotebookVirtualFile || virtualFile.fileType !is ZeppelinFileType) return emptyList()
    return actions.asSequence().filter {
      it is ZeppelinEditorAction
    }.toList()
  }
}