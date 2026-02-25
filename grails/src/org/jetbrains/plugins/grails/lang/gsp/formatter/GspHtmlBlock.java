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
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiErrorElement;
import com.intellij.psi.formatter.FormatterUtil;
import com.intellij.psi.formatter.xml.AbstractXmlBlock;
import com.intellij.psi.formatter.xml.XmlFormattingPolicy;
import com.intellij.psi.html.HtmlTag;
import com.intellij.psi.impl.source.tree.CompositeElement;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlAttributeValue;
import com.intellij.psi.xml.XmlTag;
import com.intellij.psi.xml.XmlText;
import com.intellij.psi.xml.XmlTokenType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.grails.lang.gsp.GspLanguage;
import org.jetbrains.plugins.grails.lang.gsp.formatter.processors.GspIndentProcessor;
import org.jetbrains.plugins.grails.lang.gsp.formatter.processors.GspSpacingProcessor;
import org.jetbrains.plugins.grails.lang.gsp.lexer.core.GspTokenTypes;
import org.jetbrains.plugins.grails.lang.gsp.parsing.GspElementTypes;
import org.jetbrains.plugins.grails.lang.gsp.psi.gsp.api.GspTag;
import org.jetbrains.plugins.grails.lang.gsp.psi.gsp.api.gtag.GspTaggedElement;
import org.jetbrains.plugins.grails.lang.gsp.psi.html.api.GspHtmlOuterElement;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

public class GspHtmlBlock extends AbstractXmlBlock implements Block, GspElementTypes, AbstractGspBlock {

  private static final Logger LOG = Logger.getInstance(GspHtmlBlock.class);

  private static final Comparator<PsiElement> myComparator = (elem1, elem2) -> {
    final int offset1 = elem1.getTextRange().getStartOffset();
    final int offset2 = elem2.getTextRange().getStartOffset();
    return offset1 - offset2;
  };

  private final Indent myIndent;
  private final XmlTag[] myNestedGspTags;

  public GspHtmlBlock(ASTNode node,
                      Wrap wrap,
                      Alignment alignment,
                      XmlFormattingPolicy policy,
                      Indent indent,
                      XmlTag[] nestedGspTags) {
    super(node, wrap, alignment, policy);
    myNestedGspTags = nestedGspTags;
    myIndent = indent;
  }


  @Override
  protected List<Block> buildChildren() {
    ASTNode child = myNode.getFirstChildNode();
    if (child == null) return EMPTY;

    final ArrayList<Block> result = new ArrayList<>();

    final Wrap attrWrap = Wrap.createWrap(getWrapType(myXmlFormattingPolicy.getAttributesWrap()), false);
    final Alignment attrAlignment = Alignment.createAlignment();

    while (child != null) {
      if (child.getTextLength() == 0 || FormatterUtil.containsWhiteSpacesOnly(child)) {
        child = child.getTreeNext();
        continue;
      }

      ASTNode newChild = processChild(result, child, chooseWrap(child, null, attrWrap, null),
                                      chooseAlignment(child, attrAlignment, null), Indent.getNoneIndent());
      if (newChild != null) {
        LOG.assertTrue(newChild.getTreeParent() == myNode);
        if (newChild == child) {
          newChild = child.getTreeNext();
        }
      }

      child = newChild;
    }

    return result;
  }

