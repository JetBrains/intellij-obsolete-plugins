package com.intellij.aiplayground.python

import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.psi.PsiElement

object InjectedLanguagesDetector {
  private val UNDESIRABLE_INJECTED_LANGUAGE_IDS = listOf("SQL")

  fun anyUndesirableInjectedLanguageDetected(element: PsiElement): Boolean {
    return InjectedLanguageManager
             .getInstance(element.project)
             .getInjectedPsiFiles(element)
             ?.map { it.first.language.baseLanguage?.id }  // TODO baseLanguage exists for SQL, but what if we want to detect other languages, which are not so dialect-rich? we need to look at language, not baseLanguage
             ?.intersect(UNDESIRABLE_INJECTED_LANGUAGE_IDS)
             ?.isNotEmpty()
           ?: false
  }
}