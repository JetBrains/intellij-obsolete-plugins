package com.intellij.bigdatatools.plugin.spark.python.submit.inspections.resolver

import com.intellij.bigdatatools.plugin.spark.python.submit.inspections.model.PySparkSchemaInfo
import com.intellij.bigdatatools.plugin.spark.python.submit.inspections.resolver.PySparkConsts.STRUCT_FIELD_CLASS_NAME
import com.intellij.bigdatatools.plugin.spark.python.submit.inspections.resolver.PySparkUtils.getArgByNameOrIndex
import com.intellij.openapi.util.removeUserData
import com.intellij.psi.PsiElement
import com.jetbrains.python.psi.PyCallExpression
import com.jetbrains.python.psi.PyExpression
import com.jetbrains.python.psi.PySequenceExpression
import com.jetbrains.python.psi.StringLiteralExpression
import com.jetbrains.python.psi.types.TypeEvalContext

class PySparkSchemaDefinitionResolver(context: TypeEvalContext) : PySparkPsiResolver<PySparkSchemaInfo>(context) {
  private val columnResolver = PySparkColumnResolver(context)

  override fun beforeProcess(node: PsiElement): Pair<Boolean, PySparkSchemaInfo?> {
    node.getUserData(PySparkConsts.SCHEMA_DEFINITION_KEY)?.let {
      if (node.getUserData(PySparkConsts.KEY_STAMP) != node.containingFile.modificationStamp)
        node.removeUserData(PySparkConsts.SCHEMA_DEFINITION_KEY)
      else
        return true to it
    }

    return false to null
  }

  override fun customProcess(node: PsiElement, invokeNode: PsiElement?) = when (node) {
    is PySequenceExpression -> {
      val columns = columnResolver.parseColumns(node.elements)
      val parsedColumns = PySparkSchemaInfo.create(isPartial = false, columns)
      true to parsedColumns
    }
    is PyCallExpression -> {
      val type = node.typeFqnName
      val schema = if (type == PySparkConsts.STRUCT_TYPE_CLASS_NAME) {
        parseSchemaStruct(node)
      }
      else {
        null
      }
      true to schema
    }
    is StringLiteralExpression -> {
      val stringValue = node.stringValue
      val columns = stringValue.split(",").map { it.trim().split(" ").first() }
      true to PySparkSchemaInfo.create(isPartial = false, columns)
    }
    else -> false to null
  }

  override fun afterProcess(node: PsiElement, data: PySparkSchemaInfo?): PySparkSchemaInfo? {
    node.putUserData(PySparkConsts.SCHEMA_DEFINITION_KEY, data)
    node.putUserData(PySparkConsts.KEY_STAMP, node.containingFile.modificationStamp)
    return data
  }


  private fun parseSchemaStruct(structCallExpression: PyCallExpression): PySparkSchemaInfo? {
    val structArgs: Array<PyExpression> = structCallExpression.arguments
    if (structArgs.isEmpty())
      return PySparkSchemaInfo.create(isPartial = false, emptyList())
    val structListArg = structCallExpression.arguments.first()
    val listStructField = resolveCustom(context, structListArg) { node, _ ->
      if (node !is PySequenceExpression)
        false to null
      else
        true to node
    } ?: return null

    val structFieldsRaw = listStructField.elements
    val columns = structFieldsRaw.mapNotNull {
      val structFieldColumn = resolveCallExpressionWithType(context, it, STRUCT_FIELD_CLASS_NAME)
      val schemaColumn = structFieldColumn?.getArgByNameOrIndex("name", 0) ?: return@mapNotNull null
      val schemaColumnString = resolveString(context, schemaColumn)
      schemaColumnString
    }

    return PySparkSchemaInfo.create(isPartial = false, columns)
  }
}
