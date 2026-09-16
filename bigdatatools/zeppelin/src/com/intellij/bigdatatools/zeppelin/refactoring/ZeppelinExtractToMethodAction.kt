package com.intellij.bigdatatools.zeppelin.refactoring

import com.intellij.bigdatatools.zeppelin.utils.ZepMessagesBundle
import com.intellij.lang.refactoring.RefactoringSupportProvider
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.psi.PsiClass
import com.intellij.refactoring.RefactoringActionHandler
import com.intellij.refactoring.actions.BasePlatformRefactoringAction
import org.jetbrains.plugins.scala.lang.refactoring.ScalaRefactoringSupportProvider

class ZeppelinExtractToMethodAction : BasePlatformRefactoringAction() {
  var initialSelectionClass: PsiClass? = null

  override fun isAvailableInEditorOnly(): Boolean = true

  override fun getRefactoringHandler(provider: RefactoringSupportProvider): RefactoringActionHandler? =
    if (provider is ScalaRefactoringSupportProvider) ZeppelinExtractToMethodHandler(this) else null

  override fun update(e: AnActionEvent) {
    e.presentation.isEnabledAndVisible = ZeppelinExtractRefactoringUtil.isActionAccessible(e)
    e.presentation.text = ZepMessagesBundle.message("action.Zeppelin.ExtractToMethod.text")
  }

  override fun getActionUpdateThread() = ActionUpdateThread.BGT
}