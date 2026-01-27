// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.spring.osgi.inspections;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTypesUtil;
import com.intellij.spring.CommonSpringModel;
import com.intellij.spring.model.BeanService;
import com.intellij.spring.model.CommonSpringBean;
import com.intellij.spring.model.SpringBeanPointer;
import com.intellij.spring.model.xml.beans.Beans;
import com.intellij.spring.model.xml.beans.SpringBean;
import com.intellij.spring.model.xml.beans.SpringValue;
import com.intellij.spring.osgi.SpringOsgiBundle;
import com.intellij.spring.osgi.model.xml.BaseOsgiReference;
import com.intellij.spring.osgi.model.xml.InterfacesOwner;
import com.intellij.spring.osgi.model.xml.Service;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.xml.DomElement;
import com.intellij.util.xml.DomUtil;
import com.intellij.util.xml.GenericAttributeValue;
import com.intellij.util.xml.highlighting.DomElementAnnotationHolder;

import java.util.*;

public final class SpringOsgiServiceCommonInspection extends SpringOsgiBaseInspection {
  @Override
  protected void checkOsgiService(final Service service,
                                  final Beans beans,
                                  final DomElementAnnotationHolder holder,
                                  final CommonSpringModel springModel) {
    super.checkOsgiService(service, beans, holder, springModel);

    //checkIsInterfaces(service, holder);
    checkRefBeanIsAssignable(service, holder);
  }

  private static void checkIsInterfaces(final Service service, final DomElementAnnotationHolder holder) {
    final Map<DomElement, PsiClass> serviceInterfaces = getServiceInterfaces(service);
    for (DomElement domElement : serviceInterfaces.keySet()) {
      final PsiClass psiClass = serviceInterfaces.get(domElement);
      if (psiClass != null && !psiClass.isInterface()) {
        holder.createProblem(domElement, SpringOsgiBundle.message("service.common.inspection.is.interface"));
      }
    }
  }

  private static void checkRefBeanIsAssignable(final Service service, final DomElementAnnotationHolder holder) {
    final Set<PsiClass> mustBeImplemned = new HashSet<>(getServiceInterfaces(service).values());

    final SpringBeanPointer<?>  beanPointer = service.getRef().getValue();
    if (beanPointer != null) {
      checkIsImplemented(service.getRef(), beanPointer.getSpringBean(), mustBeImplemned, holder);
    }

    final SpringBean innerBean = service.getBean();
    if (innerBean != null && DomUtil.hasXml(innerBean)) {
      checkIsImplemented(innerBean, innerBean, mustBeImplemned, holder);
    }
  }

  private static Map<DomElement, PsiClass> getServiceInterfaces(final InterfacesOwner interfacesOwner) {
    final Project project = interfacesOwner.getManager().getProject();
    Map<DomElement, PsiClass> interfaces = new HashMap<>();

    final GenericAttributeValue<PsiClass> anInterface = interfacesOwner.getInterface();
    final PsiClass psiClass = anInterface.getValue();
    if (psiClass != null) {
      interfaces.put(anInterface, psiClass);
    }

    for (SpringValue value : interfacesOwner.getInterfaces().getValues()) {
      final String stringValue = value.getStringValue();
      if (!StringUtil.isEmptyOrSpaces(stringValue)) {
        final PsiClass aClass = JavaPsiFacade.getInstance(project).findClass(stringValue, GlobalSearchScope.allScope(project));
        if (aClass != null) {
          interfaces.put(value, aClass);
        }
      }
    }
    return interfaces;
  }

  private static void checkIsImplemented(final DomElement domElement,
                                         final CommonSpringBean bean,
                                         final Set<PsiClass> mustBeImplmented,
                                         final DomElementAnnotationHolder holder) {
    final Collection<PsiClass> psiClasses = getSupportedTypes(bean);
    for (PsiClass aClass : mustBeImplmented) {
      boolean implemented = false;
      for (PsiClass supportedType : psiClasses) {
        if (supportedType.equals(aClass) || supportedType.isInheritor(aClass, true)) {
          implemented = true;
          break;
        }
      }
      if (!implemented) {
        holder.createProblem(domElement, SpringOsgiBundle.message("service.common.inspection.interface.must.be.implemented",
                                                                  aClass.getQualifiedName()));
      }
    }
  }

  private static Collection<PsiClass> getSupportedTypes(CommonSpringBean bean) {
    if (bean instanceof BaseOsgiReference) {
      return getServiceInterfaces((BaseOsgiReference)bean).values();
    }
    return ContainerUtil.mapNotNull(BeanService.getInstance().getEffectiveBeanTypes(bean), PsiTypesUtil::getPsiClass);
  }
}

