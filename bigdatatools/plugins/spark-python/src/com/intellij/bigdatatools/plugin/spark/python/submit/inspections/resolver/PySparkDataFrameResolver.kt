package com.intellij.bigdatatools.plugin.spark.python.submit.inspections.resolver

import com.intellij.bigdatatools.plugin.spark.assistance.dfanalysis.ParseUtil
import com.intellij.bigdatatools.plugin.spark.assistance.dfanalysis.inlay.DataframeSampleFileService
import com.intellij.bigdatatools.plugin.spark.assistance.dfanalysis.inlay.DataframeSampleFileService.Companion.SUPPORTED_READ_METHODS
import com.intellij.bigdatatools.plugin.spark.assistance.statistic.SparkDataFrameCreateSource
import com.intellij.bigdatatools.plugin.spark.assistance.statistic.SparkStatisticPython
import com.intellij.bigdatatools.plugin.spark.python.submit.inspections.model.PySparkSchemaInfo
import com.intellij.bigdatatools.plugin.spark.python.submit.inspections.resolver.PySparkConsts.DATAFRAME_CLASS_NAME
import com.intellij.bigdatatools.plugin.spark.python.submit.inspections.resolver.PySparkConsts.DATAFRAME_KEY
import com.intellij.bigdatatools.plugin.spark.python.submit.inspections.resolver.PySparkConsts.DATAFRAME_NA_CLASS_NAME
import com.intellij.bigdatatools.plugin.spark.python.submit.inspections.resolver.PySparkConsts.DATAFRAME_READER_CLASS_NAME
import com.intellij.bigdatatools.plugin.spark.python.submit.inspections.resolver.PySparkConsts.GROUP_DATA_CLASS_NAME
import com.intellij.bigdatatools.plugin.spark.python.submit.inspections.resolver.PySparkConsts.KEY_STAMP
import com.intellij.bigdatatools.plugin.spark.python.submit.inspections.resolver.PySparkConsts.RDD_CLASS_NAME
import com.intellij.bigdatatools.plugin.spark.python.submit.inspections.resolver.PySparkConsts.SPARK_SESSION_CLASS_NAME
import com.intellij.bigdatatools.plugin.spark.python.submit.inspections.resolver.PySparkUtils.getArgByNameOrIndex
import com.intellij.openapi.components.service
import com.intellij.openapi.util.removeUserData
import com.intellij.psi.PsiElement
import com.intellij.psi.createSmartPointer
import com.jetbrains.python.psi.PyArgumentList
import com.jetbrains.python.psi.PyCallExpression
import com.jetbrains.python.psi.PyDictLiteralExpression
import com.jetbrains.python.psi.PyReferenceExpression
import com.jetbrains.python.psi.PySequenceExpression
import com.jetbrains.python.psi.PyTypedElement
import com.jetbrains.python.psi.types.TypeEvalContext

class PySparkDataFrameResolver(context: TypeEvalContext) : PySparkPsiResolver<PySparkSchemaInfo>(context) {
  private val schemaResolver = PySparkSchemaDefinitionResolver(context)
  private val columnResolver = PySparkColumnResolver(context)

  override fun beforeProcess(node: PsiElement): Pair<Boolean, PySparkSchemaInfo?> {
    if (node !is PyTypedElement)
      return true to null
    val resultType = node.typeFqnName
    if (resultType !in setOf(DATAFRAME_CLASS_NAME, DATAFRAME_NA_CLASS_NAME, DATAFRAME_READER_CLASS_NAME, GROUP_DATA_CLASS_NAME))
      return true to null

    node.getUserData(DATAFRAME_KEY)?.let {
      if (node.getUserData(KEY_STAMP) != node.containingFile.modificationStamp)
        node.removeUserData(DATAFRAME_KEY)
      else
        return true to it
    }
    return false to null
  }

  override fun customProcess(node: PsiElement, invokeNode: PsiElement?): Pair<Boolean, PySparkSchemaInfo?> {
    val sourceSchema = invokeNode?.let { resolve(it) } ?: PySparkSchemaInfo.createNotRecognized()
    return when (node) {
      is PyCallExpression -> {
        val callee = node.callee as? PyReferenceExpression ?: return true to null
        val schema = processReference(callee, sourceSchema, invokeNode)

        true to schema
      }
      else -> false to null
    }
  }

