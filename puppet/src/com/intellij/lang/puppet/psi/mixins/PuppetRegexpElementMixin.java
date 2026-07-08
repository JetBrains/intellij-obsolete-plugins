package com.intellij.lang.puppet.psi.mixins;

import com.intellij.lang.ASTNode;
import com.intellij.lang.puppet.psi.PsiPuppetExpression;
import com.intellij.lang.puppet.psi.impl.PuppetCompositePsiElementBase;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.ElementManipulators;
import com.intellij.psi.LiteralTextEscaper;
import com.intellij.psi.PsiLanguageInjectionHost;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class PuppetRegexpElementMixin extends PuppetCompositePsiElementBase implements PsiLanguageInjectionHost {
  public PuppetRegexpElementMixin(@NotNull ASTNode node) {
    super(node);
  }

  @Override
  public boolean isValidHost() {
    return true;
  }

  @Override
  public PsiLanguageInjectionHost updateText(@NotNull String text) {
    return ElementManipulators.handleContentChange(this, text);
  }

  @Override
  public @NotNull LiteralTextEscaper<? extends PsiLanguageInjectionHost> createLiteralTextEscaper() {
    return new RegexpLiteralTextEscaper(this);
  }

  private static class RegexpLiteralTextEscaper extends LiteralTextEscaper<PuppetRegexpElementMixin> {
    protected RegexpLiteralTextEscaper(@NotNull PuppetRegexpElementMixin host) {
      super(host);
    }

    @Override
    public boolean decode(@NotNull TextRange rangeInsideHost, @NotNull StringBuilder outChars) {
      String escapedRegex = rangeInsideHost.substring(myHost.getText());
      outChars.append(escapedRegex.replaceAll("\\\\/", "/"));
      return true;
    }

    @Override
    public int getOffsetInHost(final int offsetInDecoded, final @NotNull TextRange rangeInsideHost) {
      int offset = offsetInDecoded + rangeInsideHost.getStartOffset();
      if (offset < rangeInsideHost.getStartOffset()) offset = rangeInsideHost.getStartOffset();
      if (offset > rangeInsideHost.getEndOffset()) offset = rangeInsideHost.getEndOffset();

      final String text = rangeInsideHost.substring(myHost.getText());
      for (int i = text.indexOf("\\/"), len = text.length(); i < len && i < offset && i >= 0; i = text.indexOf("\\/", i + 1)) {
        offset++;
      }

      return offset;
    }

    @Override
    public boolean isOneLine() {
      return true;
    }
  }

  @Nullable
  PsiPuppetExpression getExpression() {
    return null;
  }

}
