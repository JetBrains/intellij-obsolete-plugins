package com.intellij.bigdatatools.plugin.spark.assistance.dfanalysis


abstract class DfTypesMapper<T> {
  abstract fun byteType(): T
  abstract fun shortType(): T
  abstract fun integerType(): T
  abstract fun longType(): T
  abstract fun floatType(): T
  abstract fun doubleType(): T
  abstract fun decimalType(): T
  abstract fun stringType(): T
  abstract fun binaryType(): T
  abstract fun booleanType(): T
  abstract fun timestampType(): T
  abstract fun dateType(): T
  abstract fun valType(): T

  // if you don't know what to return - return this
  abstract fun bottomType(): T

  fun convertFrom(columnType: DfColumnType): T = columnType.mapType(this)

  fun convertTo(t: T): DfColumnType? = DfColumnTypesUtil.ALL_TYPES.find { it.mapType(this) == t }
}