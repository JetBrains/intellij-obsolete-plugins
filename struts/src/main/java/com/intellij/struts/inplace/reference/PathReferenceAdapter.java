/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.intellij.struts.inplace.reference;

import com.intellij.openapi.paths.PathReferenceProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

/**
 * @author Dmitry Avdeev
 */
public class PathReferenceAdapter extends BaseReferenceProvider {

  private final PathReferenceProvider myConverter;
  private final boolean mySoft;

  public PathReferenceAdapter(PathReferenceProvider provider, boolean soft) {
    myConverter = provider;
    mySoft = soft;
  }

  @Override
  @NotNull
  public PsiReference[] getReferencesByElement(@NotNull PsiElement element, @NotNull final ProcessingContext context) {
    final ArrayList<PsiReference> list = new ArrayList<>();
    myConverter.createReferences(element, list, mySoft);
    return list.toArray(PsiReference.EMPTY_ARRAY);
  }
}
