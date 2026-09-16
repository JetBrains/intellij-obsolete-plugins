package com.intellij.bigdatatools.zeppelin.ztools.dataframe

import com.intellij.bigdatatools.zeppelin.ztools.completion.ColumnInfo

class ZtoolsDataFrameStorage {
  fun clear() = dataFrames.clear()

  fun addDataFrame(varName: String, schema: SparkDataFrameSchema) = dataFrames.put(varName, schema)

  fun getColumns(varName: String): List<ColumnInfo> =
    dataFrames[varName]?.columns ?: dataFrames.values.flatMap { it.columns }.distinct()

  val dataFrames = mutableMapOf<String, SparkDataFrameSchema>()
}