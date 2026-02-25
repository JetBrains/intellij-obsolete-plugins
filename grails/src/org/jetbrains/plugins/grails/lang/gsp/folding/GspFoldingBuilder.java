// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.lang.gsp.folding;

import com.intellij.lang.ASTNode;
import com.intellij.lang.folding.FoldingBuilder;
import com.intellij.lang.folding.FoldingDescriptor;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlComment;
import com.intellij.psi.xml.XmlTag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.grails.lang.gsp.parsing.GspElementTypes;
import org.jetbrains.plugins.grails.lang.gsp.psi.gsp.api.gtag.GspGrailsTag;

import java.util.ArrayList;
import java.util.List;

public final class GspFoldingBuilder implements FoldingBuilder, GspElementTypes, DumbAware {

  private static final TokenSet GSP_TAGS = TokenSet.create(GSP_SCRIPTLET_TAG,
          GSP_DIRECTIVE,
          GSP_EXPR_TAG,
          GSP_DECLARATION_TAG);

  @Override
  public FoldingDescriptor @NotNull [] buildFoldRegions(@NotNull ASTNode node, @NotNull Document document) {
    List<FoldingDescriptor> descriptors = new ArrayList<>();
    appendGspDescriptors(node, descriptors);
    return descriptors.toArray(FoldingDescriptor.EMPTY_ARRAY);
  }

  private static void appendGspDescriptors(ASTNode node, List<FoldingDescriptor> descriptors) {
    // comments
    IElementType elementType = node.getElementType();
    if ((elementType == GSP_STYLE_COMMENT ||
            elementType == JSP_STYLE_COMMENT) &&
            isMultiline(node)) {
      descriptors.add(new FoldingDescriptor(node, node.getTextRange()));
    }
    if (elementType == GRAILS_TAG) {
      appendGrailsTagDescriptors(node, descriptors);
    }
    if (GSP_DIRECTIVE != elementType &&
            GSP_TAGS.contains(elementType) &&
            isMultiline(node)) {
      descriptors.add(new FoldingDescriptor(node, node.getTextRange()));
    }

    ASTNode child = node.getFirstChildNode();
    while (child != null) {
      appendGspDescriptors(child, descriptors);
      child = child.getTreeNext();
    }

  }

  private static void appendGrailsTagDescriptors(ASTNode node, List<FoldingDescriptor> descriptors) {
    PsiElement element = node.getPsi();
    if (!(element instanceof GspGrailsTag tag)) return;
    if (!isMultiline(node) || tag.endsByError()) return;
    int tagEndOffset = tag.isEmpty() ? tag.getTextRange().getEndOffset() - 2 : tag.getTextRange().getEndOffset() - 1;

    if (!tag.isValid()) return;
    XmlAttribute[] attributes = tag.getAttributes();
    if (attributes.length > 0) {
      int listEndOffset = attributes[0].getTextRange().getEndOffset();
      if (listEndOffset < tagEndOffset - 1) {
        TextRange range = new TextRange(listEndOffset, tagEndOffset);
        descriptors.add(new FoldingDescriptor(node, range));
      }
      return;
    }

    PsiElement identifier = tag.getNameElement();
    if (identifier != null) {
      int idOffset = identifier.getTextRange().getEndOffset();
      if (idOffset < tagEndOffset - 1) {
        TextRange range = new TextRange(idOffset, tagEndOffset);
        descriptors.add(new FoldingDescriptor(node, range));
      }
    }

  }

  private static boolean isMultiline(ASTNode node) {
    return (node.getText().contains("\n") || node.getText().contains("\t"));
  }


  @Override
  public String getPlaceholderText(@NotNull ASTNode node) {

    final IElementType elemType = node.getElementType();
    if (elemType == JSP_STYLE_COMMENT) return "<%--...--%>";
    if (elemType == GSP_STYLE_COMMENT) return "%{--...--}%";
    if (GRAILS_TAG.equals(elemType)) return "...";
    if (GSP_TAGS.contains(elemType)) {
      ASTNode childNode = node.getFirstChildNode();
      assert childNode != null;
      if (childNode.getElementType() == GSCRIPT_BEGIN) return "%{...}%";
      if (childNode.getElementType() == GEXPR_BEGIN) return "${...}";
      if (childNode.getElementType() == GDECLAR_BEGIN) return "!{...}!";
      if (childNode.getElementType() == GDIRECT_BEGIN) return "@{...}";
      if (childNode.getElementType() == JSCRIPT_BEGIN) return "<%...%>";
      if (childNode.getElementType() == JEXPR_BEGIN) return "<%=...%>";
      if (childNode.getElementType() == JDECLAR_BEGIN) return "<%!...%>";
      if (childNode.getElementType() == JDIRECT_BEGIN) return "<%@...%>";
      else return null;
    }

    PsiElement psi = node.getPsi();
    if (psi instanceof XmlComment) return "...";

    // Hack for some intentions
    if (psi instanceof XmlTag) return "...";

    return null;
  }

  @Override
  public boolean isCollapsedByDefault(@NotNull ASTNode node) {
    return false;
  }


}