  private ASTNode processInnerChild(ASTNode innerRoot, Wrap wrap, Alignment alignment, List<Block> result) {

    ASTNode parent = innerRoot.getTreeParent();
    if (!(innerRoot.getPsi() instanceof GspHtmlOuterElement)) {
      if (!(innerRoot.getPsi() instanceof CompositeElement)) {
        GspBlockGenerator.createHtmlBlockByChildNode(result, myNode, innerRoot, wrap,
            alignment, myXmlFormattingPolicy, calculateNestedGspTagsForElement(innerRoot.getPsi()));
        return innerRoot.getTreeNext();
      }
      XmlTag[] tags = calculateIntersectedTags(innerRoot.getPsi(), true);
      if (tags.length == 0) {
        GspBlockGenerator.createHtmlBlockByChildNode(result, myNode, innerRoot, wrap,
            alignment, myXmlFormattingPolicy, calculateNestedGspTagsForElement(innerRoot.getPsi()));
        return innerRoot.getTreeNext();
      } else {
        ASTNode inner = innerRoot.getFirstChildNode();
        while (inner != null) {
          if (inner.getText().trim().isEmpty()) {
            inner = inner.getTreeNext();
            continue;
          }
          if (!(inner instanceof CompositeElement) && intersects(inner, tags)) {
            inner = inner.getTreeNext();
          } else {
            inner = processInnerChild(inner, wrap, alignment, result);
          }
        }
        for (XmlTag tag : tags) {
          if (!alreadyHas(tag, result)) {
            GspBlockGenerator.createGspBlockByChildNode(result, myNode, tag.getNode(), wrap, alignment, myXmlFormattingPolicy);
          }
        }
        ASTNode found = parent.findLeafElementAt(tags[tags.length - 1].getTextRange().getEndOffset() - parent.getStartOffset());
        while (found != null && !parent.equals(found.getTreeParent())) {
          found = found.getTreeParent();
        }
        return found != null && found.equals(innerRoot) ? found.getTreeNext() : found;
      }
    } else {
      final FileViewProvider viewProvider = innerRoot.getPsi().getContainingFile().getViewProvider();
      PsiElement gspElement = viewProvider.findElementAt(innerRoot.getStartOffset(), GspLanguage.INSTANCE);
      assert gspElement != null;
      int offset = gspElement.getTextRange().getStartOffset();

      while (gspElement.getParent() != null
          && gspElement.getParent().getTextRange().getStartOffset() == offset) {
        if (gspElement instanceof GspTag ||
            gspElement instanceof GspTaggedElement ||
            gspElement instanceof PsiComment) break;
        gspElement = gspElement.getParent();
      }

      ASTNode node = gspElement.getNode();
      boolean isComment = node != null &&
          (node.getElementType() == GspTokenTypes.JSP_STYLE_COMMENT ||
              node.getElementType() == GspTokenTypes.GSP_STYLE_COMMENT);

      if (!(gspElement instanceof GspTaggedElement ||
          gspElement instanceof GspTag ||
          isComment)) {
        GspBlockGenerator.createHtmlBlockByChildNode(result, myNode, innerRoot, wrap, alignment,
            myXmlFormattingPolicy, calculateNestedGspTagsForElement(gspElement));
        return innerRoot.getTreeNext();
      }

      if (!getTextRange().contains(gspElement.getTextRange())) {
        LOG.error("Formatter error",
            "outer html block text: [" + myNode.getText() + "]",
            "inner gsp block text: [" + gspElement.getText() + "]");
      }
      GspBlockGenerator.createGspBlockByChildNode(result, myNode, gspElement.getNode(), wrap, alignment, myXmlFormattingPolicy);


      int outerAbsoluteEndOffset = gspElement.getTextRange().getEndOffset();
      if (outerAbsoluteEndOffset == getTextRange().getEndOffset()) {
        return null;
      }

      // todo check for nested GSP tag
      ASTNode next = parent.findLeafElementAt(outerAbsoluteEndOffset - parent.getStartOffset());
      while (next != null && next.getTreeParent() != parent) {
        next = next.getTreeParent();
      }
      return next;
    }

  }

  private static boolean intersects(ASTNode node, XmlTag[] tags) {
    for (XmlTag tag : tags) {
      if (node.getTextRange().intersectsStrict(tag.getTextRange())) {
        return true;
      }
    }
    return false;
  }

  private static boolean nonContainedInTags(ASTNode node, XmlTag[] tags) {
    TextRange nodeTextRange = node.getTextRange();
    for (XmlTag tag : tags) {
      if (tag.getTextRange().contains(nodeTextRange)) {
        return false;
      }
    }

    return true;
  }

  @Override
  protected ASTNode processChild(@NotNull List<Block> result,
                                 final @NotNull ASTNode child,
                                 final Wrap wrap,
                                 final Alignment alignment,
                                 final Indent indent) {
    // For html element
    if (!(child.getPsi() instanceof GspHtmlOuterElement)) {
      if (doesNotIntersectSubTagsWith(child.getPsi())) {
        GspBlockGenerator.createHtmlBlockByChildNode(result, myNode, child, wrap,
            alignment, myXmlFormattingPolicy, calculateNestedGspTagsForElement(child.getPsi()));
        return child;
      } else {
        //intersection with nested GSP elements
        XmlTag[] tags = calculateIntersectedTags(child.getPsi(), false);
        ASTNode inner = child.getFirstChildNode();
        while (inner != null) {
          if (inner.getText().trim().isEmpty()) {
            inner = inner.getTreeNext();
            continue;
          }
          if (!(inner instanceof CompositeElement) &&
              intersects(inner, tags)) {
            inner = inner.getTreeNext();
          } else if ((inner instanceof XmlAttribute ||
              inner instanceof XmlAttributeValue ||
              inner instanceof XmlTag ||
              inner instanceof XmlText ||
              inner instanceof PsiErrorElement) && intersects(inner, tags)) {
            processCompositeElementRecursive(inner, calculateIntersectedTags(inner.getPsi(), true), result, wrap, alignment, indent);
            inner = inner.getTreeNext();
          } else {
            inner = processInnerChild(inner, wrap, alignment, result);
          }
        }
        // All non-GSP inners are already processed.
        // create GSPBlock for intersected GSP tags
        for (XmlTag tag : tags) {
          if (!alreadyHas(tag, result)) {
            GspBlockGenerator.createGspBlockByChildNode(result, myNode, tag.getNode(), wrap, alignment, myXmlFormattingPolicy);
          }
        }

        if (tags.length > 0) {
          ASTNode found = myNode.findLeafElementAt(tags[tags.length - 1].getTextRange().getEndOffset() - myNode.getStartOffset());
          while (found != null && !myNode.equals(found.getTreeParent())) {
            found = found.getTreeParent();
          }
          return found;
        }
        return child;

      }
    }
    return processInnerChild(child, wrap, alignment, result);
  }

