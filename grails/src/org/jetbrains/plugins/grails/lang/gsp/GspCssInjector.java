// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.lang.gsp;

import com.intellij.lang.injection.MultiHostInjector;
import com.intellij.lang.injection.MultiHostRegistrar;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiLanguageInjectionHost;
import com.intellij.psi.css.CssFileType;
import com.intellij.psi.xml.XmlAttributeValue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.grails.lang.gsp.psi.gsp.api.GspTag;
import org.jetbrains.plugins.grails.lang.gsp.psi.gsp.api.gtag.GspAttribute;
import org.jetbrains.plugins.grails.util.GrailsPsiUtil;

import java.util.Collections;
import java.util.List;

final class GspCssInjector implements MultiHostInjector {
  @Override
  public void getLanguagesToInject(@NotNull MultiHostRegistrar registrar, @NotNull PsiElement context) {
    GspAttribute attribute = (GspAttribute)context;
    String attrName = attribute.getName();
    if (!"style".equals(attrName)) return;

    PsiElement tag = attribute.getParent();
    if (!(tag instanceof GspTag)) return;
    if ("g:formatDate".equals(((GspTag)tag).getName())) return;

    XmlAttributeValue value = attribute.getValueElement();
    if (value == null || !GrailsPsiUtil.isSimpleAttribute(value)) return;

    int length = value.getTextLength();
    if (length < 2) return;

    registrar.startInjecting(CssFileType.INSTANCE.getLanguage())
        .addPlace("inline.style {", "}", (PsiLanguageInjectionHost)value, new TextRange(1, length - 1))
        .doneInjecting();
  }

  @Override
  public @NotNull List<? extends Class<? extends PsiElement>> elementsToInjectIn() {
    return Collections.singletonList(GspAttribute.class);
  }
}
