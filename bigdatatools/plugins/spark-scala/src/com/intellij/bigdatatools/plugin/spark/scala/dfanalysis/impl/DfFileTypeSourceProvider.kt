package com.intellij.bigdatatools.plugin.spark.scala.dfanalysis.impl

import com.intellij.bigdatatools.plugin.spark.assistance.dfanalysis.DfTypeSchema
import com.intellij.bigdatatools.plugin.spark.assistance.dfanalysis.DfTypeSource
import com.intellij.bigdatatools.plugin.spark.assistance.dfanalysis.ParseUtil
import com.intellij.bigdatatools.plugin.spark.assistance.dfanalysis.SimpleTypeSource
import com.intellij.bigdatatools.plugin.spark.assistance.dfanalysis.inlay.DataframeSampleFileService
import com.intellij.bigdatatools.plugin.spark.assistance.dfanalysis.inlay.DataframeSampleFileService.Companion.SUPPORTED_READ_METHODS
import com.intellij.bigdatatools.plugin.spark.assistance.statistic.SparkDataFrameCreateSource
import com.intellij.bigdatatools.plugin.spark.assistance.statistic.SparkStatisticScala
import com.intellij.bigdatatools.plugin.spark.scala.SparkScalaMessagesBundle
import com.intellij.openapi.components.service
import com.intellij.psi.PsiElement
import com.intellij.psi.createSmartPointer
import com.jetbrains.bigdatatools.common.rfs.localcache.RfsDownloadedStorageManager
import org.jetbrains.plugins.scala.lang.psi.api.expr.ScMethodCall

class DfFileTypeSourceProvider : DfAbstractMethodBasedTypeProvider() {
  companion object {
    private val LOADING_METHODS = SUPPORTED_READ_METHODS

    private const val BASE_CLASS_NAME = "DataFrameReader"
    const val BASE_PARAMETER_NAME = "path"

  }

  override fun getTypeSource(psiElement: PsiElement): DfTypeSource? {
    if (!checkMethodCallExpr(psiElement, LOADING_METHODS, setOf(BASE_CLASS_NAME))) return null

    val methodCall = psiElement.parent as ScMethodCall
    val key = methodCall.createSmartPointer()
    val inlayData = psiElement.project.service<DataframeSampleFileService>().getAttached(key)
    val (schema, fullPath) = inlayData?.let { ParseUtil.parseDdlString(it.schemaDdlString) to it.externalPath } ?: run {
      val fullPath = foldStringParam(methodCall, BASE_PARAMETER_NAME) ?: return null
      val path = fullPath.indexOf("://").let { if (it == -1) fullPath else fullPath.substring(it + 3) }
      val downloadedStorageManager = service<RfsDownloadedStorageManager>()
      downloadedStorageManager.searchInRelativePathCache(path)?.let { ParseUtil.parseMeta(it.scheme) to fullPath } ?: return null
    }

    if (schema == null) return null
    val source = if (inlayData != null) SparkDataFrameCreateSource.INLAY_SCHEMA else SparkDataFrameCreateSource.PARSE_FILE_SCHEMA
    SparkStatisticScala.logSchema(psiElement, source)
    return FileTypeSource(schema, SparkScalaMessagesBundle.message("df.extracted.from.ts.description", fullPath))
  }
}

class FileTypeSource(schema: DfTypeSchema, description: String) : SimpleTypeSource(schema, description)