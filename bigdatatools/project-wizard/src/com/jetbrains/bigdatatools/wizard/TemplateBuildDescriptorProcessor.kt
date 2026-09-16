package com.jetbrains.bigdatatools.wizard

import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.io.FileUtilRt
import java.io.File
import java.io.FileFilter

class TemplateBuildDescriptorProcessor(
  private val descriptorNames: Set<String>,
  private val sourceDir: File,
  private val targetDir: File,
  private val macrosGroup: Map<String, String>
) : FileFilter {
  override fun accept(pathname: File?): Boolean {
    if (pathname == null || !descriptorNames.contains(pathname.name))
      return true

    targetDir.mkdirs()

    val relPath = pathname.relativeTo(sourceDir).path
    val targetFile = File(targetDir, relPath)
    targetFile.createNewFile()

    val text = FileUtilRt.loadFile(pathname)
    val regex = Regex(macrosGroup.map { it.key }.joinToString("|", "$MACRO_START(", ")"))
    val replaced = text.replace(regex) { matchResult ->
      macrosGroup[matchResult.value.substring(3)] ?: ""
    }

    FileUtil.writeToFile(targetFile, replaced)

    return false
  }

  companion object {
    private const val MACRO_START = "\\$\\$\\$"
  }
}