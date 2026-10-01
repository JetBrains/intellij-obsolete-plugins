package com.intellij.dataWrangler.core.statistics.model

import com.intellij.dataWrangler.core.statistics.CoreStatisticsSettings.KEYWORD_FOR_MAPPING_OTHERS
import com.intellij.dataWrangler.core.statistics.CoreStatisticsSettings.MAX_NUM_BINS
import com.intellij.dataWrangler.core.statistics.CoreStatisticsSettings.MAX_UNIQUE_VALUES_TO_SHOW_IN_VIS
import com.intellij.dataWrangler.core.statistics.computeHistogram
import com.intellij.dataWrangler.core.statistics.formatDouble
import com.intellij.dataWrangler.core.statistics.getValuesNeededFormat
import com.intellij.dataWrangler.core.statistics.kTypeToPythonDataFrame
import com.intellij.database.run.ui.table.statisticsPanel.types.ColumnVisualizationDataHistogram
import com.intellij.database.run.ui.table.statisticsPanel.types.ColumnVisualizationDataPercentage
import com.intellij.database.run.ui.table.statisticsPanel.types.ColumnVisualizationDataUnique
import com.intellij.grid.charts.impl.StatisticsPanelRenderer.Companion.getAxisXLabels
import com.intellij.grid.charts.impl.StatisticsPanelRenderer.Companion.getTooltips
import org.jetbrains.kotlinx.dataframe.DataColumn
import org.jetbrains.kotlinx.dataframe.api.convertTo
import org.jetbrains.kotlinx.dataframe.api.count
import org.jetbrains.kotlinx.dataframe.api.filter
import org.jetbrains.kotlinx.dataframe.api.getRows
import org.jetbrains.kotlinx.dataframe.api.rows
import org.jetbrains.kotlinx.dataframe.api.sum
import org.jetbrains.kotlinx.dataframe.api.valueCounts
import org.jetbrains.kotlinx.dataframe.type
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.isSuperclassOf

/**
 * Prepares data for creating percentage visualizations from dataframe.
 */
internal fun dataFrameToPercentage(column: DataColumn<*>): ColumnVisualizationDataPercentage? {
  val totalCount = column.count()
  val valueCounts = column.valueCounts(sort = true, dropNA = false)
  val uniqueCount = valueCounts.rowsCount()
  if (uniqueCount == 0) return null // if only 3 values, show all of them

  // 1 or 2 value case
  val data = valueCounts.getRows(0..<uniqueCount).rows().associate {
    it[0].toString() to listOf(formatDouble(it.count.toDouble() / totalCount.toDouble() * 100))
  } + (KEYWORD_FOR_MAPPING_OTHERS to listOf("0"))

  if (uniqueCount <= MAX_UNIQUE_VALUES_TO_SHOW_IN_VIS) return ColumnVisualizationDataPercentage(data)

  // general case
  val percentages = valueCounts.getRows(0..1)
  val rest = valueCounts.getRows(2..<valueCounts.rowsCount())
  val map: Map<String, List<String>> = percentages.rows().associate {
    it[0].toString() to listOf(formatDouble(it.count.toDouble() / totalCount.toDouble() * 100))
  } + (KEYWORD_FOR_MAPPING_OTHERS to listOf(formatDouble(rest.sum { count }.toDouble() / totalCount.toDouble() * 100)))

  return ColumnVisualizationDataPercentage(  // Assuming Pie Chart for percentage visualization
    map)
}

/**
 * Prepares data for creating histogram visualizations from dataframe.
 * This is a very primitive histogram, has to be updated in the future
 */
internal fun dataFrameToHistogram(df: DataColumn<*>): ColumnVisualizationDataHistogram? {
  val properValues = df.values().filterIsInstance<Number>().map { item -> item.toDouble() }
  if (properValues.isEmpty())
    return null
  val numberOfBins = df.valueCounts(sort = false, dropNA = true).count()
  val data = computeHistogram(properValues, numberOfBins.coerceAtMost(MAX_NUM_BINS))

  val xList = data.map { "${formatDouble(it.interval.first)} — ${formatDouble(it.interval.second)}" }
  val yList = data.map { it.count }

  val dfTypeString = kTypeToPythonDataFrame(df.type)
  val histogramValues = getValuesNeededFormat(dfTypeString, xList, yList.map { it })
  val histogramTooltips = getTooltips(dfTypeString)
  val axisXLabels = getAxisXLabels(xList, dfTypeString)

  return ColumnVisualizationDataHistogram(histogramValues, histogramTooltips, axisXLabels)
}

/**
 * Prepares data for creating unique values visualizations from dataframe.
 */
internal fun dataFrameToUnique(column: DataColumn<*>): ColumnVisualizationDataUnique {
  val numberOfUnique = column.valueCounts(sort = false).count()
  return ColumnVisualizationDataUnique(numberOfUnique.toString())
}

/**
 * Custom implemented quantile. Quantile is not yet implemented in Kotlin DataFrame.
 * [https://github.com/Kotlin/dataframe/issues/543](https://github.com/Kotlin/dataframe/issues/543)
 */
internal fun DataColumn<*>.quantile(quantiles: List<Double>): List<Double> {
  require(quantiles.all { it in 0.0..1.0 }) { "All quantiles must be between 0.0 and 1.0" }

  val data = if (type.isSubTypeOfNumber()) {
    this.filter { it != null }.convertTo<Double>().values().toList()
  }
  else {
    throw IllegalArgumentException("Not supported: {$type.classifier}")
  }

  if (data.isEmpty()) return emptyList()

  val sortedData = data.sorted()
  val n = sortedData.size

  // Compute quantile values for each quantile in the input list
  return quantiles.map { quantile ->
    val index = quantile * (n - 1)

    val lowerIndex = index.toInt() // The integer part of the index
    val upperIndex = (lowerIndex + 1).coerceAtMost(n - 1)
    val fraction = index - lowerIndex // The fractional part

    sortedData[lowerIndex] + fraction * (sortedData[upperIndex] - sortedData[lowerIndex])
  }
}

internal fun KType.isSubTypeOfNumber(): Boolean {
  return Number::class.isSuperclassOf(this.classifier as KClass<*>)
}