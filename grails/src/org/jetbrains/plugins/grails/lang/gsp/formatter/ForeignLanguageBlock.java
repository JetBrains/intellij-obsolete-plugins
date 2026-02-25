// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.lang.gsp.formatter;

import com.intellij.formatting.Block;
import com.intellij.formatting.ChildAttributes;
import com.intellij.formatting.Indent;
import com.intellij.formatting.Spacing;
import com.intellij.lang.ASTNode;
import com.intellij.psi.formatter.xml.AbstractXmlBlock;
import com.intellij.psi.formatter.xml.XmlFormattingPolicy;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ForeignLanguageBlock extends AbstractXmlBlock {
  private final Block myOriginal;
  private final Indent myIndent;

  public ForeignLanguageBlock(final ASTNode node,
                                     final XmlFormattingPolicy policy,
                                     final Block original, final Indent indent) {
    super(node, original.getWrap(), original.getAlignment(), policy);
    myOriginal = original;
    myIndent = indent;
  }

  @Override
  public Indent getIndent() {
    return myIndent;
  }

  @Override
  public boolean insertLineBreakBeforeTag() {
    return false;
  }

  @Override
  public boolean removeLineBreakBeforeTag() {
    return false;
  }

  @Override
  public boolean isTextElement() {
    return true;
  }

  @Override
  protected List<Block> buildChildren() {
    return myOriginal.getSubBlocks();
  }

  @Override
  public @Nullable Spacing getSpacing(Block child1, @NotNull Block child2) {
    return myOriginal.getSpacing(child1,  child2);
  }

  @Override
  public @NotNull ChildAttributes getChildAttributes(final int newChildIndex) {
    return myOriginal.getChildAttributes(newChildIndex);
  }
}