// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.spring.osgi.model.scope;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.spring.contexts.model.SpringModel;
import com.intellij.spring.model.scope.SpringBeanScope;
import com.intellij.spring.model.scope.SpringCustomBeanScope;
import com.intellij.spring.osgi.constants.SpringOsgiConstants;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Set;

public class SpringBundleCustomBeanScope extends SpringCustomBeanScope {

  @Override
  public String getScopeClassName() {
    return SpringOsgiConstants.OSGI_FRAMEWORK_BUNDLE_CONTEXT;
  }

  @Override
  public boolean process(List<SpringBeanScope> scopes,
                         Set<SpringModel> models,
                         @NotNull PsiClass scopeClass,
                         PsiElement psiElement) {
    scopes.add(new SpringBeanScope("bundle"));
    return true;
  }
}
