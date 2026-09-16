package com.intellij.bigdatatools.plugin.spark.scala.dfanalysis.error

import com.intellij.bigdatatools.plugin.spark.scala.SparkScalaMessagesBundle
import com.intellij.bigdatatools.plugin.spark.assistance.dfanalysis.DfColumnType
import com.intellij.bigdatatools.plugin.spark.assistance.statistic.SparkDataFrameInspection
import com.intellij.bigdatatools.plugin.spark.assistance.statistic.SparkStatisticScala
import com.intellij.bigdatatools.plugin.spark.scala.dfanalysis.impl.DfComputingUtil
import org.jetbrains.plugins.scala.lang.psi.api.expr.ScExpression

data class DfTypeCheckConflict(val name: String,
                               val prevPlace: String,
                               val prevType: DfColumnType,
                               val currentPlace: String,
                               val currentType: DfColumnType,
                               override val problemPlaceName: String?,
                               override val problemPlaceIndex: Int? = null) : DfTypeCheckError() {

  override fun logUsage(expr: ScExpression) {
    SparkStatisticScala.logInspection(expr, name, SparkDataFrameInspection.CONFLICT)
  }

  override fun errorMessage(): String =
    SparkScalaMessagesBundle.message("df.type.check.error.incompatible.types",
                                        name,
                                        DfComputingUtil.trimTextForDescription(prevPlace),
                                        prevType.name,
                                        DfComputingUtil.trimTextForDescription(currentPlace),
                                        currentType.name)

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as DfTypeCheckConflict

    if (name != other.name) return false
    if (prevPlace != other.prevPlace) return false
    if (prevType != other.prevType) return false
    if (currentPlace != other.currentPlace) return false
    if (currentType != other.currentType) return false
    if (problemPlaceName != other.problemPlaceName) return false

    return true
  }

  override fun hashCode(): Int {
    var result = name.hashCode()
    result = 31 * result + prevPlace.hashCode()
    result = 31 * result + prevType.hashCode()
    result = 31 * result + currentPlace.hashCode()
    result = 31 * result + currentType.hashCode()
    result = 31 * result + (problemPlaceName?.hashCode() ?: 0)
    return result
  }
}