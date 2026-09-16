package com.intellij.bigdatatools.visualization.table.settings

data class TableSettings(val columns: List<TableColumnSettings>,
                         val pagination: TablePaginationSettings,
                         val useFilter: Boolean,
                         val showPagination: Boolean,
                         val showAggregationFooter: Boolean)