  override fun afterProcess(node: PsiElement, data: PySparkSchemaInfo?): PySparkSchemaInfo? {
    node.putUserData(DATAFRAME_KEY, data)
    node.putUserData(KEY_STAMP, node.containingFile.modificationStamp)
    return data
  }


  private fun processReference(node: PyReferenceExpression, sourceSchema: PySparkSchemaInfo?, sourceNode: PsiElement?): PySparkSchemaInfo? =
    when (sourceNode?.typeFqnName) {
      DATAFRAME_CLASS_NAME -> resolveSchemaFromDataFrame(node, sourceSchema)
      GROUP_DATA_CLASS_NAME -> resolveDataFrameFromGroupedData(node, sourceSchema)
      DATAFRAME_NA_CLASS_NAME -> resolveDataFrameFromNaFunctions(node, sourceSchema)
      SPARK_SESSION_CLASS_NAME -> resolveDataFrameFromSparkSession(node)
      RDD_CLASS_NAME -> resolveDataFrameFromRdd(node)
      DATAFRAME_READER_CLASS_NAME -> resolveDataFrameReader(node)
      else -> null
    }

  private fun resolveDataFrameReader(node: PyReferenceExpression): PySparkSchemaInfo? {
    when (node.referencedName) {
      "schema" -> {
        val schemaArgument = node.nextSibling as? PyArgumentList ?: return PySparkSchemaInfo.createNotRecognized()
        val schemaArg = schemaArgument.arguments.firstOrNull() ?: return PySparkSchemaInfo.createNotRecognized()

        val schemaInfo = schemaResolver.resolve(schemaArg)
        if (schemaInfo != null)
          SparkStatisticPython.logSchema(node, SparkDataFrameCreateSource.EXPLICIT_SCHEMA)
        return schemaInfo
      }
      in SUPPORTED_READ_METHODS -> {
        val calleeSource = getInvokeNode(node)
        val resolved = calleeSource?.let { resolve(it) }
        if (resolved != null)
          return resolved

        val smartPointer = node.createSmartPointer()
        val schemaParts = node.project.service<DataframeSampleFileService>().getAttached(smartPointer)?.schemaDdlString ?: return null
        SparkStatisticPython.logSchema(node, SparkDataFrameCreateSource.INLAY_SCHEMA)
        return PySparkSchemaInfo.createFromSchemaPart(ParseUtil.parseDdlString(schemaParts))
      }
      //Fallback for not parsed func
      else -> {
        val calleeSource = getInvokeNode(node) ?: return null
        return resolve(calleeSource)
      }
    }
  }


