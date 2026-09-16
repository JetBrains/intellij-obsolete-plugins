// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.bigdatatools.zeppelin.notebook.interpreter

import com.intellij.bigdatatools.zeppelin.psi.ZeppelinPsiFile
import com.intellij.lang.Commenter
import com.intellij.lang.Language
import com.intellij.lang.LanguageCommenters
import com.intellij.openapi.editor.Editor
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiFile
import com.intellij.psi.templateLanguages.MultipleLangCommentProvider

class ZeppelinCommentProvider : MultipleLangCommentProvider {
  override fun getLineCommenter(file: PsiFile, editor: Editor, lineStartLanguage: Language, lineEndLanguage: Language): Commenter? =
    when {
      lineStartLanguage in ZeppelinInterpreters.languages && lineEndLanguage in ZeppelinInterpreters.languages ->
        LanguageCommenters.INSTANCE.forLanguage(lineStartLanguage)
      else -> null
    }

  override fun canProcess(file: PsiFile, viewProvider: FileViewProvider) = file is ZeppelinPsiFile
}