package com.intellij.bigdatatools.zeppelin.editor

import com.intellij.codeInsight.editorActions.JavaLikeQuoteHandler
import com.intellij.codeInsight.editorActions.MultiCharQuoteHandler
import com.intellij.openapi.editor.highlighter.HighlighterIterator
import org.jetbrains.plugins.scala.codeInsight.editorActions.ScalaQuoteHandler

class ZeppelinQuoteHandler(val b: ScalaQuoteHandler = ScalaQuoteHandler()) : JavaLikeQuoteHandler by b, MultiCharQuoteHandler {
  override fun getClosingQuote(iterator: HighlighterIterator, offset: Int): CharSequence? = b.getClosingQuote(iterator, offset)
}