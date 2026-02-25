// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.lang.gsp.formatter;

import com.intellij.formatting.Block;
import com.intellij.formatting.ChildAttributes;
import com.intellij.formatting.Indent;
import com.intellij.formatting.Spacing;
import com.intellij.lang.ASTNode;
import com.intellij.lang.Language;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.UnfairTextRange;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.formatter.java.ReadonlyWhitespaceBlock;
import com.intellij.psi.formatter.xml.AbstractXmlBlock;
import com.intellij.psi.formatter.xml.XmlFormattingPolicy;
import com.intellij.psi.impl.source.tree.TreeElement;
import com.intellij.psi.impl.source.tree.TreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.grails.lang.gsp.GspLanguage;
import org.jetbrains.plugins.grails.lang.gsp.psi.groovy.api.GspOuterGroovyElement;
import org.jetbrains.plugins.grails.lang.gsp.psi.gsp.api.GspFile;
import org.jetbrains.plugins.groovy.GroovyLanguage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GspGroovyBlock extends AbstractXmlBlock implements AbstractGspBlock {

  private final @Nullable Block myBaseLanguageBlock;
  private final Indent myParentIndent;

  public GspGroovyBlock(final ASTNode node,
                        final XmlFormattingPolicy policy,
                        Pair<PsiElement, Language> rootBlockInfo,
                        Indent indent
  ) {

    super(node, null, null, policy);
    myParentIndent = indent;
    myBaseLanguageBlock = policy.getOrCreateBlockFor(rootBlockInfo);
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
    if (myBaseLanguageBlock == null) {
      return Collections.emptyList();
    }

    ArrayList<Block> result = new ArrayList<>();
    extractBlocks(myBaseLanguageBlock, myNode.getTextRange(), result);
    for (Block block : result) {
      if (block instanceof GspTextSyntheticBlock) {
        ((GspTextSyntheticBlock)block).setIndent(myParentIndent);
      }
    }
    return result;
  }

  private static boolean extractBlocks(final Block parentBlock,
                                       final TextRange textRange,
                                       final ArrayList<Block> result) {

    if (textRange.getStartOffset() >= textRange.getEndOffset()) return false;

    final TextRange blockRange = parentBlock.getTextRange();

    if (blockRange.getStartOffset() >= textRange.getStartOffset() && blockRange.getEndOffset() <= textRange.getEndOffset()) {
      result.add(parentBlock);
      return true;
    }

    if (blockRange.getEndOffset() < textRange.getStartOffset()) return false;
    if (blockRange.getStartOffset() >= textRange.getEndOffset()) return false;

    final List<Block> subBlocks = parentBlock.getSubBlocks();
    final ArrayList<Block> localResult = new ArrayList<>();
    int fromIndex = -1;
    for (int i = 0; i < subBlocks.size(); i++) {
      final Block block = subBlocks.get(i);

      final TextRange subRange = block.getTextRange();

      final TextRange range = new UnfairTextRange(Math.max(textRange.getStartOffset(), subRange.getStartOffset()),
                                            Math.min(textRange.getEndOffset(), subRange.getEndOffset()));
      boolean added = extractBlocks(block, range, localResult);
      if (fromIndex == -1 && added) {
        fromIndex = i;
      }
    }

    if (!localResult.isEmpty()) {
      final Indent parentIndent;
      final int firstBlockStartOffset = localResult.get(0).getTextRange().getStartOffset();
      final int lastBlockEndOffset = localResult.get(localResult.size() - 1).getTextRange().getEndOffset();
      if (firstBlockStartOffset > blockRange.getStartOffset() && lastBlockEndOffset < blockRange.getEndOffset()) {
        parentIndent = Indent.getNoneIndent();
      }
      else {
        parentIndent = parentBlock.getIndent();
      }

      result.add(new GspTextSyntheticBlock(parentBlock, fromIndex, parentIndent, localResult));
    }
    else {
      TextRange intersection = blockRange.intersection(textRange);
      if (intersection != null) {
        result.add(new ReadonlyWhitespaceBlock(intersection, null, null, Indent.getNoneIndent()));
      }
    }
    return true;
  }

  @Override
  public Spacing getSpacing(Block child1, @NotNull Block child2) {
    if (myBaseLanguageBlock != null) {
      return myBaseLanguageBlock.getSpacing(child1, child2);
    }
    else {
      return null;
    }
  }

  @Override
  public @NotNull TextRange getTextRange() {
    return super.getTextRange();
  }


  @Override
  public Indent getIndent() {
    return Indent.getNoneIndent();
  }

  @Override
  public @NotNull ChildAttributes getChildAttributes(final int newChildIndex) {
    if (newChildIndex > 0 && getSubBlocks().get(newChildIndex - 1) instanceof GspTextSyntheticBlock) {
      return ChildAttributes.DELEGATE_TO_PREV_CHILD;
    }

    if (newChildIndex == 0 && getSubBlocks().get(0) instanceof GspTextSyntheticBlock) {
      return ChildAttributes.DELEGATE_TO_NEXT_CHILD;
    }

    if (myBaseLanguageBlock != null) {
      return myBaseLanguageBlock.getChildAttributes(newChildIndex);
    }
    else {
      return new ChildAttributes(null, null);
    }
  }

  @Override
  public String toString() {
    return myNode.getText();
  }

  public static Pair<PsiElement, Language> findPsiRootAt(final ASTNode child) {
    final FileViewProvider viewProvider = child.getPsi().getContainingFile().getViewProvider();
    final PsiFile file = viewProvider.getPsi(GspLanguage.INSTANCE);
    if (!(file instanceof GspFile)) return null;
    final int startOffset = child.getTextRange().getStartOffset();
    PsiElement psi = child.getPsi();
    if (psi instanceof GspOuterGroovyElement) {
      Language groovyLanguage = GroovyLanguage.INSTANCE;
      final PsiElement declElement = file.getViewProvider().findElementAt(startOffset, groovyLanguage);
      if (declElement == null) return null;
      return Pair.create(TreeUtil.getFileElement((TreeElement)declElement.getNode()).getPsi(), groovyLanguage);
    }
    return null;
  }
}
