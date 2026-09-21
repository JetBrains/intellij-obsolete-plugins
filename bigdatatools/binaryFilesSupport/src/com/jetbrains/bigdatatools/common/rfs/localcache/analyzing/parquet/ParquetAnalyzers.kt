@file:Suppress("IO_FILE_USAGE")

package com.jetbrains.bigdatatools.common.rfs.localcache.analyzing.parquet

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.io.FileUtil
import com.jetbrains.bigdatatools.common.data.StructuredFilesUtil
import com.jetbrains.bigdatatools.common.rfs.driver.FileInfo
import com.jetbrains.bigdatatools.common.rfs.io.RfsIoUtils.downloadToTempFile
import com.jetbrains.bigdatatools.common.rfs.io.RfsIoUtils.read
import com.jetbrains.bigdatatools.common.rfs.localcache.DecompiledTableWriter
import com.jetbrains.bigdatatools.common.rfs.localcache.analyzing.parquet.util.MergedIterator
import com.jetbrains.bigdatatools.common.rfs.localcache.analyzing.parquet.util.plus
import org.apache.hadoop.conf.Configuration
import org.apache.parquet.VersionParser
import org.apache.parquet.bytes.BytesInput
import org.apache.parquet.bytes.BytesUtils
import org.apache.parquet.column.ColumnDescriptor
import org.apache.parquet.column.impl.ColumnReaderImpl
import org.apache.parquet.column.page.DataPage
import org.apache.parquet.column.page.DataPageV1
import org.apache.parquet.column.page.DataPageV2
import org.apache.parquet.column.page.DictionaryPage
import org.apache.parquet.column.page.PageReadStore
import org.apache.parquet.column.page.PageReader
import org.apache.parquet.compression.CompressionCodecFactory
import org.apache.parquet.example.data.simple.convert.GroupRecordConverter
import org.apache.parquet.format.PageType
import org.apache.parquet.format.Statistics
import org.apache.parquet.format.Util
import org.apache.parquet.format.converter.ParquetMetadataConverter
import org.apache.parquet.hadoop.CodecFactory
import org.apache.parquet.hadoop.ParquetFileReader
import org.apache.parquet.hadoop.ParquetFileWriter
import org.apache.parquet.hadoop.metadata.ColumnChunkMetaData
import org.apache.parquet.hadoop.metadata.ParquetMetadata
import org.apache.parquet.io.LocalInputFile
import org.apache.parquet.io.ParquetDecodingException
import org.apache.parquet.io.api.Converter
import org.apache.parquet.io.api.PrimitiveConverter
import org.apache.parquet.schema.LogicalTypeAnnotation.DateLogicalTypeAnnotation
import org.apache.parquet.schema.LogicalTypeAnnotation.DecimalLogicalTypeAnnotation
import org.apache.parquet.schema.LogicalTypeAnnotation.IntLogicalTypeAnnotation
import org.apache.parquet.schema.LogicalTypeAnnotation.TimeUnit
import org.apache.parquet.schema.LogicalTypeAnnotation.TimestampLogicalTypeAnnotation
import org.apache.parquet.schema.MessageType
import org.apache.parquet.schema.PrimitiveType.PrimitiveTypeName.INT32
import org.apache.parquet.schema.PrimitiveType.PrimitiveTypeName.INT64
import org.apache.parquet.schema.Type
import java.io.ByteArrayInputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.Collections
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * User: Dmitry.Naydanov
 */
abstract class ParquetDownloaderBase {
  companion object {
    internal val logger = Logger.getInstance(this::class.java)

    @JvmStatic
    protected fun selectPrimitiveConverter(baseConverter: Converter, schema: Type, descriptor: ColumnDescriptor): PrimitiveConverter? {
      var currentType: Type = schema
      var currentConverter: Converter = baseConverter

      for (fieldName in descriptor.path) {
        val groupType = currentType.asGroupType()
        val fieldIndex = groupType.getFieldIndex(fieldName)
        currentType = groupType.getType(fieldName)
        currentConverter = currentConverter.asGroupConverter().getConverter(fieldIndex)
      }

      return currentConverter.asPrimitiveConverter()
    }
  }

