// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.groovy.grails.reference.controller;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethod;
import org.jetbrains.plugins.groovy.grails.Grails14TestCase;

public class GrailsController14Test extends Grails14TestCase {
  public void testResolve() {
    configureByController("""
                            class CccController {
                              def index = {
                                header<caret>("headerName", "headerValue")
                              }
                            }
                            """);

    PsiElement e = myFixture.getElementAtCaret();
    assertInstanceOf(e, PsiMethod.class);

    assertEquals("org.codehaus.groovy.grails.plugins.web.api.ControllersApi", ((PsiMethod)e).getContainingClass().getQualifiedName());
  }

  public void testNonObjectMethod() {
    configureByController("""
                            class CccController {
                              def index = {
                                setGspEncodin<caret>
                              }
                            }
                            """);

    LookupElement[] res = myFixture.completeBasic();

    assertNotNull(res);
    assertEmpty(res);
  }

  public void testCompletion() {
    PsiFile controller = addController("""
                                         class CccController {
                                           def index = {
                                             <caret>
                                           }
                                         }
                                         """);
    checkCompletion(controller, "log", "render", "modelAndView", "webRequest", "actionName");
  }
}
