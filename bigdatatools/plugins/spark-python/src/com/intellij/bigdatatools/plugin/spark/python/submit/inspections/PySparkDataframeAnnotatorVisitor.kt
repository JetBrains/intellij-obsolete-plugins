package com.intellij.bigdatatools.plugin.spark.python.submit.inspections

import com.intellij.bigdatatools.plugin.spark.assistance.statistic.SparkDataFrameInspection
import com.intellij.bigdatatools.plugin.spark.assistance.statistic.SparkStatisticPython
import com.intellij.bigdatatools.plugin.spark.assistance.util.SAMessagesBundle
import com.intellij.bigdatatools.plugin.spark.python.submit.PySparkRegistry
import com.intellij.bigdatatools.plugin.spark.python.submit.inspections.model.PySparkSchemaInfo
import com.intellij.bigdatatools.plugin.spark.python.submit.inspections.resolver.PySparkColumnResolver
import com.intellij.bigdatatools.plugin.spark.python.submit.inspections.resolver.PySparkConsts
import com.intellij.bigdatatools.plugin.spark.python.submit.inspections.resolver.PySparkDataFrameResolver
import com.intellij.bigdatatools.plugin.spark.python.submit.inspections.resolver.PySparkUtils
import com.intellij.bigdatatools.plugin.spark.python.submit.inspections.resolver.PySparkUtils.getArgByNameOrIndex
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.util.NlsSafe
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.python.inspections.PyInspectionVisitor
import com.jetbrains.python.psi.PyArgumentList
import com.jetbrains.python.psi.PyCallExpression
import com.jetbrains.python.psi.PyElement
import com.jetbrains.python.psi.PyExpression
import com.jetbrains.python.psi.PyReferenceExpression
import com.jetbrains.python.psi.PySequenceExpression
import com.jetbrains.python.psi.PyStringLiteralExpression
import com.jetbrains.python.psi.PySubscriptionExpression
import com.jetbrains.python.psi.PyUtil
import com.jetbrains.python.psi.types.PyClassType
import com.jetbrains.python.psi.types.TypeEvalContext

class PySparkDataframeAnnotatorVisitor(holder: ProblemsHolder, val context: TypeEvalContext) : PyInspectionVisitor(holder, context) {
  private val resolver = PySparkDataFrameResolver(context)

  override fun visitPyReferenceExpression(node: PyReferenceExpression) {
    super.visitPyReferenceExpression(node)

    val qualifier = node.qualifier ?: return
    val refName = node.referencedName ?: return
    val schema = resolver.resolve(qualifier) ?: return
    if (schema.isPartial)
      return

    val argumentList = (node.nextSibling as? PyArgumentList)
    val isField = node.nextSibling == null
    if (isField) {
      highlightField(node, schema, refName)
    }
    else {
      argumentList ?: return
      when (getType(qualifier)) {
        PySparkConsts.DATAFRAME_CLASS_NAME -> highlightDfMethod(refName, argumentList, schema)
        PySparkConsts.GROUP_DATA_CLASS_NAME -> highlightGroupMethod(refName, argumentList, schema)
      }
    }
  }

  override fun visitPySubscriptionExpression(node: PySubscriptionExpression) {
    super.visitPySubscriptionExpression(node)

    val rootOperand = node.rootOperand
    val indexExpression = node.indexExpression ?: return
    val classQName = getType(rootOperand) ?: return
    if (classQName != PySparkConsts.DATAFRAME_CLASS_NAME)
      return

    val resolvedIndex = PyUtil.resolveToTheTop(indexExpression)
    val columnName = PySparkColumnResolver(context).parseColumn(resolvedIndex) ?: return
    val schema = resolver.resolve(rootOperand) ?: return
    highlightField(node, schema, columnName)
  }

  override fun visitPyElement(node: PyElement) {
    super.visitPyElement(node)

    if (!PySparkRegistry.DEBUG_HIGHLIGHT)
      return

    val schema = resolver.resolve(node)
    if (schema != null)
      registerProblem(node, schema.toString())
  }

  private fun highlightDfMethod(refName: @NlsSafe String, arguments: PyArgumentList, schema: PySparkSchemaInfo) {
    when (refName) {
      "agg", "melt", "unpivot", "withColumnsRenamed" -> return
      "sampleBy", "approxQuantile" -> {
        checkArgByColumn(schema, arguments, "col", 0)
      }
      "withColumnRenamed" -> {
        checkArgByColumn(schema, arguments, "existing", 0)
      }
      "where", "filter" -> {
        val arg = arguments.arguments.firstOrNull() ?: return
        val resolved = PyUtil.resolveToTheTop(arg)
        if (resolved is PyStringLiteralExpression)
          return
        searchColumns(schema, arg)
      }
      "withColumn" -> {
        searchColumnsByColumn(schema, arguments, "col", 1)
      }
      "crosstab", "cov", "corr" -> {
        checkArgByColumn(schema, arguments, "col1", 0)
        checkArgByColumn(schema, arguments, "col2", 1)
      }
      "orderBy", "describe", "cube", "drop" -> {
        arguments.arguments.map {
          checkArg(schema, it)
        }
      }
      "drop_duplicates", "dropDuplicatesWithinWatermark", "dropDuplicates" -> {
        checkArgByColumn(schema, arguments, "subset", 0)
      }
      "dropna" -> {
        checkArgByColumn(schema, arguments, "subset", 2)
      }
      "fillna" -> {
        checkArgByColumn(schema, arguments, "subset", 1)
      }
      "sortWithinPartitions", "sort", "select", "rollup", "groupBy", "freqItems" -> {
        arguments.arguments.map {
          checkArg(schema, it)
        }
      }
      "join" -> {
        checkArgByColumn(schema, arguments, "on", 1)
      }
      "repartition", "repartitionByRange" -> {
        arguments.arguments.drop(1).map {
          checkArg(schema, it)
        }
      }
      "replace" -> {
        checkArgByColumn(schema, arguments, "subset", 2)
      }
      "withMetadata" -> {
        checkArgByColumn(schema, arguments, "columnName", 0)
      }
      else -> return
    }
  }