  abstract fun downloadFullPage(resultFileConstructor: (() -> File)? = null): Pair<File, List<Int>>
  abstract fun getSchema(): SchemaTree
  abstract fun release()

  protected fun readColumnPage(index: Int, reader: PageReader, schema: MessageType, createdBy: String): Iterator<String> {
    val columnDescriptor = schema.columns[index]
    val version = try {
      VersionParser.parse(createdBy)
    }
    catch (e: Exception) {
      null
    }
    val baseConverter = GroupRecordConverter(schema)

    val columnReader = ColumnReaderImpl(
      columnDescriptor,
      reader,
      selectPrimitiveConverter(baseConverter.rootConverter, schema, columnDescriptor),
      version
    )

    val tpe = getSchema().getByIndex(index)?.tpe
    val typeAnnotation = tpe?.logicalTypeAnnotation
    val primitiveTypeName = columnDescriptor.primitiveType.primitiveTypeName

    val type2stringPrinter = when {
      typeAnnotation is IntLogicalTypeAnnotation -> {
        val width = typeAnnotation.bitWidth
        if (width < 64) Int32Printer() else Int64Printer()
      }
      typeAnnotation is DecimalLogicalTypeAnnotation -> {
        val scale = typeAnnotation.scale

        when (primitiveTypeName) {
          INT32 -> Decimal32Printer(scale)
          INT64 -> Decimal64Printer(scale)
          else -> DecimalBinaryPrinter(scale)
        }
      }
      typeAnnotation is TimestampLogicalTypeAnnotation && primitiveTypeName == INT64 -> when (typeAnnotation.unit) {
        TimeUnit.MILLIS -> TimeMillisPrinter(typeAnnotation.isAdjustedToUTC)
        TimeUnit.MICROS -> TimeMicrosPrinter(typeAnnotation.isAdjustedToUTC)
        TimeUnit.NANOS -> TimeNanoPrinter(typeAnnotation.isAdjustedToUTC)
        else -> ValuePrintersUtil.chooseByColumnType(primitiveTypeName)
      }
      typeAnnotation is DateLogicalTypeAnnotation && primitiveTypeName == INT32 ->
        Date32Printer()
      else -> ValuePrintersUtil.chooseByColumnType(primitiveTypeName)
    }

    val typeOfColumn = TypeOfColumn(
      tpe?.repetition == Type.Repetition.OPTIONAL,
      columnDescriptor.primitiveType.repetition == Type.Repetition.REPEATED
    )
    return ColumnIterator(columnReader, type2stringPrinter, reader.totalValueCount, typeOfColumn)
  }

  protected fun openFile(file: FileInfo): ParquetFileReader {
    val downloadedFile = file.downloadToTempFile(0, file.length, false)

    //val configuration = Configuration().apply {
    //  HdfsCommonUtils.updatePluginClassLoaderIfExists(this)
    //  this.set("fs.hdfs.impl", DistributedFileSystem::class.java.name)
    //  this.set("fs.file.impl", LocalFileSystem::class.java.name)
    //}

    //return withPluginClassLoader {
     return ParquetFileReader.open(LocalInputFile(downloadedFile.toPath()))
      //ParquetFileReader.open(HadoopInputFile.fromPath(Path(downloadedFile.canonicalPath), configuration))
    //}
  }
}

