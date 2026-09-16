package com.intellij.bigdatatools.databricks.model

import com.squareup.moshi.Json

data class DbfsFile(
  @Json(name = "path")
  val path: String = "",
  @Json(name = "is_dir")
  val isDir: Boolean = false,
  @Json(name = "file_size")
  val fileSize: Long = 0,
  @Json(name = "modification_time")
  val modificationTime: Long = 0
)