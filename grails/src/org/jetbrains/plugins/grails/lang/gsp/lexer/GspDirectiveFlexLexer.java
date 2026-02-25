// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.lang.gsp.lexer;

import com.intellij.lexer.FlexAdapter;
import com.intellij.lexer.MergingLexerAdapter;
import com.intellij.psi.tree.TokenSet;
import com.intellij.psi.xml.XmlTokenType;
import org.jetbrains.plugins.grails.lang.gsp.lexer.core.GspTokenTypes;
import org.jetbrains.plugins.grails.lang.gsp.lexer.core._GspDirectiveLexer;

public class GspDirectiveFlexLexer extends MergingLexerAdapter {

  private static final TokenSet TOKENS_TO_MERGE = TokenSet.create(XmlTokenType.XML_WHITE_SPACE,
      XmlTokenType.XML_ATTRIBUTE_VALUE_TOKEN,
      GspTokenTypes.GSP_BAD_CHARACTER);

  public GspDirectiveFlexLexer() {
    super(new FlexAdapter(new _GspDirectiveLexer(null)), TOKENS_TO_MERGE);
  }
}

