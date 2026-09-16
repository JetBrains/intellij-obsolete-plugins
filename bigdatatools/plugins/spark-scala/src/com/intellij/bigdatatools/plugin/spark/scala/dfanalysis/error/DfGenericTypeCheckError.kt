package com.intellij.bigdatatools.plugin.spark.scala.dfanalysis.error

import com.intellij.bigdatatools.plugin.spark.assistance.statistic.SparkDataFrameInspection
import com.intellij.bigdatatools.plugin.spark.assistance.statistic.SparkStatisticScala
import org.jetbrains.plugins.scala.lang.psi.api.expr.ScExpression

data class DfGenericTypeCheckError(override val problemPlaceName: String?,
                                   private val errorMessage: String,
                                   override val problemPlaceIndex: Int? = null) : DfTypeCheckError() {

  override fun logUsage(expr: ScExpression) {
    SparkStatisticScala.logInspection(expr, null, SparkDataFrameInspection.OTHER)
  }

  override fun errorMessage(): String = errorMessage

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as DfGenericTypeCheckError

    if (problemPlaceName != other.problemPlaceName) return false
    if (errorMessage != other.errorMessage) return false

    return true
  }

  override fun hashCode(): Int {
    var result = problemPlaceName?.hashCode() ?: 0
    result = 31 * result + errorMessage.hashCode()
    return result
  }
}