  private fun highlightGroupMethod(refName: @NlsSafe String, arguments: PyArgumentList, schema: PySparkSchemaInfo) {
    when (refName) {
      "agg" -> {
        //TODO: map
        return
      }
      "avg", "mean", "max", "min", "sum" -> {
        arguments.arguments.map {
          checkArg(schema, it)
        }
      }
      "pivot" -> {
        checkArgByColumn(schema, arguments, "pivot_col", 0)
      }
      else -> return
    }
  }


  private fun checkArgByColumn(schema: PySparkSchemaInfo,
                               arguments: PyArgumentList,
                               columnName: String,
                               index: Int) {
    val arg = arguments.getArgByNameOrIndex(columnName, index) ?: return
    checkArg(schema, arg)
  }

  private fun checkArg(schema: PySparkSchemaInfo,
                       arg: PyExpression) {
    if (arg is PySequenceExpression) {
      arg.elements.forEach {
        checkArg(schema, it)
      }
    }
    else {
      val columns = resolveColumnNames(arg)
      highlightFields(arg, schema, columns)
    }
  }

  private fun resolveColumnNames(arg: PyExpression): List<String> {
    when (val ref = PyUtil.resolveToTheTop(arg)) {
      is PySequenceExpression -> {
        return ref.elements.flatMap { resolveColumnNames(it) }
      }
      is PyCallExpression -> {
        val classFqn = (context.getType(ref) as? PyClassType)?.classQName
        if (classFqn != PySparkConsts.COLUMN_CLASS_NAME)
          return emptyList()
        val pyReferenceExpression = ref.callee as? PyReferenceExpression ?: return emptyList()
        val refName = pyReferenceExpression.referencedName
        if (refName == "alias") {
          val pyExpression = pyReferenceExpression.qualifier ?: return emptyList()
          return resolveColumnNames(pyExpression)
        }
        if (refName in setOf("lit", "colRegex", "alias"))
          return emptyList()

        val args = ref.argumentList?.arguments?.flatMap { resolveColumnNames(it) } ?: return emptyList()
        return args
      }
      is PyStringLiteralExpression -> {
        val column = ref.stringValue.removeSuffix("`").removePrefix("`")
        return listOf(column)
      }
      else -> return emptyList()
    }
  }

  @Suppress("SameParameterValue")
  private fun searchColumnsByColumn(schema: PySparkSchemaInfo,
                                    arguments: PyArgumentList,
                                    columnName: String,
                                    index: Int) {
    val arg = arguments.getArgByNameOrIndex(columnName, index) ?: return
    searchColumns(schema, arg)
  }


  private fun searchColumns(schema: PySparkSchemaInfo, node: PsiElement) {
    val colRefs = PsiTreeUtil.findChildrenOfAnyType(node, false, PyCallExpression::class.java)
      .filter { (it.callee as? PyReferenceExpression)?.referencedName == "col" }
    val refs = colRefs
      .filter { getType(it) == PySparkConsts.COLUMN_CLASS_NAME }
      .mapNotNull { it.argumentList }

    val colNames = refs.mapNotNull {
      val argument = it.arguments.firstOrNull() ?: return@mapNotNull null
      val literal = PySparkUtils.resolveReference(argument, context) as? PyStringLiteralExpression ?: return@mapNotNull null
      val stringValue = literal.stringValue
      if (stringValue.contains("."))
        return@mapNotNull null
      argument to stringValue
    }

    colNames.forEach {
      highlightField(it.first, schema, it.second)
    }
  }

  private fun getType(rootOperand: PyExpression) = (context.getType(rootOperand) as? PyClassType)?.classQName

  private fun highlightFields(node: PsiElement,
                              schema: PySparkSchemaInfo,
                              names: List<@NlsSafe String>) {
    if ("*" in names)
      return
    if (schema.isPartial)
      return

    val notFoundColumns = names - schema.schemaColumns.toSet()
    if (notFoundColumns.isEmpty())
      return

    notFoundColumns.forEach {
      SparkStatisticPython.logInspection(node, it, SparkDataFrameInspection.NON_EXISTING_COLUMN)
    }
    if (names.size == 1) {
      highlightField(node, schema, names.first())
    }
    else {
      registerProblem(node, SAMessagesBundle.message("dataframe.inspection.message.column.not.exist",
                                                   notFoundColumns.joinToString(separator = ", ")))}
  }


  private fun highlightField(node: PsiElement,
                             schema: PySparkSchemaInfo,
                             name: @NlsSafe String) {
    if (name == "*")
      return
    if (schema.isPartial)
      return
    if (schema.schemaColumns.contains(name))
      return

    SparkStatisticPython.logInspection(node, name, SparkDataFrameInspection.NON_EXISTING_COLUMN)

    val internalNode = PsiTreeUtil.findChildOfType(node, PyStringLiteralExpression::class.java, false)
    if (internalNode is PyStringLiteralExpression)
      registerProblem(node, SAMessagesBundle.message("dataframe.inspection.message.column.not.exist", name), PySparkColumnQuickFix())
    else
      registerProblem(node, SAMessagesBundle.message("dataframe.inspection.message.column.not.exist", name))
  }
}