  private void processCompositeElementRecursive(ASTNode inner, XmlTag[] tags, List<Block> result, Wrap wrap, Alignment alignment, Indent indent) {
    for (ASTNode node : inner.getChildren(null)) {
      if (!node.getText().trim().isEmpty()) {
        if (!intersects(node, tags)) {
          GspBlockGenerator.createHtmlBlockByChildNode(result, myNode, node, wrap,
                                                       alignment, myXmlFormattingPolicy, calculateNestedGspTagsForElement(inner.getPsi()));
        } else if (nonContainedInTags(node, tags)) {
          processCompositeElementRecursive(node, tags, result, wrap, alignment, indent);
        }
      }
    }
  }

  private static boolean alreadyHas(XmlTag tag, List<Block> result) {
    for (Block block : result) {
      if (block.getTextRange().equals(tag.getTextRange())) return true;
    }
    return false;
  }

  @Override
  public Spacing getSpacing(Block child1, @NotNull Block child2) {
    if ((child1 instanceof AbstractGspBlock) && (child2 instanceof AbstractGspBlock)) {
      return GspSpacingProcessor.getSpacing(myXmlFormattingPolicy, ((AbstractGspBlock) child1), ((AbstractGspBlock) child2));
    }
    return null;
  }

  @Override
  protected boolean doesNotIntersectSubTagsWith(final PsiElement element) {
    final TextRange range = element.getTextRange();
    for (XmlTag subTag : myNestedGspTags) {
      final TextRange subTagRange = subTag.getTextRange();
      if (subTagRange.getEndOffset() < range.getStartOffset()) continue;
      if (subTagRange.getStartOffset() > range.getEndOffset()) continue;

      if (areOvercrossing(range, subTagRange))
        return false;
    }
    return true;
  }

  private XmlTag[] calculateIntersectedTags(PsiElement element, boolean isInner) {
    if (element == null) return XmlTag.EMPTY;
    final TextRange range = element.getTextRange();
    ArrayList<XmlTag> tags = new ArrayList<>();
    for (XmlTag subTag : myNestedGspTags) {
      final TextRange subTagRange = subTag.getTextRange();
      if (subTagRange.getEndOffset() < range.getStartOffset()) continue;
      if (subTagRange.getStartOffset() > range.getEndOffset()) continue;
      if (areOvercrossing(range, subTagRange) ||
          isInner && subTagRange.contains(range)) {
        tags.add(subTag);
      }
    }
    filterInners(tags);
    XmlTag[] tagsArr = tags.toArray(XmlTag.EMPTY);
    Arrays.sort(tagsArr, myComparator);
    return tagsArr;
  }


  private static void filterInners(Collection<XmlTag> tags) {
    for (XmlTag tag : tags) {
      Iterator<XmlTag> innerIterator = tags.iterator();
      while (innerIterator.hasNext()) {
        XmlTag xmlTag = innerIterator.next();
        if (tag.getTextRange().contains(xmlTag.getTextRange()) &&
            tag != xmlTag) {
          innerIterator.remove();
        }
      }
    }
  }

  public static boolean areOvercrossing(TextRange range, TextRange subTagRange) {
    return (range.getStartOffset() > subTagRange.getStartOffset() &&
        range.getStartOffset() < subTagRange.getEndOffset() &&
        range.getEndOffset() >= subTagRange.getEndOffset()) ||
        (range.getEndOffset() > subTagRange.getStartOffset() &&
            range.getEndOffset() <= subTagRange.getEndOffset() &&
            range.getStartOffset() <= subTagRange.getStartOffset());
  }

  private XmlTag[] calculateNestedGspTagsForElement(PsiElement element) {
    TextRange range = element.getTextRange();
    ArrayList<XmlTag> tags = new ArrayList<>();
    for (XmlTag tag : myNestedGspTags) {
      if (range.contains(tag.getTextRange())) {
        tags.add(tag);
      }
    }
    return tags.toArray(XmlTag.EMPTY);
  }


  @Override
  public @NotNull ChildAttributes getChildAttributes(final int newChildIndex) {
    ASTNode astNode = getNode();
    final PsiElement psiParent = astNode.getPsi();
    if (psiParent instanceof HtmlTag) {
      return new ChildAttributes(GspIndentProcessor.indentForHtmlTag(myXmlFormattingPolicy, ((HtmlTag) psiParent)), null);
    }
    if (psiParent instanceof XmlTag) {
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
    return isTextNode(myNode.getElementType()) || myNode.getElementType() == XmlTokenType.XML_DATA_CHARACTERS ||
           myNode.getElementType() == XmlTokenType.XML_CHAR_ENTITY_REF;
  }

  @Override
  public Indent getIndent() {
    return myIndent;
  }

}
