// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.pluginSupport.shiro;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.grails.references.controller.ActionReference;
import org.jetbrains.plugins.grails.util.GrailsArtifact;
import org.jetbrains.plugins.groovy.extensions.GroovyNamedArgumentReferenceProvider;
import org.jetbrains.plugins.groovy.lang.psi.api.GroovyResolveResult;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.GrField;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.arguments.GrNamedArgument;

public class GrailsShiroReferenceProvider implements GroovyNamedArgumentReferenceProvider {

  @Override
  public PsiReference[] createRef(@NotNull PsiElement element,
                                  @NotNull GrNamedArgument namedArgument,
                                  @NotNull GroovyResolveResult resolveResult,
                                  @NotNull ProcessingContext context) {
    GrField field = PsiTreeUtil.getParentOfType(namedArgument, GrField.class);
    if (field == null) return PsiReference.EMPTY_ARRAY;

    PsiClass controllerClass = field.getContainingClass();

    if (!GrailsArtifact.CONTROLLER.isInstance(controllerClass)) return PsiReference.EMPTY_ARRAY;
    assert controllerClass != null;

    String controllerName = GrailsArtifact.CONTROLLER.getArtifactName(controllerClass);

    return new PsiReference[]{new ActionReference(element, false, controllerName)};
  }
}
