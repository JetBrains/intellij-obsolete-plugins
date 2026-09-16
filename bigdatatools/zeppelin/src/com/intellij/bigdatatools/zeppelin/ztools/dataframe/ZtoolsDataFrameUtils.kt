package com.intellij.bigdatatools.zeppelin.ztools.dataframe

import com.intellij.bigdatatools.notebooks.core.impl.file.NotebookVirtualFile
import com.intellij.bigdatatools.zeppelin.ztools.completion.ColumnInfo
import com.intellij.bigdatatools.zeppelin.ztools.completion.ColumnType
import com.intellij.bigdatatools.zeppelin.ztools.variableview.ZeppelinDebugNode
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile

object ZtoolsDataFrameUtils {
  val datasetTypes = listOf(
    "org.apache.spark.sql.DataFrame",
    "org.apache.spark.sql.Dataset",
    "pyspark.sql.dataframe.DataFrame"
  )

  fun updateDataFramesInfo(root: ZeppelinDebugNode, file: VirtualFile) {
    val dataFrameNodes = findAllDataFramesNodes(root)
    val schemaInfos = dataFrameNodes.map { getSchemaInfo(it) }

    val dataFrameStorage = file.getCopyableUserData(SPARK_DATA_FRAME_KEY) ?: let {
      val newStorage = ZtoolsDataFrameStorage()
      file.putCopyableUserData(SPARK_DATA_FRAME_KEY, newStorage)
      newStorage
    }
    dataFrameStorage.clear()
    schemaInfos.forEach {
      dataFrameStorage.addDataFrame(it.name, it.schema)
    }
  }

  fun getDataFramesForFile(file: NotebookVirtualFile): Map<String, SparkDataFrameSchema> {
    val frameStorage = file.getCopyableUserData(SPARK_DATA_FRAME_KEY)
    return frameStorage?.dataFrames?.map { dfFrame -> dfFrame.key.takeLastWhile { it != '.' } to dfFrame.value }?.toMap() ?: emptyMap()
  }

  fun getSchemaInfo(node: ZeppelinDebugNode): SchemaInfo {
    val name = node.fullName.removePrefix("root.")
    val schemaNode = node.children?.firstOrNull { it.name == "schema()" }
    val columns = schemaNode?.children?.mapNotNull {
      parseStructField(it)
    } ?: emptyList()

    return SchemaInfo(SparkDataFrameSchema(columns), name)
  }

  fun parseStructField(node: ZeppelinDebugNode): ColumnInfo? {
    val columnName = parseField(node, "name") ?: return null
    val columnDataType = parseField(node, "dataType") ?: "<Unknown>"
    val nullable = parseField(node, "nullable")?.toBoolean() ?: true
    return ColumnInfo(columnName, ColumnType.getType(columnDataType), nullable)
  }

  private fun parseField(schemaColumnNode: ZeppelinDebugNode, fieldName: String) =
    schemaColumnNode.children?.firstOrNull { it.name == fieldName }?.value?.removePrefix("\"")?.removeSuffix("\"")

  data class SchemaInfo(val schema: SparkDataFrameSchema, val name: String)

  private fun findAllDataFramesNodes(root: ZeppelinDebugNode): List<ZeppelinDebugNode> {
    if (root.isDataFrame) return listOf(root)
    return root.children?.flatMap { findAllDataFramesNodes(it) } ?: emptyList()
  }
}

val ZeppelinDebugNode.isDataFrame: Boolean
  get() = ZtoolsDataFrameUtils.datasetTypes.any { this.type?.contains(it) == true }


val ZeppelinDebugNode.isStructField: Boolean
  get() =
    this.type?.contains("sql.types.StructField") == true ||
    this.type?.contains("pyspark.sql.types.StructField") == true

val ZeppelinDebugNode.isStructType: Boolean
  get() =
    this.type?.contains("sql.types.StructType") == true ||
    this.type?.contains("pyspark.sql.types.StructType") == true

val ZeppelinDebugNode.isInterpreterOrRoot: Boolean
  get() = this.type == null && this.value == null && !this.isResNode && !this.lazy && this.length == -1


data class SparkDataFrameSchema(val columns: List<ColumnInfo>)

val SPARK_DATA_FRAME_KEY = Key<ZtoolsDataFrameStorage>("SPARK_DATA_FRAME_KEY")