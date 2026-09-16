package com.intellij.bigdatatools.plugin.spark.scala.dfanalysis.impl

import com.intellij.bigdatatools.plugin.spark.assistance.dfanalysis.DfTypeSource
import com.intellij.bigdatatools.plugin.spark.scala.dfanalysis.DfTypeSourceProvider
import com.intellij.psi.PsiElement
import org.jetbrains.plugins.scala.lang.psi.api.ScalaPsiElement
import org.jetbrains.plugins.scala.lang.psi.api.statements.ScFunctionDefinition
import org.jetbrains.plugins.scala.lang.psi.api.statements.ScPatternDefinition
import org.jetbrains.plugins.scala.lang.psi.api.statements.ScValueOrVariableDefinition

class DfConvertFromContextTypeSourceProvider : DfTypeSourceProvider {
  override fun getTypeSource(psiElement: PsiElement): DfTypeSource? {
    if (psiElement !is ScFunctionDefinition && psiElement !is ScPatternDefinition && psiElement !is ScValueOrVariableDefinition) return null

    return DfComputingUtil.findContext(psiElement as ScalaPsiElement)?.let {
      DfTypeSourceProvider.createSourceFromTypeContext(it, psiElement)
    }
  }
}