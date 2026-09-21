package com.jetbrains.bigdatatools.common.rfs.localcache.filetypes

import com.intellij.openapi.components.service
import com.jetbrains.bigdatatools.common.data.StructuredFilesUtil
import com.jetbrains.bigdatatools.common.rfs.client.SchemaInfoPart
import com.jetbrains.bigdatatools.common.rfs.driver.FileInfo
import com.jetbrains.bigdatatools.common.rfs.driver.RfsPath
import com.jetbrains.bigdatatools.common.rfs.icons.RfsIcons
import com.jetbrains.bigdatatools.common.rfs.localcache.CachedContent
import com.jetbrains.bigdatatools.common.rfs.localcache.MagicAware
import com.jetbrains.bigdatatools.common.rfs.localcache.MetaInfo
import com.jetbrains.bigdatatools.common.rfs.localcache.PlainContentDownloader
import com.jetbrains.bigdatatools.common.rfs.localcache.RfsDownloadedStorageManager
import com.jetbrains.bigdatatools.common.rfs.localcache.SimpleMetaInfo
import com.jetbrains.bigdatatools.common.rfs.localcache.analyzing.TableOffsetsMap
import com.jetbrains.bigdatatools.common.rfs.localcache.analyzing.parquet.ParquetDownloaderBase
import com.jetbrains.bigdatatools.common.rfs.localcache.analyzing.parquet.ParquetPartsDownloader
import com.jetbrains.bigdatatools.common.rfs.localcache.analyzing.parquet.ParquetSimpleDownloader
import com.jetbrains.bigdatatools.common.rfs.localcache.analyzing.parquet.SerializableSchemaTree
import java.io.File
import javax.swing.Icon

/**
 * User: Dmitry.Naydanov
 * Date: 2018-11-11.
 */
class ParquetContentDownloader : PlainContentDownloader(), MagicAware by Util {
  object Util : MagicAware {
    fun accept(rfsPath: RfsPath): Boolean = rfsPath.name.endsWith(".parquet", ignoreCase = true) ||
                                            rfsPath.name.endsWith(".parquet${StructuredFilesUtil.DOWNLOADED_SUFFIX}", ignoreCase = true)

    override fun checkMagic(bytes: ByteArray): Boolean = bytes.toString(Charsets.UTF_8) == "PAR1"
  }

  companion object {
    private const val FILE_LENGTH_LIMIT = 1024 * 1024L
  }

  override fun accept(fileInfo: FileInfo): Boolean = Util.accept(fileInfo.path)

  override suspend fun extractMeta(fileInfo: FileInfo, tempFile: File): MetaInfo {
    return download(fileInfo, service(), 0, true, true).meta ?: super.extractMeta(fileInfo, tempFile)
  }

  override suspend fun download(fileInfo: FileInfo,
                        storageManager: RfsDownloadedStorageManager,
                        pageNum: Long,
                        withMeta: Boolean,
                        allowEmpty: Boolean): CachedContent {

    val downloader = createDownloader(fileInfo)

    return try {
      val (tempFile, offsets) = downloader.downloadFullPage { storageManager.touch(fileInfo, pageNum) }
      val parquetSchema = ParquetSchemaRoot(downloader.getSchema().makeSerializable()).children
      val parquetMetaInfo = ParquetMetaInfo(parquetSchema, errors = emptyList(), fileInfo)

      CachedContent(parquetMetaInfo, tempFile, TableOffsetsMap(offsets))
    }
    catch (e: Exception) {
      storageManager.clearPage(fileInfo, pageNum)
      throw e
    }
    finally {
      downloader.release()
    }
  }

  override fun contentExtension(fileInfo: FileInfo): String = StructuredFilesUtil.PARQUET_EXT

  private fun createDownloader(fileInfo: FileInfo): ParquetDownloaderBase =
    if (fileInfo.length > FILE_LENGTH_LIMIT)
      ParquetPartsDownloader(fileInfo, resultFileExtension = contentExtension(fileInfo))
    else
      ParquetSimpleDownloader(fileInfo, resultFileExtension = contentExtension(fileInfo))

  private class ParquetSchemaChild(private val node: SerializableSchemaTree.SerializableSchemaNode) : SchemaInfoPart {
    override val text: String = node.tpeString
    override val icon: Icon
      get() = RfsIcons.META_PART_ICON
    override val onClick: (() -> Unit)?
      get() = null

    override fun hasChildren(): Boolean = node.hasChildren()

    override val children: List<SchemaInfoPart> = ParquetSchemaRoot.createChildren(node)

    override fun typeString(): String = node.primType

    override fun nameString(): String = node.columnName
  }

  private class ParquetSchemaRoot(private val schema: SerializableSchemaTree) : SchemaInfoPart {
    companion object {
      fun createChildren(node: SerializableSchemaTree.SerializableSchemaNode) =
        node.getChildren().map { ParquetSchemaChild(it) }
    }

    override val text: String
      get() = ""
    override val icon: Icon?
      get() = null
    override val onClick: (() -> Unit)?
      get() = null

    override val children: List<SchemaInfoPart> = createChildren(schema.getRoot())
    override fun hasChildren(): Boolean = schema.getRoot().hasChildren()

    override fun typeString(): String = ""

    override fun nameString(): String = ""
  }

  private class ParquetMetaInfo(scheme: List<SchemaInfoPart>, errors: List<String>, fileInfo: FileInfo)
    : SimpleMetaInfo(scheme, errors, fileInfo)
}