package com.intellij.bigdatatools.zeppelin.formatter

import com.intellij.psi.PsiElement
import com.intellij.psi.templateLanguages.OuterLanguageElement

object ZeppelinFormatterUtil {
  fun splitByOuter(elements: List<PsiElement>): List<List<PsiElement>> {
    if (elements.isEmpty()) return emptyList()
    val result = mutableListOf<List<PsiElement>>()
    val loopedList = elements + elements.first()
    var curElements = mutableListOf<PsiElement>()
    for (el in loopedList) {
      if (el is OuterLanguageElement && curElements.isNotEmpty()) {
        result.add(curElements.toList())
        curElements = mutableListOf()
        continue
      }
      if (el !is OuterLanguageElement)
        curElements.add(el)
    }
    return result
  }
}