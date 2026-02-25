// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.lang.gsp.psi.gsp.impl.gtag.reference.contributor;

import com.intellij.patterns.XmlPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceContributor;
import com.intellij.psi.PsiReferenceProvider;
import com.intellij.psi.PsiReferenceRegistrar;
import com.intellij.psi.css.CssElementDescriptorProvider;
import com.intellij.psi.css.impl.util.table.CssDescriptorsUtilCore;
import com.intellij.psi.impl.PsiImplUtil;
import com.intellij.psi.xml.XmlAttributeValue;
import com.intellij.psi.xml.XmlTokenType;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.grails.util.GrailsPatterns;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class GspCssClassReferenceContributor extends PsiReferenceContributor {
  private static class Holder {
    private static final Pattern CLASS_NAME_PATTERN = Pattern.compile("\\S+");
  }

  @Override
  public void registerReferenceProviders(@NotNull PsiReferenceRegistrar registrar) {
    registrar.registerReferenceProvider(
      GrailsPatterns.gspAttributeValue(XmlPatterns.xmlAttribute("class").withParent(XmlPatterns.xmlTag().withNamespace("g"))),
      new PsiReferenceProvider() {
        @Override
        public PsiReference @NotNull [] getReferencesByElement(@NotNull PsiElement element, @NotNull ProcessingContext context) {
          // Create css reference like CssInHtmlClassOrIdReferenceProvider

          if (!(element instanceof XmlAttributeValue)) return PsiReference.EMPTY_ARRAY;

          List<PsiReference> res = new ArrayList<>();
          CssElementDescriptorProvider descriptorProvider = CssDescriptorsUtilCore.findDescriptorProvider(element);
          if (descriptorProvider == null) return PsiReference.EMPTY_ARRAY;

          for (PsiElement child = element.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (!PsiImplUtil.isLeafElementOfType(child, XmlTokenType.XML_ATTRIBUTE_VALUE_TOKEN)) continue;

            Matcher matcher = Holder.CLASS_NAME_PATTERN.matcher(child.getText());
            while (matcher.find()) {
              int offsetChild = child.getStartOffsetInParent();
              res.add(descriptorProvider.getStyleReference(element, offsetChild + matcher.start(), offsetChild + matcher.end(), false));
            }
          }

          return res.toArray(PsiReference.EMPTY_ARRAY);
        }
      }
    );
  }
}
