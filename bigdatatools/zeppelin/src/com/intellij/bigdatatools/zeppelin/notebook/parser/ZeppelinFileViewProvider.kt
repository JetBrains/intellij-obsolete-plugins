// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.bigdatatools.zeppelin.notebook.parser

import com.intellij.bigdatatools.zeppelin.language.ZeppelinLanguage
import com.intellij.bigdatatools.zeppelin.notebook.interpreter.ZeppelinInterpreters
import com.intellij.bigdatatools.zeppelin.psi.ZeppelinCustomPsiFileFactory
import com.intellij.lang.Language
import com.intellij.lang.LanguageParserDefinitions
import com.intellij.openapi.fileTypes.PlainTextLanguage
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.MultiplePsiFilesPerDocumentFileViewProvider
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.impl.source.PsiFileImpl
import com.intellij.psi.templateLanguages.TemplateLanguageFileViewProvider

open class ZeppelinFileViewProvider(manager: PsiManager,
                                    file: VirtualFile,
                                    eventSystemEnabled: Boolean) : MultiplePsiFilesPerDocumentFileViewProvider(manager, file,
                                                                                                               eventSystemEnabled),
                                                                   TemplateLanguageFileViewProvider {
  private val supportLanguages = hashSetOf(baseLanguage).plus(ZeppelinInterpreters.languages)

  override fun getTemplateDataLanguage(): Language = PlainTextLanguage.INSTANCE

  override fun getBaseLanguage(): Language = ZeppelinLanguage

  override fun getLanguages() = supportLanguages

  override fun cloneInner(fileCopy: VirtualFile) = ZeppelinFileViewProvider(manager, fileCopy, false)

  override fun createFile(lang: Language): PsiFile? {
    val psiFileImpl = createFileImpl(lang) as PsiFileImpl
    ZeppelinInterpreters.ignoreInspections(psiFileImpl, lang)

    val elementType = ZeppelinInterpreters.languageToTemplates[lang.id]
    return when {
      elementType != null -> psiFileImpl.apply { contentElementType = elementType }
      lang === baseLanguage -> createFileImpl(lang)
      else -> null
    }
  }

  private fun createFileImpl(lang: Language): PsiFile {
    val customFile = ZeppelinCustomPsiFileFactory.createCustomFile(lang, this)
    if (customFile != null) return customFile.apply { putModuleInfo(this) }

    val parserDefinition = LanguageParserDefinitions.INSTANCE.forLanguage(lang)
    return parserDefinition.createFile(this).apply { putModuleInfo(this) }
  }

  private fun putModuleInfo(psiFile: PsiFile) {
    ZeppelinCustomPsiFileFactory.findZeppelinModule(virtualFile, manager.project)?.let { module ->
      psiFile.putUserData(ModuleUtilCore.KEY_MODULE, module)
    }
  }
}