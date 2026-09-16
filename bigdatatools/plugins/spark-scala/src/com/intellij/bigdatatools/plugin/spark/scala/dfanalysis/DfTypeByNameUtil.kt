package com.intellij.bigdatatools.plugin.spark.scala.dfanalysis

import com.intellij.bigdatatools.plugin.spark.assistance.dfanalysis.DoubleType
import com.intellij.bigdatatools.plugin.spark.assistance.dfanalysis.IntegerType
import com.intellij.bigdatatools.plugin.spark.assistance.dfanalysis.LongType
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.PsiReference
import org.jetbrains.plugins.scala.lang.psi.api.expr.ScExpression
import org.jetbrains.plugins.scala.lang.psi.api.expr.ScMethodCall

/**
 * Provides expected data frame type change by function name
 *
 * See DfTypeChange
 */
object DfTypeByNameUtil {
  private val instance = DfTypeByNameImpl()

  fun getProvider(): DfTypeProvider = instance
}

interface DfTypeProvider {
  fun findTypeChange(expr: ScExpression): DfTypeChange?
}

class DfTypeByNameImpl : DfTypeProvider {
  override fun findTypeChange(expr: ScExpression): DfTypeChange? {
    val callParent = (expr as PsiElement).parent
    if (callParent !is ScMethodCall) return null

    val resolved = (expr as? PsiReference)?.resolve() ?: return null
    val name = (resolved as? PsiNamedElement)?.name ?: return null
    val descriptors = DfTypeFunctionsDescriptors.descList[name] ?: return null

    for (descriptor in descriptors) {
      val change = descriptor.change.invoke().produceChange(callParent)
      if (isNotEmpty(change)) return change
    }

    return null
  }

  private fun isNotEmpty(change: DfTypeChange?): Boolean {
    if (change == null) return false
    if (change !is DfMultipleTypeChanges) return true

    return change.changes.any { isNotEmpty(it) }
  }
}

object DfTypeFunctionsDescriptors {
  /**
   * Descriptor for a function that defines or modifies one or more mappings ColumnName -> ColumnType
   *
   * @param name   function name
   * @param change type or/and name change descriptor
   */
  data class TypeFunctionDescriptor(val name: String, val change: () -> DfTypeChangeDescriptor)

  val descList = listOf(
    TypeFunctionDescriptor("withColumn") {
      DfSuppressedTypeChangeDescriptor(DfIntroduceTypeDescriptor("colName", "col"))
    },
    TypeFunctionDescriptor("join") {
      DfMergeSchemaChangeDescriptor("right", "joinExprs")
    },
    TypeFunctionDescriptor("drop") { DfDeleteColumnDescriptor("colName") },
    TypeFunctionDescriptor("drop") { DfVarargTypeChangeDescriptor(DfDeleteColumnDescriptor("colNames")) },
    TypeFunctionDescriptor("withColumnRenamed") { DfRenameColumnDescriptor("existingName", "newName") },
    TypeFunctionDescriptor("mean") { DfFixedTypeDescriptor("columnName", IntegerType, false) },
    TypeFunctionDescriptor("max") { DfFixedTypeDescriptor("columnName", IntegerType, false) },
    TypeFunctionDescriptor("min") { DfFixedTypeDescriptor("columnName", IntegerType, false) },
    TypeFunctionDescriptor("avg") { DfFixedTypeDescriptor("columnName", IntegerType, false) },
    TypeFunctionDescriptor("sum") {
      DfFixedTypeDescriptor("columnName", IntegerType, false)
    }, // TODO this one probably should be a computable type
    TypeFunctionDescriptor("sumDistinct") {
      DfFixedTypeDescriptor("columnName", IntegerType, false)
    }, // TODO this one probably should be a computable type
    TypeFunctionDescriptor("approx_count_distinct") { DfFixedTypeDescriptor("columnName", LongType, false) },
    TypeFunctionDescriptor("count") { DfFixedTypeDescriptor("columnName", LongType, false) },
    TypeFunctionDescriptor("countDistinct") { DfFixedTypeDescriptor("columnName", LongType, false) },
    TypeFunctionDescriptor("kurtosis") { DfFixedTypeDescriptor("columnName", DoubleType, false) },
    TypeFunctionDescriptor("skewness") { DfFixedTypeDescriptor("columnName", DoubleType, false) },
    TypeFunctionDescriptor("stddev") { DfFixedTypeDescriptor("columnName", DoubleType, false) },
    TypeFunctionDescriptor("stddev_samp") { DfFixedTypeDescriptor("columnName", DoubleType, false) },
    TypeFunctionDescriptor("stddev_pop") { DfFixedTypeDescriptor("columnName", DoubleType, false) },
    TypeFunctionDescriptor("variance") { DfFixedTypeDescriptor("columnName", DoubleType, false) },
    TypeFunctionDescriptor("var_samp") { DfFixedTypeDescriptor("columnName", DoubleType, false) },
    TypeFunctionDescriptor("var_pop") { DfFixedTypeDescriptor("columnName", DoubleType, false) },
    TypeFunctionDescriptor("select") {
      DfCompoundTypeChangeDescriptor(
        listOf(
          DfSelectColumnDescriptor("col"),
          DfVarargTypeChangeDescriptor(DfSelectColumnDescriptor("cols"), isOptional = true)
        )
      )
    },
    TypeFunctionDescriptor("select") {
      DfAggregatingChangeDescriptor(
        DfVarargTypeChangeDescriptor(DfContextTypeChangeDescriptor("cols")),
        listOf(DfSelectColumnDescriptor("cols"))
      ) { name, v ->
        if (name == "cols" && v is TypedPair) v.first ?: v else v
      }
    },
    TypeFunctionDescriptor("select") {
      DfAggregatingChangeDescriptor(
        DfVarargTypeChangeDescriptor(
          DfSuppressedSingleTypeChangeDescriptor(
            DfRecursiveTypeChangeDescriptor("cols")
          )
        ),
        listOf(DfSelectColumnDescriptor("cols"))
      ) { name, v ->
        if (name == "cols" && v is DfIntroduceType) v.columnName else v
      }
    },
    TypeFunctionDescriptor("select") {
      DfAggregatingChangeDescriptor(
        DfVarargTypeChangeDescriptor(DfRecursiveTypeChangeDescriptor("c1")),
        listOf(DfSelectColumnDescriptor("cols"))
      ) { name, v ->
        if (name == "cols" && v is DfIntroduceType) v.columnName else v
      }
    },
  ).groupBy { it.name }
}