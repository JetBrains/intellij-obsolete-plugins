// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.references.domain;

import com.intellij.openapi.util.Pair;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.groovy.extensions.GroovyNamedArgumentProvider;
import org.jetbrains.plugins.groovy.extensions.NamedArgumentDescriptor;
import org.jetbrains.plugins.groovy.extensions.impl.TypeCondition;
import org.jetbrains.plugins.groovy.lang.psi.api.GroovyResolveResult;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrCall;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrMethodCall;
import org.jetbrains.plugins.groovy.lang.psi.impl.synthetic.GrLightMethodBuilder;

import java.util.Map;

public class GormPersistentPropertiesNamedArgProvider extends GroovyNamedArgumentProvider {

  @Override
  public void getNamedArguments(@NotNull GrCall call,
                                @NotNull GroovyResolveResult resolveResult,
                                @Nullable String argumentName,
                                boolean forCompletion,
                                @NotNull Map<String, NamedArgumentDescriptor> result) {
    if (!(call instanceof GrMethodCall)) return;
    PsiElement resolved = resolveResult.getElement();
    if (!(resolved instanceof PsiMethod)) return;

    PsiClass domainClass = getDomainClass((GrMethodCall)call, (PsiMethod)resolved, resolveResult);
    if (domainClass == null) return;

    if (argumentName == null) {
      for (Map.Entry<String, Pair<PsiType, PsiElement>> entry : DomainDescriptor.getPersistentProperties(domainClass).entrySet()) {
        result.put(entry.getKey(), new TypeCondition(entry.getValue().first, entry.getValue().second));
      }
    }
    else {
      Pair<PsiType, PsiElement> pair = DomainDescriptor.getPersistentProperties(domainClass).get(argumentName);
      if (pair != null) {
        result.put(argumentName, new TypeCondition(pair.first, pair.second));
      }
    }
  }

  protected @Nullable PsiClass getDomainClass(@NotNull GrMethodCall call, PsiMethod resolve, GroovyResolveResult resolveResult) {
    return ((GrLightMethodBuilder)resolve).getData();
  }
}
