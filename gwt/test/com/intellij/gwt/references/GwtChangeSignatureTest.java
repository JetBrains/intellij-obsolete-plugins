/*
 * Copyright (c) 2000-2006 JetBrains s.r.o. All Rights Reserved.
 */

package com.intellij.gwt.references;

import com.intellij.gwt.GwtTestCase;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiParameterList;
import com.intellij.psi.PsiType;
import com.intellij.psi.PsiTypes;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.refactoring.changeSignature.ChangeSignatureProcessor;
import com.intellij.refactoring.changeSignature.ParameterInfoImpl;

public class GwtChangeSignatureTest extends GwtTestCase {

  public void testChangeParameterType() {
    addGwtModule("references/service/src");

    final PsiClass serviceAsyncClass = myJavaFacade.findClass("client.MyServiceAsync", GlobalSearchScope.allScope(myProject));
    assertNotNull(serviceAsyncClass);

    final PsiMethod asyncMethod = serviceAsyncClass.getMethods()[0];
    final PsiParameter asyncParam = asyncMethod.getParameterList().getParameters()[1];
    final ParameterInfoImpl[] infos = {ParameterInfoImpl.create(0).withName("l").withType(PsiTypes.longType()),
      ParameterInfoImpl.create(1).withName(asyncParam.getName()).withType(asyncParam.getType())};
    changeSignature(asyncMethod, infos);
    final PsiParameterList parameterList = asyncMethod.getParameterList();
    assertEquals(2, parameterList.getParametersCount());
    assertEquals(PsiTypes.longType(), parameterList.getParameters()[0].getType());

    final PsiClass serviceClass = myJavaFacade.findClass("client.MyService", GlobalSearchScope.allScope(myProject));
    assertNotNull(serviceClass);
    final PsiMethod method = serviceClass.getMethods()[0];
    final PsiParameter[] parameters = method.getParameterList().getParameters();
    assertEquals(1, parameters.length);
    assertEquals(PsiTypes.longType(), parameters[0].getType());
  }

  public void testDeleteParameter() {
    addGwtModule("references/service/src");

    final PsiClass serviceAsyncClass = myJavaFacade.findClass("client.MyServiceAsync", GlobalSearchScope.allScope(myProject));
    assertNotNull(serviceAsyncClass);

    final PsiMethod asyncMethod = serviceAsyncClass.getMethods()[0];
    final PsiParameter asyncParam = asyncMethod.getParameterList().getParameters()[1];
    final PsiType asyncType = asyncParam.getType();
    final ParameterInfoImpl[] infos = {ParameterInfoImpl.create(1).withName(asyncParam.getName()).withType(asyncType)};
    changeSignature(asyncMethod, infos);
    final PsiParameterList parameterList = asyncMethod.getParameterList();
    assertEquals(1, parameterList.getParametersCount());
    assertEquals(asyncType, parameterList.getParameters()[0].getType());

    final PsiClass serviceClass = myJavaFacade.findClass("client.MyService", GlobalSearchScope.allScope(myProject));
    assertNotNull(serviceClass);
    final PsiMethod method = serviceClass.getMethods()[0];
    final PsiParameter[] parameters = method.getParameterList().getParameters();
    assertEquals(0, parameters.length);
  }

  public void testAddParameter() {
    addGwtModule("references/service/src");

    final PsiClass serviceAsyncClass = myJavaFacade.findClass("client.MyServiceAsync", GlobalSearchScope.allScope(myProject));
    assertNotNull(serviceAsyncClass);

    final PsiMethod asyncMethod = serviceAsyncClass.getMethods()[0];
    final PsiParameter asyncParam = asyncMethod.getParameterList().getParameters()[1];
    final PsiType asyncType = asyncParam.getType();
    final ParameterInfoImpl[] infos = {ParameterInfoImpl.create(0).withName("i").withType(PsiTypes.longType()),
      ParameterInfoImpl.createNew().withName("l").withType(PsiTypes.longType()).withDefaultValue("0"),
      ParameterInfoImpl.create(1).withName(asyncParam.getName()).withType(asyncType)};
    changeSignature(asyncMethod, infos);
    final PsiParameterList parameterList = asyncMethod.getParameterList();
    assertEquals(3, parameterList.getParametersCount());
    assertEquals(PsiTypes.longType(), parameterList.getParameters()[0].getType());
    assertEquals(PsiTypes.longType(), parameterList.getParameters()[1].getType());


    final PsiClass serviceClass = myJavaFacade.findClass("client.MyService", GlobalSearchScope.allScope(myProject));
    assertNotNull(serviceClass);
    final PsiMethod method = serviceClass.getMethods()[0];
    final PsiParameter[] parameters = method.getParameterList().getParameters();
    assertEquals(2, parameters.length);
    assertEquals(PsiTypes.longType(), parameters[0].getType());
    assertEquals(PsiTypes.longType(), parameters[1].getType());

  }


  private void changeSignature(final PsiMethod method, final ParameterInfoImpl[] parameterInfo) {
    new ChangeSignatureProcessor(getProject(), method, false, null,
                                 method.getName(),
                                 method.getReturnType(), parameterInfo, null).run();
  }

}
