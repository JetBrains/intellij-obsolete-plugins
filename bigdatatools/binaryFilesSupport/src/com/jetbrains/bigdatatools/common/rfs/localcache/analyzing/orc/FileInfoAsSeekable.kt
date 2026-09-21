package com.jetbrains.bigdatatools.common.rfs.localcache.analyzing.orc

import com.jetbrains.bigdatatools.common.rfs.driver.FileInfo
import org.apache.hadoop.fs.PositionedReadable
import org.apache.hadoop.fs.Seekable
import java.io.EOFException
import java.io.InputStream

class FileInfoAsSeekable private constructor(val fileInfo: FileInfo) : Seekable, PositionedReadable, InputStream() {
  private var pos = 0L
  private var stream: InputStream? = null
  override fun getPos() = pos

  override fun seekToNewSource(targetPos: Long) =
    throw UnsupportedOperationException("Seeking t new source is not supported")

  override fun seek(pos: Long) {
    if (pos >= this.pos && stream != null) stream?.skip(pos - this.pos) else {
      stream?.close()
      stream = fileInfo.readStream(pos, null).result
    }

    this.pos = pos
  }

  override fun readFully(position: Long, buffer: ByteArray, offset: Int, length: Int) {
    val inputStream = fileInfo.readStream(position, null)
      .resultOrThrow()

    inputStream.use {
      var current = 0
      while (current < length) {
        val read = it.read(buffer, offset + current, length - current)
        if (read == -1) throw EOFException("info = $fileInfo position = $position buffer = $buffer offset = $offset length = $length")
        current += read
      }
    }
  }

  override fun readFully(position: Long, buffer: ByteArray) {
    readFully(position, buffer, 0, buffer.size)
  }

  override fun read(position: Long, buffer: ByteArray, offset: Int, length: Int): Int =
    fileInfo.readStream(position, null).result?.use { it.read(buffer, offset, length) } ?: -1

  override fun read() = stream?.read() ?: -1

  override fun read(b: ByteArray) = stream?.read(b) ?: -1

  override fun read(b: ByteArray, off: Int, len: Int) = stream?.read(b, off, len) ?: -1

  override fun skip(n: Long) = stream?.skip(n) ?: 0

  override fun available() = stream?.available() ?: 0

  override fun reset() {
    stream?.reset()
  }

  override fun close() {
    stream?.close()
  }

  override fun mark(readlimit: Int) {
    stream?.mark(readlimit)
  }

  override fun markSupported() = stream?.markSupported() ?: false

  companion object {
    fun build(fileInfo: FileInfo): FileInfoAsSeekable? {
      fileInfo.readStream(0, null).result ?: return null
      return FileInfoAsSeekable(fileInfo)
    }
  }

}