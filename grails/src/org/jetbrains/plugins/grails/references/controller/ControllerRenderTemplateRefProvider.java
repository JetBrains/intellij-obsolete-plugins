// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.references.controller;

import com.intellij.openapi.paths.PathReference;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.ElementManipulators;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.grails.references.common.GroovyGspTagWrapper;
import org.jetbrains.plugins.grails.references.common.TemplateFileReferenceSet;
import org.jetbrains.plugins.grails.util.GrailsArtifact;
import org.jetbrains.plugins.groovy.extensions.GroovyNamedArgumentReferenceProvider;
import org.jetbrains.plugins.groovy.lang.psi.api.GroovyResolveResult;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.arguments.GrNamedArgument;
import org.jetbrains.plugins.groovy.lang.psi.api.util.GrNamedArgumentsOwner;
import org.jetbrains.plugins.groovy.lang.psi.util.PsiUtil;

public class ControllerRenderTemplateRefProvider implements GroovyNamedArgumentReferenceProvider {

  @Override
  public PsiReference[] createRef(@NotNull PsiElement element,
                                  @NotNull GrNamedArgument namedArgument,
                                  @NotNull GroovyResolveResult resolveResult,
                                  @NotNull ProcessingContext context) {
    final PsiClass aClass = PsiUtil.getContainingNotInnerClass(namedArgument);
    if (!GrailsArtifact.CONTROLLER.isInstance(aClass)) return PsiReference.EMPTY_ARRAY;

    final TextRange range = ElementManipulators.getValueTextRange(element);
    int offset = range.getStartOffset();
    String text = range.substring(element.getText());

    String trimedUrl = PathReference.trimPath(text);

    GroovyGspTagWrapper tagWrapper = new GroovyGspTagWrapper((GrNamedArgumentsOwner)namedArgument.getParent(), null);

    TemplateFileReferenceSet set = new TemplateFileReferenceSet(GrailsArtifact.CONTROLLER.getArtifactName(aClass),
                                                                trimedUrl,
                                                                element,
                                                                offset,
                                                                null,
                                                                true,
                                                                true,
                                                                tagWrapper);

    return set.getAllReferences();
  }

}
