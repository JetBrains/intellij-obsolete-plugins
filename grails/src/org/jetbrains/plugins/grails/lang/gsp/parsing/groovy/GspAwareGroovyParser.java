// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.lang.gsp.parsing.groovy;

import com.intellij.lang.PsiBuilder;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.plugins.grails.lang.gsp.lexer.GspTokenTypesEx;
import org.jetbrains.plugins.groovy.lang.parser.GroovyParser;

import static org.jetbrains.plugins.grails.lang.gsp.lexer.core.GspTokenTypes.GROOVY_ATTR_VALUE;
import static org.jetbrains.plugins.grails.lang.gsp.lexer.core.GspTokenTypes.GROOVY_DECLARATION;
import static org.jetbrains.plugins.grails.lang.gsp.lexer.core.GspTokenTypes.GROOVY_EXPR_CODE;
import static org.jetbrains.plugins.grails.lang.gsp.lexer.core.GspTokenTypes.GSP_MAP_ATTR_VALUE;
import static org.jetbrains.plugins.grails.lang.gsp.parsing.GspGroovyElementTypes.GSP_RUN_BLOCK;

public class GspAwareGroovyParser extends GroovyParser {

  @Override
  protected boolean parse_root_(IElementType t, PsiBuilder b) {
    if (t == GSP_MAP_ATTR_VALUE) {
      return list_or_map(b, 0);
    }
    else if (t == GROOVY_ATTR_VALUE || t == GROOVY_EXPR_CODE) {
      return expression_or_application(b, 0);
    }
    else if (t == GROOVY_DECLARATION) {
      return class_body_inner(b, 0);
    }
    else if (t == GSP_RUN_BLOCK) {
      return block_levels(b, 0);
    }
    else {
      throw new IllegalArgumentException("Unexpected element type: " + t);
    }
  }

  @Override
  public boolean parseDeep() {
    return true;
  }

  @Override
  protected boolean isExtendedSeparator(final IElementType tokenType) {
    return GspTokenTypesEx.GSP_GROOVY_SEPARATORS.contains(tokenType);
  }

  @Override
  protected boolean parseExtendedStatement(PsiBuilder builder) {
    return GspTemplateStmtParsing.parseGspTemplateStmt(builder);
  }
}
