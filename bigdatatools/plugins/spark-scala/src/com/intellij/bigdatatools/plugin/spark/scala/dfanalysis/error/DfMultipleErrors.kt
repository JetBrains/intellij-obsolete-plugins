package com.intellij.bigdatatools.plugin.spark.scala.dfanalysis.error

import com.intellij.codeInsight.daemon.impl.HighlightInfo
import org.jetbrains.plugins.scala.lang.psi.api.expr.ScExpression

data class DfMultipleErrors(val errors: Collection<DfTypeCheckError>) : DfTypeCheckError() {
  override val problemPlaceName: String? = null

  override val problemPlaceIndex: Int? = null

  override fun errorMessage(): String = errors.joinToString(separator = "\n") { it.errorMessage() }

  override fun logUsage(expr: ScExpression) {
    errors.forEach { it.logUsage(expr) }
  }

  override fun produceHighlighting(expr: ScExpression): List<HighlightInfo> = errors.flatMap { it.produceHighlighting(expr) }
}