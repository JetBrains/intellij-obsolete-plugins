package com.jetbrains.bigdatatools.dataproc.ui.component

import com.jetbrains.bigdatatools.common.rfs.util.withPrefixSlash
import com.jetbrains.spark.submit.model.FilePath
import com.jetbrains.spark.submit.model.FilePathSerializer
import com.jetbrains.spark.submit.model.FileType

object DataprocFileTypeSerializer : FilePathSerializer {
  override fun toText(path: FilePath): String {
    return when {
      path.path.isBlank() -> ""
      path.type == FileType.GCS -> path.toString()
      path.type == FileType.SERVER -> path.path.withPrefixSlash()
      else -> path.path
    }
  }

  override fun fromText(text: String): FilePath {
    val path = text.split("://").last()
    return when {
      text.startsWith(FileType.GCS.scheme, ignoreCase = true) -> {
        FilePath(FileType.GCS, path)
      }
      text.startsWith(FileType.FILE.scheme, ignoreCase = true) -> {
        FilePath(FileType.FILE, path)
      }
      else -> {
        FilePath(FileType.CUSTOM, path)
      }
    }
  }
}