// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.lang.gsp.parsing.groovy;

import com.intellij.lang.PsiBuilder;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.plugins.grails.GrailsBundle;
import org.jetbrains.plugins.grails.lang.gsp.lexer.GspTokenTypesEx;
import org.jetbrains.plugins.grails.lang.gsp.parsing.GspGroovyElementTypes;
import org.jetbrains.plugins.groovy.GroovyBundle;

public class GspTemplateStmtParsing implements GspTokenTypesEx, GspGroovyElementTypes {

  public static boolean parseGspTemplateStmt(PsiBuilder builder) {
    boolean smthParsed = false;
    if (JSCRIPT_END.equals(builder.getTokenType()) || GSCRIPT_END.equals(builder.getTokenType())) {
      eatTemplateStatement(builder);
      smthParsed = true;
    }

    IElementType tt = builder.getTokenType();

    while (GSP_TEMPLATE_DATA == tt || GSP_MAP_ATTR_VALUE == tt || GROOVY_ATTR_VALUE == tt || GSP_COMMENTS.contains(tt) ||
            GSP_GROOVY_SEPARATORS.contains(tt)) {
      if (JSCRIPT_BEGIN == tt || GSCRIPT_BEGIN == tt) {
        eatTemplateStatement(builder);
        return true;
      }

      smthParsed = true;
      if (GSP_TEMPLATE_DATA == tt || GSP_COMMENTS.contains(tt)) {
        eatTemplateStatement(builder);
      }
      /*
      ${...} or <%= ... %> injection
       */
      else if (JEXPR_BEGIN == tt || GEXPR_BEGIN == tt) {
        parseExprInjection(builder);
      }
      /*
      !{...}! or <%! ... %> declaration
       */
      else if (JDECLAR_BEGIN == tt || GDECLAR_BEGIN == tt) {
        parseDeclaration(builder);
      }
      else {
        builder.advanceLexer();
      }

      tt = builder.getTokenType();
    }

    return smthParsed;
  }

  private static void eatTemplateStatement(PsiBuilder builder) {
    builder.advanceLexer();
  }

  private static void parseExprInjection(PsiBuilder builder) {
    eatTemplateStatement(builder);
    if (GROOVY_EXPR_CODE.equals(builder.getTokenType())) {
      builder.advanceLexer();
    } else {
      builder.error(GroovyBundle.message("expression.expected"));
    }
    if (JEXPR_END.equals(builder.getTokenType()) ||
            GEXPR_END.equals(builder.getTokenType())) {
      eatTemplateStatement(builder);
    } else {
      builder.error(GrailsBundle.message("expr.closing.end.tag.expected"));
      while (!builder.eof()) {
        builder.advanceLexer();
      }
    }
  }

  private static void parseDeclaration(PsiBuilder builder) {
    eatTemplateStatement(builder);
    if (GROOVY_DECLARATION.equals(builder.getTokenType())) {
      builder.advanceLexer();
    } else {
      builder.error(GrailsBundle.message("declaraion.expected"));
    }
    if (JDECLAR_END.equals(builder.getTokenType()) ||
            GDECLAR_END.equals(builder.getTokenType())) {
      eatTemplateStatement(builder);
    } else {
      builder.error(GrailsBundle.message("expr.closing.end.tag.expected"));
      while (!builder.eof()) {
        builder.advanceLexer();
      }
    }
  }

}
