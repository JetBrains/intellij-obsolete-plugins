package com.intellij.lang.puppet.ide.completion;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementPresentation;
import com.intellij.codeInsight.lookup.LookupElementRenderer;
import com.intellij.openapi.util.Key;
import org.jetbrains.annotations.NotNull;

abstract class PuppetLookupCachingRenderer extends LookupElementRenderer<LookupElement> {
  private static final Key<LookupElementPresentation> LOOKUP_ELEMENT_PRESENTATION_KEY = new Key<>("puppet.lookup.presentation.cache");

  @Override
  public void renderElement(@NotNull LookupElement lookupElement, @NotNull LookupElementPresentation presentation) {
    LookupElementPresentation cachedPresentation = LOOKUP_ELEMENT_PRESENTATION_KEY.get(lookupElement);
    if (cachedPresentation == null) {
      calcPresentation(lookupElement, presentation);
      LOOKUP_ELEMENT_PRESENTATION_KEY.set(lookupElement, presentation);
    }
    else {
      presentation.copyFrom(cachedPresentation);
    }
  }

  protected abstract void calcPresentation(LookupElement element, LookupElementPresentation presentation);
}
