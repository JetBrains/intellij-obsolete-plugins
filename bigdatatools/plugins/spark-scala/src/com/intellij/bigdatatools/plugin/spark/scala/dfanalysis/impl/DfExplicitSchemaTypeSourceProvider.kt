package com.intellij.bigdatatools.plugin.spark.scala.dfanalysis.impl

import com.intellij.bigdatatools.plugin.spark.assistance.dfanalysis.DfTypeSchema
import com.intellij.bigdatatools.plugin.spark.assistance.dfanalysis.DfTypeSource
import com.intellij.bigdatatools.plugin.spark.assistance.dfanalysis.ParseUtil
import com.intellij.bigdatatools.plugin.spark.assistance.dfanalysis.SimpleTypeSource
import com.intellij.bigdatatools.plugin.spark.assistance.statistic.SparkDataFrameCreateSource
import com.intellij.bigdatatools.plugin.spark.assistance.statistic.SparkStatisticScala
import com.intellij.bigdatatools.plugin.spark.scala.SparkScalaMessagesBundle
import com.intellij.bigdatatools.plugin.spark.scala.dfanalysis.expr.DfRecursiveExprFolder
import com.intellij.psi.PsiElement
import org.jetbrains.plugins.scala.lang.psi.api.expr.ScMethodCall

class DfExplicitSchemaTypeSourceProvider : DfAbstractMethodBasedTypeProvider() {
  companion object {
    private const val METHOD_NAME = "schema"
    private const val BASE_CLASS_NAME = "DataFrameReader"

    private const val STRING_PARAMETER_NAME = "schemaString"
    private const val STRUCT_PARAMETER_NAME = "schema"
  }

  override fun getTypeSource(psiElement: PsiElement): DfTypeSource? {
    if (!checkMethodCallExpr(psiElement, setOf(METHOD_NAME), setOf(BASE_CLASS_NAME))) return null
    val methodCall = psiElement.parent as? ScMethodCall ?: return null

    fun shortDesc(src: String): String =
      SparkScalaMessagesBundle.message("df.extracted.from.ts.description", DfComputingUtil.trimTextForDescription(src))

    foldStringParam(methodCall, STRING_PARAMETER_NAME)?.let { schemaString ->
      val schema = ParseUtil.parseDdlString(schemaString)
      if (schema.map.isEmpty()) {
        return null
      }
      SparkStatisticScala.logSchema(psiElement, SparkDataFrameCreateSource.EXPLICIT_SCHEMA)
      return SimpleTypeSource(schema, shortDesc(schemaString))
    }

    foldParamWith(methodCall, STRUCT_PARAMETER_NAME) { DfRecursiveExprFolder().foldDdlStructType(it) }?.let { tpeList ->
      SparkStatisticScala.logSchema(psiElement, SparkDataFrameCreateSource.EXPLICIT_SCHEMA)
      return SimpleTypeSource(DfTypeSchema(DfComputingUtil.linkedHashMapOf(tpeList)), shortDesc((methodCall as PsiElement).text))
    }

    return null
  }
}