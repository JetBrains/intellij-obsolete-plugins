package com.intellij.bigdatatools.plugin.spark.python.submit.inspections.resolver

import com.intellij.psi.PsiElement
import com.jetbrains.python.psi.PyCallExpression
import com.jetbrains.python.psi.PyElement
import com.jetbrains.python.psi.PyExpression
import com.jetbrains.python.psi.PyNumericLiteralExpression
import com.jetbrains.python.psi.PyReferenceExpression
import com.jetbrains.python.psi.PySequenceExpression
import com.jetbrains.python.psi.PyStarArgument
import com.jetbrains.python.psi.PySubscriptionExpression
import com.jetbrains.python.psi.PyTargetExpression
import com.jetbrains.python.psi.PyTypedElement
import com.jetbrains.python.psi.StringLiteralExpression
import com.jetbrains.python.psi.types.TypeEvalContext

class PySparkColumnResolver(context: TypeEvalContext) : AbstractPySparkResolver(context) {
  fun parseColumns(elements: Array<out PyExpression>): List<String> {
    val firstElement = elements.firstOrNull()?.let { getInvokeNode(it) }
    return when {
      firstElement is PyStarArgument -> {
        val ref = firstElement.children.firstOrNull() as? PyReferenceExpression ?: return emptyList()
        val source = getInvokeNode(ref) as? PySequenceExpression ?: return emptyList()
        parseColumns(source.elements)
      }
      elements.size == 1 && firstElement is PySequenceExpression -> {
        firstElement.elements.mapNotNull {
          parseColumn(it)
        }
      }
      else -> elements.mapNotNull {
        parseColumn(it)
      }
    }
  }


  fun parseColumnOrColumns(element: PyElement): List<String> {
    val psiElement = resolveReferences(element)
    return if (psiElement is PySequenceExpression) {
      parseColumns(psiElement.elements)
    }
    else {
      listOfNotNull(parseColumn(psiElement))
    }
  }

  fun parseColumn(element: PsiElement): String? {
    return when (element) {
      is StringLiteralExpression -> element.stringValue.removeSuffix("`").removePrefix("`")
      is PyNumericLiteralExpression -> element.firstChild?.text ?: element.text
      is PyCallExpression -> {
        val typeFqnName = element.typeFqnName
        if (typeFqnName != PySparkConsts.COLUMN_CLASS_NAME)
          return null
        val callee = element.callee as? PyReferenceExpression ?: return null
        return when (val referencedName = callee.referencedName) {
          "col", "expr", "lit" -> {
            val colEl = element.arguments.firstOrNull() ?: return null
            return parseColumn(colEl)
          }
          "getItem", "getField" -> {
            val prevChild = element.firstChild?.firstChild as? PyTypedElement ?: return null
            val prevColumn = parseColumn(prevChild) ?: return null

            val colEl = element.arguments.firstOrNull() ?: return null
            val colName = parseColumn(colEl) ?: return null
            return "$prevColumn[$colName]"

          }
          "count_distinct", "countDistinct" -> {
            val columns = element.arguments.mapNotNull { parseColumn(it) }
            "count(DISTINCT ${columns.joinToString(separator = ", ") { it }})"
          }
          "cast", "asc", "desc" -> {
            val argument = element.arguments.firstOrNull()
            if (argument != null)
              return parseColumn(argument)
            val prevChild = element.firstChild?.firstChild as? PyTypedElement ?: return null
            parseColumn(prevChild)

          }
          "log" -> {
            val colEl = element.arguments.firstOrNull() ?: return null
            val colName = parseColumn(colEl) ?: return null
            "ln(${colName})"
          }
          "variance" -> {
            val colEl = element.arguments.firstOrNull() ?: return null
            val colName = parseColumn(colEl) ?: return null
            "var_samp(${colName})"
          }
          "alias" -> {
            val colEl = element.arguments.firstOrNull() ?: return null
            parseColumn(colEl)
          }
          "explode" -> {
            return "col"
          }
          else -> {
            val columns = element.arguments.mapNotNull { parseColumn(it) }
            "${referencedName}(${columns.joinToString(separator = ", ") { it }})"
          }
        }
      }
      is PySubscriptionExpression -> {
        val operand = element.rootOperand as? PyTypedElement ?: return null
        val type = operand.typeFqnName ?: return null
        val index = element.indexExpression as? PyTypedElement ?: return null
        return when (type) {
          PySparkConsts.COLUMN_CLASS_NAME, PySparkConsts.DATAFRAME_CLASS_NAME -> {
            parseColumn(index)
          }
          else -> null
        }
      }
      is PyReferenceExpression -> {
        val qualifier = getInvokeNode(element) as? PyTypedElement ?: return null
        val type = qualifier.typeFqnName ?: return null
        return when (type) {
          PySparkConsts.DATAFRAME_CLASS_NAME -> element.referencedName
          PySparkConsts.COLUMN_CLASS_NAME -> parseColumn(qualifier)
          else -> parseColumn(qualifier)
        }
      }
      is PyTargetExpression -> {
        val assigned = element.findAssignedValue() as? PyTypedElement ?: return null
        return parseColumn(assigned)
      }
      else -> null
    }
  }
}
