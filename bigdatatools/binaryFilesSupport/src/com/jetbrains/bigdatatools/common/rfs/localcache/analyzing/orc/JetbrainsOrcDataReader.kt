package com.jetbrains.bigdatatools.common.rfs.localcache.analyzing.orc

import org.apache.hadoop.fs.FSDataInputStream
import org.apache.orc.DataReader
import org.apache.orc.OrcProto
import org.apache.orc.StripeInformation
import org.apache.orc.impl.BufferChunk
import org.apache.orc.impl.BufferChunkList
import org.apache.orc.impl.InStream
import org.apache.orc.impl.OrcCodecPool
import java.nio.ByteBuffer

class JetbrainsOrcDataReader(
    private val options: InStream.StreamOptions,
    private val fsDataInputStream: FSDataInputStream
) : DataReader {
    private var open = false

    override fun releaseBuffer(toRelease: ByteBuffer?) = Unit

    override fun readStripeFooter(stripe: StripeInformation): OrcProto.StripeFooter {
        if (!open) {
            open()
        }
        val offset = stripe.offset + stripe.indexLength + stripe.dataLength
        val tailLength = stripe.footerLength.toInt()

        // read the footer
        val tailBuf = ByteBuffer.allocate(tailLength)
        fsDataInputStream.readFully(offset, tailBuf.array(), tailBuf.arrayOffset(), tailLength)

        return OrcProto.StripeFooter.parseFrom(
            InStream.createCodedInputStream(
                InStream.create(
                    "footer",
                    BufferChunk(tailBuf, 0),
                    0,
                    tailLength.toLong(),
                    options
                )
            )
        )
    }

    override fun clone(): DataReader = throw UnsupportedOperationException("uncloneable")

    override fun open() {
        open = true
    }

    override fun getCompressionOptions() = options

    override fun isTrackingDiskRanges() = false

    override fun readFileData(range: BufferChunkList?, doForceDirect: Boolean): BufferChunkList {
        var current: BufferChunk? = range?.get()
        while (current != null) {
            while (current!!.hasData()) {
                current = current.next as BufferChunk
            }
            val last = findSingleRead(current)
            readRanges(fsDataInputStream, current, last, doForceDirect)
            current = last.next as BufferChunk?
        }

        return range!!
    }

    override fun close() {
        if (options.codec != null) {
            OrcCodecPool.returnCodec(options.codec.kind, options.codec)
            options.withCodec(null)
        }
    }


    private fun findSingleRead(first: BufferChunk): BufferChunk {
        var last = first
        var currentEnd = first.end
        while (last.next != null &&
            !last.next.hasData() && last.next.offset <= currentEnd && last.next.end - first.offset < Int.MAX_VALUE
        ) {
            last = last.next as BufferChunk
            currentEnd = currentEnd.coerceAtLeast(last.end)
        }
        return last
    }

    private fun readRanges(
        file: FSDataInputStream,
        first: BufferChunk,
        last: BufferChunk,
        allocateDirect: Boolean
    ) {
        // assume that the chunks are sorted by offset
        val offset = first.offset
        val readSize = (computeEnd(first, last) - offset).toInt()
        val buffer = ByteArray(readSize)
        file.readFully(offset, buffer, 0, buffer.size)

        // get the data into a ByteBuffer
        val bytes: ByteBuffer
        if (allocateDirect) {
            bytes = ByteBuffer.allocateDirect(readSize)
            bytes.put(buffer)
            bytes.flip()
        } else {
            bytes = ByteBuffer.wrap(buffer)
        }

        // populate each BufferChunks with the data
        var current: BufferChunk? = first
        while (current !== last.next) {
            val currentBytes = if (current === last) bytes else bytes.duplicate()
            currentBytes.position((current!!.offset - offset).toInt())
            currentBytes.limit((current.end - offset).toInt())
            current.setChunk(currentBytes)
            current = current.next as BufferChunk?
        }
    }

    private fun computeEnd(first: BufferChunk, last: BufferChunk): Long {
        var end: Long = 0
        var ptr: BufferChunk? = first
        while (ptr !== last.next) {
            end = ptr!!.end.coerceAtLeast(end)
            ptr = ptr.next as BufferChunk?
        }
        return end
    }
}