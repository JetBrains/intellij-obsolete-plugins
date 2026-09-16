// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.bigdatatools.zeppelin.notebook.parser

import com.intellij.bigdatatools.notebooks.zeppelin.psi.ZeppelinParser
import com.intellij.bigdatatools.notebooks.zeppelin.psi.ZeppelinTypes
import com.intellij.bigdatatools.zeppelin.language.ZeppelinLanguage
import com.intellij.bigdatatools.zeppelin.notebook.lexer.ZeppelinTemplateLexer
import com.intellij.bigdatatools.zeppelin.psi.ZeppelinPsiFile
import com.intellij.lang.ASTNode
import com.intellij.lang.ParserDefinition
import com.intellij.openapi.project.Project
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.impl.source.PsiPlainTextFileImpl
import com.intellij.psi.tree.IFileElementType
import com.intellij.psi.tree.TokenSet

class ZeppelinParserDefinition : ParserDefinition {
  override fun createLexer(project: Project?) = ZeppelinTemplateLexer(project)
  override fun createParser(project: Project?) = ZeppelinParser()
  override fun createFile(viewProvider: FileViewProvider): PsiFile =
    if (viewProvider is ZeppelinFileViewProvider)
      ZeppelinPsiFile(viewProvider)
    else
      PsiPlainTextFileImpl(viewProvider)

  override fun getFileNodeType() = FILE
  override fun getCommentTokens(): TokenSet = TokenSet.EMPTY
  override fun getStringLiteralElements(): TokenSet = TokenSet.EMPTY
  override fun createElement(node: ASTNode?): PsiElement = ZeppelinTypes.Factory.createElement(node)
  override fun spaceExistenceTypeBetweenTokens(left: ASTNode?, right: ASTNode?) = ParserDefinition.SpaceRequirements.MAY


  companion object {
    val FILE = IFileElementType(ZeppelinLanguage)
  }
}