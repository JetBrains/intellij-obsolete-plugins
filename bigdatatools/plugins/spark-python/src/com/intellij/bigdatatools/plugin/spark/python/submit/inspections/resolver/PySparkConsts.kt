package com.intellij.bigdatatools.plugin.spark.python.submit.inspections.resolver

import com.intellij.bigdatatools.plugin.spark.python.submit.inspections.model.PySparkSchemaInfo
import com.intellij.openapi.util.Key

object PySparkConsts {
  const val DATAFRAME_CLASS_NAME = "pyspark.sql.dataframe.DataFrame"
  const val DATAFRAME_NA_CLASS_NAME = "pyspark.sql.dataframe.DataFrameNaFunctions"
  const val COLUMN_CLASS_NAME = "pyspark.sql.column.Column"
  const val DATAFRAME_READER_CLASS_NAME = "pyspark.sql.readwriter.DataFrameReader"
  const val SPARK_SESSION_CLASS_NAME = "pyspark.sql.session.SparkSession"
  const val RDD_CLASS_NAME = "pyspark.rdd.RDD"
  const val ROW_CLASS_NAME = "pyspark.sql.types.Row"
  const val GROUP_DATA_CLASS_NAME = "pyspark.sql.group.GroupedData"

  const val STRUCT_TYPE_CLASS_NAME = "pyspark.sql.types.StructType"
  const val STRUCT_FIELD_CLASS_NAME = "pyspark.sql.types.StructField"

  val KEY_STAMP = Key<Long>("PYSPARK_SCHEMA_STAMP")
  val DATAFRAME_KEY = Key<PySparkSchemaInfo>("PYSPARK_SCHEMA")
  val SCHEMA_DEFINITION_KEY = Key<PySparkSchemaInfo>("SCHEMA_DEFINITION_KEY")

}