package com.intellij.aiplayground.python

import com.intellij.aiplayground.models.statistic.PlaygroundCollector
import com.intellij.aiplayground.ui.utils.isChinaRegion
import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction
import com.intellij.codeInspection.util.IntentionFamilyName
import com.intellij.codeInspection.util.IntentionName
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.registry.Registry
import com.intellij.psi.PsiElement

class AiPlaygroundPromptIntention : PsiElementBaseIntentionAction() {
  override fun getText(): @IntentionName String = AIPlaygroundPythonBundle.message("intention.text")

  override fun invoke(project: Project, editor: Editor?, element: PsiElement) {
    openPlaygroundWithPrompt(project, element)
    PlaygroundCollector.logIntentionClicked()
  }

  override fun isAvailable(project: Project, editor: Editor?, element: PsiElement): Boolean {
    if (!enabled) return false
    return element.asPromptElementOrNull(mustBeFirstChild = false) != null
  }

  override fun startInWriteAction(): Boolean = false

  override fun getFamilyName(): @IntentionFamilyName String = AIPlaygroundPythonBundle.message("intention.family.name")

  private val enabled: Boolean
    get() {
      return Registry.`is`("aiplayground.inlay.hint.for.prompts.enabled")
             && !isChinaRegion()
    }
}
