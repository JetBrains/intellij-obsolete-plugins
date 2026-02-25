// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.lang.gsp.formatter;

import com.intellij.formatting.Alignment;
import com.intellij.formatting.Block;
import com.intellij.formatting.Indent;
import com.intellij.formatting.Wrap;
import com.intellij.lang.ASTNode;
import com.intellij.lang.Language;
import com.intellij.lang.html.HTMLLanguage;
import com.intellij.lang.javascript.JavaScriptSupportLoader;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiErrorElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.formatter.xml.XmlFormattingPolicy;
import com.intellij.psi.impl.source.tree.LeafElement;
import com.intellij.psi.impl.source.tree.TreeElement;
import com.intellij.psi.impl.source.tree.TreeUtil;
import com.intellij.psi.search.PsiElementProcessor;
import com.intellij.psi.templateLanguages.OuterLanguageElement;
import com.intellij.psi.xml.XmlElement;
import com.intellij.psi.xml.XmlElementType;
import com.intellij.psi.xml.XmlTag;
import com.intellij.psi.xml.XmlTokenType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.grails.addins.js.JavaScriptIntegrationUtil;
import org.jetbrains.plugins.grails.lang.gsp.psi.GspPsiUtil;
import org.jetbrains.plugins.grails.lang.gsp.psi.groovy.api.GspOuterGroovyElement;
import org.jetbrains.plugins.grails.lang.gsp.psi.gsp.api.GspDeclarationTag;
import org.jetbrains.plugins.grails.lang.gsp.psi.gsp.api.GspScriptletTag;
import org.jetbrains.plugins.grails.lang.gsp.psi.gsp.api.directive.GspDirective;
import org.jetbrains.plugins.grails.lang.gsp.psi.gsp.api.gtag.GspGrailsTag;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class GspFormattingHelper {
  private final XmlFormattingPolicy myXmlFormattingPolicy;
  private final ASTNode myNode;
  private TextRange myTextRange;
  private Collection<PsiElement> myChildrenToSkip = new ArrayList<>();

  public GspFormattingHelper(XmlFormattingPolicy policy, ASTNode node, TextRange textRange) {
    myNode = node;
    myXmlFormattingPolicy = policy;
    myTextRange = textRange;
  }

  public ASTNode processNonGspChild(final ASTNode child,
                                    final Indent indent,
                                    final List<Block> result,
                                    final Wrap wrap,
                                    final Alignment alignment) {

//     We ignore fragmented injections
//    if (myChildrenToSkip.contains(child.getPsi())) {
//      return child.getTreeNext();
//    }
    final Pair<PsiElement, Language> root = GspGroovyBlock.findPsiRootAt(child);
    int htmlTagOffset;
    if (root != null && child.getPsi() instanceof GspOuterGroovyElement) {
      createGspGroovyNode(result, child, Indent.getNormalIndent());
      myTextRange = new TextRange(child.getTextRange().getEndOffset(), myTextRange.getEndOffset());
      return child.getTreeNext();
    } else if (JavaScriptIntegrationUtil.isJavaScriptInjection(child.getPsi()) && myChildrenToSkip.isEmpty()) {

      // todo implement for fragmented injections
      myChildrenToSkip = processJavascriptTagBody(child.getPsi());
      // create block for JavaScript injection
      if (myChildrenToSkip.size() < 2) {
        GspBlockGenerator.createForeignLanguageBlock(JavaScriptSupportLoader.JAVASCRIPT.getLanguage(),
                child, result, myXmlFormattingPolicy, myXmlFormattingPolicy.getSettings());
      } else {
        GspBlockGenerator.createGspBlockByChildNode(result, myNode, child, wrap, alignment, myXmlFormattingPolicy);
      }
      myTextRange = new TextRange(child.getTextRange().getEndOffset(), myTextRange.getEndOffset());

    } else if ((htmlTagOffset = calculatePossibleHtmlTagBegin(child)) >= 0) {
      XmlTag tag = getHtmlTagByOffset(htmlTagOffset, child);
      if (tag != null &&
              myTextRange.contains(tag.getTextRange()) &&
              doesNotIntersectSubTagsWith(tag) &&
              isGoodTag(tag)) {
        if (htmlTagOffset > myTextRange.getStartOffset()) {
          TextRange trashRange = new TextRange(Math.max(child.getTextRange().getStartOffset(), myTextRange.getStartOffset()), htmlTagOffset);
          GspBlockGenerator.createGspBlockByTextRange(result, child, wrap, alignment, myXmlFormattingPolicy, trashRange);
        }
        XmlTag[] nestedGspTags = getSubTags();
        GspBlockGenerator.createHtmlBlockByChildNode(result, myNode, tag.getNode(), wrap,
                alignment, myXmlFormattingPolicy, nestedGspTags);
        int tagEndOffset = tag.getTextRange().getEndOffset();
        if (tagEndOffset == myTextRange.getEndOffset()) {
          myTextRange = new TextRange(tagEndOffset, tagEndOffset);
          return child.getTreeNext();
        } else {
          // Outer element is a outers element
          ASTNode newChild = myNode.findLeafElementAt(tagEndOffset - myNode.getStartOffset());
          myTextRange = new TextRange(tagEndOffset, myTextRange.getEndOffset());
          while (newChild != null && newChild.getTreeParent() != myNode) {
            newChild = newChild.getTreeParent();
          }
          return newChild;
        }
      }
    }
    GspBlockGenerator.createGspBlockByTextRange(result, child, wrap, alignment, myXmlFormattingPolicy,
            myTextRange.intersection(child.getTextRange()));
    myTextRange = new TextRange(child.getTextRange().getEndOffset(), myTextRange.getEndOffset());
    return child.getTreeNext();
  }

  private static ArrayList<PsiElement> processJavascriptTagBody(PsiElement child) {
    ArrayList<PsiElement> childrenToSkip = new ArrayList<>();
    PsiElement parent = child.getParent();
    assert parent instanceof GspGrailsTag;

    while (child != null) {
      if (JavaScriptIntegrationUtil.isJavaScriptInjection(child)) {
        childrenToSkip.add(child);
      }
      child = child.getNextSibling();
    }

    return childrenToSkip;
  }

  private static boolean isGoodTag(XmlTag tag) {
    for (PsiElement element : tag.getChildren()) {
      if (element instanceof PsiErrorElement) {
        return false;
      }
    }
    return true;
  }

  private static @Nullable XmlTag getHtmlTagByOffset(int htmlTagOffset, ASTNode child) {
    final FileViewProvider viewProvider = child.getPsi().getContainingFile().getViewProvider();
    final PsiFile file = viewProvider.getPsi(HTMLLanguage.INSTANCE);
    ASTNode found = file.getNode().findLeafElementAt(htmlTagOffset);
    if (found != null) {
      final ASTNode foundTag = findTagParentWithTheSameOffset(found);
      if (foundTag == null) return null;
      final PsiElement foundPsiElement = foundTag.getPsi();
      if (foundPsiElement instanceof XmlTag) {
        return (XmlTag) foundPsiElement;
      }
    }
    return null;
  }

  protected static ASTNode findTagParentWithTheSameOffset(final ASTNode correspondingNode) {
    int offset = correspondingNode.getTextRange().getStartOffset();
    ASTNode result = correspondingNode;
    while (result.getTreeParent() != null
            && result.getTreeParent().getTextRange().getStartOffset() == offset) {
      if (result.getTreeParent().getPsi() instanceof XmlTag) return result.getTreeParent();
      result = result.getTreeParent();
    }
    return result;
  }


  private int calculatePossibleHtmlTagBegin(ASTNode child) {
    final FileViewProvider viewProvider = child.getPsi().getContainingFile().getViewProvider();
    final PsiFile file = viewProvider.getPsi(HTMLLanguage.INSTANCE);
    assert file != null;
    TextRange range = child.getTextRange();
    int curOffset = Math.max(range.getStartOffset(), myTextRange.getStartOffset());
    int endOffset = Math.min(range.getEndOffset(), myTextRange.getEndOffset());

    ASTNode astNode = file.getNode();
    assert astNode != null;
    ASTNode leaf = astNode.findLeafElementAt(curOffset);
    while (leaf != null && curOffset < endOffset && leaf.getElementType() != XmlTokenType.XML_START_TAG_START) {
      curOffset++;
      leaf = astNode.findLeafElementAt(curOffset);
    }

    if (leaf == null) return -1;
    if (leaf.getElementType() == XmlTokenType.XML_START_TAG_START) return curOffset;
    return -1;
  }


  protected ASTNode processChild(List<Block> result,
                                 final ASTNode child,
                                 final Wrap wrap,
                                 final Alignment alignment,
                                 final Indent indent) {
    final PsiElement childPsi = child.getPsi();
    if (!myTextRange.intersectsStrict(child.getTextRange())) return child.getTreeNext();
    if (childPsi instanceof OuterLanguageElement) {
      return processNonGspChild(child, indent, result, wrap, alignment);
    } else {
      GspBlockGenerator.createGspBlockByChildNode(result, myNode, child, wrap, alignment, myXmlFormattingPolicy);
      myTextRange = new TextRange(child.getTextRange().getEndOffset(), myTextRange.getEndOffset());
      return child.getTreeNext();
    }
  }

  protected void createGspGroovyNode(final List<Block> localResult, final ASTNode child, final Indent indent) {
    localResult.add(new GspGroovyBlock(child, myXmlFormattingPolicy, GspGroovyBlock.findPsiRootAt(child), indent));
  }

  public static ASTNode findChildAfter(final @NotNull ASTNode child, final int endOffset) {
    TreeElement fileNode = TreeUtil.getFileElement((TreeElement) child);
    final LeafElement leaf = fileNode.findLeafElementAt(endOffset);
    if (leaf != null && leaf.getStartOffset() == endOffset && endOffset > 0) {
      return fileNode.findLeafElementAt(endOffset - 1);
    }
    return leaf;
  }

  public boolean doesNotIntersectSubTagsWith(final PsiElement tag) {
    final TextRange tagRange = tag.getTextRange();
    final XmlTag[] subTags = getSubTags();
    for (XmlTag subTag : subTags) {
      final TextRange subTagRange = subTag.getTextRange();
      if (subTagRange.getEndOffset() < tagRange.getStartOffset()) continue;
      if (subTagRange.getStartOffset() > tagRange.getEndOffset()) return true;

      if (GspHtmlBlock.areOvercrossing(subTagRange, tagRange))
        return false;
    }
    return true;
  }


  public static XmlTag[] collectSubTags(final XmlElement node) {
    final List<XmlTag> result = new ArrayList<>();
    node.processElements(new PsiElementProcessor() {
      @Override
      public boolean execute(final @NotNull PsiElement element) {
        if (element instanceof XmlTag) {
          result.add((XmlTag) element);
        }
        return true;
      }
    }, node);
    return result.toArray(XmlTag.EMPTY);
  }

  public XmlTag[] getSubTags() {

    if (myNode instanceof XmlTag) {
      return ((XmlTag) myNode.getPsi()).getSubTags();
    } else if (myNode.getPsi() instanceof XmlElement) {
      return collectSubTags((XmlElement) myNode.getPsi());
    } else {
      return XmlTag.EMPTY;
    }
  }

  public boolean canBeAnotherTreeTagStart(final ASTNode child) {
    boolean can = GspPsiUtil.getGspFile(myNode.getPsi()) != null
            && (isXmlTag(myNode) || myNode.getElementType() == XmlElementType.XML_DOCUMENT || myNode.getPsi() instanceof PsiFile)
            && child.getPsi() instanceof OuterLanguageElement;
    return can;
  }

  protected static boolean isXmlTag(final ASTNode child) {
    return isXmlTag(child.getPsi());
  }

  protected static boolean isXmlTag(final PsiElement psi) {
    return psi instanceof XmlTag
            && !(psi instanceof GspScriptletTag)
            && !(psi instanceof GspDirective)
            && !(psi instanceof GspDeclarationTag);
  }


}
