package com.intellij.bigdatatools.zeppelin.psi

import com.intellij.bigdatatools.zeppelin.dependency.module.ZeppelinModuleUtils
import com.intellij.bigdatatools.zeppelin.file.NotebookFileUtil
import com.intellij.lang.Language
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiFile

interface ZeppelinCustomPsiFileFactory {
  fun createCustomPsiFile(lang: Language, viewProvider: FileViewProvider): PsiFile?

  fun supports(lang: Language): Boolean

  companion object {
    private val EP_NAME: ExtensionPointName<ZeppelinCustomPsiFileFactory> =
      ExtensionPointName.create("com.intellij.bigdatatools.zeppelin.zeppelinCustomPsiFileFactory")

    fun createCustomFile(lang: Language, viewProvider: FileViewProvider) =
      EP_NAME.extensionList.find { it.supports(lang) }?.createCustomPsiFile(lang, viewProvider)

    fun findZeppelinModule(file: VirtualFile, project: Project): Module? =
      NotebookFileUtil.getConfigId(file)?.let { ZeppelinModuleUtils.getModule(project, it) }
  }
}