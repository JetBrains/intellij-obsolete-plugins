package com.intellij.bigdatatools.plugin.spark

import com.intellij.psi.PsiElement

fun PsiElement.textIs(text: String): Boolean {
  return this.textLength == text.length && this.text == text
}

fun PsiElement.textIn(accepted: Set<String>): Boolean {
  return this.textLength in accepted.map { it.length } && this.text in accepted
}