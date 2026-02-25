// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.inspections;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiModifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.grails.util.GrailsArtifact;
import org.jetbrains.plugins.groovy.codeInspection.declaration.GrMethodMayBeStaticInspectionFilter;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.typedef.members.GrMethod;

public final class GrailsMethodMayBeStaticFilter extends GrMethodMayBeStaticInspectionFilter {
  @Override
  public boolean isIgnored(@NotNull GrMethod method) {
    if (!method.getModifierList().hasModifierProperty(PsiModifier.PUBLIC)) {
      return false;
    }

    PsiClass aClass = method.getContainingClass();

    return GrailsArtifact.getType(aClass) != null;
  }
}
