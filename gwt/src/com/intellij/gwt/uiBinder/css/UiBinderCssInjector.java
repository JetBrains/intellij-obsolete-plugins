package com.intellij.gwt.uiBinder.css;

import com.intellij.gwt.clientBundle.css.language.GwtCssLanguage;
import com.intellij.gwt.uiBinder.UiBinderUtil;
import com.intellij.lang.injection.MultiHostInjector;
import com.intellij.lang.injection.MultiHostRegistrar;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiLanguageInjectionHost;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import com.intellij.psi.xml.XmlText;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public final class UiBinderCssInjector implements MultiHostInjector {
  @Override
  public void getLanguagesToInject(@NotNull MultiHostRegistrar registrar, @NotNull PsiElement context) {
    if (!(context instanceof XmlText)) return;

    XmlTag tag = ((XmlText)context).getParentTag();
    if (tag == null) return;

    final PsiFile file = tag.getContainingFile();
    if (!(file instanceof XmlFile) || !UiBinderUtil.isUiXmlFile((XmlFile)file)) return;

    if (!UiBinderUtil.UI_STYLE_TAG.equals(tag.getLocalName()) || !UiBinderUtil.UI_BINDER_NAMESPACE.equals(tag.getNamespace())) {
      return;
    }

    registrar.startInjecting(GwtCssLanguage.GWT_CSS_LANGUAGE)
             .addPlace(null, null, (PsiLanguageInjectionHost)context, new TextRange(0, context.getTextLength()))
             .doneInjecting();
  }

  @Override
  public @NotNull List<? extends Class<? extends PsiElement>> elementsToInjectIn() {
    return Collections.singletonList(XmlText.class);
  }
}
