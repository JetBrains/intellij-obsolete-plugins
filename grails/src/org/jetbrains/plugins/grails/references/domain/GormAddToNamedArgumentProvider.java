// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.references.domain;

import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiType;
import com.intellij.psi.util.PsiTypesUtil;
import com.intellij.psi.util.PsiUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.groovy.extensions.GroovyNamedArgumentProvider;
import org.jetbrains.plugins.groovy.extensions.NamedArgumentDescriptor;
import org.jetbrains.plugins.groovy.extensions.impl.TypeCondition;
import org.jetbrains.plugins.groovy.lang.psi.api.GroovyResolveResult;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrCall;

import java.util.Map;

public class GormAddToNamedArgumentProvider extends GroovyNamedArgumentProvider {
  @Override
  public void getNamedArguments(@NotNull GrCall call,
                                @NotNull GroovyResolveResult resolveResult,
                                @Nullable String argumentName,
                                boolean forCompletion,
                                @NotNull Map<String, NamedArgumentDescriptor> result) {
    PsiElement resolve = resolveResult.getElement();
    if (!(resolve instanceof PsiMethod method)) return;

    String methodName = method.getName();
    assert methodName.startsWith("addTo");

    String propertyName = StringUtil.decapitalize(methodName.substring("addTo".length()));

    PsiType domainClassType = method.getReturnType();
    assert domainClassType != null;
    PsiClass domainClass = ((PsiClassType)domainClassType).resolve();

    assert GormUtils.isGormBean(domainClass);
    assert domainClass != null;

    Pair<PsiType,PsiElement> pair = DomainDescriptor.getPersistentProperties(domainClass).get(propertyName);
    if (pair == null) return;

    PsiClass referencedDomain = PsiTypesUtil.getPsiClass(PsiUtil.extractIterableTypeParameter(pair.first, true));
    if (!GormUtils.isGormBean(referencedDomain)) return;
    assert referencedDomain != null;

    DomainDescriptor referencedDescriptor = DomainDescriptor.getDescriptor(referencedDomain);

    if (argumentName == null) {
      for (Map.Entry<String, Pair<PsiType, PsiElement>> entry : referencedDescriptor.getPersistentProperties().entrySet()) {
        PsiType fieldType = entry.getValue().first;
        result.put(entry.getKey(), new TypeCondition(fieldType, entry.getValue().second));
      }
    }
    else {
      Pair<PsiType, PsiElement> p = referencedDescriptor.getPersistentProperties().get(argumentName);
      if (p != null) {
        result.put(argumentName, new TypeCondition(p.first, p.second));
      }
    }
  }
}
