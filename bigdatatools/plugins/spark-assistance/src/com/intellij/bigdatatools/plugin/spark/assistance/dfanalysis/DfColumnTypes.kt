package com.intellij.bigdatatools.plugin.spark.assistance.dfanalysis

interface DfColumnType {
  val name: String // as in DataType.typeName()
  val sqlName: String // as in DataType.sql()
  val jsonStructure: String // as in DataType.prettyJson()
  val simpleName: String // as in DataType.simpleName()
  val isComplex: Boolean

  /**
   * To use with cast function
   *
   * Looks like right now there is no reasonable way to statically determine if we can case column[A] to column[B] ,
   * so the cast checking is disabled for now
   */
  fun canCastTo(other: DfColumnType): Boolean = true

  /**
   * Checks if a function, which expects 'this' type, can handle 'other' type
   */
  fun isCompatible(other: DfColumnType): Boolean

  fun union(other: DfColumnType?): DfColumnType?

  fun <T> mapType(mapper: DfTypesMapper<T>): T
}

data class DfTypeSchema(val map: Map<String, DfColumnType>, val aliases: Map<String, Set<String>> = emptyMap())