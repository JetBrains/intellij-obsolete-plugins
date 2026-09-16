package com.intellij.bigdatatools.zeppelin.formatter

import com.intellij.bigdatatools.zeppelin.notebook.interpreter.supported.raw.RawTextBlock
import com.intellij.formatting.Indent
import com.intellij.lang.ASTNode

class ZeppelinMarkerBlock(node: ASTNode) : RawTextBlock(node) {
  override fun getIndent(): Indent? = Indent.getAbsoluteNoneIndent()
}