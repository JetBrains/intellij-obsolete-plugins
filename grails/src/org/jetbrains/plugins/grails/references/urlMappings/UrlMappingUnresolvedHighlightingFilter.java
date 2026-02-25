// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.references.urlMappings;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.groovy.extensions.GroovyUnresolvedHighlightFilter;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrMethodCall;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrReferenceExpression;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.literals.GrString;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.literals.GrStringInjection;

public final class UrlMappingUnresolvedHighlightingFilter extends GroovyUnresolvedHighlightFilter {
  @Override
  public boolean isReject(@NotNull GrReferenceExpression expression) {
    PsiElement parent = expression.getParent();

    if (parent instanceof GrStringInjection) {
      PsiElement gString = parent.getParent();
      if (!(gString instanceof GrString)) return false;

      PsiElement eMethodCall = gString.getParent();
      if (!(eMethodCall instanceof GrMethodCall methodCall)) return false;

      return UrlMappingUtil.isMappingDefinition(methodCall);
    }

    if (parent instanceof GrMethodCall) {
      return UrlMappingUtil.isMappingDefinition((GrMethodCall)parent);
    }

    return false;
  }
}
