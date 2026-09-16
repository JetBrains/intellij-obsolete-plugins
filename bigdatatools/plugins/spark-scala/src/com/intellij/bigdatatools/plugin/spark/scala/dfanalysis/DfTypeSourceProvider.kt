package com.intellij.bigdatatools.plugin.spark.scala.dfanalysis

import com.intellij.bigdatatools.plugin.spark.assistance.dfanalysis.DfTypeSource
import com.intellij.bigdatatools.plugin.spark.assistance.dfanalysis.SimpleTypeSource
import com.intellij.bigdatatools.plugin.spark.assistance.statistic.SparkDataFrameCreateSource
import com.intellij.bigdatatools.plugin.spark.assistance.statistic.SparkStatisticScala
import com.intellij.bigdatatools.plugin.spark.scala.dfanalysis.error.DfTypeCheckError
import com.intellij.bigdatatools.plugin.spark.scala.dfanalysis.impl.DfComputingUtil
import com.intellij.bigdatatools.plugin.spark.scala.dfanalysis.impl.DfConvertFromContextTypeSourceProvider
import com.intellij.bigdatatools.plugin.spark.scala.dfanalysis.impl.DfExplicitSchemaTypeSourceProvider
import com.intellij.bigdatatools.plugin.spark.scala.dfanalysis.impl.DfFileTypeSourceProvider
import com.intellij.bigdatatools.plugin.spark.scala.dfanalysis.impl.DfPartialTypeSourceProvider
import com.intellij.bigdatatools.plugin.spark.scala.dfanalysis.impl.DfScalaCollectionTypeSourceProvider
import com.intellij.psi.PsiElement

class CheckingTypeSource(private val delegate: DfTypeSource, val errors: List<DfTypeCheckError>) : DfTypeSource by delegate

interface DfTypeSourceProvider {
  companion object {
    private fun getAll() = listOf(
      DfFileTypeSourceProvider(),
      DfExplicitSchemaTypeSourceProvider(),
      DfScalaCollectionTypeSourceProvider(),
      DfConvertFromContextTypeSourceProvider(),
      DfPartialTypeSourceProvider()
    ) //EP_NAME.extensionList - probably there is no need to retrieve providers via EP right now

    fun findTypeSource(psiElement: PsiElement): DfTypeSource? {
      for (provider in getAll()) {
        val source = provider.getTypeSource(psiElement)
        if (source != null) return source
      }

      return null
    }

    fun createSourceFromTypeContext(context: DfTypeContext, psiElement: PsiElement): DfTypeSource {
      SparkStatisticScala.logSchema(psiElement, SparkDataFrameCreateSource.DATAFRAME_TRANSFORM)
      return SimpleTypeSource(context.toSchema(), DfComputingUtil.psiElementToDescription(psiElement))
    }
  }

  fun getTypeSource(psiElement: PsiElement): DfTypeSource?
}

