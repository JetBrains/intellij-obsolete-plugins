package com.intellij.dataWrangler.core.test.statistics.model

import com.intellij.dataWrangler.core.statistics.CoreStatisticsSettings.KEYWORD_FOR_MAPPING_OTHERS
import com.intellij.dataWrangler.core.statistics.model.dataFrameToHistogram
import com.intellij.dataWrangler.core.statistics.model.dataFrameToPercentage
import com.intellij.dataWrangler.core.statistics.model.dataFrameToUnique
import com.intellij.dataWrangler.core.statistics.model.quantile
import com.intellij.database.run.ui.table.statisticsPanel.types.ColumnVisualisationType
import org.jetbrains.kotlinx.dataframe.api.columnOf
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class CoreStatisticsDataTransformationUtilsTest {
  @Test
  fun `dataFrameToPercentage handles empty column`() {
    val column = columnOf() // Empty column
    val expected = null

    val result = dataFrameToPercentage(column)

    Assertions.assertEquals(expected, result)
  }

  @Test
  fun `dataFrameToPercentage handles column with few distinct values`() {
    val column = columnOf("A", "B", "A", "C", "A")
    val result = dataFrameToPercentage(column)
    Assertions.assertNotNull(result)
    result!!
    Assertions.assertTrue(result.percentageMap.keys.contains("A"))
    Assertions.assertTrue(result.percentageMap["A"] == listOf("60"))
    Assertions.assertTrue(result.percentageMap.keys.contains("B"))
    Assertions.assertTrue(result.percentageMap["B"] == listOf("20"))
    Assertions.assertTrue(result.percentageMap.keys.contains("C"))
    Assertions.assertTrue(result.percentageMap["C"] == listOf("20"))
  }

  @Test
  fun `dataFrameToPercentage handles column with many distinct values`() {
    val column = columnOf("A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L")
    val result = dataFrameToPercentage(column)

    Assertions.assertNotNull(result)
    result!!
    Assertions.assertEquals(ColumnVisualisationType.PERCENTAGE, result.visualisationType)
    Assertions.assertTrue(result.percentageMap.keys.contains(KEYWORD_FOR_MAPPING_OTHERS))
  }

  @Test
  fun `dataFrameToHistogram bins values correctly`() {
    val column = columnOf(1, 2, 3, 4, 5, 6)
    val result = dataFrameToHistogram(column)

    Assertions.assertNotNull(result)
    result!!
    Assertions.assertEquals(ColumnVisualisationType.HISTOGRAM, result.visualisationType)
    Assertions.assertTrue(result.data.barHeights == listOf(1.0, 1.0, 1.0, 1.0, 1.0, 1.0))
  }

  @Test
  fun `dataFrameToHistogram handles non-numeric data`() {
    val column = columnOf("A", "B", "C")
    val result = dataFrameToHistogram(column)

    // Should return an empty output as there are no numeric values
    Assertions.assertNull(result)
  }

  @Test
  fun `dataFrameToUnique calculates unique count`() {
    val column = columnOf("A", "B", "C", "A", "B")
    val result = dataFrameToUnique(column)

    Assertions.assertNotNull(result)
    Assertions.assertEquals(ColumnVisualisationType.UNIQUE, result.visualisationType)
    Assertions.assertEquals(3, result.numberOfUnique.toInt()) // 3 unique values
  }

  @Test
  fun `dataFrameToUnique works with 2 unique`() {
    val column = columnOf("A", "A", "C", "C", "C")
    val result = dataFrameToUnique(column)

    Assertions.assertNotNull(result)
    Assertions.assertEquals(ColumnVisualisationType.UNIQUE, result.visualisationType)
    Assertions.assertEquals(2, result.numberOfUnique.toInt()) // 2 unique values
  }

  @Test
  fun `dataFrameToUnique works with 1 unique`() {
    val column = columnOf("A")
    val result = dataFrameToUnique(column)

    Assertions.assertNotNull(result)
    Assertions.assertEquals(ColumnVisualisationType.UNIQUE, result.visualisationType)
    Assertions.assertEquals(1, result.numberOfUnique.toInt()) // 1 unique values
  }

  @Test
  fun `dataFrameToUnique works with nothing`() {
    val column = columnOf()
    val result = dataFrameToUnique(column)

    Assertions.assertNotNull(result)
    Assertions.assertEquals(ColumnVisualisationType.UNIQUE, result.visualisationType)
    Assertions.assertEquals(0, result.numberOfUnique.toInt()) // 0 unique values
  }

  @Test
  fun `quantile calculates multiple quantiles correctly`() {
    val column = columnOf(1.0, 2.0, 3.0, 4.0, 5.0, null)

    val quantiles = listOf(0.25, 0.5, 0.75)
    val results = column.quantile(quantiles) // Compute multiple quantiles

    Assertions.assertEquals(3, results.size) // There should be three results
    Assertions.assertEquals(2.0, results[0]) // 25th percentile
    Assertions.assertEquals(3.0, results[1]) // Median
    Assertions.assertEquals(4.0, results[2]) // 75th percentile
  }

  @Test
  fun `quantile throws exception for non-numeric column`() {
    val column = columnOf("A", "B", "C")

    val exception = Assertions.assertThrows(IllegalArgumentException::class.java) {
      column.quantile(listOf(0.5)) // Only valid for numeric columns
    }

    Assertions.assertTrue(exception.message!!.contains("Not supported"))
  }

  @Test
  fun `quantile throws exception for invalid quantile value`() {
    val column = columnOf(1.0, 2.0, 3.0)

    Assertions.assertThrows(IllegalArgumentException::class.java) {
      column.quantile(listOf(-0.1)) // Invalid quantile
    }

    Assertions.assertThrows(IllegalArgumentException::class.java) {
      column.quantile(listOf(1.1)) // Invalid quantile
    }
  }

  @Test
  fun `quantile handles single element column`() {
    val column = columnOf(42.0)

    val quantiles = listOf(0.0, 0.5, 1.0) // Minimum, median, and maximum
    val results = column.quantile(quantiles)

    Assertions.assertEquals(3, results.size)
    Assertions.assertEquals(42.0, results[0]) // Minimum
    Assertions.assertEquals(42.0, results[1]) // Median
    Assertions.assertEquals(42.0, results[2]) // Maximum
  }

  @Test
  fun `quantile handles two element column`() {
    val column = columnOf(10.0, 20.0)

    val quantiles = listOf(0.0, 0.25, 0.5, 0.75, 1.0) // Multiple quantiles
    val results = column.quantile(quantiles)

    Assertions.assertEquals(10.0, results[0]) // Minimum
    Assertions.assertEquals(12.5, results[1]) // Interpolated
    Assertions.assertEquals(15.0, results[2]) // Median
    Assertions.assertEquals(17.5, results[3]) // Interpolated
    Assertions.assertEquals(20.0, results[4]) // Maximum
  }

  @Test
  fun `quantile handles small column (3 elements)`() {
    val column = columnOf(1.0, 2.0, 3.0)

    val quantiles = listOf(0.0, 0.25, 0.5, 0.75, 1.0) // Multiple quantiles
    val results = column.quantile(quantiles)

    Assertions.assertEquals(1.0, results[0]) // Minimum
    Assertions.assertEquals(1.5, results[1]) // Interpolated 25th percentile
    Assertions.assertEquals(2.0, results[2]) // Median
    Assertions.assertEquals(2.5, results[3]) // Interpolated 75th percentile
    Assertions.assertEquals(3.0, results[4]) // Maximum
  }

  @Test
  fun `quantile handles sorted and unsorted input`() {
    val sortedColumn = columnOf(1.0, 2.0, 3.0, 4.0, 5.0)
    val unsortedColumn = columnOf(3.0, 1.0, 5.0, 2.0, 4.0)
    val quantiles = listOf(0.0, 0.5, 1.0) // Min, Median, Max

    val resultsSorted = sortedColumn.quantile(quantiles)
    val resultsUnsorted = unsortedColumn.quantile(quantiles)

    Assertions.assertEquals(resultsSorted, resultsUnsorted)
    Assertions.assertEquals(1.0, resultsSorted[0]) // Minimum
    Assertions.assertEquals(3.0, resultsSorted[1]) // Median
    Assertions.assertEquals(5.0, resultsSorted[2]) // Maximum
  }

  @Test
  fun `quantile handles input with null values`() {
    val columnWithNulls = columnOf(1.0, 2.0, 3.0, null, 5.0, null)
    val quantiles = listOf(0.5) // Median

    val results = columnWithNulls.quantile(quantiles)

    Assertions.assertEquals(1, results.size)
    Assertions.assertEquals(2.5, results[0]) // Median of non-null values: [1.0, 2.0, 3.0, 5.0]
  }

  @Test
  fun `quantile handles input with all null values`() {
    val columnWithAllNulls = columnOf<Double?>(null, null, null)

    val exception = Assertions.assertThrows(IllegalArgumentException::class.java) {
      columnWithAllNulls.quantile(listOf(0.5))
    }

    Assertions.assertTrue(exception.message!!.contains("Not supported"))
  }
}