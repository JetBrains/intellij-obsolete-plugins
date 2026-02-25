// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.lang.gsp.parsing.gsp;

import com.intellij.lang.PsiBuilder;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.plugins.grails.GrailsBundle;
import org.jetbrains.plugins.grails.lang.gsp.parsing.GspElementTypes;
import org.jetbrains.plugins.groovy.GroovyBundle;
import org.jetbrains.plugins.groovy.lang.parser.parsing.util.ParserUtils;

import static com.intellij.psi.xml.XmlTokenType.XML_ATTRIBUTE_VALUE_END_DELIMITER;
import static com.intellij.psi.xml.XmlTokenType.XML_ATTRIBUTE_VALUE_START_DELIMITER;
import static com.intellij.psi.xml.XmlTokenType.XML_EMPTY_ELEMENT_END;
import static com.intellij.psi.xml.XmlTokenType.XML_END_TAG_START;
import static com.intellij.psi.xml.XmlTokenType.XML_EQ;
import static com.intellij.psi.xml.XmlTokenType.XML_NAME;
import static com.intellij.psi.xml.XmlTokenType.XML_START_TAG_START;
import static com.intellij.psi.xml.XmlTokenType.XML_TAG_END;
import static com.intellij.psi.xml.XmlTokenType.XML_TAG_NAME;

public class GrailsTag implements GspElementTypes {

  public static void parse(PsiBuilder builder) {
    PsiBuilder.Marker tagMarker = builder.mark();

    ParserUtils.getToken(builder, XML_START_TAG_START);
    if (XML_TAG_NAME != builder.getTokenType()) {
      builder.error(GroovyBundle.message("identifier.expected"));
      tagMarker.drop();
      return;
    }
    String tagName = builder.getTokenText();
    builder.advanceLexer();
    parseAttrList(builder);
    if (ParserUtils.getToken(builder, XML_EMPTY_ELEMENT_END)) {
      tagMarker.done(GRAILS_TAG);
    } else if (ParserUtils.getToken(builder, XML_TAG_END)) {
      parseBody(builder, tagName);
      parseEndTag(builder, tagName);
      tagMarker.done(GRAILS_TAG);
    } else {
      if (XML_END_TAG_START == builder.getTokenType()
          || XML_START_TAG_START == builder.getTokenType()
          || GSP_TEMPLATE_DATA == builder.getTokenType()) {
        tagMarker.done(GRAILS_TAG);
        return;
      }
      while (!(XML_EMPTY_ELEMENT_END == builder.getTokenType() ||
               XML_TAG_END == builder.getTokenType() ||
          builder.eof())) {
        builder.error(GrailsBundle.message("wrong.attributes"));
        builder.advanceLexer();
      }
      if (ParserUtils.getToken(builder, XML_EMPTY_ELEMENT_END)) {
        tagMarker.done(GRAILS_TAG);
      } else if (ParserUtils.getToken(builder, XML_TAG_END)) {
        parseBody(builder, tagName);
        parseEndTag(builder, tagName);
        tagMarker.done(GRAILS_TAG);
      } else {
        tagMarker.done(GRAILS_TAG);
      }
    }
  }

  private static void parseEndTag(PsiBuilder builder, String tagName) {
    if (ParserUtils.getToken(builder, XML_END_TAG_START)) {
      ParserUtils.getToken(builder, XML_TAG_NAME, GrailsBundle.message("closing.tag.brace.expected"));

      ParserUtils.getToken(builder, XML_TAG_END, GrailsBundle.message("closing.tag.brace.expected"));
    } else {
      builder.error(GrailsBundle.message("closing.grails.tag.expected", tagName));
    }
  }

  private static void processInvalidAttrElement(PsiBuilder builder) {
    while (!builder.eof()) {
      IElementType tt = builder.getTokenType();

      if (XML_EMPTY_ELEMENT_END == tt || XML_TAG_END == tt
          || tt == GSP_TEMPLATE_DATA || tt == XML_END_TAG_START || tt == XML_START_TAG_START
          || tt == XML_NAME) {
        break;
      }

      builder.advanceLexer();
    }
  }

  private static void parseAttrList(PsiBuilder builder) {
    while (true) {
      IElementType tt = builder.getTokenType();

      if (builder.eof() || tt == GSP_TEMPLATE_DATA || tt == XML_END_TAG_START || tt == XML_START_TAG_START) {
        builder.error(GrailsBundle.message("closing.tag.brace.expected1"));
        break;
      }

      if (XML_EMPTY_ELEMENT_END == tt || XML_TAG_END == tt) {
        break;
      }

      PsiBuilder.Marker attrMarker = builder.mark();

      if (XML_NAME != builder.getTokenType()) {
        builder.error(GroovyBundle.message("identifier.expected"));
        attrMarker.drop();
        processInvalidAttrElement(builder);
        continue;
      }
      builder.advanceLexer();

      if (XML_EQ != builder.getTokenType()) {
        builder.error(GrailsBundle.message("equal.expected"));
        attrMarker.done(GRAILS_TAG_ATTRIBUTE);
        processInvalidAttrElement(builder);
        continue;
      }
      builder.advanceLexer();

      if (XML_ATTRIBUTE_VALUE_START_DELIMITER != builder.getTokenType()) {
        builder.error(GrailsBundle.message("delim.val.expected"));
        attrMarker.done(GRAILS_TAG_ATTRIBUTE);
        processInvalidAttrElement(builder);
        continue;
      }

      PsiBuilder.Marker valMarker = builder.mark();
      builder.advanceLexer();

      while (!builder.eof() && XML_ATTRIBUTE_VALUE_END_DELIMITER != (tt = builder.getTokenType())) {
        builder.advanceLexer();

        if (tt == GSTRING_DOLLAR) {
          if (builder.getTokenType() != GSP_GROOVY_CODE) {
            builder.error(GrailsBundle.message("error.message.identifier.expected"));
          }
        }
      }

      if (XML_ATTRIBUTE_VALUE_END_DELIMITER == builder.getTokenType()) {
        builder.advanceLexer();
      }
      valMarker.done(GSP_ATTRIBUTE_VALUE);

      attrMarker.done(GRAILS_TAG_ATTRIBUTE);
    }
  }

  private static void parseBody(PsiBuilder builder, String tagName) {
    PsiBuilder.Marker first = null;
    while (true) {
      GspParser.parseVariousTagContent(builder, true);
      if (XML_END_TAG_START == builder.getTokenType()) {
        if (first == null) {
          first = builder.mark();
        }
        PsiBuilder.Marker rb = builder.mark();
        builder.advanceLexer();
        if (tagName.equals(builder.getTokenText())) {
          rb.rollbackTo();
          first.drop();
          first = null;
          break;
        } else {
          builder.error(GrailsBundle.message("clos.tag.in.wrong.place"));
          ParserUtils.getToken(builder, XML_TAG_NAME);
          ParserUtils.getToken(builder, XML_TAG_END);
          rb.drop();
        }
      }
      if (builder.eof()) {
        builder.error(GrailsBundle.message("closing.grails.tag.expected", tagName));
        break;
      }
    }
    if (first != null) {
      first.rollbackTo();
    }
  }

}
