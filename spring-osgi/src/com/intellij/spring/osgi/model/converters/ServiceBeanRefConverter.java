// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.spring.osgi.model.converters;

import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.CommonClassNames;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.spring.model.converters.SpringBeanResolveConverterForDefiniteClasses;
import com.intellij.spring.model.xml.beans.SpringValue;
import com.intellij.spring.osgi.model.xml.Service;
import com.intellij.util.ArrayUtilRt;
import com.intellij.util.xml.ConvertContext;
import com.intellij.util.xml.DomElement;

import java.util.ArrayList;
import java.util.List;

public class ServiceBeanRefConverter extends SpringBeanResolveConverterForDefiniteClasses {

  @Override
  protected String[] getClassNames(final ConvertContext context) {
    List<String> classes = new ArrayList<>();
    final DomElement element = context.getInvocationElement();
    final Service service = element.getParentOfType(Service.class, false);
    if (service != null) {
      final PsiClass psiClass = service.getInterface().getValue();
      if (psiClass != null) {
        classes.add(psiClass.getQualifiedName());
      }

        for (SpringValue value : service.getInterfaces().getValues()) {
          final String stringValue = value.getStringValue();
          if (!StringUtil.isEmptyOrSpaces(stringValue)) {

            GlobalSearchScope scope = context.getSearchScope();
            if (scope == null)  scope = GlobalSearchScope.allScope(context.getProject());

            final PsiClass aClass = JavaPsiFacade.getInstance(context.getProject()).findClass(stringValue, scope);
            if (aClass != null) {
              classes.add(aClass.getQualifiedName());
            }
          }
      }
    }

    return classes.isEmpty() ? new String[]{CommonClassNames.JAVA_LANG_OBJECT} : ArrayUtilRt.toStringArray(classes);
  }
}
