// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.spring;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiType;
import com.intellij.psi.PsiVariable;
import com.intellij.spring.CommonSpringModel;
import com.intellij.spring.model.SpringBeanPointer;
import com.intellij.spring.model.utils.SpringModelSearchers;
import com.intellij.spring.model.utils.SpringModelUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.grails.util.GrailsArtifact;
import org.jetbrains.plugins.grails.util.GrailsUtils;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.GrField;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.GrVariable;
import org.jetbrains.plugins.groovy.lang.psi.impl.statements.expressions.TypesUtil;
import org.jetbrains.plugins.groovy.lang.psi.typeEnhancers.GrVariableEnhancer;

public final class InjectedSpringBeanProvider extends GrVariableEnhancer {

  public static boolean isSupportInjection(@NotNull PsiClass aClass) {
    return GrailsUtils.isBootStrapClass(aClass)
           || GrailsArtifact.getType(aClass) != null
           || GrailsUtils.isInGrailsTests(aClass)
           || GrailsUtils.isCommandClass(aClass);
  }

  public static @Nullable SpringBeanPointer<?>  getInjectedBean(@NotNull PsiVariable variable) {
    if (!(variable instanceof GrField) || !((GrField)variable).isProperty()) {
      return null;
    }

    PsiType declaredType = ((GrField)variable).getDeclaredType();
    if (declaredType != null && !(declaredType instanceof PsiClassType)) return null;

    PsiClass aClass = ((GrField)variable).getContainingClass();
    if (aClass == null || !isSupportInjection(aClass)) return null;

    final CommonSpringModel model = SpringModelUtils.getInstance().getSpringModel(aClass);

    final SpringBeanPointer<?>  springBean = SpringModelSearchers.findBean(model, variable.getName());
    if (springBean == null) return null;

    if (declaredType != null) {
      if (!beanCanBeAssignedTo(springBean, declaredType)) return null;
    }

    return springBean;
  }

  private static boolean beanCanBeAssignedTo(SpringBeanPointer<?> springBean, PsiType variableType) {
    var type = springBean.getEffectiveBeanTypes();
    if (type.isEmpty()) return true;

    for (PsiType psiType : type) {
      if (psiType.isAssignableFrom(variableType) || variableType.isAssignableFrom(psiType)) {
        return true;
      }
    }

    return false;
  }

  @Override
  public PsiType getVariableType(GrVariable variable) {
    if (variable.getDeclaredType() != null) return null;

    SpringBeanPointer<?>  bean = getInjectedBean(variable);

    if (bean != null) {
      return TypesUtil.getLeastUpperBound(bean.getEffectiveBeanTypes().toArray(PsiType.EMPTY_ARRAY), variable.getManager());
    }

    return null;
  }

}
