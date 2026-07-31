package com.intellij.gwt.module.index;

import com.intellij.lexer.HtmlLexer;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.patterns.ElementPattern;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.xml.XmlAttributeValue;
import com.intellij.psi.xml.XmlTokenType;
import com.intellij.xml.util.HtmlUtil;
import org.jetbrains.annotations.NonNls;

import java.util.HashMap;
import java.util.Map;

import static com.intellij.patterns.XmlPatterns.xmlAttribute;
import static com.intellij.patterns.XmlPatterns.xmlAttributeValue;
import static com.intellij.patterns.XmlPatterns.xmlTag;

public final class GwtHtmlUtil {
  private static final @NonNls String META_TAG_NAME = "meta";
  private static final @NonNls String GWT_MODULE_META_NAME = "gwt:module";
  private static final @NonNls String JAVASCRIPT_LANGUAGE_NAME = "javascript";
  private static final @NonNls String JAVASCRIPT_TYPE = "text/javascript";
  private static final @NonNls String NO_CACHE_SUFFIX = ".nocache.js";
  private static final @NonNls String NAME_ATTRIBUTE = "name";
  private static final @NonNls String CONTENT_ATTRIBUTE = "content";

  private GwtHtmlUtil() {
  }

  public static void collectGwtModules(CharSequence fileText, Map<String, Void> result) {
    HtmlLexer lexer = new HtmlLexer();
    lexer.start(fileText);
    IElementType tokenType;
    String currentTag = null;
    @NonNls Map<String, String> attributes = new HashMap<>();
    while ((tokenType = lexer.getTokenType()) != null) {
      //System.out.println(tokenType + ":" + lexer.getTokenStart() + "-" + lexer.getTokenEnd() + "='" + getTokenText(lexer) + "'");
      if (tokenType == XmlTokenType.XML_START_TAG_START) {
        currentTag = null;
        lexer.advance();
        if (lexer.getTokenType() == XmlTokenType.XML_NAME) {
          currentTag = StringUtil.toLowerCase(getTokenText(lexer));
          lexer.advance();
        }
      }
      else if (tokenType == XmlTokenType.XML_NAME) {
        String attributeName = getTokenText(lexer);
        skipWhiteSpaces(lexer);
        if (lexer.getTokenType() == XmlTokenType.XML_EQ) {
          skipWhiteSpaces(lexer);
          if (lexer.getTokenType() == XmlTokenType.XML_ATTRIBUTE_VALUE_START_DELIMITER) {
            lexer.advance();
            if (lexer.getTokenType() == XmlTokenType.XML_ATTRIBUTE_VALUE_TOKEN) {
              String attributeValue = getTokenText(lexer);
              lexer.advance();
              if (lexer.getTokenType() == XmlTokenType.XML_ATTRIBUTE_VALUE_END_DELIMITER) {
                attributes.put(attributeName, attributeValue);
                lexer.advance();
              }
            }
          }
        }
      }
      else if (tokenType == XmlTokenType.XML_TAG_END) {
        if (META_TAG_NAME.equals(currentTag) && GWT_MODULE_META_NAME.equals(attributes.get(NAME_ATTRIBUTE))) {
          final String content = attributes.get(CONTENT_ATTRIBUTE);
          if (content != null) {
            result.put(content, null);
          }
        }
        else if (HtmlUtil.SCRIPT_TAG_NAME.equals(currentTag) && (JAVASCRIPT_LANGUAGE_NAME.equalsIgnoreCase(attributes.get("language")) ||
                  JAVASCRIPT_TYPE.equalsIgnoreCase(attributes.get("type")) ||
                  (null == attributes.get("language") && null == attributes.get("type")))) {
          String src = attributes.get("src");
          if (src != null && src.endsWith(NO_CACHE_SUFFIX)) {
            int start = Math.max(src.lastIndexOf('/'), src.lastIndexOf('\\'));
            result.put(src.substring(start + 1, src.length() - NO_CACHE_SUFFIX.length()), null);
          }
        }
        currentTag = null;
        attributes.clear();
        lexer.advance();
      }
      else {
        lexer.advance();
      }
    }
  }

  private static String getTokenText(HtmlLexer lexer) {
    return lexer.getBufferSequence().subSequence(lexer.getTokenStart(), lexer.getTokenEnd()).toString();
  }

  private static void skipWhiteSpaces(HtmlLexer lexer) {
    lexer.advance();
    while (lexer.getTokenType() == XmlTokenType.XML_WHITE_SPACE) {
      lexer.advance();
    }
  }

  public static ElementPattern<? extends XmlAttributeValue> createMetaValuePattern() {
    return xmlAttributeValue(xmlAttribute(CONTENT_ATTRIBUTE)
        .withParent(xmlTag().withLocalName(META_TAG_NAME).withAttributeValue(NAME_ATTRIBUTE, GWT_MODULE_META_NAME))
    );
  }
}