class ParquetSimpleDownloader(private val fileInfo: FileInfo,
                              private val resultFileExtension: String = StructuredFilesUtil.TABLE_EXTENSION) : ParquetDownloaderBase() {
  companion object {
    private const val EMPTY_BLOCKS_LIMIT = 100
  }

  private var schemaTree: SchemaTree? = null

  override fun downloadFullPage(resultFileConstructor: (() -> File)?): Pair<File, List<Int>> {
    val tempFile = resultFileConstructor?.invoke() ?: File(
      FileUtil.createTempDirectory("RFSParquet", fileInfo.name),
      fileInfo.name.replace('.', '_') + ".$resultFileExtension"
    )

    val reader = openFile(fileInfo)
    val fileMetaData = reader.fileMetaData
    val schema = fileMetaData.schema
    schemaTree = SchemaTree(schema)

    val createdBy = fileMetaData.createdBy
    val columns = schema.columns

    val iterators = columns.indices.map { i ->
      Pair(i, Collections.emptyIterator<String>())
    }.toMutableList()

    val rowGroupSize = reader.rowGroups.size
    for (rowGroup in 0 until rowGroupSize) {
      val page = nextRowGroup(reader) ?: continue

      for (i in columns.indices) {
        val pageReader = page.getPageReader(columns[i])
        val currentColumn = readColumnPage(i, pageReader, schema, createdBy)
        iterators[i] = Pair(iterators[i].first, iterators[i].second.plus(currentColumn))
      }
    }
    val columnNames = columns.columnHeaders()

    // For 'Map' schema columns, its two separate columns for key and value. Need to merge 2 columns
    val mapColumnIndexes = getMapColumnIndexes(columnNames)
    for (i in mapColumnIndexes) {
      // Merge two columns
      iterators[i] = Pair(iterators[i].first, MergedIterator(iterators[i].second, iterators[i + 1].second))
      iterators.removeAt(i + 1)

      // Merge column headers
      columnNames[i] = columns[i].path.dropLast(2).last()
      columnNames.removeAt(i + 1)
    }

    val offsets = DecompiledTableWriter.write(tempFile, columnNames.toTypedArray(), iterators, 10000)
    return Pair(tempFile, offsets)
  }

  private fun getMapColumnIndexes(columnHeaders: List<String>): List<Int> {
    val indexes = mutableListOf<Int>()
    for (i in 0 until columnHeaders.size - 1) {
      if (columnHeaders[i] == "key" && columnHeaders[i + 1] == "value")
        indexes.add(i)
    }
    return indexes
  }

  private fun List<ColumnDescriptor>.columnHeaders() = this.map { column ->
    val path = column.path
    return@map if (path.last() != "array") path.last() else path.dropLast(1).last()
  }.toMutableList()

  private fun nextRowGroup(reader: ParquetFileReader): PageReadStore? {
    for (i in 1..EMPTY_BLOCKS_LIMIT) {
      try {
        return reader.readNextRowGroup()
      }
      catch (e: RuntimeException) {
        reader.skipNextRowGroup()
      }
    }
    return null
  }

  override fun getSchema(): SchemaTree = schemaTree!!

  override fun release() {}
}

