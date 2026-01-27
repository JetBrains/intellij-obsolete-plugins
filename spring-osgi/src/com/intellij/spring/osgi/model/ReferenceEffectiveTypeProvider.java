// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.spring.osgi.model;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiType;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTypesUtil;
import com.intellij.spring.model.CommonSpringBean;
import com.intellij.spring.model.SpringBeanEffectiveTypeProvider;
import com.intellij.spring.model.xml.beans.SpringValue;
import com.intellij.spring.osgi.model.xml.BaseOsgiReference;
import com.intellij.util.Processor;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

public class ReferenceEffectiveTypeProvider extends SpringBeanEffectiveTypeProvider {
  private final @NonNls String[] UnboxingReferencesClassNames = new String[]{"org.osgi.framework.ServiceReference"};

  @Override
  public boolean processEffectiveTypes(final @NotNull CommonSpringBean bean, final @NotNull Processor<PsiType> processor) {
    if (!(bean instanceof BaseOsgiReference reference)) return true;

    final Project project = bean.getPsiManager().getProject();
    final PsiClass psiClass = reference.getInterface().getValue();
    if (psiClass != null){
      if (!processor.process(PsiTypesUtil.getClassType(psiClass))) return false;
    }

    for (SpringValue value : reference.getInterfaces().getValues()) {
      if (!processClass(processor, project, value.getStringValue())) return false;
    }
    for (String className : UnboxingReferencesClassNames) {
      // 6.2.1.9 http://static.springframework.org/osgi/docs/1.2.0-m1/reference/html/service-registry.html
      if(!processClass(processor, project, className)) return false;
    }
    return true;
  }

  private static boolean processClass(final @NotNull Processor<PsiType> processor, final Project project, final String className) {
    if (!StringUtil.isEmptyOrSpaces(className)) {
      final PsiClass aClass = JavaPsiFacade.getInstance(project).findClass(className, GlobalSearchScope.allScope(project));
      if (aClass != null) {
        if (!processor.process(PsiTypesUtil.getClassType(aClass))) return false;
      }
    }
    return true;
  }
}
