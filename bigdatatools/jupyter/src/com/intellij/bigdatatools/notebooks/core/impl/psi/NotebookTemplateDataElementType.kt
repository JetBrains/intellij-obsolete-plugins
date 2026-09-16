// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.bigdatatools.notebooks.core.impl.psi

import com.intellij.bigdatatools.notebooks.core.impl.file.NotebookVirtualFile
import com.intellij.bigdatatools.notebooks.core.impl.lexer.NotebookTemplateLexer
import com.intellij.lang.Language
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.psi.templateLanguages.TemplateDataElementType
import com.intellij.psi.templateLanguages.TemplateLanguageFileViewProvider
import com.intellij.psi.tree.IElementType
import com.intellij.testFramework.LightVirtualFile

abstract class NotebookTemplateDataElementType(
  debugName: String,
  templateElementType: IElementType,
  language: Language,
  outerTemplateType: IElementType
) :
  TemplateDataElementType(
    debugName,
    language,
    templateElementType,
    outerTemplateType
  ) {

  override fun createTemplateFile(
    psiFile: PsiFile,
    templateLanguage: Language?,
    sourceCode: CharSequence,
    viewProvider: TemplateLanguageFileViewProvider?,
    rangeCollector: RangeCollector
  ): PsiFile {
    val lexer = getTemplateLexer(psiFile.project,viewProvider?.virtualFile)
    val notebookVirtualFile = psiFile.viewProvider.virtualFile

    if (notebookVirtualFile is NotebookVirtualFile) {
      lexer.notebook = notebookVirtualFile.notebook
    }
    else if (notebookVirtualFile is LightVirtualFile && notebookVirtualFile.originalFile is NotebookVirtualFile) {
      val originalFile = notebookVirtualFile.originalFile
      if (originalFile is NotebookVirtualFile) {
        lexer.notebook = originalFile.notebook
      }
    }

    val templateSourceCode = createTemplateText(sourceCode, lexer, rangeCollector)
    return createPsiFileFromSource(templateLanguage, templateSourceCode, psiFile.manager)
  }

  protected abstract fun getTemplateLexer(project: Project, virtualFile: VirtualFile?): NotebookTemplateLexer
}