// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.spring.osgi.model.converters;

import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTypesUtil;
import com.intellij.spring.model.SpringBeanPointer;
import com.intellij.spring.osgi.model.xml.Service;
import com.intellij.util.xml.ConvertContext;
import org.jetbrains.annotations.Nullable;

// method signature must be:
// public void anyMethodName(ServiceType serviceInstance, Map serviceProperties);
// public void anyMethodName(ServiceType serviceInstance, Dictionary serviceProperties);

// where ServiceType can be any type compatible with the exported service interface of the service
public class RegistrationListenerMethodConverter extends BasicListenerMethodConverter {

  @Override
  protected boolean checkParameterList(final PsiMethod method, final ConvertContext context) {
    final Project project = method.getProject();
    final PsiParameter[] parameters = method.getParameterList().getParameters();
    if (parameters.length == 2) {
      final PsiType type2 = parameters[1].getType();

      return checkServiceTypeParameter(context, parameters[0].getType()) &&
             (checkType(type2, CommonClassNames.JAVA_UTIL_MAP, project) ||
              checkType(type2, CommonClassNames.JAVA_UTIL_DICTIONARY, project));

    }
    return false;
  }

  private static boolean checkServiceTypeParameter(final ConvertContext context, final PsiType type) {
    final Service service = context.getInvocationElement().getParentOfType(Service.class, false);

    if (service != null && type != null) {
      final PsiClass beanClass = getServiceBeanClass(service);
      if (beanClass != null) {
        return type.isAssignableFrom(PsiTypesUtil.getClassType(beanClass));
      }

    }
    return true;
  }

  private static @Nullable PsiClass getServiceBeanClass(final Service service) {
    final SpringBeanPointer<?>  value = service.getRef().getValue();
    if (value != null) {
      return value.getBeanClass();
    }
    return PsiTypesUtil.getPsiClass(service.getBean().getBeanType());
  }
}
