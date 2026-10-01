// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.python.huggingFace.modelChoice

import com.intellij.notebooks.ui.editor.actions.JupyterEditorAction
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.util.registry.Registry
import com.intellij.psi.PsiDocumentManager
import com.intellij.python.community.impl.huggingFace.service.HuggingFaceCardsUsageCollector
import com.intellij.python.huggingFace.modelChoice.ui.HfModelSelectionDialog
import org.jetbrains.annotations.ApiStatus

@ApiStatus.Internal
class HfOpenModelSelectionWindowAction: DumbAwareAction(), JupyterEditorAction {
  override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    val editor = e.getData(CommonDataKeys.EDITOR) ?: return
    val fileExtensionForTracking = when (getFileExtension(editor)) {
      "py" -> HuggingFaceCardsUsageCollector.ActiveFileType.PY
      "ipynb" -> HuggingFaceCardsUsageCollector.ActiveFileType.IPYNB
      else -> return
    }
    val dialog = HfModelSelectionDialog(project, fileExtensionForTracking)
    dialog.show()
  }

  override fun update(e: AnActionEvent) {
    val editor = e.getData(CommonDataKeys.EDITOR)
    val presentation = e.presentation
    val viewer = editor?.isViewer ?: false
    if (viewer || !isPythonOrJupyter(editor)) {
      presentation.isEnabledAndVisible = false
    } else {
      presentation.isEnabledAndVisible = Registry.`is`("hugging.face.model.selection", false)
    }
  }

  private fun isPythonOrJupyter(editor: Editor?): Boolean {
    // The file must be either .py or .ipynb
    // Extension point would've been a more solid solution, but the jupyter PSI module
    // should not depend on any python-related module
    val fileExtension = getFileExtension(editor)
    return fileExtension == "py" || fileExtension == "ipynb"
  }

  private fun getFileExtension(editor: Editor?): String? {
    if (editor == null) return null
    val project = editor.project ?: return null
    val psi = PsiDocumentManager.getInstance(project).getPsiFile(editor.document) ?: return null
    val virtualFile = psi.virtualFile ?: return null
    return virtualFile.extension
  }
}