class ParquetPartsDownloader(private val file: FileInfo, private val baseFraction: Float = 0.1f,
                             private val blockNum: Int = 0, private val entriesLimit: Int = 10000,
                             resultFileExtension: String = StructuredFilesUtil.TABLE_EXTENSION) : ParquetDownloaderBase() {
  companion object {
    private const val MAX_DOWNLOAD_ATTEMPTS = 5
    private const val PAGE_SIZE_MIN = 50240
    private const val PAGE_SIZE_SANE = 5 * 1024 * 1024
    private const val FILE_IN_MEMORY_LIMIT = 102400
    private const val FOOTER_LENGTH_SIZE = 4L
    private val MAGIC_SIZE = ParquetFileWriter.MAGIC.size.toLong()

    private val DEFAULT_PARQUET_FILTER = ParquetMetadataConverter.NO_FILTER

    private fun footerLengthIndex(file: FileInfo) = file.length - FOOTER_LENGTH_SIZE - MAGIC_SIZE
  }

  private val codecFactory = CodecFactory(Configuration(), 0)
  private val metadataConverter = ParquetMetadataConverter()
  private val storageHandler: StorageHandlerBase = FileStorageHandler(file, resultFileExtension) //DI ?
  private val metadata: ParquetMetadata by lazy {
    try {
      metadataConverter.readParquetMetadata(downloadFooter(), DEFAULT_PARQUET_FILTER)
    }
    catch (_: Exception) {
      openFile(file).footer
    }
  }

  private val createdBy: String by lazy {
    metadata.fileMetaData.createdBy
  }

  private val columns: List<ColumnChunkMetaData> by lazy {
    if (metadata.blocks.size > blockNum) metadata.blocks[blockNum].columns else emptyList()
  }

  private val schema: MessageType by lazy {
    metadata.fileMetaData.schema
  }

  private val schemaTree: SchemaTree by lazy {
    SchemaTree(schema)
  }

  /**
   * @return size of the footer
   */
  private fun findFooterLength(): Int {
    //TODO there will be NPE here in case of remote FS issues, are we prepared for this?
    val footerLength = file.read(footerLengthIndex(file), 4).result
    return BytesUtils.readIntLittleEndian(footerLength, 0)
  }

  private fun downloadFooter(): InputStream {
    val footerLength = findFooterLength()

    if (footerLength < FILE_IN_MEMORY_LIMIT) {
      //TODO there will be NPE here in case of remote FS issues, are we prepared for this?
      val footer = file.read(footerLengthIndex(file) - footerLength.toLong(), footerLength).resultOrThrow()
      return ByteArrayInputStream(footer.array())
    }

    val tempFile = file.downloadToTempFile(file.length, footerLength.toLong(), true)
    return tempFile.inputStream()
  }

  private fun downloadSamples() {
    fun calculateOffsets(): List<DownloadDescriptor> {
      return columns.withIndex().map { (i, c) ->
        DownloadDescriptor(
          i, c.startingPos,
          max((c.totalSize * baseFraction).toDouble().roundToInt(), PAGE_SIZE_MIN)
        )
      }
    }

    download(calculateOffsets())
  }

  private fun download(downloadData: Collection<DownloadDescriptor>) {
    if (downloadData.isEmpty()) return
    downloadData.forEach { storageHandler.download(it) }
  }

  private fun processSample(i: Int): Chunk {
    val primitiveType = schema.getType(*schema.columns[i].path).asPrimitiveType()

    //todo - error if null?
    val inputStream = storageHandler.getPart(i) ?: return BrokenChunk(i, 0, PAGE_SIZE_MIN)

    val totalPartLength = storageHandler.getPartLength(i)
    val totalSize = columns[i].totalSize

    var processedSize = 0


    inputStream.use { input ->
      @Suppress("UNUSED_VARIABLE") var dataPage: DataPage? = null
      @Suppress("UNUSED_VARIABLE") var dictionaryPage: DictionaryPage? = null

      while (processedSize < totalSize) {
        val mark1 = input.available()
        if (mark1 < 15)
          return BrokenChunk(i, columns[i].startingPos + totalPartLength, PAGE_SIZE_MIN * 3)

        val pageHeader = try {
          Util.readPageHeader(input)
        }
        catch (e: Exception) {
          logger.error(
            "Got exception $e while reading page header in column num $i with startPos = ${columns[i].startingPos} " +
            "dic page found ${dictionaryPage != null}; downloaded: $totalPartLength; " +
            "total size: ${columns[i].totalSize}"
          )
          throw e
        }
        val mark2 = input.available()

        val sizeMissed = mark1 - mark2 + pageHeader.compressed_page_size.toLong() + processedSize - totalPartLength

        if (sizeMissed > 0L) {
          val columnOffset = columns[i].startingPos
          return BrokenChunk(i, columnOffset + totalPartLength,
                             (if (totalSize < PAGE_SIZE_SANE) totalSize else sizeMissed).toInt())
        }

        fun readBytes(length: Int): BytesInput {
          val buffer = ByteArray(length)
          val actLength = input.read(buffer)
          return BytesInput.from(buffer, 0, actLength)
        }

        when (pageHeader.type) {
          PageType.DICTIONARY_PAGE -> {
            val dicHeader = pageHeader.getDictionary_page_header()
            dictionaryPage = DictionaryPage(
              readBytes(pageHeader.compressed_page_size),
              pageHeader.uncompressed_page_size,
              dicHeader.getNum_values(),
              metadataConverter.getEncoding(dicHeader.getEncoding())
            )
          }
          PageType.DATA_PAGE -> {
            val dataHeaderV1 = pageHeader.getData_page_header()
            val statistics = dataHeaderV1.getStatistics() ?: Statistics()

            dataPage = DataPageV1(
              readBytes(pageHeader.compressed_page_size),
              dataHeaderV1.getNum_values(),
              pageHeader.uncompressed_page_size,
              metadataConverter.fromParquetStatistics(
                createdBy, statistics, primitiveType
              ),
              metadataConverter.getEncoding(dataHeaderV1.getRepetition_level_encoding()),
              metadataConverter.getEncoding(dataHeaderV1.getDefinition_level_encoding()),
              metadataConverter.getEncoding(dataHeaderV1.getEncoding())
            )
          }
          PageType.DATA_PAGE_V2 -> {
            val dataHeaderV2 = pageHeader.getData_page_header_v2()
            val dataSize = pageHeader.compressed_page_size -
                           dataHeaderV2.getRepetition_levels_byte_length() -
                           dataHeaderV2.getDefinition_levels_byte_length()

            dataPage = DataPageV2(
              dataHeaderV2.getNum_rows(),
              dataHeaderV2.getNum_nulls(),
              dataHeaderV2.getNum_values(),
              readBytes(dataHeaderV2.getRepetition_levels_byte_length()),
              readBytes(dataHeaderV2.getDefinition_levels_byte_length()),
              metadataConverter.getEncoding(dataHeaderV2.getEncoding()),
              readBytes(dataSize),
              pageHeader.uncompressed_page_size,
              metadataConverter.fromParquetStatistics(
                createdBy,
                dataHeaderV2.getStatistics() ?: Statistics(),
                primitiveType),
              dataHeaderV2.isIs_compressed
            )
          }
          PageType.INDEX_PAGE -> {
          }
          else -> {
            //todo debug HEX ?
            //todo move stream pointer?
          }
        }

        if (dataPage != null)
          return if (dataPage.valueCount > 0)
            ReaderPageChunk(i, SinglePageReader(dataPage, dictionaryPage, codecFactory.getDecompressor(columns[i].codec)))
          else
            NullChunk

        processedSize += pageHeader.compressed_page_size
      }

      return SinglePageChunk(i, 0, totalSize.toInt()) //todo probably this cannot be ?
    }
  }

  private fun processSamples(): Pair<List<ValidChunk>, List<BrokenChunk>> {
    val validChunks = mutableListOf<ValidChunk>()
    val brokenChunks = mutableListOf<BrokenChunk>()

    for (i in 0 until storageHandler.getCount()) {
      when (val c = processSample(i)) {
        is ValidChunk -> validChunks.add(c)
        is BrokenChunk -> brokenChunks.add(c)
      }
    }

    return Pair(validChunks, brokenChunks)
  }

  private fun fixChunks(chunks: Collection<BrokenChunk>) {
    download(chunks.map { DownloadDescriptor(it.index, it.offset, it.missedLength, true) })
  }

  override fun getSchema(): SchemaTree = schemaTree

  override fun downloadFullPage(resultFileConstructor: (() -> File)?): Pair<File, List<Int>> {
    storageHandler.setResultConstructor(resultFileConstructor)

    downloadSamples()
    val (valid, broken) = processSamples()

    fun fixBroken(): List<ValidChunk> {
      val repaired = mutableListOf<ValidChunk>()
      fixChunks(broken)
      var fixed = broken.map { processSample(it.index) }.toMutableList()

      var j = 0

      do {
        ++j
        val i = fixed.iterator()

        while (i.hasNext()) {
          val c = i.next()
          if (c is ValidChunk) {
            repaired.add(c)
            i.remove()
          }
        }

        val broken_ = fixed.filterIsInstance<BrokenChunk>()
        fixChunks(broken_)
        fixed = broken_.map { processSample(it.index) }.toMutableList()
      }
      while (fixed.isNotEmpty() && j < MAX_DOWNLOAD_ATTEMPTS)

      if (fixed.isNotEmpty()) {
        logger.warn("File $file wasn't downloaded after 5 attempts; incomplete columns: ${fixed.map { (it as? BrokenChunk)?.index }}")
      }

      return repaired
    }

    val repaired = fixBroken()

    val columnIterators = (valid + repaired).map {
      if (it is ReaderPageChunk)
        Pair(it.index, readColumnPage(it))
      else
        Pair(Int.MAX_VALUE, Collections.emptyIterator())
    }

    val offsets = DecompiledTableWriter.write(
      storageHandler.getResultFile(),
      columns.map { it.path.last() }.toTypedArray(), columnIterators, entriesLimit
    )

    return Pair(storageHandler.getResultFile(), offsets)
  }

  override fun release() {
    codecFactory.release()
  }

  private fun readColumnPage(chunk: ReaderPageChunk): Iterator<String> =
    try {
      readColumnPage(chunk.index, chunk.reader, schema, createdBy)
    }
    catch (e: Exception) {
      logger.error("Got exception $e in chunk $chunk")
      emptyList<String>().listIterator()
    }
}

