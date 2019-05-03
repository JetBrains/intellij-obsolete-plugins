/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.intellij.struts.inplace.reference.config;

import com.intellij.javaee.web.ServletPathReferenceProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceProvider;
import com.intellij.struts.StrutsManager;
import com.intellij.struts.StrutsModel;
import com.intellij.struts.inplace.reference.path.ActionWebPathsProvider;
import com.intellij.util.ArrayUtil;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;

/**
 * Created by IntelliJ IDEA.
 * User: DAvdeev
 *
 * see http://struts.apache.org/1.2.9/userGuide/struts-html.html#form
 */
public class ActionReferenceProvider extends PsiReferenceProvider {

  private final static ActionWebPathsProvider PROVIDER = new ActionWebPathsProvider(false);
  @Override
  @NotNull
  public PsiReference[] getReferencesByElement(@NotNull final PsiElement element, @NotNull final ProcessingContext context) {
    final StrutsModel model = StrutsManager.getInstance().getStrutsModel(element);

    if (model != null) {
      final PsiReference[] references = PROVIDER.createReferences(element, model.getServletMappingInfo(), false);
      final PsiReference mappingReference = ServletPathReferenceProvider.createMappingReference(element);
      return mappingReference == null ? references : ArrayUtil.append(references, mappingReference, PsiReference.class);
    } else {
      return PsiReference.EMPTY_ARRAY;
    }
  }
}
