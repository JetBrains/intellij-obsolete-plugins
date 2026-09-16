package com.intellij.bigdatatools.zeppelin.ztools.completion

sealed class ColumnType(val presentableName: String) {
  companion object {
    private val predefinedTypes = listOf(IntColumnType, StringColumnType, BooleanColumnType)
    fun getType(name: String) = predefinedTypes.firstOrNull { it.presentableName == name } ?: MiscColumnType(name)
  }
}

object IntColumnType : ColumnType("IntegerType")
object StringColumnType : ColumnType("StringType")
object BooleanColumnType : ColumnType("BooleanType")

class MiscColumnType(name: String) : ColumnType(name)

data class ColumnInfo(val name: String,
                      val tpe: ColumnType,
                      val nullable: Boolean) {
  override fun toString(): String =  "$name:${tpe.presentableName}"
}

data class ZtoolsLightColumnInfo(val name: String,
                                 val type: String,
                                 val nullable: Boolean,
                                 val metadata: Any) {
  private val adjustedTypeString = when(type.lowercase()) {
    "integer" -> "IntegerType"
    "string" -> "StringType"
    "boolean" -> "BooleanType"
    else -> type
  }

  private val sqlTypeString = when(type.lowercase()) {
    "integer" -> "\"org.apache.spark.sql.types.IntegerType\$\""
    "string" -> "\"org.apache.spark.sql.types.StringType\$\""
    "boolean" -> "\"org.apache.spark.sql.types.BooleanType\$\""
    else -> "\"MiscType\""
  }

  fun toColumnInfo(): ColumnInfo = ColumnInfo(name, ColumnType.getType(adjustedTypeString), nullable)
  fun toJson(): String = "{\"jvm-type\":\"org.apache.spark.sql.types.StructField\", \"value\":" +
                         "{\"metadata\":{\"jvm-type\":\"org.apache.spark.sql.types.Metadata\",\"type\":\"org.apache.spark.sql.types.Metadata\",\"value\":\"$metadata\"}," +
                         "\"nullable\":{\"type\":\"Boolean\",\"value\":$nullable}," +
                         "\"dataType\":{\"jvm-type\":$sqlTypeString,\"type\":\"org.apache.spark.sql.types.DataType\",\"value\":\"$adjustedTypeString\"}," +
                         "\"name\":{\"type\":\"String\",\"value\":\"$name\"}" +
                         "}}"
}