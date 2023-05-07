/*
 * Copyright (c) 2000-2005 by JetBrains s.r.o. All Rights Reserved.
 * Use is subject to license terms.
 */
package com.intellij.play.language;

import com.intellij.lang.ASTNode;
import com.intellij.play.language.groovy.GroovyExpressionLazyParseableElementType;
import com.intellij.play.language.psi.PlayNameValueCompositeElement;
import com.intellij.play.language.psi.PlayTag;
import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.IFileElementType;
import com.intellij.psi.tree.TokenSet;

public interface PlayElementTypes extends PlaySimpleElementTypes {
  IFileElementType PLAY_FILE = new IFileElementType("PLAY_FILE",PlayLanguage.INSTANCE);

  // texts
  PlayTokenType STRING_TEXT = new PlayTokenType("STRING_TEXT");
  PlayTokenType TEMPLATE_TEXT = new PlayTokenType("TEMPLATE_TEXT");

  // expressions ${...}
  PlayTokenType EL_START = new PlayTokenType("EL_START");
  PlayTokenType EL_END = new PlayTokenType("EL_END");
  IElementType EL_EXPRESSION =  new GroovyExpressionLazyParseableElementType("EL_EXPRESSION");

  // Template decorators : #{extends /} and #{doLayout /}
  PlayTokenType TEMPLATE_DECORATOR_START = new PlayTokenType("#{");
  PlayTokenType TEMPLATE_DECORATOR_END = new PlayTokenType("}");

  IElementType NAME_VALUE_PAIR = new PlayCompositeElementType("NameValuePair") {
    @Override
    public PsiElement createPsiElement(final ASTNode node) {
      return new PlayNameValueCompositeElement(node);
    }
  };
  PlayTokenType ATTR_NAME = new PlayTokenType("ATTR_NAME");

  // Tags: #{tagName /}
  PlayTokenType TAG_START = new PlayTokenType("TAG_START");          // #{
  PlayTokenType END_TAG_START = new PlayTokenType("END_TAG_START");  // #{/
  PlayTokenType CLOSE_TAG = new PlayTokenType("CLOSE_TAG");          // /}
  PlayTokenType TAG_END = new PlayTokenType("TAG_END");        // }
  PlayTokenType TAG_NAME = new PlayTokenType("TAG_NAME");

  IElementType TAG_EXPRESSION =  new TagExpressionElementType();
  IElementType TAG =  new PlayCompositeElementType("TAG") {
    @Override
    public PsiElement createPsiElement(ASTNode node) {
      return new PlayTag(node);
    }
  };

  // Actions: @{...} or @@{...}
  PlayTokenType ACTION_START = new PlayTokenType("ACTION_START");
  PlayTokenType ACTION_DOUBLE_START = new PlayTokenType("ACTION_DOUBLE_START");
  PlayTokenType ACTION_END = new PlayTokenType("ACTION_END");

  IElementType ACTION_SCRIPT = new PlayActionElementType();


  // Messages: &{...}
  PlayTokenType MESSAGE_START = new PlayTokenType("MESSAGE_START");
  PlayTokenType MESSAGE_END = new PlayTokenType("MESSAGE_END");
  IElementType MESSAGE_TEXT = new PlayMessageElementType();
  IElementType MESSAGE_PARAMETER = new PlayMessageParameterElementType();

  // Comment: *{...}*
  PlayTokenType COMMENT_START = new PlayTokenType("COMMENT_START");    // *{
  PlayTokenType COMMENT_END = new PlayTokenType("COMMENT_END");        // }*
  PlayTokenType COMMENT_TEXT = new PlayTokenType("COMMENT_TEXT");

  // Scripts:  %{...}%
  PlayTokenType SCRIPT_START = new PlayTokenType("SCRIPT_START");
  PlayTokenType SCRIPT_START_TOO = new PlayTokenType("SCRIPT_START_TOO");
  PlayTokenType SCRIPT_END = new PlayTokenType("SCRIPT_END");
  PlayTokenType GROOVY_SCRIPT = new PlayTokenType("GROOVY_SCRIPT");

  TokenSet STRING_LITERALS = TokenSet.create(SINGLE_QUOTE, DOUBLE_QUOTE, STRING_TEXT, TEMPLATE_TEXT);
}
