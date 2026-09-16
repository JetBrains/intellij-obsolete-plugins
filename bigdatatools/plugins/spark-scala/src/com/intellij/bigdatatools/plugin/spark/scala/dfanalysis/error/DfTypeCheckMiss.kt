package com.intellij.bigdatatools.plugin.spark.scala.dfanalysis.error

import com.intellij.bigdatatools.plugin.spark.assistance.statistic.SparkDataFrameInspection
import com.intellij.bigdatatools.plugin.spark.assistance.statistic.SparkStatisticScala
import com.intellij.bigdatatools.plugin.spark.assistance.util.SAMessagesBundle
import org.jetbrains.plugins.scala.lang.psi.api.expr.ScExpression

data class DfTypeCheckMiss(val place: String,
                           val name: String,
                           override val problemPlaceName: String?,
                           override val problemPlaceIndex: Int? = null) : DfTypeCheckError() {

  override fun logUsage(expr: ScExpression) {
    SparkStatisticScala.logInspection(expr, name, SparkDataFrameInspection.NON_EXISTING_COLUMN)
  }

  override fun errorMessage(): String = SAMessagesBundle.message("dataframe.inspection.message.column.not.exist", name)

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as DfTypeCheckMiss

    if (place != other.place) return false
    if (name != other.name) return false
    if (problemPlaceName != other.problemPlaceName) return false

    return true
  }

  override fun hashCode(): Int {
    var result = place.hashCode()
    result = 31 * result + name.hashCode()
    result = 31 * result + (problemPlaceName?.hashCode() ?: 0)
    return result
  }
}