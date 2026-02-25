// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.references.pluginClass;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceProvider;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.grails.references.common.GrailsRootBasedFileReferenceSet;
import org.jetbrains.plugins.grails.util.GrailsUtils;

/**
 * @author user
 */
public class GrailsPluginExcludeReferenceProvider extends PsiReferenceProvider {
  @Override
  public PsiReference @NotNull [] getReferencesByElement(@NotNull PsiElement element, @NotNull ProcessingContext context) {
    PsiField aField = (PsiField)element.getParent().getParent();
    PsiClass aClass = aField.getContainingClass();

    if (!GrailsUtils.isGrailsPluginClass(aClass)) return PsiReference.EMPTY_ARRAY;

    return GrailsRootBasedFileReferenceSet.createReferences(element);
  }
}
