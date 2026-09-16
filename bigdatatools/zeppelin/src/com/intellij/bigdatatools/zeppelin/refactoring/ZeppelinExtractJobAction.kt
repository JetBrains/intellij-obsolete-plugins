package com.intellij.bigdatatools.zeppelin.refactoring

import com.intellij.bigdatatools.zeppelin.utils.ZepMessagesBundle
import com.intellij.lang.refactoring.RefactoringSupportProvider
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.refactoring.RefactoringActionHandler
import com.intellij.refactoring.actions.BasePlatformRefactoringAction
import org.jetbrains.plugins.scala.lang.refactoring.ScalaRefactoringSupportProvider

class ZeppelinExtractJobAction : BasePlatformRefactoringAction() {
  var initialBaseDir: VirtualFile? = null

  override fun isAvailableInEditorOnly(): Boolean = true

  override fun getRefactoringHandler(provider: RefactoringSupportProvider): RefactoringActionHandler? =
    if (provider is ScalaRefactoringSupportProvider) ZeppelinExtractJobHandler(this) else null

  override fun update(e: AnActionEvent) {
    e.presentation.isEnabledAndVisible = ZeppelinExtractRefactoringUtil.isActionAccessible(e)
    e.presentation.text = ZepMessagesBundle.message("action.Zeppelin.ExtractJob.text")
  }

  override fun getActionUpdateThread() = ActionUpdateThread.BGT
}