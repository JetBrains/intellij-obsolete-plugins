package com.intellij.bigdatatools.plugin.spark.scala.dfanalysis.expr

import com.intellij.bigdatatools.plugin.spark.assistance.dfanalysis.DfColumnType
import com.intellij.bigdatatools.plugin.spark.scala.dfanalysis.impl.DfComputingUtil
import com.intellij.bigdatatools.plugin.spark.scala.dfanalysis.scala.DfScalaTypesMapper
import com.intellij.psi.PsiElement
import org.jetbrains.plugins.scala.lang.psi.api.expr.ScExpression
import org.jetbrains.plugins.scala.lang.psi.api.expr.ScMethodCall

abstract class DfControlFlowTypeSupport<T> : DfFoldableTypeSupport<T> {
  protected abstract val folder: DfRecursiveExprFolder

  // we have to handle this case separately as we need just type, not the param value, thus we don't have to fold it
  protected fun processLitCall(expr: ScExpression): DfColumnType? {
    if (expr !is ScMethodCall) return null
    if (DfComputingUtil.callToName(expr) != LIT_NAME) return null

    val scalaType = DfComputingUtil.getParameterExpr(expr, LIT_PARAMETER)?.let { DfComputingUtil.toScalaType(it) } ?: return null
    val scalaMapper = DfScalaTypesMapper((expr as PsiElement).project)

    return scalaMapper.convertTo(scalaType)
  }

  companion object {
    private const val LIT_NAME = "lit"
    private const val LIT_PARAMETER = "literal"
  }
}