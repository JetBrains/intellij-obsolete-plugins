package com.intellij.play.language;

import com.intellij.lang.ASTNode;
import com.intellij.lang.PsiBuilder;
import com.intellij.lang.PsiParser;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.play.utils.PlayBundle;
import com.intellij.psi.tree.IElementType;
import com.intellij.util.containers.Stack;
import org.jetbrains.annotations.NotNull;

public class PlayParser implements PsiParser {

  @NotNull
  @Override
  public ASTNode parse(IElementType root, PsiBuilder builder) {
    final PsiBuilder.Marker marker = builder.mark();
    while (!builder.eof()) {
      if (builder.getTokenType() == PlayElementTypes.TAG_START) {
        parseTag(builder);
      }
      else if (builder.getTokenType() == PlayElementTypes.END_TAG_START) {
        builder.advanceLexer();
        if (builder.getTokenType() == PlayElementTypes.TAG_NAME) {
          builder.error(PlayBundle.message("play.parser.start.tag.required", builder.getTokenText()));
        }
        else {
          builder.error(PlayBundle.message("play.parser.tag.name.expected"));
        }
      }
      builder.advanceLexer();
    }

    marker.done(root);

    return builder.getTreeBuilt();
  }

  private void parseTag(PsiBuilder builder) {
    parseTag(builder, new Stack<>());
  }

  private void parseTag(PsiBuilder builder, @NotNull Stack<String> tagNames) {
    PsiBuilder.Marker tagMarker = builder.mark();

    if (!parseTagName(builder, tagNames)) {
      tagMarker.drop();
      return;
    }

    while (builder.getTokenType() != PlayElementTypes.TAG_END
           && builder.getTokenType() != PlayElementTypes.CLOSE_TAG
           && !builder.eof()) {

      if (builder.getTokenType() == PlayElementTypes.ATTR_NAME) {
        parseNameValuePair(builder);
        continue;
      }
      else if (builder.getTokenType() == PlayElementTypes.TAG_START) {
        builder.error(PlayBundle.message("play.parser.expected.tag.closing.element", tagNames.peek()));
        tagMarker.drop();
        parseTag(builder);
        return;
      }
      builder.advanceLexer();
    }

    if (builder.getTokenType() == PlayElementTypes.CLOSE_TAG) {
      doneTage(builder, tagNames, tagMarker); // #{name ... /}
    }
    else if (builder.getTokenType() == PlayElementTypes.TAG_END) {
      parseTagContent(builder, tagNames, tagMarker);
    }
    else {
      builder.error(PlayBundle.message("play.parser.expected.tag.closing.element", tagNames.peek()));
      tagMarker.drop();
    }
  }

  private void parseTagContent(PsiBuilder builder, Stack<String> tagNames, PsiBuilder.Marker tagMarker) {
    builder.advanceLexer(); // #{if ... }

    // find close tag and parse content
    while (!builder.eof()) {
      if (builder.getTokenType() == PlayElementTypes.TAG_START) {
        parseTag(builder, tagNames);  // parse sub tag
        continue;
      }
      else if (builder.getTokenType() == PlayElementTypes.END_TAG_START) {
        builder.advanceLexer();
        parseClosingTag(builder, tagNames, tagMarker);
        return;
      }
      else if (builder.getTokenType() == PlayElementTypes.TAG_END || builder.getTokenType() == PlayElementTypes.CLOSE_TAG) {
        builder.error(PlayBundle.message("play.parser.unexpected.token"));
        tagMarker.drop();
        return;
      }
      builder.advanceLexer();
    }
    builder.error(PlayBundle.message("play.parser.expected.closing.tag", tagNames.peek()));
    tagMarker.drop();
  }

  private boolean parseClosingTag(PsiBuilder builder, Stack<String> tagNames, PsiBuilder.Marker tagMarker) {
    final String requiredTagName = tagNames.peek();
    if (builder.getTokenType() == PlayElementTypes.TAG_NAME) {
      if (requiredTagName.equals(builder.getTokenText())) {
        builder.advanceLexer();
        while ((builder.getTokenType() != PlayElementTypes.TAG_END && !builder.eof())) {
          if (builder.getTokenType() != PlayElementTypes.WHITE_SPACE) {
            builder.error(PlayBundle.message("play.parser.expected.tag.closing.element", requiredTagName));
            tagMarker.drop();
            return false;
          }
          builder.advanceLexer();
        }
        if (builder.getTokenType() == PlayElementTypes.TAG_END) {
          doneTage(builder, tagNames, tagMarker);
          return true;
        }
      }
    } else if (builder.getTokenType() == PlayElementTypes.TAG_END) { // empty closing tag  #{/}   IDEA-80543
      doneTage(builder, tagNames, tagMarker);
      return true;
    }
    else {
      builder.error(PlayBundle.message("play.parser.closing.tag.name.expected", requiredTagName));
      tagMarker.drop();
      return false;
    }
    builder.error(PlayBundle.message("play.parser.expected.tag.closing.element", requiredTagName));
    tagMarker.drop();
    return false;
  }

  private boolean parseTagName(PsiBuilder builder, Stack<String> tagNames) {
    builder.advanceLexer();

    if (builder.getTokenType() == PlayElementTypes.TAG_NAME && !StringUtil.isEmptyOrSpaces(builder.getTokenText())) {
      tagNames.push(builder.getTokenText());
      builder.advanceLexer();
      return true;
    }
    else {
      builder.error(PlayBundle.message("play.parser.tag.name.expected"));
    }
    return false;
  }

  private void doneTage(PsiBuilder builder, Stack<String> tagNames, PsiBuilder.Marker tagMarker) {
    if (!tagNames.empty()) {
      builder.advanceLexer();
      tagMarker.done(PlayElementTypes.TAG);
      tagNames.pop();
    }
    else {
      tagMarker.drop();
    }
  }

  private void parseNameValuePair(PsiBuilder builder) {
    PsiBuilder.Marker nameValueMarker = builder.mark();
    builder.advanceLexer();
    if (builder.getTokenType() == PlayElementTypes.COLON) {
      builder.advanceLexer();
    }
    else {
      builder.error(PlayBundle.message("play.parser.colon.required"));
      dropNameValuePair(builder, nameValueMarker);
      return;
    }

    if (builder.getTokenType() == PlayElementTypes.TAG_EXPRESSION) {
      doneNameValuePair(builder, nameValueMarker);
    }
    else if (builder.getTokenType() == PlayElementTypes.AT) {
      builder.advanceLexer();
      if (builder.getTokenType() == PlayElementTypes.ACTION_SCRIPT) {
        doneNameValuePair(builder, nameValueMarker);
      }
      else {
        dropNameValuePair(builder, nameValueMarker);
      }
    }
    else {
      dropNameValuePair(builder, nameValueMarker);
    }
  }

  private void dropNameValuePair(PsiBuilder builder, PsiBuilder.Marker nameValueMarker) {
    builder.error(PlayBundle.message("play.parser.groovy.expression.required"));
    nameValueMarker.drop();
  }

  private void doneNameValuePair(PsiBuilder builder, PsiBuilder.Marker nameValueMarker) {
    builder.advanceLexer();
    nameValueMarker.done(PlayElementTypes.NAME_VALUE_PAIR);
  }
}
