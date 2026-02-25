// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.references.common;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReferenceBase;
import com.intellij.xml.util.documentation.MimeTypeDictionary;
import org.jetbrains.annotations.NotNull;

public class ContentTypeReference extends PsiReferenceBase<PsiElement> {
  public ContentTypeReference(PsiElement element, TextRange range, boolean soft) {
    super(element, range, soft);
  }

  public ContentTypeReference(PsiElement element, TextRange range) {
    super(element, range);
  }

  public ContentTypeReference(PsiElement element, boolean soft) {
    super(element, soft);
  }

  public ContentTypeReference(@NotNull PsiElement element) {
    super(element);
  }

  @Override
  public PsiElement resolve() {
    return null;
  }

  @Override
  public Object @NotNull [] getVariants() {
    return MimeTypeDictionary.HTML_CONTENT_TYPES;
  }
}
