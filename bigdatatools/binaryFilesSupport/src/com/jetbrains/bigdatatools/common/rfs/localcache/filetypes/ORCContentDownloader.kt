package com.jetbrains.bigdatatools.common.rfs.localcache.filetypes

import com.intellij.bigdatatools.coreUi.util.MessagesBundle
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
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
import com.jetbrains.bigdatatools.common.rfs.localcache.analyzing.orc.JetbrainsOrcReader
import com.jetbrains.bigdatatools.common.rfs.localcache.analyzing.orc.JetbrainsOrcRecordReader
import com.jetbrains.bigdatatools.common.rfs.localcache.analyzing.orc.ORCReadException
import com.jetbrains.bigdatatools.common.rfs.localcache.metainfo.ORCMetaInfoProvider
import org.apache.hadoop.fs.FSDataInputStream
import org.apache.hadoop.hive.ql.exec.vector.BytesColumnVector
import org.apache.hadoop.hive.ql.exec.vector.ColumnVector
import org.apache.hadoop.hive.ql.exec.vector.DecimalColumnVector
import org.apache.hadoop.hive.ql.exec.vector.DoubleColumnVector
import org.apache.hadoop.hive.ql.exec.vector.ListColumnVector
import org.apache.hadoop.hive.ql.exec.vector.LongColumnVector
import org.apache.hadoop.hive.ql.exec.vector.MapColumnVector
import org.apache.hadoop.hive.ql.exec.vector.StructColumnVector
import org.apache.hadoop.hive.ql.exec.vector.TimestampColumnVector
import org.apache.hadoop.hive.ql.exec.vector.UnionColumnVector
import org.apache.hadoop.hive.ql.exec.vector.VectorizedRowBatch
import org.apache.hadoop.hive.serde2.io.DateWritable
import org.apache.orc.OrcProto
import org.apache.orc.OrcUtils
import org.apache.orc.TypeDescription
import org.apache.orc.TypeDescription.Category.BINARY
import org.apache.orc.TypeDescription.Category.BOOLEAN
import org.apache.orc.TypeDescription.Category.BYTE
import org.apache.orc.TypeDescription.Category.CHAR
import org.apache.orc.TypeDescription.Category.DATE
import org.apache.orc.TypeDescription.Category.DECIMAL
import org.apache.orc.TypeDescription.Category.DOUBLE
import org.apache.orc.TypeDescription.Category.FLOAT
import org.apache.orc.TypeDescription.Category.INT
import org.apache.orc.TypeDescription.Category.LIST
import org.apache.orc.TypeDescription.Category.LONG
import org.apache.orc.TypeDescription.Category.MAP
import org.apache.orc.TypeDescription.Category.SHORT
import org.apache.orc.TypeDescription.Category.STRING
import org.apache.orc.TypeDescription.Category.STRUCT
import org.apache.orc.TypeDescription.Category.TIMESTAMP
import org.apache.orc.TypeDescription.Category.TIMESTAMP_INSTANT
import org.apache.orc.TypeDescription.Category.UNION
import org.apache.orc.TypeDescription.Category.VARCHAR
import java.io.File

class ORCContentDownloader : PlainContentDownloader(), MagicAware by Util {
  override fun accept(fileInfo: FileInfo): Boolean = Util.accept(fileInfo.path)

  override suspend fun extractMeta(fileInfo: FileInfo, tempFile: File): MetaInfo {
    val scheme = ORCMetaInfoProvider().getMetaInfo(fileInfo)
    return SimpleMetaInfo(scheme = scheme, fileInfo = fileInfo)
  }

  override suspend fun download(fileInfo: FileInfo,
                        storageManager: RfsDownloadedStorageManager,
                        pageNum: Long,
                        withMeta: Boolean,
                        allowEmpty: Boolean): CachedContent {

    val tempFile = storageManager.touch(fileInfo, pageNum)
    val offsets = writeCsv(fileInfo, tempFile)

    val metaInfo = if (withMeta) extractMeta(fileInfo, tempFile) else null
    return CachedContent(metaInfo, tempFile, TableOffsetsMap(offsets))
  }

  override fun contentExtension(fileInfo: FileInfo): String = StructuredFilesUtil.ORC_EXT

  private fun writeCsv(fileInfo: FileInfo, file: File): List<Int> {
    val (footer, batch) = readBatch(fileInfo)
    val fields = OrcUtils.convertTypeFromProtobuf(footer.typesList, 0).children

    val header = OrcUtils.convertTypeFromProtobuf(footer.typesList, 0).fieldNames.toTypedArray()
    val rows = (0 until batch.count())
      .map { r ->
        val row = r.toInt()
        (0 until batch.projectionSize)
          .map { col ->
            val column = batch.cols[batch.projectedColumns[col]]
            fields[col].stringify(row, column) ?: "null"
          }
      }

    return DecompiledTableWriter.write(file, header, rows.iterator(), 0)
  }

