package com.intellij.bigdatatools.plugin.spark.assistance.dfanalysis

object DfColumnTypesUtil {
  val INTEGER_TYPES = setOf(ByteType, ShortType, IntegerType, LongType)
  private val FLOAT_TYPES = setOf(FloatType, DoubleType)
  val NUMERIC_TYPES = INTEGER_TYPES + FLOAT_TYPES

  val ALL_SIMPLE_TYPES = setOf(BooleanType, ByteType, ShortType, IntegerType, LongType, FloatType, DoubleType, StringType, TimestampType,
                               DateType)
  val ALL_TYPES = ALL_SIMPLE_TYPES // todo

  val DDL_TYPES =
    ALL_SIMPLE_TYPES.associateBy { it.sqlName }.plus(
      arrayOf(
        Pair("VARCHAR", StringType),
        Pair("INT", IntegerType)
      )
    )
}

abstract class SimpleDfColumnType : DfColumnType {
  override val name: String
    get() = this::class.java.simpleName
  override val isComplex: Boolean
    get() = false
  override val jsonStructure: String
    get() = name.lowercase().removeSuffix("type")
  override val simpleName: String
    get() = name

  /**
   * Right now works as org.apache.spark.sql.functions#coalesce(Column*)
   */
  override fun union(other: DfColumnType?): DfColumnType? = if (other == this) this else null
}

abstract class SimpleIntNColumnType : SimpleDfColumnType() {
  abstract fun length(): Int

  override fun isCompatible(other: DfColumnType): Boolean = DfColumnTypesUtil.INTEGER_TYPES.contains(other) || other == NumericType

  override fun union(other: DfColumnType?): DfColumnType? {
    if (other !is SimpleIntNColumnType) return super.union(other)

    return if (other.length() > length()) other else this
  }
}

object BooleanType : SimpleDfColumnType() {
  override val sqlName: String = "BOOLEAN"

  override fun isCompatible(other: DfColumnType): Boolean {
    return other == this  // || other == StringType
  }

  override fun <T> mapType(mapper: DfTypesMapper<T>): T = mapper.booleanType()
}

object ByteType : SimpleIntNColumnType() {
  override val sqlName: String = "TINYINT"

  override fun <T> mapType(mapper: DfTypesMapper<T>): T = mapper.byteType()

  override fun length(): Int = 1
}

object ShortType : SimpleIntNColumnType() {
  override val sqlName = "SMALLINT"

  override fun <T> mapType(mapper: DfTypesMapper<T>): T = mapper.shortType()

  override fun length(): Int = 2
}

object IntegerType : SimpleIntNColumnType() {
  override val sqlName: String = "INTEGER"

  override fun <T> mapType(mapper: DfTypesMapper<T>): T = mapper.integerType()

  override fun length(): Int = 4
}

object LongType : SimpleIntNColumnType() {
  override val sqlName: String = "BIGINT"

  override fun <T> mapType(mapper: DfTypesMapper<T>): T = mapper.longType()

  override fun length(): Int = 8
}

object FloatType : SimpleDfColumnType() {
  override val sqlName: String = "FLOAT"

  override fun isCompatible(other: DfColumnType): Boolean = DfColumnTypesUtil.NUMERIC_TYPES.contains(other)

  override fun <T> mapType(mapper: DfTypesMapper<T>): T = mapper.floatType()
}

object DoubleType : SimpleDfColumnType() {
  override val sqlName: String = "DOUBLE"

  override fun isCompatible(other: DfColumnType): Boolean = DfColumnTypesUtil.NUMERIC_TYPES.contains(other)

  override fun <T> mapType(mapper: DfTypesMapper<T>): T = mapper.doubleType()
}

object StringType : SimpleDfColumnType() {
  override val sqlName: String = "STRING"

  override fun isCompatible(other: DfColumnType): Boolean = !DfColumnTypesUtil.NUMERIC_TYPES.contains(other) && other !is NumericType

  override fun <T> mapType(mapper: DfTypesMapper<T>): T = mapper.stringType()
}

object TimestampType : SimpleDfColumnType() {
  override val sqlName = "TIMESTAMP"


  override fun isCompatible(other: DfColumnType): Boolean = other == this

  override fun <T> mapType(mapper: DfTypesMapper<T>): T = mapper.timestampType()
}

object DateType : SimpleDfColumnType() {
  override val sqlName = "DATE"


  override fun isCompatible(other: DfColumnType): Boolean = other == this

  override fun <T> mapType(mapper: DfTypesMapper<T>): T = mapper.dateType()
}

// marker interface to represent type, that don't exist in spark/sql
interface DfSyntheticColumnType : DfColumnType
object NumericType : DfSyntheticColumnType {
  override val name: String = "NUMERIC"
  override val sqlName: String = name
  override val jsonStructure: String = name
  override val simpleName: String = name
  override val isComplex: Boolean = false

  override fun isCompatible(other: DfColumnType): Boolean = other == this || DfColumnTypesUtil.NUMERIC_TYPES.contains(other)

  override fun union(other: DfColumnType?): DfColumnType? = if (other != null && isCompatible(other)) this else null

  override fun <T> mapType(mapper: DfTypesMapper<T>): T = mapper.valType()
}

object UnknownType : DfSyntheticColumnType {
  override val name: String = "UNKNOWN"
  override val sqlName: String = name
  override val jsonStructure: String = name
  override val simpleName: String = name
  override val isComplex: Boolean = false

  override fun isCompatible(other: DfColumnType): Boolean = true

  override fun union(other: DfColumnType?): DfColumnType? = other

  override fun <T> mapType(mapper: DfTypesMapper<T>): T = mapper.bottomType()
}