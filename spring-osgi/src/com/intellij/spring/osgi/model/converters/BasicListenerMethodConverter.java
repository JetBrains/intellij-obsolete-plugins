// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.spring.osgi.model.converters;

import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTypesUtil;
import com.intellij.spring.model.SpringBeanPointer;
import com.intellij.spring.model.converters.SpringBeanMethodConverter;
import com.intellij.spring.model.xml.beans.SpringBean;
import com.intellij.spring.osgi.model.xml.BasicListener;
import com.intellij.util.xml.ConvertContext;
import com.intellij.util.xml.DomUtil;
import org.jetbrains.annotations.Nullable;

public abstract class BasicListenerMethodConverter extends SpringBeanMethodConverter {

  @Override
  protected MethodAccepter getMethodAccepter(final ConvertContext context, final boolean forCompletion) {
    return new MethodAccepter() {

      @Override
      public boolean accept(PsiMethod method) {
        final PsiClass containingClass = method.getContainingClass();
        final String containing = containingClass != null ? containingClass.getQualifiedName() : null;

        return checkParameterList(method, context) &&
               checkModifiers(method) &&
               checkReturnType(context, method, forCompletion) &&
               !method.isConstructor() &&
               containing != null &&
               !containing.equals(CommonClassNames.JAVA_LANG_OBJECT);
      }
    };
  }

  @Override
  protected PsiClass getPsiClass(final ConvertContext context) {
    final BasicListener registrationListener = getBasicListener(context);

    if (registrationListener != null) {
      if (!DomUtil.hasXml(registrationListener.getRef())) {
        final SpringBean bean = registrationListener.getBean();
        if (bean != null) return PsiTypesUtil.getPsiClass(bean.getBeanType());
      }
      else {
        final SpringBeanPointer<?>  value = registrationListener.getRef().getValue();
        if (value != null) {
          return value.getBeanClass();
        }
      }
    }

    return null;
  }

  private static @Nullable BasicListener getBasicListener(final ConvertContext context) {
    return context.getInvocationElement().getParentOfType(BasicListener.class, false);
  }

  protected abstract boolean checkParameterList(final PsiMethod method, final ConvertContext context);

  public static boolean checkType(final @Nullable PsiType type, final String className, final Project project) {
    if (type == null) return false;
    if (className.equals(type.getCanonicalText())) return true;

    final PsiClass psiClass = JavaPsiFacade.getInstance(project).findClass(className, GlobalSearchScope.allScope(project));

    return psiClass != null && type.isAssignableFrom(PsiTypesUtil.getClassType(psiClass));
  }

  @Override
  protected boolean checkReturnType(final ConvertContext context, final PsiMethod method, final boolean forCompletion) {
    final PsiType returnType = method.getReturnType();

    return PsiTypes.voidType().equals(returnType);
  }

  @Override
  public LocalQuickFix[] getQuickFixes(final ConvertContext context) {
    return LocalQuickFix.EMPTY_ARRAY; //todo serega.vasiliev
  }
}

