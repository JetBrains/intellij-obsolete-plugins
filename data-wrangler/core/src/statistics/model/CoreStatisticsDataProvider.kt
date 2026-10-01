package com.intellij.dataWrangler.core.statistics.model

import com.intellij.dataWrangler.core.CoreDataWranglerBundle
import com.intellij.dataWrangler.core.statistics.formatDouble
import com.intellij.dataWrangler.core.statistics.getVisualizationType
import com.intellij.database.datagrid.DataGrid
import com.intellij.database.datagrid.GridColumn
import com.intellij.database.datagrid.GridModel
import com.intellij.database.datagrid.GridRow
import com.intellij.database.run.ui.table.statisticsPanel.types.ColumnDescriptionStatistics
import com.intellij.database.run.ui.table.statisticsPanel.types.ColumnVisualisationType
import com.intellij.database.run.ui.table.statisticsPanel.types.ColumnVisualizationData
import com.intellij.database.run.ui.table.statisticsPanel.types.StatisticsDescriptionUnit
import com.intellij.util.containers.addIfNotNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import org.jetbrains.kotlinx.dataframe.AnyFrame
import org.jetbrains.kotlinx.dataframe.DataColumn
import org.jetbrains.kotlinx.dataframe.DataFrame
import org.jetbrains.kotlinx.dataframe.api.ColumnDescription
import org.jetbrains.kotlinx.dataframe.api.count
import org.jetbrains.kotlinx.dataframe.api.dataFrameOf
import org.jetbrains.kotlinx.dataframe.api.describe
import org.jetbrains.kotlinx.dataframe.api.emptyDataFrame
import org.jetbrains.kotlinx.dataframe.api.max
import org.jetbrains.kotlinx.dataframe.api.mean
import org.jetbrains.kotlinx.dataframe.api.median
import org.jetbrains.kotlinx.dataframe.api.min
import org.jetbrains.kotlinx.dataframe.api.parse
import org.jetbrains.kotlinx.dataframe.api.std
import org.jetbrains.kotlinx.dataframe.api.valueCounts
import org.jetbrains.kotlinx.dataframe.type

/**
 * Statistics for each column in the dataframe like the output of `df.describe()`
 */
class CoreStatisticsHeaderDataProvider(private val grid: DataGrid) {
  var dataFrame: AnyFrame = gridToDataFrame(grid.dataHookup.dataModel)

  suspend fun gridChanged() {
    withContext(Dispatchers.Default) {
      dataFrame = async { gridToDataFrame(grid.dataHookup.dataModel) }.await()
    }
  }

  fun gridToDataFrame(gridModel: GridModel<GridRow, GridColumn>): AnyFrame {
    val cols = gridModel.columns.map { gridCol -> gridCol.name }
    if (cols.isEmpty()) return emptyDataFrame<Any>()
    val rows = gridModel.rows.map { gridRow -> gridRow.toList() }
    if (rows.isEmpty()) return emptyDataFrame<Any>()

    val df = dataFrameOf(cols, rows.flatten()).parse()
    return df
  }

  /**
   * Generates a list of visualization data for all columns.
   * The condition for which visualization to use is decided [getVisualizationType]
   */
  fun generateVisualization(): List<ColumnVisualizationData> {
    val df = dataFrame
    val visuals: MutableList<ColumnVisualizationData> = mutableListOf()

    df.columns().forEach { col ->
      when (getVisualizationType(col)) {
        ColumnVisualisationType.HISTOGRAM -> visuals.addIfNotNull(dataFrameToHistogram(col))
        ColumnVisualisationType.UNIQUE -> visuals.add(dataFrameToUnique(col))
        ColumnVisualisationType.PERCENTAGE -> visuals.addIfNotNull(dataFrameToPercentage(col))
      }
    }
    return visuals
  }

  fun generateStatistics(): List<ColumnDescriptionStatistics> {
    val df = dataFrame
    return df.columns().map { col ->
      val statistics = mutableListOf<StatisticsDescriptionUnit>()

      // General statistics
      statistics.add(generateMissingCountStatistics(col))
      statistics.add(generateTotalCountStatistics(col))

      if (col.type.isSubTypeOfNumber()) {
        statistics.addAll(generateNumericStatistics(col))
      }
      else {
        statistics.addAll(generateDistinctValueStatistics(col))
      }

      ColumnDescriptionStatistics(statistics)
    }
  }

  // General statistics
  private fun generateMissingCountStatistics(col: DataColumn<*>): StatisticsDescriptionUnit {
    val missingCount = col.count { it == null }.toDouble()
    return StatisticsDescriptionUnit(
      CoreDataWranglerBundle.message("table.tooltip.statistics.missing"),
      formatDouble(missingCount)
    )
  }

