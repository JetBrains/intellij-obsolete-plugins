package com.intellij.bigdatatools.zeppelin.formatter

import com.intellij.bigdatatools.zeppelin.language.ZeppelinLanguage
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.codeStyle.CodeStyleSettings
import com.intellij.psi.formatter.StaticSymbolWhiteSpaceDefinitionStrategy
import com.intellij.psi.formatter.WhiteSpaceFormattingStrategyFactory
import com.intellij.psi.impl.source.tree.LeafElement
import kotlin.math.max
import kotlin.math.min

class ZeppelinWhiteSpaceFormattingStrategy : StaticSymbolWhiteSpaceDefinitionStrategy(' ', '\t', '\n') {
  override fun replaceDefaultStrategy(): Boolean = true

  override fun containsWhitespacesOnly(node: ASTNode): Boolean {
    val language = node.psi.language
    if (language.`is`(ZeppelinLanguage))
      return super.containsWhitespacesOnly(node)
    return WhiteSpaceFormattingStrategyFactory.getStrategy(language).containsWhitespacesOnly(node)
  }

  override fun addWhitespace(treePrev: ASTNode, whiteSpaceElement: LeafElement): Boolean {
    val language = treePrev.psi.language
    if (language.`is`(ZeppelinLanguage))
      return super.addWhitespace(treePrev, whiteSpaceElement)
    return WhiteSpaceFormattingStrategyFactory.getStrategy(language).addWhitespace(treePrev, whiteSpaceElement)
  }

  override fun check(text: CharSequence, start: Int, end: Int): Int {
    return max(super.check(text, start, end), checkPythonWhitespace(text, start, end))
  }

  override fun adjustWhiteSpaceIfNecessary(whiteSpaceText: CharSequence,
                                           startElement: PsiElement,
                                           startOffset: Int,
                                           endOffset: Int,
                                           codeStyleSettings: CodeStyleSettings?): CharSequence {
    val language = startElement.language
    if (language.`is`(ZeppelinLanguage))
      return super.adjustWhiteSpaceIfNecessary(whiteSpaceText, startElement, startOffset, endOffset, codeStyleSettings)
    return WhiteSpaceFormattingStrategyFactory.getStrategy(language).adjustWhiteSpaceIfNecessary(whiteSpaceText, startElement, startOffset,
                                                                                                 endOffset, codeStyleSettings)
  }

  override fun adjustWhiteSpaceIfNecessary(whiteSpaceText: CharSequence,
                                           text: CharSequence,
                                           startOffset: Int,
                                           endOffset: Int,
                                           codeStyleSettings: CodeStyleSettings?,
                                           nodeAfter: ASTNode?): CharSequence {
    if (nodeAfter == null || nodeAfter.elementType.language.`is`(ZeppelinLanguage))
      return super.adjustWhiteSpaceIfNecessary(whiteSpaceText, text, startOffset, endOffset, codeStyleSettings, nodeAfter)

    return WhiteSpaceFormattingStrategyFactory.getStrategy(nodeAfter.elementType.language)
      .adjustWhiteSpaceIfNecessary(whiteSpaceText, text, startOffset, endOffset, codeStyleSettings, nodeAfter)
  }

  private fun checkPythonWhitespace(text: CharSequence, start: Int, end: Int): Int {
    var seenSlash = false

    for (i in start..text.length) {
      when (text[i]) {
        ' ', '\t' -> {}
        '\n' -> return min(i, end)
        '\\' -> if (seenSlash) return 0 else seenSlash = true
        else -> return 0
      }
    }

    return 0
  }
}