private interface Chunk
private interface ValidChunk : Chunk
private data class SinglePageChunk(val index: Int, val offset: Int, val length: Int) : ValidChunk
private data class ReaderPageChunk(val index: Int, val reader: PageReader) : ValidChunk
private data class BrokenChunk(val index: Int, val offset: Long, val missedLength: Int) : Chunk
private object NullChunk : ValidChunk

private class SinglePageReader(
  private val compressedPage: DataPage,
  private val compressedDictionaryPage: DictionaryPage?,
  private val decompressor: CompressionCodecFactory.BytesInputDecompressor) : PageReader {
  override fun readDictionaryPage(): DictionaryPage? {
    if (compressedDictionaryPage == null) return null

    try {
      return DictionaryPage(
        decompressor.decompress(compressedDictionaryPage.bytes, compressedDictionaryPage.uncompressedSize),
        compressedDictionaryPage.dictionarySize,
        compressedDictionaryPage.encoding)
    }
    catch (e: IOException) {
      throw ParquetDecodingException("Could not decompress dictionary page", e)
    }
  }

  override fun getTotalValueCount(): Long = compressedPage.valueCount.toLong()

  override fun readPage(): DataPage {
    return compressedPage.accept(object : DataPage.Visitor<DataPage> {
      override fun visit(dataPageV1: DataPageV1): DataPage {
        try {
          return DataPageV1(
            decompressor.decompress(dataPageV1.bytes, dataPageV1.uncompressedSize),
            dataPageV1.valueCount,
            dataPageV1.uncompressedSize,
            dataPageV1.statistics,
            dataPageV1.rlEncoding,
            dataPageV1.dlEncoding,
            dataPageV1.valueEncoding)
        }
        catch (e: IOException) {
          throw ParquetDecodingException("could not decompress page", e)
        }
      }

      override fun visit(dataPageV2: DataPageV2): DataPage {
        if (!dataPageV2.isCompressed) {
          return dataPageV2
        }
        try {
          val uncompressedSize = Math.toIntExact(
            dataPageV2.uncompressedSize.toLong()
            - dataPageV2.definitionLevels.size()
            - dataPageV2.repetitionLevels.size())
          return DataPageV2.uncompressed(
            dataPageV2.rowCount,
            dataPageV2.nullCount,
            dataPageV2.valueCount,
            dataPageV2.repetitionLevels,
            dataPageV2.definitionLevels,
            dataPageV2.dataEncoding,
            decompressor.decompress(dataPageV2.data, uncompressedSize),
            dataPageV2.statistics
          )
        }
        catch (e: IOException) {
          throw ParquetDecodingException("could not decompress page", e)
        }
      }
    })
  }
}
