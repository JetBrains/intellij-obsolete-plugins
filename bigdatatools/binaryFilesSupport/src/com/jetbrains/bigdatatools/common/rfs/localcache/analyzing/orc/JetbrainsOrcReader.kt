package com.jetbrains.bigdatatools.common.rfs.localcache.analyzing.orc

import com.jetbrains.bigdatatools.common.rfs.driver.FileInfo
import org.apache.hadoop.fs.FSDataInputStream
import org.apache.orc.CompressionKind
import org.apache.orc.OrcProto
import org.apache.orc.impl.BufferChunk
import org.apache.orc.impl.InStream
import org.apache.orc.impl.OrcCodecPool
import java.nio.ByteBuffer

object JetbrainsOrcReader {
  private const val MAX_POSTSCRIPT_SIZE = 256

  @Throws(ORCReadException::class)
  fun readPostScript(fileLength: Long, fsDataInputStream: FSDataInputStream): Pair<OrcProto.PostScript, Int> {
    // this is the maximum size of postscript + 1 byte for storing of postscript size
    val last257Bytes = ByteArray(257)
    fsDataInputStream.also { it.seek(fileLength - (MAX_POSTSCRIPT_SIZE + 1)) }.readFully(last257Bytes)
    //last byte stores postscript length
    val postscriptLength = last257Bytes.last().toInt()
    //and it can't be encoded/compressed by specification
    try {
      return OrcProto.PostScript.parseFrom(
        last257Bytes.takeLast(postscriptLength + 1).dropLast(1).toByteArray()
      ) to postscriptLength
    }
    catch (e: IllegalArgumentException) {
      throw ORCReadException("Invalid file", e)
    }
  }


  fun readFooter(
    postscript: OrcProto.PostScript,
    postscriptLength: Int,
    fileLength: Long,
    fsDataInputStream: FSDataInputStream
  ): OrcProto.Footer? {
    val compressionKind = CompressionKind.valueOf(postscript.compression.name)
    // footer starts right before postscript
    val offset = fileLength - (1 + postscriptLength + postscript.footerLength)
    val footerLength = postscript.footerLength.toInt()
    val footerBuf = ByteBuffer.allocate(footerLength)
    fsDataInputStream.readFully(offset, footerBuf.array(), footerBuf.arrayOffset(), footerLength)
    val codec = OrcCodecPool.getCodec(compressionKind)
    return if (compressionKind != CompressionKind.NONE) {
      val footerChunk = BufferChunk(footerBuf, 0)
      OrcProto.Footer.parseFrom(
        InStream.createCodedInputStream(
          InStream.create(
            "footer",
            footerChunk,
            0,
            postscript.footerLength,
            InStream.StreamOptions().withCodec(codec)
              .withBufferSize(postscript.compressionBlockSize.toInt())
          )
        )
      )
    }
    else OrcProto.Footer.parseFrom(footerBuf.array())
  }

  /**
   * Opens optimal InputStream for reading ORC file.
   * It will be native Hadoop [FSDataInputStream] if possible or [FileInfoAsSeekable] for non-HDFS filesytems
   *
   * Closes this [java.io.InputStream] automatically
   */
  inline fun <reified T> withInputStream(fileInfo: FileInfo, block: (FSDataInputStream?) -> T): T {
    val underlyingStreamIsFSDataInputStream = fileInfo.readStream(0, null).result?.use { it is FSDataInputStream }
    val fsDataInputStream = if (underlyingStreamIsFSDataInputStream == true)
      fileInfo.readStream(0, null).result as FSDataInputStream
    else
      FileInfoAsSeekable.build(fileInfo)?.let { FSDataInputStream(it) }

    return fsDataInputStream?.use { block(it) } ?: block(null)
  }
}