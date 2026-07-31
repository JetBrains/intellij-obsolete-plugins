/*
 * @author max
 */
package com.intellij.gwt.jsinject.parser;

import com.intellij.lang.PsiBuilder;
import com.intellij.lang.javascript.JSFlexAdapter;
import com.intellij.lang.javascript.JavascriptParserDefinition;
import com.intellij.lang.javascript.parsing.JavaScriptParser;
import com.intellij.lang.javascript.types.JSFileElementType;
import com.intellij.lexer.Lexer;
import com.intellij.openapi.project.Project;
import com.intellij.psi.tree.IFileElementType;
import org.jetbrains.annotations.NotNull;

public final class GwtParserDefinition extends JavascriptParserDefinition {
  static final JSFileElementType GWT_FILE = JSFileElementType.create(GwtLanguageDialect.GWT_DIALECT);

  @Override
  public @NotNull Lexer createLexer(final Project project) {
    return new JSFlexAdapter(GwtLanguageDialect.DIALECT_OPTION_HOLDER);
  }

  @Override
  public @NotNull IFileElementType getFileNodeType() {
    return GWT_FILE;
  }

  @Override
  public @NotNull JavaScriptParser createJSParser(@NotNull PsiBuilder builder) {
    return new GwtParser(builder);
  }
}