  /**
   * Code below is heavily influenced by org.apache.orc.tools.PrintData
   * https://github.com/apache/orc/blob/31ed8b44e4889f15736ad926c8ce6f8286173a41/java/tools/src/java/org/apache/orc/tools/PrintData.java
   * Suddenly there are tons of cornercases when handling data in ORC — for instance dates are being stored in long and should be recovered
   * Also binary data is being encoded to base64
   */
  private fun TypeDescription.stringify(row: Int, column: ColumnVector?): String? {
    return when (category) {
      BOOLEAN -> (column as LongColumnVector).vector[row] != 0L
      BYTE, SHORT, LONG, INT -> (column as LongColumnVector).vector[row]
      FLOAT, DOUBLE -> (column as DoubleColumnVector).vector[row]
      STRING, CHAR, VARCHAR -> (column as BytesColumnVector).toString(row)
      BINARY -> (column as BytesColumnVector)
        .vector[row].map { "%02x".format(it) }
        .asSequence()
        .windowed(2, 2, true)
        .joinToString(" ") { it.joinToString("") }
      DECIMAL -> (column as DecimalColumnVector).vector[row].toString()
      DATE -> DateWritable((column as LongColumnVector).vector[row].toInt())
      TIMESTAMP, TIMESTAMP_INSTANT -> (column as TimestampColumnVector).asScratchTimestamp(row)
      LIST -> stringifyList(column as ListColumnVector, row)
      MAP -> stringifyMap(column as MapColumnVector, row)
      STRUCT -> stringifyStruct(column as StructColumnVector, row)
      UNION -> stringifyUnion(column as UnionColumnVector, row)
      else -> throw IllegalArgumentException("Category $category is of unsupported type for printing")
    }?.toString()
  }

  private fun TypeDescription.stringifyList(vector: ListColumnVector, row: Int): String {
    val childType = children[0]
    val offset = vector.offsets[row].toInt()
    val len = vector.lengths[row]
    return (0 until len)
      .joinToString(", ", prefix = "[", postfix = "]") {
        childType.stringify((offset + it).toInt(), vector.child) ?: ""
      }
  }

  private fun TypeDescription.stringifyMap(vector: MapColumnVector, row: Int): String {
    val keyType = children[0]
    val valueType = children[1]
    val offset = vector.offsets[row].toInt()
    return (0 until vector.lengths[row])
      .joinToString(", ", prefix = "[", postfix = "]") {
        val key = keyType.stringify((offset + it).toInt(), vector.keys)
        val value = valueType.stringify((offset + it).toInt(), vector.values)
        """{"_key": $key, "_value": $value}"""
      }
  }

  private fun TypeDescription.stringifyUnion(vector: UnionColumnVector, row: Int): String? {
    val tag = vector.tags[row]
    return children[tag].stringify(row, vector.fields[tag])
  }

  private fun TypeDescription.stringifyStruct(vector: StructColumnVector, row: Int): String {
    val fieldNames = fieldNames
    val fieldTypes = children
    return fieldTypes.indices.joinToString(", ", "{", "}") { i ->
      "\"${fieldNames[i]}\": \"${fieldTypes[i].stringify(row, vector.fields[i])}\""
    }
  }

  private fun readBatch(fileInfo: FileInfo) =
    JetbrainsOrcReader.withInputStream(fileInfo) { input: FSDataInputStream? ->
      if (input == null)
        throw Exception("Input stream is not found")
      try {
        readBatchFromStream(fileInfo, input)
      }
      catch (e: ORCReadException) {
        Notifications.Bus.notify(Notification("ORC Files",
                                              MessagesBundle.message("notification.orc.reading.error.title"),
                                              MessagesBundle.message("notification.orc.reading.error.message"),
                                              NotificationType.ERROR))
        throw e
      }
    }

  private fun readBatchFromStream(fileInfo: FileInfo,
                                  fsDataInputStream: FSDataInputStream): Pair<OrcProto.Footer, VectorizedRowBatch> {
    val fileLength = fileInfo.length
    val (postscript, postscriptLength) = JetbrainsOrcReader.readPostScript(fileLength, fsDataInputStream)
    val footer = JetbrainsOrcReader.readFooter(postscript, postscriptLength, fileLength, fsDataInputStream)!!
    val stripes = OrcUtils.convertProtoStripesToStripes(footer.stripesList)
    val recordReader = JetbrainsOrcRecordReader(
      footer,
      postscript,
      stripes,
      fsDataInputStream
    )
    val (batch, _) = recordReader.nextBatch(1000)
    return footer to batch
  }

  object Util : MagicAware {
    fun accept(rfsPath: RfsPath) = rfsPath.name.endsWith(".orc", true) ||
                                   rfsPath.name.endsWith(".orc${StructuredFilesUtil.DOWNLOADED_SUFFIX}", true)

    override fun checkMagic(bytes: ByteArray) = bytes.sliceArray(0..2).toString(Charsets.UTF_8) == "ORC"
  }
}
