package com.intellij.lang.puppet.psi.regexp;

import com.intellij.lang.puppet.PuppetTokenTypes;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.InjectedLanguagePlaces;
import com.intellij.psi.LanguageInjector;
import com.intellij.psi.PsiLanguageInjectionHost;
import org.intellij.lang.regexp.RegExpLanguage;
import org.jetbrains.annotations.NotNull;

public class PuppetRegexpLanguageInjector implements LanguageInjector {
  @Override
  public void getLanguagesToInject(@NotNull PsiLanguageInjectionHost host, @NotNull InjectedLanguagePlaces injectionPlacesRegistrar) {
    if (host.getNode().getElementType() != PuppetTokenTypes.REGEXP) {
      return;
    }

    injectionPlacesRegistrar.addPlace(RegExpLanguage.INSTANCE, TextRange.create(1, host.getTextLength() - 1), null, null);
  }
}
