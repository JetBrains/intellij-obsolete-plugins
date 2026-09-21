package com.jetbrains.bigdatatools.common.rfs.localcache.filetypes

import com.intellij.bigdatatools.coreUi.util.MessagesBundle
import com.jetbrains.bigdatatools.common.data.StructuredFilesUtil
import com.jetbrains.bigdatatools.common.rfs.driver.FileInfo
import com.jetbrains.bigdatatools.common.rfs.driver.RfsPath
import com.jetbrains.bigdatatools.common.rfs.localcache.CachedContent
import com.jetbrains.bigdatatools.common.rfs.localcache.DecompiledTableWriter
import com.jetbrains.bigdatatools.common.rfs.localcache.MagicAware
import com.jetbrains.bigdatatools.common.rfs.localcache.MetaInfo
import com.jetbrains.bigdatatools.common.rfs.localcache.PlainContentDownloader
import com.jetbrains.bigdatatools.common.rfs.localcache.RfsDownloadedStorageManager
import com.jetbrains.bigdatatools.common.rfs.localcache.SimpleMetaInfo
import com.jetbrains.bigdatatools.common.rfs.localcache.analyzing.TableOffsetsMap
import com.jetbrains.bigdatatools.common.rfs.localcache.metainfo.AvroMetaInfoProvider
import com.jetbrains.bigdatatools.common.util.toPresentableText
import org.apache.avro.Schema
import org.apache.avro.file.DataFileStream
import org.apache.avro.generic.GenericDatumReader
import org.apache.avro.generic.GenericRecord
import java.io.File

class AvroDownloader : PlainContentDownloader(), MagicAware by Util {
  override fun accept(fileInfo: FileInfo) = Util.accept(fileInfo.path)

  override suspend fun extractMeta(fileInfo: FileInfo, tempFile: File): MetaInfo = try {
    if (fileInfo.length == 0L) {
      SimpleMetaInfo(errors = listOf("Schema cannot be calculated - file size is 0"), fileInfo = fileInfo)
    }
    else {
      val scheme = AvroMetaInfoProvider().getMetaInfo(fileInfo)
      SimpleMetaInfo(scheme = scheme.toList(), fileInfo = fileInfo)
    }
  }
  catch (t: Throwable) {
    SimpleMetaInfo(errors = listOf(t.toPresentableText()), fileInfo = fileInfo)
  }

  override fun contentExtension(fileInfo: FileInfo): String = StructuredFilesUtil.AVRO_EXT

  override suspend fun download(fileInfo: FileInfo,
                        storageManager: RfsDownloadedStorageManager,
                        pageNum: Long,
                        withMeta: Boolean,
                        allowEmpty: Boolean): CachedContent {
    if (fileInfo.isFile && fileInfo.length == 0L) {
      throw Exception(MessagesBundle.message("avro.file.is.broken.length.zero", fileInfo.name))
    }

    val tmp = storageManager.touch(fileInfo, pageNum)
    val input = fileInfo.readStream(0, null).result?.buffered()

    val offsets = DataFileStream<GenericRecord>(input, GenericDatumReader()).use { dataFileStream ->
      // Avro file should have RECORD type schema - BDIDE-4945
      val recordSchema = when (dataFileStream.schema.type) {
        Schema.Type.RECORD -> dataFileStream.schema
        Schema.Type.UNION -> dataFileStream.schema.types.firstOrNull { it.type == Schema.Type.RECORD } ?: isNotRecordSchema(fileInfo)
        else -> isNotRecordSchema(fileInfo)
      }

      val fields = recordSchema.fields.map { f -> f.name() }.toTypedArray()
      val data = dataFileStream
        .asIterable()
        .asSequence()
        .take(1000)
        .map { rec ->
          fields.map { rec.get(it)?.toString() ?: "null" }
        }.iterator()

      DecompiledTableWriter.write(tmp, fields, data, entriesLimit = 0)
    }

    return CachedContent(null, tmp, TableOffsetsMap(offsets))
  }

  private fun isNotRecordSchema(fileInfo: FileInfo): Nothing = throw Exception(
    MessagesBundle.message("avro.file.should.have.record.schema.type", fileInfo.name))

  object Util : MagicAware {
    fun accept(rfsPath: RfsPath) = rfsPath.name.endsWith(".avro", ignoreCase = true) ||
                                   rfsPath.name.endsWith(".avro${StructuredFilesUtil.DOWNLOADED_SUFFIX}", ignoreCase = true)

    override fun checkMagic(bytes: ByteArray) = bytes[0].toInt().toChar() == 'O' &&
                                                bytes[1].toInt().toChar() == 'b' &&
                                                bytes[2].toInt().toChar() == 'j' &&
                                                bytes[3] == 1.toByte()
  }
}
