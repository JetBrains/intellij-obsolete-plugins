// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.references.controller;

import com.intellij.psi.PsiMethod;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.grails.lang.gsp.resolve.taglib.TagLibNamespaceDescriptor;
import org.jetbrains.plugins.groovy.lang.psi.api.GroovyMethodResult;
import org.jetbrains.plugins.groovy.lang.resolve.GrMethodComparator;

final class GspTagMethodComparator extends GrMethodComparator {
  @Override
  public Boolean dominated(@NotNull GroovyMethodResult result1,
                           @NotNull GroovyMethodResult result2,
                           @NotNull Context context) {

    final PsiMethod method1 = result1.getElement();
    final PsiMethod method2 = result2.getElement();

    if (method1 instanceof TagLibNamespaceDescriptor.GspTagMethod) {
      if (!(method2 instanceof TagLibNamespaceDescriptor.GspTagMethod)) {
        return true;
      }
    }
    else {
      if (method2 instanceof TagLibNamespaceDescriptor.GspTagMethod) {
        return false;
      }
    }

    return null;
  }
}
