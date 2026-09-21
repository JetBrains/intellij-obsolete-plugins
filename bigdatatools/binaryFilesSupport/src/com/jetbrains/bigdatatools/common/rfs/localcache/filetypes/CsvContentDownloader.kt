package com.jetbrains.bigdatatools.common.rfs.localcache.filetypes

import com.intellij.charts.dataframe.analyzing.AnalysisFail
import com.intellij.charts.dataframe.analyzing.CsvFileAnalyzer
import com.intellij.charts.dataframe.analyzing.CsvSuccessBase
import com.intellij.openapi.project.Project
import com.jetbrains.bigdatatools.common.data.StructuredFilesUtil
import com.jetbrains.bigdatatools.common.data.dataframe.analyzing.PagedContentProvider
import com.jetbrains.bigdatatools.common.rfs.client.ErrorInSchemaInfoPart
import com.jetbrains.bigdatatools.common.rfs.client.SchemaInfoPart
import com.jetbrains.bigdatatools.common.rfs.driver.FileInfo
import com.jetbrains.bigdatatools.common.rfs.icons.RfsIcons
import com.jetbrains.bigdatatools.common.rfs.localcache.MetaInfo
import com.jetbrains.bigdatatools.common.rfs.localcache.PlainContentDownloader
import com.jetbrains.bigdatatools.common.rfs.localcache.SimpleMetaInfo
import java.io.File
import javax.swing.Icon

/**
 * User: Dmitry.Naydanov
 * Date: 2018-11-09.
 */
class CsvContentDownloader(private val project: Project) : PlainContentDownloader() {
  override fun accept(fileInfo: FileInfo): Boolean = fileInfo.name.endsWith(".csv") ||
                                                     fileInfo.name.endsWith(".csv${StructuredFilesUtil.DOWNLOADED_SUFFIX}")

  override suspend fun extractMeta(fileInfo: FileInfo, tempFile: File): MetaInfo {
    val simpleMeta = super.extractMeta(fileInfo, tempFile)

    val scheme = when (val extracted = CsvFileAnalyzer(3).analyze(
      PagedContentProvider(fileInfo, project))) { //not 100% clear
      is CsvSuccessBase -> extracted.result.map {
        CsvSchemaInfoPart("${it.name} : ${it.type.presentableName}", it.name, it.type.presentableName)
      }
      is AnalysisFail -> listOf(ErrorInSchemaInfoPart(extracted.errorMessage))
      else -> emptyList()
    }

    return SimpleMetaInfo(scheme = simpleMeta.scheme + scheme,
                          errors = simpleMeta.errors, fileInfo = fileInfo)
  }

  override fun contentExtension(fileInfo: FileInfo): String = StructuredFilesUtil.TABLE_EXTENSION

  private class CsvSchemaInfoPart(override val text: String, val name: String = "", val tpe: String = "") : SchemaInfoPart {
    override val icon: Icon
      get() = RfsIcons.META_PART_ICON
    override val onClick: (() -> Unit)?
      get() = null

    override fun equals(other: Any?): Boolean {
      if (this === other) return true
      if (javaClass != other?.javaClass) return false

      return text == (other as CsvSchemaInfoPart).text
    }

    override fun typeString(): String = tpe

    override fun nameString(): String = name

    override fun hashCode(): Int = text.hashCode()
  }
}