package com.intellij.bigdatatools.visualization.inlays.utils

import com.intellij.charts.dataframe.DataFrame
import com.intellij.charts.dataframe.DataFrameImpl
import com.intellij.charts.dataframe.analyzing.CsvTypeParser
import com.intellij.charts.dataframe.columns.Column
import com.intellij.charts.dataframe.columns.NullType
import com.intellij.charts.dataframe.columns.StringType
import com.intellij.charts.dataframe.columns.TableColumnType
import com.intellij.openapi.diagnostic.Logger
import java.util.Scanner
import java.util.regex.Pattern

/**
 * Extracts data frames from text like:
 *
"+---+-----------+-------+---------+-------+\n" +
"|age|   |+  j|ob|marital|education|balance|\n" +
"+---+-----------+-------+---------+-------+\n" +
"| 30| unemployed|married|  primary|   1787|\n" +
"| 33|   services|married|secondary|   4789|\n" +
"| 35| management| |in+|e| tertiary|   1350|\n" +
"| 30| management|mar|ied| tertiary|   1476|\n" +
"| 59|blue-collar|married|secondary|      0|\n" +
"+---+-----------+-------+---------+-------+\n" +
 * Such text could be a result of "df.show()" command in notebook cell.
 */
object TextToDataFrame {

  private val logger = Logger.getInstance(this::class.java)
  private val typeParser = CsvTypeParser()
  private val tableSeparatorPattern = Pattern.compile("^\\+(((-*\\+-*)*)|(-+))\\+\$")

  data class ColumnInfo(val name: String,
                        val normalLineLength: Int,
                        val startIndex: Int,
                        val endIndex: Int,
                        val data: MutableList<String> = mutableListOf())

  /**
   *  We will not convert strings more than doubleConversionLength to doubles.
   *  This was made to prevent loosing significant digits in case like "1.21412523562363462362435252".
   */
  fun parse(text: String, doubleConversionLength: Int = Int.MAX_VALUE): List<DataFrame> {

    val result = mutableListOf<DataFrame>()

    try {
      var columns: List<ColumnInfo>? = null

      val scanner = Scanner(text)
      while (scanner.hasNextLine()) {
        val line = scanner.nextLine()

        if (columns != null) { // We are reading table.
          if (isTableSeparator(line)) {  // Table is finished
            result.add(DataFrameImpl(convertToDataFrameColumns(columns, doubleConversionLength)))
            columns = null
          }
          else {  // We are continuing reading of table data.
            fillColumns(line, columns)
          }
        }
        else if (isTableSeparator(line) && scanner.hasNextLine()) { // Table started.
          // Read columns info.
          columns = getColumns(line, scanner.nextLine())
          // Skip line.
          scanner.nextLine()
        }
      }
      scanner.close()
    }
    catch (e: Exception) {
      // Here we have expected exceptions, during processing partial outputs.
      logger.warn("Failed to convert text to dataframe. Message: ${e.message}")
    }

    return result
  }

  private fun convertToDataFrameColumns(columns: List<ColumnInfo>, doubleConversionLength: Int): ArrayList<Column<*>> {
    val result = ArrayList<Column<*>>()
    columns.forEach { column ->
      if (column.data.isEmpty()) {
        result.add(StringType.createDataColumn(column.name, StringType.createDataArray()))
      }
      else {
        var columnType: TableColumnType = NullType
        // Currently, column type detection is invalid
        column.data.forEach {
          if (it.isNotEmpty() && it != "null")
            columnType = columnType.union(typeParser.parse(it, doubleConversionLength))
        }
        if (columnType == NullType) {
          columnType = StringType
        }
        val columnData = columnType.createDataArray()
        column.data.forEach { columnType.appendToDataArray(columnData, it) }
        result.add(columnType.createDataColumn(column.name, columnData))
      }
    }
    return result
  }

  private fun isTableSeparator(text: String) = tableSeparatorPattern.matcher(text).matches()

  private fun fillColumns(text: String, columns: List<ColumnInfo>) {
    if (text.isEmpty() || columns.isEmpty()) {
      return // myDf.show(0) produces empty table with empty row
    }

    if (columns.first().normalLineLength == text.length) {
      // We cannot split by vertical separator, because cell text can contain this separator,
      // that's why we are storing split positions in ColumnInfo.
      columns.forEach { it.data.add(text.substring(it.startIndex, it.endIndex).trim()) }
    }
    else {
      // This is the case of multibyte characters in string, for example, chinese letters.
      val values = text.split("|").toMutableList().apply {
        removeFirst()
        removeLast()
      }

      columns.forEachIndexed { index, columnInfo ->
        columnInfo.data.add(values[index].trim())
      }
    }
  }

  private fun getColumns(separatorLine: String, columnsLine: String): List<ColumnInfo> {
    val result = ArrayList<ColumnInfo>()
    val separatorSplit = separatorLine.split("+").filter { it.isNotBlank() }

    val ranges = ArrayList<Pair<Int, Int>>()
    separatorSplit.indices.forEach { i ->
      val startIndex = (ranges.lastOrNull()?.second ?: 0) + 1
      val endIndex = startIndex + separatorSplit[i].length
      ranges.add(startIndex to endIndex)
    }

    // ASCII case.
    if (separatorLine.length == columnsLine.length) {
      ranges.forEach {
        result.add(ColumnInfo(columnsLine.substring(it.first, it.second).trim(), separatorLine.length, it.first, it.second))
      }
    }
    else {
      // Any multibyte character case
      val columns = columnsLine.split("|").toMutableList().apply {
        removeFirst()
        removeLast()
      }

      ranges.forEachIndexed { index, it ->
        result.add(ColumnInfo(columns[index].trim(), separatorLine.length, it.first, it.second))
      }
    }

    return result
  }
}