package com.intellij.bigdatatools.zeppelin.formatter

import com.intellij.formatting.Alignment
import com.intellij.formatting.Block
import com.intellij.formatting.ChildAttributes
import com.intellij.formatting.Indent
import com.intellij.formatting.Spacing
import com.intellij.formatting.Wrap
import com.intellij.openapi.util.TextRange

open class StubBlock(private val range: TextRange) : Block {
  override fun getAlignment(): Alignment? = null
  override fun isIncomplete(): Boolean = false
  override fun isLeaf(): Boolean = true
  override fun getSpacing(child1: Block?, child2: Block): Spacing? = null
  override fun getTextRange(): TextRange = range
  override fun getSubBlocks(): MutableList<Block> = mutableListOf()
  override fun getChildAttributes(newChildIndex: Int): ChildAttributes = ChildAttributes(null, null)
  override fun getWrap(): Wrap? = null
  override fun getIndent(): Indent? = Indent.getAbsoluteNoneIndent()
}