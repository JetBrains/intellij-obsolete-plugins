package com.jetbrains.bigdatatools.common.rfs.localcache.analyzing.parquet

import com.jetbrains.bigdatatools.common.rfs.localcache.analyzing.parquet.TimeStamp.export
import com.jetbrains.bigdatatools.common.rfs.localcache.analyzing.parquet.TimeStamp.getZoneId
import com.jetbrains.bigdatatools.common.rfs.localcache.analyzing.parquet.TimeStamp.str2
import com.jetbrains.bigdatatools.common.rfs.localcache.analyzing.parquet.TimeStamp.str3
import com.jetbrains.bigdatatools.common.rfs.localcache.analyzing.parquet.TimeStamp.str6
import com.jetbrains.bigdatatools.common.rfs.localcache.analyzing.parquet.TimeStamp.str9
import org.apache.parquet.column.impl.ColumnReaderImpl
import org.apache.parquet.example.data.simple.NanoTime
import org.apache.parquet.io.api.Binary
import org.apache.parquet.schema.PrimitiveType
import java.math.BigDecimal
import java.math.BigInteger
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.Date

/**
 * User: Dmitry.Naydanov
 * Date: 2018-11-20.
 */

interface ValuePrettyPrinter {
  fun printCurrentValue(reader: ColumnReaderImpl): String
}

class DefaultPrinter(private val tpe: PrimitiveType.PrimitiveTypeName) : ValuePrettyPrinter {
  override fun printCurrentValue(reader: ColumnReaderImpl): String = tpe.toString(reader)
}

class Utf8Printer : ValuePrettyPrinter {
  override fun printCurrentValue(reader: ColumnReaderImpl): String = String(reader.binary.bytes, StandardCharsets.UTF_8)
}

class Int96Printer : ValuePrettyPrinter {
  override fun printCurrentValue(reader: ColumnReaderImpl): String {
    val nanoTime = NanoTime.fromBinary(Binary.fromConstantByteArray(reader.binary.bytes))
    val nsTotal = (nanoTime.julianDay - 2440588L) * (86400L * 1000 * 1000 * 1000) + nanoTime.timeOfDayNanos
    val nanos = nsTotal % (1000 * 1000)
    val date = Date(nsTotal / (1000 * 1000))
    val nanoString = if (nanos == 0L) "" else " + " + nanos.toString(10) + "ns"

    return date.toString() + nanoString
  }
}

abstract class DecimalPrinter<T : Number>(private val scale: Int) : ValuePrettyPrinter {
  override fun printCurrentValue(reader: ColumnReaderImpl): String {
    val v = read(reader).toString()
    return if (v == "0") v else v.substring(0, v.length - scale) + "." + v.substring(v.length - scale)
  }

  protected abstract fun read(reader: ColumnReaderImpl): T
}

class Decimal32Printer(scale: Int) : DecimalPrinter<Int>(scale) {
  override fun read(reader: ColumnReaderImpl): Int = reader.integer
}

class Decimal64Printer(scale: Int) : DecimalPrinter<Long>(scale) {
  override fun read(reader: ColumnReaderImpl): Long = reader.long
}

class DecimalBinaryPrinter(private val scale: Int) : ValuePrettyPrinter {
  override fun printCurrentValue(reader: ColumnReaderImpl): String =
    BigDecimal(BigInteger(reader.binary.bytes), scale).toPlainString()
}

class Int32Printer : ValuePrettyPrinter {
  override fun printCurrentValue(reader: ColumnReaderImpl): String = "${reader.integer}"
}

class Int64Printer : ValuePrettyPrinter {
  override fun printCurrentValue(reader: ColumnReaderImpl): String = "${reader.long}"
}

class Date32Printer : ValuePrettyPrinter {
  override fun printCurrentValue(reader: ColumnReaderImpl): String {
    val df = SimpleDateFormat("yyyy-MM-dd")
    val resultDate = Date(reader.integer.toLong() * 24 * 60 * 60 * 1000)
    return df.format(resultDate)
  }
}

class TimeMillisPrinter(private val isAdjustedToUTC: Boolean) : ValuePrettyPrinter {
  override fun printCurrentValue(reader: ColumnReaderImpl): String =
    LocalDateTime.ofInstant(Instant.ofEpochMilli(reader.long), getZoneId(isAdjustedToUTC)).export(true)

  // Copied from Database plugin to get rid of the dependency
  private fun LocalDateTime.export(withMilliseconds: Boolean): String =
    toLocalDate().export() + ' ' + toLocalTime().export(withMilliseconds)

  private fun LocalTime.export(withMilliseconds: Boolean): String =
    if (withMilliseconds) "${hour.str2}:${minute.str2}:${second.str2}.${(nano / 1_000_000).str3}"
    else "${hour.str2}:${minute.str2}:${second.str2}"
}

class TimeMicrosPrinter(private val isAdjustedToUTC: Boolean) : ValuePrettyPrinter {
  override fun printCurrentValue(reader: ColumnReaderImpl): String {
    val microSeconds = reader.long
    val seconds = microSeconds / 1_000_000
    val nanoSeconds = (microSeconds % 1_000_000) * 1_000
    return LocalDateTime.ofInstant(Instant.ofEpochSecond(seconds, nanoSeconds), getZoneId(isAdjustedToUTC)).export()
  }

  private fun LocalDateTime.export(): String =
    toLocalDate().export() + ' ' + toLocalTime().export()

  private fun LocalTime.export(): String = "${hour.str2}:${minute.str2}:${second.str2}.${(nano / 1_000).str6}"
}

class TimeNanoPrinter(private val isAdjustedToUTC: Boolean) : ValuePrettyPrinter {
  override fun printCurrentValue(reader: ColumnReaderImpl): String {
    val nanoSeconds = reader.long
    val seconds = nanoSeconds / 1_000_000_000
    val nano = nanoSeconds % 1_000_000_000
    return LocalDateTime.ofInstant(Instant.ofEpochSecond(seconds, nano), getZoneId(isAdjustedToUTC)).export()
  }

  private fun LocalDateTime.export(): String =
    toLocalDate().export() + ' ' + toLocalTime().export()

  private fun LocalTime.export(): String = "${hour.str2}:${minute.str2}:${second.str2}.${(nano).str9}"
}

class FloatPrinter : ValuePrettyPrinter {
  override fun printCurrentValue(reader: ColumnReaderImpl): String = "${reader.float}"
}

class DoublePrinter : ValuePrettyPrinter {
  override fun printCurrentValue(reader: ColumnReaderImpl): String = "${reader.double}"
}

class BooleanPrinter : ValuePrettyPrinter {
  override fun printCurrentValue(reader: ColumnReaderImpl): String = "${reader.boolean}"
}

object ValuePrintersUtil {
  fun chooseByColumnType(tpe: PrimitiveType.PrimitiveTypeName): ValuePrettyPrinter {
    return when (tpe) {
      PrimitiveType.PrimitiveTypeName.INT32 -> Int32Printer()
      PrimitiveType.PrimitiveTypeName.INT64 -> Int64Printer()
      PrimitiveType.PrimitiveTypeName.INT96 -> Int96Printer()
      PrimitiveType.PrimitiveTypeName.DOUBLE -> DoublePrinter()
      PrimitiveType.PrimitiveTypeName.FLOAT -> FloatPrinter()
      PrimitiveType.PrimitiveTypeName.BOOLEAN -> BooleanPrinter()
      PrimitiveType.PrimitiveTypeName.BINARY -> Utf8Printer()
      else -> DefaultPrinter(tpe)
    }
  }
}