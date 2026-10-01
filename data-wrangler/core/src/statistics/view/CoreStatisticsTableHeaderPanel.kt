package com.intellij.dataWrangler.core.statistics.view

import com.intellij.dataWrangler.core.CoreDataWranglerBundle
import com.intellij.dataWrangler.core.statistics.CoreStatisticsSettings.KEYWORD_FOR_MAPPING_OTHERS
import com.intellij.dataWrangler.core.statistics.model.CoreStatisticsHeaderDataProvider
import com.intellij.database.run.ui.table.TableResultView
import com.intellij.database.run.ui.table.statisticsPanel.HistogramSettings
import com.intellij.database.run.ui.table.statisticsPanel.StatisticsPanelMode
import com.intellij.database.run.ui.table.statisticsPanel.StatisticsTableHeader
import com.intellij.database.run.ui.table.statisticsPanel.types.ColumnDescriptionStatistics
import com.intellij.database.run.ui.table.statisticsPanel.types.ColumnVisualisationType
import com.intellij.database.run.ui.table.statisticsPanel.types.ColumnVisualizationData
import com.intellij.database.run.ui.table.statisticsPanel.types.ColumnVisualizationDataHistogram
import com.intellij.database.run.ui.table.statisticsPanel.types.ColumnVisualizationDataPercentage
import com.intellij.database.run.ui.table.statisticsPanel.types.ColumnVisualizationDataTooltip
import com.intellij.database.run.ui.table.statisticsPanel.types.ColumnVisualizationDataUnique
import com.intellij.grid.charts.impl.StatisticsPanelRenderer
import com.intellij.openapi.application.EDT
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.util.NlsContexts
import com.intellij.openapi.util.NlsSafe
import com.intellij.ui.IdeBorderFactory
import com.intellij.ui.SideBorder
import com.intellij.ui.components.JBLabel
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.util.preferredHeight
import com.intellij.util.awaitCancellationAndInvoke
import com.intellij.util.ui.JBUI
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.html.body
import kotlinx.html.html
import kotlinx.html.stream.createHTML
import kotlinx.html.style
import kotlinx.html.table
import org.jetbrains.letsPlot.tooltips.layerTooltips
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Dimension
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTable
import javax.swing.table.TableColumn

