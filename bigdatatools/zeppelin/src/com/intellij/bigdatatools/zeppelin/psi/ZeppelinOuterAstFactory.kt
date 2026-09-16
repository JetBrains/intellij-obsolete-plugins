package com.intellij.bigdatatools.zeppelin.psi

import com.intellij.bigdatatools.zeppelin.psi.ZeppelinTemplateTypes.OUTER
import com.intellij.lang.ASTFactory
import com.intellij.psi.templateLanguages.OuterLanguageElementImpl
import com.intellij.psi.tree.IElementType

class ZeppelinOuterAstFactory : ASTFactory() {
  override fun createLeaf(type: IElementType, text: CharSequence) = if (type == OUTER)
    OuterLanguageElementImpl(type, text)
  else
    super.createLeaf(type, text)
}