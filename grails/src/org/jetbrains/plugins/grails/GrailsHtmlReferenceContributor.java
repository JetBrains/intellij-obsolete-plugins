// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails;

import com.intellij.patterns.XmlPatterns;
import com.intellij.psi.PsiReferenceContributor;
import com.intellij.psi.PsiReferenceRegistrar;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.grails.references.tagSupport.GspLayoutReferenceProvider;

import static com.intellij.patterns.XmlPatterns.xmlAttribute;
import static com.intellij.patterns.XmlPatterns.xmlAttributeValue;

public final class GrailsHtmlReferenceContributor extends PsiReferenceContributor {
  @Override
  public void registerReferenceProviders(@NotNull PsiReferenceRegistrar registrar) {
    registrar.registerReferenceProvider(
      xmlAttributeValue(
        xmlAttribute("content")
          .withParent(XmlPatterns.xmlTag().withName("meta").withAttributeValue("name", "layout"))),
      new GspLayoutReferenceProvider()
    );
  }
}
