// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.references.util;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiReferenceBase;
import com.intellij.psi.PsiTypes;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.typedef.members.GrAccessorMethod;
import org.jetbrains.plugins.groovy.lang.psi.util.GroovyPropertyUtils;

public abstract class PsiFieldReference extends PsiReferenceBase<PsiElement> {

  public PsiFieldReference(PsiElement element, boolean soft) {
    super(element, soft);
  }

  @Override
  public PsiElement handleElementRename(@NotNull String newElementName) throws IncorrectOperationException {

    PsiElement resolve = resolve();
    if (resolve instanceof PsiMethod && !(resolve instanceof GrAccessorMethod)) {
      String s = GroovyPropertyUtils.getPropertyNameByGetterName(newElementName, PsiTypes.booleanType().equals(
        ((PsiMethod)resolve).getReturnType()));
      if (s == null) return getElement();
      newElementName = s;
    }

    PsiElement res = super.handleElementRename(newElementName);
    setRangeInElement(null);
    return res;
  }

}
