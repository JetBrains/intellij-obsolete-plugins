package com.intellij.bigdatatools.zeppelin.vcs

import com.intellij.bigdatatools.zeppelin.utils.ZepMessagesBundle
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.FilePath
import com.intellij.openapi.vcs.LocalFilePath
import com.intellij.openapi.vcs.changes.IgnoredBeanFactory
import com.intellij.openapi.vcs.changes.IgnoredFileBean
import com.intellij.openapi.vcs.changes.IgnoredFileDescriptor
import com.intellij.openapi.vcs.changes.IgnoredFileProvider
import com.intellij.util.containers.ContainerUtil
import kotlin.io.path.invariantSeparatorsPathString

internal class ZeppelinIgnoreFileProvider : IgnoredFileProvider {
  override fun isIgnoredFile(project: Project, filePath: FilePath): Boolean {
    return filePath.isUnder(LocalFilePath(ZeppelinPluginFolder.getDir(project), true), false)
  }

  override fun getIgnoredFiles(project: Project): Set<IgnoredFileDescriptor> {
    val zeppelinPluginPath = ZeppelinPluginFolder.getDir(project).invariantSeparatorsPathString
    return setOf(IgnoredBeanFactory.ignoreUnderDirectory(zeppelinPluginPath, project))
  }

  override fun getIgnoredGroupDescription(): String = ZepMessagesBundle.message("zeppelin.ignore.file.provider.descr")
}
