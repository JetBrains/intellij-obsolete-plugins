package com.jetbrains.spark.monitoring.graph.file

import com.intellij.icons.AllIcons
import com.intellij.openapi.fileTypes.FileType

object DotFileType : FileType {
  override fun getName() = "dag-graph"
  override fun getDescription() = ""
  override fun getDefaultExtension() = ""
  override fun getIcon() = AllIcons.FileTypes.Diagram
  override fun isBinary() = false
}