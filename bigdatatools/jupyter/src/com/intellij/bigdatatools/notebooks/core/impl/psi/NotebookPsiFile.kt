// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.bigdatatools.notebooks.core.impl.psi

import com.intellij.bigdatatools.notebooks.core.api.psi.PsiCell
import com.intellij.extapi.psi.PsiFileBase
import com.intellij.lang.Language
import com.intellij.psi.FileViewProvider

abstract class NotebookPsiFile(viewProvider: FileViewProvider, language: Language) : PsiFileBase(viewProvider, language) {
  val cells: List<PsiCell> get() = firstChild.children.filterIsInstance<PsiCell>()
}

fun NotebookPsiFile.getPsiFileForLanguage(language: Language) = viewProvider.allFiles.firstOrNull { it.language == language }