  private fun resolveSchemaFromDataFrame(node: PyReferenceExpression, sourceSchema: PySparkSchemaInfo?): PySparkSchemaInfo? {
    val arguments = (node.nextSibling as? PyArgumentList)?.arguments ?: emptyArray()

    val argumentList = node.nextSibling as? PyArgumentList

    when (node.referencedName) {
      "select" -> {
        val args = columnResolver.parseColumns(arguments)
        SparkStatisticPython.logSchema(node, SparkDataFrameCreateSource.DATAFRAME_TRANSFORM)
        return sourceSchema?.select(args)
      }
      "withColumn" -> {
        val colNameRef = argumentList?.getArgByNameOrIndex("colName", 0) ?: return sourceSchema
        val colName = columnResolver.parseColumn(colNameRef)
        if (colName != null) {
          SparkStatisticPython.logSchema(node, SparkDataFrameCreateSource.DATAFRAME_TRANSFORM_PARTIAL)
          return sourceSchema?.withColumn(colName)
        }
        else
          return sourceSchema
      }
      "withColumnRenamed" -> {
        val args = columnResolver.parseColumns(arguments)
        if (args.size == 2) {
          SparkStatisticPython.logSchema(node, SparkDataFrameCreateSource.DATAFRAME_TRANSFORM_PARTIAL)
          return sourceSchema?.renameColumn(args[0], args[1])
        }
        else
          return sourceSchema
      }
      "drop" -> {
        val args = columnResolver.parseColumns(arguments)
        return sourceSchema?.drop(args)
      }
      //"colRegex" -> {
      //  val colRegexArg = columnResolver.parseColumns(arguments).firstOrNull() ?: return null
      //  return sourceSchema?.getColumnByRegex(colRegexArg)
      //}
      "rdd" -> return null
      "crossJoin" -> {
        val joinArg = arguments.firstOrNull() ?: return null
        val jointSchema = schemaResolver.resolve(joinArg) ?: return null
        return sourceSchema?.crossJoin(jointSchema)
      }
      "summary", "describe" -> {
        val columnArg = arguments.firstOrNull() as? PySequenceExpression ?: return null
        val columns = columnResolver.parseColumns(columnArg.elements)
        SparkStatisticPython.logSchema(node, SparkDataFrameCreateSource.DATAFRAME_TRANSFORM)
        return PySparkSchemaInfo.create(isPartial = false, listOf("summary") + columns)
      }
      "freqItems" -> {
        val columnArg = arguments.firstOrNull() as? PySequenceExpression ?: return null
        val columns = columnResolver.parseColumns(columnArg.elements)
        SparkStatisticPython.logSchema(node, SparkDataFrameCreateSource.DATAFRAME_TRANSFORM)
        return PySparkSchemaInfo.create(isPartial = false, columns.map { it + "_freqItems" })
      }
      "filter", "crosstab", "cube", "distinct", "dropDuplicates",
      "dropDuplicatesWithinWatermark", "drop_duplicates", "dropna", "exceptAll",
      "fillna", "fill", "limit", "localCheckpoint", "offset",
      "orderBy", "persist", "unpersist", "repartition", "repartitionByRange",
      "replace", "sample", "sort", "sortWithinPartitions",
      "union", "unionAll",
      "where", "withMetadata", "withWatermark" -> {
        return sourceSchema
      }
      "join" -> {
        val pyArgumentList = node.nextSibling as? PyArgumentList ?: return null
        val otherDfRef = pyArgumentList.getArgByNameOrIndex("other", 0) ?: return null
        val otherDf = resolve(otherDfRef) ?: return null
        //TODO: support more clever join
        //val onFields = pyArgumentList.getArgByNameOrIndex("on", 1)
        //val howRef = pyArgumentList.getArgByNameOrIndex("how", 2)
        //val howField = howRef?.let { resolver.parseColumn(howRef) } ?: "inner"

        return sourceSchema?.join(otherDf)
      }
      "to" -> {
        val schemaArg = arguments.firstOrNull() ?: return null
        SparkStatisticPython.logSchema(node, SparkDataFrameCreateSource.EXPLICIT_SCHEMA)
        return schemaResolver.resolve(schemaArg)
      }
      "unionByName" -> {
        val df2 = arguments.firstOrNull() ?: return null
        val schema2 = resolve(df2)
        return sourceSchema?.unionByName(schema2)
      }
      "unpivot" -> {
        val ids = argumentList?.getArgByNameOrIndex("ids", 0) ?: return null
        val newColumn1 = argumentList.getArgByNameOrIndex("variableColumnName", 2)?.let { columnResolver.parseColumn(it) } ?: return null
        val newColumn2 = argumentList.getArgByNameOrIndex("valueColumnName", 3)?.let { columnResolver.parseColumn(it) } ?: return null
        val sourceColumns = columnResolver.parseColumnOrColumns(ids).takeIf { it.isNotEmpty() } ?: return null
        SparkStatisticPython.logSchema(node, SparkDataFrameCreateSource.DATAFRAME_TRANSFORM_PARTIAL)
        return sourceSchema?.unpivot(sourceColumns, newColumn1, newColumn2)
      }
      "withColumns" -> {
        val arg = arguments.firstOrNull() ?: return null
        val dict = resolveCustom(context, arg) { element, _ ->
          if (element is PyDictLiteralExpression) {
            true to element
          }
          else false to null
        }
        val newKeys = dict?.elements?.mapNotNull { resolveString(context, it.key) } ?: emptyList()
        SparkStatisticPython.logSchema(node, SparkDataFrameCreateSource.DATAFRAME_TRANSFORM_PARTIAL)
        return sourceSchema?.withColumns(newKeys)
      }
      "withColumnsRenamed" -> {
        val arg = arguments.firstOrNull() ?: return null
        val dict = resolveCustom(context, arg) { element, _ ->
          if (element is PyDictLiteralExpression) {
            true to element
          }
          else false to null
        }
        val renameKeys = dict?.elements?.mapNotNull {
          val source = resolveString(context, it.key) ?: return@mapNotNull null
          val target = it.value?.let { value -> resolveString(context, value) } ?: return@mapNotNull null
          source to target
        } ?: emptyList()
        SparkStatisticPython.logSchema(node, SparkDataFrameCreateSource.DATAFRAME_TRANSFORM_PARTIAL)
        return sourceSchema?.withColumnsRenamed(renameKeys)
      }
      "toDF" -> {
        val args = columnResolver.parseColumns(arguments)
        SparkStatisticPython.logSchema(node, SparkDataFrameCreateSource.DATAFRAME_TRANSFORM)
        return PySparkSchemaInfo.create(false, args)
      }
      "intersectAll", "intersect", "subtract" -> {
        //TODO: Add error handler
        return sourceSchema
      }
      "na" -> {
        return sourceSchema
      }
      "rollup", "groupBy" -> {
        val args = columnResolver.parseColumns(arguments)
        SparkStatisticPython.logSchema(node, SparkDataFrameCreateSource.DATAFRAME_TRANSFORM_PARTIAL)
        return sourceSchema?.groupBy(args)
      }
      "sampleBy" -> {
        val id = arguments.firstOrNull() ?: return null
        val parseColumn = columnResolver.parseColumn(id) ?: return null
        SparkStatisticPython.logSchema(node, SparkDataFrameCreateSource.DATAFRAME_TRANSFORM)
        return sourceSchema?.select(listOf(parseColumn))
      }
      "selectExpr" -> {
        val columns = columnResolver.parseColumns(arguments)
        SparkStatisticPython.logSchema(node, SparkDataFrameCreateSource.DATAFRAME_TRANSFORM)
        return PySparkSchemaInfo.create(isPartial = false, columns)
      }
      "mapInPandas", "mapInArrow", "melt", "observe" -> {
        //TODO: experemental low priority
        return null
      }
      else -> return null
    }
  }