  private fun generateTotalCountStatistics(col: DataColumn<*>): StatisticsDescriptionUnit {
    val totalCount = col.count().toDouble()
    return StatisticsDescriptionUnit(
      CoreDataWranglerBundle.message("table.tooltip.statistics.count"),
      formatDouble(totalCount)
    )
  }

  // Numeric statistics
  private fun generateNumericStatistics(col: DataColumn<*>): List<StatisticsDescriptionUnit> {
    val colDescription = col.describe()
    val statistics = mutableListOf<StatisticsDescriptionUnit>()

    // Mean
    statistics.add(
      StatisticsDescriptionUnit(
        CoreDataWranglerBundle.message("table.tooltip.statistics.mean"),
        formatDouble(colDescription.mean.values().firstOrNull()?.toDouble())
      )
    )

    // Standard Deviation
    statistics.add(
      StatisticsDescriptionUnit(
        CoreDataWranglerBundle.message("table.tooltip.statistics.stdDeviation"),
        formatDouble(colDescription.std.values().firstOrNull()?.toDouble())
      )
    )

    statistics.add(generateMinValueStatistics(colDescription))

    // Percentiles
    val percentageValues = col.quantile(listOf(0.05, 0.25, 0.75, 0.95))
    statistics.add(StatisticsDescriptionUnit(CoreDataWranglerBundle.message("table.tooltip.statistics.percentile5"),
                                             formatDouble(percentageValues.getOrNull(0))))
    statistics.add(StatisticsDescriptionUnit(CoreDataWranglerBundle.message("table.tooltip.statistics.percentile25"),
                                             formatDouble(percentageValues.getOrNull(1))))

    statistics.add(generateMedianStatistics(colDescription))

    // Percentile (75% and 95%)
    statistics.add(StatisticsDescriptionUnit(CoreDataWranglerBundle.message("table.tooltip.statistics.percentile75"),
                                             formatDouble(percentageValues.getOrNull(2))))
    statistics.add(StatisticsDescriptionUnit(CoreDataWranglerBundle.message("table.tooltip.statistics.percentile95"),
                                             formatDouble(percentageValues.getOrNull(3))))

    statistics.add(generateMaxValueStatistics(colDescription))

    return statistics
  }

  private fun generateMinValueStatistics(
    colDescription: DataFrame<ColumnDescription>,
  ): StatisticsDescriptionUnit {
    val minValue = colDescription.min.values().filterIsInstance<Number>().firstOrNull()?.toDouble()
    return StatisticsDescriptionUnit(
      CoreDataWranglerBundle.message("table.tooltip.statistics.min"),
      formatDouble(minValue)
    )
  }

  private fun generateMedianStatistics(
    colDescription: DataFrame<ColumnDescription>,
  ): StatisticsDescriptionUnit {
    val medianValue = colDescription.median.values().filterIsInstance<Number>().firstOrNull()?.toDouble()
    return StatisticsDescriptionUnit(
      CoreDataWranglerBundle.message("table.tooltip.statistics.median"),
      formatDouble(medianValue)
    )
  }

  private fun generateMaxValueStatistics(
    colDescription: DataFrame<ColumnDescription>,
  ): StatisticsDescriptionUnit {
    val maxValue = colDescription.max.values().filterIsInstance<Number>().firstOrNull()?.toDouble()
    return StatisticsDescriptionUnit(
      CoreDataWranglerBundle.message("table.tooltip.statistics.max"),
      formatDouble(maxValue)
    )
  }

  // Distinct value statistics for non-numeric columns
  private fun generateDistinctValueStatistics(col: DataColumn<*>): List<StatisticsDescriptionUnit> {
    val statistics = mutableListOf<StatisticsDescriptionUnit>()

    val uniqueCount = col.countDistinct().toDouble()
    statistics.add(
      StatisticsDescriptionUnit(
        CoreDataWranglerBundle.message("table.tooltip.statistics.unique"),
        formatDouble(uniqueCount)
      )
    )

    // Top Value and Frequency
    statistics.addAll(generateTopValueStatistics(col))

    return statistics
  }

  private fun generateTopValueStatistics(col: DataColumn<*>): List<StatisticsDescriptionUnit> {
    val statistics = mutableListOf<StatisticsDescriptionUnit>()

    val topValueCounts = col.valueCounts(sort = true, ascending = false, dropNA = false)[0]
    val topValueName = topValueCounts[0].toString()
    val topValueCount = topValueCounts.count.toDouble()

    // Top Value
    statistics.add(
      StatisticsDescriptionUnit(
        CoreDataWranglerBundle.message("table.tooltip.statistics.top"),
        topValueName
      )
    )

    // Frequency of Top Value
    statistics.add(
      StatisticsDescriptionUnit(
        CoreDataWranglerBundle.message("table.tooltip.statistics.freq"),
        formatDouble(topValueCount)
      )
    )

    return statistics
  }
}