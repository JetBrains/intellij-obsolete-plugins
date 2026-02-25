// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.pluginSupport.resources;

import com.intellij.pom.PomDeclarationSearcher;
import com.intellij.pom.PomTarget;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethod;
import com.intellij.util.Consumer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.groovy.lang.psi.GroovyFile;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrMethodCall;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrReferenceExpression;

import java.util.Map;

public final class GrailsResourcesPomDeclarationSearcher extends PomDeclarationSearcher {
  @Override
  public void findDeclarationsAt(@NotNull PsiElement element, int offsetInElement, @NotNull Consumer<? super PomTarget> consumer) {
    if (!(element instanceof GrReferenceExpression)) return;

    PsiElement parent = element.getParent();
    if (!(parent instanceof GrMethodCall)) return;

    if (!GrailsResourcesUtil.isModuleDefinition((GrMethodCall)parent)) {
      return;
    }
    
    PsiFile containingFile = parent.getContainingFile().getOriginalFile();

    if (!(containingFile instanceof GroovyFile)) return;

    Map<String,PsiMethod> map = GrailsResourcesUtil.extractResourcesModules((GroovyFile)containingFile);
    for (PsiMethod method : map.values()) {
      if (method.getNavigationElement() == parent) {
        consumer.consume(method);
        break;
      }
    }
  }
}
