package com.intellij.bigdatatools.plugin.spark.scala.dfanalysis.error

import com.intellij.bigdatatools.plugin.spark.scala.SparkScalaMessagesBundle
import com.intellij.bigdatatools.plugin.spark.assistance.dfanalysis.DfColumnType
import com.intellij.bigdatatools.plugin.spark.assistance.statistic.SparkDataFrameInspection
import com.intellij.bigdatatools.plugin.spark.assistance.statistic.SparkStatisticScala
import com.intellij.codeInsight.daemon.impl.HighlightInfoType
import com.intellij.openapi.util.TextRange
import org.jetbrains.plugins.scala.lang.psi.api.expr.ScExpression

data class DfTypeOpWeakError(val first: DfColumnType,
                             val second: DfColumnType?,
                             val op: String,
                             val range: TextRange) : DfTypeCheckError() {
  override val problemPlaceName: String? = null
  override val problemPlaceIndex: Int? = null

  override fun logUsage(expr: ScExpression) {
    SparkStatisticScala.logInspection(expr, null, SparkDataFrameInspection.SUSPICIOUS_CAST)
  }

  override fun errorMessage(): String =
    if (second != null)
      SparkScalaMessagesBundle.message("df.non.casted.types.weak.warning", first.name, op, second.name)
    else
      SparkScalaMessagesBundle.message("df.non.casted.one.type.weak.warning", op, first.name)

  override fun defaultSeverity(): HighlightInfoType = HighlightInfoType.WEAK_WARNING

  override fun calculateTextRange(expr: ScExpression): TextRange = range

  override fun suppressable(): Boolean = false
}