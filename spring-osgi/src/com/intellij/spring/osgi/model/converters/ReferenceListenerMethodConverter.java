// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.spring.osgi.model.converters;

import com.intellij.openapi.project.Project;
import com.intellij.psi.CommonClassNames;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiType;
import com.intellij.spring.osgi.constants.SpringOsgiConstants;
import com.intellij.util.xml.ConvertContext;

//public void anyMethodName(ServiceType service, Dictionary properties);
//public void anyMethodName(ServiceType service, Map properties);
//public void anyMethodName(ServiceReference ref);
public class ReferenceListenerMethodConverter extends BasicListenerMethodConverter {

  @Override
  protected boolean checkParameterList(final PsiMethod method, final ConvertContext context) {
    return checkProperParameters(method);
  }

  public static boolean checkProperParameters(PsiMethod method) {
    final Project project = method.getProject();
    final PsiParameter[] parameters = method.getParameterList().getParameters();
    if (parameters.length == 2) {
      final PsiType type2 = parameters[1].getType();

      return checkType(type2, CommonClassNames.JAVA_UTIL_MAP, project) || checkType(type2, CommonClassNames.JAVA_UTIL_DICTIONARY, project);
    } else if (parameters.length == 1) {
      return checkType(parameters[0].getType(), SpringOsgiConstants.OSGI_SERVICE_REFERENCE_CLASSNAME, project);
    }
    return false;
  }

}