class CoreStatisticsTableHeaderPanel(
  table: TableResultView,
  val dataProvider: CoreStatisticsHeaderDataProvider,
  val scope: CoroutineScope,
) : StatisticsTableHeader() {

  override fun createColumnsController(): ColumnsControllerPanel {
    return CoreColumnsControllerPanel(table as JTable)
  }

  init {
    isOpaque = false
    this.position = DEFAULT_POSITION
    installTable(table as JTable)

    scope.awaitCancellationAndInvoke {
      detachController()
    }
  }

  inner class CoreColumnsControllerPanel(table: JTable) : StatisticsColumnsControllerPanel(table) {
    private var tableStatisticsData: List<ColumnDescriptionStatistics>? = null
    private var tableVisualizationData: List<ColumnVisualizationData>? = null
    private val refreshFlow = MutableSharedFlow<StatisticsPanelMode>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    init {
      isOpaque = false
      for (i in 0 until tableColumnModel.columnCount) {
        createColumn(i)
      }

      myPreferredSize = computeMyPreferredSize()
      placeComponents()
      tableColumnModel.addColumnModelListener(this)
      // Updating header if content of the table changes
      tableModel.addTableModelListener { event ->
        refreshFlow.tryEmit(statisticsPanelMode)
      }

      scope.launch { collectRefreshFlow() }
    }

    /**
     * Listen to changes in the statistics panel mode and update columns reactively.
     */
    private suspend fun collectRefreshFlow() {
      refreshFlow.collectLatest { mode ->
        if (mode == StatisticsPanelMode.OFF) {
          for (column in columns) {
            (column as CoreStatisticsColumnPanel).setMode(mode)
          }
        }
        else {
          updateHeaderPanel()
        }
      }
    }

    override fun createColumn(columnView: Int): CoreStatisticsColumnPanel {
      val tc = tableColumnModel.getColumn(columnView)
      val column = CoreStatisticsColumnPanel(tc, columnView)
      column.updateHeight()
      columns.add(column)
      add(column)
      return column
    }

    override fun setMode(statisticsPanelMode: StatisticsPanelMode) {
      refreshFlow.tryEmit(statisticsPanelMode)
    }

    suspend fun updateHeaderPanel() {
      dataProvider.gridChanged()
      withContext(Dispatchers.Default) {
        val deferredTableStatisticsData = async { dataProvider.generateStatistics() }
        val deferredTableVisualizationData = async { dataProvider.generateVisualization() }
        tableStatisticsData = deferredTableStatisticsData.await()
        tableVisualizationData = deferredTableVisualizationData.await()
      }
      val maxStatisticsAndVisualizationPanelHeight = computeMaxStatisticsAndVisualisationPanelHeight(statisticsPanelMode)
      updateAllColumns(maxStatisticsAndVisualizationPanelHeight)
    }

    suspend fun updateAllColumns(maxHeight: Int) {
      for ((columnIndex, c) in columns.withIndex()) {
        val column = (c as CoreStatisticsColumnPanel)
        column.maxHeightAmongAllTableStatisticsAndVisualizationPanels = maxHeight

        withContext(Dispatchers.EDT) {
          setToColumnStatisticsAndVisualisationData(column, columnIndex)
          column.setMode(statisticsPanelMode)
        }
      }
    }

    private fun getColumnStatisticsNumber(columnIndex: Int): Int {
      return tableStatisticsData?.getOrNull(columnIndex)?.columnStatistics?.size ?: -1
    }

    /**
     * In compact mode, we have only one statistics to show.
     * In detailed mode, the number depends on the data type.
     *
     * To make a proper united appearance of statistics and visualisations,
     * we may need to know the max height of all statistics panels for a table.
     * To compute the size of a panel between statistics and visualisations panels.
     */
    fun getColumnIndexWithMaxStatisticsNumber(): Int {
      var maxStatisticsNumber = -1
      var maxStatisticsNumberColumnIndex = -1
      for (columnInd in 0 until tableColumnModel.columnCount) {
        val curColumnStatisticsNumber = getColumnStatisticsNumber(columnInd)
        if (curColumnStatisticsNumber > maxStatisticsNumber) {
          maxStatisticsNumber = curColumnStatisticsNumber
          maxStatisticsNumberColumnIndex = columnInd
        }
      }

      return maxStatisticsNumberColumnIndex
    }

    private suspend fun computeMaxStatisticsAndVisualisationPanelHeight(statisticsPanelMode: StatisticsPanelMode): Int {
      var maxStatisticsAndVisualizationPanelHeight = 0
      withContext(Dispatchers.EDT) {
        val columnIndexWithMaxHeight = getColumnIndexWithMaxStatisticsNumber()
        val columnWithMaxHeight = columns[columnIndexWithMaxHeight] as CoreStatisticsColumnPanel
        setToColumnStatisticsAndVisualisationData(columnWithMaxHeight, columnIndexWithMaxHeight)
        columnWithMaxHeight.setMode(statisticsPanelMode)
        maxStatisticsAndVisualizationPanelHeight = columnWithMaxHeight.myHeight
      }
      return maxStatisticsAndVisualizationPanelHeight
    }

    private fun setToColumnStatisticsAndVisualisationData(
      column: CoreStatisticsColumnPanel,
      columnIndex: Int,
    ) {
      column.curColumnStatisticsData = tableStatisticsData?.getOrNull(columnIndex)
      column.curColumnVisualizationData = tableVisualizationData?.getOrNull(columnIndex)
    }

    inner class CoreStatisticsColumnPanel(
      tc: TableColumn,
      indexForStatistics: Int,
    ) : StatisticsPanel(tc) {
      var maxHeightAmongAllTableStatisticsAndVisualizationPanels: Int = 0
      var curColumnStatisticsData: ColumnDescriptionStatistics? = tableStatisticsData?.getOrNull(indexForStatistics)
      var curColumnVisualizationData: ColumnVisualizationData? = tableVisualizationData?.getOrNull(indexForStatistics)
      private val statisticsRenderer = StatisticsPanelRenderer(table, this@CoreStatisticsColumnPanel)

      init {
        isOpaque = false
        myWidth = tableColumn.width
        myHeight = getPreferredSize().height
        border = IdeBorderFactory.createBorder(SideBorder.RIGHT)

        panel = offStatisticsPanel

        add(panel!!, BorderLayout.CENTER)
        tableColumn.addPropertyChangeListener(this)
      }

      /**
       * Sets the mode for the statistics panel.
       * This function has a contract that before its call
       * [maxHeightAmongAllTableStatisticsAndVisualizationPanels],[curColumnVisualizationData],[curColumnStatisticsData] should be assigned.
       * To do so, it's necessary to request and await data for statistics and visualization.
       * And then, invoke [setToColumnStatisticsAndVisualisationData] function.
       *
       * @param newMode The new mode to set for the statistics panel.
       */
      override fun setMode(newMode: StatisticsPanelMode) {
        createAndSetNewPanel(newMode)
      }

      private fun createAndSetNewPanel(newMode: StatisticsPanelMode) {
        val newPanel =
          when (newMode) {
            StatisticsPanelMode.OFF -> offStatisticsPanel

            StatisticsPanelMode.COMPACT -> {
              if (compactStatisticsPanel == null) {
                compactStatisticsPanel = createCompactStatistics()
              }
              compactStatisticsPanel?.let { createPanelWithVisualisation(it) }
            }

            StatisticsPanelMode.DETAILED -> {
              if (detailedStatisticsPanel == null) {
                detailedStatisticsPanel = createDetailedStatistics()
              }
              detailedStatisticsPanel?.let { createPanelWithVisualisation(it) }
            }
          } ?: return

        myHeight = newPanel.preferredHeight
        newPanel.border = SideBorder(JBUI.CurrentTheme.Editor.BORDER_COLOR, SideBorder.BOTTOM or SideBorder.RIGHT)

        resetPanel(newPanel)
      }


      override fun createCompactStatistics(): String? {
        val columnDescriptionData = curColumnStatisticsData
        if (columnDescriptionData == null) {
          thisLogger().warn("Statistics is empty. No compact statistics will be shown")
          return null
        }

        @NlsSafe val compactHtml = createHTML().html {
          body {
            table {
              statisticsRenderer.composeStatisticsTableHtml(
                columnDescriptionData.columnStatistics.take(1),
                this@table,
                CoreDataWranglerBundle.message("table.tooltip.statistics.missing"),
                tableColumn.width
              )
            }
          }
        }

        return compactHtml
      }

      override fun createDetailedStatistics(): String? {
        val columnDescriptionData = curColumnStatisticsData
        if (columnDescriptionData == null) {
          thisLogger().warn("Statistics is empty. No detailed statistics will be shown")
          return null
        }

        @NlsSafe val detailedHtml = createHTML().html {
          body {
            table {
              style = "margin: 0; padding: 0; border-collapse: collapse;"
              statisticsRenderer.composeStatisticsTableHtml(
                columnDescriptionData.columnStatistics,
                this@table,
                CoreDataWranglerBundle.message("table.tooltip.statistics.missing"),
                tableColumn.width
              )
            }
          }
        }

        return detailedHtml
      }

      private fun createPanelWithVisualisation(@NlsContexts.Label statisticsPanelHtml: String): JPanel {
        val panelWithStatisticsAndVis = panel {
          // Add statistics here
          row {
            cell(JBLabel(statisticsPanelHtml).setCopyable(true)).applyToComponent {
              alignmentY = TOP_ALIGNMENT
              alignmentX = CENTER_ALIGNMENT
            }
          }
        }
        panelWithStatisticsAndVis.layout = BoxLayout(panelWithStatisticsAndVis, BoxLayout.Y_AXIS)
        val backgroundColor = (table as TableResultView).background
        panelWithStatisticsAndVis.background = backgroundColor

        if (curColumnStatisticsData == null || curColumnVisualizationData == null) return panelWithStatisticsAndVis

        val visualisation: Component? = when (curColumnVisualizationData?.visualisationType) {
          ColumnVisualisationType.HISTOGRAM -> {
            val data = curColumnVisualizationData as ColumnVisualizationDataHistogram
            statisticsRenderer.createVisualizationHistogram(data.data, createTooltip(data.tooltips), data.axisXLabels, backgroundColor).apply {
              preferredSize = Dimension(panelWithStatisticsAndVis.preferredSize.width, HistogramSettings.HISTOGRAM_HEIGHT)
            }
          }

          ColumnVisualisationType.UNIQUE -> {
            val data = curColumnVisualizationData as ColumnVisualizationDataUnique
            val messageForUnique = CoreDataWranglerBundle.message("table.visualisation.unique")
            val uniqueValuesVisualization = statisticsRenderer.createVisualizationUniqueValues(data.numberOfUnique, messageForUnique, tableColumn.width)
            JLabel(uniqueValuesVisualization).apply {
              alignmentX = CENTER_ALIGNMENT
            }
          }

          ColumnVisualisationType.PERCENTAGE -> {
            val data = curColumnVisualizationData as ColumnVisualizationDataPercentage
            val messageForOthers = CoreDataWranglerBundle.message("table.visualisation.other")
            val percentageVisualization = statisticsRenderer.createVisualizationPercentage(data.percentageMap, KEYWORD_FOR_MAPPING_OTHERS, messageForOthers, tableColumn.width)
            JLabel(percentageVisualization).apply {
              alignmentX = CENTER_ALIGNMENT
            }
          }

          else -> null
        }
        if (statisticsPanelMode == StatisticsPanelMode.DETAILED) {
          val curColumnTotalHeight = panelWithStatisticsAndVis.preferredSize.height + (visualisation?.preferredSize?.height ?: 0)
          if (curColumnTotalHeight < maxHeightAmongAllTableStatisticsAndVisualizationPanels) {
            panelWithStatisticsAndVis.add(Box.createVerticalStrut(maxHeightAmongAllTableStatisticsAndVisualizationPanels - curColumnTotalHeight))
          }
          else {
            panelWithStatisticsAndVis.add(Box.createVerticalGlue())
          }
        }
        panelWithStatisticsAndVis.add(visualisation)
        return panelWithStatisticsAndVis
      }

      private fun createTooltip(tooltip: ColumnVisualizationDataTooltip?): layerTooltips? {
        tooltip ?: return null

        var result = layerTooltips().anchor(tooltip.anchor).line(tooltip.line)

        tooltip.format.forEach {
          result = result.format(it.field, it.format)
        }

        return result
      }
    }
  }
}