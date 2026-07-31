package com.intellij.gwt.clientBundle.css.language;

import com.intellij.gwt.GwtBundle;
import com.intellij.lang.PsiBuilder;
import com.intellij.openapi.util.NlsSafe;
import com.intellij.psi.css.impl.CssElementTypes;
import com.intellij.psi.css.impl.parsing.CssParser2;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

public class GwtCssParser extends CssParser2 {

  @Override
  protected IElementType getStylesheetLazyElementType() {
    return GwtCssElementTypes.GWT_CSS_LAZY_STYLESHEET;
  }

  @Override
  protected IElementType getStylesheetElementType() {
    return GwtCssStubElementTypes.GWT_CSS_STYLESHEET;
  }

  @Override
  protected boolean parseAtRule() {
    final CharSequence ruleName = getAtName();
    if (ruleName.equals("def")) {
      parseDef();
    }
    else if (ruleName.equals("if")) {
      parseIf();
    }
    else if (ruleName.equals("eval")) {
      parseEval();
    }
    else if (ruleName.equals("external")) {
      parseExternal();
    }
    else if (ruleName.equals("noflip")) {
      parseNoflip();
    }
    else if (ruleName.equals("sprite")) {
      parseSprite();
    }
    else if (ruleName.equals("url")) {
      parseUrl();
    }
    else {
      return false;
    }
    return true;
  }

  private void parseDef() {
    final PsiBuilder.Marker defElement = createNamedElement();
    parseTermList(false);
    addSemicolonOrError();
    defElement.done(GwtCssElementTypes.CSS_DEF);
  }

  private void parseUrl() {
    final PsiBuilder.Marker url = createNamedElement();
    addTokensUntilSemicolon();
    url.done(GwtCssElementTypes.CSS_URL_DECLARATION);
  }

  private void parseEval() {
    final PsiBuilder.Marker eval = createNamedElement();
    addTokensUntilSemicolon();
    eval.done(GwtCssElementTypes.CSS_EVAL);
  }

  private void parseExternal() {
    final PsiBuilder.Marker external = createCompositeElement();
    addToken();
    addTokensUntilSemicolon();
    external.done(GwtCssElementTypes.CSS_EXTERNAL);
  }

  private void parseIf() {
    final PsiBuilder.Marker ifStatement = createCompositeElement();
    addToken();
    parseCondition();
    parseBody();

    while (isAtToken("elif")) {
      addToken();
      parseCondition();
      parseBody();
    }

    if (isAtToken("else")) {
      addToken();
      parseBody();
    }
    ifStatement.done(GwtCssElementTypes.CSS_IF_STATEMENT);
  }

  private void parseBody() {
    addTokenOrError(CssElementTypes.CSS_LBRACE, "'{'");
    parseRulesetList(true);
    addTokenOrError(CssElementTypes.CSS_RBRACE, "'}'");
  }

  private void parseCondition() {
    while (!isDone() && getTokenType() != CssElementTypes.CSS_LBRACE) {
      addToken();
    }
  }

  private void parseSprite() {
    final PsiBuilder.Marker sprite = createCompositeElement();
    addToken();
    if (!parseRuleset()) {
      createErrorElement(GwtBundle.message("parsing.error.rule.expected"));
    }
    sprite.done(GwtCssElementTypes.CSS_SPRITE);
  }

  private void parseNoflip() {
    final PsiBuilder.Marker noFlip = createCompositeElement();
    addToken();
    parseBody();
    noFlip.done(GwtCssElementTypes.CSS_NO_FLIP);
  }

  private PsiBuilder.Marker createNamedElement() {
    final PsiBuilder.Marker defElement = createCompositeElement();
    addToken();
    addIdentOrError();
    return defElement;
  }

  private void addTokensUntilSemicolon() {
    while (!isDone() && getTokenType() != CssElementTypes.CSS_SEMICOLON) {
      addToken();
    }
    addToken();
  }

  private @NlsSafe String getAtName() {
    String tokenText = getTokenText();
    return tokenText != null && !tokenText.isEmpty() ? tokenText.substring(1) : "";
  }

  private boolean isAtToken(@NotNull @NonNls String token) {
    if (getTokenType() != CssElementTypes.CSS_ATKEYWORD) {
      return false;
    }
    return token.equals(getAtName());
  }
}
