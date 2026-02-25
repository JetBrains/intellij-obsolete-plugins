// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.references.common;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.vfs.CharsetToolkit;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceBase;
import com.intellij.psi.PsiReferenceProvider;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.Charset;

public class GrailsEncodingReferenceProvider extends PsiReferenceProvider {
  @Override
  public PsiReference @NotNull [] getReferencesByElement(@NotNull PsiElement element, @NotNull ProcessingContext context) {
    return new PsiReference[]{new PsiReferenceBase<>(element, false) {
      @Override
      public PsiElement resolve() {
        return null;
      }

      @Override
      public Object @NotNull [] getVariants() {
        Charset[] charsets = CharsetToolkit.getAvailableCharsets();

        LookupElement[] res = new LookupElement[charsets.length];
        for (int i = 0; i < charsets.length; i++) {
          res[i] = LookupElementBuilder.create(charsets[i].name()).withCaseSensitivity(false);
        }

        return res;
      }
    }};
  }
}
