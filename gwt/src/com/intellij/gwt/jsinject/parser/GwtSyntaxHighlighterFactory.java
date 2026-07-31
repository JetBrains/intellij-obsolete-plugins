package com.intellij.gwt.jsinject.parser;

import com.intellij.lang.javascript.JSTokenTypes;
import com.intellij.lang.javascript.highlighting.JSHighlighter;
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.fileTypes.SingleLazyInstanceSyntaxHighlighterFactory;
import com.intellij.openapi.fileTypes.SyntaxHighlighter;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public final class GwtSyntaxHighlighterFactory extends SingleLazyInstanceSyntaxHighlighterFactory {
  @Override
  protected @NotNull SyntaxHighlighter createHighlighter() {
    return new GwtSyntaxHighlighter();
  }

  private static class GwtSyntaxHighlighter extends JSHighlighter {
    private final Map<IElementType, TextAttributesKey> myKeysMap = new HashMap<>();

    GwtSyntaxHighlighter() {
      super(GwtLanguageDialect.DIALECT_OPTION_HOLDER);
      myKeysMap.put(JSTokenTypes.COLON_COLON, JS_OPERATION_SIGN);
      myKeysMap.put(JSTokenTypes.GWT_FIELD_OR_METHOD, DefaultLanguageHighlighterColors.FUNCTION_CALL);
      myKeysMap.put(JSTokenTypes.AT, JS_OPERATION_SIGN);
      myKeysMap.put(JSTokenTypes.IDENTIFIER, DefaultLanguageHighlighterColors.CLASS_NAME);
    }

    @Override
    public TextAttributesKey @NotNull [] getTokenHighlights(final IElementType tokenType) {
      if (myKeysMap.containsKey(tokenType)) {
        return pack(myKeysMap.get(tokenType));
      }
      return super.getTokenHighlights(tokenType);
    }
  }
}
