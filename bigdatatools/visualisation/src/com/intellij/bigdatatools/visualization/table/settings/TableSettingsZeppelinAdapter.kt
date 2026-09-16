package com.intellij.bigdatatools.visualization.table.settings

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.intellij.charts.utils.asJsonObjectOrNull
import com.intellij.charts.utils.getAsJsonArrayOrNull
import com.intellij.charts.utils.getAsJsonObjectOrNull
import com.intellij.charts.utils.getAsStringOrNull
import com.intellij.charts.utils.getOrCreateJsonArray
import com.intellij.charts.utils.getOrCreateJsonObject
import com.intellij.charts.utils.optBoolean
import com.intellij.charts.utils.optInt
import com.intellij.charts.utils.optString

// Zeppelin table settings looks like
// {"name":"job","visible":true,"width":"*","sort":{},"filters":[{}],"pinned":""}
// In Zeppelin 9 name ends with column index in model! it could be marital2, age0, job1, education3, balance4.
// In Zeppelin 8 names are always normal.
object TableSettingsZeppelinAdapter {
  fun fromJson(graph: JsonObject?): TableSettings? {
    val table = graph?.getAsJsonObjectOrNull("setting")?.getAsJsonObjectOrNull("table") ?: return null
    val tableGrid = table.getAsJsonObjectOrNull("tableGridState") ?: return null

    val options = table.getAsJsonObjectOrNull("tableOptionValue")

    val types = table.getAsJsonObjectOrNull("tableColumnTypeState")?.getAsJsonObjectOrNull("names") ?: return null

    return TableSettings(columns = getColumnsSettings(tableGrid.getAsJsonArrayOrNull("columns"), types),
                         pagination = getPaginationSettings(tableGrid.getAsJsonObjectOrNull("pagination")),
                         useFilter = options?.optBoolean("useFilter") ?: false,
                         showPagination = options?.optBoolean("showPagination") ?: false,
                         showAggregationFooter = options?.optBoolean("showAggregationFooter") ?: false)
  }

  fun toJson(graph: JsonObject, tableSettings: TableSettings): Boolean {
    val table = graph.getOrCreateJsonObject("setting").getOrCreateJsonObject("table")

    table.getOrCreateJsonObject("tableOptionValue").apply {
      addProperty("useFilter", tableSettings.useFilter)
      addProperty("showPagination", tableSettings.showPagination)
      addProperty("showAggregationFooter", tableSettings.showAggregationFooter)
    }

    table.getOrCreateJsonObject("tableGridState").apply {
      val columns = getOrCreateJsonArray("columns")
      val types = table.getAsJsonObjectOrNull("tableColumnTypeState")?.getAsJsonObjectOrNull("names")
      val realNames = types?.keySet()

      val nameToColumns = columns.mapNotNull { columnJson ->
        val name = (columnJson as? JsonObject)?.getAsStringOrNull("name") ?: return@mapNotNull null
        val realName = realNames?.firstOrNull { name.startsWith(it) } ?: name
        realName to columnJson
      }.toMap()

      val newColumns = JsonArray()
      tableSettings.columns.forEach { column ->

        val columnJson = nameToColumns[column.name] ?: JsonObject()
        columnJson.addProperty("visible", column.visible)
        if (column.sort != null) {
          columnJson.add("sort", JsonObject().apply {
            addProperty("priority", column.sort.priority)
            addProperty("direction", if (column.sort.direction == TableColumnSortingDirection.ASC) "asc" else "desc")
          })
        }
        newColumns.add(columnJson)
      }

      add("columns", newColumns)

      getOrCreateJsonObject("pagination").apply {
        addProperty("paginationCurrentPage", tableSettings.pagination.currentPage)
        addProperty("paginationPageSize", tableSettings.pagination.pageSize)
      }
    }

    return false
  }

  private fun getPaginationSettings(settings: JsonObject?): TablePaginationSettings {
    settings ?: return TablePaginationSettings(1, 250)
    return TablePaginationSettings(settings.optInt("paginationCurrentPage") ?: 1, settings.optInt("paginationPageSize") ?: 250)
  }

  private fun getColumnsSettings(settings: JsonArray?, types: JsonObject): List<TableColumnSettings> {
    settings ?: return emptyList()
    val result = mutableListOf<TableColumnSettings>()

    val realNames = types.keySet()

    settings.forEach { data ->
      val column = data.asJsonObjectOrNull() ?: return@forEach

      // We have problems with name (In Zeppelin 0.9 it could have suffix with original index.
      val name = column.getAsStringOrNull("name") ?: return@forEach

      val realName = realNames.firstOrNull { name.startsWith(it) } ?: return@forEach
      val sortSettings = column.getAsJsonObjectOrNull("sort")

      result.add(TableColumnSettings(
        name = realName,
        visible = column.optBoolean("visible"),
        sort = getTableColumnSortingSettings(sortSettings)
      ))
    }

    return result
  }

  private fun getTableColumnSortingSettings(settings: JsonObject?): TableColumnSortingSettings? {
    settings ?: return null
    return TableColumnSortingSettings(settings.optInt("priority") ?: 0,
                                      if (settings.optString("direction") == "desc") TableColumnSortingDirection.DESC
                                      else TableColumnSortingDirection.ASC)
  }
}