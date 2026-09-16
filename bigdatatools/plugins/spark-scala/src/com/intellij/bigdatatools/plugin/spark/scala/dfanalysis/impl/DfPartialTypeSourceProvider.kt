package com.intellij.bigdatatools.plugin.spark.scala.dfanalysis.impl

import com.intellij.bigdatatools.plugin.spark.assistance.dfanalysis.DfTypeSource
import com.intellij.bigdatatools.plugin.spark.assistance.dfanalysis.EmptyTypeSource
import com.intellij.bigdatatools.plugin.spark.assistance.statistic.SparkDataFrameCreateSource
import com.intellij.bigdatatools.plugin.spark.assistance.statistic.SparkStatisticScala
import com.intellij.bigdatatools.plugin.spark.scala.dfanalysis.DfTypeSourceProvider
import com.intellij.psi.PsiElement
import org.jetbrains.plugins.scala.lang.psi.api.expr.ScMethodCall
import org.jetbrains.plugins.scala.lang.psi.api.statements.ScValueOrVariableDefinition
import org.jetbrains.plugins.scala.lang.psi.api.statements.params.ScParameter
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.ScNamedElement

class DfPartialTypeSourceProvider : DfTypeSourceProvider {
  override fun getTypeSource(psiElement: PsiElement): DfTypeSource? {
    if (psiElement !is ScParameter && psiElement !is ScMethodCall && psiElement !is ScValueOrVariableDefinition) return null
    if (!DfComputingUtil.isDataFrameType(psiElement)) return null
    val name = (psiElement as? ScNamedElement)?.name ?: return null // todo probably we can also handle cases like {abstract val schema = ???; override val schema = ...}

    SparkStatisticScala.logSchema(psiElement, SparkDataFrameCreateSource.UNKNOWN_SCHEMA)
    return EmptyTypeSource("From $name, exact schema unknown", if (psiElement is ScParameter) name else "")
  }
}