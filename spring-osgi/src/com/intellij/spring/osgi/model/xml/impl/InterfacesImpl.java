// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.spring.osgi.model.xml.impl;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiType;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.spring.osgi.model.xml.Interfaces;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public abstract class InterfacesImpl implements Interfaces {
  @Override
  public @NotNull List<PsiType> getRequiredTypes() {
    final Project project = getManager().getProject();
    final PsiClassType clazzType = PsiType.getJavaLangClass(PsiManager.getInstance(project), GlobalSearchScope.allScope(project));
    return Collections.singletonList(clazzType);
  }
}
