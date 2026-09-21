package com.jetbrains.bigdatatools.common.rfs.localcache.analyzing.orc

import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.FSDataInputStream
import org.apache.hadoop.hive.ql.exec.vector.VectorizedRowBatch
import org.apache.orc.CompressionKind
import org.apache.orc.OrcFile
import org.apache.orc.OrcProto
import org.apache.orc.OrcProto.StripeFooter
import org.apache.orc.OrcUtils
import org.apache.orc.Reader
import org.apache.orc.RecordReader
import org.apache.orc.StripeInformation
import org.apache.orc.TypeDescription
import org.apache.orc.impl.InStream
import org.apache.orc.impl.OrcCodecPool
import org.apache.orc.impl.OrcIndex
import org.apache.orc.impl.PositionProvider
import org.apache.orc.impl.ReaderImpl
import org.apache.orc.impl.RecordReaderImpl
import org.apache.orc.impl.RecordReaderImpl.PositionProviderImpl
import org.apache.orc.impl.SchemaEvolution
import org.apache.orc.impl.TreeReaderFactory
import org.apache.orc.impl.TreeReaderFactory.TreeReader
import org.apache.orc.impl.reader.ReaderEncryption
import org.apache.orc.impl.reader.StripePlanner
import kotlin.math.min
import kotlin.reflect.jvm.isAccessible

/**
 * This class is almost full copy-paste from [RecordReaderImpl]
 * It's ported to Kotlin and has only several methods which we need, but it proves we don't really need Hadoop's Path
 * and Filesystem concepts to read all the needed data from [FSDataInputStream]
 */
