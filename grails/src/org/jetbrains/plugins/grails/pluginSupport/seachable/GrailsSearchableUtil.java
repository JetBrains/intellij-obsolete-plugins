// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.pluginSupport.seachable;

import com.intellij.psi.CommonClassNames;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.grails.references.domain.GormUtils;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.GrField;
import org.jetbrains.plugins.groovy.lang.psi.impl.synthetic.GrLightMethodBuilder;

public final class GrailsSearchableUtil {

  private GrailsSearchableUtil() {
  }

  public static PsiMethod createAllMethod(PsiManager manager) {
    GrLightMethodBuilder res = new GrLightMethodBuilder(manager, "all");
    res.addOptionalParameter("args", CommonClassNames.JAVA_UTIL_MAP);
    return res;
  }

  public static PsiMethod createMethod(String name, PsiElement navigationElement, PsiClass containingClass) {
    GrLightMethodBuilder res = new GrLightMethodBuilder(navigationElement.getManager(), name);
    res.addOptionalParameter("args", CommonClassNames.JAVA_UTIL_MAP);
    res.setNavigationElement(navigationElement);
    res.setContainingClass(containingClass);
    return res;
  }

  public static boolean isSearchableField(@NotNull GrField field) {
    if (!"searchable".equals(field.getName()) || !field.hasModifierProperty(PsiModifier.STATIC)) {
      return false;
    }

    return GormUtils.isGormBean(field.getContainingClass());
  }

}
