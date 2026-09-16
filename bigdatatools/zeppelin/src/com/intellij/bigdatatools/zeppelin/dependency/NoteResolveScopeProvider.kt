// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.bigdatatools.zeppelin.dependency

import com.intellij.bigdatatools.zeppelin.dependency.module.ZeppelinModuleUtils
import com.intellij.bigdatatools.zeppelin.file.NotebookFileUtil
import com.intellij.bigdatatools.zeppelin.file.getOriginalFile
import com.intellij.bigdatatools.zeppelin.file.isNoteFile
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.ResolveScopeProvider
import com.intellij.psi.search.GlobalSearchScope

class NoteResolveScopeProvider : ResolveScopeProvider() {
  override fun getResolveScope(file: VirtualFile, project: Project): GlobalSearchScope? {
    if (!file.isNoteFile)
      return null

    val originalFile = file.getOriginalFile()
    val configId = NotebookFileUtil.getConfigId(originalFile) ?: return null
    val configSpecification = NotebookFileUtil.getConfigSpecification(originalFile) ?: ""
    val module = ZeppelinModuleUtils.getModule(project, configId + configSpecification) ?: return null

    return GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(module)
  }
}
