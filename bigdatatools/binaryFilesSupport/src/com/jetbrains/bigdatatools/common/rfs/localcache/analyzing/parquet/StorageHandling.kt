package com.jetbrains.bigdatatools.common.rfs.localcache.analyzing.parquet

import com.intellij.openapi.util.io.FileUtil
import com.jetbrains.bigdatatools.common.rfs.driver.FileInfo
import com.jetbrains.bigdatatools.common.rfs.io.RfsIoUtils.downloadToFile
import java.io.File
import java.io.FileInputStream
import java.io.FileWriter

/**
 * User: Dmitry.Naydanov
 * Date: 2018-11-20.
 */
abstract class StorageHandlerBase(protected val fileInfo: FileInfo) {
  abstract fun getPart(ind: Int): FileInputStream?

  abstract fun getPartLength(ind: Int): Long

  abstract fun setResultConstructor(constructor: (() -> File)?)

  abstract fun getResultFile(): File

  abstract fun writeToResult(value: String)

  abstract fun writeToResult(values: Iterator<String>)

  abstract fun download(descriptor: DownloadDescriptor)

  abstract fun getCount(): Int
}

class FileStorageHandler(
  fileInfo: FileInfo,
  resultFileExtension: String
) : StorageHandlerBase(fileInfo) {
  private val tempDir = FileUtil.createTempDirectory("RFSParquet", fileInfo.name)

  private var resultConstructor: (() -> File)? = null

  private val resultTempFile: File by lazy {
    resultConstructor?.invoke() ?: tempFile(fileInfo.name.replace('.', '_') + ".$resultFileExtension")
  }

  private val resultFileWriter: FileWriter by lazy {
    FileWriter(resultTempFile)
  }

  private var count = 0

  private fun getName(ind: Int): String = "${fileInfo.name}.part$ind"

  private fun tempFile(name: String): File {
    val tempFile = File(tempDir, name)
    if (!tempFile.exists()) tempFile.createNewFile()
    return tempFile
  }

  override fun setResultConstructor(constructor: (() -> File)?) {
    resultConstructor = constructor
  }

  override fun writeToResult(value: String) {
    resultFileWriter.write(value)
    resultFileWriter.write("\n")
  }

  override fun writeToResult(values: Iterator<String>) {
    for (v in values) {
      resultFileWriter.write(v)
      resultFileWriter.write("\n")
    }
  }

  override fun download(descriptor: DownloadDescriptor) {
    if (!descriptor.append) ++count

    fileInfo.downloadToFile(tempFile(getName(descriptor.index)), descriptor.offset, descriptor.size.toLong(), descriptor.append)
  }

  override fun getPart(ind: Int): FileInputStream? {
    val partFile = File(tempDir, getName(ind))
    return if (partFile.exists()) partFile.inputStream() else null
  }

  override fun getPartLength(ind: Int): Long {
    val partFile = File(tempDir, getName(ind))
    return if (partFile.exists()) partFile.length() else 0L
  }

  override fun getResultFile(): File {
    resultFileWriter.close()
    return resultTempFile
  }

  override fun getCount(): Int = count
}