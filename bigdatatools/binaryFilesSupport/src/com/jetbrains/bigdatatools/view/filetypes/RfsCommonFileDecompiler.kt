package com.jetbrains.bigdatatools.view.filetypes

import com.intellij.openapi.fileTypes.BinaryFileDecompiler
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.bigdatatools.common.rfs.driver.local.LocalDriverManager
import com.jetbrains.bigdatatools.common.rfs.localcache.RfsFileContentManager
import com.jetbrains.bigdatatools.common.rfs.util.RfsFileUtil
import kotlinx.coroutines.runBlocking

class RfsCommonFileDecompiler : BinaryFileDecompiler {
  override fun decompile(file: VirtualFile): CharSequence {
    val fileInfo = try {
      LocalDriverManager.instance.createFileInfo(RfsFileUtil.convertToIOFile(file))
    }
    catch (_: Exception) {
      return ""
    }

    val managers = ProjectManager.getInstance().openProjects.map {
      RfsFileContentManager.getInstance(it)
    }

    return runBlocking {
      managers.find { it.hasCachedContent(fileInfo) }?.let { return@runBlocking it.getContentForVirtualFile(fileInfo, file) ?: "" }
      managers.firstOrNull()?.let { return@runBlocking it.getContentForVirtualFile(fileInfo, file) ?: "" }
      return@runBlocking ""
    }
  }
}