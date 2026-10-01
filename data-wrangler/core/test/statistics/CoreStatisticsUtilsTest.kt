package com.intellij.dataWrangler.core.test.statistics

import com.intellij.dataWrangler.core.statistics.HistogramUnit
import com.intellij.dataWrangler.core.statistics.computeHistogram
import com.intellij.dataWrangler.core.statistics.formatDouble
import com.intellij.dataWrangler.core.statistics.getValuesNeededFormat
import com.intellij.dataWrangler.core.statistics.getVisualizationType
import com.intellij.dataWrangler.core.statistics.kTypeToPythonDataFrame
import com.intellij.database.run.ui.table.statisticsPanel.types.ColumnVisualisationType
import org.jetbrains.kotlinx.dataframe.api.columnOf
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import kotlin.reflect.full.createType

class CoreStatisticsUtilsTest {
  @Test
  fun `getVisualizationType returns HISTOGRAM for numeric columns`() {
    val column = columnOf(1, 2, 3, 4) // Int column
    val visualizationType = getVisualizationType(column)
    Assertions.assertEquals(ColumnVisualisationType.HISTOGRAM, visualizationType)
  }

  @Test
  fun `getVisualizationType returns PERCENTAGE for columns with low distinct count`() {
    val column = columnOf("A", "A", "B", "B", "C")
    val visualizationType = getVisualizationType(column)
    Assertions.assertEquals(ColumnVisualisationType.PERCENTAGE, visualizationType)
  }

  @Test
  fun `getVisualizationType returns UNIQUE for columns with high distinct count`() {
    val column = columnOf("a", "b", "c", "d", "e", "f", "g", "h", "i", "j")
    val visualizationType = getVisualizationType(column)
    Assertions.assertEquals(ColumnVisualisationType.UNIQUE, visualizationType)
  }

  @Test
  fun `formatDouble returns NaN for null input`() {
    val input: Double? = null
    val expected = "NaN"
    val output = formatDouble(input)
    Assertions.assertEquals(expected, output)
  }

  @Test
  fun `computeHistogram returns correct number of bins`() {
    val data = listOf(1.0, 2.0, 3.0, 4.0, 5.0, 6.0)
    val nBins = 3
    val histogram = computeHistogram(data, nBins)

    Assertions.assertEquals(nBins, histogram.size)
  }

  @Test
  fun `computeHistogram returns correct counts for bins`() {
    val data = listOf(1.0, 2.0, 2.5, 3.0, 4.0)
    val nBins = 2

    val expectedHistogram = listOf(
      HistogramUnit(Pair(1.0, 2.5), 2), // First bin: 1, 2
      HistogramUnit(Pair(2.5, 4.0), 3), // Second bin: 2.5, 3, 4
    )

    val histogram = computeHistogram(data, nBins)

    Assertions.assertEquals(expectedHistogram, histogram)
  }

  @Test
  fun `computeHistogram returns empty list for empty list`() {
    val data = emptyList<Double>()
    val nBins = 2

    val expectedHistogram = emptyList<HistogramUnit>()

    val histogram = computeHistogram(data, nBins)

    Assertions.assertEquals(expectedHistogram, histogram)
  }

  @Test
  fun `kTypeToPythonDataFrame maps int type to int64`() {
    val type = Int::class.createType()
    val result = kTypeToPythonDataFrame(type)
    Assertions.assertEquals("int64", result)
  }

  @Test
  fun `kTypeToPythonDataFrame maps long type to int64`() {
    val type = Long::class.createType()
    val result = kTypeToPythonDataFrame(type)
    Assertions.assertEquals("int64", result)
  }

  @Test
  fun `kTypeToPythonDataFrame maps float type to float64`() {
    val type = Float::class.createType()
    val result = kTypeToPythonDataFrame(type)
    Assertions.assertEquals("float64", result)
  }

  @Test
  fun `kTypeToPythonDataFrame maps double type to float64`() {
    val type = Double::class.createType()
    val result = kTypeToPythonDataFrame(type)
    Assertions.assertEquals("float64", result)
  }

  @Test
  fun `kTypeToPythonDataFrame maps string type to object`() {
    val type = String::class.createType()
    val result = kTypeToPythonDataFrame(type)
    Assertions.assertEquals("object", result)
  }

  @Test
  fun `kTypeToPythonDataFrame maps boolean type to bool`() {
    val type = Boolean::class.createType()
    val result = kTypeToPythonDataFrame(type)
    Assertions.assertEquals("bool", result)
  }

  @Test
  fun `kTypeToPythonDataFrame returns unknown for null types`() {
    val result = kTypeToPythonDataFrame(Any::class.createType())
    Assertions.assertEquals("unknown", result)
  }

  @Test
  fun `getValuesNeededFormat handles zeros appropriately`() {
    val type = "testType"
    val xList = listOf("A", "B", "C", "D")
    val yList = listOf(0, 20, 0, 10)

    val result = getValuesNeededFormat(type, xList, yList)

    // Validate x and labels
    Assertions.assertEquals(xList, result.xLabel)
    Assertions.assertEquals(yList, result.yLabel)

    // Validate normalized y values (ensuring zero height is adjusted)
    val normalizedY = result.barHeights
    Assertions.assertEquals(4, normalizedY.size)
    Assertions.assertTrue(normalizedY[0] == 0.05) // Adjusted from 0 to 0.05
    Assertions.assertTrue(normalizedY[2] == 0.05) // Adjusted from 0 to 0.05
  }
}