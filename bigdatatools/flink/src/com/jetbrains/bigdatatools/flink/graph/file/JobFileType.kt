package com.jetbrains.bigdatatools.flink.graph.file

import com.intellij.icons.AllIcons
import com.intellij.openapi.fileTypes.FileType

object JobFileType : FileType {
  override fun getName() = "flink-graph"
  override fun getDescription() = ""
  override fun getDefaultExtension() = ""
  override fun getIcon() = AllIcons.FileTypes.Diagram
  override fun isBinary() = false
}