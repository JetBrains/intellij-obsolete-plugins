package com.intellij.dataWrangler.core.statistics

import com.intellij.dataWrangler.core.statistics.CoreStatisticsSettings.MAX_NUM_BINS
import com.intellij.dataWrangler.core.statistics.CoreStatisticsSettings.MAX_UNIQUE_VALUES_TO_SHOW_IN_VIS
import com.intellij.dataWrangler.core.statistics.CoreStatisticsSettings.UNIQUE_VALUES_PERCENT
import com.intellij.dataWrangler.core.statistics.model.isSubTypeOfNumber
import com.intellij.database.run.ui.table.statisticsPanel.types.ColumnVisualisationType
import com.intellij.database.run.ui.table.statisticsPanel.types.HistogramData
import org.jetbrains.kotlinx.dataframe.DataColumn
import org.jetbrains.kotlinx.dataframe.api.count
import org.jetbrains.kotlinx.dataframe.type
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.KType

/**
 * Decide to render HISTOGRAM vs. PERCENTAGE vs. UNIQUE.
 *
 * Find more about it: pydevd_pandas.py
 */
internal fun getVisualizationType(col: DataColumn<*>): ColumnVisualisationType {
  if (col.type.isSubTypeOfNumber())
    return ColumnVisualisationType.HISTOGRAM

  val totalCount = col.count().toDouble()
  val totalDistinct = col.countDistinct().toDouble()
  val ratioUnique = totalDistinct / totalCount
  if (totalDistinct <= MAX_UNIQUE_VALUES_TO_SHOW_IN_VIS || (ratioUnique * 100) <= UNIQUE_VALUES_PERCENT)
    return ColumnVisualisationType.PERCENTAGE

  return ColumnVisualisationType.UNIQUE
}

/**
 * Converts double to string such that
 * 1. If it has no fractional part, have the int to string
 * 2. If it does have fractional, round to 1 decimal and to string.
 */
internal fun formatDouble(x: Double?): String {
  if (x == null) return "NaN"
  return if (x.rem(1).equals(0.0))
    x.toInt().toString()
  else
    String.format(Locale.US, "%.1f", x)
}

/**
 * Helper data class to create computeHistogram
 */
internal data class HistogramUnit(val interval: Pair<Double, Double>, val count: Int)

internal fun computeHistogram(data: List<Double>, nBins: Int = MAX_NUM_BINS): List<HistogramUnit> {
  if (data.isEmpty()) return emptyList<HistogramUnit>()
  // Changes only if binWidth is 0
  val maxValue = data.max()
  val minValue = data.min()
  val binWidth = (maxValue - minValue) / nBins

  val histogram = IntArray(nBins)

  data.forEach { point ->
    val bin = ((point - minValue) / binWidth).toInt().coerceIn(0, nBins - 1)
    histogram[bin]++
  }

  val list = mutableListOf<HistogramUnit>()
  for (i in 0 until nBins) {
    list.add(HistogramUnit(Pair(minValue + binWidth * i, minValue + binWidth * (i + 1)), histogram[i]))
  }

  return list
}

/**
 * Kotlin dataframe type to python types
 */
internal fun kTypeToPythonDataFrame(kType: KType): String {
  // Mapping of Kotlin KTypes to Python pandas DataFrame types
  val typeMapping = mapOf<KClass<*>, String>(
    Int::class to "int64",
    Long::class to "int64",
    Float::class to "float64",
    Double::class to "float64",
    String::class to "object",
    Boolean::class to "bool",
  )

  val classifier = kType.classifier as? KClass<*>
  return typeMapping[classifier] ?: "unknown"
}

/**
 * Prepare values for rendering
 */
internal fun getValuesNeededFormat(type: String, xList: List<String>, yList: List<Int>): HistogramData {
  fun normalizeList(listOfBarHeights: List<Int>): List<Double> {
    val maxValuesCountInBars = listOfBarHeights.max()
    return listOfBarHeights.map { it.toDouble() / maxValuesCountInBars }
  }

  // normalize values for better visualization.
  val barHeights = normalizeList(yList).map {
    when { // columns with zero height are shown as a small rectangle, not an empty space
      it == 0.0 -> 0.05
      it < 0.08 -> 0.08
      else -> it
    }
  }

  val data = HistogramData(barHeights,xList,yList )
  // bool for pandas, boolean for polars
  if (type.startsWith("bool")) {
    val sum = yList.sum()
    val percentageList = yList.map { (it * 100.0 / sum).toInt() }
    data.percentageList = percentageList
  }
  return data
}