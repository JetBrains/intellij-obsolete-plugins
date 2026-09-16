package com.jetbrains.spark.submit.model

interface FilePathSerializer {
  fun toText(path: FilePath): String
  fun fromText(text: String): FilePath
}

object SchemeFilePathSerializer : FilePathSerializer {
  override fun toText(path: FilePath) = path.toString()
  override fun fromText(text: String) = FilePath.fromPathWithScheme(text)
}

class SingleFilePathSerializer(private val fileType: FileType) : FilePathSerializer {
  override fun toText(path: FilePath) = path.path
  override fun fromText(text: String) = FilePath(fileType, text)
}