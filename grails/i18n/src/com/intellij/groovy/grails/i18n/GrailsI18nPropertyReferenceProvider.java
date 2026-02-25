// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package com.intellij.groovy.grails.i18n;

import com.intellij.lang.properties.psi.PropertiesFile;
import com.intellij.lang.properties.references.PropertyReference;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.patterns.ElementPattern;
import com.intellij.patterns.InitialPatternCondition;
import com.intellij.patterns.PsiElementPattern;
import com.intellij.patterns.XmlAttributeValuePattern;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceProvider;
import com.intellij.psi.xml.XmlAttributeValue;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.grails.lang.gsp.psi.gsp.api.gtag.GspAttribute;
import org.jetbrains.plugins.grails.references.common.GspAttributeWrapper;
import org.jetbrains.plugins.grails.references.common.GspTagWrapper;
import org.jetbrains.plugins.grails.references.common.XmlGspAttributeWrapper;
import org.jetbrains.plugins.grails.util.GrailsPatterns;
import org.jetbrains.plugins.grails.util.GrailsPsiUtil;
import org.jetbrains.plugins.grails.util.GrailsUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

final class GrailsI18nPropertyReferenceProvider extends PsiReferenceProvider {

  public static final ElementPattern<GspAttribute> ATTRIBUTE_PATTERN =
    new PsiElementPattern.Capture<GspAttribute>(new InitialPatternCondition<GspAttribute>(GspAttribute.class) {
      @Override
      public boolean accepts(@Nullable Object o, ProcessingContext context) {
        if (!(o instanceof GspAttribute attr)) return false;

        return getTypeOfReference(new XmlGspAttributeWrapper(attr)) != null;
      }
    }) {
    };

  public static final XmlAttributeValuePattern ATTRIBUTE_VALUE_PATTERN = GrailsPatterns.gspAttributeValue(ATTRIBUTE_PATTERN);

  /**
   * @return Returns Boolean.TRUE if soft reference, Boolean.FALSE if non-soft reference, null if reference should not be injected.
   */
  static @Nullable Boolean getTypeOfReference(GspAttributeWrapper attr) {
    String attributeName = attr.getName();

    if ("code".equals(attributeName)) {
      GspTagWrapper tag = attr.getTag();
      String tagName = tag.getTagName();
      if ("g:message".equals(tagName)) {
        return tag.hasAttribute("default");
      }
      if ("tooltip:tip".equals(tagName)) {
        return true;
      }
    }
    else if ("titleKey".equals(attributeName)) {
      GspTagWrapper tag = attr.getTag();
      if ("g:sortableColumn".equals(tag.getTagName())) {
        return tag.hasAttribute("title");
      }
    }

    return null;
  }

  static @Nullable PropertyReference getReferenceByElement(@NotNull XmlAttributeValue value) {
    if (!GrailsPsiUtil.isSimpleAttribute(value)) return null;

    return new PropertyReference(value.getValue(), value, "messages", true) {
      @Override
      protected List<PropertiesFile> retrievePropertyFilesByBundleName(String bundleName, PsiElement element) {
        VirtualFile i18nFile = GrailsUtils.findI18nDirectory(element);
        if (i18nFile == null) return super.retrievePropertyFilesByBundleName(bundleName, element);

        PsiDirectory i18nPsiDir = getElement().getManager().findDirectory(i18nFile);
        if (i18nPsiDir == null) return super.retrievePropertyFilesByBundleName(bundleName, element);

        List<PropertiesFile> res = new ArrayList<>();

        for (PsiFile file : i18nPsiDir.getFiles()) {
          if (file instanceof PropertiesFile) {
            res.add((PropertiesFile)file);
          }
        }

        if (res.isEmpty()) return super.retrievePropertyFilesByBundleName(bundleName, element);

        res.sort(Comparator.comparing(PropertiesFile::getName));

        return res;
      }
    };
  }

  @Override
  public PsiReference @NotNull [] getReferencesByElement(@NotNull PsiElement element,
                                                         @NotNull ProcessingContext context) {
    PsiReference reference = getReferenceByElement((XmlAttributeValue)element);
    if (reference == null) return PsiReference.EMPTY_ARRAY;

    return new PsiReference[]{reference};
  }
}
