package com.intellij.bigdatatools.zeppelin.integration

import com.intellij.bigdatatools.zeppelin.language.ZeppelinLanguage
import com.intellij.codeInspection.InspectionProfileEntry
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.jetbrains.plugins.scala.codeInspection.ScalaFileNameInspection
import org.jetbrains.plugins.scala.util.IntentionAvailabilityChecker

class ZeppelinCodeInsightChecker : IntentionAvailabilityChecker() {
  override fun isInspectionAvailable(inspection: InspectionProfileEntry?, psiElement: PsiElement?): Boolean {
    return !(psiElement is PsiFile && inspection is ScalaFileNameInspection)
  }

  override fun canCheck(psiElement: PsiElement?): Boolean = psiElement?.containingFile?.viewProvider?.baseLanguage == ZeppelinLanguage
}