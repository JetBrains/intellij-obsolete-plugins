// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.lang.gsp.formatter;

import com.intellij.formatting.Alignment;
import com.intellij.formatting.Block;
import com.intellij.formatting.ChildAttributes;
import com.intellij.formatting.Indent;
import com.intellij.formatting.Spacing;
import com.intellij.formatting.Wrap;
import com.intellij.lang.ASTNode;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiErrorElement;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.formatter.FormatterUtil;
import com.intellij.psi.formatter.xml.AbstractXmlBlock;
import com.intellij.psi.formatter.xml.XmlFormattingPolicy;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.grails.lang.gsp.formatter.processors.GspSpacingProcessor;
import org.jetbrains.plugins.grails.lang.gsp.parsing.GspElementTypes;
import org.jetbrains.plugins.grails.lang.gsp.psi.gsp.api.GspTag;
import org.jetbrains.plugins.grails.lang.gsp.psi.gsp.api.gtag.GspXmlRootTag;

import java.util.ArrayList;
import java.util.List;

public class GspBlock extends AbstractXmlBlock implements AbstractGspBlock, GspElementTypes {

  private static final Logger LOG = Logger.getInstance(GspBlock.class);

  private final Indent myIndent;
  private final TextRange myTextRange;
  private final GspFormattingHelper myFormattingHelper;

  public GspBlock(ASTNode node,
                  Wrap wrap,
                  Alignment alignment,
                  XmlFormattingPolicy policy,
                  Indent indent,
                  TextRange textRange) {
    super(node, wrap, alignment, policy);
    myTextRange = textRange != null ? textRange : super.getTextRange();
    myIndent = indent;
    myFormattingHelper = new GspFormattingHelper(myXmlFormattingPolicy, myNode, myTextRange);
  }

  @Override
  public @NotNull TextRange getTextRange() {
    return myTextRange;
  }

  @Override
  protected List<Block> buildChildren() {
    if (myNode.getElementType() == GSP_DIRECTIVE_ATTRIBUTE_VALUE
        || myNode.getElementType() == JSP_STYLE_COMMENT
        || myNode.getElementType() == GSP_STYLE_COMMENT) {
      return EMPTY;
    }

    if (myNode.getFirstChildNode() == null) return EMPTY;

    final ArrayList<Block> result = new ArrayList<>();

    final Wrap attrWrap = Wrap.createWrap(getWrapType(myXmlFormattingPolicy.getAttributesWrap()), false);
    final Alignment attrAlignment = Alignment.createAlignment();

    ASTNode child = myNode.getFirstChildNode();
    while (child != null) {
      if (FormatterUtil.containsWhiteSpacesOnly(child) || child.getTextLength() == 0) {
        child = child.getTreeNext();
        continue;
      }

      IElementType elementType = child.getElementType();
      Wrap wrap = elementType == GspElementTypes.GRAILS_TAG_ATTRIBUTE ? attrWrap : null;

      Alignment alignment = (elementType == GspElementTypes.GRAILS_TAG_ATTRIBUTE && myXmlFormattingPolicy.getShouldAlignAttributes()) ? attrAlignment : null;

      child = myFormattingHelper.processChild(result, child, wrap, alignment, Indent.getNoneIndent());
      if (child != null) {
        LOG.assertTrue(child.getTreeParent() == myNode);
      }
    }

    return result;
  }

  @Override
  public Spacing getSpacing(Block child1, @NotNull Block child2) {
    if ((child1 instanceof AbstractGspBlock) && (child2 instanceof AbstractGspBlock)) {
      return GspSpacingProcessor.getSpacing(myXmlFormattingPolicy, ((AbstractGspBlock) child1), ((AbstractGspBlock) child2));
    }
    return null;
  }

  @Override
  public @NotNull ChildAttributes getChildAttributes(final int newChildIndex) {
    ASTNode astNode = getNode();
    final PsiElement psiParent = astNode.getPsi();
    if (psiParent instanceof GspXmlRootTag) {
      return new ChildAttributes(Indent.getNoneIndent(), null);
    }
    if (psiParent instanceof GspTag) {
      return new ChildAttributes(Indent.getNormalIndent(), null);
    }

    return new ChildAttributes(Indent.getNoneIndent(), null);
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
    return false;
  }

  @Override
  public Indent getIndent() {
    return myIndent;
  }

  @Override
  public boolean isIncomplete() {
    return isIncomplete(myNode);
  }

  /**
   * @param node Tree node
   * @return true if node is incomplete
   */
  public boolean isIncomplete(final @NotNull ASTNode node) {
    ASTNode lastChild = node.getLastChildNode();
    while (lastChild != null &&
        (lastChild.getPsi() instanceof PsiWhiteSpace || lastChild.getPsi() instanceof PsiComment)) {
      lastChild = lastChild.getTreePrev();
    }
    return lastChild != null && (lastChild.getPsi() instanceof PsiErrorElement || isIncomplete(lastChild));
  }

  @Override
  public boolean isLeaf() {
    return myNode.getFirstChildNode() == null;
  }


}
