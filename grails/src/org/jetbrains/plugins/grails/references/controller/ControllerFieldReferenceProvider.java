// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.references.controller;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceProvider;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.grails.util.GrailsArtifact;
import org.jetbrains.plugins.groovy.lang.psi.util.PsiUtil;

public class ControllerFieldReferenceProvider extends PsiReferenceProvider {

  @Override
  public PsiReference @NotNull [] getReferencesByElement(@NotNull PsiElement element, @NotNull ProcessingContext context) {
    PsiField field = (PsiField)element.getParent();

    String fieldName = field.getName();
    if (!"namespace".equals(fieldName) && !"defaultAction".equals(fieldName)) {
      return PsiReference.EMPTY_ARRAY;
    }

    final PsiClass aClass = field.getContainingClass();

    if (!GrailsArtifact.CONTROLLER.isInstance(aClass)) return PsiReference.EMPTY_ARRAY;

    assert aClass != null;

    PsiReference reference;

    if ("namespace".equals(fieldName)) {
      GrailsNamespaceReference namespaceReference = new GrailsNamespaceReference(element, false);
      namespaceReference.setIgnoredController(PsiUtil.getOriginalClass(aClass));
      reference = namespaceReference;
    }
    else {
      String controllerName = GrailsArtifact.CONTROLLER.getArtifactName(aClass);
      reference = new ActionReference(element, false, controllerName);
    }

    return new PsiReference[]{reference};
  }
}
