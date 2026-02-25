// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.references.domain;

import com.intellij.openapi.util.Pair;
import com.intellij.psi.CommonClassNames;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiType;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.groovy.extensions.GroovyNamedArgumentProvider;
import org.jetbrains.plugins.groovy.extensions.NamedArgumentDescriptor;
import org.jetbrains.plugins.groovy.extensions.impl.StringTypeCondition;
import org.jetbrains.plugins.groovy.lang.psi.api.GroovyResolveResult;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.GrField;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrCall;

import java.util.Map;

public class GormSortNamedArgumentProvider extends GroovyNamedArgumentProvider {
  @Override
  public void getNamedArguments(@NotNull GrCall call,
                                @NotNull GroovyResolveResult resolveResult,
                                @Nullable String argumentName,
                                boolean forCompletion,
                                @NotNull Map<String, NamedArgumentDescriptor> result) {
    GrField field = PsiTreeUtil.getParentOfType(call, GrField.class);
    if (field == null || !"mapping".equals(field.getName()) || !field.hasModifierProperty(PsiModifier.STATIC)) return;

    PsiClass domainClass = field.getContainingClass();
    if (!GormUtils.isGormBean(domainClass)) return;

    @SuppressWarnings("ConstantConditions")
    DomainDescriptor descriptor = DomainDescriptor.getDescriptor(domainClass);

    if (argumentName == null) {
      for (Map.Entry<String, Pair<PsiType, PsiElement>> entry : descriptor.getPersistentProperties().entrySet()) {
        result.put(entry.getKey(), new StringTypeCondition(CommonClassNames.JAVA_LANG_STRING, entry.getValue().second));
      }
    }
    else {
      Pair<PsiType, PsiElement> p = descriptor.getPersistentProperties().get(argumentName);
      if (p != null) {
        result.put(argumentName, new StringTypeCondition(CommonClassNames.JAVA_LANG_STRING, p.second));
      }
    }
  }
}
