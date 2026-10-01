package com.intellij.dbt.highlighting

import com.intellij.jinja.lexer.Jinja2TokenTypes.LPAR
import com.intellij.jinja.lexer.Jinja2TokenTypes.RPAR
import com.intellij.lang.BracePair
import com.intellij.lang.PairedBraceMatcher
import com.intellij.psi.PsiFile
import com.intellij.psi.tree.IElementType

class DbtJinjaBraceMatcher : PairedBraceMatcher {
  private val PAIRS =  arrayOf(BracePair(LPAR, RPAR, false))

  override fun getPairs() = PAIRS

  override fun isPairedBracesAllowedBeforeType(lbraceType: IElementType, contextType: IElementType?) = true

  override fun getCodeConstructStart(file: PsiFile?, openingBraceOffset: Int) = openingBraceOffset
}