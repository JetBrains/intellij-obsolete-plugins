// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.lang.gsp.formatter.processors;

import com.intellij.formatting.Spacing;
import com.intellij.lang.ASTNode;
import com.intellij.psi.formatter.xml.XmlFormattingPolicy;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import com.intellij.psi.xml.IXmlTagElementType;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlElementType;
import com.intellij.psi.xml.XmlTag;
import org.jetbrains.plugins.grails.lang.gsp.formatter.AbstractGspBlock;
import org.jetbrains.plugins.grails.lang.gsp.lexer.GspTokenTypesEx;
import org.jetbrains.plugins.grails.lang.gsp.parsing.GspElementTypes;
import org.jetbrains.plugins.grails.lang.gsp.psi.gsp.api.GspExpressionTag;

import static com.intellij.psi.xml.XmlTokenType.XML_BAD_CHARACTER;
import static com.intellij.psi.xml.XmlTokenType.XML_EMPTY_ELEMENT_END;
import static com.intellij.psi.xml.XmlTokenType.XML_END_TAG_START;
import static com.intellij.psi.xml.XmlTokenType.XML_EQ;
import static com.intellij.psi.xml.XmlTokenType.XML_NAME;
import static com.intellij.psi.xml.XmlTokenType.XML_TAG_END;
import static com.intellij.psi.xml.XmlTokenType.XML_TAG_NAME;

public final class GspSpacingProcessor implements GspTokenTypesEx {

  private static final Spacing NO_SPACING_WITH_NEWLINE = Spacing.createSpacing(0, 0, 0, true, 1);
  private static final Spacing NO_SPACING = Spacing.createSpacing(0, 0, 0, false, 0);
  private static final Spacing COMMON_SPACING = Spacing.createSpacing(1, 1, 0, true, 100);

  private static final TokenSet GSP_GROOVY_EXPR_SEPARATORS = TokenSet.create(
          JEXPR_BEGIN,
          GEXPR_BEGIN,
          GEXPR_END
  );

  private GspSpacingProcessor() {
  }

  public static Spacing getSpacing(XmlFormattingPolicy policy, AbstractGspBlock child1, AbstractGspBlock child2) {

    ASTNode leftNode = child1.getNode();
    ASTNode rightNode = child2.getNode();

    IElementType lt = leftNode.getElementType();
    IElementType rt = rightNode.getElementType();

    if (isXmlTagName(lt, rt)) {
      if (policy.getShouldAddSpaceAroundTagName()) {
        return Spacing.createSpacing(1, 1, 0, policy.getShouldKeepLineBreaks(), policy.getKeepBlankLines());
      }
    }

    if (lt == XML_TAG_END || rt == XML_END_TAG_START) {
      return NO_SPACING_WITH_NEWLINE;
    }

    if (rt == XML_TAG_END) return NO_SPACING;

    if (rt == XML_EMPTY_ELEMENT_END) {
      if (policy.addSpaceIntoEmptyTag()) {
        return Spacing.createSpacing(1, 1, 0, policy.getShouldKeepLineBreaks(), policy.getKeepBlankLines());
      }

      return NO_SPACING;
    }

    if (rt == XML_BAD_CHARACTER || lt == XML_BAD_CHARACTER) {
      return null;
    }

    if (leftNode.getPsi() instanceof XmlAttribute || rightNode.getPsi() instanceof XmlAttribute) {
      return COMMON_SPACING;
    }
    if (XML_EQ == lt || XML_EQ == rt) {
      int spaces = policy.getShouldAddSpaceAroundEqualityInAttribute() ? 1 : 0;
      return Spacing.createSpacing(spaces, spaces, 0, policy.getShouldKeepLineBreaks(), policy.getKeepBlankLines());
    }

    if (GSP_GROOVY_EXPR_SEPARATORS.contains(lt) ||
            GSP_GROOVY_EXPR_SEPARATORS.contains(rt)) {
      return NO_SPACING_WITH_NEWLINE;
    }
    if (rt == JEXPR_END && rightNode.getTreeParent().getPsi() instanceof GspExpressionTag) {
      return NO_SPACING_WITH_NEWLINE;
    }

    if (GSP_GROOVY_SEPARATORS.contains(lt) && !rightNode.getText().trim().isEmpty() ||
        GSP_GROOVY_SEPARATORS.contains(rt) && !leftNode.getText().trim().isEmpty()) {
      return COMMON_SPACING;
    }

    if (rt instanceof IXmlTagElementType && lt instanceof IXmlTagElementType) {
      if (policy.insertLineBreakBeforeTag((XmlTag)rightNode.getPsi())) {
        return Spacing.createSpacing(0, Integer.MAX_VALUE, 2, policy.getShouldKeepLineBreaks(),
                                     policy.getKeepBlankLines());
      }
    }

    return null;
  }

  private static boolean isXmlTagName(final IElementType type1, final IElementType type2) {
    if ((type1 == XML_NAME || type1 == XML_TAG_NAME) &&
        type2 == XML_TAG_END) {
      return true;
    }

    if ((type1 == XML_NAME || type1 == XML_TAG_NAME) &&
        type2 == XML_EMPTY_ELEMENT_END) {
      return true;
    }

    if ((type1 == XmlElementType.XML_ATTRIBUTE || type1 == GspElementTypes.GRAILS_TAG_ATTRIBUTE) &&
        type2 == XML_EMPTY_ELEMENT_END) {
      return true;
    }

    return ((type1 == XmlElementType.XML_ATTRIBUTE || type1 == GspElementTypes.GRAILS_TAG_ATTRIBUTE) &&
            type2 == XML_TAG_END);
  }
}
