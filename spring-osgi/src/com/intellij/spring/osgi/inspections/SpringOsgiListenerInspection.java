// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.spring.osgi.inspections;

import com.intellij.psi.*;
import com.intellij.psi.util.InheritanceUtil;
import com.intellij.psi.util.PsiTypesUtil;
import com.intellij.spring.CommonSpringModel;
import com.intellij.spring.model.SpringBeanPointer;
import com.intellij.spring.model.xml.beans.Beans;
import com.intellij.spring.model.xml.beans.SpringBean;
import com.intellij.spring.osgi.SpringOsgiBundle;
import com.intellij.spring.osgi.constants.SpringOsgiConstants;
import com.intellij.spring.osgi.model.converters.ReferenceListenerMethodConverter;
import com.intellij.spring.osgi.model.xml.BaseOsgiReference;
import com.intellij.spring.osgi.model.xml.Listener;
import com.intellij.util.xml.DomElement;
import com.intellij.util.xml.GenericAttributeValue;
import com.intellij.util.xml.highlighting.DomElementAnnotationHolder;
import org.jetbrains.annotations.Nullable;

public final class SpringOsgiListenerInspection extends SpringOsgiBaseInspection {
  @Override
  protected void checkOsgiReference(BaseOsgiReference reference, Beans beans, DomElementAnnotationHolder holder, CommonSpringModel springModel) {
    for (Listener listener : reference.getListeners()) {
      checkBindMethodsSignature(listener, holder);
      checkListenerClass(listener, holder);
    }
  }

  private static void checkListenerClass(Listener listener, DomElementAnnotationHolder holder) {
    if(listener.getBindMethod().getValue() != null && listener.getUnbindMethod().getValue() != null) return; // IDEADEV-40586

    SpringBeanPointer<?>  springBeanPointer = listener.getRef().getValue();
    if (springBeanPointer != null) {
      checkListenerClass(holder, springBeanPointer.getBeanClass(), listener.getRef());
    }

    SpringBean bean = listener.getBean();
    if (bean != null) {
      checkListenerClass(holder, PsiTypesUtil.getPsiClass(bean.getBeanType()), bean);
    }
  }

  private static void checkListenerClass(DomElementAnnotationHolder holder, @Nullable PsiClass psiClass, DomElement element) {
    if (psiClass == null) return;
    if (!InheritanceUtil.isInheritor(psiClass, SpringOsgiConstants.OSGI_SERVICE_LIFECYCLE_LISTENER_CLASSNAME)) {
      holder.createProblem(element, SpringOsgiBundle.message("model.inspection.listener.class.extends", SpringOsgiConstants.OSGI_SERVICE_LIFECYCLE_LISTENER_CLASSNAME));
    }
  }

  private static void checkBindMethodsSignature(Listener listener, DomElementAnnotationHolder holder) {
    checkMethodSignature(listener.getBindMethod(), holder);
    checkMethodSignature(listener.getUnbindMethod(), holder);
  }

  //public void anyMethodName(ServiceType service, Dictionary properties);
  //public void anyMethodName(ServiceType service, Map properties);
  //public void anyMethodName(ServiceReference ref);
  private static void checkMethodSignature(GenericAttributeValue<PsiMethod> method, DomElementAnnotationHolder holder) {
    PsiMethod psiMethod = method.getValue();
    if (psiMethod != null) {
      final PsiType returnType = psiMethod.getReturnType();
      if (!PsiTypes.voidType().equals(returnType)) {
        holder.createProblem(method, SpringOsgiBundle.message("model.inspection.listener.common.method.return.type"));
      }
      if (!psiMethod.getModifierList().hasModifierProperty(PsiModifier.PUBLIC)) {
        holder.createProblem(method, SpringOsgiBundle.message("model.inspection.listener.common.method.public"));
      }

      if (!ReferenceListenerMethodConverter.checkProperParameters(psiMethod) ) {
        holder.createProblem(method, SpringOsgiBundle.message("model.inspection.listener.common.method.parameters"));
      }
    }
  }
}