// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.lang.gsp.parsing.gsp;

import com.intellij.lang.ASTNode;
import com.intellij.lang.PsiBuilder;
import com.intellij.lang.PsiParser;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import com.intellij.psi.xml.XmlElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.grails.GrailsBundle;
import org.jetbrains.plugins.grails.lang.gsp.parsing.GspElementTypes;
import org.jetbrains.plugins.groovy.lang.parser.parsing.util.ParserUtils;

import static com.intellij.psi.xml.XmlTokenType.XML_END_TAG_START;
import static com.intellij.psi.xml.XmlTokenType.XML_START_TAG_START;
import static com.intellij.psi.xml.XmlTokenType.XML_TAG_END;
import static com.intellij.psi.xml.XmlTokenType.XML_TAG_NAME;

public class GspParser implements PsiParser, GspElementTypes {

  @Override
  public @NotNull ASTNode parse(final @NotNull IElementType root, final @NotNull PsiBuilder builder) {

    builder.enforceCommentTokens(TokenSet.EMPTY);
    final PsiBuilder.Marker file = builder.mark();
    final PsiBuilder.Marker docMarker = builder.mark();
    builder.mark().done(XmlElementType.XML_PROLOG);

    final PsiBuilder.Marker rootTag = builder.mark();
    while (true) {
      parseVariousTagContent(builder, false);
      if (builder.eof()) break;
    }
    rootTag.done(GSP_ROOT_TAG);
    docMarker.done(GSP_XML_DOCUMENT);
    file.done(GspParserDefinition.GSP_FILE);
    return builder.getTreeBuilt();
  }

  public static void parseVariousTagContent(PsiBuilder builder, boolean isInGrailsTag) {
    IElementType tokenType = builder.getTokenType();
    if (JSCRIPT_BEGIN == tokenType || GSCRIPT_BEGIN == tokenType) {
      parseScriptlet(builder);
      return;
    }
    if (JEXPR_BEGIN == tokenType || GEXPR_BEGIN == tokenType) {
      parseExprInjection(builder);
      return;
    }
    if (JDECLAR_BEGIN == tokenType || GDECLAR_BEGIN == tokenType) {
      parseDeclaration(builder);
      return;
    }
    if (XML_START_TAG_START == tokenType) {
      GrailsTag.parse(builder);
      return;
    }
    if (XML_END_TAG_START == tokenType) {
      if (!isInGrailsTag) {
        ParserUtils.getToken(builder, XML_END_TAG_START);
        builder.error(GrailsBundle.message("clos.tag.in.wrong.place"));
        ParserUtils.getToken(builder, XML_TAG_NAME);
        ParserUtils.getToken(builder, XML_TAG_END);
      }
      return;
    }
    if (!builder.eof()) {
      builder.advanceLexer();
    }
  }

  private static void parseScriptlet(PsiBuilder builder) {
    PsiBuilder.Marker marker = builder.mark();
    builder.advanceLexer();
    if (GSP_GROOVY_CODE == builder.getTokenType()) {
      builder.advanceLexer();
    }
    if (!(GSCRIPT_END == builder.getTokenType() || JSCRIPT_END == builder.getTokenType())) {
      builder.error(GrailsBundle.message("script.end.tag.expected"));
    }
    else {
      builder.advanceLexer();
    }
    marker.done(GSP_SCRIPTLET_TAG);
  }

  private static void parseExprInjection(PsiBuilder builder) {
    PsiBuilder.Marker marker = builder.mark();
    builder.advanceLexer();
    if (GSP_GROOVY_CODE == builder.getTokenType()) {
      builder.advanceLexer();
    }
    if (!(GEXPR_END == builder.getTokenType() || JEXPR_END == builder.getTokenType())) {
      builder.error(GrailsBundle.message("script.end.tag.expected"));
    }
    else {
      builder.advanceLexer();
    }
    marker.done(GSP_EXPR_TAG);
  }

  private static void parseDeclaration(PsiBuilder builder) {
    PsiBuilder.Marker marker = builder.mark();
    builder.advanceLexer();
    if (GSP_GROOVY_CODE == builder.getTokenType()) {
      builder.advanceLexer();
    }
    if (!(GDECLAR_END == builder.getTokenType() || JDECLAR_END == builder.getTokenType())) {
      builder.error(GrailsBundle.message("script.end.tag.expected"));
    }
    else {
      builder.advanceLexer();
    }
    marker.done(GSP_DECLARATION_TAG);
  }
}
