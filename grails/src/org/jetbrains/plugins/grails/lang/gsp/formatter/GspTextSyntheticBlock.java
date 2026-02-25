// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.lang.gsp.formatter;

import com.intellij.formatting.Alignment;
import com.intellij.formatting.Block;
import com.intellij.formatting.ChildAttributes;
import com.intellij.formatting.Indent;
import com.intellij.formatting.Spacing;
import com.intellij.formatting.Wrap;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.TextRange;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class GspTextSyntheticBlock implements Block {
  private final Block myParentBlock;
  private Indent myIndent;
  private final List<Block> mySubBlocks;

  private final int myFromIndex;

  private final TextRange myTextRange;

  public GspTextSyntheticBlock(final Block parentBlock,
                               final int fromIndex, final Indent indent,
                               final List<Block> subBlocks) {
    myParentBlock = parentBlock;
    myIndent = indent;
    mySubBlocks = subBlocks;
    myFromIndex = fromIndex;

    myTextRange =
        new TextRange(subBlocks.get(0).getTextRange().getStartOffset(), subBlocks.get(subBlocks.size() - 1).getTextRange().getEndOffset());

  }

  @Override
  public @NotNull TextRange getTextRange() {
    return myTextRange;
  }

  @Override
  public @NotNull List<Block> getSubBlocks() {
    return mySubBlocks;
  }

  @Override
  public @Nullable Wrap getWrap() {
    return null;
  }

  @Override
  public @Nullable Indent getIndent() {
    return myIndent;
  }

  @Override
  public @Nullable Alignment getAlignment() {
    return null;
  }

  @Override
  public @NotNull ChildAttributes getChildAttributes(final int newChildIndex) {
    if (newChildIndex > 0 && mySubBlocks.get(newChildIndex - 1) instanceof GspTextSyntheticBlock) {
      return ChildAttributes.DELEGATE_TO_PREV_CHILD;
    }

    if (newChildIndex == 0 && mySubBlocks.get(0) instanceof GspTextSyntheticBlock) {
      return ChildAttributes.DELEGATE_TO_NEXT_CHILD;
    }

    return myParentBlock.getChildAttributes(myFromIndex + newChildIndex);
  }

  @Override
  public boolean isIncomplete() {
    return myParentBlock.isIncomplete();
  }

  private static final Logger LOG = Logger.getInstance(GspTextSyntheticBlock.class);

  @Override
  public @Nullable Spacing getSpacing(Block child1, @NotNull Block child2) {
    final Block first;
    final Block second;

    if (child1 instanceof GspTextSyntheticBlock) {
      first = ((GspTextSyntheticBlock) child1).myParentBlock;
    } else {
      first = child1;
    }

    if (child2 instanceof GspTextSyntheticBlock) {
      second = ((GspTextSyntheticBlock) child2).myParentBlock;
    } else {
      second = child2;
    }

    return myParentBlock.getSpacing(first, second);
  }

  public void setIndent(final Indent parentIndent) {
    myIndent = parentIndent;
  }

  @Override
  public boolean isLeaf() {
    return false;
  }
}