  private fun resolveDataFrameFromGroupedData(node: PyReferenceExpression, sourceSchema: PySparkSchemaInfo?): PySparkSchemaInfo? {
    val arguments = (node.nextSibling as? PyArgumentList)?.arguments ?: emptyArray()

    val args = columnResolver.parseColumns(arguments)
    return when (node.referencedName) {
      "pivot" -> sourceSchema?.finishGroupingColumn(null)
      "agg" -> sourceSchema?.finishGrouping(args)
      "count" -> sourceSchema?.finishGroupingColumn(node.referencedName)
      "mean" -> sourceSchema?.finishGroupingColumn("avg($args)")
      else -> sourceSchema?.finishGroupingColumn("${node.referencedName}($args)")
    }
  }

  private fun resolveDataFrameFromNaFunctions(node: PyReferenceExpression, sourceSchema: PySparkSchemaInfo?): PySparkSchemaInfo? =
    when (node.referencedName) {
      "drop", "fill", "replace" -> sourceSchema
      else -> null
    }

  private fun resolveDataFrameFromRdd(node: PyReferenceExpression): PySparkSchemaInfo? {
    if (node.referencedName != "toDF")
      return null

    val arg = (node.nextSibling as? PyArgumentList) ?: return null

    val parseSchema = schemaResolver.resolve(arg)
    if (parseSchema != null)
      SparkStatisticPython.logSchema(node, SparkDataFrameCreateSource.EXPLICIT_SCHEMA)
    return parseSchema
  }


  private fun resolveDataFrameFromSparkSession(node: PyReferenceExpression): PySparkSchemaInfo? {
    return when (node.referencedName) {
      "createDataFrame" -> {
        val args: PyArgumentList = node.nextSibling as? PyArgumentList ?: return null
        val schemaArg = args.getArgByNameOrIndex("schema", 1)
        val dataArg = args.getArgByNameOrIndex("data", 0)

        val dfBySchema = schemaArg?.let { schemaResolver.resolve(it) }
        val dfByRow = dataArg?.let { SchemaByDataResolver(context).resolve(it) }

        val resultSchema = dfBySchema ?: dfByRow
        if (resultSchema != null)
          SparkStatisticPython.logSchema(node, SparkDataFrameCreateSource.EXPLICIT_SCHEMA)
        resultSchema
      }
      "range" -> {
        SparkStatisticPython.logSchema(node, SparkDataFrameCreateSource.EXPLICIT_SCHEMA)
        PySparkSchemaInfo.create(false, listOf("id"))
      }
      else -> null
    }
  }

  companion object {
    fun resolveDfSchema(psiElement: PsiElement, context: TypeEvalContext): PySparkSchemaInfo? {
      return PySparkDataFrameResolver(context).resolve(psiElement)
    }
  }
}