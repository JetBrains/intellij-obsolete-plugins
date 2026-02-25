/*
 * Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.plugins.grails.config

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vcs.FilePath
import com.intellij.openapi.vcs.changes.IgnoredBeanFactory
import com.intellij.openapi.vcs.changes.IgnoredFileDescriptor
import com.intellij.openapi.vcs.changes.IgnoredFileProvider
import org.jetbrains.plugins.grails.GrailsBundle

internal class GrailsIgnoredProvider : IgnoredFileProvider {
  override fun isIgnoredFile(project: Project, filePath: FilePath) =
    FileUtil.isAncestor(GrailsFramework.getUserHomeGrails(), filePath.path, false)

  override fun getIgnoredFiles(project: Project): Set<IgnoredFileDescriptor> {
    val grailsDir = GrailsFramework.getUserHomeGrails()
    val projectBasePath = project.basePath ?: return emptySet()

    if (FileUtil.isAncestor(projectBasePath, grailsDir, true)) {
      return setOf(IgnoredBeanFactory.ignoreUnderDirectory(grailsDir, project))
    }

    return emptySet()
  }

  override fun getIgnoredGroupDescription() = GrailsBundle.message("ignored.files.description.framework.dir")
}