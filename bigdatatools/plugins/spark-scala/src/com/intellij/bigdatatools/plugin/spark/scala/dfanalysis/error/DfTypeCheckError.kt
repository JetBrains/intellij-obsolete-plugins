package com.intellij.bigdatatools.plugin.spark.scala.dfanalysis.error

import com.intellij.bigdatatools.plugin.spark.scala.dfanalysis.impl.DfComputingUtil
import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.codeInsight.daemon.impl.HighlightInfoType
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import org.jetbrains.annotations.Nls
import org.jetbrains.plugins.scala.lang.psi.api.expr.ScExpression
import org.jetbrains.plugins.scala.lang.psi.api.expr.ScMethodCall

fun Collection<DfTypeCheckError>.getError(): DfTypeCheckError? {
  val distinctList = distinct()
  if (distinctList.isEmpty()) return null
  if (distinctList.size == 1) return first()
  return DfMultipleErrors(distinctList)
}

abstract class DfTypeCheckError {
  abstract val problemPlaceName: String?

  abstract val problemPlaceIndex: Int?

  abstract fun errorMessage(): @Nls String

  open fun suppressable(): Boolean = true

  open fun logUsage(expr: ScExpression) {}

  open fun produceHighlighting(expr: ScExpression): List<HighlightInfo> =
    listOfNotNull(
      HighlightInfo.newHighlightInfo(defaultSeverity()).descriptionAndTooltip(errorMessage()).range(calculateTextRange(expr)).create()
    )

  protected open fun defaultSeverity(): HighlightInfoType = HighlightInfoType.WARNING

  protected open fun calculateTextRange(expr: ScExpression): TextRange =
    (if (problemPlaceName != null) {
      val paramPsi = if (expr is ScMethodCall)
        placeToExpr(expr)
      else if ((expr as PsiElement).parent is ScMethodCall)
        placeToExpr((expr as PsiElement).parent as ScExpression)
      else
        null

      (paramPsi as? PsiElement)?.textRange
    }
    else null) ?: DfComputingUtil.psiElementToPlaceRange(expr as PsiElement)

  private fun placeToExpr(expr: ScExpression): ScExpression? {
    if (problemPlaceIndex == null) return DfComputingUtil.getParameterExpr(expr, problemPlaceName!!)
    val list = DfComputingUtil.groupByParameters(expr, setOf(problemPlaceName!!))[problemPlaceName!!]

    return DfComputingUtil.getVarargParam(list, problemPlaceIndex!!)
  }
}