class JetbrainsOrcRecordReader(
    footer: OrcProto.Footer,
    postscript: OrcProto.PostScript,
    stripe: MutableList<StripeInformation>,
    fsDataInputStream: FSDataInputStream
) {
    private var stripeFooter: StripeFooter? = null
    private var rowIndexStride = footer.rowIndexStride.toLong()
    private var rowBaseInStripe: Byte = 0.toByte()
    private var indexes: OrcIndex
    private val reader: TreeReader
    private val fileIncluded: BooleanArray
    private val rows: Long
    private val planner: StripePlanner
    private var includedRowGroups: BooleanArray? = null
    private var rowCountInStripe: Long = 0
    private var rowInStripe: Long = 0
    private var currentStripe = -1
    private val stripes: MutableList<StripeInformation> = mutableListOf()
    private val dataReader: JetbrainsOrcDataReader
    private val schema: TypeDescription
    private val columns: Int

    init {
        val writerVersion =
            writerVersion(
                footer,
                postscript
            )
        val typeDescription = OrcUtils.convertTypeFromProtobuf(footer.typesList, 0)
        val options = Reader.Options()
        val readerOptions =
            OrcFile.ReaderOptions(Configuration())
        val evolution = SchemaEvolution(typeDescription, null, options)
        schema = evolution.readerSchema
        fileIncluded = evolution.fileIncluded
        stripes.addAll(stripe)
      rows = stripes.sumOf { it.numberOfRows }
        val unencryptedOptions = InStream.options()
            .withCodec(OrcCodecPool.getCodec(CompressionKind.valueOf(postscript.compression.name)))
            .withBufferSize(postscript.compressionBlockSize.toInt())

        val readerContext = TreeReaderFactory.ReaderContext()
            .setSchemaEvolution(evolution)
            .skipCorrupt(true)
            .fileFormat(ReaderImpl.getFileVersion(postscript.versionList))
            .useUTCTimestamp(readerOptions.useUTCTimestamp)
            .setEncryption(ReaderEncryption())

        reader = TreeReaderFactory.createTreeReader(schema, readerContext)
        columns = evolution.fileSchema.maximumId + 1
        indexes = OrcIndex(
            arrayOfNulls(columns),
            arrayOfNulls(columns),
            arrayOfNulls(columns)
        )
        dataReader = JetbrainsOrcDataReader(
            unencryptedOptions,
            fsDataInputStream
        )
        planner = StripePlanner(
            evolution.fileSchema,
            ReaderEncryption(),
            dataReader,
            writerVersion,
            false,
            (Int.MAX_VALUE - 1024).toLong()
        )
        advanceToNextRow(reader, 0)
    }

    /**
     * The only public method we really need is to obtain next batch
     * API here slightly differs from one from [RecordReaderImpl] and [RecordReader]
     * because it will
     *
     * @return Pair of [VectorizedRowBatch] with results and information if batch is empty
     */
    fun nextBatch(requestedSize: Int): Pair<VectorizedRowBatch, Boolean> {
        val batch = schema.createRowBatch(requestedSize)

        if (rowInStripe >= rowCountInStripe) {
            currentStripe += 1
            if (currentStripe >= stripes.size) {
                batch.size = 0
                return batch to false
            }
            readStripe()
        }
        val batchSize = computeBatchSize(batch.maxSize.toLong())
        rowInStripe += batchSize.toLong()
        reader.setVectorColumnCount(batch.dataColumnCount)
        reader.nextBatch(batch, batchSize)
        batch.selectedInUse = false
        batch.size = batchSize
        advanceToNextRow(reader, rowInStripe + rowBaseInStripe)
        return batch to (batch.size != 0)
    }

    private fun advanceToNextRow(reader: TreeReader, nextRow: Long): Boolean {
        var nextRowInStripe: Long = nextRow - rowBaseInStripe
        // check for row skipping
        if (rowIndexStride != 0L && includedRowGroups != null && nextRowInStripe < rowCountInStripe) {
            var rowGroup = (nextRowInStripe.toInt() / rowIndexStride.toInt())
            if (!includedRowGroups!![rowGroup]) {
                while (rowGroup < includedRowGroups!!.size && !includedRowGroups!![rowGroup]) {
                    rowGroup += 1
                }
                if (rowGroup >= includedRowGroups!!.size) {
                    advanceStripe()
                    return true
                }
                nextRowInStripe = min(rowCountInStripe, rowGroup * rowIndexStride)
            }
        }
        if (nextRowInStripe >= rowCountInStripe) {
            advanceStripe()
            return true
        }
        if (nextRowInStripe != rowInStripe) {
            if (rowIndexStride != 0L) {
                val rowGroup = (nextRowInStripe.toInt() / rowIndexStride.toInt())
                seekToRowEntry(reader, rowGroup)
                reader.skipRows(nextRowInStripe - rowGroup * rowIndexStride)
            } else {
                reader.skipRows(nextRowInStripe - rowInStripe)
            }
            rowInStripe = nextRowInStripe
        }
        return true
    }

    private fun advanceStripe() {
        rowInStripe = rowCountInStripe
        while (rowInStripe >= rowCountInStripe &&
            currentStripe < stripes.size - 1
        ) {
            currentStripe += 1
            readStripe()
        }
    }

    private fun seekToRowEntry(reader: TreeReader, rowEntry: Int) {
        val rowIndices = indexes.rowGroupIndex
        val index = arrayOfNulls<PositionProvider>(rowIndices.size)
        for (i in index.indices) {
            if (rowIndices[i] != null) {
                index[i] = PositionProviderImpl(rowIndices[i]!!.getEntry(rowEntry))
            }
        }
        reader.seek(index)
    }

    private fun readStripe() {
        val stripe: StripeInformation = beginReadStripe()
        planner.parseStripe(stripe, fileIncluded)
        includedRowGroups = pickRowGroups()

        // move forward to the first unskipped row
        if (includedRowGroups != null) {
            while (rowInStripe < rowCountInStripe &&
                !includedRowGroups!![(rowInStripe / rowIndexStride).toInt()]
            ) {
                rowInStripe = min(rowCountInStripe, rowInStripe + rowIndexStride)
            }
        }

        // if we haven't skipped the whole stripe, read the data
        if (rowInStripe < rowCountInStripe) {
            planner.readData(indexes, includedRowGroups, false)
            reader.startStripe(planner)
            // if we skipped the first row group, move the pointers forward
            if (rowInStripe != 0L) {
                seekToRowEntry(reader, (rowInStripe / rowIndexStride).toInt())
            }
        }
    }

    private fun beginReadStripe(): StripeInformation {
        val stripe = stripes[currentStripe]
        stripeFooter = readStripeFooter(stripe)
        clearStreams()
        // setup the position in the stripe
        rowCountInStripe = stripe.numberOfRows
        rowInStripe = 0
        rowBaseInStripe = 0
        for (i in 0 until currentStripe) {
            rowBaseInStripe = (rowBaseInStripe + stripes[i].numberOfRows).toByte()
        }
        // reset all of the indexes
        val rowIndex = indexes.rowGroupIndex
        for (i in rowIndex.indices) {
            rowIndex[i] = null
        }
        return stripe
    }

    private fun clearStreams() {
        planner.clearStreams()
    }

    private fun readStripeFooter(stripe: StripeInformation): StripeFooter = dataReader.readStripeFooter(stripe)

    private fun pickRowGroups(): BooleanArray? = null

    private fun computeBatchSize(targetBatchSize: Long): Int {
        val batchSize: Int
        // In case of PPD, batch size should be aware of row group boundaries. If only a subset of row
        // groups are selected then marker position is set to the end of range (subset of row groups
        // within strip). Batch size computed out of marker position makes sure that batch size is
        // aware of row group boundary and will not cause overflow when reading rows
        // illustration of this case is here https://issues.apache.org/jira/browse/HIVE-6287
        if (rowIndexStride != 0L && includedRowGroups != null && rowInStripe < rowCountInStripe) {
            var startRowGroup = (rowInStripe / rowIndexStride).toInt()
            if (!includedRowGroups!![startRowGroup]) {
                while (startRowGroup < includedRowGroups!!.size && !includedRowGroups!![startRowGroup]) {
                    startRowGroup += 1
                }
            }
            var endRowGroup = startRowGroup
            while (endRowGroup < includedRowGroups!!.size && includedRowGroups!![endRowGroup]) {
                endRowGroup += 1
            }
            val markerPosition =
                if (endRowGroup * rowIndexStride < rowCountInStripe) endRowGroup * rowIndexStride else rowCountInStripe
            batchSize = min(targetBatchSize, markerPosition - rowInStripe).toInt()
        } else {
            batchSize = min(targetBatchSize, rowCountInStripe - rowInStripe).toInt()
        }
        return batchSize
    }

    /**
     * This function should be public in [TreeReader] API but currently it isn't
     * See https://issues.apache.org/jira/browse/ORC-618 for progress
     */
    private fun TreeReader.skipRows(rows: Long) {
        this::class.members.find { it.name == "skipRows" }?.also {
            it.isAccessible = true
            it.call(this, rows)
        }
    }

    /**
     * This function should be public in [TreeReader] API but currently it isn't
     * See https://issues.apache.org/jira/browse/ORC-618 for progress
     */
    private fun TreeReader.setVectorColumnCount(vectorColumnCount: Int) {
        this::class.members.find { it.name == "setVectorColumnCount" }?.also {
            it.isAccessible = true
            it.call(this, vectorColumnCount)
        }
    }

    /**
     * This function should be public in [TreeReader] API but currently it isn't
     * See https://issues.apache.org/jira/browse/ORC-618 for progress
     */
    private fun TreeReader.startStripe(stripePlanner: StripePlanner) {
        this::class.members.find { it.name == "startStripe" }?.also {
            it.isAccessible = true
            it.call(this, stripePlanner)
        }
    }

    companion object {
        private fun writerVersion(footer: OrcProto.Footer, postscript: OrcProto.PostScript): OrcFile.WriterVersion =
            OrcFile.WriterVersion.from(
                OrcFile.WriterImplementation.from(
                    footer.writer
                ), postscript.writerVersion
            )

    }
}