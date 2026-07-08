package com.intellij.lang.puppet.formatter;

import com.intellij.formatting.Alignment;
import com.intellij.formatting.Block;
import com.intellij.formatting.ChildAttributes;
import com.intellij.formatting.Indent;
import com.intellij.formatting.Spacing;
import com.intellij.formatting.SpacingBuilder;
import com.intellij.formatting.SpacingImpl;
import com.intellij.lang.ASTNode;
import com.intellij.psi.TokenType;
import com.intellij.psi.formatter.common.AbstractBlock;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Anna Bulenkova
 */
class PuppetFormattingBlock extends AbstractBlock {
  private final SpacingBuilder mySpacingBuilder;
  private final PuppetIndentProcessor myIndentProcessor;
  private final PuppetAlignmentProcessor myAlignmentProcessor;
  private final PuppetWrappingProcessor myWrappingProcessor;

  private final @Nullable Indent myIndent;

  private List<Block> myBlocks;

  PuppetFormattingBlock(
    final ASTNode astNode,
    final SpacingBuilder spacingBuilder,
    final @NotNull PuppetIndentProcessor indentProcessor,
    @NotNull PuppetAlignmentProcessor alignmentProcessor,
    @NotNull PuppetWrappingProcessor wrappingProcessor
  ) {
    super(astNode, wrappingProcessor.getNodeWrap(astNode), alignmentProcessor.getNodeAlignment(astNode));

    myIndent = indentProcessor.getNodeIndent(astNode);

    mySpacingBuilder = spacingBuilder;
    myIndentProcessor = indentProcessor;
    myAlignmentProcessor = alignmentProcessor;
    myWrappingProcessor = wrappingProcessor;
  }

  @Override
  public @NotNull List<Block> buildChildren() {
    if (myBlocks == null) {
      myBlocks = buildSubBlocks();
    }
    return new ArrayList<>(myBlocks);
  }

  private List<Block> buildSubBlocks() {
    final List<Block> myBlocks = new ArrayList<>();
    for (ASTNode childNode = myNode.getFirstChildNode(); childNode != null; childNode = childNode.getTreeNext()) {
      if (childNode.getTextRange().getLength() == 0) {
        continue;
      }
      if (childNode.getElementType() == TokenType.WHITE_SPACE) {
        continue;
      }
      myBlocks.add(new PuppetFormattingBlock(childNode, mySpacingBuilder, myIndentProcessor, myAlignmentProcessor, myWrappingProcessor));
    }
    return Collections.unmodifiableList(myBlocks);
  }


  @Override
  public @Nullable Indent getIndent() {
    return myIndent;
  }

  @Override
  public Spacing getSpacing(final Block child1, final @NotNull Block child2) {
    Spacing spacing = mySpacingBuilder.getSpacing(this, child1, child2);
    if (spacing instanceof SpacingImpl spacingImpl && spacingImpl.getMinLineFeeds() > 0 &&
        child2 instanceof AbstractBlock && myWrappingProcessor.isHereDocOnLineBefore(((AbstractBlock)child2).getNode())) {
      return Spacing.createSpacing(
        spacingImpl.getMinSpaces(),
        spacingImpl.getMaxSpaces(),
        0,
        spacingImpl.shouldKeepLineFeeds(),
        spacingImpl.getKeepBlankLines(),
        spacingImpl.getPrefLineFeeds());
    }
    return spacing;
  }

  @Override
  public @NotNull ChildAttributes getChildAttributes(int newChildIndex) {
    final PuppetFormattingBlock prevChildBlock = newChildIndex > 0 ? getBlockBefore(newChildIndex) : null;
    final ASTNode prevChildNode = prevChildBlock == null ? null : prevChildBlock.getNode();

    Indent indent = myIndentProcessor.getChildNodeIndent(myNode, prevChildNode);
    Alignment alignment = myAlignmentProcessor.getNewChildAlignment(myNode, prevChildNode);

    if (indent == PuppetIndentProcessor.DELEGATE_TO_PREV) {
      return ChildAttributes.DELEGATE_TO_PREV_CHILD;
    }

    return new ChildAttributes(indent, alignment);
  }

  @Override
  public boolean isIncomplete() {
    return false;
  }

  @Override
  public boolean isLeaf() {
    return myNode.getFirstChildNode() == null;
  }

  private @Nullable PuppetFormattingBlock getBlockBefore(int newChildIndex) {
    if (newChildIndex == 0) {
      return null;
    }
    int prevIndex = newChildIndex - 1;
    return (PuppetFormattingBlock)getSubBlocks().get(prevIndex);
  }
}
