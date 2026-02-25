// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.lang.gsp.parsing.gsp.chameleons;

import com.intellij.lang.ASTFactory;
import com.intellij.lang.ASTNode;
import com.intellij.lexer.Lexer;
import com.intellij.lexer.LexerUtil;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.psi.impl.source.tree.CompositeElement;
import com.intellij.psi.impl.source.tree.Factory;
import com.intellij.psi.impl.source.tree.TreeElement;
import com.intellij.psi.tree.CustomParsingType;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.ILazyParseableElementType;
import com.intellij.util.CharTable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.grails.GrailsBundle;
import org.jetbrains.plugins.grails.lang.gsp.GspLanguage;
import org.jetbrains.plugins.grails.lang.gsp.lexer.GspDirectiveFlexLexer;
import org.jetbrains.plugins.grails.lang.gsp.lexer.core.GspTokenTypes;
import org.jetbrains.plugins.grails.lang.gsp.parsing.GspElementTypes;
import org.jetbrains.plugins.grails.lang.gsp.psi.gsp.impl.directive.GspDirectiveImpl;

import static com.intellij.psi.xml.XmlTokenType.XML_ATTRIBUTE_VALUE_END_DELIMITER;
import static com.intellij.psi.xml.XmlTokenType.XML_ATTRIBUTE_VALUE_START_DELIMITER;
import static com.intellij.psi.xml.XmlTokenType.XML_ATTRIBUTE_VALUE_TOKEN;
import static com.intellij.psi.xml.XmlTokenType.XML_EQ;
import static com.intellij.psi.xml.XmlTokenType.XML_NAME;
import static com.intellij.psi.xml.XmlTokenType.XML_TAG_NAME;
import static com.intellij.psi.xml.XmlTokenType.XML_WHITE_SPACE;

public class GspDirectiveElement extends CustomParsingType implements GspTokenTypes, GspElementTypes {
  public GspDirectiveElement(String debugName) {
    super(debugName, GspLanguage.INSTANCE);
  }

  @Override
  public @NotNull ASTNode parse(@NotNull CharSequence text, @NotNull CharTable table) {
    CompositeElement root = new GspDirectiveImpl();
    final Lexer lexer = new GspDirectiveFlexLexer() {
      @Override
      public IElementType getTokenType() {
        IElementType type = super.getTokenType();
        if (type == XML_TAG_NAME) return XML_NAME;
        return type;
      }
    };

    lexer.start(text);

    parseDirective(lexer, root, table);
    if (lexer.getTokenType() != null) {
      final CompositeElement errorElement = addErrorElement(root, GrailsBundle.message("gsp.unparseable.content"));
      while (lexer.getTokenType() != null) {
        addAndAdvance(errorElement, lexer, table);
      }
    }
    return root;
  }


  private static void parseDirective(final Lexer lexer, final CompositeElement treeElement, CharTable table) {
    if (lexer.getTokenType() == GDIRECT_BEGIN || lexer.getTokenType() == JDIRECT_BEGIN) {
      addAndAdvance(treeElement, lexer, table);
    }
    else {
      return;
    }
    if (lexer.getTokenType() == XML_WHITE_SPACE) addAndAdvance(treeElement, lexer, table);
    if (lexer.getTokenType() != XML_NAME) return;
    addAndAdvance(treeElement, lexer, table);
    while (lexer.getTokenType() == XML_WHITE_SPACE || lexer.getTokenType() == XML_NAME) {
      if (lexer.getTokenType() == XML_WHITE_SPACE) addAndAdvance(treeElement, lexer, table);
      if (lexer.getTokenType() == XML_NAME) {
        final CompositeElement attribute = ASTFactory.composite(GSP_DIRECTIVE_ATTRIBUTE);
        treeElement.rawAddChildren(attribute);
        parseAttribute(lexer, attribute, table);
      }
    }
    if (lexer.getTokenType() == GDIRECT_END || lexer.getTokenType() == JDIRECT_END) {
      addAndAdvance(treeElement, lexer, table);
    }
  }

  private static void parseAttribute(final Lexer lexer, final CompositeElement attribute, final CharTable table) {
    addAndAdvance(attribute, lexer, table);
    if (lexer.getTokenType() == XML_WHITE_SPACE) addAndAdvance(attribute, lexer, table);
    if (lexer.getTokenType() != XML_EQ) {
      addErrorElement(attribute, GrailsBundle.message("expected.attribute.eq.sign"));

      return;
    }
    addAndAdvance(attribute, lexer, table);
    if (lexer.getTokenType() == XML_WHITE_SPACE) addAndAdvance(attribute, lexer, table);
    if (lexer.getTokenType() != XML_ATTRIBUTE_VALUE_START_DELIMITER) {
      addErrorElement(attribute, GrailsBundle.message("attribute.value.expected"));
      return;
    }

    final CompositeElement attributeValue = ASTFactory.composite(GSP_DIRECTIVE_ATTRIBUTE_VALUE);
    attribute.rawAddChildren(attributeValue);
    addAndAdvance(attributeValue, lexer, table);
    if (lexer.getTokenType() == XML_ATTRIBUTE_VALUE_TOKEN) addAndAdvance(attributeValue, lexer, table);
    if (lexer.getTokenType() != XML_ATTRIBUTE_VALUE_END_DELIMITER) {
      addErrorElement(attributeValue, GrailsBundle.message("quote.expected"));
      return;
    }
    addAndAdvance(attributeValue, lexer, table);
  }

  private static void addAndAdvance(final CompositeElement attribute, final Lexer lexer, final CharTable table) {
    attribute.rawAddChildren(createTokenElement(lexer, table));
    lexer.advance();
  }

  private static CompositeElement addErrorElement(final CompositeElement treeElement, @NotNull @NlsContexts.DetailedDescription String message) {
    final CompositeElement errorElement = Factory.createErrorElement(message);
    treeElement.rawAddChildren(errorElement);
    return errorElement;
  }

  private static @Nullable TreeElement createTokenElement(Lexer lexer, CharTable table) {
    IElementType tokenType = lexer.getTokenType();
    if (tokenType == null) return null;

    if (tokenType instanceof ILazyParseableElementType) {
      return ASTFactory.lazy((ILazyParseableElementType)tokenType, LexerUtil.internToken(lexer, table));
    }

    return ASTFactory.leaf(tokenType, LexerUtil.internToken(lexer, table));
  }


}
