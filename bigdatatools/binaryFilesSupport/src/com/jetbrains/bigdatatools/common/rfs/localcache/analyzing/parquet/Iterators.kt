package com.jetbrains.bigdatatools.common.rfs.localcache.analyzing.parquet

import org.apache.parquet.column.impl.ColumnReaderImpl
import org.apache.parquet.io.ParquetDecodingException
import kotlin.math.abs

class ColumnIterator(private val columnReader: ColumnReaderImpl, private val tpePrinter: ValuePrettyPrinter,
                     private val valuesTotal: Long,
                     private val typeOfColumn: TypeOfColumn,
                     private val delimiter: String) : Iterator<String> {
  constructor(columnReader: ColumnReaderImpl, tpePrinter: ValuePrettyPrinter, valuesTotal: Long, typeOfColumn: TypeOfColumn) : this(
    columnReader, tpePrinter, valuesTotal, typeOfColumn, ", ")

  private val buffer = StringBuilder()

  private var lastRepetitionLevel = 0
  private var valuesRead = 0

  override fun hasNext(): Boolean = valuesRead < valuesTotal

  override fun next(): String {
    if (!hasNext()) throw NoSuchElementException("Next on empty iterator")

    if (typeOfColumn.isOptional && columnReader.currentDefinitionLevel == 0) {
      advance()
      return "null"
    }

    if (!typeOfColumn.isOptional && typeOfColumn.isRepeated && columnReader.currentDefinitionLevel == 0) {
      advance()
      return "[]"
    }

    val current = try {
      readValue()
    }
    catch (_: ParquetDecodingException) {
      advance()
      return "null"
    }

    if (repetitionLevel() > 0) {
      for (i in 0 until columnReader.currentRepetitionLevel) buffer.append('[')
      buffer.append(current)
      lastRepetitionLevel = columnReader.currentRepetitionLevel

      while (columnReader.currentRepetitionLevel > 0 && hasNext()) {
        val dif = columnReader.currentRepetitionLevel - lastRepetitionLevel
        lastRepetitionLevel = columnReader.currentRepetitionLevel

        if (dif < 0) for (i in 0 until -dif) buffer.append(']')
        for (i in 0 until abs(dif)) buffer.append('[')
        if (dif == 0) buffer.append(delimiter)
        buffer.append(readValue())
      }

      for (i in 0 until lastRepetitionLevel) buffer.append(']')
      val result = buffer.toString()
      buffer.setLength(0)
      return result
    }

    return current
  }

  private fun readValue(): String {
    ++valuesRead
    columnReader.readValue()
    val res = tpePrinter.printCurrentValue(columnReader)
    columnReader.consume()
    return res
  }

  private fun repetitionLevel(): Int = columnReader.currentRepetitionLevel

  private fun advance() {
    valuesRead += 1
    columnReader.consume()
  }
}

class TypeOfColumn(val isOptional: Boolean, val isRepeated: Boolean)
